<template>
  <!-- 共用父容器，设置 relative 定位，作为层叠上下文的基准 -->
  <div class="dv-screen-view">

    <!-- 3D Canvas 容器：绝对定位，铺满父容器，z-index 较低 -->
    <div id="canvas-container" class="canvas-container">
      <div id="app-32-map" class="is-full"></div>
        <!-- Loading 加载 -->
      <dv-loading v-if="loading" :loading="loading">加载中...</dv-loading>
    </div>
    
    <!-- 2D UI 容器：绝对定位，同样铺满，z-index 较高，pointer-events 控制穿透 -->
    <div class="ui-overlay">
      <!-- 顶部标题栏 -->
       <!-- 横向装饰线（默认从左向右流动） -->
      <div class="deco-horizontal top-bar">
        <div >数据大屏</div>
        <dv-decoration2 :dur="2" style="width:200px; height:5px;" />
      </div>

      <!-- 图表区域 -->
      <div class="chart-container">
        <!-- 左侧图表列 -->
        <div class="chart-panel left">
          <!-- 每月注册人数柱状图 -->
          <div ref="registerChart" class="chart-item"></div>
          <!-- 用户风险等级环图 -->
          <div ref="riskChart" class="chart-item"></div>
          <!-- 在线人数波浪图 -->
          <div ref="onlineChart" class="chart-item"></div>
          <!-- 每月贷款申请数量锥形柱图 -->
          <div ref="loanChart" class="chart-item"></div>
        </div>
        
        <!-- 右侧图表列 -->
        <div class="chart-panel right">
          <!-- 贷款申请用途胶囊柱图 -->
          <div ref="purposeChart" class="chart-item"></div>
          <!-- 用户状态环图 -->
          <div ref="statusChart" class="chart-item"></div>
          <!-- 每月总放款金额/总还款金额双线波浪图 -->
          <div ref="amountChart" class="chart-item"></div>
          <!-- 数据集柱状图 -->
          <div ref="datasetChart" class="chart-item"></div>
        </div>
      </div>

      <!-- 底部小导航 -->
      <div class="bottom-nav">
        <div class="nav-item">
          <span text-white>风险分析</span>
        </div>
      </div>

    </div>
  </div>
</template>

<script>
import Map3d from "@/utils/Map3d.js"
import TWEEN from "@tweenjs/tween.js"
import * as THREE from "three"
import * as echarts from "echarts"
import { onBeforeUnmount, onMounted, ref, reactive } from "vue"
import useFileLoader from "@/hooks/useFileLoader.js"
import useCoord from "@/hooks/useCoord.js"
import useConversionStandardData from "@/hooks/useConversionStandardData.js"
import useCountryMesh from "@/hooks/useCountryMesh.js"

let centerXY = [106.59893798828125, 26.918846130371094]

// 图表引用
const registerChart = ref(null)
const riskChart = ref(null)
const onlineChart = ref(null)
const loanChart = ref(null)
const purposeChart = ref(null)
const statusChart = ref(null)
const amountChart = ref(null)
const datasetChart = ref(null)

// 图表实例
let registerChartInstance = null
let riskChartInstance = null
let onlineChartInstance = null
let loanChartInstance = null
let purposeChartInstance = null
let statusChartInstance = null
let amountChartInstance = null
let datasetChartInstance = null

// 初始化每月注册人数柱状图
const initRegisterChart = () => {
  if (!registerChart.value) return
  registerChartInstance = echarts.init(registerChart.value)
  const option = {
    grid: {
      left: '3%',
      right: '4%',
      top: '15%',
      bottom: '3%',
      containLabel: true
    },
    title: {
      text: '每月注册人数',
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
      }
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        color: '#fff'
      },
      maxInterval: 1500
    },
    series: [{
      data: [1200, 1900, 1500, 2100, 1800, 2500],
      type: 'bar',
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#83bff6' },
          { offset: 0.5, color: '#188df0' },
          { offset: 1, color: '#188df0' }
        ])
      }
    }]
  }
  registerChartInstance.setOption(option)
}

