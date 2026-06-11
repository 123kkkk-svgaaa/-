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

function addOption() { form.options.push('') }
function removeOption(i) { form.options.splice(i, 1) }

async function doCreate() {
  if (!form.title.trim()) return ElMessage.warning('请输入标题')
  const validOptions = form.options.filter(o => o.trim())
  if (validOptions.length < 2) {
    return ElMessage.warning('至少需要 2 个非空选项')
  }
  if (!window.ethereum) {
    return ElMessage.warning('请先安装并连接 MetaMask')
  }
  creating.value = true
  try {
    const contract = await getWriteContract()
    const tx = await contract.createPoll(
      form.title, form.description, validOptions, form.duration
    )
    ElMessage.info('交易已提交，等待链上确认...')
    const receipt = await tx.wait()

    // 从事件中获取 pollId
    const event = receipt.logs.find(log => log.fragment?.name === 'PollCreated')
    const pollId = event?.args?.pollId

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
