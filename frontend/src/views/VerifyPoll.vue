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
    <el-alert
      v-if="result"
      :title="result.consistent ? '✅ 数据一致' : '❌ 数据不一致'"
      :type="result.consistent ? 'success' : 'error'"
      style="margin-top:16px" show-icon :closable="false"
    />
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
