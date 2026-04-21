<template>
  <div ref="chartRef" class="chart-item"></div>
</template>

<script>
import * as echarts from 'echarts'
import { onMounted, onBeforeUnmount, ref } from 'vue'

export default {
  name: 'RegisterChart',
  setup() {
    const chartRef = ref(null)
    let chartInstance = null

    onMounted(() => {
      if (!chartRef.value) return
      
      chartInstance = echarts.init(chartRef.value)
      const option = {
        grid: {
          left: '3%',
          right: '4%',
          top: '15%',
          bottom: '8%',
          containLabel: true
        },
        title: {
          text: '每月注册/登录人数',
          textStyle: {
            color: '#fff',
            fontSize: 16
          },
          left: 'center'
        },
        xAxis: {
          type: 'category',
          data: ['1 月', '2 月', '3 月', '4 月', '5 月', '6 月'],
          axisLabel: {
            color: '#fff'
          },
          axisLine: {
            lineStyle: {
              color: 'rgba(255, 255, 255, 0.3)'
            }
          }
        },
        yAxis: {
          type: 'value',
          axisLabel: {
            color: '#fff'
          },
          axisLine: {
            lineStyle: {
              color: 'rgba(255, 255, 255, 0.3)'
            }
          },
          splitLine: {
            lineStyle: {
              color: 'rgba(255, 255, 255, 0.1)'
            }
          }
        },
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'shadow'
          },
          backgroundColor: 'rgba(0, 0, 0, 0.7)',
          borderColor: 'rgba(255, 255, 255, 0.2)',
          borderWidth: 1,
          textStyle: {
            color: '#fff'
          }
        },
        series: [
          {
            name: '登录人数',
            data: [1500, 2200, 1800, 2500, 2100, 2800],
            type: 'bar',
            barWidth: '40%',
            label: {
              show: true,
              position: 'top',
              color: '#fff',
              fontSize: 11
            },
            itemStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: 'rgba(131, 191, 246, 0.8)' },
                { offset: 1, color: 'rgba(24, 141, 240, 0.6)' }
              ]),
              borderRadius: [4, 4, 0, 0]
            }
          },
          {
            name: '注册人数',
            data: [800, 1200, 900, 1400, 1100, 1600],
            type: 'bar',
            barWidth: '20%',
            barGap: '-100%',
            label: {
              show: true,
              position: 'top',
              color: '#fff',
              fontSize: 11,
              offset: [0, -15]
            },
            itemStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: 'rgba(186, 144, 255, 0.9)' },
                { offset: 1, color: 'rgba(147, 112, 219, 0.7)' }
              ]),
              borderRadius: [4, 4, 0, 0]
            }
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
