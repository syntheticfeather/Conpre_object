<template>
  <div ref="chartRef" class="chart-item"></div>
</template>

<script>
import * as echarts from 'echarts'
import { onMounted, onBeforeUnmount, ref } from 'vue'
import { createPieTooltipConfig } from '@/utils/pieTooltipHelper'

export default {
  name: 'PurposeChart',
  setup() {
    const chartRef = ref(null)
    let chartInstance = null

    onMounted(() => {
      if (!chartRef.value) return
      
      chartInstance = echarts.init(chartRef.value)
      
      // 计算总数
      const data = [
        { value: 400, name: '消费贷款', itemStyle: { color: '#ff6b6b' } },
        { value: 300, name: '企业经营', itemStyle: { color: '#4ecdc4' } },
        { value: 250, name: '购房装修', itemStyle: { color: '#45b7d1' } },
        { value: 200, name: '教育培训', itemStyle: { color: '#ffa07a' } },
        { value: 150, name: '医疗健康', itemStyle: { color: '#98d8c8' } },
        { value: 100, name: '其他用途', itemStyle: { color: '#f7d794' } }
      ]
      const total = data.reduce((sum, item) => sum + item.value, 0)
      
      const option = {
        title: {
          text: '资金用途分类',
          textStyle: {
            color: '#fff',
            fontSize: 16
          },
          left: 'center'
        },
        tooltip: createPieTooltipConfig(total),
        series: [
          {
            name: '资金用途',
            type: 'pie',
            radius: ['20%', '70%'],
            center: ['50%', '50%'],
            roseType: 'area',
            itemStyle: {
              borderRadius: 5,
              borderColor: '#fff',
              borderWidth: 2
            },
            label: {
              show: true,
              color: '#fff',
              fontSize: 12,
              formatter: '{b}: {c}'
            },
            data
          }
        ]
      }
      chartInstance.setOption(option)
    })

    onBeforeUnmount(() => {
      if (chartInstance) {
        chartInstance.dispose()
        chartInstance = null
      }
    })

    const resize = () => {
      chartInstance?.resize()
    }

    return {
      chartRef,
      resize
    }
  }
}
</script>

<style scoped>
.chart-item {
  width: 100%;
  height: 160px;
  background: rgba(123, 166, 194, 0.5);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  padding: 0px;
  color: white;
  pointer-events: auto;
}
</style>
