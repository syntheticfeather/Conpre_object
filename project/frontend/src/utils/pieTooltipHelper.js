/**
 * 环状图表 Tooltip 位置辅助函数
 * 根据鼠标位置动态调整 tooltip 显示位置，避免遮挡数据
 * 
 * @param {Array} point - 鼠标位置 [x, y]
 * @param {Object} params - ECharts 参数对象
 * @param {HTMLElement} dom - tooltip DOM 元素
 * @param {Object} rect - 图表区域信息
 * @param {Object} size - 图表尺寸信息 {viewSize: [width, height], contentSize: [width, height]}
 * @returns {Array} tooltip 位置 [x, y]
 */
export function getTooltipPosition(point, params, dom, rect, size) {
  // 图表中心点
  const chartCenterX = size.viewSize[0] / 2
  const chartCenterY = size.viewSize[1] / 2
  
  // 计算 tooltip 的宽高
  const tooltipWidth = dom.offsetWidth
  const tooltipHeight = dom.offsetHeight
  
  // 边距
  const margin = 10
  
  // 根据鼠标位置判断象限，动态调整 tooltip 位置
  if (point[0] < chartCenterX && point[1] < chartCenterY) {
    // 左上象限 - tooltip 显示在鼠标左上
    return [point[0] - tooltipWidth - margin, point[1] - tooltipHeight - margin]
  } else if (point[0] < chartCenterX && point[1] >= chartCenterY) {
    // 左下象限 - tooltip 显示在鼠标左下
    return [point[0] - tooltipWidth - margin, point[1] + margin]
  } else if (point[0] >= chartCenterX && point[1] >= chartCenterY) {
    // 右下象限 - tooltip 显示在鼠标右下
    return [point[0] + margin, point[1] + margin]
  } else {
    // 右上象限 - tooltip 显示在鼠标右上
    return [point[0] + margin, point[1] - tooltipHeight - margin]
  }
}

/**
 * 创建环图 tooltip 配置
 * 
 * @param {number} total - 总数，用于计算百分比
 * @returns {Object} tooltip 配置对象
 */
export function createPieTooltipConfig(total = 0) {
  return {
    trigger: 'item',
    backgroundColor: 'rgba(0, 0, 0, 0.7)',
    borderColor: 'rgba(255, 255, 255, 0.2)',
    borderWidth: 1,
    textStyle: {
      color: '#fff'
    },
    formatter: function(params) {
      const percentage = total > 0 ? ((params.value / total) * 100).toFixed(2) : 0
      return `${params.name}: ${params.value} (${percentage}%)`
    },
    position: getTooltipPosition
  }
}

export default {
  getTooltipPosition,
  createPieTooltipConfig
}
