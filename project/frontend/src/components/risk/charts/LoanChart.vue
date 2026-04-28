<template>
  <div ref="chartRef" class="chart-item"></div>
</template>

<script>
import * as echarts from 'echarts'
import 'echarts-gl'
import { onMounted, onBeforeUnmount, ref } from 'vue'

function getParametricEquation(startRatio, endRatio, isSelected, isHovered, k, height, radiusScale = 0.7) {
  let midRatio = (startRatio + endRatio) / 2
  let startRadian = startRatio * Math.PI * 2
  let endRadian = endRatio * Math.PI * 2
  let midRadian = midRatio * Math.PI * 2

  if (startRatio === 0 && endRatio === 1) {
    isSelected = false
  }

  k = typeof k !== 'undefined' ? k : 1 / 3
  let offsetX = isSelected ? Math.cos(midRadian) * 0.1 : 0
  let offsetY = isSelected ? Math.sin(midRadian) * 0.1 : 0
  let hoverRate = isHovered ? 1.05 : 1

  return {
    u: {
      min: -Math.PI,
      max: Math.PI * 3,
      step: Math.PI / 32,
    },
    v: {
      min: 0,
      max: Math.PI * 2,
      step: Math.PI / 20,
    },
    x: function (u, v) {
      if (u < startRadian) {
        return offsetX + Math.cos(startRadian) * (1 + Math.cos(v) * k) * hoverRate * radiusScale
      }
      if (u > endRadian) {
        return offsetX + Math.cos(endRadian) * (1 + Math.cos(v) * k) * hoverRate * radiusScale
      }
      return offsetX + Math.cos(u) * (1 + Math.cos(v) * k) * hoverRate * radiusScale
    },
    y: function (u, v) {
      if (u < startRadian) {
        return offsetY + Math.sin(startRadian) * (1 + Math.cos(v) * k) * hoverRate * radiusScale
      }
      if (u > endRadian) {
        return offsetY + Math.sin(endRadian) * (1 + Math.cos(v) * k) * hoverRate * radiusScale
      }
      return offsetY + Math.sin(u) * (1 + Math.cos(v) * k) * hoverRate * radiusScale
    },
    z: function (u, v) {
      if (u < startRadian) {
        return Math.sin(v) > 0 ? 1 * height : -1
      }
      if (u > endRadian) {
        return Math.sin(v) > 0 ? 1 * height : -1
      }
      return Math.sin(v) > 0 ? 1 * height : -1
    },
  }
}

function getPie3D(pieData, internalDiameterRatio, radiusScale = 0.8) {
  let series = []
  let sumValue = 0
  let startValue = 0
  let endValue = 0
  let maxValue = 0
  let k = typeof internalDiameterRatio !== 'undefined'
    ? (1 - internalDiameterRatio) / (1 + internalDiameterRatio)
    : 1 / 3

  for (let i = 0; i < pieData.length; i++) {
    sumValue += pieData[i].value
    if (pieData[i].value > maxValue) {
      maxValue = pieData[i].value
    }
    let seriesItem = {
      name: typeof pieData[i].name === 'undefined' ? `series${i}` : pieData[i].name,
      type: 'surface',
      parametric: true,
      wireframe: { show: false },
      pieData: pieData[i],
      pieStatus: {
        selected: false,
        hovered: false,
        k: k,
      },
    }

    if (typeof pieData[i].itemStyle !== 'undefined') {
      let itemStyle = {}
      if (typeof pieData[i].itemStyle.color !== 'undefined') {
        itemStyle.color = pieData[i].itemStyle.color
      }
      if (typeof pieData[i].itemStyle.opacity !== 'undefined') {
        itemStyle.opacity = pieData[i].itemStyle.opacity
      }
      seriesItem.itemStyle = itemStyle
    }
    series.push(seriesItem)
  }

  for (let i = 0; i < series.length; i++) {
    endValue = startValue + series[i].pieData.value
    series[i].pieData.startRatio = startValue / sumValue
    series[i].pieData.endRatio = endValue / sumValue
    let normalizedHeight = (series[i].pieData.value / maxValue) * 10000
    series[i].parametricEquation = getParametricEquation(
      series[i].pieData.startRatio,
      series[i].pieData.endRatio,
      false,
      false,
      k,
      normalizedHeight,
      radiusScale,
    )
    startValue = endValue
  }

  return series
}

export default {
  name: 'LoanChart',
  setup() {
    const chartRef = ref(null)
    let chartInstance = null

    const data = [
      { value: 120, name: 'AI通过', itemStyle: { color: '#f4f7f7' } },
      { value: 80, name: '人工通过', itemStyle: { color: '#aacfd0' } },
      { value: 50, name: '待审核', itemStyle: { color: '#79a8a9' } },
      { value: 20, name: '人工拒绝', itemStyle: { color: '#1f4e5f' } },
    ]

    onMounted(() => {
      if (!chartRef.value) return

      chartInstance = echarts.init(chartRef.value)

      const series = getPie3D(data, 0.5)

      series.push({
        name: 'pie2d',
        type: 'pie',
        label: {
          show: true,
          opacity: 1,
          fontSize: 11,
          lineHeight: 14,
          color: '#fff',
          formatter: '{b}: {c}个',
        },
        labelLine: {
          show: true,
          length: 15,
          length2: 20,
          lineStyle: {
            color: 'rgba(255, 255, 255, 0.6)',
            width: 1,
          },
        },
        startAngle: -30,
        clockwise: false,
        radius: ['30%', '65%'],
        center: ['50%', '50%'],
        data: data,
        itemStyle: {
          opacity: 0,
        },
      })

      let chartOption = {
        legend: {
          show: true,
          orient: 'vertical',
          data: data.map(item => item.name),
          left: 'left',
          top: '',
          itemGap: 8,
          itemHeight: 10,
          itemWidth: 14,
          textStyle: {
            color: 'rgba(255, 255, 255, 0.8)',
            fontSize: 13,
          },
        },
        tooltip: {
          formatter: (params) => {
            if (params.seriesName !== 'mouseoutSeries' && params.seriesName !== 'pie2d') {
              return `${params.seriesName}<br/><span style="display:inline-block;margin-right:5px;border-radius:10px;width:10px;height:10px;background-color:${params.color};"></span>${chartOption.series[params.seriesIndex].pieData.value} 笔`
            }
          },
          backgroundColor: 'rgba(0, 0, 0, 0.7)',
          borderColor: 'rgba(255, 255, 255, 0.2)',
          borderWidth: 1,
          textStyle: {
            color: '#fff',
            fontSize: 11,
          },
        },
        xAxis3D: {
          min: -1,
          max: 1,
        },
        yAxis3D: {
          min: -1,
          max: 1,
        },
        zAxis3D: {
          min: -3,
          max: 4,
        },
        grid3D: {
          show: false,
          boxHeight: 0.01,
          viewControl: {
            distance: 160,
            alpha: 30,
            beta: 40,
            autoRotate: false,
          },
        },
        series: series,
      }

      chartInstance.setOption(chartOption)
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
      resize,
    }
  },
}
</script>

<style scoped>
.chart-item {
  width: 100%;
  height: 200px;
  background: rgba(123, 166, 194, 0.5);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  padding: 0px;
  color: white;
  pointer-events: auto;
}
</style>
