package com.voting.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.Web3ClientVersion;

/**
 * Startup health checker — verifies blockchain node connectivity on boot.
 */
@Component
public class StartupChecker {

    private static final Logger log = LoggerFactory.getLogger(StartupChecker.class);

    private final Web3j web3j;

    @Value("${web3j.contract-address}")
    private String contractAddress;

    @Value("${web3j.node-url}")
    private String nodeUrl;

    public StartupChecker(Web3j web3j) {
        this.web3j = web3j;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void check() {
        try {
            Web3ClientVersion client = web3j.web3ClientVersion().send();
            if (client.hasError()) {
                log.warn("⚠ Blockchain node ({}) returned error: {}", nodeUrl, client.getError().getMessage());
            } else {
                log.info("✓ Blockchain node connected: {} @ {}", client.getWeb3ClientVersion(), nodeUrl);
            }
            log.info("✓ Contract address: {}", contractAddress);
        } catch (Exception e) {
            log.warn("⚠ Cannot connect to blockchain node at {}: {}", nodeUrl, e.getMessage());
            log.warn("  Make sure Hardhat node is running: cd contracts && npx hardhat node");
        }
    }
}
