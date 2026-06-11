# 去中心化投票系统 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建一个完整的去中心化投票 DApp，包含 Solidity 合约、SpringBoot 后端、Vue3 前端三层

**Architecture:** 混合模式 — 写操作通过 ethers.js + MetaMask 直接上链，读操作走后端 API（MySQL + Redis 缓存），后端 Web3j 监听合约事件同步数据

**Tech Stack:** Solidity ^0.8.x + Hardhat | SpringBoot 2.7 + MyBatis-Plus + Web3j 6.x + MySQL 8 + Redis 6 | Vue 3 + Vite + Element Plus + ECharts 5 + ethers.js 6

---

## 文件结构总览

```
dapp-voting/
├── contracts/
│   ├── package.json
│   ├── hardhat.config.js
│   ├── contracts/VotingContract.sol
│   ├── test/VotingContract.test.js
│   └── scripts/deploy.js
├── backend/
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/voting/
│       │   ├── VotingApplication.java
│       │   ├── entity/{Poll,User,PollResult}.java
│       │   ├── mapper/{Poll,User,PollResult}Mapper.java
│       │   ├── service/{Auth,Poll,Web3j}Service.java
│       │   ├── controller/{Auth,Poll,User}Controller.java
│       │   ├── config/{Web3j,Redis,Cors}Config.java
│       │   ├── listener/ContractEventListener.java
│       │   ├── util/JwtUtil.java
│       │   └── dto/{LoginRequest,PollRequest,SyncRequest,Result}.java
│       └── resources/
│           ├── application.yml
│           └── mapper/{Poll,User,PollResult}Mapper.xml
└── frontend/
    ├── package.json
    ├── vite.config.js
    ├── index.html
    └── src/
        ├── main.js
        ├── App.vue
        ├── router/index.js
        ├── api/{index,auth,poll}.js
        ├── utils/{contract,wallet}.js
        ├── stores/user.js
        ├── views/{PollList,PollDetail,CreatePoll,VerifyPoll,Profile}.vue
        └── components/{NavBar,PollCard,VoteChart}.vue
```

---

## Phase 1: 智能合约层 (完全独立，可自行测试)

### Task 1.1: 初始化 Hardhat 项目

**Files:**
- Create: `contracts/package.json`
- Create: `contracts/hardhat.config.js`

- [ ] **Step 1: 创建 contracts/package.json**

```json
{
  "name": "dapp-voting-contracts",
  "version": "1.0.0",
  "devDependencies": {
    "@nomicfoundation/hardhat-toolbox": "^3.0.0",
    "hardhat": "^2.17.0"
  }
}
```

- [ ] **Step 2: 创建 contracts/hardhat.config.js**

```js
require("@nomicfoundation/hardhat-toolbox");

/** @type import('hardhat/config').HardhatUserConfig */
module.exports = {
  solidity: "0.8.20",
  networks: {
    localhost: {
      url: "http://127.0.0.1:8545"
    }
  }
};
```

- [ ] **Step 3: 安装依赖**

```bash
cd contracts && npm install
```

- [ ] **Step 4: Commit**

```bash
git add contracts/package.json contracts/hardhat.config.js contracts/package-lock.json
git commit -m "feat: init Hardhat project with solidity 0.8.20"
```

---

### Task 1.2: 编写 VotingContract.sol

**Files:**
- Create: `contracts/contracts/VotingContract.sol`

- [ ] **Step 1: 编写合约代码**

```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

/**
 * @title 去中心化投票合约
 * @notice 所有投票数据上链存储，不可篡改，公开可验证
 */
contract VotingContract {

    // ============ 数据结构 ============

    struct Poll {
        uint256 id;
        address creator;
        string title;
        string description;
        string[] options;
        uint256 startTime;
        uint256 endTime;
    }

    // ============ 状态变量 ============

    Poll[] private s_polls;

    // pollId => (voter => hasVoted)
    mapping(uint256 => mapping(address => bool)) private s_hasVoted;

    // pollId => (optionIndex => count)
    mapping(uint256 => mapping(uint256 => uint256)) private s_voteCounts;

    uint256 private s_pollCount;

    // ============ 事件 ============

    event PollCreated(
        uint256 indexed pollId,
        address indexed creator,
        uint256 endTime
    );

    event VoteCasted(
        uint256 indexed pollId,
        address indexed voter,
        uint256 optionIndex
    );

    // ============ 核心函数 ============

    /**
     * @notice 创建新的投票
     * @param _title       投票标题
     * @param _description 投票描述
     * @param _options     选项列表 (至少 2 个)
     * @param _duration    投票持续时间 (秒)
     * @return pollId      新创建的投票 ID
     */
    function createPoll(
        string memory _title,
        string memory _description,
        string[] memory _options,
        uint256 _duration
    ) external returns (uint256) {
        require(bytes(_title).length > 0, "Poll: title cannot be empty");
        require(_options.length >= 2, "Poll: at least 2 options");
        require(_duration > 0, "Poll: duration must be > 0");

        uint256 pollId = s_pollCount;
        uint256 startTime = block.timestamp;
        uint256 endTime = startTime + _duration;

        Poll storage newPoll = s_polls.push();
        newPoll.id = pollId;
        newPoll.creator = msg.sender;
        newPoll.title = _title;
        newPoll.description = _description;
        newPoll.options = _options;
        newPoll.startTime = startTime;
        newPoll.endTime = endTime;

        s_pollCount++;

        emit PollCreated(pollId, msg.sender, endTime);

        return pollId;
    }

    /**
     * @notice 为指定投票的选项投票
     * @param _pollId      投票 ID
     * @param _optionIndex 选项索引 (从 0 开始)
     */
    function vote(uint256 _pollId, uint256 _optionIndex) external {
        require(_pollId < s_polls.length, "Poll: poll does not exist");
        require(!s_hasVoted[_pollId][msg.sender], "Poll: already voted");

        Poll storage poll = s_polls[_pollId];
        require(
            block.timestamp >= poll.startTime && block.timestamp <= poll.endTime,
            "Poll: not in voting period"
        );
        require(_optionIndex < poll.options.length, "Poll: invalid option");

        s_hasVoted[_pollId][msg.sender] = true;
        s_voteCounts[_pollId][_optionIndex]++;

        emit VoteCasted(_pollId, msg.sender, _optionIndex);
    }

    // ============ 查询函数 ============

    /**
     * @notice 获取投票基本信息 (不含 mapping 字段)
     */
    function getPollInfo(
        uint256 _pollId
    )
        external
        view
        returns (
            uint256 id,
            address creator,
            string memory title,
            string memory description,
            string[] memory options,
            uint256 startTime,
            uint256 endTime
        )
    {
        require(_pollId < s_polls.length, "Poll: poll does not exist");
        Poll storage poll = s_polls[_pollId];
        return (
            poll.id,
            poll.creator,
            poll.title,
            poll.description,
            poll.options,
            poll.startTime,
            poll.endTime
        );
    }

    /**
     * @notice 获取指定投票的票数分布
     */
    function getVoteCounts(
        uint256 _pollId
    ) external view returns (uint256[] memory) {
        require(_pollId < s_polls.length, "Poll: poll does not exist");
        uint256 optLen = s_polls[_pollId].options.length;
        uint256[] memory counts = new uint256[](optLen);
        for (uint256 i = 0; i < optLen; i++) {
            counts[i] = s_voteCounts[_pollId][i];
        }
        return counts;
    }

    /**
     * @notice 查询某地址是否已投票
     */
    function getHasVoted(
        uint256 _pollId,
        address _voter
    ) external view returns (bool) {
        return s_hasVoted[_pollId][_voter];
    }

    /**
     * @notice 获取投票总数
     */
    function getPollCount() external view returns (uint256) {
        return s_pollCount;
    }

    /**
     * @notice 判断投票是否活跃 (在投票期内)
     */
    function isPollActive(uint256 _pollId) external view returns (bool) {
        require(_pollId < s_polls.length, "Poll: poll does not exist");
        Poll storage poll = s_polls[_pollId];
        return block.timestamp >= poll.startTime
            && block.timestamp <= poll.endTime;
    }
}
```

