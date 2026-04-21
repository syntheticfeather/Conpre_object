<template>
  <div ref="chartRef" class="chart-item"></div>
</template>

<script>
import * as echarts from 'echarts'
import { onMounted, onBeforeUnmount, ref } from 'vue'

export default {
  name: 'AmountChart',
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
          bottom: '8%',
          containLabel: true
        },
        title: {
          text: '资金流入流出',
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
        yAxis: [
          {
            type: 'value',
            name: '流入/流出',
            nameTextStyle: {
              color: '#fff',
              fontSize: 12
            },
            axisLabel: {
              color: '#fff',
              formatter: (value) => (value / 10000).toFixed(0) + '万'
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
          {
            type: 'value',
            name: '差值',
            nameTextStyle: {
              color: '#fff',
              fontSize: 12
            },
            position: 'right',
            axisLabel: {
              color: '#0096ff',
              formatter: (value) => (value / 10000).toFixed(0) + '万'
            },
            axisLine: {
              lineStyle: {
                color: '#0096ff'
              }
            },
            splitLine: {
              show: false
            }
          }
        ],
        tooltip: {
          trigger: 'axis',
          backgroundColor: 'rgba(0, 0, 0, 0.7)',
          borderColor: 'rgba(255, 255, 255, 0.2)',
          borderWidth: 1,
          textStyle: {
            color: '#fff'
          },
          formatter: (params) => {
            let result = params[0].axisValue + '<br/>'
            params.forEach(item => {
              result += item.marker + item.seriesName + ': ' + (item.value / 10000).toFixed(0) + '万<br/>'
            })
            return result
          }
        },
        series: [
          {
            name: '资金流入',
            data: [500000, 600000, 450000, 700000, 550000, 800000],
            type: 'line',
            smooth: true,
            symbol: 'circle',
            symbolSize: 8,
            lineStyle: {
              width: 3,
              color: '#4caf50'
            },
            itemStyle: {
              color: '#4caf50'
            },
            areaStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: 'rgba(76, 175, 80, 0.5)' },
                { offset: 1, color: 'rgba(76, 175, 80, 0.05)' }
              ])
            }
          },
          {
            name: '资金流出',
            data: [300000, 400000, 350000, 500000, 450000, 600000],
            type: 'line',
            smooth: true,
            symbol: 'circle',
            symbolSize: 8,
            lineStyle: {
              width: 3,
              color: '#f44336'
            },
            itemStyle: {
              color: '#f44336'
            },
            areaStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: 'rgba(244, 67, 54, 0.5)' },
                { offset: 1, color: 'rgba(244, 67, 54, 0.05)' }
              ])
            }
          },
          {
            name: '差值',
            data: [200000, 200000, 100000, 200000, 100000, 200000],
            type: 'line',
            smooth: true,
            symbol: 'circle',
            symbolSize: 6,
            yAxisIndex: 1,
            lineStyle: {
              width: 2,
              color: '#0096ff',
              type: 'dashed'
            },
            itemStyle: {
              color: '#0096ff'
            },
            areaStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: 'rgba(0, 150, 255, 0.3)' },
                { offset: 1, color: 'rgba(0, 150, 255, 0.02)' }
              ])
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
