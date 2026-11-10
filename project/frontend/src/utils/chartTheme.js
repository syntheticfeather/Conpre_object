/**
 * ECharts 主题令牌
 *
 * 图表画在 canvas 上，读不到 CSS 变量，
 * 所以这里维护与 base.css 语义一致的少量颜色，供 setOption 使用。
 * 只放图表真正会用到的颜色，不要复制整份 CSS 变量表。
 */
const CHART_THEMES = {
  light: {
    text: '#303133',
    textSecondary: '#606266',
    axisLine: '#dcdfe6',
    splitLine: '#e4e7ed',
    splitArea: ['#f8f9fa', '#ffffff'],
    tooltipBg: '#ffffff',
    tooltipBorder: '#e4e7ed',
    tooltipText: '#303133'
  },
  dark: {
    text: '#e8e8ea',
    textSecondary: '#a8abb2',
    axisLine: '#3a3a52',
    splitLine: '#2b2b45',
    splitArea: ['#1f1f33', '#1a1a2e'],
    tooltipBg: '#232338',
    tooltipBorder: '#3a3a52',
    tooltipText: '#e8e8ea'
  }
}

let currentTheme = 'light'
const listeners = new Set()

/**
 * 获取当前主题下的图表颜色
 * @param {'light'|'dark'} theme 不传则取最近一次 setChartTheme 的结果
 */
export function getChartTheme(theme = currentTheme) {
  return CHART_THEMES[theme] || CHART_THEMES.light
}

/**
 * 由 app store 在切换主题时调用，通知所有图表重新渲染
 */
export function setChartTheme(theme) {
  currentTheme = theme === 'dark' ? 'dark' : 'light'
  listeners.forEach((callback) => callback(currentTheme))
}

/**
 * 订阅主题变化，返回取消订阅函数
 */
export function onChartThemeChange(callback) {
  listeners.add(callback)
  return () => listeners.delete(callback)
}