- [ ] **Step 2: 编译合约**

```bash
cd contracts && npx hardhat compile
```

- [ ] **Step 3: Commit**

```bash
git add contracts/contracts/VotingContract.sol
git commit -m "feat: add VotingContract with createPoll/vote/getPollInfo"
```

---

### Task 1.3: 编写合约测试

**Files:**
- Create: `contracts/test/VotingContract.test.js`

- [ ] **Step 1: 编写测试文件**

```js
const { expect } = require("chai");
const { ethers } = require("hardhat");

describe("VotingContract", function () {
  let contract, owner, addr1, addr2;

  beforeEach(async () => {
    [owner, addr1, addr2] = await ethers.getSigners();
    const VotingContract = await ethers.getContractFactory("VotingContract");
    contract = await VotingContract.deploy();
  });

  describe("createPoll", () => {
    it("应该成功创建投票并返回正确 ID", async () => {
      const tx = await contract.createPoll(
        "测试投票",
        "这是一个测试",
        ["选项A", "选项B", "选项C"],
        3600 // 1小时
      );
      const receipt = await tx.wait();

      // 验证事件
      await expect(tx).to.emit(contract, "PollCreated").withArgs(0, owner.address, await getBlockTimestamp() + 3600);

      // 验证返回值 通过 getPollCount
      expect(await contract.getPollCount()).to.equal(1);
    });

    it("标题为空时应该 revert", async () => {
      await expect(
        contract.createPoll("", "描述", ["A", "B"], 3600)
      ).to.be.revertedWith("Poll: title cannot be empty");
    });

    it("选项少于 2 个时应该 revert", async () => {
      await expect(
        contract.createPoll("标题", "描述", ["A"], 3600)
      ).to.be.revertedWith("Poll: at least 2 options");
    });

    it("duration 为 0 时应该 revert", async () => {
      await expect(
        contract.createPoll("标题", "描述", ["A", "B"], 0)
      ).to.be.revertedWith("Poll: duration must be > 0");
    });
  });

  describe("vote", () => {
    beforeEach(async () => {
      await contract.createPoll("投票", "描述", ["A", "B"], 3600);
    });

    it("应该成功投票", async () => {
      await expect(contract.connect(addr1).vote(0, 1))
        .to.emit(contract, "VoteCasted")
        .withArgs(0, addr1.address, 1);

      const counts = await contract.getVoteCounts(0);
      expect(counts[1]).to.equal(1);
    });

    it("重复投票应该 revert", async () => {
      await contract.connect(addr1).vote(0, 0);
      await expect(
        contract.connect(addr1).vote(0, 1)
      ).to.be.revertedWith("Poll: already voted");
    });

    it("投票不存在的 poll 应该 revert", async () => {
      await expect(
        contract.vote(99, 0)
      ).to.be.revertedWith("Poll: poll does not exist");
    });

    it("选项越界应该 revert", async () => {
      await expect(
        contract.connect(addr1).vote(0, 5)
      ).to.be.revertedWith("Poll: invalid option");
    });
  });

  describe("getPollInfo", () => {
    it("应该返回正确的投票信息", async () => {
      await contract.createPoll("标题", "描述", ["X", "Y", "Z"], 7200);
      const info = await contract.getPollInfo(0);

      expect(info.title).to.equal("标题");
      expect(info.description).to.equal("描述");
      expect(info.options).to.deep.equal(["X", "Y", "Z"]);
      expect(info.creator).to.equal(owner.address);
    });
  });

  describe("getHasVoted", () => {
    it("未投票返回 false，已投票返回 true", async () => {
      await contract.createPoll("投票", "描述", ["A", "B"], 3600);
      expect(await contract.getHasVoted(0, addr1.address)).to.equal(false);

      await contract.connect(addr1).vote(0, 0);
      expect(await contract.getHasVoted(0, addr1.address)).to.equal(true);
    });
  });
});

// 辅助函数：获取当前区块时间戳
async function getBlockTimestamp() {
  const block = await ethers.provider.getBlock("latest");
  return block.timestamp;
}
```

- [ ] **Step 2: 启动本地节点并运行测试**

```bash
# 终端1: 启动 Hardhat 本地节点
cd contracts && npx hardhat node

# 终端2: 运行测试
cd contracts && npx hardhat test --network localhost
```

- [ ] **Step 3: 验证所有测试通过**

预期输出: 所有 10 个测试用例 PASS

- [ ] **Step 4: Commit**

```bash
git add contracts/test/VotingContract.test.js
git commit -m "test: add VotingContract unit tests (10 cases)"
```

---

### Task 1.4: 编写部署脚本

**Files:**
- Create: `contracts/scripts/deploy.js`

- [ ] **Step 1: 编写部署脚本**

```js
const hre = require("hardhat");

async function main() {
  const VotingContract = await hre.ethers.getContractFactory("VotingContract");
  const contract = await VotingContract.deploy();

  await contract.waitForDeployment();
  const address = await contract.getAddress();

  console.log("VotingContract deployed to:", address);
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
```

- [ ] **Step 2: 部署到本地节点**

```bash
cd contracts && npx hardhat run scripts/deploy.js --network localhost
```

预期: 输出合约地址，记下这个地址供后端/前端配置使用

- [ ] **Step 3: 导出 ABI**

```bash
cd contracts && cp artifacts/contracts/VotingContract.sol/VotingContract.json ../backend/src/main/resources/abi/
```

- [ ] **Step 4: Commit**

```bash
git add contracts/scripts/deploy.js
git commit -m "feat: add deploy script"
```

---

## Phase 2: SpringBoot 后端

### Task 2.1: 初始化 SpringBoot 项目

**Files:**
- Create: `backend/pom.xml`
- Create: `backend/src/main/java/com/voting/VotingApplication.java`
- Create: `backend/src/main/resources/application.yml`

- [ ] **Step 1: 创建 backend/pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.7.18</version>
    </parent>

    <groupId>com.voting</groupId>
    <artifactId>dapp-voting-backend</artifactId>
    <version>1.0.0</version>

    <properties>
        <java.version>11</java.version>
    </properties>

    <dependencies>
        <!-- SpringBoot Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- MyBatis-Plus -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-boot-starter</artifactId>
            <version>3.5.3.1</version>
        </dependency>

        <!-- MySQL -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
        </dependency>

        <!-- Redis -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>

        <!-- Web3j -->
        <dependency>
            <groupId>org.web3j</groupId>
            <artifactId>core</artifactId>
            <version>4.10.0</version>
        </dependency>

        <!-- JWT -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>0.11.5</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>0.11.5</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>0.11.5</version>
            <scope>runtime</scope>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2: 创建启动类 VotingApplication.java**

```java
package com.voting;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.voting.mapper")
public class VotingApplication {
    public static void main(String[] args) {
        SpringApplication.run(VotingApplication.class, args);
    }
}
```

- [ ] **Step 3: 创建 application.yml**

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/dapp_voting?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf-8
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver
  redis:
    host: localhost
    port: 6379

mybatis-plus:
  mapper-locations: classpath:mapper/*.xml
  configuration:
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      id-type: auto

# Web3j 配置
web3j:
  node-url: http://localhost:8545
  contract-address: ""  # 部署后填入

# JWT 配置
jwt:
  secret: dapp-voting-secret-key-change-in-production-2026
  expiration: 86400000  # 24小时

# 日志
logging:
  level:
    com.voting: debug
```

- [ ] **Step 4: 创建 ABI 目录**

```bash
mkdir -p backend/src/main/resources/abi
```

- [ ] **Step 5: Commit**

```bash
git add backend/pom.xml backend/src/main/java/com/voting/VotingApplication.java backend/src/main/resources/application.yml
git commit -m "feat: init SpringBoot project with MyBatis-Plus, Web3j, Redis, JWT"
```

---

### Task 2.2: 创建 Entity 实体类

**Files:**
- Create: `backend/src/main/java/com/voting/entity/Poll.java`
- Create: `backend/src/main/java/com/voting/entity/User.java`
- Create: `backend/src/main/java/com/voting/entity/PollResult.java`

- [ ] **Step 1: 创建 Poll.java**

```java
package com.voting.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 投票实体 — 对应链上 Poll 结构
 */
