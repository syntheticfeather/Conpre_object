<template>
  <div class="agent-config">
    <div class="header">Agent 配置管理</div>

    <div class="config-split">
      <!-- 左栏：Prompt 编辑表单（代码编辑器感灰底 + 等宽字体） -->
      <el-card class="config-card editor-card">
        <template #header>
          <div class="card-header">
            <span>新增提示词</span>
            <div class="header-actions">
              <el-button type="primary" @click="createPrompt">创建</el-button>
              <el-button @click="resetCreateForm">重置</el-button>
            </div>
          </div>
        </template>

        <el-form label-position="top" class="prompt-editor">
          <el-form-item label="提示词名称">
            <el-input v-model="createName" placeholder="输入提示词名称，如「贷款客服 v1」" />
          </el-form-item>
          <el-form-item label="角色定义（系统规则）">
            <el-input
              v-model="createContent.role_definition"
              type="textarea"
              :rows="7"
              placeholder="定义 AI 的身份和角色定位"
            />
          </el-form-item>
          <el-form-item label="业务规则">
            <el-input
              v-model="createContent.business_rules"
              type="textarea"
              :rows="5"
              placeholder="业务逻辑约束和行为规则"
            />
          </el-form-item>
          <el-form-item label="回复风格">
            <el-input
              v-model="createContent.tone_style"
              type="textarea"
              :rows="4"
              placeholder="回复的语气、风格要求"
            />
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 右栏：提示词列表（卡片堆叠 + 激活开关 + 语义色边） -->
      <el-card class="config-card list-card">
        <template #header>
          <div class="card-header">
            <span>提示词列表</span>
            <el-button type="warning" @click="fetchPrompts">刷新</el-button>
          </div>
        </template>

        <div v-loading="loading" class="prompt-card-list">
          <div v-if="!promptsList.length && !loading" class="prompt-empty">暂无提示词数据</div>
          <div
            v-for="row in promptsList"
            :key="row.prompt_id"
            class="prompt-item"
            :class="{ 'is-active': row.is_active }"
          >
            <div class="prompt-item__head">
              <span class="prompt-item__name">{{ row.name }}</span>
              <el-switch :model-value="row.is_active" @change="toggleActive(row)" />
            </div>
            <div class="prompt-item__meta">
              <el-tag size="small" effect="plain">{{ row.category || '通用' }}</el-tag>
              <span class="prompt-item__version">v{{ row.version }}</span>
              <span class="prompt-item__time">更新于 {{ row.updated_at || '—' }}</span>
            </div>
            <div class="prompt-item__actions">
              <el-button size="small" type="primary" text @click="openEditDialog(row)">编辑</el-button>
              <el-button
                size="small"
                :type="row.is_active ? 'warning' : 'success'"
                text
                @click="toggleActive(row)"
              >
                {{ row.is_active ? '停用' : '激活' }}
              </el-button>
              <el-button size="small" type="danger" text @click="deletePrompt(row)">删除</el-button>
            </div>
          </div>
        </div>
      </el-card>
    </div>

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

    <!-- 悬浮入口与调试弹窗放在根节点内部，保证组件为单一根节点以支持路由过渡动画 -->
    <button class="chat-fab" @click="chatVisible = true" title="智能对话调试">💬</button>
    <ChatDialog v-model="chatVisible" />
  </div>
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

/* ===== 双栏布局：左编辑 / 右列表 ===== */
.config-split {
  display: flex;
  align-items: flex-start;
  gap: 16px;
}

.editor-card {
  flex: 0 0 65%;
  position: sticky;
  top: 84px;
}

.list-card {
  flex: 1 1 35%;
  min-width: 0;
}

/* ===== 左栏：代码编辑器感（深色灰底 + 等宽字体 + 内阴影） ===== */
.prompt-editor :deep(.el-textarea__inner) {
  background: #23272e;
  color: #d4dae3;
  font-family: 'JetBrains Mono', 'Fira Code', Consolas, Monaco, 'Courier New', monospace;
  font-size: 12.5px;
  line-height: 1.75;
  border: 1px solid #313742;
  border-radius: 8px;
  box-shadow: inset 0 1px 4px rgba(0, 0, 0, 0.3);
  transition: border-color var(--duration-fast, 150ms) ease;
}

.prompt-editor :deep(.el-textarea__inner:focus) {
  border-color: var(--brand-500, #3b82f6);
}

.prompt-editor :deep(.el-textarea__inner::placeholder) {
  color: #6b7686;
}

/* ===== 右栏：提示词卡片堆叠 ===== */
.prompt-card-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: calc(100vh - 240px);
  overflow-y: auto;
  padding-right: 4px;
}

.prompt-item {
  padding: 12px 16px;
  background: var(--card-bg, #fff);
  border: 1px solid var(--border-color-light, #ebeef5);
  border-left: 3px solid var(--border-color-base, #dcdfe6);
  border-radius: 10px;
  transition: transform var(--duration-fast, 150ms) var(--ease-out-expo, ease),
              box-shadow var(--duration-fast, 150ms) var(--ease-out-expo, ease),
              border-color var(--duration-fast, 150ms) ease;
}

.prompt-item:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba(15, 23, 42, 0.08);
}

/* 激活态：品牌青左边框 + 青色浅光晕（语义色边） */
.prompt-item.is-active {
  border-left-color: var(--brand-accent, #25e0bf);
  box-shadow: 0 2px 10px rgba(37, 224, 191, 0.14);
}

.prompt-item__head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.prompt-item__name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-color, #303133);
}

.prompt-item__meta {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 10px 0;
  font-size: 12px;
  color: var(--text-secondary, #606266);
}

.prompt-item__time {
  margin-left: auto;
}

.prompt-item__actions {
  display: flex;
  gap: 4px;
  padding-top: 8px;
  border-top: 1px dashed var(--border-color-light, #ebeef5);
}

.prompt-empty {
  padding: 48px 0;
  text-align: center;
  color: var(--text-color-placeholder, #909399);
}

/* 小屏降级为上下堆叠 */
@media (max-width: 1024px) {
  .config-split {
    flex-direction: column;
  }

  .editor-card,
  .list-card {
    flex: 1 1 auto;
    width: 100%;
    position: static;
  }

  .prompt-card-list {
    max-height: none;
  }
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
