package com.voting.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.voting.entity.User;
import com.voting.mapper.UserMapper;
import com.voting.util.JwtUtil;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * 认证服务 — 钱包签名登录
 */
@Service
public class AuthService {

    private final UserMapper userMapper;
    private final RedisTemplate<String, String> redisTemplate;
    private final JwtUtil jwtUtil;
    private final SecureRandom random = new SecureRandom();

    public AuthService(UserMapper userMapper,
                       RedisTemplate<String, String> redisTemplate,
                       JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.redisTemplate = redisTemplate;
        this.jwtUtil = jwtUtil;
    }

    /**
     * 获取登录随机数 — 前端用此 nonce 让用户签名后传回
     */
    public String getNonce(String address) {
        String nonce = String.format("%06d", random.nextInt(1000000));
        // 缓存 nonce，5 分钟有效
        redisTemplate.opsForValue()
                .set("user:nonce:" + address, nonce, 5, TimeUnit.MINUTES);
        // 首次登录自动创建用户记录
        if (!exists(address)) {
            User user = new User();
            user.setWalletAddress(address);
            user.setNonce(nonce);
            userMapper.insert(user);
        }
        return nonce;
    }

    /**
     * 验证签名并返回 JWT
     * @param address   用户钱包地址
     * @param signature MetaMask personal_sign 签名结果
     * @return JWT token
     */
    public String login(String address, String signature) {
        // 1. 获取缓存的 nonce
        String nonce = redisTemplate.opsForValue().get("user:nonce:" + address);
        if (nonce == null) {
            throw new RuntimeException("nonce 已过期，请重新获取");
        }
        // 2. 构建签名原文 (与前端保持一致)
        String message = "Login to DApp Voting: " + nonce;
        // 3. 从签名恢复地址并验证
        String recovered = recoverAddress(message, signature);
        if (!recovered.equalsIgnoreCase(address)) {
            throw new RuntimeException("签名验证失败");
        }
        // 4. 删除已用 nonce (防重放)
        redisTemplate.delete("user:nonce:" + address);
        // 5. 生成 JWT
        return jwtUtil.generateToken(address);
    }

    /**
     * 从以太坊 personal_sign 签名恢复地址
     */
    private String recoverAddress(String message, String signature) {
        String prefix = "Ethereum Signed Message:\n"
                + message.length() + message;
        byte[] msgHash = org.web3j.crypto.Hash.sha3(
                prefix.getBytes(StandardCharsets.UTF_8));

        byte[] sigBytes = Numeric.hexStringToByteArray(signature);
        byte v = sigBytes[64];
        if (v < 27) v += 27;

        Sign.SignatureData sigData = new Sign.SignatureData(
                v,
                Arrays.copyOfRange(sigBytes, 0, 32),
                Arrays.copyOfRange(sigBytes, 32, 64));

        BigInteger publicKey = Sign.signedMessageHashToKey(msgHash, sigData);
        return Keys.getAddress(publicKey);
    }

    private boolean exists(String address) {
        return userMapper.selectCount(
                new LambdaQueryWrapper<User>()
                        .eq(User::getWalletAddress, address)) > 0;
    }
}
