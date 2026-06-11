package com.voting.listener;

import com.voting.cache.InMemoryCache;
import com.voting.entity.Poll;
import com.voting.mapper.PollMapper;
import com.voting.mapper.PollResultMapper;
import com.voting.service.Web3jService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.Log;

import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Contract event listener - async listen to PollCreated / VoteCasted events after startup
 */
@Component
public class ContractEventListener {

    private static final Logger log = LoggerFactory.getLogger(ContractEventListener.class);

    private final Web3j web3j;
    private final PollMapper pollMapper;
    private final PollResultMapper pollResultMapper;
    private final InMemoryCache cache;
    private final Web3jService web3jService;

    @Value("${web3j.contract-address}")
    private String contractAddress;

    private static final Event POLL_CREATED = new Event(
            "PollCreated",
            Arrays.asList(
                    new TypeReference<Uint256>(true) {},
                    new TypeReference<Address>(true) {},
                    new TypeReference<Uint256>() {}));

    private static final Event VOTE_CASTED = new Event(
            "VoteCasted",
            Arrays.asList(
                    new TypeReference<Uint256>(true) {},
                    new TypeReference<Address>(true) {},
                    new TypeReference<Uint256>() {}));

    public ContractEventListener(Web3j web3j, PollMapper pollMapper,
                                  PollResultMapper pollResultMapper,
                                  InMemoryCache cache,
                                  Web3jService web3jService) {
        this.web3j = web3j;
        this.pollMapper = pollMapper;
        this.pollResultMapper = pollResultMapper;
        this.cache = cache;
        this.web3jService = web3jService;
    }

    /**
     * Register event listener asynchronously after application startup.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void startListening() {
        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                EthFilter filter = new EthFilter(
                        DefaultBlockParameterName.EARLIEST,
                        DefaultBlockParameterName.LATEST,
                        contractAddress);
                filter.addOptionalTopics(
                        EventEncoder.encode(POLL_CREATED),
                        EventEncoder.encode(VOTE_CASTED));

                web3j.ethLogFlowable(filter).subscribe(
                        this::handleEvent,
                        error -> log.error("Event listener error: {}", error.getMessage(), error));
                log.info("Contract event listener started, address: {}", contractAddress);
            } catch (Exception e) {
                log.error("Failed to start event listener: {}", e.getMessage(), e);
            }
        });
    }

    private void handleEvent(Log logEvent) {
        try {
            String topic = logEvent.getTopics().get(0);
            if (topic.equals(EventEncoder.encode(POLL_CREATED))) {
                handlePollCreated(logEvent);
            } else if (topic.equals(EventEncoder.encode(VOTE_CASTED))) {
                handleVoteCasted(logEvent);
            }
        } catch (Exception e) {
            log.error("Failed to handle event: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle PollCreated event - sync to MySQL
     */
    @SuppressWarnings("unchecked")
    private void handlePollCreated(Log logEvent) {
        BigInteger pollId = new BigInteger(
                logEvent.getTopics().get(1).substring(2), 16);

        Map<String, Object> chainData = web3jService.getPollFromChain(pollId.longValue());

        Poll poll = new Poll();
        poll.setId(pollId.longValue());
        poll.setCreatorAddress((String) chainData.get("creator"));
        poll.setTitle((String) chainData.get("title"));
        poll.setDescription((String) chainData.get("description"));
        poll.setOptions((List<String>) chainData.get("options"));
        poll.setStartTime(toLocalDateTime((Long) chainData.get("startTime")));
        poll.setEndTime(toLocalDateTime((Long) chainData.get("endTime")));
        poll.setTxHash(logEvent.getTransactionHash());

        // Skip if already synced (event replay from EARLIEST)
        if (pollMapper.selectById(poll.getId()) != null) {
            log.debug("Poll {} already in DB, skipping insert", pollId);
            return;
        }
        pollMapper.insert(poll);
        log.info("Synced poll created: pollId={}, title={}", pollId, poll.getTitle());
    }

    /**
     * Handle VoteCasted event - update cache
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void handleVoteCasted(Log logEvent) {
        BigInteger pollId = new BigInteger(
                logEvent.getTopics().get(1).substring(2), 16);

        // optionIndex is NOT indexed in the event — decode from data field
        List<Type> decoded = FunctionReturnDecoder.decode(
                logEvent.getData(),
                (List) Collections.singletonList(new TypeReference<Uint256>() {}));
        BigInteger optionIndex = ((Uint256) decoded.get(0)).getValue();

        String key = "poll:" + pollId + ":vote_counts";
        String field = optionIndex.toString();

        // Atomic increment in cache
        cache.hincrBy(key, field, 1);
        // Record voter
        String voter = "0x" + logEvent.getTopics().get(2).substring(26);
        cache.sadd("poll:" + pollId + ":voters", voter);

        // Sync to MySQL
        String newCount = cache.hget(key, field);
        pollResultMapper.upsertVoteCount(
                pollId.longValue(), optionIndex.intValue(),
                Integer.parseInt(newCount != null ? newCount : "0"));

        log.info("Synced vote: pollId={}, option={}, count={}", pollId, optionIndex, newCount);
    }

    private LocalDateTime toLocalDateTime(Long epochSecond) {
        return LocalDateTime.ofInstant(
                Instant.ofEpochSecond(epochSecond), ZoneId.of("Asia/Shanghai"));
    }
}
