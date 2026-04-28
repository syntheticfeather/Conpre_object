import { ref } from 'vue'

export function useNotificationStream(options = {}) {
  const {
    onNotification = () => {},
    getToken = null,
    notificationTitle = '新通知',
    streamUrl = '/api/notifications/admin/stream',
    maxReconnectAttempts = 10
  } = options

  const isStreamConnected = ref(false)
  const streamError = ref(null)
  const reconnectAttempts = ref(0)
  let sseReader = null

  const defaultGetToken = () => {
    const stored = localStorage.getItem('auth-store')
    if (!stored) return null
    try {
      const data = JSON.parse(stored)
      return data.token || null
    } catch {
      return null
    }
  }

  const resolveToken = getToken || defaultGetToken

  const showDesktopNotification = notification => {
    if ('Notification' in window && Notification.permission === 'granted') {
      try {
        new Notification(notificationTitle, {
          body: notification.content || notification.title || '',
          icon: '/favicon.ico',
          tag: `notification-${notification.id}`
        })
      } catch (error) {
        console.error('显示桌面通知失败:', error)
      }
    }
  }

  const requestNotificationPermission = () => {
    if ('Notification' in window && Notification.permission === 'default') {
      Notification.requestPermission()
    }
  }

  let sseBuffer = ''
  let currentEventType = ''

  const processSSEMessages = chunk => {
    sseBuffer += chunk

    const lines = sseBuffer.split('\n')
    sseBuffer = lines.pop() || ''

    lines.forEach(line => {
      const trimmed = line.trim()
      if (!trimmed) return

      if (trimmed.startsWith('event:')) {
        currentEventType = trimmed[6] === ' ' ? trimmed.slice(7).trim() : trimmed.slice(6).trim()
        return
      }

      if (trimmed.startsWith('data:')) {
        let raw = trimmed.slice(5)
        if (raw.startsWith(' ')) raw = raw.slice(1)

        if (!raw.trim()) return

        try {
          const notification = JSON.parse(raw)
          onNotification(notification)
        } catch (error) {
          if (currentEventType === 'connected') return
          console.error('解析 SSE 数据失败:', error, line)
        }
      }
    })
  }

  const closeNotificationStream = () => {
    sseBuffer = ''
    currentEventType = ''
    if (sseReader) {
      try {
        sseReader.cancel()
      } catch {
        // ignore
      }
      sseReader = null
    }
    isStreamConnected.value = false
    console.log('SSE 连接已关闭')
  }

  const retryConnect = () => {
    if (reconnectAttempts.value < maxReconnectAttempts) {
      reconnectAttempts.value++
      const delay = Math.min(1000 * Math.pow(2, reconnectAttempts.value), 30000)
      console.log(
        `SSE 将在 ${delay / 1000} 秒后重连 (第 ${reconnectAttempts.value}/${maxReconnectAttempts} 次)`
      )
      setTimeout(() => {
        if (!isStreamConnected.value) {
          initNotificationStreamWithFetch()
        }
      }, delay)
    } else {
      console.error('SSE 已达到最大重连次数，停止重连')
    }
  }

  const initNotificationStreamWithFetch = async () => {
    try {
      const token = resolveToken()
      if (!token) {
        isStreamConnected.value = false
        streamError.value = '未登录或 token 无效'
        return
      }

      closeNotificationStream()

      isStreamConnected.value = false
      streamError.value = null

      const baseUrl = import.meta.env.VITE_API_BASE_URL || ''
      const fullUrl = `${baseUrl}${streamUrl}`

      const response = await fetch(fullUrl, {
        headers: {
          Accept: 'text/event-stream',
          'Cache-Control': 'no-cache',
          Authorization: `Bearer ${token}`
        }
      })

      if (!response.ok) {
        throw new Error(`SSE 连接失败: ${response.status} ${response.statusText}`)
      }

      if (!response.body) {
        throw new Error('response.body 为空, 浏览器不支持 ReadableStream')
      }

      const reader = response.body.getReader()
      sseReader = reader
      isStreamConnected.value = true
      reconnectAttempts.value = 0
      console.log('SSE 连接已建立')

      const decoder = new TextDecoder()

      const readStream = async () => {
        try {
          while (true) {
            const { done, value } = await reader.read()
            if (done) {
              const remaining = sseBuffer.trim()
              if (remaining) {
                const flushLines = remaining.split('\n')
                flushLines.forEach(line => {
                  const t = line.trim()
                  if (!t) return
                  if (t.startsWith('data:')) {
                    let raw = t.slice(5)
                    if (raw.startsWith(' ')) raw = raw.slice(1)
                    if (raw.trim()) {
                      try {
                        const notification = JSON.parse(raw)
                        onNotification(notification)
                      } catch {
                        // incomplete data, ignore
                      }
                    }
                  }
                })
              }
              sseBuffer = ''
              currentEventType = ''
              isStreamConnected.value = false
              retryConnect()
              break
            }
            const chunk = decoder.decode(value, { stream: true })
            processSSEMessages(chunk)
          }
        } catch (error) {
          isStreamConnected.value = false
          streamError.value = error.message
          retryConnect()
        }
      }

      readStream()
    } catch (error) {
      isStreamConnected.value = false
      streamError.value = error.message
      retryConnect()
    }
  }

  const reconnectStream = () => {
    reconnectAttempts.value = 0
    initNotificationStreamWithFetch()
  }

  return {
    isStreamConnected,
    streamError,
    initNotificationStreamWithFetch,
    closeNotificationStream,
    reconnectStream,
    requestNotificationPermission,
    showDesktopNotification
  }
}