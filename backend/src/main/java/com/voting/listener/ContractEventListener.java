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
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Contract event listener — initial sync + real-time listening.
 *
 * Strategy:
 *  1. On startup, if DB is empty, pull ALL existing polls from chain.
 *  2. Then subscribe to LATEST events only, avoiding replay.
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
     * After app is ready: initial sync (incremental), then subscribe to new events.
     * Single-thread executor ensures no race between concurrent event callbacks.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void startListening() {
        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                // ── Step 1: incremental sync from chain ──
                initialSyncIfNeeded();

                // ── Step 2: single-thread event handler ──
                Executor eventExecutor = Executors.newSingleThreadExecutor(r -> {
                    Thread t = new Thread(r, "event-handler");
                    t.setDaemon(true);
                    return t;
                });

                EthFilter filter = new EthFilter(
                        DefaultBlockParameterName.LATEST,
                        DefaultBlockParameterName.LATEST,
                        contractAddress);
                filter.addOptionalTopics(
                        EventEncoder.encode(POLL_CREATED),
                        EventEncoder.encode(VOTE_CASTED));

                web3j.ethLogFlowable(filter).subscribe(
                        log -> eventExecutor.execute(() -> handleEvent(log)),
                        error -> log.error("Event listener error: {}", error.getMessage(), error));
                log.info("Contract event listener started, address: {}", contractAddress);
            } catch (Exception e) {
                log.error("Failed to start event listener: {}", e.getMessage(), e);
            }
        });
    }

    /**
     * Incremental sync: compare DB max poll ID vs chain poll count.
     * Syncs any polls on chain that are not yet in DB.
     */
    @SuppressWarnings("unchecked")
    private void initialSyncIfNeeded() {
        try {
            long chainTotal = web3jService.getPollCount();
            if (chainTotal == 0) {
                log.info("Chain has no polls, nothing to sync");
                return;
            }

            Long maxPollId = pollMapper.selectMaxPollId();
            long startFrom = (maxPollId != null) ? maxPollId + 1 : 0;

            if (startFrom >= chainTotal) {
                log.info("DB up to date (max={}, chainTotal={}), skipping sync", maxPollId, chainTotal);
                return;
            }

            log.info("Syncing polls {} to {} (chainTotal={})", startFrom, chainTotal - 1, chainTotal);
            for (long i = startFrom; i < chainTotal; i++) {
                Map<String, Object> chainData = web3jService.getPollFromChain(i);
                Poll poll = new Poll();
                poll.setId(((Number) chainData.get("id")).longValue());
                poll.setCreatorAddress((String) chainData.get("creator"));
                poll.setTitle((String) chainData.get("title"));
                poll.setDescription((String) chainData.get("description"));
                poll.setOptions((List<String>) chainData.get("options"));
                poll.setStartTime(toLocalDateTime(((Number) chainData.get("startTime")).longValue()));
                poll.setEndTime(toLocalDateTime(((Number) chainData.get("endTime")).longValue()));
                pollMapper.insert(poll);

                List<Long> counts = (List<Long>) chainData.get("voteCounts");
                for (int j = 0; j < counts.size(); j++) {
                    int cnt = counts.get(j).intValue();
                    pollResultMapper.upsertVoteCount(i, j, cnt);
                    cache.hset("poll:" + i + ":vote_counts", String.valueOf(j), String.valueOf(cnt));
                }
                log.info("  Synced poll {}: {}", i, poll.getTitle());
            }
            log.info("Initial sync complete — {} polls synced", chainTotal - startFrom);
        } catch (Exception e) {
            log.error("Initial sync failed: {}", e.getMessage(), e);
        }
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
     * Handle PollCreated event — sync to MySQL.
     */
    @SuppressWarnings("unchecked")
    private void handlePollCreated(Log logEvent) {
        BigInteger pollId = new BigInteger(
                logEvent.getTopics().get(1).substring(2), 16);

        if (pollMapper.selectById(pollId.longValue()) != null) {
            log.debug("Poll {} already in DB, skipping", pollId);
            return;
        }

        Map<String, Object> chainData = web3jService.getPollFromChain(pollId.longValue());

        Poll poll = new Poll();
        poll.setId(pollId.longValue());
        poll.setCreatorAddress((String) chainData.get("creator"));
        poll.setTitle((String) chainData.get("title"));
        poll.setDescription((String) chainData.get("description"));
        poll.setOptions((List<String>) chainData.get("options"));
        poll.setStartTime(toLocalDateTime(((Number) chainData.get("startTime")).longValue()));
        poll.setEndTime(toLocalDateTime(((Number) chainData.get("endTime")).longValue()));
        poll.setTxHash(logEvent.getTransactionHash());

        pollMapper.insert(poll);
        log.info("Synced poll created: pollId={}, title={}", pollId, poll.getTitle());
    }

    /**
     * Handle VoteCasted event — pull fresh counts from chain (race-condition safe).
     *
     * Strategy: read authoritative vote counts from chain on every event,
     * then write to cache + MySQL. This avoids hincrBy drift when the
     * frontend sync endpoint and the listener process the same vote.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void handleVoteCasted(Log logEvent) {
        BigInteger pollId = new BigInteger(
                logEvent.getTopics().get(1).substring(2), 16);

        // Pull authoritative counts from chain
        List<Long> chainCounts = web3jService.getVoteCountsFromChain(pollId.longValue());

        // Update cache + MySQL for every option (chain is source of truth)
        String key = "poll:" + pollId + ":vote_counts";
        for (int i = 0; i < chainCounts.size(); i++) {
            int count = chainCounts.get(i).intValue();
            cache.hset(key, String.valueOf(i), String.valueOf(count));
            pollResultMapper.upsertVoteCount(pollId.longValue(), i, count);
        }

        // Track voter
        String voter = "0x" + logEvent.getTopics().get(2).substring(26);
        cache.sadd("poll:" + pollId + ":voters", voter);

        log.info("Synced vote from chain: pollId={}, counts={}", pollId, chainCounts);
    }

    private LocalDateTime toLocalDateTime(Long epochSecond) {
        return LocalDateTime.ofInstant(
                Instant.ofEpochSecond(epochSecond), ZoneId.of("Asia/Shanghai"));
    }
}
