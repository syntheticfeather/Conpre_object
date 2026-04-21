import * as THREE from 'three'

/**
 * 3D 地图交互 Composable
 * @param {Object} map3dInstance - Map3D 实例
 * @param {Function} onProvinceClick - 省份点击回调
 * @param {Function} onResetTitle - 重置标题回调（可选）
 * @returns {Object} 交互方法
 */
export default function useMapInteraction(map3dInstance, onProvinceClick, onResetTitle) {
  // 鼠标和射线投射器
  const mouse = new THREE.Vector2()
  const raycaster = new THREE.Raycaster()

  /**
   * 初始化左键点击事件
   * @param {HTMLElement} container - 容器元素
   */
  const initClickEvent = (container) => {
    container.addEventListener('click', (event) => {
      // 获取容器的边界框
      const rect = container.getBoundingClientRect()
      
      // 计算鼠标在标准化设备坐标中的位置（-1 到 +1）
      mouse.x = ((event.clientX - rect.left) / map3dInstance.options.width) * 2 - 1
      mouse.y = -((event.clientY - rect.top) / map3dInstance.options.height) * 2 + 1

      // 更新射线投射器
      raycaster.setFromCamera(mouse, map3dInstance.camera)

      // 检测与地图组中对象的交集
      let intersects = []
      if (map3dInstance.mapGroup) {
        intersects = raycaster.intersectObjects(map3dInstance.mapGroup.children, true)
      }

      if (intersects.length > 0) {
        // 按距离排序，取最近的
        intersects.sort((a, b) => a.distance - b.distance)

        for (let i = 0; i < intersects.length; i++) {
          const object = intersects[i].object

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

            // 调用回调函数
            if (onProvinceClick) {
              onProvinceClick(center, provinceName)
            }

            break
          }
        }
      }
    })
  }

  /**
   * 初始化右键重置事件
   * @param {HTMLElement} container - 容器元素
   * @param {Function} onReset - 重置回调
   */
  const initRightClickReset = (container, onReset) => {
    container.addEventListener('contextmenu', (event) => {
      event.preventDefault() // 阻止默认右键菜单

      // 调用重置标题回调
      if (onResetTitle) {
        onResetTitle()
      }

      // 调用重置回调
      if (onReset) {
        onReset()
      }
    })
  }

  return {
    initClickEvent,
    initRightClickReset
  }
}
