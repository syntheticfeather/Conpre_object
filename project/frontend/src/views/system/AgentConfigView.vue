<template>
  <div class="agent-config">
    <div class="header">Agent 配置管理</div>

    <el-card class="config-card">
      <template #header>
        <div class="card-header">
          <span>新增提示词</span>
          <div class="header-actions">
            <el-button type="primary" @click="createPrompt">创建</el-button>
            <el-button @click="resetCreateForm">重置</el-button>
          </div>
        </div>
      </template>

      <el-form label-width="120px">
        <el-form-item label="提示词名称">
          <el-input v-model="createName" placeholder="输入提示词名称，如「贷款客服 v1」" />
        </el-form-item>
        <el-form-item label="角色定义">
          <el-input
            v-model="createContent.role_definition"
            type="textarea"
            :rows="6"
            placeholder="定义 AI 的身份和角色定位"
          />
        </el-form-item>
        <el-form-item label="业务规则">
          <el-input
            v-model="createContent.business_rules"
            type="textarea"
            :rows="4"
            placeholder="业务逻辑约束和行为规则"
          />
        </el-form-item>
        <el-form-item label="回复风格">
          <el-input
            v-model="createContent.tone_style"
            type="textarea"
            :rows="3"
            placeholder="回复的语气、风格要求"
          />
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="config-card">
      <template #header>
        <div class="card-header">
          <span>提示词列表</span>
          <el-button type="warning" @click="fetchPrompts">刷新</el-button>
        </div>
      </template>

      <el-table :data="promptsList" stripe v-loading="loading" empty-text="暂无提示词数据">
        <el-table-column prop="name" label="名称" min-width="150" />
        <el-table-column prop="version" label="版本" width="80" />
        <el-table-column prop="category" label="分类" width="100" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.is_active ? 'success' : 'info'">
              {{ row.is_active ? '激活' : '未激活' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="created_at" label="创建时间" width="180" />
        <el-table-column prop="updated_at" label="更新时间" width="180" sortable />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="openEditDialog(row)">编辑</el-button>
            <el-button
              size="small"
              :type="row.is_active ? 'warning' : 'success'"
              @click="toggleActive(row)"
            >
              {{ row.is_active ? '停用' : '激活' }}
            </el-button>
            <el-button size="small" type="danger" @click="deletePrompt(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="editDialogVisible" title="编辑提示词" width="700px">
      <el-form label-width="120px">
        <el-form-item label="提示词名称">
          <el-input v-model="editName" placeholder="输入提示词名称" />
        </el-form-item>
        <el-form-item label="角色定义">
          <el-input
            v-model="editContent.role_definition"
            type="textarea"
            :rows="6"
            placeholder="定义 AI 的身份和角色定位"
          />
        </el-form-item>
        <el-form-item label="业务规则">
          <el-input
            v-model="editContent.business_rules"
            type="textarea"
            :rows="4"
            placeholder="业务逻辑约束和行为规则"
          />
        </el-form-item>
        <el-form-item label="回复风格">
          <el-input
            v-model="editContent.tone_style"
            type="textarea"
            :rows="3"
            placeholder="回复的语气、风格要求"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>

  <button class="chat-fab" @click="chatVisible = true" title="智能对话调试">💬</button>
  <ChatDialog v-model="chatVisible" />
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import promptsAPI from '@/api/modules/prompts'
import ChatDialog from '@/components/chat/ChatDialog.vue'

const loading = ref(false)
const chatVisible = ref(false)
const promptsList = ref([])

const defaultContent = {
  role_definition: `你是一个友好的贷款智能客服。请根据以下规则回答用户问题：

1. 如果用户询问贷款申请状态，使用 query_application_status 工具查询
2. 如果用户询问还款计划/月还款额，使用 calculate_repayment 工具计算
3. 如果用户询问其他问题，优先从知识库中查找答案
4. 如果知识库没有相关信息，给出通用建议并引导用户提供更多信息`,
  business_rules: '',
  tone_style: `回答要求：
- 语气友好、专业
- 简洁明了
- 对于需要调用工具的问题，先调用工具再基于结果回答`
}

const createName = ref('')
const createContent = ref({ ...defaultContent })

const editDialogVisible = ref(false)
const editId = ref(null)
const editName = ref('')
const editContent = ref({ ...defaultContent })

// ====== 获取所有提示词列表 GET /api/prompts ======
const fetchPrompts = async () => {
  loading.value = true
  try {
    const res = await promptsAPI.getAll()
    promptsList.value = res.data || []
  } catch {
    ElMessage.error('获取提示词列表失败')
    promptsList.value = []
  } finally {
    loading.value = false
  }
}

// ====== 创建提示词 ======
const createPrompt = async () => {
  if (!createName.value.trim()) {
    ElMessage.warning('请填写提示词名称')
    return
  }

  try {
    await promptsAPI.create({ name: createName.value, content: createContent.value })
    ElMessage.success('创建成功')
    resetCreateForm()
    await fetchPrompts()
  } catch {
    ElMessage.error('创建失败')
  }
}

const resetCreateForm = () => {
  createName.value = ''
  createContent.value = { ...defaultContent }
  ElMessage.success('已重置表单')
}

// ====== 编辑提示词 ======
const openEditDialog = async (prompt) => {
  if (!prompt || !prompt.prompt_id) {
    ElMessage.warning('无法获取提示词信息')
    return
  }
  try {
    const res = await promptsAPI.getById(prompt.prompt_id)
    if (res.data) {
      editId.value = res.data.prompt_id
      editName.value = res.data.name || ''
      editContent.value = {
        role_definition: res.data.content?.role_definition || '',
        business_rules: res.data.content?.business_rules || '',
        tone_style: res.data.content?.tone_style || ''
      }
      editDialogVisible.value = true
    }
  } catch {
    ElMessage.error('获取提示词详情失败')
  }
}

const saveEdit = async () => {
  if (!editName.value.trim()) {
    ElMessage.warning('请填写提示词名称')
    return
  }

  try {
    await promptsAPI.update(editId.value, { name: editName.value, content: editContent.value })
    ElMessage.success('更新成功')
    editDialogVisible.value = false
    await fetchPrompts()
  } catch {
    ElMessage.error('保存失败')
  }
}

// ====== 切换激活状态 ======
const toggleActive = async (prompt) => {
  try {
    const newActive = !prompt.is_active
    await promptsAPI.update(prompt.prompt_id, { is_active: newActive })
    ElMessage.success(newActive ? '已激活' : '已停用')
    await fetchPrompts()
  } catch {
    ElMessage.error('操作失败')
  }
}

// ====== 删除提示词 ======
const deletePrompt = async (prompt) => {
  try {
    await ElMessageBox.confirm(`确定要删除提示词「${prompt.name}」吗？`, '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await promptsAPI.delete(prompt.prompt_id)
    ElMessage.success('删除成功')
    await fetchPrompts()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(async () => {
  await fetchPrompts()
})
</script>

<style scoped>
.agent-config {
  padding: 20px;
  padding-top: 0;
}
.config-card {
  margin-bottom: 20px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
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
