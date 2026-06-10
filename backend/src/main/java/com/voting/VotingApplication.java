package com.voting;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 去中心化投票系统 — SpringBoot 启动类
 */
@SpringBootApplication
@MapperScan("com.voting.mapper")
public class VotingApplication {
    public static void main(String[] args) {
        SpringApplication.run(VotingApplication.class, args);
    }
}
