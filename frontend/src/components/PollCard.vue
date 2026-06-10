<template>
  <el-card shadow="hover" class="poll-card" @click="goDetail">
    <template #header>
      <div class="card-header">
        <span class="title">{{ poll.title }}</span>
        <el-tag :type="isActive ? 'success' : 'info'" size="small">
          {{ isActive ? '进行中' : '已结束' }}
        </el-tag>
      </div>
    </template>
    <p class="desc">{{ poll.description || '暂无描述' }}</p>
    <div class="card-footer">
      <span>{{ poll.options?.length || 0 }} 个选项</span>
      <span class="creator">{{ poll.creatorAddress?.slice(0,8) }}...</span>
    </div>
  </el-card>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'

const props = defineProps({ poll: Object })
const router = useRouter()

const isActive = computed(() => {
  if (!props.poll.endTime) return false
  return new Date(props.poll.endTime) > new Date()
})

function goDetail() {
  router.push(`/poll/${props.poll.id}`)
}
</script>

<style scoped>
.poll-card { cursor: pointer; margin-bottom: 16px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.title { font-weight: bold; font-size: 16px; }
.desc { color: #666; margin: 8px 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.card-footer { display: flex; justify-content: space-between; color: #999; font-size: 13px; }
</style>
