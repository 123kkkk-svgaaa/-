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