// 初始化用户风险等级环图
const initRiskChart = () => {
  if (!riskChart.value) return
  riskChartInstance = echarts.init(riskChart.value)
  const option = {
    title: {
      text: '用户风险等级',
      textStyle: {
        color: '#fff',
      },
      left: 'center'
    },
    tooltip: {
      trigger: 'item'
    },
    legend: {
      orient: 'vertical',
      left: 'left',
      textStyle: {
        color: '#fff'
      }
    },
    series: [{
      name: '风险等级',
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: {
        borderRadius: 5,
        borderColor: '#000',
        borderWidth: 2
      },
      label: {
        show: false,
        position: 'center'
      },
      emphasis: {
        label: {
          show: true,
          fontSize: '18',
          fontWeight: 'bold',
          color: '#fff'
        }
      },
      labelLine: {
        show: false
      },
      data: [
        { value: 300, name: '低风险' },
        { value: 200, name: '中风险' },
        { value: 100, name: '高风险' },
        { value: 50, name: '极高风险' }
      ]
    }]
  }
  riskChartInstance.setOption(option)
}

// 初始化在线人数波浪图
const initOnlineChart = () => {
  if (!onlineChart.value) return
  onlineChartInstance = echarts.init(onlineChart.value)
  const option = {
    grid: {
      left: '3%',
      right: '4%',
      top: '15%',
      bottom: '3%',
      containLabel: true
    },
    title: {
      text: '在线人数',
      textStyle: {
        color: '#fff'
      },
      left: 'center'
    },
    xAxis: {
      type: 'category',
      data: ['0 时', '4 时', '8 时', '12 时', '16 时', '20 时'],
      axisLabel: {
        color: '#fff'
      }
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        color: '#fff'
      }
    },
    series: [{
      data: [300, 400, 800, 1200, 1000, 600],
      type: 'line',
      smooth: true,
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(0, 150, 255, 0.5)' },
          { offset: 1, color: 'rgba(0, 150, 255, 0.1)' }
        ])
      },
      lineStyle: {
        color: '#0096ff'
      },
      itemStyle: {
        color: '#0096ff'
      }
    }]
  }
  onlineChartInstance.setOption(option)
}

// 初始化每月贷款申请数量锥形柱图
const initLoanChart = () => {
  if (!loanChart.value) return
  loanChartInstance = echarts.init(loanChart.value)
  const option = {
    grid: {
      left: '3%',
      right: '4%',
      top: '15%',
      bottom: '3%',
      containLabel: true
    },
    title: {
      text: '每月贷款申请数量',
      textStyle: {
        color: '#fff'
      },
      left: 'center'
    },
    xAxis: {
      type: 'category',
      data: ['1 月', '2 月', '3 月', '4 月', '5 月', '6 月'],
      axisLabel: {
        color: '#fff'
      }
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        color: '#fff'
      }
    },
    series: [{
      data: [120, 190, 150, 210, 180, 250],
      type: 'bar',
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#ff9a9e' },
          { offset: 1, color: '#fad0c4' }
        ]),
        borderRadius: [4, 4, 0, 0]
      }
    }]
  }
  loanChartInstance.setOption(option)
}

// 初始化贷款申请用途胶囊柱图
const initPurposeChart = () => {
  if (!purposeChart.value) return
  purposeChartInstance = echarts.init(purposeChart.value)
  const option = {
    grid: {
      left: '3%',
      right: '4%',
      top: '15%',
      bottom: '3%',
      containLabel: true
    },
    title: {
      text: '贷款申请用途',
      textStyle: {
        color: '#fff'
      },
      left: 'center'
    },
    xAxis: {
      type: 'category',
      data: ['消费', '教育', '医疗', '旅游', '其他'],
      axisLabel: {
        color: '#fff'
      }
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        color: '#fff'
      }
    },
    series: [{
      data: [300, 200, 150, 100, 50],
      type: 'bar',
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#a8edea' },
          { offset: 1, color: '#fed6e3' }
        ]),
        borderRadius: [20, 20, 20, 20]
      }
    }]
  }
  purposeChartInstance.setOption(option)
}

