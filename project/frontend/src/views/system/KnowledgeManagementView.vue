<template>
  <div class="knowledge-management">
    <div class="header">知识库管理</div>

    <!-- 增添-左右分栏：左侧问答对 / 右侧文档上传 -->
    <div class="upload-section">
      <!-- 左侧：手动添加问答对 -->
      <el-card class="add-faq-card">
        <div class="card-header">
          <el-icon><ChatDotRound /></el-icon>
          <h3>手动添加问答对</h3>
        </div>
        <p class="card-desc">逐条录入问题与答案</p>
        
        <el-form :model="form" class="faq-form">
          <el-form-item label="问题">
            <el-input 
              v-model="form.question" 
              placeholder="请输入问题，如：如何申请贷款？" 
              maxlength="500"
              show-word-limit
            />
          </el-form-item>
          <el-form-item label="答案">
            <el-input 
              v-model="form.answer" 
              type="textarea" 
              :rows="4" 
              placeholder="请输入答案内容"
              maxlength="10000"
              show-word-limit
            />
          </el-form-item>
          <el-form-item label="分类">
            <el-select v-model="form.category" placeholder="选择分类">
              <el-option label="申请流程" value="申请流程" />
              <el-option label="产品咨询" value="产品咨询" />
              <el-option label="还款问题" value="还款问题" />
              <el-option label="通用" value="通用" />
            </el-select>
          </el-form-item>
          <el-form-item label="附件">
            <el-upload
              disabled
              class="faq-attachment-upload"
              drag
              :show-file-list="false"
            >
              <el-icon class="faq-upload-icon"><UploadFilled /></el-icon>
              <div class="faq-upload-text">附件上传功能开发中...</div>
            </el-upload>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="saveKnowledge" :loading="savingFaq">
              <el-icon><Plus /></el-icon>
              添加问答对
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 右侧：批量导入文档 -->
      <el-card class="upload-doc-card">
        <div class="card-header">
          <el-icon><Upload /></el-icon>
          <h3>批量导入文档</h3>
        </div>
        <p class="card-desc">选择文件后点击确认上传，支持Markdown和JSON格式</p>

        <el-upload
          ref="docUploadRef"
          class="doc-upload-demo"
          drag
          :auto-upload="false"
          :file-list="pendingDocList"
          :show-file-list="false"
          :limit="10"
          :multiple="true"
          :on-change="handleDocFileChange"
          :on-remove="handleDocFileRemove"
          accept=".json,.md"
        >
          <el-icon class="doc-upload-icon"><UploadFilled /></el-icon>
          <div class="doc-upload-text">
            将文件拖拽到此处，或 <em>点击选择</em>
          </div>
        </el-upload>

        <div class="upload-actions">
          <el-button 
            type="primary" 
            @click="confirmDocUpload"
            :loading="docUploading"
            :disabled="pendingDocList.length === 0"
          >
            <el-icon><Check /></el-icon>
            确认上传 ({{ pendingDocList.length }})
          </el-button>
          <el-button 
            @click="clearPendingDocs"
            :disabled="pendingDocList.length === 0"
          >
            清空列表
          </el-button>
          <span class="actions-hint">已选择 {{ pendingDocList.length }} 个文件待上传</span>
        </div>

        <div v-if="pendingDocList.length > 0" class="upload-result-list">
          <div class="result-header">
            <span>待上传文件 ({{ pendingDocList.length }})</span>
            <el-button type="danger" size="small" text @click="clearPendingDocs">清空列表</el-button>
          </div>
          <div class="result-items">
            <div v-for="(file, idx) in pendingDocList" :key="idx" class="result-item" :class="file.status">
              <el-icon v-if="file.status === 'success'"><CircleCheckFilled /></el-icon>
              <el-icon v-else-if="file.status === 'uploading'"><Loading /></el-icon>
              <el-icon v-else><Document /></el-icon>
              <div class="item-info">
                <span class="item-name">{{ file.name }}</span>
                <span class="item-status">{{ getStatusText(file) }}</span>
              </div>
              <el-button
                v-if="file.status !== 'uploading'"
                type="danger"
                size="small"
                text
                @click="removePendingDoc(idx)"
              >
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 搜索问答对 -->
    <el-card class="search-form">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="关键词">
          <el-input 
            v-model="searchForm.keyword" 
            placeholder="搜索问题或答案" 
            clearable
            @clear="searchKnowledge"
          />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="searchForm.category" placeholder="全部分类" clearable @change="searchKnowledge">
            <el-option label="申请流程" value="申请流程" />
            <el-option label="产品咨询" value="产品咨询" />
            <el-option label="还款问题" value="还款问题" />
            <el-option label="通用" value="通用" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="searchKnowledge">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 知识库列表 -->
    <el-card class="knowledge-list">
      <div class="list-header">
        <h3>知识库列表</h3>
        <el-button type="primary" @click="refreshCurrentTab">刷新</el-button>
      </div>

      <el-tabs v-model="activeTab" @tab-change="handleTabChange" class="knowledge-tabs">
        <!-- Tab 1: 问答对知识项 (接口#1 GET /api/knowledge/faq) -->
        <el-tab-pane label="问答对" name="faq">
          <div class="tab-info">共 {{ faqTotal }} 条问答对</div>
          <el-table :data="paginatedFaqList" stripe v-loading="loading">
            <el-table-column label="序号" width="60" align="center">
              <template #default="{ $index }">
                {{ (faqPage - 1) * faqPageSize + $index + 1 }}
              </template>
            </el-table-column>
            <el-table-column prop="id" label="ID" width="200" />
            <el-table-column prop="question" label="问题" width="200" >
              <template #default="{ row }">
                <ContentTooltip :content="row.question || ''">
                  <span class="cell-text">{{ row.question }}</span>
                </ContentTooltip>
              </template>
            </el-table-column>
            <el-table-column prop="answer" label="答案">
              <template #default="{ row }">
                <ContentTooltip :content="row.answer || ''">
                  <span class="cell-text">{{ row.answer }}</span>
                </ContentTooltip>
              </template>
            </el-table-column>
            <el-table-column prop="category" label="分类" width="100">
              <template #default="{ row }">
                <el-tag :type="getCategoryTagType(row.category)">{{ row.category }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" size="small" @click="editKnowledge(row)">编辑</el-button>
                <el-button type="danger" size="small" @click="deleteKnowledge(row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-container">
            <el-pagination
              v-model:current-page="faqPage"
              v-model:page-size="faqPageSize"
              :page-sizes="[10, 20, 50, 100]"
              layout="total, sizes, prev, pager, next, jumper"
              :total="faqTotal"
              @size-change="faqPage = 1"
            />
          </div>
        </el-tab-pane>

        <!-- Tab 2: 文档分块知识项 -->
        <el-tab-pane label="文档块" name="documents">
          <div class="tab-info">共 {{ docTotal }} 条文档分块 | 
            <el-button type="danger" size="small" @click="showDeleteDocDialog = true" style="margin-left: 8px">
              按文档名删除
            </el-button>
          </div>
          <el-table :data="paginatedDocList" stripe v-loading="docLoading">
            <el-table-column label="序号" width="60" align="center">
              <template #default="{ $index }">
                {{ (docPage - 1) * docPageSize + $index + 1 }}
              </template>
            </el-table-column>
            <el-table-column prop="document_name" label="文档名称" width="100">
              <template #default="{ row }">
                <el-tag type="warning">{{ getActualDocName(row.document_name) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="section" label="章节" min-width="80">
              <template #default="{ row }">
                <ContentTooltip :content="row.section || ''">
                  <span class="cell-text">{{ row.section || '-' }}</span>
                </ContentTooltip>
              </template>
            </el-table-column>
            <el-table-column prop="section_level" label="层级" width="80" align="center">
              <template #default="{ row }">
                <el-tag size="small">{{ row.section_level || '-' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="section_path" label="章节路径" min-width="250">
              <template #default="{ row }">
                <ContentTooltip :content="row.section_path || ''">
                  <span class="cell-text">{{ row.section_path || '-' }}</span>
                </ContentTooltip>
              </template>
            </el-table-column>
            <el-table-column prop="source_path" label="资源路径" width="350">
              <template #default="{ row }">
                <ContentTooltip :content="row.source_path || ''">
                  <span class="cell-text">{{ row.source_path || '-' }}</span>
                </ContentTooltip>
              </template>
            </el-table-column>
            <el-table-column prop="chunk_index" label="分块序号" width="90" align="center" />
            <el-table-column label="操作" max-width="150" fixed="right">
              <template #default="{ row }"> <el-button type="danger" size="small" @click="deleteDocChunk(row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-container">
            <el-pagination
              v-model:current-page="docPage"
              v-model:page-size="docPageSize"
              :page-sizes="[10, 20, 50, 100]"
              layout="total, sizes, prev, pager, next, jumper"
              :total="docTotal"
              @size-change="docPage = 1"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="editDialogVisible" title="编辑问答对" width="600px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="问题">
          <el-input v-model="editForm.question" placeholder="请输入问题" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="答案">
          <el-input v-model="editForm.answer" type="textarea" :rows="4" placeholder="请输入答案" maxlength="10000" show-word-limit />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="editForm.category" placeholder="选择分类">
            <el-option label="申请流程" value="申请流程" />
            <el-option label="产品咨询" value="产品咨询" />
            <el-option label="还款问题" value="还款问题" />
            <el-option label="通用" value="通用" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmEdit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 按文档名删除整篇文档对话框 -->
    <el-dialog v-model="showDeleteDocDialog" title="按文档名删除整篇文档" width="450px">
      <el-alert
        type="warning"
        :closable="false"
        show-icon
        style="margin-bottom: 16px"
      >
        <template #title>此操作将删除该文档名称下的<strong>所有分块</strong>，且不可恢复！</template>
      </el-alert>
      <el-form :model="deleteDocForm" label-width="100px">
        <el-form-item label="文档名称" required>
          <el-input
            v-model="deleteDocForm.document_name"
            placeholder="输入要删除的文档名称（不含扩展名）"
            clearable
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDeleteDocDialog = false">取消</el-button>
        <el-button type="danger" @click="deleteByDocumentName" :loading="docDeleteLoading">确认删除</el-button>
      </template>
    </el-dialog>
  </div>

  <button class="chat-fab" @click="chatVisible = true" title="智能对话调试">💬</button>
  <ChatDialog v-model="chatVisible" />
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  UploadFilled,
  ChatDotRound,
  Upload,
  Plus,
  CircleCheckFilled,
  Loading,
  Check,
  Document,
  Delete
} from '@element-plus/icons-vue'
import request from '@/utils/request'
import ChatDialog from '@/components/chat/ChatDialog.vue'
import ContentTooltip from '@/components/shared/ContentTooltip.vue'

const API_BASE = '/knowledge'
const buildUrl = (path = '') => `${API_BASE}/${path}`.replace(/\/+/g, '/')

const loading = ref(false)
const docLoading = ref(false)
const docDeleteLoading = ref(false)
const savingFaq = ref(false)
const docUploading = ref(false)
const editDialogVisible = ref(false)
const showDeleteDocDialog = ref(false)
const chatVisible = ref(false)
const activeTab = ref('faq')

const uploadUrl = '/knowledge/upload'
const token = localStorage.getItem('auth-store') ? JSON.parse(localStorage.getItem('auth-store'))?.token || '' : ''
const uploadHeaders = { Authorization: `Bearer ${token}` }

const form = ref({
  question: '',
  answer: '',
  category: '通用'
})

const editForm = ref({
  id: '',
  question: '',
  answer: '',
  category: '通用'
})

const searchForm = ref({
  keyword: '',
  category: ''
})

const deleteDocForm = ref({
  document_name: ''
})

const pendingDocList = ref([])

// ====== 问答对数据 (接口#1) ======
const knowledgeList = ref([])
const filteredFaqList = ref([])
const faqPage = ref(1)
const faqPageSize = ref(10)
const faqTotal = computed(() => filteredFaqList.value.length)

const paginatedFaqList = computed(() => {
  const start = (faqPage.value - 1) * faqPageSize.value
  const end = start + faqPageSize.value
  return filteredFaqList.value.slice(start, end)
})

// ====== 文档分块数据 (接口#2) ======
const documentList = ref([])
const docPage = ref(1)
const docPageSize = ref(10)
const docTotal = computed(() => documentList.value.length)

const paginatedDocList = computed(() => {
  const start = (docPage.value - 1) * docPageSize.value
  const end = start + docPageSize.value
  return documentList.value.slice(start, end)
})

// ====== 文档上传相关方法（手动上传模式）======

const handleDocFileChange = (uploadFile, fileList) => {
  pendingDocList.value = fileList
}

const handleDocFileRemove = (uploadFile, fileList) => {
  pendingDocList.value = fileList
}

const confirmDocUpload = async () => {
  if (pendingDocList.value.length === 0) {
    ElMessage.warning('请先选择要上传的文件')
    return
  }

  docUploading.value = true
  let successCount = 0
  let failCount = 0

  for (const file of pendingDocList.value) {
    file.status = 'uploading'
    
    const formData = new FormData()
    formData.append('file', file.raw)

    try {
      const response = await request.post(uploadUrl, formData, {
        headers: uploadHeaders,
        onUploadProgress: (progressEvent) => {
          if (progressEvent.total) {
            file.percentage = Math.round((progressEvent.loaded * 100) / progressEvent.total)
          }
        }
      })

      if (response.code === 200) {
        const { saved_filename, filename, file_size } = response.data
        file.status = 'success'
        file.name = saved_filename || filename
        file.size = file_size
        successCount++
      } else {
        file.status = 'error'
        failCount++
      }
    } catch (error) {
      console.error('文档上传错误:', error)
      file.status = 'error'
      failCount++
    }
  }

  docUploading.value = false

  if (successCount > 0) {
    ElMessage.success(`成功上传 ${successCount} 个文件，正在后台解析...`)
    
    setTimeout(() => {
      refreshCurrentTab()
    }, 2000)
  }

  if (failCount > 0) {
    ElMessage.error(`${failCount} 个文件上传失败，请检查格式和大小`)
  }
}

const clearPendingDocs = () => {
  pendingDocList.value = []
}

const removePendingDoc = (index) => {
  pendingDocList.value.splice(index, 1)
}

const getStatusText = (file) => {
  switch (file.status) {
    case 'uploading':
      return `上传中... ${file.percentage || 0}%`
    case 'success':
      return `✓ 已上传 (${formatFileSize(file.size)})`
    case 'error':
      return '✗ 上传失败'
    default:
      return `待上传 (${formatFileSize(file.raw?.size)})`
  }
}

const formatFileSize = (bytes) => {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

// 提取实际文档名称（去掉时间戳前缀）
const getActualDocName = (documentName) => {
  if (!documentName) return '-'
  // 格式: "20260512_190225_9bc3b228_启动方式" -> "启动方式"
  const parts = documentName.split('_')
  if (parts.length >= 4) {
    return parts.slice(3).join('_')
  }
  return documentName
}

// 格式化上传日期（从文档名中提取时间戳）
// const formatUploadDate = (documentName) => {
//   if (!documentName) return '-'
//   // 格式: "20260512_190225_9bc3b228_启动方式" -> "2026-05-12 19:02:25"
//   const parts = documentName.split('_')
//   if (parts.length >= 2) {
//     const datePart = parts[0] // "20260512"
//     const timePart = parts[1] // "190225"
//     if (datePart.length === 8 && timePart.length === 6) {
//       const year = datePart.substring(0, 4)
//       const month = datePart.substring(4, 6)
//       const day = datePart.substring(6, 8)
//       const hour = timePart.substring(0, 2)
//       const minute = timePart.substring(2, 4)
//       const second = timePart.substring(4, 6)
//       return `${year}-${month}-${day} ${hour}:${minute}:${second}`
//     }
//   }
//   return '-'
// }
// ====== 接口#1: 获取问答对知识项 GET /api/knowledge/faq ======
const fetchKnowledge = async () => {
  loading.value = true
  try {
    const res = await request.get(buildUrl('faq'))
    knowledgeList.value = res.data || []
    filteredFaqList.value = knowledgeList.value
  } catch {
    ElMessage.error('获取问答对知识库失败')
  } finally {
    loading.value = false
  }
}

// ====== 接口#2: 获取文档分块知识项 GET /api/knowledge/documents ======
const fetchDocuments = async () => {
  docLoading.value = true
  try {
    const res = await request.get(buildUrl('documents'))
    documentList.value = res.data || []
  } catch {
    ElMessage.error('获取文档分块失败')
  } finally {
    docLoading.value = false
  }
}

const handleTabChange = (tab) => {
  if (tab === 'documents' && documentList.value.length === 0) {
    fetchDocuments()
  }
}

const refreshCurrentTab = () => {
  if (activeTab.value === 'faq') {
    fetchKnowledge()
  } else {
    fetchDocuments()
  }
}

const searchKnowledge = () => {
  const { keyword, category } = searchForm.value
  if (!keyword && !category) {
    filteredFaqList.value = knowledgeList.value
  } else {
    filteredFaqList.value = knowledgeList.value.filter(item => {
      const matchKeyword = !keyword ||
        item.question.toLowerCase().includes(keyword.toLowerCase()) ||
        item.answer.toLowerCase().includes(keyword.toLowerCase())
      const matchCategory = !category || item.category === category
      return matchKeyword && matchCategory
    })
  }
  faqPage.value = 1
}

const resetSearch = () => {
  searchForm.value = { keyword: '', category: '' }
  filteredFaqList.value = knowledgeList.value
  faqPage.value = 1
}

const saveKnowledge = async () => {
  if (!form.value.question.trim() || !form.value.answer.trim()) {
    ElMessage.warning('请填写完整信息')
    return
  }

  savingFaq.value = true
  try {
    await request.post(buildUrl(), {
      question: form.value.question,
      answer: form.value.answer,
      category: form.value.category
    })
    ElMessage.success('添加成功')
    form.value = { question: '', answer: '', category: '通用' }
    fetchKnowledge()
  } catch {
    ElMessage.error('添加失败')
  } finally {
    savingFaq.value = false
  }
}

const editKnowledge = (row) => {
  editForm.value = {
    id: row.id,
    question: row.question,
    answer: row.answer,
    category: row.category || '通用'
  }
  editDialogVisible.value = true
}

const confirmEdit = async () => {
  if (!editForm.value.question.trim() || !editForm.value.answer.trim()) {
    ElMessage.warning('请填写完整信息')
    return
  }

  try {
    await request.put(buildUrl(editForm.value.id), {
      question: editForm.value.question,
      answer: editForm.value.answer,
      category: editForm.value.category
    })
    ElMessage.success('更新成功')
    editDialogVisible.value = false
    fetchKnowledge()
  } catch {
    ElMessage.error('更新失败')
  }
}

const deleteKnowledge = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除该知识条目吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await request.delete(buildUrl(id))
    ElMessage.success('删除成功')
    fetchKnowledge()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// ====== 接口#6: 按文档名删除整篇文档 DELETE /api/knowledge/document?document_name=xxx ======
const deleteByDocumentName = async () => {
  const docName = deleteDocForm.value.document_name.trim()
  if (!docName) {
    ElMessage.warning('请输入要删除的文档名称')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要删除文档「${docName}」的所有分块吗？此操作不可恢复！`,
      '确认删除',
      {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    docDeleteLoading.value = true
    await request.delete('/api/knowledge/document', {
      params: { document_name: docName }
    })
    ElMessage.success(`文档「${docName}」及其所有分块已删除`)
    showDeleteDocDialog.value = false
    deleteDocForm.value.document_name = ''
    fetchDocuments()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error?.response?.data?.message || '删除失败')
    }
  } finally {
    docDeleteLoading.value = false
  }
}

const deleteDocChunk = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除该文档分块吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await request.delete(buildUrl(`documents/${id}`))
    ElMessage.success('删除成功')
    fetchDocuments()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const getCategoryTagType = (category) => {
  const types = {
    '申请流程': 'primary',
    '产品咨询': 'success',
    '还款问题': 'warning',
    '通用': 'info'
  }
  return types[category] || 'info'
}

onMounted(async () => {
  await fetchKnowledge()
})
</script>

<style scoped>
.knowledge-management {
  padding: 20px;
  padding-top: 0;
}

/* ====== 左右分栏布局 ====== */
.upload-section {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 20px;
  align-items: stretch;
}

@media (max-width: 1200px) {
  .upload-section {
    grid-template-columns: 1fr;
  }
}

/* ====== 卡片通用样式 ====== */
.add-faq-card,
.upload-doc-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.card-header .el-icon {
  font-size: 20px;
  color: #409eff;
}

.card-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.card-desc {
  margin: 0 0 16px 0;
  font-size: 13px;
  color: #909399;
  padding-left: 28px;
}

.faq-form {
  margin-top: 8px;
}

/* ====== FAQ附件上传（禁用/待开发） ====== */
.faq-attachment-upload {
  width: 100%;
}

.faq-attachment-upload :deep(.el-upload) {
  width: 100%;
}

.faq-attachment-upload :deep(.el-upload-dragger) {
  width: 100%;
  height: 90px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border: 1.5px dashed #e4e7ed;
  border-radius: 6px;
  background: #f5f7fa;
  cursor: not-allowed;
  opacity: 0.75;
  transition: none;
}

.faq-attachment-upload :deep(.el-upload-dragger:hover) {
  border-color: #e4e7ed;
  background: #f5f7fa;
}

.faq-upload-icon {
  font-size: 28px;
  color: #c0c4cc;
  margin-bottom: 4px;
}

.faq-upload-text {
  font-size: 13px;
  color: #909399;
}

.faq-upload-tip {
  margin-top: 6px;
  font-size: 11px;
  color: #b88230;
  text-align: center;
}

/* ====== 文档上传按钮区域 ====== */
.upload-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 14px;
  padding: 12px 16px;
  background: #f5f7fa;
  border-radius: 6px;
}

.upload-actions .el-button {
  flex-shrink: 0;
}

.actions-hint {
  font-size: 13px;
  color: #606266;
}

/* ====== 文档上传区域样式 ====== */
.doc-upload-demo {
  width: 100%;
}

.doc-upload-demo :deep(.el-upload) {
  width: 100%;
}

.doc-upload-demo :deep(.el-upload-dragger) {
  width: 100%;
  height: 100px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border: 2px dashed #d9d9d9;
  border-radius: 8px;
  background: #fafafa;
  transition: all 0.3s;
}

.doc-upload-demo :deep(.el-upload-dragger:hover) {
  border-color: #409eff;
  background: #ecf5ff;
}

.doc-upload-icon {
  font-size: 48px;
  color: #c0c4cc;
  margin-bottom: 12px;
}

.doc-upload-text {
  font-size: 14px;
  color: #606266;
}

.doc-upload-text em {
  color: #409eff;
  font-style: normal;
}

/* ====== 上传指南 ====== */
.upload-guide {
  margin-top: 16px;
  padding: 12px 16px;
  background: #f5f7fa;
  border-radius: 6px;
  border-left: 3px solid #409eff;
}

.guide-title {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.guide-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.guide-list li {
  font-size: 12px;
  color: #606266;
  line-height: 1.8;
  display: flex;
  gap: 8px;
  align-items: flex-start;
}

.guide-list li strong {
  color: #303133;
  min-width: 110px;
}

.guide-list li span {
  color: #909399;
}

.guide-note {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px dashed #e4e7ed;
  font-size: 12px;
  color: #e6a23c;
}

/* ====== 上传结果列表 ====== */
.upload-result-list {
  margin-top: 16px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  overflow: hidden;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  background: #fafafa;
  border-bottom: 1px solid #ebeef5;
  font-size: 13px;
  font-weight: 500;
  color: #606266;
}

.result-items {
  max-height: 200px;
  overflow-y: auto;
}

.result-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-bottom: 1px solid #f0f0f0;
  transition: background 0.2s;
}

.result-item:last-child {
  border-bottom: none;
}

.result-item:hover {
  background: #f5f7fa;
}

.result-item .el-icon {
  font-size: 18px;
  flex-shrink: 0;
}

.result-item.success .el-icon {
  color: #67c23a;
}

.result-item.error .el-icon {
  color: #f56c6c;
}

.result-item.uploading .el-icon {
  color: #409eff;
  animation: rotating 1.5s linear infinite;
}

@keyframes rotating {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.item-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.item-name {
  font-size: 13px;
  color: #303133;
  word-break: break-all;
}

.item-status {
  font-size: 11px;
  color: #909399;
}

/* ====== 搜索和列表区域 ====== */
.search-form {
  margin-bottom: 20px;
}
.knowledge-list {
  margin-top: 10px;
}
.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}
.list-header h3 {
  margin: 0;
}
.knowledge-tabs {
  margin-top: 5px;
}
.tab-info {
  font-size: 13px;
  color: var(--color-info);
  margin-bottom: 12px;
  display: flex;
  align-items: center;
}
.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.chat-fab {
  position: fixed;
  right: 32px;
  bottom: 32px;
  width: 52px;
  height: 52px;
  border: none;
  border-radius: 50%;
  background: var(--fab-gradient);
  color: var(--fab-text);
  font-size: 22px;
  cursor: pointer;
  box-shadow: 0 4px 16px var(--fab-shadow);
  z-index: 9998;
  transition: transform 0.2s, box-shadow 0.2s;
}
.chat-fab:hover {
  transform: scale(1.08);
  box-shadow: 0 6px 24px var(--fab-shadow-hover);
}

.cell-text {
  display: inline-block;
  max-width: 150px;
  /* 其他样式由 ContentTooltip 内部的 .tooltip-trigger 处理 */
}

.document-content-viewer {
  max-height: 70vh;
  overflow-y: auto;
}
.content-meta {
  margin-bottom: 20px;
}
.content-body h4 {
  margin: 0 0 12px 0;
  font-size: 16px;
  color: var(--text-color);
}
.content-text {
  background: var(--knowledge-content-bg, #f5f7fa);
  padding: 16px;
  border-radius: 8px;
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.8;
  font-size: 14px;
  color: var(--text-color);
  max-height: 400px;
  overflow-y: auto;
  margin: 0;
}
</style>
