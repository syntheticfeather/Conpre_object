<template>
  <div class="knowledge-management">
    <h2>知识库管理</h2>

    <!-- 添加表单 -->
    <el-card class="add-form">
      <h3>添加知识条目</h3>
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
          <el-button type="primary" @click="addKnowledge">添加</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 知识列表 -->
    <div class="knowledge-list-container">
     <el-card class="knowledge-list">
      <h3>常见/通用问题</h3>
      <el-table :data="knowledgeList" stripe>
        <el-table-column prop="question" label="问题" />
        <el-table-column prop="answer" label="答案" show-overflow-tooltip />
        <el-table-column prop="category" label="分类" width="100" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button type="danger" size="small" @click="deleteKnowledge(row.id)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    
    <el-card class="knowledge-list">
      <h3>申请流程</h3>
      <el-table :data="knowledgeList" stripe>
        <el-table-column prop="question" label="问题" />
        <el-table-column prop="answer" label="答案" show-overflow-tooltip />
        <el-table-column prop="category" label="分类" width="100" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button type="danger" size="small" @click="deleteKnowledge(row.id)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    
    <el-card class="knowledge-list">
      <h3>产品咨询</h3>
      <el-table :data="knowledgeList" stripe>
        <el-table-column prop="question" label="问题" />
        <el-table-column prop="answer" label="答案" show-overflow-tooltip />
        <el-table-column prop="category" label="分类" width="100" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button type="danger" size="small" @click="deleteKnowledge(row.id)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card class="knowledge-list">
      <h3>还款问题</h3>
      <el-table :data="knowledgeList" stripe>
        <el-table-column prop="question" label="问题" />
        <el-table-column prop="answer" label="答案" show-overflow-tooltip />
        <el-table-column prop="category" label="分类" width="100" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button type="danger" size="small" @click="deleteKnowledge(row.id)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const API_BASE = 'http://localhost:8000/knowledge'

const form = ref({
  question: '',
  answer: '',
  category: '通用'
})

const knowledgeList = ref([])

const fetchKnowledge = async () => {
  try {
    const res = await axios.get(API_BASE)
    knowledgeList.value = res.data
  } catch (error) {
    ElMessage.error('获取知识库失败')
    console.error('Error fetching knowledge:', error)
  }
}

const addKnowledge = async () => {
  try {
    await axios.post(API_BASE, form.value)
    ElMessage.success('添加成功')
    form.value = { question: '', answer: '', category: '通用' }
    fetchKnowledge()
  } catch (error) {
    ElMessage.error('添加失败')
    console.error('Error adding knowledge:', error)
  }
}

const deleteKnowledge = async (id) => {
  try {
    await axios.delete(`${API_BASE}/${id}`)
    ElMessage.success('删除成功')
    fetchKnowledge()
  } catch (error) {
    ElMessage.error('删除失败')
    console.error('Error deleting knowledge:', error)
  }
}

onMounted(fetchKnowledge) 
</script>

<style scoped>
.knowledge-management {
  padding: 20px;
}
.add-form {
  margin-bottom: 20px;
}
.knowledge-list-container {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
}
.knowledge-list {
  margin-top: 10px;
}

</style>
