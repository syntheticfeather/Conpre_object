<template>
  <Teleport to="body">
    <Transition name="chat-slide">
      <div v-if="visible" class="chat-overlay" @click.self="close">
        <div class="chat-panel">
          <div class="chat-header">
            <span class="chat-title">💬 智能对话</span>
            <button class="close-btn" @click="close">✕</button>
          </div>

          <div class="chat-messages" ref="messagesRef">
            <div
              v-for="msg in messages"
              :key="msg.id"
              :class="['message-item', msg.role]"
            >
              <div v-if="msg.role === 'user'" class="message-bubble user-bubble">
                <div v-html="renderMarkdown(msg.content)"></div>
              </div>
              <div v-else-if="msg.role === 'system'" class="message-tool">
                {{ msg.content }}
              </div>
              <div v-else class="message-bubble assistant-bubble">
                <div v-if="msg.content" class="assistant-content" v-html="renderMarkdown(msg.content)"></div>
              </div>
            </div>

            <div v-if="isThinking && !streamingContent" class="thinking-indicator">
              思考中......
            </div>
          </div>

          <div class="chat-input-area">
            <input
              ref="inputRef"
              v-model="inputText"
              type="text"
              class="chat-input"
              placeholder="输入消息..."
              :disabled="isThinking"
              @keydown.enter="doSend"
            />
            <button
              class="send-btn"
              :disabled="isThinking || !inputText.trim()"
              @click="doSend"
            >
              发送
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, computed, nextTick, watch } from 'vue'
import MarkdownIt from 'markdown-it'

const md = new MarkdownIt({
  html: false,
  breaks: true,
  linkify: true,
  typographer: true
})

const renderMarkdown = (content) => {
  if (!content) return ''
  return md.render(content)
}

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue'])

import { useChatSSE } from '@/composables/useChatSSE'

const { messages, isThinking, streamingContent, sendMessage, abort } = useChatSSE()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const inputText = ref('')
const inputRef = ref(null)
const messagesRef = ref(null)

const close = () => {
  abort()
  visible.value = false
}

const scrollToBottom = async () => {
  await nextTick()
  const el = messagesRef.value
  if (el) {
    el.scrollTop = el.scrollHeight
  }
}

watch([messages, isThinking, streamingContent], scrollToBottom, { deep: true })

watch(visible, (val) => {
  if (val) {
    nextTick(() => inputRef.value?.focus())
  }
})

const doSend = async () => {
  const text = inputText.value
  if (!text.trim() || isThinking.value) return
  inputText.value = ''
  await sendMessage(text)
}
</script>

<style scoped>
.chat-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.3);
  z-index: 9999;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  padding: 40px;
  box-sizing: border-box;
}

.chat-panel {
  width: 350px;
  height: calc(100% - 60px);
  max-height: 680px;
  background: #fff;
  display: flex;
  flex-direction: column;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: -4px 0 20px rgba(0, 0, 0, 0.12);
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 18px;
  background: linear-gradient(135deg, #1a73e8, #1557b0);
  color: #fff;
  flex-shrink: 0;
}

.chat-title {
  font-size: 15px;
  font-weight: 600;
}

.close-btn {
  width: 28px;
  height: 28px;
  border: none;
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
  border-radius: 50%;
  cursor: pointer;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
}

.close-btn:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.35);
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px 18px;
  background: #f5f7fa;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.message-item {
  display: flex;
  flex-direction: column;
}

.message-item.user {
  align-items: flex-end;
}

.message-item.assistant {
  align-items: flex-start;
}

.message-item.system {
  align-items: center;
}

.message-bubble {
  max-width: 88%;
  padding: 12px 16px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.7;
  word-break: break-word;
}

.user-bubble {
  background: #1a73e8;
  color: #fff;
  border-bottom-right-radius: 4px;
}

.user-bubble p {
  margin: 0;
}

.assistant-bubble {
  background: #fff;
  color: #333;
  border-bottom-left-radius: 4px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

/* Markdown 渲染样式 */
.assistant-content p {
  margin: 0 0 8px;
}
.assistant-content p:last-child {
  margin-bottom: 0;
}

.assistant-content ul,
.assistant-content ol {
  padding-left: 20px;
  margin: 4px 0;
}

.assistant-content li {
  margin-bottom: 4px;
}

.assistant-content code {
  background: #f0f2f5;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 13px;
  font-family: 'Consolas', 'Monaco', monospace;
  color: #d63384;
}

.assistant-content pre {
  background: #f6f8fa;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 12px 16px;
  overflow-x: auto;
  margin: 8px 0;
}

.assistant-content pre code {
  background: none;
  padding: 0;
  color: #333;
}

.assistant-content a {
  color: #1a73e8;
  text-decoration: underline;
}

.assistant-content blockquote {
  border-left: 4px solid #1a73e8;
  padding-left: 12px;
  margin: 8px 0;
  color: #666;
  background: #f8f9fa;
  border-radius: 0 4px 4px 0;
}

.assistant-content h1,
.assistant-content h2,
.assistant-content h3,
.assistant-content h4 {
  margin: 12px 0 6px;
  line-height: 1.4;
}

.assistant-content table {
  border-collapse: collapse;
  width: 100%;
  margin: 8px 0;
  font-size: 13px;
}

.assistant-content th,
.assistant-content td {
  border: 1px solid #e4e7ed;
  padding: 6px 10px;
  text-align: left;
}

.assistant-content th {
  background: #f5f7fa;
  font-weight: 600;
}

.assistant-content strong {
  font-weight: 600;
}

.message-tool {
  font-size: 12px;
  color: #909399;
  padding: 4px 10px;
  background: #e8eaed;
  border-radius: 12px;
  max-width: 90%;
}

.thinking-indicator {
  text-align: center;
  color: #909399;
  font-size: 13px;
  padding: 8px 0;
  animation: pulse 1.2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 0.5; }
  50% { opacity: 1; }
}

