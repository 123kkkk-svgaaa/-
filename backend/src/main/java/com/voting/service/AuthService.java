package com.voting.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.voting.cache.InMemoryCache;
import com.voting.entity.User;
import com.voting.mapper.UserMapper;
import com.voting.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * Auth service - wallet signature login
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserMapper userMapper;
    private final InMemoryCache cache;
    private final JwtUtil jwtUtil;
    private final SecureRandom random = new SecureRandom();

    public AuthService(UserMapper userMapper,
                       InMemoryCache cache,
                       JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.cache = cache;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Generate login nonce for MetaMask signing.
     */
    public String getNonce(String address) {
        String nonce = String.format("%06d", random.nextInt(1000000));
        // Cache nonce, 5 minutes TTL
        cache.set("user:nonce:" + address, nonce, 5, TimeUnit.MINUTES);
        // Auto-create user record on first login
        if (!exists(address)) {
            User user = new User();
            user.setWalletAddress(address);
            user.setNonce(nonce);
            userMapper.insert(user);
        }
        return nonce;
    }

    /**
     * Verify signature and return JWT token.
     */
    public String login(String address, String signature) {
        // 1. Get cached nonce
        String nonce = cache.get("user:nonce:" + address);
        if (nonce == null) {
            log.warn("Login failed: nonce expired for {}", address);
            throw new RuntimeException("Nonce expired, please request a new one");
        }
        // 2. Build signed message (must match frontend)
        String message = "Login to DApp Voting: " + nonce;
        // 3. Recover address and verify
        String recovered = recoverAddress(message, signature);
        if (!recovered.equalsIgnoreCase(address)) {
            log.warn("Login failed: signature mismatch for {}", address);
            throw new RuntimeException("Signature verification failed");
        }
        // 4. Delete used nonce (anti-replay)
        cache.delete("user:nonce:" + address);
        // 5. Generate JWT
        return jwtUtil.generateToken(address);
    }

    /**
     * Recover address from Ethereum personal_sign signature.
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

        try {
            BigInteger publicKey = Sign.signedMessageHashToKey(msgHash, sigData);
            return Keys.getAddress(publicKey);
        } catch (Exception e) {
            throw new RuntimeException("Failed to recover address from signature", e);
        }
    }

    private boolean exists(String address) {
        return userMapper.selectCount(
                new LambdaQueryWrapper<User>()
                        .eq(User::getWalletAddress, address)) > 0;
    }
}