@Data
@TableName(value = "polls", autoResultMap = true)
public class Poll {

    @TableId(type = IdType.INPUT)  // 使用链上的 pollId 作为主键
    private Long id;

    private String creatorAddress;

    private String title;

    private String description;

    /**
     * 选项列表，使用 JSON 存储 — ["张三","李四","王五"]
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> options;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    /** 创建投票的链上交易哈希 */
    private String txHash;

    private LocalDateTime createdAt;
}
```

- [ ] **Step 2: 创建 User.java**

```java
package com.voting.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体 — 仅存钱包地址和登录 nonce，不存实名信息
 */
@Data
@TableName("users")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 钱包地址 (唯一) */
    private String walletAddress;

    /** 登录签名随机数 */
    private String nonce;

    private LocalDateTime createdAt;
}
```

- [ ] **Step 3: 创建 PollResult.java**

```java
package com.voting.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 投票结果实体 — 每个选项的得票数快照
 */
@Data
@TableName("poll_results")
public class PollResult {

    private Long pollId;

    private Integer optionIndex;

    private Integer voteCount;

    private LocalDateTime updatedAt;
}
```

- [ ] **Step 4: 创建数据库初始化 SQL（手动执行）**

```sql
CREATE DATABASE IF NOT EXISTS dapp_voting DEFAULT CHARSET utf8mb4;

USE dapp_voting;

