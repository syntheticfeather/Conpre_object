import * as THREE from 'three'

/**
 * 道格拉斯 - 普克算法简化多边形
 * @param {Array} points - 原始点数组 [[x1,y1], [x2,y2], ...]
 * @param {Number} tolerance - 简化容差（越大越简化）
 * @returns {Array} 简化后的点数组
 */
function simplifyPolygon(points, tolerance) {
  if (points.length <= 3) return points
  
  /**
   * 计算点到线段的距离
   */
  function perpendicularDistance(point, lineStart, lineEnd) {
    const dx = lineEnd[0] - lineStart[0]
    const dy = lineEnd[1] - lineStart[1]
    const lineLengthSq = dx * dx + dy * dy
    
    if (lineLengthSq === 0) {
      return Math.sqrt(
        Math.pow(point[0] - lineStart[0], 2) +
        Math.pow(point[1] - lineStart[1], 2)
      )
    }
    
    const t = Math.max(0, Math.min(1,
      ((point[0] - lineStart[0]) * dx +
       (point[1] - lineStart[1]) * dy) / lineLengthSq
    ))
    
    const projX = lineStart[0] + t * dx
    const projY = lineStart[1] + t * dy
    
    return Math.sqrt(
      Math.pow(point[0] - projX, 2) +
      Math.pow(point[1] - projY, 2)
    )
  }
  
  /**
   * 递归简化
   */
  function simplifyDP(points, start, end, tol) {
    let maxDist = -1
    let maxIndex = -1
    
    // 找到距离最远的点
    for (let i = start + 1; i < end; i++) {
      const dist = perpendicularDistance(
        points[i],
        points[start],
        points[end]
      )
      if (dist > maxDist) {
        maxDist = dist
        maxIndex = i
      }
    }
    
    // 如果最大距离大于容差，递归简化
    if (maxDist > tol) {
      const left = simplifyDP(points, start, maxIndex, tol)
      const right = simplifyDP(points, maxIndex, end, tol)
      return left.slice(0, -1).concat(right)
    } else {
      return [points[start], points[end]]
    }
  }
  
  // 对闭合多边形进行简化
  const result = simplifyDP(points, 0, points.length - 1, tolerance)
  
  // 确保多边形闭合
  if (result.length > 0 && 
      (result[0][0] !== result[result.length - 1][0] ||
       result[0][1] !== result[result.length - 1][1])) {
    result.push(result[0])
  }
  
  return result
}

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
      
      // 跳过没有名称的区域，不显示
      if (!name || !name.trim()) {
        continue
      }
      
      // 创建省份形状
      const shape = new THREE.Shape()
      
      // 绘制省份轮廓（使用第一个多边形）
      if (coordinates.length > 0 && coordinates[0].length > 0) {
        const polygon = coordinates[0][0]
        
        // 使用道格拉斯 - 普克算法简化轮廓，减少点数
      const simplified = simplifyPolygon(polygon, 0.05) // 0.05 是简化容差，保留更多细节
        
        // 用简化后的点绘制形状
        for (let j = 0; j < simplified.length; j++) {
          let [x, y] = simplified[j]
          if (j === 0) {
            shape.moveTo(x, y)
          } else {
            shape.lineTo(x, y)
          }
        }
      }
      
      // 拉伸设置 - 增加厚度
      const extrudeSettings = {
        depth: 3,            // 厚度
        bevelEnabled: false, // 关闭斜面
      }
      
      // 创建拉伸几何体
      const geometry = new THREE.ExtrudeGeometry(shape, extrudeSettings)
      
      // 关键：计算顶点法线，启用平滑着色
      geometry.computeVertexNormals()
      
      // 设置材质 - 使用 Phong 材质并启用平滑着色
      const material = new THREE.MeshPhongMaterial({
        color: 0x2abae0,      // #2abae0 地图表面颜色（绿色）
        transparent: true,    // 启用透明度
        opacity: 0.7,         // 透明度（0-1，1 为完全不透明，0 为完全透明）
        side: THREE.DoubleSide,
        shininess: 50,        // 高光亮度（中等亮度）
        specular: 0x111111,   // 高光颜色（柔和的白色高光）
        flatShading: false,   // 关闭平面着色，启用平滑着色
      })
      
      const mesh = new THREE.Mesh(geometry, material)
      mesh.name = name
      
      // 保存原始颜色
      mesh.userData = {
        originalColor: 0x2abae0,
        hoverColor: 0x7acae4ff  // #7acae4ff 更亮的青色（悬停时的颜色，明显变亮）
      }
      
      // 创建顶部边界线 - 使用简化后的点创建顶部轮廓线
      if (coordinates.length > 0 && coordinates[0].length > 0) {
        const polygon = coordinates[0][0]
        
        // 使用同样的简化算法，容差保持一致
        const simplified = simplifyPolygon(polygon, 0.05)
        
        const topEdgePoints = simplified.map(coord => 
          new THREE.Vector3(coord[0], coord[1], 3)
        )
        
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
      }
      
      // 计算省份中心点，用于添加文本标签（此时 name 一定有效）
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
