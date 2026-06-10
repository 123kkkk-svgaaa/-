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
