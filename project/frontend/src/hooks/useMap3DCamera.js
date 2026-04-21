import * as THREE from 'three'
// import TWEEN from '@tweenjs/tween.js'

/**
 * 3D 地图相机控制 Composable
 * @param {Object} map3dInstance - Map3D 实例
 * @param {Array} centerXY - 地图中心坐标 [lng, lat]
 * @returns {Object} 相机控制方法
 */
export default function useMap3DCamera(map3dInstance, centerXY) {
  // 相机移动动画状态
  let isMoving = false
  let moveStartTime = 0
  let moveDuration = 1500
  let startPos = null
  let targetPos = null
  let startLook = null
  let targetLook = null

  /**
   * 移动到指定省份
   * @param {THREE.Vector3} target - 目标省份中心点
   */
  const moveToProvince = (target) => {
    if (!map3dInstance?.camera || !map3dInstance?.controls) {
      console.error('相机或控制器不存在')
      return
    }

    // 计算目标相机位置
    const currentPos = map3dInstance.camera.position.clone()

    // 计算从省份中心到相机的偏移向量
    const offset = new THREE.Vector3(
      currentPos.x - centerXY[0],
      currentPos.y - centerXY[1],
      30
    )

    // 目标相机位置 = 省份中心 + 偏移
    // 增加 Z 轴高度让相机上升，同时扩大 X/Y 偏移让相机后移
    targetPos = {
      x: target.x + offset.x * 0.5,  // 从 0.3 增加到 0.5，后移
      y: target.y + offset.y * 0.5,  // 从 0.3 增加到 0.5，后移
      z: 35                          // 从 25 增加到 35，上升
    }

    // 目标观察点就是省份中心
    targetLook = {
      x: target.x,
      y: target.y,
      z: 0
    }

    // 保存起始位置
    startPos = currentPos
    startLook = map3dInstance.controls.target.clone()

    // 设置动画参数
    isMoving = true
    moveStartTime = performance.now()
  }

  /**
   * 重置相机到初始位置
   */
  const resetCamera = () => {
    if (!map3dInstance?.camera) {
      console.error('相机不存在')
      return
    }

    // 保存当前相机状态
    const currentPos = map3dInstance.camera.position.clone()

    // 设置目标位置（地图中心）
    targetPos = {
      x: 100,  // 初始 X 位置
      y: -3,   // 初始 Y 位置
      z: 40    // 初始 Z 位置
    }

    targetLook = {
      x: centerXY[0],
      y: centerXY[1],
      z: 0
    }

    // 保存起始位置
    startPos = currentPos
    startLook = map3dInstance.controls.target.clone()

    // 设置动画参数
    isMoving = true
    moveStartTime = performance.now()
  }

  /**
   * 更新相机位置（每帧调用）
   * @returns {boolean} 是否仍在移动中
   */
  const updateCamera = () => {
    if (!isMoving || !map3dInstance?.camera) {
      return false
    }

    const now = performance.now()
    const elapsed = now - moveStartTime
    const progress = Math.min(elapsed / moveDuration, 1)

    // 缓动函数（Cubic InOut）
    const ease = progress < 0.5
      ? 4 * progress * progress * progress
      : 1 - Math.pow(-2 * progress + 2, 3) / 2

    // 更新相机位置
    if (startPos && targetPos) {
      map3dInstance.camera.position.x = startPos.x + (targetPos.x - startPos.x) * ease
      map3dInstance.camera.position.y = startPos.y + (targetPos.y - startPos.y) * ease
      map3dInstance.camera.position.z = startPos.z + (targetPos.z - startPos.z) * ease

      // 更新观察点
      if (startLook && targetLook) {
        const currentLook = {
          x: startLook.x + (targetLook.x - startLook.x) * ease,
          y: startLook.y + (targetLook.y - startLook.y) * ease,
          z: startLook.z + (targetLook.z - startLook.z) * ease
        }
        map3dInstance.camera.lookAt(currentLook.x, currentLook.y, currentLook.z)
        map3dInstance.controls.target.set(currentLook.x, currentLook.y, currentLook.z)
      }
    }

    // 动画结束
    if (progress >= 1) {
      isMoving = false
      return false
    }

    return true
  }

  return {
    isMoving,
    moveToProvince,
    resetCamera,
    updateCamera
  }
}
