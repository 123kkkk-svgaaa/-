package com.voting.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

/**
 * Web3j 配置 — 连接 Hardhat 本地节点
 */
@Configuration
public class Web3jConfig {

    @Value("${web3j.node-url}")
    private String nodeUrl;

    @Bean
    public Web3j web3j() {
        return Web3j.build(new HttpService(nodeUrl));
    }
}
