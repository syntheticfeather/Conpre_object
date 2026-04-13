import * as THREE from 'three';
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls';
import Stats from 'three/examples/jsm/libs/stats.module';
import TWEEN from '@tweenjs/tween.js';
import { deepMerge, isType } from '@/utils';

export default class Map3d {
  constructor(options = {}) {
    let defaultOptions = {
      isFull: true,
      container: null,
      width: window.innerWidth,
      height: window.innerHeight,
      background: '/bg1.jpg',    // 背景图片路径
      bgColor: 0xffffff,         // 场景背景色（白色）
      backgroundFollowMap: false, // 背景是否跟随地图旋转
      materialColor: 0xff0000,
      controls: {
        visibel: true, // 是否开启
        enableDamping: true, // 阻尼
        dampingFactor: 0.5,  // 阻尼系数（越大停止越快）
        rotateSpeed: 0.5,     // 旋转速度
        zoomSpeed: 0.8,       // 缩放速度
        panSpeed: 0.5,        // 平移速度
        autoRotate: false, // 自动旋转
        maxPolarAngle: Math.PI, // 相机垂直旋转角度的上限
      },
      statsVisibel: true,
      axesVisibel: false, // 隐藏辅助坐标轴
      axesHelperSize: 250, // 左边尺寸
    };
    this.options = deepMerge(defaultOptions, options);
    this.container = document.querySelector(this.options.container);
    this.options.width = this.container.offsetWidth;
    this.options.height = this.container.offsetHeight;
    this.scene = new THREE.Scene(); // 场景
    this.camera = null; // 相机
    this.renderer = null; // 渲染器
    this.mesh = null; // 网格
    this.animationStop = null; // 用于停止动画
    this.controls = null; // 轨道控制器
    this.stats = null; // 统计
    
    // 鼠标交互相关
    this.raycaster = new THREE.Raycaster();
    this.mouse = new THREE.Vector2();
    this.hoveredObject = null;
    this.tooltip = null; // 悬浮提示框

    this.init();
  }
  init() {
    this.initStats();
    this.initCamera();
    this.initModel();
    this.initRenderer();
    this.initLight();
    // 只在需要时初始化坐标轴
    if (this.options.axesVisibel) {
      this.initAxes();
    }
    this.initControls();
    this.initMouseEvents(); // 添加鼠标事件
    console.log('WebGL 渲染器初始化成功', this.renderer.info);
    this.loop(); // 调用 loop 方法而不是 animate
  }
  async initModel() {}

  /**
   * 运行
   */
  run() {
    this.loop();
  }
  // 循环渲染
  loop() {
    this.animationStop = window.requestAnimationFrame(() => {
      this.loop()
    })
    // 处理鼠标悬浮
    this.handleMouseHover();
    // 这里是你自己业务上需要的code
    this.renderer.render(this.scene, this.camera)
    // 控制相机旋转缩放的更新
    if (this.options.controls.visibel) this.controls.update()
    // 统计更新
    if (this.options.statsVisibel) this.stats.update()

    // TWEEN.update() 已弃用，使用 TWEEN.update(time) 替代
    TWEEN.update(performance.now())
  }
  initCamera() {
    let { width, height } = this.options;
    let rate = width / height;
    // 设置45°的透视相机,更符合人眼观察
    this.camera = new THREE.PerspectiveCamera(45, rate, 0.1, 1500)
    this.camera.position.set(270.27, 173.24, 257.54)

    this.camera.lookAt(0, 0, 0)
  }
  /**
   * 初始化渲染器
   */
  initRenderer() {
    let { width, height, bgColor, background } = this.options
    
    // 清理容器中的旧 canvas
    while (this.container && this.container.firstChild) {
      this.container.removeChild(this.container.firstChild)
    }
    
    let renderer = new THREE.WebGLRenderer({
      antialias: true, // 锯齿
    });
    // 设置canvas的分辨率
    renderer.setPixelRatio(window.devicePixelRatio)
    // 设置canvas 的尺寸大小
    renderer.setSize(width, height)
    // 设置背景色
    renderer.setClearColor(bgColor || 0xffffff, 1)
    // 插入到dom中
    this.container.appendChild(renderer.domElement)
    this.renderer = renderer;

    // 加载背景图片
    if (background) {
      this.loadBackground(background)
    }
  }

