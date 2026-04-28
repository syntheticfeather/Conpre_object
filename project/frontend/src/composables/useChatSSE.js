import { ref } from 'vue'

export function useChatSSE() {
  const messages = ref([])
  const isThinking = ref(false)
  const streamingContent = ref('')
  let abortController = null
  let sessionId = null

  const getToken = () => {
    const stored = localStorage.getItem('auth-store')
    if (!stored) return null
    try {
      const data = JSON.parse(stored)
      return data.token || null
    } catch {
      return null
    }
  }

  const sendMessage = async (text) => {
    if (!text.trim()) return

    abortController = new AbortController()
    const token = getToken()
    if (!token) return

    const userMsg = {
      id: Date.now().toString(),
      role: 'user',
      content: text.trim(),
      type: 'text'
    }
    messages.value.push(userMsg)

    isThinking.value = true
    streamingContent.value = ''

    const assistantMsg = {
      id: (Date.now() + 1).toString(),
      role: 'assistant',
      content: '',
      type: 'text'
    }
    messages.value.push(assistantMsg)

    try {
      const baseUrl = import.meta.env.VITE_API_BASE_URL || ''
      const response = await fetch(`${baseUrl}/api/chat/stream`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`,
          'Accept': 'text/event-stream'
        },
        body: JSON.stringify({
          message: text.trim(),
          session_id: sessionId
        }),
        signal: abortController.signal
      })

      if (!response.ok) {
        throw new Error(`请求失败: ${response.status}`)
      }

      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''

      while (true) {
        const { done, value } = await reader.read()
        if (done) break

        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        buffer = lines.pop() || ''

        let currentEvent = ''
        for (const line of lines) {
          const trimmed = line.trim()
          if (trimmed.startsWith('event: ')) {
            currentEvent = trimmed.slice(7)
          } else if (trimmed.startsWith('data: ')) {
            const data = trimmed.slice(6)
            handleEvent(currentEvent, data)
          }
        }
      }

      if (buffer.trim()) {
        const trimmed = buffer.trim()
        if (trimmed.startsWith('data: ')) {
          handleEvent('', trimmed.slice(6))
        }
      }
    } catch (err) {
      if (err.name === 'AbortError') return
      const last = messages.value[messages.value.length - 1]
      if (last && last.role === 'assistant' && !last.content) {
        messages.value.pop()
      }
      messages.value.push({
        id: (Date.now() + 2).toString(),
        role: 'assistant',
        content: err.message || '连接失败',
        type: 'error'
      })
    } finally {
      isThinking.value = false
      streamingContent.value = ''
    }
  }

  const handleEvent = (eventType, data) => {
    if (!data.trim()) return

    try {
      const parsed = JSON.parse(data)

      if (eventType === 'session_init' && parsed.session_id) {
        sessionId = parsed.session_id
        return
      }

      if (eventType === 'tool_call') {
        messages.value.push({
          id: (Date.now() + Math.random()).toString(),
          role: 'system',
          content: `🔧 使用工具: ${parsed.tool_name}`,
          type: 'tool_call'
        })
        return
      }

      if (eventType === 'tool_result') {
        messages.value.push({
          id: (Date.now() + Math.random()).toString(),
          role: 'system',
          content: `✅ 工具返回: ${parsed.tool_name}`,
          type: 'tool_result'
        })
        return
      }

      if (eventType === 'error') {
        messages.value.push({
          id: (Date.now() + Math.random()).toString(),
          role: 'assistant',
          content: typeof parsed === 'string' ? parsed : parsed.message || '未知错误',
          type: 'error'
        })
        return
      }

      if (eventType === 'message') {
        const content = typeof parsed === 'string' ? parsed : parsed.content || ''
        const last = messages.value[messages.value.length - 1]
        if (last && last.role === 'assistant' && last.type === 'text') {
          last.content += content
          streamingContent.value = last.content
        }
        return
      }
    } catch {
      if (eventType === 'message') {
        const last = messages.value[messages.value.length - 1]
        if (last && last.role === 'assistant' && last.type === 'text') {
          last.content += data
          streamingContent.value = last.content
        }
      }
    }
  }

  const abort = () => {
    abortController?.abort()
    isThinking.value = false
    streamingContent.value = ''
  }

  const clearMessages = () => {
    messages.value = []
    sessionId = null
    streamingContent.value = ''
    abort()
  }

  return {
    messages,
    isThinking,
    streamingContent,
    sendMessage,
    abort,
    clearMessages
  }
}
