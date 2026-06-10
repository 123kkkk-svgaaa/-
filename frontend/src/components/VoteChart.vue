<template>
  <div ref="chartRef" style="width:100%;height:400px"></div>
</template>

<script setup>
import { ref, onMounted, watch, onUnmounted } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  options: { type: Array, default: () => [] },
  counts: { type: Array, default: () => [] }
})

const chartRef = ref(null)
let chart = null

function renderChart() {
  if (!chartRef.value) return
  if (!chart) chart = echarts.init(chartRef.value)
  chart.setOption({
    title: { text: '实时投票结果', left: 'center' },
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: props.options,
      axisLabel: { rotate: props.options.length > 5 ? 30 : 0 }
    },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{
      name: '票数',
      type: 'bar',
      data: props.counts,
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#667eea' },
          { offset: 1, color: '#764ba2' }
        ])
      }
    }]
  })
}

onMounted(renderChart)
watch(() => [props.options, props.counts], renderChart, { deep: true })
onUnmounted(() => { chart?.dispose() })
</script>