// 初始化用户状态环图
const initStatusChart = () => {
  if (!statusChart.value) return
  statusChartInstance = echarts.init(statusChart.value)
  const option = {
    title: {
      text: '用户状态',
      textStyle: {
        color: '#fff'
      },
      left: 'center'
    },
    tooltip: {
      trigger: 'item'
    },
    legend: {
      orient: 'vertical',
      left: 'left',
      textStyle: {
        color: '#fff'
      }
    },
    series: [{
      name: '用户状态',
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: {
        borderRadius: 5,
        borderColor: '#000',
        borderWidth: 2
      },
      label: {
        show: false,
        position: 'center'
      },
      emphasis: {
        label: {
          show: true,
          fontSize: '18',
          fontWeight: 'bold',
          color: '#fff'
        }
      },
      labelLine: {
        show: false
      },
      data: [
        { value: 300, name: '还款中' },
        { value: 150, name: '申请贷款中' },
        { value: 400, name: '正常' },
        { value: 80, name: '逾期' },
        { value: 20, name: '黑名单' }
      ]
    }]
  }
  statusChartInstance.setOption(option)
}

// 初始化每月总放款金额/总还款金额双线波浪图
const initAmountChart = () => {
  if (!amountChart.value) return
  amountChartInstance = echarts.init(amountChart.value)
  const option = {
    grid: {
      left: '3%',
      right: '4%',
      top: '15%',
      bottom: '3%',
      containLabel: true
    },
    title: {
      text: '每月放款/还款金额',
      textStyle: {
        color: '#fff'
      },
      left: 'center'
    },
    xAxis: {
      type: 'category',
      data: ['1 月', '2 月', '3 月', '4 月', '5 月', '6 月'],
      axisLabel: {
        color: '#fff'
      }
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        color: '#fff'
      }
    },
    series: [
      {
        name: '放款金额',
        data: [1200000, 1900000, 1500000, 2100000, 1800000, 2500000],
        type: 'line',
        smooth: true,
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(255, 99, 132, 0.5)' },
            { offset: 1, color: 'rgba(255, 99, 132, 0.1)' }
          ])
        },
        lineStyle: {
          color: '#ff6384'
        },
        itemStyle: {
          color: '#ff6384'
        }
      },
      {
        name: '还款金额',
        data: [1000000, 1600000, 1300000, 1800000, 1500000, 2200000],
        type: 'line',
        smooth: true,
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(75, 192, 192, 0.5)' },
            { offset: 1, color: 'rgba(75, 192, 192, 0.1)' }
          ])
        },
        lineStyle: {
          color: '#4bc0c0'
        },
        itemStyle: {
          color: '#4bc0c0'
        }
      }
    ]
  }
  amountChartInstance.setOption(option)
}

// 初始化数据集柱状图
const initDatasetChart = () => {
  if (!datasetChart.value) return
  datasetChartInstance = echarts.init(datasetChart.value)
  const option = {
    grid: {
      left: '3%',
      right: '4%',
      top: '15%',
      bottom: '3%',
      containLabel: true
    },
    title: {
      text: '每月申请审核情况',
      textStyle: {
        color: '#fff'
      },
      left: 'center'
    },
    xAxis: {
      type: 'category',
      data: ['1 月', '2 月', '3 月', '4 月', '5 月', '6 月'],
      axisLabel: {
        color: '#fff'
      }
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        color: '#fff'
      }
    },
    series: [
      {
        name: '待审核',
        data: [120, 190, 150, 210, 180, 250],
        type: 'bar',
        itemStyle: {
          color: '#ff9800'
        }
      },
      {
        name: 'AI 通过',
        data: [100, 160, 130, 180, 150, 220],
        type: 'bar',
        itemStyle: {
          color: '#4caf50'
        }
      },
      {
        name: '人工拒绝',
        data: [20, 30, 20, 30, 30, 30],
        type: 'bar',
        itemStyle: {
          color: '#f44336'
        }
      }
    ]
  }
  datasetChartInstance.setOption(option)
}

