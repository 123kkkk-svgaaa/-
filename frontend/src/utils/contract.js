import { ethers } from 'ethers'

// 合约 ABI — 仅包含前端直接调用的方法
const ABI = [
  "function createPoll(string,string,string[],uint256) returns (uint256)",
  "function vote(uint256,uint256)",
  "function getPollInfo(uint256) view returns (uint256,address,string,string,string[],uint256,uint256)",
  "function getVoteCounts(uint256) view returns (uint256[])",
  "function getHasVoted(uint256,address) view returns (bool)",
  "function getPollCount() view returns (uint256)",
  "function isPollActive(uint256) view returns (bool)"
]

const CONTRACT_ADDRESS = import.meta.env.VITE_CONTRACT_ADDRESS || '0x67d269191c92Caf3cD7723F116c85e6E9bf55933'

/** 获取只读合约实例 (直接连节点) */
export function getReadContract() {
  const provider = new ethers.JsonRpcProvider('http://localhost:8545')
  return new ethers.Contract(CONTRACT_ADDRESS, ABI, provider)
}

/** 获取可写合约实例 (通过 MetaMask signer) */
export async function getWriteContract() {
  if (!window.ethereum) throw new Error('请安装 MetaMask')
  const provider = new ethers.BrowserProvider(window.ethereum)
  const signer = await provider.getSigner()
  return new ethers.Contract(CONTRACT_ADDRESS, ABI, signer)
}
