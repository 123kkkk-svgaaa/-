# 去中心化投票系统 — 设计文档

**日期**: 2026-06-10
**状态**: 已确认
**项目路径**: `D:\项目管理\project-select`

---

## 1. 概述

一个课程设计项目，实现基于以太坊的去中心化投票 DApp。核心原则：**所有投票数据上链，结果不可篡改，匿名可追溯，公开可验证**。

## 2. 技术栈

| 层 | 技术 |
|---|------|
| 合约 | Solidity ^0.8.x, Hardhat, 本地节点 (后续可迁移 Sepolia) |
| 后端 | SpringBoot 2.7+, MyBatis-Plus, MySQL 8, Redis 6, Web3j 6.x |
| 前端 | Vue 3 + Vite, Element Plus, ECharts 5, ethers.js 6, MetaMask |

## 3. 项目结构 (monorepo)

```
dapp-voting/
├── contracts/           # Hardhat + Solidity
│   ├── contracts/
│   │   └── VotingContract.sol
│   ├── scripts/
│   │   └── deploy.js
│   └── hardhat.config.js
├── backend/             # SpringBoot
│   ├── src/main/java/com/voting/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── mapper/
│   │   ├── entity/
│   │   ├── config/
│   │   └── listener/
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── mapper/
│   └── pom.xml
├── frontend/            # Vue 3 + Vite
│   ├── src/
│   │   ├── views/
│   │   ├── components/
│   │   ├── api/
│   │   ├── utils/
│   │   └── router/
│   └── package.json
└── docs/
    └── superpowers/
        └── specs/
```

## 4. 架构模式：混合模式

```
用户浏览器 (MetaMask)
    │
    ├── 写操作 (createPoll, vote)
    │   └── ethers.js → MetaMask 签名 → 合约直接上链 → 返回 txHash
    │       └── 前端拿 txHash 调 POST /api/polls/sync 通知后端
    │
    └── 读操作 (列表, 详情, 统计)
        └── axios → SpringBoot API → MySQL/Redis → 返回 JSON

Hardhat 本地节点 ←→ Web3j 事件监听 → MySQL 同步 + Redis 缓存更新
```

## 5. 智能合约设计

### 5.1 核心结构

```solidity
struct Poll {
    uint256 id;
    address creator;
    string title;
    string description;
    string[] options;
    uint256 startTime;
    uint256 endTime;
    bool isActive;
}

// 链上存储
Poll[] private polls;
mapping(uint256 => mapping(address => bool)) private hasVoted;
mapping(uint256 => mapping(uint256 => uint256)) private voteCounts;
```

### 5.2 函数签名

- `createPoll(string title, string desc, string[] options, uint256 duration) → uint256`
- `vote(uint256 pollId, uint256 optionIndex)`
- `getPollInfo(uint256 pollId) view returns (Poll memory)`
- `getVoteCounts(uint256 pollId) view returns (uint256[] memory)`
- `getHasVoted(uint256 pollId, address voter) view returns (bool)`
- `getPollCount() view returns (uint256)`

### 5.3 事件

- `PollCreated(uint256 indexed pollId, address indexed creator, uint256 endTime)`
- `VoteCasted(uint256 indexed pollId, address indexed voter, uint256 optionIndex)`

### 5.4 设计要点

- 无 `updatePoll` / `deletePoll` → 不可篡改
- mapping 不直接通过 struct 返回，单独提供查询函数
- 匿名性：链上仅存 address，后端 JWT 不绑定钱包实名

## 6. 后端设计

### 6.1 数据模型

**polls 表**
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 对应链上 pollId |
| creator_address | VARCHAR(42) | 创建者地址 |
| title | VARCHAR(200) | 标题 |
| description | TEXT | 描述 |
| options | JSON | 选项数组 |
| start_time | DATETIME | 开始时间 |
| end_time | DATETIME | 结束时间 |
| tx_hash | VARCHAR(66) | 创建交易哈希 |
| created_at | DATETIME | 创建时间 |

**poll_results 表**
| 字段 | 类型 | 说明 |
|------|------|------|
| poll_id | BIGINT FK | 关联 polls |
| option_index | INT | 选项索引 |
| vote_count | INT | 得票数 |
| updated_at | DATETIME | 更新时间 |

**users 表**
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 自增 |
| wallet_address | VARCHAR(42) UNIQUE | 钱包地址 |
| nonce | VARCHAR(64) | 登录随机数 |
| created_at | DATETIME | 创建时间 |

**Redis 缓存**
- `poll:{id}:vote_counts` (Hash) — 实时票数
- `poll:{id}:voters` (Set) — 已投票地址
- `user:nonce:{address}` (String) — 登录 nonce

### 6.2 API 端点

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/nonce?address=` | 获取登录随机数 |
| POST | `/api/auth/login?address=&sig=` | 验证签名，返回 JWT |
| GET | `/api/polls` | 投票列表 (分页) |
| GET | `/api/polls/{id}` | 投票详情 + 实时票数 |
| POST | `/api/polls/sync?txHash=` | 前端上链后通知后端同步 |
| GET | `/api/polls/{id}/verify` | 链上数据对比验证 |
| GET | `/api/user/votes` | 个人投票记录 (需 JWT) |

### 6.3 事件监听

SpringBoot 启动时初始化 Web3j，注册 `PollCreated` / `VoteCasted` 事件过滤器，收到事件后自动同步 MySQL + Redis。

## 7. 前端设计

### 7.1 路由

| 路径 | 页面 | 说明 |
|------|------|------|
| `/` | 投票列表 | 首页，展示所有投票卡片 |
| `/poll/:id` | 投票详情 | 实时票数 ECharts 图表 + 投票操作 |
| `/create` | 创建投票 | 表单，提交后调 MetaMask 上链 |
| `/verify` | 链上验证 | 输入 pollId 对比链上链下数据 |
| `/profile` | 个人中心 | JWT 认证，历史投票记录 |

### 7.2 核心交互流程

1. **用户连接 MetaMask** → 获取钱包地址
2. **登录** → 获取 nonce → 签名 → 验证 → 获取 JWT
3. **创建投票** → 填写表单 → ethers.js 调用合约 → 拿 txHash 通知后端
4. **投票** → 选择选项 → ethers.js 调用合约 → 拿 txHash 通知后端
5. **查看实时票数** → 后端 API 返回 (来自 Redis + MySQL)

## 8. 区块链融入点总结

- 投票信息全量上链（创建时写合约）
- 投票行为链上记录（每次投票一笔交易）
- 后端事件监听同步（链上为唯一真相源）
- 前端可独立验证（ethers.js 直接读合约对比）
- 匿名：仅存地址，无实名关联
- 不可篡改：合约无修改/删除接口

## 9. 部署说明

- 合约：Hardhat 本地节点 `localhost:8545`
- 后端：`mvn spring-boot:run` → `localhost:8080`
- 前端：`npm run dev` → `localhost:5173`
- 数据库：MySQL 8 本地实例，库名 `dapp_voting`
- Redis：Redis 6 本地实例
