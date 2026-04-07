<template>
  <div class="header">
    用户地区分布
  </div>

  <div class="user-manage-view">
    <div ref="chartRef" class="main"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import * as echarts from 'echarts'
import 'echarts-gl'

const chartRef = ref(null)
let myChart = null

onMounted(async () => {
  if (!chartRef.value) return

  myChart = echarts.init(chartRef.value)

  // 加载地图数据
  try {
    const response = await fetch('/maps/China.geojson')
    const geoJsonData = await response.json()
    
    // 注册地图
    echarts.registerMap('china', geoJsonData)
  } catch (error) {
    console.error('地图数据加载失败:', error)
    return
  }

  // 3D 地球配置
  const option = {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'item',
      formatter: function(params) {
        return `<div style="padding: 8px; background: rgba(0,0,0,0.8); border-radius: 4px; color: #fff; font-size: 14px;">
                  <strong>${params.name}</strong>
                </div>`
      },
      backgroundColor: 'rgba(0, 0, 0, 0.8)',
      borderColor: '#00ffff',
      textStyle: {
        color: '#fff',
        fontSize: 14
      }
    },
    series: [
      {
        type: 'map3D',
        map: 'china',
        name: '中国地图',
        data: [],
        shading: 'realistic', // 使用真实渲染，增强 3D 效果
        silent: false, // 允许交互
        itemStyle: {
          areaColor: '#1a1f3c',
          borderColor: '#00ffff',
          borderWidth: 1.5
        },
        emphasis: {
          itemStyle: {
            areaColor: '#2a333d',
            color: '#00ffff' // 悬浮时边框更亮
          },
          label: {
            show: true,
            color: '#000', // 悬浮时文字颜色
            fontSize: 16,
            formatter: '{b}'
          }
        },
        select: {
          itemStyle: {
            areaColor: '#3a434d'
          },
          label: {
            show: true,
            color: '#00ff0',
            fontSize: 18
          }
        },
        label: {
          show: false,
          color: '#fff',
          fontSize: 12
        },
        viewControl: {
          projection: 'perspective',
          autoRotate: false,
          autoRotateSpeed: 2,
          distance: 180,
          minDistance: 100,
          maxDistance: 300,
          rotateSensitivity: 1,
          zoomSensitivity: 1,
          panSensitivity: 0.5
        },
        regionHeight: 15,
        // 添加光照效果让 3D 效果更明显
        light: {
          ambient: {
            intensity: 0.6
          },
          main: {
            intensity: 0.8,
            shadow: true
          }
        }
      }
    ]
  }

  myChart.setOption(option)

  window.addEventListener('resize', resizeChart)
})

onBeforeUnmount(() => {
  if (myChart) {
    window.removeEventListener('resize', resizeChart)
    myChart.dispose()
  }
})

const resizeChart = () => {
  if (myChart) {
    myChart.resize()
  }
}
</script>

<style scoped>
.main {
  min-height: 600px;
  margin-top: 20px;
}
</style>