// 自适应 resize
const handleResize = () => {
  registerChartInstance?.resize()
  riskChartInstance?.resize()
  onlineChartInstance?.resize()
  loanChartInstance?.resize()
  purposeChartInstance?.resize()
  statusChartInstance?.resize()
  amountChartInstance?.resize()
  datasetChartInstance?.resize()
}

export default {
  name: "3dMap",
  setup() {
    let baseEarth = null
    const loading = ref(true) // 加载状态
    const cityName = ref('测试')
    const cityValue = ref(58)
    const conf = reactive({
      lineWidth: 24,
      digitalFlopStyle: {
        fill: 'pink',
      },
      data: [
        {
          name: '杭州',
          value: 98,
        },
        {
          name: '金华',
          value: 150,
        },
        {
          name: '宁波',
          value: 62,
        },
        {
          name: '太原',
          value: 54,
        },
      ],
    })
    const addData = () => {
      if (!cityName.value || !cityValue.value)
        return

      conf.data.push({
        name: cityName.value,
        value: parseInt(cityValue.value),
      })
    }

    // 重置
    const resize = () => {
      baseEarth.resize()
    }

    const { requestData } = useFileLoader()
    const { transfromGeoJSON } = useConversionStandardData()
    const { getBoundingBox } = useCoord()
    const { generateMap } = useCountryMesh()
    
    onMounted(async () => {
      // 中国数据
      let provinceData = null
      try {
        provinceData = await requestData("/maps/China.geojson")
        if (!provinceData || !provinceData.features) {
          console.error("地图数据加载失败或格式错误")
          return
        }
        provinceData = transfromGeoJSON(provinceData)
        console.log("地图数据加载成功，共", provinceData.features.length, "个省份")
      } catch (error) {
        console.error("加载地图数据失败:", error)
        return
      }

      // 地图类继承
      class CurrentMap3d extends Map3d {
        constructor(props) {
          super(props)
          this.particleArr = []
          this.rotatingApertureMesh = null
          this.rotatingPointMesh = null
          this.css2dRender = null
        }
        initCamera() {
          let { width, height } = this.options
          let rate = width / height
          // 设置 45°的透视相机，更符合人眼观察
          this.camera = new THREE.PerspectiveCamera(45, rate, 0.001, 90000000)
          this.camera.up.set(0, 0, 1)
          // 中国地图相机位置
          this.camera.position.set(100, -3, 40) //相机在 Three.js 坐标系中的位置
          this.camera.lookAt(...centerXY, 0)
        }
        initModel() {
          try {
            // 使用 useCountryMesh 的 generateMap 方法生成网格（包含白色边线）
            const meshArr = generateMap(provinceData)

            // 创建地图组
            this.mapGroup = new THREE.Group()

            // 将所有省份网格添加到地图组
            meshArr.forEach((mesh) => {
              this.mapGroup.add(mesh)
            })

            // 计算包围盒并更新中心点坐标
            let earthGroupBound = getBoundingBox(this.mapGroup)
            centerXY = [earthGroupBound.center.x, earthGroupBound.center.y]
            
            console.log('地图包围盒信息:', {
              center: earthGroupBound.center,
              size: earthGroupBound.size,
              minX: earthGroupBound.box3.min.x,
              maxX: earthGroupBound.box3.max.x,
              minY: earthGroupBound.box3.min.y,
              maxY: earthGroupBound.box3.max.y
            })

            // 将地图组添加到场景中
            this.scene.add(this.mapGroup)
            
            // 更新相机目标点和控制器目标
            if (this.controls) {
              this.controls.target.set(centerXY[0], centerXY[1], 0)
            }
            this.camera.lookAt(centerXY[0], centerXY[1], 0)

            console.log('地图模型初始化成功，共', meshArr.length, '个省份')
          } catch (error) {
            console.error("初始化地图模型失败:", error)
            console.error("错误堆栈:", error.stack)
          }
        }
        getDataRenderMap() {}

        destroy() {}
        initControls() {
          super.initControls()
          this.controls.target = new THREE.Vector3(...centerXY, 0)
        }
        initLight() {
          //   平行光1
          let directionalLight1 = new THREE.DirectionalLight(0x7af4ff, 1)
          directionalLight1.position.set(...centerXY, 30)
          //   平行光2
          let directionalLight2 = new THREE.DirectionalLight(0x7af4ff, 1)
          directionalLight2.position.set(...centerXY, 30)
          // 环境光
          let ambientLight = new THREE.AmbientLight(0x7af4ff, 1)
          // 将光源添加到场景中
          this.addObject(directionalLight1)
          this.addObject(directionalLight2)
          this.addObject(ambientLight)
        }
        initRenderer() {
          super.initRenderer()
          // this.renderer.outputEncoding = THREE.sRGBEncoding
        }
        loadBackground(backgroundPath) {
          const textureLoader = new THREE.TextureLoader()
          textureLoader.load(
            backgroundPath,
            (texture) => {
              const { width, height } = this.options
              const aspect = width / height

              const planeGeometry = new THREE.PlaneGeometry(500, 500 / aspect)
              const planeMaterial = new THREE.MeshBasicMaterial({
                map: texture,
                side: THREE.DoubleSide,
                transparent: true,
                depthWrite: false,        // 禁止深度写入，避免 Z-fighting
              })

              const backgroundPlane = new THREE.Mesh(planeGeometry, planeMaterial)

              // 使用世界坐标（墨卡托投影后的坐标）而不是经纬度值
              const worldCenter = new THREE.Vector3(...centerXY, 0)
              // Z 轴设置为地图底部下方（地图从 Z=0 到 Z=3，背景放在 Z=-0.1）
              backgroundPlane.position.set(worldCenter.x, worldCenter.y, -0.1)

              // 将背景添加到场景中（不添加到 mapGroup），避免跟随旋转导致震颤
              this.scene.add(backgroundPlane)

              this.backgroundPlane = backgroundPlane
              console.log('背景图片加载成功:', backgroundPath)
            },
            undefined,
            (error) => {
              console.warn('背景图片加载失败，使用纯色背景:', error)
              this.renderer.setClearColor(this.options.bgColor || 0xffffff, 1)
            }
          )
        }
        loop() {
          this.animationStop = window.requestAnimationFrame(() => {
            this.loop()
          })
          // 调用父类的 handleMouseHover 处理鼠标悬浮
          if (typeof this.handleMouseHover === 'function') {
            this.handleMouseHover()
          }
          // 这里是你自己业务上需要的code
          this.renderer.render(this.scene, this.camera)
          // 控制相机旋转缩放的更新
          if (this.options.controls.visibel && this.controls) {
            // this.controls.target.set(...centerXY, 0)
            this.controls.update()
          }
          // 统计更新
          if (this.options.statsVisibel) this.stats.update()
          if (this.rotatingApertureMesh) {
            this.rotatingApertureMesh.rotation.z += 0.0005
          }
          if (this.rotatingPointMesh) {
            this.rotatingPointMesh.rotation.z -= 0.0005
          }
          // 渲染标签
          if (this.css2dRender) {
            this.css2dRender.render(this.scene, this.camera)
          }
          // 粒子上升
          if (this.particleArr && this.particleArr.length) {
            for (let i = 0; i < this.particleArr.length; i++) {
              this.particleArr[i].updateSequenceFrame()
              this.particleArr[i].position.z += 0.01
              if (this.particleArr[i].position.z >= 6) {
                this.particleArr[i].position.z = -6
              }
            }
          }
          TWEEN.update()
          // console.log(this.camera.position)
        }
        resize() {
          super.resize()
          // 这里是你自己业务上需要的code
          this.renderer.render(this.scene, this.camera)
          this.renderer.setPixelRatio(window.devicePixelRatio)

          if (this.css2dRender) {
            this.css2dRender.setSize(this.options.width, this.options.height)
          }
        }
      }
      baseEarth = new CurrentMap3d({
        container: "#app-32-map",
        axesVisibel: false, // 隐藏辅助坐标轴
        controls: {
          enableDamping: false,  // 关闭阻尼，消除地图震颤
          autoRotate: false,     // 关闭自动旋转
          maxPolarAngle: (Math.PI / 2) * 0.98,
        },
      })
      baseEarth.run()
      loading.value = false // 加载完成
      window.addEventListener("resize", resize)
    })
    onMounted(() => {
      initRegisterChart()
      initRiskChart()
      initOnlineChart()
      initLoanChart()
      initPurposeChart()
      initStatusChart()
      initAmountChart()
      initDatasetChart()
      window.addEventListener('resize', handleResize)
    })

    onBeforeUnmount(() => {
      window.removeEventListener("resize", resize)
      window.removeEventListener('resize', handleResize)
      // 销毁 WebGL 实例
      if (baseEarth) {
        baseEarth.destroy()
        baseEarth = null
      }
      // 销毁图表实例
      registerChartInstance?.dispose()
      riskChartInstance?.dispose()
      onlineChartInstance?.dispose()
      loanChartInstance?.dispose()
      purposeChartInstance?.dispose()
      statusChartInstance?.dispose()
      amountChartInstance?.dispose()
      datasetChartInstance?.dispose()
      registerChartInstance = null
      riskChartInstance = null
      onlineChartInstance = null
      loanChartInstance = null
      purposeChartInstance = null
      statusChartInstance = null
      amountChartInstance = null
      datasetChartInstance = null
    })

    return {
      loading,
      cityName,
      cityValue,
      conf,
      addData,
      registerChart,
      riskChart,
      onlineChart,
      loanChart,
      purposeChart,
      statusChart,
      amountChart,
      datasetChart
    }
  },
}
</script>

