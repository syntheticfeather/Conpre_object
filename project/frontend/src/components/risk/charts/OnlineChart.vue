<template>
  <div ref="chartRef" class="chart-item"></div>
</template>

<script>
import * as echarts from 'echarts'
import { onMounted, onBeforeUnmount, ref } from 'vue'

export default {
  name: 'OnlineChart',
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
          text: '用户生命周期活跃度',
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
          // name: '活跃人数',
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
            let result = params[0].axisValue + '<br/>'
            let total = 0
            params.forEach(item => {
              result += item.marker + item.seriesName + ': ' + item.value + '人<br/>'
              total += item.value
            })
            result += '<hr style="margin: 5px 0; border: none; border-top: 1px solid rgba(255,255,255,0.3);"/>'
            result += '总活跃人数: ' + total + '人'
            return result
          }
        },
        series: [
          {
            name: '注册1月内',
            data: [1200, 1500, 1800, 1400, 1600, 1900],
            type: 'bar',
            stack: 'total',
            label: {
              show: true,
              position: 'insideTop',
              color: '#fff',
              fontSize: 11,
              formatter: '{c}'
            },
            itemStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: 'rgba(173, 216, 230, 0.9)' },
                { offset: 1, color: 'rgba(135, 206, 250, 0.7)' }
              ]),
            }
          },
          {
            name: '注册一年内',
            data: [2500, 2800, 3200, 3000, 3100, 3500],
            type: 'bar',
            stack: 'total',
            label: {
              show: true,
              position: 'insideTop',
              color: '#fff',
              fontSize: 11,
              formatter: '{c}'
            },
            itemStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: 'rgba(100, 149, 237, 0.9)' },
                { offset: 1, color: 'rgba(70, 130, 180, 0.7)' }
              ]),
            }
          },
          {
            name: '注册一年以上',
            data: [1800, 2000, 2200, 2400, 2300, 2500],
            type: 'bar',
            stack: 'total',
            label: {
              show: true,
              position: 'insideTop',
              color: '#fff',
              fontSize: 11,
              formatter: '{c}'
            },
            itemStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: 'rgba(25, 25, 112, 0.9)' },
                { offset: 1, color: 'rgba(0, 0, 128, 0.7)' }
              ]),
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
  border-radius: 12px;
  padding: 0px;
  color: white;
  pointer-events: auto;
}
</style>
