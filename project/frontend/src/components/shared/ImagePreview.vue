<template>
  <div v-if="visible" class="image-preview-overlay" @click.self="handleClose">
    <div class="image-preview-container">
      <div class="preview-header">
        <h3>{{ title }}</h3>
        <div class="header-actions">
          <button class="action-btn" @click="handleDownload" title="下载">
            <el-icon><Download /></el-icon>
          </button>
          <button class="action-btn" @click="handleRotate" title="旋转">
            <el-icon><RefreshRight /></el-icon>
          </button>
          <button class="action-btn close-btn" @click="handleClose" title="关闭">
            <el-icon><Close /></el-icon>
          </button>
        </div>
      </div>

      <div class="preview-content" @wheel.prevent="handleWheel">
        <div 
          class="image-wrapper"
          :style="{ transform: `scale(${scale}) rotate(${rotation}deg)` }"
        >
          <img 
            v-if="imageUrl" 
            :src="imageUrl" 
            :alt="title"
            @load="handleImageLoad"
            @error="handleImageError"
          />
          <div v-else class="no-image">
            <el-icon class="no-image-icon"><Picture /></el-icon>
            <p>暂无图片</p>
          </div>
        </div>
      </div>

      <div class="preview-footer">
        <div class="zoom-controls">
          <button class="zoom-btn" @click="handleZoomOut" :disabled="scale <= 0.5">
            <el-icon><ZoomOut /></el-icon>
          </button>
          <span class="zoom-level">{{ Math.round(scale * 100) }}%</span>
          <button class="zoom-btn" @click="handleZoomIn" :disabled="scale >= 3">
            <el-icon><ZoomIn /></el-icon>
          </button>
        </div>
        <button class="reset-btn" @click="handleReset">重置</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted } from 'vue'
import { Download, RefreshRight, Close, Picture, ZoomIn, ZoomOut } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  imageUrl: {
    type: String,
    default: ''
  },
  title: {
    type: String,
    default: '图片预览'
  }
})

const emit = defineEmits(['update:visible', 'close'])

const scale = ref(1)
const rotation = ref(0)
const loading = ref(false)

const handleZoomIn = () => {
  if (scale.value < 3) {
    scale.value = Math.min(scale.value + 0.25, 3)
  }
}

const handleZoomOut = () => {
  if (scale.value > 0.5) {
    scale.value = Math.max(scale.value - 0.25, 0.5)
  }
}

const handleWheel = (e) => {
  if (e.deltaY < 0) {
    handleZoomIn()
  } else {
    handleZoomOut()
  }
}

const handleRotate = () => {
  rotation.value = (rotation.value + 90) % 360
}

const handleReset = () => {
  scale.value = 1
  rotation.value = 0
}

const handleDownload = () => {
  if (!props.imageUrl) {
    ElMessage.warning('暂无图片可下载')
    return
  }

  try {
    const link = document.createElement('a')
    link.href = props.imageUrl
    link.download = props.title || 'image'
    link.target = '_blank'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    ElMessage.success('下载已开始')
  } catch (error) {
    console.error('下载失败:', error)
    ElMessage.error('下载失败')
  }
}

const handleImageLoad = () => {
  loading.value = false
}

const handleImageError = () => {
  loading.value = false
  ElMessage.error('图片加载失败')
}

const handleClose = () => {
  emit('update:visible', false)
  emit('close')
}

const handleKeyDown = (e) => {
  if (!props.visible) return
  
  switch (e.key) {
    case 'Escape':
      handleClose()
      break
    case 'ArrowUp':
    case '+':
      handleZoomIn()
      break
    case 'ArrowDown':
    case '-':
      handleZoomOut()
      break
    case 'r':
    case 'R':
      handleRotate()
      break
  }
}

watch(() => props.visible, (newVal) => {
  if (newVal) {
    loading.value = true
    scale.value = 1
    rotation.value = 0
  }
})

onMounted(() => {
  window.addEventListener('keydown', handleKeyDown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeyDown)
})
</script>

<style scoped>
.image-preview-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.85);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.image-preview-container {
  width: 90vw;
  height: 90vh;
  background: white;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: #f8f9fa;
  border-bottom: 1px solid #dee2e6;
}

.preview-header h3 {
  margin: 0;
  font-size: 18px;
  color: #333;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.action-btn {
  width: 36px;
  height: 36px;
  border: none;
  background: white;
  border-radius: 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  color: #666;
}

.action-btn:hover {
  background: #e9ecef;
  color: #333;
}

.action-btn.close-btn:hover {
  background: #dc3545;
  color: white;
}

.preview-content {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: auto;
  background: #f5f5f5;
  position: relative;
}

.image-wrapper {
  transition: transform 0.3s ease;
  max-width: 100%;
  max-height: 100%;
}

.image-wrapper img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  display: block;
}

.no-image {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #999;
  padding: 60px;
}

.no-image-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.no-image p {
  margin: 0;
  font-size: 16px;
}

.preview-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  background: #f8f9fa;
  border-top: 1px solid #dee2e6;
}

.zoom-controls {
  display: flex;
  align-items: center;
  gap: 12px;
}

.zoom-btn {
  width: 32px;
  height: 32px;
  border: 1px solid #dee2e6;
  background: white;
  border-radius: 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  color: #666;
}

.zoom-btn:hover:not(:disabled) {
  background: #e9ecef;
  color: #333;
}

.zoom-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.zoom-level {
  font-size: 14px;
  color: #666;
  min-width: 50px;
  text-align: center;
}

.reset-btn {
  padding: 6px 16px;
  border: 1px solid #dee2e6;
  background: white;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
  color: #666;
}

.reset-btn:hover {
  background: #e9ecef;
  color: #333;
}
</style>
