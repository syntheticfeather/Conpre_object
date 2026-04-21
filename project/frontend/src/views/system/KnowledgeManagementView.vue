<template>
  <div class="knowledge-management">
    <h2>知识库管理</h2>

    <!-- 添加表单 -->
    <el-card class="add-form">
      <h3>{{ isEdit ? '编辑知识条目' : '添加知识条目' }}</h3>
      <el-form :model="form" label-width="80px">
        <el-form-item label="问题">
          <el-input v-model="form.question" placeholder="请输入问题" />
        </el-form-item>
        <el-form-item label="答案">
          <el-input v-model="form.answer" type="textarea" rows="3" placeholder="请输入答案" />
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
          <el-button type="primary" @click="saveKnowledge">
            {{ isEdit ? '更新' : '添加' }}
          </el-button>
          <el-button v-if="isEdit" @click="cancelEdit">取消编辑</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 搜索和过滤 -->
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

    <!-- 知识列表 -->
    <el-card class="knowledge-list">
      <div class="list-header">
        <h3>知识库列表（共 {{ total }} 条）</h3>
        <el-button type="success" @click="exportKnowledge">导出 Excel</el-button>
      </div>
      <el-table :data="paginatedList" stripe v-loading="loading">
        <el-table-column prop="question" label="问题" show-overflow-tooltip />
        <el-table-column prop="answer" label="答案" show-overflow-tooltip />
        <el-table-column prop="category" label="分类" width="100">
          <template #default="{ row }">
            <el-tag :type="getCategoryTagType(row.category)">{{ row.category }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="usage_count" label="使用次数" width="90" sortable />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="editKnowledge(row)">编辑</el-button>
            <el-button type="danger" size="small" @click="deleteKnowledge(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
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

    <!-- 编辑对话框 -->
    <el-dialog v-model="editDialogVisible" title="编辑知识条目" width="600px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="问题">
          <el-input v-model="editForm.question" placeholder="请输入问题" />
        </el-form-item>
        <el-form-item label="答案">
          <el-input v-model="editForm.answer" type="textarea" rows="4" placeholder="请输入答案" />
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
</template>

<script setup>
import { ref, computed} from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'

const API_BASE = 'http://localhost:8000/knowledge'

const loading = ref(false)
const isEdit = ref(false)
const editDialogVisible = ref(false)

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

// const fetchKnowledge = async () => {
//   loading.value = true
//   try {
//     const res = await axios.get(API_BASE)
//     knowledgeList.value = res.data || []
//     filteredList.value = knowledgeList.value
//     total.value = filteredList.value.length
//   } catch (error) {
//     ElMessage.error('获取知识库失败')
//     console.error('Error fetching knowledge:', error)
//   } finally {
//     loading.value = false
//   }
// }

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

const saveKnowledge = async () => {
  if (!form.value.question.trim() || !form.value.answer.trim()) {
    ElMessage.warning('请填写完整信息')
    return
  }
  
  try {
    if (isEdit.value) {
      await axios.put(`${API_BASE}/${editForm.value.id}`, editForm.value)
      ElMessage.success('更新成功')
    } else {
      await axios.post(API_BASE, form.value)
      ElMessage.success('添加成功')
    }
    form.value = { question: '', answer: '', category: '通用' }
    isEdit.value = false
    // fetchKnowledge()
  } catch (error) {
    ElMessage.error(isEdit.value ? '更新失败' : '添加失败')
    console.error('Error saving knowledge:', error)
  }
}

const editKnowledge = (row) => {
  isEdit.value = true
  editForm.value = { ...row }
  editDialogVisible.value = true
}

const confirmEdit = async () => {
  try {
    await axios.put(`${API_BASE}/${editForm.value.id}`, editForm.value)
    ElMessage.success('更新成功')
    editDialogVisible.value = false
    // fetchKnowledge()
  } catch (error) {
    ElMessage.error('更新失败'+error)
  }
}

const cancelEdit = () => {
  isEdit.value = false
  form.value = { question: '', answer: '', category: '通用' }
}

const deleteKnowledge = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除该知识条目吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await axios.delete(`${API_BASE}/${id}`)
    ElMessage.success('删除成功')
    // fetchKnowledge()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
      console.error('Error deleting knowledge:', error)
    }
  }
}

const handleSizeChange = () => {
  currentPage.value = 1
}

const handleCurrentChange = () => {
  // 分页变化时自动更新显示
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

const exportKnowledge = () => {
  ElMessage.info('导出功能开发中...')
}

// onMounted(fetchKnowledge) 
</script>

<style scoped>
.knowledge-management {
  padding: 20px;
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
</style>
