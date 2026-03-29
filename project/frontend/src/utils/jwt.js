// JWT 工具类，用于解码和检查 token 过期时间

/**
 * 解码 JWT token
 * @param {string} token - JWT token 字符串
 * @returns {Object|null} 解码后的 token 数据
 */
export function decodeToken(token) {
  try {
    const base64Url = token.split('.')[1]
    if (!base64Url) return null
    
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/')
    const jsonPayload = decodeURIComponent(
      window.atob(base64)
        .split('')
        .map((c) => `%${('00' + c.charCodeAt(0).toString(16)).slice(-2)}`)
        .join('')
    )
    
    return JSON.parse(jsonPayload)
  } catch (error) {
    console.error('解码 token 失败:', error)
    return null
  }
}

/**
 * 检查 token 是否已过期
 * @param {string} token - JWT token 字符串
 * @returns {boolean} 是否过期
 */
export function isTokenExpired(token) {
  const decoded = decodeToken(token)
  if (!decoded || !decoded.exp) return true
  
  const currentTime = Date.now() / 1000
  return decoded.exp < currentTime
}

/**
 * 检查 token 是否即将过期（默认30分钟内）
 * @param {string} token - JWT token 字符串
 * @param {number} threshold - 过期阈值（秒），默认 1800 秒（30分钟）
 * @returns {boolean} 是否即将过期
 */
export function isTokenAboutToExpire(token, threshold = 1800) {
  const decoded = decodeToken(token)
  if (!decoded || !decoded.exp) return true
  
  const currentTime = Date.now() / 1000
  return decoded.exp - currentTime < threshold
}

/**
 * 从 token 中获取用户信息
 * @param {string} token - JWT token 字符串
 * @returns {Object|null} 用户信息
 */
export function getUserInfoFromToken(token) {
  const decoded = decodeToken(token)
  if (!decoded) return null
  
  return {
    userId: decoded.userId,
    phone: decoded.sub,
    ...decoded
  }
}