CREATE TABLE IF NOT EXISTS polls (
    id BIGINT PRIMARY KEY,
    creator_address VARCHAR(42) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    options JSON NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    tx_hash VARCHAR(66),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_creator (creator_address),
    INDEX idx_end_time (end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS poll_results (
    poll_id BIGINT NOT NULL,
    option_index INT NOT NULL,
    vote_count INT DEFAULT 0,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (poll_id, option_index),
    INDEX idx_poll_id (poll_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    wallet_address VARCHAR(42) NOT NULL UNIQUE,
    nonce VARCHAR(64),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX idx_wallet (wallet_address)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/voting/entity/
git commit -m "feat: add entity classes — Poll, User, PollResult"
```

---

### Task 2.3: 创建 Mapper 层

**Files:**
- Create: `backend/src/main/java/com/voting/mapper/PollMapper.java`
- Create: `backend/src/main/java/com/voting/mapper/UserMapper.java`
- Create: `backend/src/main/java/com/voting/mapper/PollResultMapper.java`
- Create: `backend/src/main/resources/mapper/PollMapper.xml`
- Create: `backend/src/main/resources/mapper/UserMapper.xml`
- Create: `backend/src/main/resources/mapper/PollResultMapper.xml`

- [ ] **Step 1: 创建 PollMapper.java**

```java
package com.voting.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.voting.entity.Poll;
import org.apache.ibatis.annotations.Mapper;

/**
 * 投票 Mapper — 继承 MyBatis-Plus BaseMapper，自动获得 CRUD 能力
 */
@Mapper
public interface PollMapper extends BaseMapper<Poll> {
}
```

- [ ] **Step 2: 创建 UserMapper.java**

```java
package com.voting.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.voting.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
```

- [ ] **Step 3: 创建 PollResultMapper.java**

```java
package com.voting.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.voting.entity.PollResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 投票结果 Mapper — 包含自定义 upsert 操作
 */
@Mapper
public interface PollResultMapper extends BaseMapper<PollResult> {

    /**
     * 插入或更新票数 (ON DUPLICATE KEY UPDATE)
     */
    @Update("INSERT INTO poll_results (poll_id, option_index, vote_count) " +
            "VALUES (#{pollId}, #{optionIndex}, #{voteCount}) " +
            "ON DUPLICATE KEY UPDATE vote_count = #{voteCount}, updated_at = NOW()")
    int upsertVoteCount(@Param("pollId") Long pollId,
                        @Param("optionIndex") Integer optionIndex,
                        @Param("voteCount") Integer voteCount);
}
```

- [ ] **Step 4: 创建 PollMapper.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.voting.mapper.PollMapper">

    <resultMap id="pollResultMap" type="com.voting.entity.Poll">
        <id column="id" property="id"/>
        <result column="creator_address" property="creatorAddress"/>
        <result column="title" property="title"/>
        <result column="description" property="description"/>
        <result column="options" property="options"
                typeHandler="com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler"/>
        <result column="start_time" property="startTime"/>
        <result column="end_time" property="endTime"/>
        <result column="tx_hash" property="txHash"/>
        <result column="created_at" property="createdAt"/>
    </resultMap>
</mapper>
```

- [ ] **Step 5: 创建 UserMapper.xml** (空壳，保持结构一致)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.voting.mapper.UserMapper">
</mapper>
```

- [ ] **Step 6: 创建 PollResultMapper.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.voting.mapper.PollResultMapper">
</mapper>
```

- [ ] **Step 7: Commit**

---

### Task 2.4: 创建工具类与配置

**Files:**
- Create: `backend/src/main/java/com/voting/util/JwtUtil.java`
- Create: `backend/src/main/java/com/voting/config/Web3jConfig.java`
- Create: `backend/src/main/java/com/voting/config/RedisConfig.java`
- Create: `backend/src/main/java/com/voting/config/CorsConfig.java`

- [ ] **Step 1: 创建 JwtUtil.java**

```java
package com.voting.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类 — 生成和验证钱包登录 Token
 */
@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expiration;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expiration}") long expiration) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    /**
     * 生成 JWT — subject 为钱包地址
     */
    public String generateToken(String walletAddress) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(walletAddress)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expiration))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 从 Token 中提取钱包地址
     */
    public String getWalletAddress(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * 验证 Token 是否有效
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
```

- [ ] **Step 2: 创建 Web3jConfig.java**

```java
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
```

- [ ] **Step 3: 创建 RedisConfig.java**

```java
package com.voting.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置 — key/value 统一用 String 序列化
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, String> redisTemplate(
            RedisConnectionFactory factory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        return template;
    }
}
```

- [ ] **Step 4: 创建 CorsConfig.java**

```java
package com.voting.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * 跨域配置 — 允许前端 localhost:5173 访问
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOriginPattern("*");
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
```

- [ ] **Step 5: Commit**

---

### Task 2.5: 创建 DTO 类

**Files:**
- Create: `backend/src/main/java/com/voting/dto/LoginRequest.java`
- Create: `backend/src/main/java/com/voting/dto/PollRequest.java`
- Create: `backend/src/main/java/com/voting/dto/SyncRequest.java`
- Create: `backend/src/main/java/com/voting/dto/Result.java`

- [ ] **Step 1: 创建 LoginRequest.java**

```java
package com.voting.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String address;
    private String signature;
}
```

- [ ] **Step 2: 创建 PollRequest.java**

```java
package com.voting.dto;

import lombok.Data;
import java.util.List;

@Data
public class PollRequest {
    private String title;
    private String description;
    private List<String> options;
    private Long duration;  // 秒
    private String txHash;  // 上链后传入
}
```

- [ ] **Step 3: 创建 SyncRequest.java**

```java
package com.voting.dto;

import lombok.Data;

@Data
public class SyncRequest {
    private String txHash;
}
```

- [ ] **Step 4: 创建 Result.java (统一响应)**

```java
package com.voting.dto;

import lombok.Data;

/**
 * 统一 API 响应格式
 */
@Data
public class Result<T> {
    private int code;
    private String message;
    private T data;

    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.code = 200;
        r.message = "success";
        r.data = data;
        return r;
    }

    public static <T> Result<T> error(int code, String message) {
        Result<T> r = new Result<>();
        r.code = code;
        r.message = message;
        return r;
    }
}
```

- [ ] **Step 5: Commit**

---

### Task 2.6: 编写 Service 层

**Files:**
- Create: `backend/src/main/java/com/voting/service/AuthService.java`
- Create: `backend/src/main/java/com/voting/service/PollService.java`
- Create: `backend/src/main/java/com/voting/service/Web3jService.java`

- [ ] **Step 1: 创建 AuthService.java**

```java
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
import java.security.SecureRandom;
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
     * 获取登录随机数 — 前端用此 nonce 让用户签名
     */
    public String getNonce(String address) {
        String nonce = String.format("%06d", random.nextInt(1000000));
        redisTemplate.opsForValue()
                .set("user:nonce:" + address, nonce, 5, TimeUnit.MINUTES);
        // 首次登录自动创建用户
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
     * 签名内容为: "Login to DApp Voting: {nonce}"
     */
    public String login(String address, String signature) {
        // 1. 获取缓存的 nonce
        String nonce = redisTemplate.opsForValue()
                .get("user:nonce:" + address);
        if (nonce == null) {
            throw new RuntimeException("nonce 已过期，请重新获取");
        }

        // 2. 构建原始消息
        String message = "Login to DApp Voting: " + nonce;

        // 3. 从签名恢复地址并验证
        String recoveredAddress = recoverAddress(message, signature);
        if (!recoveredAddress.equalsIgnoreCase(address)) {
            throw new RuntimeException("签名验证失败");
        }

        // 4. 删除已用 nonce (防重放)
        redisTemplate.delete("user:nonce:" + address);

        // 5. 生成 JWT
        return jwtUtil.generateToken(address);
    }

    /**
     * 从以太坊签名恢复地址
     */
    private String recoverAddress(String message, String signature) {
        String prefix = "Ethereum Signed Message:\n"
                + message.length() + message;
        byte[] msgHash = org.web3j.crypto.Hash.sha3(
                prefix.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        byte[] sigBytes = Numeric.hexStringToByteArray(signature);
        byte v = sigBytes[64];
        if (v < 27) v += 27;

        Sign.SignatureData sigData = new Sign.SignatureData(
                v, java.util.Arrays.copyOfRange(sigBytes, 0, 32),
                java.util.Arrays.copyOfRange(sigBytes, 32, 64));

        BigInteger publicKey = Sign.signedMessageHashToKey(msgHash, sigData);
        return Keys.getAddress(publicKey);
    }

    private boolean exists(String address) {
        return userMapper.selectCount(
                new LambdaQueryWrapper<User>()
                        .eq(User::getWalletAddress, address)) > 0;
    }
}
```

- [ ] **Step 2: 创建 PollService.java**

```java
package com.voting.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.voting.entity.Poll;
import com.voting.entity.PollResult;
import com.voting.mapper.PollMapper;
import com.voting.mapper.PollResultMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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

        // 先从 Redis 获取实时票数，miss 则查 MySQL
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
    public void syncFromChain(Long pollId, String txHash) {
        // 从链上获取投票信息
        Map<String, Object> chainData = web3jService.getPollFromChain(pollId);

        // 更新 MySQL
        Poll poll = new Poll();
        poll.setId(pollId);
        poll.setTxHash(txHash);
        pollMapper.updateById(poll);

        // 同步票数
        @SuppressWarnings("unchecked")
        List<Long> chainCounts = (List<Long>) chainData.get("voteCounts");
        for (int i = 0; i < chainCounts.size(); i++) {
            pollResultMapper.upsertVoteCount(pollId, i, chainCounts.get(i).intValue());
            // 同步到 Redis
            redisTemplate.opsForHash().put(
                    "poll:" + pollId + ":vote_counts",
                    String.valueOf(i),
                    String.valueOf(chainCounts.get(i)));
        }
    }

    /**
     * 链上验证 — 对比链上数据和本地数据库
     */
    public Map<String, Object> verifyPoll(Long pollId) {
        Poll localPoll = pollMapper.selectById(pollId);
        Map<String, Object> chainData = web3jService.getPollFromChain(pollId);

        Map<String, Object> result = new HashMap<>();
        result.put("chain", chainData);
        result.put("local", localPoll);
        result.put("consistent", compareWithChain(localPoll, chainData));
        return result;
    }

    private List<Integer> getVoteCounts(Long pollId, int optionCount) {
        List<Integer> counts = new ArrayList<>();
        for (int i = 0; i < optionCount; i++) {
            String cached = (String) redisTemplate.opsForHash()
                    .get("poll:" + pollId + ":vote_counts", String.valueOf(i));
            if (cached != null) {
                counts.add(Integer.parseInt(cached));
            } else {
                // 从 MySQL 查
                PollResult pr = pollResultMapper.selectById(
                        new AbstractMap.SimpleEntry<>(pollId, i));
                int count = pr != null ? pr.getVoteCount() : 0;
                counts.add(count);
                redisTemplate.opsForHash().put(
                        "poll:" + pollId + ":vote_counts",
                        String.valueOf(i), String.valueOf(count));
            }
        }
        return counts;
    }

    private boolean compareWithChain(Poll local, Map<String, Object> chain) {
        return local != null
                && chain.get("title") != null
                && chain.get("title").equals(local.getTitle());
    }
}
```

- [ ] **Step 3: 创建 Web3jService.java**

```java
package com.voting.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;

import javax.annotation.PostConstruct;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Web3j 服务 — 与链上合约交互
 */
@Service
public class Web3jService {

    private final Web3j web3j;

    @Value("${web3j.contract-address}")
    private String contractAddress;

    public Web3jService(Web3j web3j) {
        this.web3j = web3j;
    }

    /**
     * 从链上读取投票信息
     */
    public Map<String, Object> getPollFromChain(Long pollId) {
        // 构建 getPollInfo 调用
        Function function = new Function(
                "getPollInfo",
                Collections.singletonList(new Uint256(pollId)),
                Arrays.asList(
                        new TypeReference<Uint256>() {},
                        new TypeReference<Address>() {},
                        new TypeReference<Utf8String>() {},
                        new TypeReference<Utf8String>() {},
                        new TypeReference<DynamicArray<Utf8String>>() {},
                        new TypeReference<Uint256>() {},
                        new TypeReference<Uint256>() {}
                ));

        List<Type> result = callContract(function);
        if (result == null || result.isEmpty()) {
            throw new RuntimeException("链上查询失败");
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", ((Uint256) result.get(0)).getValue().longValue());
        data.put("creator", ((Address) result.get(1)).getValue());
        data.put("title", ((Utf8String) result.get(2)).getValue());
        data.put("description", ((Utf8String) result.get(3)).getValue());
        @SuppressWarnings("unchecked")
        List<Utf8String> opts = ((DynamicArray<Utf8String>) result.get(4)).getValue();
        data.put("options", opts.stream().map(Utf8String::getValue).collect(Collectors.toList()));
        data.put("startTime", ((Uint256) result.get(5)).getValue().longValue());
        data.put("endTime", ((Uint256) result.get(6)).getValue().longValue());

        // 同时获取票数
        data.put("voteCounts", getVoteCountsFromChain(pollId));

        return data;
    }

    /**
     * 获取指定投票的票数分布
     */
    private List<Long> getVoteCountsFromChain(Long pollId) {
        Function function = new Function(
                "getVoteCounts",
                Collections.singletonList(new Uint256(pollId)),
                Collections.singletonList(
                        new TypeReference<DynamicArray<Uint256>>() {}));

        List<Type> result = callContract(function);
        if (result == null || result.isEmpty()) {
            return Collections.emptyList();
        }

        @SuppressWarnings("unchecked")
        List<Uint256> counts = ((DynamicArray<Uint256>) result.get(0)).getValue();
        return counts.stream().map(c -> c.getValue().longValue()).collect(Collectors.toList());
    }

    /**
     * 执行只读合约调用
     */
    private List<Type> callContract(Function function) {
        try {
            String encoded = FunctionEncoder.encode(function);
            Transaction tx = Transaction.createEthCallTransaction(
                    "0x0000000000000000000000000000000000000000",
                    contractAddress, encoded);

            EthCall response = web3j.ethCall(tx, DefaultBlockParameterName.LATEST).send();
            if (response.hasError()) {
                return null;
            }
            return FunctionReturnDecoder.decode(
                    response.getValue(), function.getOutputParameters());
        } catch (Exception e) {
            throw new RuntimeException("合约调用失败: " + e.getMessage(), e);
        }
    }
}
```

- [ ] **Step 4: Commit**

---

### Task 2.7: 创建事件监听器

**Files:**
- Create: `backend/src/main/java/com/voting/listener/ContractEventListener.java`

- [ ] **Step 1: 编写事件监听器**

```java
package com.voting.listener;

import com.voting.entity.Poll;
import com.voting.mapper.PollMapper;
import com.voting.mapper.PollResultMapper;
import com.voting.service.Web3jService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.Log;

import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * 合约事件监听器 — 启动后异步监听 PollCreated / VoteCasted 事件，自动同步 MySQL + Redis
 */
@Component
public class ContractEventListener {

    private static final Logger log = LoggerFactory.getLogger(ContractEventListener.class);

    private final Web3j web3j;
    private final PollMapper pollMapper;
    private final PollResultMapper pollResultMapper;
    private final RedisTemplate<String, String> redisTemplate;
    private final Web3jService web3jService;

    @Value("${web3j.contract-address}")
    private String contractAddress;

    // 事件定义
    private static final Event POLL_CREATED = new Event(
            "PollCreated",
            Arrays.asList(
                    new TypeReference<Uint256>(true) {},
                    new TypeReference<Address>(true) {},
                    new TypeReference<Uint256>() {}));

    private static final Event VOTE_CASTED = new Event(
            "VoteCasted",
            Arrays.asList(
                    new TypeReference<Uint256>(true) {},
                    new TypeReference<Address>(true) {},
                    new TypeReference<Uint256>() {}));

    public ContractEventListener(Web3j web3j, PollMapper pollMapper,
                                  PollResultMapper pollResultMapper,
                                  RedisTemplate<String, String> redisTemplate,
                                  Web3jService web3jService) {
        this.web3j = web3j;
        this.pollMapper = pollMapper;
        this.pollResultMapper = pollResultMapper;
        this.redisTemplate = redisTemplate;
        this.web3jService = web3jService;
    }

    /**
     * 应用启动后异步注册事件监听
     */
    @EventListener(ApplicationReadyEvent.class)
    public void startListening() {
        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                // 监听 PollCreated 事件
                EthFilter filter = new EthFilter(
                        DefaultBlockParameterName.EARLIEST,
                        DefaultBlockParameterName.LATEST,
                        contractAddress);
                filter.addSingleTopic(EventEncoder.encode(POLL_CREATED));
                filter.addSingleTopic(EventEncoder.encode(VOTE_CASTED));

                web3j.ethLogFlowable(filter).subscribe(
                        this::handleEvent,
                        error -> log.error("事件监听错误: {}", error.getMessage()));
                log.info("合约事件监听已启动, 合约地址: {}", contractAddress);
            } catch (Exception e) {
                log.error("启动事件监听失败: {}", e.getMessage(), e);
            }
        });
    }

    private void handleEvent(Log logEvent) {
        try {
            String topic = logEvent.getTopics().get(0);
            if (topic.equals(EventEncoder.encode(POLL_CREATED))) {
                handlePollCreated(logEvent);
            } else if (topic.equals(EventEncoder.encode(VOTE_CASTED))) {
                handleVoteCasted(logEvent);
            }
        } catch (Exception e) {
            log.error("处理事件失败: {}", e.getMessage());
        }
    }

    /**
     * 处理投票创建事件 — 从链上同步到 MySQL
     */
    private void handlePollCreated(Log logEvent) {
        BigInteger pollId = new BigInteger(
                logEvent.getTopics().get(1).substring(2), 16);

        // 从链上获取完整数据
        Map<String, Object> chainData = web3jService.getPollFromChain(pollId.longValue());

        Poll poll = new Poll();
        poll.setId(pollId.longValue());
        poll.setCreatorAddress((String) chainData.get("creator"));
        poll.setTitle((String) chainData.get("title"));
        poll.setDescription((String) chainData.get("description"));
        @SuppressWarnings("unchecked")
        List<String> options = (List<String>) chainData.get("options");
        poll.setOptions(options);
        poll.setStartTime(toLocalDateTime((Long) chainData.get("startTime")));
        poll.setEndTime(toLocalDateTime((Long) chainData.get("endTime")));
        poll.setTxHash(logEvent.getTransactionHash());

        pollMapper.insert(poll);
        log.info("同步投票创建: pollId={}, title={}", pollId, poll.getTitle());
    }

    /**
     * 处理投票事件 — 更新 Redis 缓存
     */
    private void handleVoteCasted(Log logEvent) {
        BigInteger pollId = new BigInteger(
                logEvent.getTopics().get(1).substring(2), 16);
        BigInteger optionIndex = new BigInteger(
                logEvent.getTopics().get(3).substring(2), 16);

        String key = "poll:" + pollId + ":vote_counts";
        String field = optionIndex.toString();

        // 原子自增
        redisTemplate.opsForHash().increment(key, field, 1);
        // 记录投票者
        String voter = "0x" + logEvent.getTopics().get(2).substring(26);
        redisTemplate.opsForSet().add("poll:" + pollId + ":voters", voter);

        // 异步同步到 MySQL
        String newCount = (String) redisTemplate.opsForHash().get(key, field);
        pollResultMapper.upsertVoteCount(
                pollId.longValue(), optionIndex.intValue(),
                Integer.parseInt(newCount != null ? newCount : "0"));

        log.info("同步投票: pollId={}, option={}, count={}", pollId, optionIndex, newCount);
    }

    private LocalDateTime toLocalDateTime(Long epochSecond) {
        return LocalDateTime.ofInstant(
                Instant.ofEpochSecond(epochSecond), ZoneId.of("Asia/Shanghai"));
    }
}
```

- [ ] **Step 2: Commit**

---

### Task 2.8: 创建 Controller 层

**Files:**
- Create: `backend/src/main/java/com/voting/controller/AuthController.java`
- Create: `backend/src/main/java/com/voting/controller/PollController.java`
- Create: `backend/src/main/java/com/voting/controller/UserController.java`

- [ ] **Step 1: 创建 AuthController.java**

```java
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

    /**
     * 获取登录 nonce
     */
    @PostMapping("/nonce")
    public Result<Map<String, String>> getNonce(@RequestParam String address) {
        String nonce = authService.getNonce(address);
        return Result.ok(Map.of("nonce", nonce,
                "message", "Login to DApp Voting: " + nonce));
    }

    /**
     * 验证签名登录
     */
    @PostMapping("/login")
    public Result<Map<String, String>> login(@RequestParam String address,
                                              @RequestParam String sig) {
        String token = authService.login(address, sig);
        return Result.ok(Map.of("token", token));
    }
}
```

- [ ] **Step 2: 创建 PollController.java**

```java
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

    /**
     * 投票列表 (分页)
     */
    @GetMapping
    public Result<Page<Poll>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(pollService.listPolls(pageNum, pageSize));
    }

    /**
     * 投票详情 + 实时票数
     */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.ok(pollService.getPollDetail(id));
    }

    /**
     * 链上同步 — 前端上链后调用
     */
    @PostMapping("/sync")
    public Result<String> sync(@RequestParam Long pollId,
                                @RequestParam String txHash) {
        pollService.syncFromChain(pollId, txHash);
        return Result.ok("同步成功");
    }

    /**
     * 链上数据验证 — 对比链上和本地
     */
    @GetMapping("/{id}/verify")
    public Result<Map<String, Object>> verify(@PathVariable Long id) {
        return Result.ok(pollService.verifyPoll(id));
    }
}
```

- [ ] **Step 3: 创建 UserController.java**

```java
package com.voting.controller;

import com.voting.dto.Result;
import com.voting.util.JwtUtil;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户控制器 — 个人信息
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final JwtUtil jwtUtil;

    public UserController(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    /**
     * 获取当前用户信息 (需 JWT)
     */
    @GetMapping("/votes")
    public Result<?> getMyVotes(
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        if (!jwtUtil.validateToken(token)) {
            return Result.error(401, "Token 无效或已过期");
        }
        String address = jwtUtil.getWalletAddress(token);
        // TODO: 从数据库查询该地址的投票记录
        return Result.ok(Map.of("address", address,
                "message", "此功能需要前端传入具体 pollId 后查询链上 hasVoted"));
    }
}
```

- [ ] **Step 4: Commit**

---

## Phase 3: Vue 3 前端

### Task 3.1: 初始化 Vite + Vue 3 项目

**Files:**
- Create: `frontend/package.json`
- Create: `frontend/vite.config.js`
- Create: `frontend/index.html`
- Create: `frontend/src/main.js`
- Create: `frontend/src/App.vue`

- [ ] **Step 1: 创建 package.json**

```json
{
  "name": "dapp-voting-frontend",
  "version": "1.0.0",
  "private": true,
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "vue": "^3.4.0",
    "vue-router": "^4.3.0",
    "pinia": "^2.1.0",
    "axios": "^1.6.0",
    "element-plus": "^2.5.0",
    "echarts": "^5.5.0",
    "ethers": "^6.10.0"
  },
  "devDependencies": {
    "@vitejs/plugin-vue": "^5.0.0",
    "vite": "^5.0.0"
  }
}
```

- [ ] **Step 2: 创建 vite.config.js**

```js
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
```

- [ ] **Step 3: 创建 index.html**

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>去中心化投票系统</title>
</head>
<body>
  <div id="app"></div>
  <script type="module" src="/src/main.js"></script>
</body>
</html>
```

- [ ] **Step 4: 创建 main.js**

```js
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(ElementPlus)
app.mount('#app')
```

- [ ] **Step 5: 创建 App.vue**

```vue
<template>
  <NavBar />
  <div class="main-container">
    <router-view />
  </div>
</template>

<script setup>
import NavBar from './components/NavBar.vue'
</script>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
body { font-family: 'Microsoft YaHei', sans-serif; background: #f5f7fa; }
.main-container { max-width: 1200px; margin: 20px auto; padding: 0 20px; }
</style>
```

- [ ] **Step 6: 安装依赖**

```bash
cd frontend && npm install
```

---

### Task 3.2: 创建路由、API 层、工具函数

**Files:**
- Create: `frontend/src/router/index.js`
- Create: `frontend/src/api/index.js`
- Create: `frontend/src/api/auth.js`
- Create: `frontend/src/api/poll.js`
- Create: `frontend/src/utils/contract.js`
- Create: `frontend/src/utils/wallet.js`
- Create: `frontend/src/stores/user.js`

- [ ] **Step 1: 创建 router/index.js**

```js
import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', name: 'PollList', component: () => import('../views/PollList.vue') },
  { path: '/poll/:id', name: 'PollDetail', component: () => import('../views/PollDetail.vue') },
  { path: '/create', name: 'CreatePoll', component: () => import('../views/CreatePoll.vue') },
  { path: '/verify', name: 'VerifyPoll', component: () => import('../views/VerifyPoll.vue') },
  { path: '/profile', name: 'Profile', component: () => import('../views/Profile.vue') },
]

export default createRouter({
  history: createWebHistory(),
  routes
})
```

- [ ] **Step 2: 创建 api/index.js (axios 实例)**

```js
import axios from 'axios'
import { ElMessage } from 'element-plus'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

// 请求拦截器 — 自动附加 JWT
api.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截器 — 统一错误处理
api.interceptors.response.use(
  res => res.data,
  err => {
    ElMessage.error(err.response?.data?.message || '请求失败')
    return Promise.reject(err)
  }
)

export default api
```

- [ ] **Step 3: 创建 api/auth.js**

```js
import api from './index'

export function getNonce(address) {
  return api.post('/auth/nonce', null, { params: { address } })
}

export function login(address, sig) {
  return api.post('/auth/login', null, { params: { address, sig } })
}
```

- [ ] **Step 4: 创建 api/poll.js**

```js
import api from './index'

export function getPollList(pageNum = 1, pageSize = 10) {
  return api.get('/polls', { params: { pageNum, pageSize } })
}

export function getPollDetail(id) {
  return api.get(`/polls/${id}`)
}

export function syncPoll(pollId, txHash) {
  return api.post('/polls/sync', null, { params: { pollId, txHash } })
}

export function verifyPoll(id) {
  return api.get(`/polls/${id}/verify`)
}
```

- [ ] **Step 5: 创建 utils/contract.js**

```js
import { ethers } from 'ethers'

// 合约 ABI — 仅包含前端需要的方法
const ABI = [
  "function createPoll(string,string,string[],uint256) returns (uint256)",
  "function vote(uint256,uint256)",
  "function getPollInfo(uint256) view returns (uint256,address,string,string,string[],uint256,uint256)",
  "function getVoteCounts(uint256) view returns (uint256[])",
  "function getHasVoted(uint256,address) view returns (bool)",
  "function getPollCount() view returns (uint256)",
  "function isPollActive(uint256) view returns (bool)",
]

// 部署后替换为实际地址
const CONTRACT_ADDRESS = import.meta.env.VITE_CONTRACT_ADDRESS || ''

/**
 * 获取合约实例 (只读)
 */
export function getReadContract() {
  const provider = new ethers.JsonRpcProvider('http://localhost:8545')
  return new ethers.Contract(CONTRACT_ADDRESS, ABI, provider)
}

/**
 * 获取合约实例 (可写 — 通过 MetaMask signer)
 */
export async function getWriteContract() {
  if (!window.ethereum) throw new Error('请安装 MetaMask')
  const provider = new ethers.BrowserProvider(window.ethereum)
  const signer = await provider.getSigner()
  return new ethers.Contract(CONTRACT_ADDRESS, ABI, signer)
}
```

- [ ] **Step 6: 创建 utils/wallet.js**

```js
/**
 * 连接 MetaMask 钱包
 * @returns {Promise<{address: string, chainId: number}>}
 */
export async function connectWallet() {
  if (!window.ethereum) {
    throw new Error('请先安装 MetaMask 浏览器插件')
  }
  const accounts = await window.ethereum.request({
    method: 'eth_requestAccounts'
  })
  const chainId = await window.ethereum.request({ method: 'eth_chainId' })
  return { address: accounts[0], chainId: parseInt(chainId, 16) }
}

/**
 * 获取当前已连接的钱包地址 (不弹窗)
 */
export async function getCurrentAccount() {
  if (!window.ethereum) return null
  const accounts = await window.ethereum.request({ method: 'eth_accounts' })
  return accounts.length > 0 ? accounts[0] : null
}

/**
 * 用 MetaMask 签名消息
 * @param {string} message - 待签名消息
 * @param {string} address - 签名地址
 * @returns {Promise<string>} 签名 hex
 */
export async function signMessage(message, address) {
  return await window.ethereum.request({
    method: 'personal_sign',
    params: [message, address]
  })
}

/**
 * 监听账户切换
 */
export function onAccountsChanged(callback) {
  if (window.ethereum) {
    window.ethereum.on('accountsChanged', callback)
  }
}

/**
 * 监听链切换
 */
export function onChainChanged(callback) {
  if (window.ethereum) {
    window.ethereum.on('chainChanged', callback)
  }
}
```

- [ ] **Step 7: 创建 stores/user.js (Pinia)**

```js
import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  const address = ref(localStorage.getItem('walletAddress') || '')
  const token = ref(localStorage.getItem('token') || '')

  function setAuth(addr, tk) {
    address.value = addr
    token.value = tk
    localStorage.setItem('walletAddress', addr)
    localStorage.setItem('token', tk)
  }

  function logout() {
    address.value = ''
    token.value = ''
    localStorage.removeItem('walletAddress')
    localStorage.removeItem('token')
  }

  return { address, token, setAuth, logout }
})
```

---

### Task 3.3: 创建公共组件

**Files:**
- Create: `frontend/src/components/NavBar.vue`
- Create: `frontend/src/components/PollCard.vue`
- Create: `frontend/src/components/VoteChart.vue`

- [ ] **Step 1: 创建 NavBar.vue**

```vue
<template>
  <el-menu mode="horizontal" :ellipsis="false" router>
    <div class="nav-left">
      <el-menu-item index="/">
        <span style="font-weight:bold;font-size:18px">🗳️ 去中心化投票</span>
      </el-menu-item>
    </div>
    <div class="nav-right">
      <el-menu-item index="/create">创建投票</el-menu-item>
      <el-menu-item index="/verify">链上验证</el-menu-item>
      <el-menu-item index="/profile">个人中心</el-menu-item>
      <el-menu-item v-if="!store.address" @click="handleConnect">
        连接钱包
      </el-menu-item>
      <el-menu-item v-else>
        {{ store.address.slice(0,6) }}...{{ store.address.slice(-4) }}
      </el-menu-item>
    </div>
  </el-menu>
</template>

<script setup>
import { useUserStore } from '../stores/user'
import { connectWallet, signMessage } from '../utils/wallet'
import { getNonce, login } from '../api/auth'
import { ElMessage } from 'element-plus'

const store = useUserStore()

async function handleConnect() {
  try {
    const { address } = await connectWallet()
    // 获取 nonce 并签名登录
    const { data } = await getNonce(address)
    const sig = await signMessage(data.message, address)
    const res = await login(address, sig)
    store.setAuth(address, res.data.token)
    ElMessage.success('登录成功')
  } catch (e) {
    ElMessage.error(e.message || '连接失败')
  }
}
</script>

<style scoped>
.nav-left { flex: 1; }
.nav-right { display: flex; }
</style>
```

- [ ] **Step 2: 创建 PollCard.vue**

```vue
<template>
  <el-card shadow="hover" class="poll-card" @click="goDetail">
    <template #header>
      <div class="card-header">
        <span class="title">{{ poll.title }}</span>
        <el-tag :type="isActive ? 'success' : 'info'" size="small">
          {{ isActive ? '进行中' : '已结束' }}
        </el-tag>
      </div>
    </template>
    <p class="desc">{{ poll.description || '暂无描述' }}</p>
    <div class="card-footer">
      <span>{{ poll.options?.length || 0 }} 个选项</span>
      <span class="creator">{{ poll.creatorAddress?.slice(0,8) }}...</span>
    </div>
  </el-card>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'

const props = defineProps({ poll: Object })
const router = useRouter()

const isActive = computed(() => {
  if (!props.poll.endTime) return false
  return new Date(props.poll.endTime) > new Date()
})

function goDetail() {
  router.push(`/poll/${props.poll.id}`)
}
</script>

<style scoped>
.poll-card { cursor: pointer; margin-bottom: 16px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.title { font-weight: bold; font-size: 16px; }
.desc { color: #666; margin: 8px 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.card-footer { display: flex; justify-content: space-between; color: #999; font-size: 13px; }
</style>
```

- [ ] **Step 3: 创建 VoteChart.vue**

```vue
<template>
  <div ref="chartRef" style="width:100%;height:400px"></div>
</template>

<script setup>
import { ref, onMounted, watch, onUnmounted } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  options: { type: Array, default: () => [] },
  counts: { type: Array, default: () => [] }
})

const chartRef = ref(null)
let chart = null

function renderChart() {
  if (!chartRef.value) return
  if (!chart) {
    chart = echarts.init(chartRef.value)
  }
  chart.setOption({
    title: { text: '实时投票结果', left: 'center' },
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: props.options,
      axisLabel: { rotate: props.options.length > 5 ? 30 : 0 }
    },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{
      name: '票数',
      type: 'bar',
      data: props.counts,
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#667eea' },
          { offset: 1, color: '#764ba2' }
        ])
      }
    }]
  })
}

