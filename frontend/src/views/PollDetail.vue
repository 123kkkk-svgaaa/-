<template>
  <div v-if="poll">
    <h2>{{ poll.title }}</h2>
    <p class="meta">
      投票 ID: {{ poll.id }} |
      创建者: {{ poll.creatorAddress?.slice(0,10) }}... |
      结束时间: {{ poll.endTime }}
      <el-tag :type="active ? 'success' : 'danger'" size="small" style="margin-left:10px">
        {{ active ? '进行中' : '已结束' }}
      </el-tag>
    </p>
    <p class="desc" v-if="poll.description">{{ poll.description }}</p>

    <!-- ECharts 图表 -->
    <VoteChart :options="poll.options" :counts="voteCounts" />

    <!-- 投票区域 -->
    <div v-if="active && !hasVoted" class="vote-area">
      <h4>请选择你要投票的选项：</h4>
      <el-radio-group v-model="selectedOption" class="option-group">
        <el-radio v-for="(opt, i) in poll.options" :key="i" :label="i" border>
          {{ opt }}
        </el-radio>
      </el-radio-group>
      <el-button type="primary" size="large" @click="doVote"
                 :loading="voting" style="margin-top:16px">
        {{ voting ? '交易确认中...' : '提交投票 (MetaMask)' }}
      </el-button>
    </div>
    <div v-else-if="hasVoted" style="margin-top:20px">
      <el-alert title="你已在此投票中投过票" type="info" show-icon :closable="false" />
    </div>
  </div>
  <div v-else-if="loading" class="loading-center">
    <el-skeleton :rows="5" animated />
  </div>
  <el-empty v-else description="投票不存在或加载失败" />
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getPollDetail, syncPoll } from '../api/poll'
import { getReadContract, getWriteContract } from '../utils/contract'
import { useUserStore } from '../stores/user'
import { ElMessage } from 'element-plus'
import VoteChart from '../components/VoteChart.vue'

const route = useRoute()
const store = useUserStore()
const poll = ref(null)
const voteCounts = ref([])
const selectedOption = ref(null)
const voting = ref(false)
const hasVoted = ref(false)
const loading = ref(true)

const active = computed(() => poll.value && new Date(poll.value.endTime) > new Date())

onMounted(async () => {
  try {
    const id = route.params.id
    const { data } = await getPollDetail(id)
    poll.value = data.poll
    voteCounts.value = data.voteCounts

    // 检查当前用户是否已投票
    if (store.address) {
      const contract = getReadContract()
      hasVoted.value = await contract.getHasVoted(id, store.address)
    }
  } catch (e) {
    ElMessage.error('加载投票详情失败')
  } finally {
    loading.value = false
  }
})

async function doVote() {
  if (!store.address) {
    return ElMessage.warning('请先连接钱包')
  }
  if (selectedOption.value === null) {
    return ElMessage.warning('请先选择一个选项')
  }
  voting.value = true
  try {
    const contract = await getWriteContract()
    const tx = await contract.vote(poll.value.id, selectedOption.value)
    ElMessage.info('交易已提交，等待链上确认...')
    await tx.wait()
    ElMessage.success('投票成功！')

    // 通知后端同步
    await syncPoll(poll.value.id, tx.hash)

    hasVoted.value = true
    // 刷新票数
    const { data } = await getPollDetail(poll.value.id)
    voteCounts.value = data.voteCounts
  } catch (e) {
    ElMessage.error('投票失败: ' + (e.reason || e.message))
  } finally {
    voting.value = false
  }
}
</script>

<style scoped>
.meta { color: #666; margin: 12px 0; }
.desc { background: #f0f2f5; padding: 16px; border-radius: 8px; margin: 16px 0; }
.vote-area { margin-top: 24px; }
.option-group { display: flex; flex-direction: column; gap: 12px; margin-top: 12px; }
.loading-center { padding: 60px 0; }
</style>
