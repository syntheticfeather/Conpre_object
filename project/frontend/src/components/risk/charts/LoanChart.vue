<template>
  <div ref="chartRef" class="chart-item"></div>
</template>

<script>
import * as echarts from 'echarts'
import { onMounted, onBeforeUnmount, ref } from 'vue'
import { createPieTooltipConfig } from '@/utils/pieTooltipHelper'

export default {
  name: 'LoanChart',
  setup() {
    const chartRef = ref(null)
    let chartInstance = null

    onMounted(() => {
      if (!chartRef.value) return
      
      chartInstance = echarts.init(chartRef.value)
      
      // 计算总数
      const data = [
        { value: 1200, name: '审核中', itemStyle: { color: '#ffd700' } },
        { value: 800, name: '已通过', itemStyle: { color: '#4caf50' } },
        { value: 300, name: '已拒绝', itemStyle: { color: '#f44336' } },
        { value: 500, name: '待放款', itemStyle: { color: '#2196f3' } },
        { value: 200, name: '已放款', itemStyle: { color: '#9c27b0' } }
      ]
      const total = data.reduce((sum, item) => sum + item.value, 0)
      
      const option = {
        title: {
          text: '贷款申请状态分布',
          textStyle: {
            color: '#fff',
            fontSize: 16
          },
          left: 'center'
        },
        tooltip: createPieTooltipConfig(total),
        legend: {
          orient: 'vertical',
          left: 'left',
          textStyle: {
            color: '#fff'
          }
        },
        series: [
          {
            name: '申请状态',
            type: 'pie',
            radius: ['35%', '65%'],
            avoidLabelOverlap: false,
            itemStyle: {
              borderRadius: 8,
              borderColor: '#fff',
              borderWidth: 2
            },
            label: {
              show: false,
              position: 'center'
            },
            emphasis: {
              label: {
                show: true,
                fontSize: '16',
                fontWeight: 'bold',
                color: '#fff'
              }
            },
            labelLine: {
              show: false
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
  border-radius: 8px;
  padding: 0px;
  color: white;
  pointer-events: auto;
}
</style>
