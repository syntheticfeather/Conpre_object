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
        <div class="title-content">
          <span>{{ currentProvince || '全国' }}</span>数据大屏
          <el-button class="back-btn" @click="goBack" size="small">
            返回
          </el-button>
        </div>
        <dv-decoration2 :dur="2" style="width:200px; height:5px;" />
      </div>

      <!-- 图表区域 -->
      <div class="chart-container">
        <!-- 左侧图表列 -->
        <div class="chart-panel left">
          <!-- 每月注册/登录人数-堆叠柱状图-登录人数包括注册人数 -->
          <RegisterChart ref="register" />

          <!-- 每月申请与放款趋势-堆叠面积图+折线 -->
          <AuditChart ref="audit" />

          <!-- 用户生命周期活跃度-堆叠条形图 -->
          <OnlineChart ref="online" />

          <!-- 贷款申请状态分布-3D立体环形图 -->
          <LoanChart ref="loan" />
        </div>
        
        <!-- 右侧图表列 -->
        <div class="chart-panel right">
          <!-- 资金用途分类-南丁格尔玫瑰图 -->
          <PurposeChart ref="purpose" />

          <!-- 资金流入流出-三线波浪图 -->
          <AmountChart ref="amount" />

          <!-- 用户状态分布-待定环图 -->
          <StatusChart ref="status" />

          <!-- 还款方式偏好-正负对比柱状图 -->
          <RepayChart ref="repay" />
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
import { onBeforeUnmount, onMounted, ref, reactive } from "vue"
import { useRouter } from 'vue-router'
import useFileLoader from "@/hooks/useFileLoader.js"
import useCoord from "@/hooks/useCoord.js"
import useConversionStandardData from "@/hooks/useConversionStandardData.js"
import useCountryMesh from "@/hooks/useCountryMesh.js"
import useMap3DCamera from "@/hooks/useMap3DCamera.js"
import useMapInteraction from "@/hooks/useMapInteraction.js"
import RegisterChart from "@/components/risk/charts/RegisterChart.vue"
import AuditChart from "@/components/risk/charts/AuditChart.vue"
import OnlineChart from "@/components/risk/charts/OnlineChart.vue"
import LoanChart from "@/components/risk/charts/LoanChart.vue"
import PurposeChart from "@/components/risk/charts/PurposeChart.vue"
import StatusChart from "@/components/risk/charts/StatusChart.vue"
import AmountChart from "@/components/risk/charts/AmountChart.vue"
import RepayChart from "@/components/risk/charts/RepayChart.vue"

let centerXY = [106.59893798828125, 26.918846130371094]

const chartRefs = {
  register: ref(null),
  audit: ref(null),
  online: ref(null),
  loan: ref(null),
  purpose: ref(null),
  status: ref(null),
  amount: ref(null),
  repay: ref(null)
}

const handleResize = () => {
  chartRefs.register.value?.resize()
  chartRefs.audit.value?.resize()
  chartRefs.online.value?.resize()
  chartRefs.loan.value?.resize()
  chartRefs.purpose.value?.resize()
  chartRefs.status.value?.resize()
  chartRefs.amount.value?.resize()
  chartRefs.repay.value?.resize()
}

