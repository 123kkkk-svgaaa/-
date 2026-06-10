package com.voting.listener;

import com.voting.entity.Poll;
import com.voting.mapper.PollMapper;
import com.voting.mapper.PollResultMapper;
import com.voting.service.Web3jService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * 合约事件监听器 — 启动后异步监听 PollCreated / VoteCasted 事件
 */
@Component
public class ContractEventListener {

    private static final Logger log = LoggerFactory.getLogger(ContractEventListener.class);

    private final Web3j web3j;
    private final PollMapper pollMapper;
    private final PollResultMapper pollResultMapper;
    private final RedisTemplate<String, String> redisTemplate;
    private final Web3jService web3jService;

    @Value("${web3j.contract-address}")
    private String contractAddress;

    // 链上事件定义 (必须与合约中一致)
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
                                  RedisTemplate<String, String> redisTemplate,
                                  Web3jService web3jService) {
        this.web3j = web3j;
        this.pollMapper = pollMapper;
        this.pollResultMapper = pollResultMapper;
        this.redisTemplate = redisTemplate;
        this.web3jService = web3jService;
    }

    /**
     * 应用启动后异步注册事件监听
     */
    @EventListener(ApplicationReadyEvent.class)
    public void startListening() {
        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                EthFilter filter = new EthFilter(
                        DefaultBlockParameterName.EARLIEST,
                        DefaultBlockParameterName.LATEST,
                        contractAddress);
                // 添加两个事件的 topic
                filter.addOptionalTopics(
                        EventEncoder.encode(POLL_CREATED),
                        EventEncoder.encode(VOTE_CASTED));

                web3j.ethLogFlowable(filter).subscribe(
                        this::handleEvent,
                        error -> log.error("事件监听错误: {}", error.getMessage()));
                log.info("合约事件监听已启动, 地址: {}", contractAddress);
            } catch (Exception e) {
                log.error("启动事件监听失败: {}", e.getMessage(), e);
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
            log.error("处理事件失败: {}", e.getMessage());
        }
    }

    /**
     * 处理投票创建事件 — 同步到 MySQL
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

        pollMapper.insert(poll);
        log.info("同步投票创建: pollId={}, title={}", pollId, poll.getTitle());
    }

    /**
     * 处理投票事件 — 更新 Redis 缓存
     */
    private void handleVoteCasted(Log logEvent) {
        BigInteger pollId = new BigInteger(
                logEvent.getTopics().get(1).substring(2), 16);
        BigInteger optionIndex = new BigInteger(
                logEvent.getTopics().get(3).substring(2), 16);

        String key = "poll:" + pollId + ":vote_counts";
        String field = optionIndex.toString();

        // Redis 原子自增
        redisTemplate.opsForHash().increment(key, field, 1);
        // 记录投票者
        String voter = "0x" + logEvent.getTopics().get(2).substring(26);
        redisTemplate.opsForSet().add("poll:" + pollId + ":voters", voter);

        // 异步同步到 MySQL
        String newCount = (String) redisTemplate.opsForHash().get(key, field);
        pollResultMapper.upsertVoteCount(
                pollId.longValue(), optionIndex.intValue(),
                Integer.parseInt(newCount != null ? newCount : "0"));

        log.info("同步投票: pollId={}, option={}, count={}", pollId, optionIndex, newCount);
    }

    private LocalDateTime toLocalDateTime(Long epochSecond) {
        return LocalDateTime.ofInstant(
                Instant.ofEpochSecond(epochSecond), ZoneId.of("Asia/Shanghai"));
    }
}
