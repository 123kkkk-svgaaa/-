<template>
  <div>
    <h2 style="margin-bottom:20px">投票列表</h2>
    <el-row :gutter="20">
      <el-col v-for="poll in polls" :key="poll.id" :md="8" :sm="12" :xs="24">
        <PollCard :poll="poll" />
      </el-col>
    </el-row>
    <div class="pagination" v-if="total > pageSize">
      <el-pagination
        v-model:current-page="pageNum"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="fetchPolls"
      />
    </div>
    <el-empty v-if="!loading && polls.length === 0" description="暂无投票" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getPollList } from '../api/poll'
import PollCard from '../components/PollCard.vue'

const polls = ref([])
const pageNum = ref(1)
const pageSize = ref(12)
const total = ref(0)
const loading = ref(true)

async function fetchPolls() {
  loading.value = true
  const { data } = await getPollList(pageNum.value, pageSize.value)
  polls.value = data.records
  total.value = data.total
  loading.value = false
}

onMounted(fetchPolls)
</script>

<style scoped>
.pagination { display: flex; justify-content: center; margin-top: 20px; }
</style>
