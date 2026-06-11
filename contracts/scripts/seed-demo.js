const hre = require("hardhat");

async function main() {
  const [owner, addr1, addr2, addr3] = await hre.ethers.getSigners();
  const contractAddress = "0x5FbDB2315678afecb367f032d93F642f64180aa3";
  const abi = [
    "function createPoll(string,string,string[],uint256) returns (uint256)",
    "function vote(uint256,uint256)",
    "function getPollCount() view returns (uint256)",
    "function getPollInfo(uint256) view returns (uint256,address,string,string,string[],uint256,uint256)",
    "function getVoteCounts(uint256) view returns (uint256[])",
  ];
  const contract = new hre.ethers.Contract(contractAddress, abi, owner);

  console.log("Current poll count:", Number(await contract.getPollCount()));

  // === Create Poll 1: 编程语言投票 ===
  console.log("\n--- 创建投票1: 最喜欢的编程语言 ---");
  const tx1 = await contract.createPoll(
    "最喜欢的编程语言？",
    "选出你认为2026年最值得学习的编程语言",
    ["Solidity", "Rust", "TypeScript", "Python", "Go"],
    86400 * 7 // 7 days
  );
  const r1 = await tx1.wait();
  console.log("Poll 0 created, tx:", r1.hash);

  // === Create Poll 2: Web3技术 ===
  console.log("\n--- 创建投票2: 最具潜力的Web3技术 ---");
  const tx2 = await contract.createPoll(
    "最具潜力的Web3技术方向？",
    "哪个方向会在未来3年爆发式增长？",
    ["DeFi", "NFT/游戏", "去中心化身份(DID)", "Layer2扩容", "AI+区块链"],
    86400 * 3 // 3 days
  );
  const r2 = await tx2.wait();
  console.log("Poll 1 created, tx:", r2.hash);

  // === Create Poll 3: 课程反馈 ===
  console.log("\n--- 创建投票3: 课程设计反馈 ---");
  const tx3 = await contract.createPoll(
    "这门课最难的部分是？",
    "帮助我们改进课程内容",
    ["智能合约开发", "前后端联调", "区块链基础概念", "环境搭建"],
    3600 * 6 // 6 hours
  );
  const r3 = await tx3.wait();
  console.log("Poll 2 created, tx:", r3.hash);

  // === Vote on polls ===
  console.log("\n--- 投票 ---");
  // addr1 votes on Poll 0
  const v1 = await contract.connect(addr1).vote(0, 2); // TypeScript
  await v1.wait();
  console.log("addr1 voted Poll0: TypeScript");

  const v2 = await contract.connect(addr2).vote(0, 1); // Rust
  await v2.wait();
  console.log("addr2 voted Poll0: Rust");

  const v3 = await contract.connect(addr3).vote(0, 2); // TypeScript
  await v3.wait();
  console.log("addr3 voted Poll0: TypeScript");

  // Votes on Poll 1
  const v4 = await contract.connect(addr1).vote(1, 0); // DeFi
  await v4.wait();
  console.log("addr1 voted Poll1: DeFi");

  const v5 = await contract.connect(addr2).vote(1, 4); // AI+区块链
  await v5.wait();
  console.log("addr2 voted Poll1: AI+区块链");

  const v6 = await contract.connect(addr3).vote(1, 4); // AI+区块链
  await v6.wait();
  console.log("addr3 voted Poll1: AI+区块链");

  // Votes on Poll 2
  const v7 = await contract.connect(addr1).vote(2, 0); // 智能合约
  await v7.wait();
  console.log("addr1 voted Poll2: 智能合约开发");

  // === Show results ===
  console.log("\n=== 投票结果 ===");
  for (let i = 0; i < 3; i++) {
    const info = await contract.getPollInfo(i);
    const counts = await contract.getVoteCounts(i);
    console.log(`\n[Poll ${i}] ${info.title}`);
    const total = counts.reduce((a, b) => a + Number(b), 0);
    console.log(`总票数: ${total}`);
    info.options.forEach((opt, j) => {
      console.log(`  ${opt}: ${counts[j]} 票`);
    });
  }
}

main().catch(console.error);
