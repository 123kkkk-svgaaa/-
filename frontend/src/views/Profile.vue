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
