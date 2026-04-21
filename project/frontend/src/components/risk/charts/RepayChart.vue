<template>
  <div ref="chartRef" class="chart-item"></div>
</template>

<script>
import * as echarts from 'echarts'
import { onMounted, onBeforeUnmount, ref } from 'vue'

export default {
  name: 'RepayChart',
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
          top: '12%',
          bottom: '12%',
          containLabel: true
        },
        title: {
          text: '还款方式偏好',
          textStyle: {
            color: '#fff',
            fontSize: 16
          },
          left: 'center'
        },
        xAxis: {
          type: 'category',
          data: ['等额本息', '等额本金', '先息后本', '按月付息', '一次性还本'],
          axisLabel: {
            color: '#fff',
            interval: 0,
            rotate: 30
          },
          axisLine: {
            lineStyle: {
              color: 'rgba(255, 255, 255, 0.3)'
            }
          }
        },
        yAxis: {
          type: 'value',
          name: '选择人数',
          nameTextStyle: {
            color: '#fff',
            fontSize: 12
          },
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
          },
          formatter: function(params) {
            const data = params[0]
            const seriesData = [450, 320, 180, 250, 150]
            const total = seriesData.reduce((a, b) => a + b, 0)
            const percentage = ((data.value / total) * 100).toFixed(1)
            
            let result = data.axisValue + '<br/>'
            result += '<span style="color:' + data.color + '">●</span> 选择人数: ' + data.value + '人<br/>'
            result += '占比: ' + percentage + '%<br/>'
            result += '<hr style="margin: 5px 0; border: none; border-top: 1px solid rgba(255,255,255,0.3);">'
            result += '总样本: ' + total + '人'
            
            return result
          }
        },
        series: [
          {
            name: '选择人数',
            data: [450, 320, 180, 250, 150],
            type: 'bar',
            barWidth: '40%',
            label: {
              show: true,
              position: 'top',
              color: '#fff',
              fontSize: 12
            },
            itemStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: '#667eea' },
                { offset: 1, color: '#764ba2' }
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