onMounted(renderChart)
watch(() => [props.options, props.counts], renderChart, { deep: true })
onUnmounted(() => { chart?.dispose() })
</script>
```

---

### Task 3.4: 创建页面组件

**Files:**
- Create: `frontend/src/views/PollList.vue`
- Create: `frontend/src/views/PollDetail.vue`
- Create: `frontend/src/views/CreatePoll.vue`
- Create: `frontend/src/views/VerifyPoll.vue`
- Create: `frontend/src/views/Profile.vue`

- [ ] **Step 1: 创建 PollList.vue**

```vue
<template>
  <div>
    <h2 style="margin-bottom:20px">投票列表</h2>
    <el-row :gutter="20">
      <el-col v-for="poll in polls" :key="poll.id" :md="8" :sm="12" :xs="24">
        <PollCard :poll="poll" />
      </el-col>
    </el-row>
    <div class="pagination" v-if="total > pageSize">
      <el-pagination
        v-model:current-page="pageNum"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="fetchPolls"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getPollList } from '../api/poll'
import PollCard from '../components/PollCard.vue'

const polls = ref([])
const pageNum = ref(1)
const pageSize = ref(12)
const total = ref(0)

async function fetchPolls() {
  const { data } = await getPollList(pageNum.value, pageSize.value)
  polls.value = data.records
  total.value = data.total
}