export default {
  name: "3dMap",
  components: {
    RegisterChart,
    AuditChart,
    OnlineChart,
    LoanChart,
    PurposeChart,
    StatusChart,
    AmountChart,
    RepayChart
  },
  setup() {
    const router = useRouter()
    let baseEarth = null
    const loading = ref(true) // 加载状态
    const cityName = ref('测试')
    const currentProvince = ref('') // 当前选中的省份
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

    // 更新省份名称
    const updateProvinceName = (name) => {
      currentProvince.value = name
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
          // 相机移动动画相关
          this.isMoving = false
          this.moveStartTime = 0
          this.moveDuration = 1500
          this.startPos = null
          this.targetPos = null
          this.startLook = null
          this.targetLook = null
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
          
          // 初始化 Composables 并挂载到实例上
          this.cameraControl = useMap3DCamera(this, centerXY)
          this.mapInteraction = useMapInteraction(this, this.handleProvinceClick.bind(this), () => {
            // 重置标题为"全国"
            if (this.updateProvinceName) {
              this.updateProvinceName('')
            }
          })
          
          // 使用 Composables 初始化事件监听
          this.mapInteraction.initClickEvent(this.container)
          this.mapInteraction.initRightClickReset(this.container, this.cameraControl.resetCamera.bind(this.cameraControl))
        }
        
        // 处理省份点击
        handleProvinceClick(center, provinceName) {
          if (this.cameraControl) {
            this.cameraControl.moveToProvince(center)
          }
          // 更新省份名称
          if (provinceName) {
            this.updateProvinceName(provinceName)
          }
        }
        
        // 重置相机到初始位置
        resetCamera() {
          console.log('重置相机位置')
          
          // 保存当前相机状态
          const currentPos = this.camera.position.clone()
          
          // 设置目标位置（地图中心）
          const targetPos = {
            x: 100,  // 初始 X 位置
            y: -3,   // 初始 Y 位置
            z: 40    // 初始 Z 位置
          }
          
          const targetLook = {
            x: centerXY[0],
            y: centerXY[1],
            z: 0
          }
          
          // 保存起始位置
          this.startPos = currentPos
          this.startLook = this.controls.target.clone()
          this.targetPos = targetPos
          this.targetLook = targetLook
          
          // 设置动画参数
          this.isMoving = true
          this.moveStartTime = performance.now()
        }
        
        // 初始化点击事件
        initClickEvent() {
          this.container.addEventListener('click', (event) => {
            // 获取容器的边界框
            const rect = this.container.getBoundingClientRect()
            // 计算鼠标在标准化设备坐标中的位置（-1 到 +1）
            this.mouse.x = ((event.clientX - rect.left) / this.options.width) * 2 - 1
            this.mouse.y = -((event.clientY - rect.top) / this.options.height) * 2 + 1
            
            // 更新射线投射器
            this.raycaster.setFromCamera(this.mouse, this.camera)
            
            // 检测与地图组中对象的交集
            let intersects = []
            if (this.mapGroup) {
              intersects = this.raycaster.intersectObjects(this.mapGroup.children, true)
            }
            
            if (intersects.length > 0) {
              // 按距离排序，取最近的
              intersects.sort((a, b) => a.distance - b.distance)
              
              for (let i = 0; i < intersects.length; i++) {
                let object = intersects[i].object
                
                // 跳过边界线对象，只处理省份 Mesh
                if (object.type === 'LineSegments' || object.type === 'Line' || object.type === 'LineLoop') {
                  continue
                }
                
                // 检查是否是省份网格
                if (object.type === 'Mesh' && object.userData && object.userData.hoverColor !== undefined) {
                  // 获取省份名称
                  const provinceName = object.name || '未知省份'
                  
                  // 获取省份中心点（使用包围盒）
                  const box = new THREE.Box3().setFromObject(object)
                  const center = box.getCenter(new THREE.Vector3())
                  
                  // 移动到省份中心
                  this.moveToProvince(center, provinceName)
                  
                  break
                }
              }
            }
          })
        }
        
        // 相机移动到指定省份
        moveToProvince(target) {
          if (!this.camera || !this.controls) {
            return
          }
          
          // 计算目标相机位置
          const currentPos = this.camera.position.clone()
          
          // 计算从省份中心到相机的偏移向量
          const offset = new THREE.Vector3(
            currentPos.x - centerXY[0],
            currentPos.y - centerXY[1],
            30
          )
          
          // 目标相机位置 = 省份中心 + 偏移
          // 增加 Z 轴高度让相机上升，同时扩大 X/Y 偏移让相机后移
          this.targetPos = {
            x: target.x + offset.x * 0.5,  // 从 0.3 增加到 0.5，后移
            y: target.y + offset.y * 0.5,  // 从 0.3 增加到 0.5，后移
            z: 35                          // 从 25 增加到 35，上升
          }
          
          // 目标观察点就是省份中心
          this.targetLook = {
            x: target.x,
            y: target.y,
            z: 0
          }
          
          // 保存起始位置
          this.startPos = currentPos
          this.startLook = this.controls.target.clone()
          
          // 设置动画参数
          this.isMoving = true
          this.moveStartTime = performance.now()
        }
        initLight() {
          //   平行光 1
          let directionalLight1 = new THREE.DirectionalLight(0x7af4ff, 1)
          directionalLight1.position.set(...centerXY, 30)
          //   平行光 2
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
          
          // 处理相机移动动画（使用 Composable）
          if (this.cameraControl) {
            this.cameraControl.updateCamera()
          }
          
          // 这里是你自己业务上需要的 code
          this.renderer.render(this.scene, this.camera)
          // 控制相机旋转缩放的更新
          if (this.options.controls.visibel && this.controls) {
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
          // 更新 TWEEN 动画（必须传入时间参数）
          TWEEN.update(performance.now())
        }
        resize() {
          super.resize()
          // 这里是你自己业务上需要的 code
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
      // 绑定更新省份名称的方法到 baseEarth 实例
      baseEarth.updateProvinceName = updateProvinceName
      baseEarth.run()
      loading.value = false // 加载完成
      window.addEventListener("resize", resize)
    })
    onMounted(() => {
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
    })

    // 返回风险管理页面
    const goBack = () => {
      router.push('/dashboard/risk')
    }

    return {
      loading,
      cityName,
      currentProvince,
      cityValue,
      conf,
      addData,
      goBack
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
  transform: perspective(1000px) rotateY(-15deg);
  transform-origin: right center;
}
.chart-panel.left {
  transform: perspective(1000px) rotateY(15deg);
  transform-origin: left center;
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

.title-content {
  display: flex;
  align-items: center;
  gap: 20px;
}

.back-btn {
  background: rgba(0, 224, 255, 0.2) !important;
  border: 1px solid rgba(0, 224, 255, 0.5) !important;
  color: #00e0ff !important;
  font-size: 12px !important;
  padding: 4px 12px !important;
  cursor: pointer;
  transition: all 0.3s ease;
}

.back-btn:hover {
  background: rgba(0, 224, 255, 0.3) !important;
  border-color: #00e0ff !important;
  box-shadow: 0 0 10px rgba(0, 224, 255, 0.5);
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
