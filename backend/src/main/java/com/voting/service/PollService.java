package com.voting.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.voting.entity.Poll;
import com.voting.entity.PollResult;
import com.voting.mapper.PollMapper;
import com.voting.mapper.PollResultMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 投票业务服务 — 列表查询、详情、同步、验证
 */
@Service
public class PollService {

    private final PollMapper pollMapper;
    private final PollResultMapper pollResultMapper;
    private final RedisTemplate<String, String> redisTemplate;
    private final Web3jService web3jService;

    public PollService(PollMapper pollMapper,
                       PollResultMapper pollResultMapper,
                       RedisTemplate<String, String> redisTemplate,
                       Web3jService web3jService) {
        this.pollMapper = pollMapper;
        this.pollResultMapper = pollResultMapper;
        this.redisTemplate = redisTemplate;
        this.web3jService = web3jService;
    }

    /**
     * 分页获取投票列表 — 按创建时间倒序
     */
    public Page<Poll> listPolls(int pageNum, int pageSize) {
        Page<Poll> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Poll> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Poll::getCreatedAt);
        return pollMapper.selectPage(page, wrapper);
    }

    /**
     * 获取投票详情 (含实时票数)
     */
    public Map<String, Object> getPollDetail(Long pollId) {
        Poll poll = pollMapper.selectById(pollId);
        if (poll == null) {
            throw new RuntimeException("投票不存在");
        }
        List<Integer> counts = getVoteCounts(pollId, poll.getOptions().size());

        Map<String, Object> result = new HashMap<>();
        result.put("poll", poll);
        result.put("voteCounts", counts);
        result.put("totalVotes", counts.stream().mapToInt(Integer::intValue).sum());
        return result;
    }

    /**
     * 前端上链后回调 — 从链上读取投票数据同步到 MySQL
     */
    @SuppressWarnings("unchecked")
    public void syncFromChain(Long pollId, String txHash) {
        Map<String, Object> chainData = web3jService.getPollFromChain(pollId);

        // 更新 MySQL
        Poll poll = new Poll();
        poll.setId(pollId);
        poll.setTxHash(txHash);
        pollMapper.updateById(poll);

        // 同步票数到 MySQL + Redis
        List<Long> chainCounts = (List<Long>) chainData.get("voteCounts");
        for (int i = 0; i < chainCounts.size(); i++) {
            int count = chainCounts.get(i).intValue();
            pollResultMapper.upsertVoteCount(pollId, i, count);
            redisTemplate.opsForHash().put(
                    "poll:" + pollId + ":vote_counts",
                    String.valueOf(i), String.valueOf(count));
        }
    }

    /**
     * 链上验证 — 对比链上数据和本地 MySQL
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
     * 获取票数 — Redis 优先，miss 查 MySQL
     */
    private List<Integer> getVoteCounts(Long pollId, int optionCount) {
        List<Integer> counts = new ArrayList<>();
        for (int i = 0; i < optionCount; i++) {
            String cached = (String) redisTemplate.opsForHash()
                    .get("poll:" + pollId + ":vote_counts", String.valueOf(i));
            if (cached != null) {
                counts.add(Integer.parseInt(cached));
            } else {
                PollResult pr = new PollResult();
                pr.setPollId(pollId);
                pr.setOptionIndex(i);
                // 使用 Map 构建复合主键查询
                Map<String, Object> pk = new HashMap<>();
                pk.put("poll_id", pollId);
                pk.put("option_index", i);
                PollResult fromDb = pollResultMapper.selectByMap(pk)
                        .stream().findFirst().orElse(null);
                int count = fromDb != null ? fromDb.getVoteCount() : 0;
                counts.add(count);
                // 回写缓存
                redisTemplate.opsForHash().put(
                        "poll:" + pollId + ":vote_counts",
                        String.valueOf(i), String.valueOf(count));
            }
        }
        return counts;
    }
}