  /**
   * 加载背景图片
   */
  loadBackground(backgroundPath) {
    const textureLoader = new THREE.TextureLoader();
    textureLoader.load(
      backgroundPath,
      (texture) => {
        // 创建背景平面
        const { width, height } = this.options;
        const aspect = width / height;
        
        // 创建平面几何体（足够大以覆盖整个视野）
        const planeGeometry = new THREE.PlaneGeometry(500, 500 / aspect);
        
        // 创建材质
        const planeMaterial = new THREE.MeshBasicMaterial({
          map: texture,
          side: THREE.DoubleSide,
          transparent: true,
        });
        
        // 创建网格
        const backgroundPlane = new THREE.Mesh(planeGeometry, planeMaterial);
        
        // 设置位置（在相机后方）
        backgroundPlane.position.set(0, 0, -100);
        
        // 添加到场景
        this.scene.add(backgroundPlane);
        
        // 保存引用，以便后续清理
        this.backgroundPlane = backgroundPlane;
        
        console.log('背景图片加载成功:', backgroundPath);
      },
      undefined,
      (error) => {
        // 加载失败，使用纯色背景
        console.warn('背景图片加载失败，使用纯色背景:', error);
        this.renderer.setClearColor(this.options.bgColor || 0xffffff, 1);
      }
    );
  }
  initLight() {
    //   平行光1
    let directionalLight1 = new THREE.DirectionalLight(0xffffff, 0.6);
    directionalLight1.position.set(400, 200, 200);
    //   平行光2
    let directionalLight2 = new THREE.DirectionalLight(0xffffff, 0.6);
    directionalLight2.position.set(-400, -200, -300);
    // 环境光
    let ambientLight = new THREE.AmbientLight(0xffffff, 0.5);
    // 将光源添加到场景中
    this.addObject(directionalLight1);
    this.addObject(directionalLight2);
    this.addObject(ambientLight);
  }

  initStats() {
    if (!this.options.statsVisibel) return false;
    this.stats = new Stats();
    this.container.appendChild(this.stats.dom);
  }
  initControls() {
    try {
      let {
        controls: { enableDamping, autoRotate, visibel, maxPolarAngle, dampingFactor, rotateSpeed, zoomSpeed, panSpeed },
      } = this.options;
      if (!visibel) return false;
      // 轨道控制器，使相机围绕目标进行轨道运动（旋转|缩放|平移）
      this.controls = new OrbitControls(this.camera, this.renderer.domElement);
      this.controls.maxPolarAngle = maxPolarAngle;
      this.controls.autoRotate = autoRotate;
      this.controls.enableDamping = enableDamping;
      this.controls.dampingFactor = dampingFactor || 0.25;
      this.controls.rotateSpeed = rotateSpeed || 0.5;
      this.controls.zoomSpeed = zoomSpeed || 0.8;
      this.controls.panSpeed = panSpeed || 0.5;
    } catch (error) {
      console.log(error);
    }
  }
  initAxes() {
    if (!this.options.axesVisibel) return false;
    var axes = new THREE.AxesHelper(this.options.axesHelperSize);
    this.addObject(axes);
  }
  
  /**
   * 初始化鼠标事件
   */
  initMouseEvents() {
    // 创建 tooltip 元素
    this.createTooltip();
    
    // 鼠标移动事件
    this.container.addEventListener('mousemove', (event) => {
      // 获取容器的边界框，考虑容器的偏移量
      const rect = this.container.getBoundingClientRect();
      // 计算鼠标在标准化设备坐标中的位置（-1 到 +1）
      this.mouse.x = ((event.clientX - rect.left) / this.options.width) * 2 - 1;
      this.mouse.y = -((event.clientY - rect.top) / this.options.height) * 2 + 1;
      
      // 更新 tooltip 位置
      if (this.tooltip) {
        this.tooltip.style.left = (event.clientX + 15) + 'px';
        this.tooltip.style.top = (event.clientY + 15) + 'px';
      }
    });
    
    // 鼠标离开事件
    this.container.addEventListener('mouseleave', () => {
      if (this.tooltip) {
        this.tooltip.style.display = 'none';
      }
    });
  }
  
  /**
   * 创建悬浮提示框
   */
  createTooltip() {
    // 检查是否已存在
    if (document.querySelector('.map-tooltip')) {
      this.tooltip = document.querySelector('.map-tooltip');
      return;
    }
    
    const tooltip = document.createElement('div');
    tooltip.className = 'map-tooltip';
    tooltip.style.cssText = `
      position: fixed;
      background: rgba(0, 0, 0, 0.8);
      color: white;
      padding: 8px 12px;
      border-radius: 4px;
      font-size: 14px;
      pointer-events: none;
      z-index: 9999;
      display: none;
      white-space: nowrap;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
    `;
    document.body.appendChild(tooltip);
    this.tooltip = tooltip;
  }
  
  /**
   * 显示悬浮提示
   */
  showTooltip(text) {
    if (this.tooltip) {
      this.tooltip.textContent = text;
      this.tooltip.style.display = 'block';
    }
  }
  
  /**
   * 隐藏悬浮提示
   */
  hideTooltip() {
    if (this.tooltip) {
      this.tooltip.style.display = 'none';
    }
  }
  
