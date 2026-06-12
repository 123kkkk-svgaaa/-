package com.voting.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.voting.dto.Result;
import com.voting.entity.Poll;
import com.voting.service.PollService;
import com.voting.util.JwtUtil;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 投票控制器
 */
@RestController
@RequestMapping("/api/polls")
public class PollController {

    private final PollService pollService;
    private final JwtUtil jwtUtil;

    public PollController(PollService pollService, JwtUtil jwtUtil) {
        this.pollService = pollService;
        this.jwtUtil = jwtUtil;
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

    /** 链上同步 — 前端上链后调用 (需要 JWT 认证) */
    @PostMapping("/sync")
    public Result<String> sync(@RequestParam Long pollId,
                                @RequestParam String txHash,
                                @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        if (!jwtUtil.validateToken(token)) {
            return Result.error(401, "请先连接钱包登录");
        }
        pollService.syncFromChain(pollId, txHash);
        return Result.ok("同步成功");
    }

    /** 链上数据验证 */
    @GetMapping("/{id}/verify")
    public Result<Map<String, Object>> verify(@PathVariable Long id) {
        return Result.ok(pollService.verifyPoll(id));
    }
}