onMounted(fetchPolls)
</script>

<style scoped>
.pagination { display: flex; justify-content: center; margin-top: 20px; }
</style>
```

- [ ] **Step 2: 创建 PollDetail.vue**

```vue
<template>
  <div v-if="poll">
    <h2>{{ poll.title }}</h2>
    <p class="meta">
      创建者: {{ poll.creatorAddress?.slice(0,10) }}... |
      结束时间: {{ poll.endTime }}
      <el-tag :type="active ? 'success' : 'danger'" size="small" style="margin-left:10px">
        {{ active ? '进行中' : '已结束' }}
      </el-tag>
    </p>
    <p class="desc">{{ poll.description }}</p>

    <!-- ECharts 图表 -->
    <VoteChart :options="poll.options" :counts="voteCounts" />

    <!-- 投票按钮 -->
    <div v-if="active && !hasVoted" class="vote-area">
      <h4>请选择你要投票的选项：</h4>
      <el-radio-group v-model="selectedOption" class="option-group">
        <el-radio v-for="(opt, i) in poll.options" :key="i" :label="i" border>
          {{ opt }}
        </el-radio>
      </el-radio-group>
      <el-button type="primary" size="large" @click="doVote"
                 :loading="voting" style="margin-top:16px">
        {{ voting ? '交易确认中...' : '提交投票 (MetaMask)' }}
      </el-button>
    </div>
    <div v-else-if="hasVoted">
      <el-alert title="你已在此投票中投过票" type="info" show-icon :closable="false" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getPollDetail, syncPoll } from '../api/poll'