  /**
   * 处理鼠标悬浮
   */
  handleMouseHover() {
    // 更新射线投射器
    this.raycaster.setFromCamera(this.mouse, this.camera);

    // 检测与地图组中对象的交集（如果有 mapGroup）
    let intersects = [];
    
    // 优先检测 mapGroup（如果存在）
    if (this.mapGroup) {
      intersects = this.raycaster.intersectObjects(this.mapGroup.children, true);
    } else {
      // 否则检测整个场景
      intersects = this.raycaster.intersectObjects(this.scene.children, true);
    }

    // 重置之前悬浮的对象
    if (this.hoveredObject) {
      // 恢复原始颜色
      if (this.hoveredObject.userData && this.hoveredObject.userData.originalColor !== undefined) {
        const material = this.hoveredObject.material;
        if (Array.isArray(material)) {
          // 材质数组：恢复所有材质的颜色
          material.forEach(mat => {
            if (mat && mat.color) mat.color.setHex(this.hoveredObject.userData.originalColor);
          });
        } else if (material && material.color) {
          // 单一材质
          material.color.setHex(this.hoveredObject.userData.originalColor);
        }
      }
      // 恢复原始缩放
      this.hoveredObject.scale.set(1, 1, 1);
      this.hideTooltip();
      this.hoveredObject = null;
    }

    // 检测是否有新的对象被悬浮
    if (intersects.length > 0) {
      // 按距离排序，取最近的
      intersects.sort((a, b) => a.distance - b.distance);
      
      for (let i = 0; i < intersects.length; i++) {
        let object = intersects[i].object;

        // 跳过边界线对象（LineSegments, Line, LineLoop），只处理省份 Mesh
        if (object.type === 'LineSegments' || object.type === 'Line' || object.type === 'LineLoop') {
          continue;
        }

        // 检查是否是省份网格（必须有 userData 和 hoverColor）
        if (object.type === 'Mesh' && object.userData && object.userData.hoverColor !== undefined) {
          this.hoveredObject = object;

          // 更改颜色为悬停颜色
          const material = object.material;
          if (Array.isArray(material)) {
            // 材质数组：更改所有材质的颜色
            material.forEach(mat => {
              if (mat && mat.color) mat.color.setHex(object.userData.hoverColor);
            });
          } else if (material && material.color) {
            // 单一材质
            material.color.setHex(object.userData.hoverColor);
          }

          // 增加厚度（Z 轴缩放）
          this.hoveredObject.scale.set(1, 1, 1.33);
          
          // 显示省份名称 tooltip
          const provinceName = object.name || object.userData.name || '未知省份';
          this.showTooltip(provinceName);
          
          break;
        }
      }
    }
  }

  // 清空dom
  empty(elem) {
    while (elem && elem.lastChild) elem.removeChild(elem.lastChild);
  }
  /**
   * 添加对象到场景
   * @param {*} object  {} []
   */
  addObject(object) {
    if (isType('Array', object)) {
      this.scene.add(...object);
    } else {
      this.scene.add(object);
    }
  }
  /**
   * 移除对象
   * @param {*} object {} []
   */
  removeObject(object) {
    if (isType('Array', object)) {
      object.map((item) => {
        item.geometry.dispose();
      });
      this.scene.remove(...object);
    } else {
      object.geometry.dispose();
      this.scene.remove(object);
    }
  }
  /**
   * 重置
   */
  resize() {
    // 重新设置宽高

    this.options.width = this.container.innerWidth || window.innerWidth;
    this.options.height = this.container.innerHeight || window.innerHeight;

    this.renderer.setSize(this.options.width, this.options.height);
    // 重新设置相机的位置
    let rate = this.options.width / this.options.height;

    // 必須設置相機的比例，重置的時候才不会变形
    this.camera.aspect = rate;

    // 渲染器执行render方法的时候会读取相机对象的投影矩阵属性projectionMatrix
    // 但是不会每渲染一帧，就通过相机的属性计算投影矩阵(节约计算资源)
    // 如果相机的一些属性发生了变化，需要执行updateProjectionMatrix ()方法更新相机的投影矩阵
    this.camera.updateProjectionMatrix();
  }
  /**
   * 销毁实例
   */
  destroy() {
    // 停止动画循环
    if (this.animationStop) {
      window.cancelAnimationFrame(this.animationStop);
      this.animationStop = null;
    }
    
    // 清理场景中的所有对象
    if (this.scene) {
      this.scene.traverse((object) => {
        if (object.geometry) {
          object.geometry.dispose();
        }
        if (object.material) {
          if (Array.isArray(object.material)) {
            object.material.forEach((material) => {
              if (material.map) material.map.dispose();
              material.dispose();
            });
          } else {
            if (object.material.map) object.material.map.dispose();
            object.material.dispose();
          }
        }
      });
      this.scene.clear();
    }
    
    // 清理渲染器
    if (this.renderer) {
      this.renderer.dispose();
      this.renderer.forceContextLoss();
      this.renderer.domElement = null;
      this.renderer = null;
    }
    
    // 清理容器中的 canvas
    if (this.container) {
      this.empty(this.container);
    }
    
    // 清理控制器
    if (this.controls) {
      this.controls.dispose();
      this.controls = null;
    }
    
    // 清理统计面板
    if (this.stats && this.stats.dom) {
      this.stats.dom.remove();
    }
    
    this.camera = null;
    this.scene = null;
    this.mesh = null;
  }
}