.chat-input-area {
  display: flex;
  gap: 8px;
  padding: 12px 14px;
  border-top: 1px solid #e4e7ed;
  background: #fff;
  flex-shrink: 0;
}

.chat-input {
  flex: 1;
  height: 38px;
  border: 1px solid #dcdfe6;
  border-radius: 12px;
  padding: 0 12px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s;
}

.chat-input:focus {
  border-color: #1a73e8;
}

.send-btn {
  height: 38px;
  padding: 0 18px;
  border: none;
  background: #1a73e8;
  color: #fff;
  border-radius: 12px;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.2s;
  flex-shrink: 0;
}

.send-btn:hover:not(:disabled) {
  background: #1557b0;
}

.send-btn:disabled,
.chat-input:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.chat-slide-enter-active {
  transition: opacity 0.3s ease;
}
.chat-slide-leave-active {
  transition: opacity 0.25s ease;
}
.chat-slide-enter-active .chat-panel {
  transition: transform 0.35s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}
.chat-slide-leave-active .chat-panel {
  transition: transform 0.25s cubic-bezier(0.55, 0.06, 0.68, 0.53);
}

.chat-slide-enter-from,
.chat-slide-leave-to {
  opacity: 0;
}
.chat-slide-enter-from .chat-panel {
  transform: translateX(120%);
}
.chat-slide-leave-to .chat-panel {
  transform: translateX(100%);
}
</style>

<style>
.assistant-content {
  font-size: 14px;
  line-height: 1.7;
  color: #333;
}

.assistant-content h1,
.assistant-content h2,
.assistant-content h3,
.assistant-content h4 {
  margin: 14px 0 8px;
  font-weight: 600;
  line-height: 1.4;
  color: #1a1a1a;
}

.assistant-content h1 { font-size: 17px; }
.assistant-content h2 { font-size: 16px; }
.assistant-content h3 { font-size: 15px; }
.assistant-content h4 { font-size: 14px; }

.assistant-content h1:first-child,
.assistant-content h2:first-child,
.assistant-content h3:first-child,
.assistant-content h4:first-child {
  margin-top: 0;
}

.assistant-content p {
  margin: 0 0 8px;
  line-height: 1.7;
}

.assistant-content p:last-child {
  margin-bottom: 0;
}

.assistant-content ul,
.assistant-content ol {
  padding-left: 20px;
  margin: 6px 0 10px;
}

.assistant-content li {
  margin-bottom: 4px;
  line-height: 1.7;
}

.assistant-content li:last-child {
  margin-bottom: 0;
}

.assistant-content code {
  font-family: 'Consolas', 'Courier New', 'SFMono-Regular', monospace;
  font-size: 13px;
  background: #f0f2f5;
  padding: 2px 6px;
  border-radius: 4px;
  color: #d63384;
  word-break: break-word;
}

.assistant-content pre {
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 12px 16px;
  border-radius: 8px;
  overflow-x: auto;
  margin: 8px 0 12px;
}

.assistant-content pre code {
  background: none;
  padding: 0;
  color: inherit;
  font-size: 13px;
  line-height: 1.5;
}

.assistant-content blockquote {
  border-left: 3px solid #1a73e8;
  padding: 6px 12px;
  margin: 8px 0;
  color: #666;
  background: #f8f9fa;
  border-radius: 0 6px 6px 0;
}

.assistant-content blockquote p {
  margin: 4px 0;
}

.assistant-content table {
  width: 100%;
  border-collapse: collapse;
  margin: 8px 0 12px;
  font-size: 13px;
}

.assistant-content th,
.assistant-content td {
  border: 1px solid #e0e0e0;
  padding: 6px 10px;
  text-align: left;
}

.assistant-content th {
  background: #f5f5f5;
  font-weight: 600;
}

.assistant-content a {
  color: #1a73e8;
  text-decoration: underline;
}

.assistant-content a:hover {
  color: #1557b0;
}

.assistant-content hr {
  border: none;
  border-top: 1px solid #e0e0e0;
  margin: 12px 0;
}

.assistant-content strong {
  font-weight: 600;
}

.assistant-content img {
  max-width: 100%;
  border-radius: 6px;
  margin: 8px 0;
}
</style>