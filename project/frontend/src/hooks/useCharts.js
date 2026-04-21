import * as echarts from 'echarts'

/**
 * ECharts 图表管理 Composable
 * @returns {Object} 图表管理方法
 */
export default function useCharts() {
  // 存储所有图表实例
  const chartInstances = new Map()

  /**
   * 初始化图表
   * @param {HTMLElement} ref - 图表容器 DOM 元素
   * @param {Object} option - 图表配置项
   * @returns {Object} 图表实例
   */
  const initChart = (ref, option) => {
    if (!ref) {
      console.warn('图表容器 ref 为空')
      return null
    }

    const chart = echarts.init(ref)
    chart.setOption(option)
    chartInstances.set(ref, chart)
    return chart
  }

  /**
   * 获取图表实例
   * @param {HTMLElement} ref - 图表容器 DOM 元素
   * @returns {Object} 图表实例
   */
  const getChart = (ref) => {
    return chartInstances.get(ref)
  }

  /**
   * 调整所有图表大小
   */
  const resizeAll = () => {
    chartInstances.forEach((chart) => {
      try {
        chart.resize()
      } catch (error) {
        console.error('调整图表大小失败:', error)
      }
    })
  }

  /**
   * 销毁所有图表实例
   */
  const disposeAll = () => {
    chartInstances.forEach((chart) => {
      try {
        chart.dispose()
      } catch (error) {
        console.error('销毁图表失败:', error)
      }
    })
    chartInstances.clear()
  }

  /**
   * 销毁单个图表实例
   * @param {HTMLElement} ref - 图表容器 DOM 元素
   */
  const disposeChart = (ref) => {
    const chart = chartInstances.get(ref)
    if (chart) {
      try {
        chart.dispose()
      } catch (error) {
        console.error('销毁图表失败:', error)
      }
      chartInstances.delete(ref)
    }
  }

  return {
    initChart,
    getChart,
    resizeAll,
    disposeAll,
    disposeChart
  }
}
