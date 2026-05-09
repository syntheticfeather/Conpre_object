<template>
  <div class="knowledge-management">
    <div class="header">知识库管理</div>

    <el-card class="add-form">
      <h3>添加知识条目</h3>
      <el-form :model="form" label-width="80px">
        <el-form-item label="问题">
          <el-input v-model="form.question" placeholder="请输入问题" />
        </el-form-item>
        <el-form-item label="答案">
          <el-input v-model="form.answer" type="textarea" :rows="3" placeholder="请输入答案" />
        </el-form-item>
        <el-form-item label="附件">
          <el-upload
            ref="addUploadRef"
            class="upload-demo"
            drag
            :disabled="uploadDisabled"
            :action="uploadUrl"
            :headers="uploadHeaders"
            :on-success="handleAddUploadSuccess"
            :on-error="handleUploadError"
            :on-remove="handleAddUploadRemove"
            :file-list="addFileList"
            :limit="5"
            :multiple="true"
            name="file"
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">
              将文件拖拽到此处或<em>点击上传</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">
                支持上传图片或文档文件
              </div>
            </template>
            <template #file="{ file }">
              <div class="upload-file-item">
                <img v-if="isImageFile(file)" :src="file.url" class="upload-preview-img" />
                <div v-else class="file-type-icon">
                  <el-icon :size="32"><Document /></el-icon>
                </div>
                <div class="upload-file-actions">
                  <el-icon @click="removeAddFile(file)"><Delete /></el-icon>
                </div>
              </div>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.category" placeholder="选择分类">
            <el-option label="申请流程" value="申请流程" />
            <el-option label="产品咨询" value="产品咨询" />
            <el-option label="还款问题" value="还款问题" />
            <el-option label="通用" value="通用" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="saveKnowledge">添加</el-button>
        </el-form-item>
      </el-form>
    </el-card>

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

    <el-card class="knowledge-list">
      <div class="list-header">
        <h3>知识库列表（共 {{ total }} 条）</h3>
        <el-button type="primary" @click="fetchKnowledge">刷新</el-button>
      </div>
      <el-table :data="paginatedList" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="200" />
        <el-table-column prop="question" label="问题" show-overflow-tooltip />
        <el-table-column prop="answer" label="答案" show-overflow-tooltip />
        <el-table-column prop="category" label="分类" width="100">
          <template #default="{ row }">
            <el-tag :type="getCategoryTagType(row.category)">{{ row.category }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="附件" width="120">
          <template #default="{ row }">
            <div v-if="row.attachments && row.attachments.length > 0" class="attachment-cell">
              <el-tooltip placement="top">
                <template #content>
                  <div v-for="(att, idx) in row.attachments" :key="idx" class="attachment-tip-item">
                    <el-link :href="getFileUrl(att)" target="_blank" :underline="false">
                      <el-icon><Link /></el-icon> {{ getFileName(att) }}
                    </el-link>
                  </div>
                </template>
                <el-tag size="small" type="warning">
                  <el-icon style="vertical-align: middle"><Paperclip /></el-icon>
                  {{ row.attachments.length }} 个文件
                </el-tag>
              </el-tooltip>
            </div>
            <span v-else class="no-attachment">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="created_at" label="创建时间" width="170" />
        <el-table-column prop="updated_at" label="更新时间" width="170" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="editKnowledge(row)">编辑</el-button>
            <el-button type="danger" size="small" @click="deleteKnowledge(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="editDialogVisible" title="编辑知识条目" width="600px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="问题">
          <el-input v-model="editForm.question" placeholder="请输入问题" />
        </el-form-item>
        <el-form-item label="答案">
          <el-input v-model="editForm.answer" type="textarea" :rows="4" placeholder="请输入答案" />
        </el-form-item>
        <el-form-item label="附件">
          <el-upload
            ref="editUploadRef"
            class="upload-demo"
            drag
            :disabled="uploadDisabled"
            :action="uploadUrl"
            :headers="uploadHeaders"
            :on-success="handleEditUploadSuccess"
            :on-error="handleUploadError"
            :on-remove="handleEditUploadRemove"
            :file-list="editFileList"
            :limit="5"
            :multiple="true"
            name="file"
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">
              将文件拖拽到此处或<em>点击上传</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">
                支持上传图片或文档文件
              </div>
            </template>
            <template #file="{ file }">
              <div class="upload-file-item">
                <img v-if="isImageFile(file)" :src="file.url" class="upload-preview-img" />
                <div v-else class="file-type-icon">
                  <el-icon :size="32"><Document /></el-icon>
                </div>
                <div class="upload-file-actions">
                  <el-icon @click="removeEditFile(file)"><Delete /></el-icon>
                </div>
              </div>
            </template>
          </el-upload>
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
  </div>

  <button class="chat-fab" @click="chatVisible = true" title="智能对话调试">💬</button>
  <ChatDialog v-model="chatVisible" />
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UploadFilled, Document, Delete, Paperclip, Link } from '@element-plus/icons-vue'
import request from '@/utils/request'
import ChatDialog from '@/components/chat/ChatDialog.vue'

const API_BASE = '/knowledge'
const buildUrl = (path = '') => `${API_BASE}/${path}`.replace(/\/+/g, '/')

const loading = ref(false)
const editDialogVisible = ref(false)
const chatVisible = ref(false)
const uploadDisabled = ref(true)

const addUploadRef = ref(null)
const editUploadRef = ref(null)
const addFileList = ref([])
const editFileList = ref([])

