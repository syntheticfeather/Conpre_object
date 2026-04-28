<template>
  <div ref="chartRef" class="chart-item"></div>
</template>

<script>
import * as echarts from 'echarts'
import { onMounted, onBeforeUnmount, ref } from 'vue'
import { createPieTooltipConfig } from '@/utils/pieTooltipHelper'

export default {
  name: 'StatusChart',
  setup() {
    const chartRef = ref(null)
    let chartInstance = null

    onMounted(() => {
      if (!chartRef.value) return
      
      chartInstance = echarts.init(chartRef.value)
      
      // 计算总数
      const data = [
        { value: 300, name: '还款中', itemStyle: { color: '#9DC8C8' } },
        { value: 150, name: '申请贷款中', itemStyle: { color: '#58C9B9' } },
        { value: 350, name: '正常', itemStyle: { color: '#8EC0E4' } },
        { value: 80, name: '逾期', itemStyle: { color: '#D1B6E1' } },
        { value: 20, name: '黑名单', itemStyle: { color: '#F17F42' } }
      ]
      const total = data.reduce((sum, item) => sum + item.value, 0)
      
      const option = {
        title: {
          text: '用户状态',
          textStyle: {
            color: '#fff'
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
        series: [{
          name: '用户状态',
          type: 'pie',
          radius: ['40%', '70%'],
          avoidLabelOverlap: false,
          itemStyle: {
            borderRadius: 5,
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
              fontSize: '18',
              fontWeight: 'bold',
              color: '#fff'
            }
          },
          labelLine: {
            show: false
          },
          data
        }]
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