import { getReadContract, getWriteContract } from '../utils/contract'
import { useUserStore } from '../stores/user'
import { ElMessage } from 'element-plus'
import VoteChart from '../components/VoteChart.vue'

const route = useRoute()
const store = useUserStore()
const poll = ref(null)
const voteCounts = ref([])
const selectedOption = ref(null)
const voting = ref(false)
const hasVoted = ref(false)

const active = computed(() => poll.value && new Date(poll.value.endTime) > new Date())

onMounted(async () => {
  const id = route.params.id
  // 从后端获取基本数据
  const { data } = await getPollDetail(id)
  poll.value = data.poll
  voteCounts.value = data.voteCounts

  // 检查当前用户是否已投票
  if (store.address) {
    const contract = getReadContract()
    hasVoted.value = await contract.getHasVoted(id, store.address)
  }
})

async function doVote() {
  if (selectedOption.value === null) {
    return ElMessage.warning('请先选择一个选项')
  }
  voting.value = true
  try {
    const contract = await getWriteContract()
    const tx = await contract.vote(poll.value.id, selectedOption.value)
    ElMessage.info('交易已提交，等待链上确认...')
    await tx.wait()
    ElMessage.success('投票成功！')

    // 通知后端同步
    await syncPoll(poll.value.id, tx.hash)

    hasVoted.value = true
    // 刷新票数
    const { data } = await getPollDetail(poll.value.id)
    voteCounts.value = data.voteCounts
  } catch (e) {
    ElMessage.error('投票失败: ' + (e.reason || e.message))
  } finally {
    voting.value = false
  }
}
</script>

