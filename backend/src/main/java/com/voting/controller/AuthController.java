package com.voting.controller;

import com.voting.dto.Result;
import com.voting.service.AuthService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证控制器 — 钱包签名登录
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** 获取登录 nonce */
    @PostMapping("/nonce")
    public Result<Map<String, String>> getNonce(@RequestParam String address) {
        String nonce = authService.getNonce(address);
        return Result.ok(Map.of(
                "nonce", nonce,
                "message", "Login to DApp Voting: " + nonce));
    }

    /** 验证签名登录 */
    @PostMapping("/login")
    public Result<Map<String, String>> login(@RequestParam String address,
                                              @RequestParam String sig) {
        String token = authService.login(address, sig);
        return Result.ok(Map.of("token", token));
    }
}
