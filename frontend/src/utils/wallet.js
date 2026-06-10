/** 连接 MetaMask 并获取账户地址 */
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

/** 获取当前已连接账户 (不弹窗) */
export async function getCurrentAccount() {
  if (!window.ethereum) return null
  const accounts = await window.ethereum.request({ method: 'eth_accounts' })
  return accounts.length > 0 ? accounts[0] : null
}

/** 用 MetaMask personal_sign 签名消息 */
export async function signMessage(message, address) {
  return await window.ethereum.request({
    method: 'personal_sign',
    params: [message, address]
  })
}

/** 监听账户切换 */
export function onAccountsChanged(callback) {
  if (window.ethereum) {
    window.ethereum.on('accountsChanged', callback)
  }
}

/** 监听链切换 */
export function onChainChanged(callback) {
  if (window.ethereum) {
    window.ethereum.on('chainChanged', callback)
  }
}