const uploadUrl = '/api/upload/knowledge'
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
  category: '通用',
  attachments: []
})

const searchForm = ref({
  keyword: '',
  category: ''
})

const knowledgeList = ref([])
const filteredList = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const paginatedList = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredList.value.slice(start, end)
})

const isImageFile = (file) => {
  const imgExts = ['.jpg', '.jpeg', '.png', '.gif', '.bmp', '.webp']
  const name = file.name || file.url || ''
  const ext = name.substring(name.lastIndexOf('.')).toLowerCase()
  return imgExts.includes(ext)
}

const getFileName = (filepath) => {
  if (!filepath) return ''
  const parts = filepath.split('/')
  return parts[parts.length - 1]
}

const getFileUrl = (filepath) => {
  if (!filepath) return ''
  return `/uploads/${filepath}`
}

const fetchKnowledge = async () => {
  loading.value = true
  try {
    const res = await request.get(buildUrl())
    knowledgeList.value = res.data || []
    filteredList.value = knowledgeList.value
    total.value = filteredList.value.length
  } catch {
    ElMessage.error('获取知识库失败')
  } finally {
    loading.value = false
  }
}

const searchKnowledge = () => {
  const { keyword, category } = searchForm.value
  if (!keyword && !category) {
    filteredList.value = knowledgeList.value
  } else {
    filteredList.value = knowledgeList.value.filter(item => {
      const matchKeyword = !keyword || 
        item.question.toLowerCase().includes(keyword.toLowerCase()) ||
        item.answer.toLowerCase().includes(keyword.toLowerCase())
      const matchCategory = !category || item.category === category
      return matchKeyword && matchCategory
    })
  }
  total.value = filteredList.value.length
  currentPage.value = 1
}

const resetSearch = () => {
  searchForm.value = { keyword: '', category: '' }
  searchKnowledge()
}

const handleAddUploadSuccess = (response) => {
  if (response.code === 200) {
    const { filepath, filename } = response.data
    addFileList.value.push({ name: filename, url: getFileUrl(filepath), filepath })
    ElMessage.success(`${filename} 上传成功`)
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

const handleEditUploadSuccess = (response) => {
  if (response.code === 200) {
    const { filepath, filename } = response.data
    editFileList.value.push({ name: filename, url: getFileUrl(filepath), filepath })
    ElMessage.success(`${filename} 上传成功`)
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

const handleUploadError = () => {
  ElMessage.error('文件上传失败')
}

const removeAddFile = (file) => {
  const idx = addFileList.value.findIndex(f => f.uid === file.uid)
  if (idx > -1) addFileList.value.splice(idx, 1)
}

const handleAddUploadRemove = (file) => {
  removeAddFile(file)
}

const removeEditFile = (file) => {
  const idx = editFileList.value.findIndex(f => f.uid === file.uid)
  if (idx > -1) editFileList.value.splice(idx, 1)
}

const handleEditUploadRemove = (file) => {
  removeEditFile(file)
}

const saveKnowledge = async () => {
  if (!form.value.question.trim() || !form.value.answer.trim()) {
    ElMessage.warning('请填写完整信息')
    return
  }

  const attachments = addFileList.value.map(f => f.filepath).filter(Boolean)

  try {
    await request.post(buildUrl(), { ...form.value, attachments })
    ElMessage.success('添加成功')
    form.value = { question: '', answer: '', category: '通用' }
    addFileList.value = []
    fetchKnowledge()
  } catch {
    ElMessage.error('添加失败')
  }
}

const editKnowledge = (row) => {
  editForm.value = {
    id: row.id,
    question: row.question,
    answer: row.answer,
    category: row.category || '通用',
    attachments: row.attachments || []
  }
  editFileList.value = (row.attachments || []).map((fp, idx) => ({
    name: getFileName(fp),
    url: getFileUrl(fp),
    filepath: fp,
    uid: Date.now() + idx
  }))
  editDialogVisible.value = true
}

const confirmEdit = async () => {
  if (!editForm.value.question.trim() || !editForm.value.answer.trim()) {
    ElMessage.warning('请填写完整信息')
    return
  }

  const attachments = editFileList.value.map(f => f.filepath).filter(Boolean)

  try {
    await request.put(buildUrl(editForm.value.id), {
      question: editForm.value.question,
      answer: editForm.value.answer,
      category: editForm.value.category,
      attachments
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

const handleSizeChange = () => {
  currentPage.value = 1
}

const handleCurrentChange = () => {
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

onMounted(fetchKnowledge) 
</script>

<style scoped>
.knowledge-management {
  padding: 20px;
  padding-top: 0;
}
.add-form {
  margin-bottom: 20px;
}
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
.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.upload-file-item {
  position: relative;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.upload-preview-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.file-type-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  background: var(--hover-bg);
  color: var(--color-info);
}
.upload-file-actions {
  position: absolute;
  top: 2px;
  right: 2px;
  cursor: pointer;
  color: var(--color-danger);
  background: var(--knowledge-delete-icon-bg);
  border-radius: 50%;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.upload-demo {
  display: block;
}
.upload-demo :deep(.el-upload--drag) {
  width: 100%;
}
.upload-demo :deep(.el-upload-dragger) {
  width: 100%;
}

.attachment-cell {
  display: flex;
  align-items: center;
  gap: 4px;
}
.no-attachment {
  color: var(--text-color-disabled);
}
.attachment-tip-item {
  padding: 4px 0;
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
</style>
