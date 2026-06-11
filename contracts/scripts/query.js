const hre = require("hardhat");

async function main() {
  const c = await hre.ethers.getContractAt(
    "VotingContract",
    "0x67d269191c92Caf3cD7723F116c85e6E9bf55933"
  );

  const count = await c.getPollCount();
  console.log("总投票数:", Number(count));

  for (let i = 0; i < Number(count); i++) {
    const info = await c.getPollInfo(i);
    const counts = await c.getVoteCounts(i);
    console.log("\n[投票" + i + "]", info[2]); // title via indexed access
    console.log("  描述:", info[3]);
    console.log("  选项:", info[4]);
    console.log("  票数:", counts.map(c => Number(c)));
    console.log("  结束时间:", new Date(Number(info[6]) * 1000).toLocaleString());
  }
}

main();
