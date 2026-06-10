package com.voting.controller;

import com.voting.dto.Result;
import com.voting.util.JwtUtil;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户控制器 — 个人信息 (需 JWT)
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final JwtUtil jwtUtil;

    public UserController(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    /** 获取当前用户的投票记录 */
    @GetMapping("/votes")
    public Result<?> getMyVotes(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        if (!jwtUtil.validateToken(token)) {
            return Result.error(401, "Token 无效或已过期");
        }
        String address = jwtUtil.getWalletAddress(token);
        return Result.ok(Map.of(
                "address", address,
                "message", "个人中心已就绪，可查询链上投票记录"));
    }
}
