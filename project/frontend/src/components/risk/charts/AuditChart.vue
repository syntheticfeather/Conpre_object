<template>
  <div ref="chartRef" class="chart-item"></div>
</template>

<script>
import * as echarts from 'echarts'
import { onMounted, onBeforeUnmount, ref } from 'vue'

export default {
  name: 'AuditChart',
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
          top: '18%',
          bottom: '8%',
          containLabel: true
        },
        title: {
          text: '每月申请与放款趋势',
          textStyle: {
            color: '#fff',
            fontSize: 16
          },
          left: 'center'
        },
        legend: {
          data: ['贷款申请数量', '通过申请数量'],
          top: '5%',
          textStyle: {
            color: '#fff',
            fontSize: 12
          }
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
          name: '申请数量（个）',
          nameTextStyle: {
            color: '#fff',
            fontSize: 12
          },
          axisLabel: {
            color: '#fff',
            formatter: (value) => value + '个'
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
          backgroundColor: 'rgba(0, 0, 0, 0.7)',
          borderColor: 'rgba(255, 255, 255, 0.2)',
          borderWidth: 1,
          textStyle: {
            color: '#fff'
          },
          formatter: function(params) {
            let result = params[0].axisValue + '<br/>'
            let totalApply = 0
            let totalLoan = 0
            
            params.forEach(item => {
              if (item.seriesName === '贷款申请数量') {
                result += '<span style="color:' + item.color + '">●</span> ' + item.seriesName + ': ' + item.value + '个<br/>'
                totalApply = item.value
              } else if (item.seriesName === '通过申请数量') {
                result += '<span style="color:' + item.color + '">●</span> ' + item.seriesName + ': ' + item.value + '个<br/>'
                totalLoan = item.value
              }
            })
            
            const approvalRate = totalApply > 0 ? ((totalLoan / totalApply) * 100).toFixed(1) : 0
            result += '<hr style="margin: 5px 0; border: none; border-top: 1px solid rgba(255,255,255,0.3);">'
            result += '通过率: ' + approvalRate + '%'
            
            return result
          }
        },
        series: [
          {
            name: '贷款申请数量',
            data: [80, 120, 95, 150, 110, 180],
            type: 'bar',
            stack: 'total',
            label: {
              show: true,
              position: 'top',
              color: '#fff',
              fontSize: 12,
              formatter: (params) => {
                return (params.value / 1).toFixed(0) + ''
              }
            },
            itemStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: 'rgba(255, 154, 158, 0.8)' },
                { offset: 1, color: 'rgba(250, 208, 196, 0.8)' }
              ])
            }
          },
          {
            name: '通过申请数量',
            data: [60, 90, 72, 110, 85, 140],
            type: 'line',
            smooth: true,
            label: {
              show: true,
              position: 'top',
              color: '#fff',
              fontSize: 12,
              formatter: (params) => {
                return (params.value / 1).toFixed(0) + ''
              }
            },
            lineStyle: {
              color: '#0096ff',
              width: 3
            },
            itemStyle: {
              color: '#0096ff'
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
