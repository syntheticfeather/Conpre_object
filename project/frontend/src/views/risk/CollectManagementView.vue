<template>
  <div class="header">
    催收管理
  </div>

  <!-- 1. 添加 ref="chartRef" 用于获取 DOM 元素 -->
  <div ref="chartRef" class="main"></div>

  <div class="user-manage-view"></div>
</template>

<script setup>
import { ref, onMounted , onBeforeUnmount } from 'vue'
import * as echarts from 'echarts'

const chartRef = ref(null)
let myChart = null

onMounted(async () => {
  if (!chartRef.value) return

  myChart = echarts.init(chartRef.value)

  // 加载地图数据
  try {
    // 使用本地文件
    const response = await fetch('/maps/China.geojson')

    const geoJsonData = await response.json()
    
    // 注册地图
    echarts.registerMap('china', geoJsonData)
  } catch (error) {
    console.error('地图数据加载失败:', error)
    return;
  }

  // 配置项
  const option = {
    // 背景色（可选）
    backgroundColor: '#fff',
    series: [
      {
        name: '中国地图',
        type: 'map',
        map: 'china',
        // 5. 基础样式配置
        itemStyle: {
          areaColor: '#eee',
          borderColor: '#333'
        },
        // 6. 鼠标悬停样式
        emphasis: {
          itemStyle: {
            areaColor: '#ccc'
          }
        }
      }
    ]
  };

  // 设置配置项
  myChart.setOption(option);

  // 7. 重要：添加窗口大小监听，防止窗口缩放后图表错乱
  window.addEventListener('resize', resizeChart)
});

// 组件销毁时清理实例和事件监听
onBeforeUnmount(() => {
  if (myChart) {
    window.removeEventListener('resize', resizeChart)
    myChart.dispose() // 释放资源
  }
})

const resizeChart = () => {
  if (myChart) {
    myChart.resize()
  }
}
</script>

<style scoped>
/* 8. 确保容器有固定尺寸 */
.main {
  width: 100%;
  height: 600px; /* 建议设置具体高度，或通过 flex 布局撑开 */
  margin-top: 20px;
}
</style>