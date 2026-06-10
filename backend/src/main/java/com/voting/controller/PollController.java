package com.voting.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.voting.dto.Result;
import com.voting.entity.Poll;
import com.voting.service.PollService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 投票控制器
 */
@RestController
@RequestMapping("/api/polls")
public class PollController {

    private final PollService pollService;

    public PollController(PollService pollService) {
        this.pollService = pollService;
    }

    /** 投票列表 (分页) */
    @GetMapping
    public Result<Page<Poll>> list(@RequestParam(defaultValue = "1") int pageNum,
                                    @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(pollService.listPolls(pageNum, pageSize));
    }

    /** 投票详情 + 实时票数 */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.ok(pollService.getPollDetail(id));
    }

    /** 链上同步 — 前端上链后调用 */
    @PostMapping("/sync")
    public Result<String> sync(@RequestParam Long pollId,
                                @RequestParam String txHash) {
        pollService.syncFromChain(pollId, txHash);
        return Result.ok("同步成功");
    }

    /** 链上数据验证 */
    @GetMapping("/{id}/verify")
    public Result<Map<String, Object>> verify(@PathVariable Long id) {
        return Result.ok(pollService.verifyPoll(id));
    }
}
