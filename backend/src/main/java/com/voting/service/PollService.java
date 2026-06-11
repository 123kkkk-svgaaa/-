package com.voting.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.voting.cache.InMemoryCache;
import com.voting.entity.Poll;
import com.voting.entity.PollResult;
import com.voting.mapper.PollMapper;
import com.voting.mapper.PollResultMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Poll business service - list, detail, sync, verify
 */
@Service
public class PollService {

    private static final Logger log = LoggerFactory.getLogger(PollService.class);

    private final PollMapper pollMapper;
    private final PollResultMapper pollResultMapper;
    private final InMemoryCache cache;
    private final Web3jService web3jService;

    public PollService(PollMapper pollMapper,
                       PollResultMapper pollResultMapper,
                       InMemoryCache cache,
                       Web3jService web3jService) {
        this.pollMapper = pollMapper;
        this.pollResultMapper = pollResultMapper;
        this.cache = cache;
        this.web3jService = web3jService;
    }

    /**
     * Paginated poll list, ordered by creation time desc.
     */
    public Page<Poll> listPolls(int pageNum, int pageSize) {
        Page<Poll> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Poll> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Poll::getCreatedAt);
        return pollMapper.selectPage(page, wrapper);
    }

    /**
     * Get poll detail with real-time vote counts.
     */
    public Map<String, Object> getPollDetail(Long pollId) {
        Poll poll = pollMapper.selectById(pollId);
        if (poll == null) {
            log.warn("Poll not found: id={}", pollId);
            throw new RuntimeException("Poll not found");
        }
        List<Integer> counts = getVoteCounts(pollId, poll.getOptions().size());

        Map<String, Object> result = new HashMap<>();
        result.put("poll", poll);
        result.put("voteCounts", counts);
        result.put("totalVotes", counts.stream().mapToInt(Integer::intValue).sum());
        return result;
    }

    /**
     * Sync poll data from chain to MySQL + cache.
     */
    @SuppressWarnings("unchecked")
    public void syncFromChain(Long pollId, String txHash) {
        Map<String, Object> chainData = web3jService.getPollFromChain(pollId);

        // Update MySQL
        Poll poll = new Poll();
        poll.setId(pollId);
        poll.setTxHash(txHash);
        pollMapper.updateById(poll);

        // Sync vote counts to MySQL + cache
        List<Long> chainCounts = (List<Long>) chainData.get("voteCounts");
        for (int i = 0; i < chainCounts.size(); i++) {
            int count = chainCounts.get(i).intValue();
            pollResultMapper.upsertVoteCount(pollId, i, count);
            cache.hset(
                    "poll:" + pollId + ":vote_counts",
                    String.valueOf(i), String.valueOf(count));
        }
    }

    /**
     * Verify chain data consistency with local MySQL.
     */
    public Map<String, Object> verifyPoll(Long pollId) {
        Poll local = pollMapper.selectById(pollId);
        Map<String, Object> chainData = web3jService.getPollFromChain(pollId);

        Map<String, Object> result = new HashMap<>();
        result.put("chain", chainData);
        result.put("local", local);
        result.put("consistent", local != null
                && chainData.get("title") != null
                && chainData.get("title").equals(local.getTitle()));
        return result;
    }

    /**
     * Get vote counts - cache first, fallback to MySQL.
     */
    private List<Integer> getVoteCounts(Long pollId, int optionCount) {
        List<Integer> counts = new ArrayList<>();
        for (int i = 0; i < optionCount; i++) {
            String cached = cache.hget(
                    "poll:" + pollId + ":vote_counts", String.valueOf(i));
            if (cached != null) {
                counts.add(Integer.parseInt(cached));
            } else {
                Map<String, Object> pk = new HashMap<>();
                pk.put("poll_id", pollId);
                pk.put("option_index", i);
                PollResult fromDb = pollResultMapper.selectByMap(pk)
                        .stream().findFirst().orElse(null);
                int count = fromDb != null ? fromDb.getVoteCount() : 0;
                counts.add(count);
                // Write back to cache
                cache.hset(
                        "poll:" + pollId + ":vote_counts",
                        String.valueOf(i), String.valueOf(count));
            }
        }
        return counts;
    }
}