<style scoped>
.meta { color: #666; margin: 12px 0; }
.desc { background: #f0f2f5; padding: 16px; border-radius: 8px; margin: 16px 0; }
.vote-area { margin-top: 24px; }
.option-group { display: flex; flex-direction: column; gap: 12px; margin-top: 12px; }
</style>
```

- [ ] **Step 3: 创建 CreatePoll.vue**

```vue
<template>
  <div>
    <h2>创建新投票</h2>
    <el-form :model="form" label-width="100px" style="max-width:600px;margin-top:20px">
      <el-form-item label="投票标题" required>
        <el-input v-model="form.title" placeholder="请输入投票标题" />
      </el-form-item>
      <el-form-item label="投票描述">
        <el-input v-model="form.description" type="textarea" :rows="3"
                  placeholder="描述这次投票的目的" />
      </el-form-item>
      <el-form-item label="投票选项" required>
        <div v-for="(opt, i) in form.options" :key="i" class="option-row">
          <el-input v-model="form.options[i]" :placeholder="`选项 ${i+1}`" />
          <el-button v-if="form.options.length > 2" type="danger" :icon="'Delete'"
                     circle size="small" @click="removeOption(i)" />
        </div>
        <el-button type="primary" text @click="addOption">+ 添加选项</el-button>
      </el-form-item>
      <el-form-item label="持续时间">
        <el-select v-model="form.duration" placeholder="选择投票持续时间">
          <el-option label="1 小时" :value="3600" />
          <el-option label="6 小时" :value="21600" />
          <el-option label="24 小时" :value="86400" />
          <el-option label="3 天" :value="259200" />
          <el-option label="7 天" :value="604800" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" size="large" @click="doCreate" :loading="creating">
          {{ creating ? '交易确认中...' : '创建投票 (MetaMask)' }}
        </el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getWriteContract } from '../utils/contract'
import { syncPoll } from '../api/poll'
import { ElMessage } from 'element-plus'

const router = useRouter()
const creating = ref(false)

const form = reactive({
  title: '',
  description: '',
  options: ['', ''],
  duration: 86400
})

function addOption() {
  form.options.push('')
}

function removeOption(i) {
  form.options.splice(i, 1)
}

async function doCreate() {
  if (!form.title.trim()) return ElMessage.warning('请输入标题')
  if (form.options.filter(o => o.trim()).length < 2) {
    return ElMessage.warning('至少需要 2 个非空选项')
  }
  creating.value = true
  try {
    const validOptions = form.options.filter(o => o.trim())
    const contract = await getWriteContract()
    const tx = await contract.createPoll(
      form.title, form.description, validOptions, form.duration
    )
    ElMessage.info('交易已提交，等待链上确认...')
    const receipt = await tx.wait()

    // 从事件中获取 pollId
    const event = receipt.logs.find(
      log => log.fragment?.name === 'PollCreated'
    )
    const pollId = event?.args?.pollId

    // 通知后端同步
    if (pollId !== undefined) {
      await syncPoll(Number(pollId), tx.hash)
    }

    ElMessage.success('投票创建成功！')
    router.push('/')
  } catch (e) {
    ElMessage.error('创建失败: ' + (e.reason || e.message))
  } finally {
    creating.value = false
  }
}
</script>

<style scoped>
.option-row { display: flex; gap: 8px; margin-bottom: 8px; align-items: center; }
</style>
```

- [ ] **Step 4: 创建 VerifyPoll.vue**

```vue
<template>
  <div>
    <h2>链上数据验证</h2>
    <p class="hint">输入投票 ID，对比链上数据和后端数据库是否一致</p>
    <el-input v-model="pollId" placeholder="输入 Poll ID" style="max-width:300px;margin:16px 0" />
    <el-button type="primary" @click="doVerify" :loading="verifying">验证</el-button>

    <el-row v-if="result" :gutter="20" style="margin-top:24px">
      <el-col :span="12">
        <h4>📦 链上数据 (合约)</h4>
        <pre>{{ JSON.stringify(result.chain, null, 2) }}</pre>
      </el-col>
      <el-col :span="12">
        <h4>🗄️ 本地数据 (MySQL)</h4>
        <pre>{{ JSON.stringify(result.local, null, 2) }}</pre>
      </el-col>
    </el-row>
    <el-alert v-if="result" :title="result.consistent ? '✅ 数据一致' : '❌ 数据不一致'"
              :type="result.consistent ? 'success' : 'error'"
              style="margin-top:16px" show-icon :closable="false" />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { verifyPoll } from '../api/poll'

const pollId = ref('')
const verifying = ref(false)
const result = ref(null)

async function doVerify() {
  if (!pollId.value) return
  verifying.value = true
  const { data } = await verifyPoll(pollId.value)
  result.value = data
  verifying.value = false
}
</script>

<style scoped>
.hint { color: #999; }
pre { background: #1e1e1e; color: #d4d4d4; padding: 16px; border-radius: 8px;
      overflow-x: auto; font-size: 12px; max-height: 400px; }
</style>
```

- [ ] **Step 5: 创建 Profile.vue**

```vue
<template>
  <div>
    <h2>个人中心</h2>
    <el-card v-if="store.address" style="max-width:500px;margin-top:20px">
      <p><strong>钱包地址：</strong>{{ store.address }}</p>
      <p><strong>登录状态：</strong><el-tag type="success">已登录</el-tag></p>
      <el-button type="danger" @click="store.logout()" style="margin-top:16px">退出登录</el-button>
    </el-card>
    <el-empty v-else description="请先连接钱包登录" />
  </div>
</template>

<script setup>
import { useUserStore } from '../stores/user'
const store = useUserStore()
</script>
```

---

### Task 3.5: 创建 .env 文件

**Files:**
- Create: `frontend/.env.development`

- [ ] **Step 1: 创建 .env.development**

```env
VITE_CONTRACT_ADDRESS=0x5FbDB2315678afecb367f032d93F642f64180aa3
```

> 注：部署合约后替换为实际地址

---

## 附录 A: 启动顺序

```bash
# 1. 启动 Hardhat 本地节点
cd contracts && npx hardhat node

# 2. 部署合约 (新终端)
cd contracts && npx hardhat run scripts/deploy.js --network localhost

# 3. 确保 MySQL 和 Redis 已启动
# 在 MySQL 中执行建表 SQL

# 4. 启动后端
cd backend && mvn spring-boot:run

# 5. 启动前端
cd frontend && npm run dev
```

## 附录 B: 关键配置项

启动前需要确认/修改的配置：

| 文件 | 配置项 | 说明 |
|------|--------|------|
| `backend/src/main/resources/application.yml` | `web3j.contract-address` | 部署后填入 |
| `backend/src/main/resources/application.yml` | `spring.datasource.password` | 本地 MySQL 密码 |
| `frontend/.env.development` | `VITE_CONTRACT_ADDRESS` | 部署后填入 |
| `backend/src/main/resources/abi/` | VotingContract.json | 从 contracts 复制 |