<style scoped>
.dv-platform-view {
  display: flex;
  flex-direction: column;
  position: relative;      /* 创建层叠上下文 */
  width: 100%;
  height: 100vh;
  overflow: hidden;

  width: 100%;
  height: 100%;
  background-color: #ffffff;
  overflow: hidden;
}

/* 3D 画布容器：铺满且位于底层 */
.canvas-container {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 1;             /* 较低层级 */
}
.is-full {
  width: 100%;
  height: 100%;
}

/* UI 覆盖层：与 canvas 容器完全重叠，但层级更高 */
.ui-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 10;            /* 高于 canvas */
  pointer-events: none;   /* 默认让鼠标事件穿透，由内部可交互元素自行开启 */
  
  display: flex;
  flex-direction: column;
  justify-content: space-around;
}
.chart-container {
  display: flex;
  justify-content: space-between;
  flex: 1;
  padding:0 10px;
  padding-top: 20px;
}
.chart-panel {
  display: flex;
  flex-direction: column;
  gap: 20px;
  width: 28%;
  min-height: 700px;
}
.chart-panel.right {
  transform: perspective(1000px) rotateY(-10deg);
  transform-origin: right center;
}
.chart-panel.left {
  transform: perspective(1000px) rotateY(10deg);
  transform-origin: left center;
}
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

/* 具体的 UI 模块：需要响应鼠标事件的，重置 pointer-events */
.top-bar,
.left-panel,
.right-panel,
.loading-indicator {
  pointer-events: auto;   /* 使这些区域可以点击、悬停 */
}

/* 标题栏悬浮在顶部 */
.top-bar {
  position: absolute;
  top: 20px;
  left: 50%;
  transform: translateX(-50%);

  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  
  padding: 10px 30px;

  background: rgba(55, 71, 106, 0.6);
  color: #00e0ff;
  font-size: 24px;
  font-family: '方正小标宋', sans-serif;
  backdrop-filter: blur(5px);
  border: 1px solid rgba(78, 127, 243, 0.3);
  box-shadow: 0 0 20px rgba(106, 129, 132, 0.2);
  border-radius: 3px;

  z-index: 11;    
  pointer-events: auto; 
}

/* Loading 居中 */
.loading-indicator {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: rgba(0, 0, 0, 0.7);
  color: white;
  padding: 20px 40px;
  border-radius: 8px;
  pointer-events: auto;
}


.dv-decoration2 {
  margin-top: 20px;
}

</style>
