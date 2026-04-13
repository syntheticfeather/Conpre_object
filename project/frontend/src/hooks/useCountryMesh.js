import * as THREE from 'three'

/**
 * 生成国家网格 Mesh
 * @returns
 */
export default function useCountryMesh() {
  /**
   * 生成地图的网格
   * @param {*} worldData
   */
  const generateMap = worldData => {
    // 生成国家网格
    let features = worldData.features
    let meshArr = []
    for (let i = 0; i < features.length; i++) {
      // 坐标 
      let coordinates = features[i].geometry.coordinates
      // 国家名称
      let name = features[i].properties.name
      
      // 创建省份形状
      const shape = new THREE.Shape()
      
      // 绘制省份轮廓（使用第一个多边形）
      if (coordinates.length > 0 && coordinates[0].length > 0) {
        const polygon = coordinates[0][0]
        for (let j = 0; j < polygon.length; j++) {
          let [x, y] = polygon[j]
          if (j === 0) {
            shape.moveTo(x, y)
          }
          shape.lineTo(x, y)
        }
      }
      
      // 拉伸设置 - 增加厚度
      const extrudeSettings = {
        depth: 3,  // 厚度增加到 3 个单位
        bevelEnabled: false,  // 关闭斜面，保持简洁
      }
      
      // 创建拉伸几何体
      const geometry = new THREE.ExtrudeGeometry(shape, extrudeSettings)
      
      // 设置材质 - 使用单一材质，顶部和侧面统一颜色
      const material = new THREE.MeshLambertMaterial({
        color: 0x00E0FF,  // 深青色
        side: THREE.DoubleSide,
      })
      
      const mesh = new THREE.Mesh(geometry, material)
      mesh.name = name
      
      // 保存原始颜色
      mesh.userData = {
        originalColor: 0x00E0FF,
        hoverColor: 0x80FFFF  // 更浅的青色（悬停时的颜色）
      }
      
      // 创建顶部边界线 - 直接从拉伸几何体提取，但只显示顶面的边
      // 使用 LineLoop 创建闭合的顶部轮廓线
      const topEdgePoints = []
      if (coordinates.length > 0 && coordinates[0].length > 0) {
        const polygon = coordinates[0][0]
        for (let j = 0; j < polygon.length; j++) {
          let [x, y] = polygon[j]
          topEdgePoints.push(new THREE.Vector3(x, y, 3))  // Z=3 在顶部
        }
      }
      
      if (topEdgePoints.length > 0) {
        const topEdgeGeometry = new THREE.BufferGeometry().setFromPoints(topEdgePoints)
        const topEdgeMaterial = new THREE.LineBasicMaterial({
          color: 0xffffff,  // 白色
          linewidth: 1
        })
        const topEdgeLine = new THREE.LineLoop(topEdgeGeometry, topEdgeMaterial)
        topEdgeLine.name = `${name}_top_edge`
        topEdgeLine.renderOrder = 999  // 确保最后渲染，在最上层
        mesh.add(topEdgeLine)
      }
      
      // 计算省份中心点，用于添加文本标签
      if (coordinates.length > 0 && coordinates[0].length > 0) {
        const polygon = coordinates[0][0]
        let sumX = 0, sumY = 0
        for (let j = 0; j < polygon.length; j++) {
          sumX += polygon[j][0]
          sumY += polygon[j][1]
        }
        const centerX = sumX / polygon.length
        const centerY = sumY / polygon.length
        
        // 创建文本标签（使用 Canvas 纹理）
        const canvas = document.createElement('canvas')
        const ctx = canvas.getContext('2d')
        const fontSize = 12  // 字体大小减半
        const padding = 5    // 内边距也相应减小
        
        // 测量文本
        ctx.font = `bold ${fontSize}px Arial`
        const textWidth = ctx.measureText(name).width
        
        // 设置画布大小
        canvas.width = textWidth + padding * 2
        canvas.height = fontSize + padding * 2
        
        // 绘制文本
        ctx.font = `bold ${fontSize}px Arial`
        ctx.fillStyle = 'white'
        ctx.textAlign = 'center'
        ctx.textBaseline = 'middle'
        ctx.fillText(name, canvas.width / 2, canvas.height / 2)
        
        // 创建纹理
        const texture = new THREE.CanvasTexture(canvas)
        const spriteMaterial = new THREE.SpriteMaterial({ 
          map: texture,
          transparent: true
        })
        const sprite = new THREE.Sprite(spriteMaterial)
        sprite.name = `${name}_label`
        sprite.position.set(centerX, centerY, 4)  // 在地图上方
        sprite.scale.set(canvas.width / 15, canvas.height / 15, 1)  // 缩放 sprite
        mesh.add(sprite)
      }
      
      meshArr.push(mesh)
    }
    return meshArr
  }

  return { generateMap }
}
