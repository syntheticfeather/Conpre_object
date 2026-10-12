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
          <el-form-item label="提示词档位">
            <el-select v-model="createCategory" style="width: 100%" placeholder="选择提示词分类">
              <el-option
                v-for="c in CATEGORY_OPTIONS"
                :key="c.value"
                :label="c.label"
                :value="c.value"
              />
            </el-select>
          </el-form-item>

          <!-- base/general 档：三层架构（系统规则 / 业务规则 / 风格规则） -->
          <template v-if="isCreateBaseDoc">
            <!-- L1 系统规则 -->
            <div class="layer-block">
              <div class="layer-block__title">
                <span class="layer-tag layer-tag--l1">L1</span>
                系统规则 · 角色定义
              </div>
              <p class="layer-block__desc">定义 AI 的身份、角色定位与核心职责，所有对话共享。</p>
              <el-input
                v-model="createContent.role_definition"
                type="textarea"
                :rows="7"
                placeholder="如：你是一个友好、专业的贷款智能客服，负责回答用户关于贷款产品、申请、还款等方面的问题。"
              />
            </div>

            <!-- L2 业务规则 -->
            <div class="layer-block">
              <div class="layer-block__title">
                <span class="layer-tag layer-tag--l2">L2</span>
                业务规则 · 行为约束
              </div>
              <p class="layer-block__desc">业务逻辑约束、工具使用策略、合规红线，每轮对话强制遵守。</p>
              <el-input
                v-model="createContent.business_rules"
                type="textarea"
                :rows="5"
                placeholder="如：贷款申请状态查询由系统 Workflow 自动处理；产品推荐使用 recommend_products 工具..."
              />
            </div>

            <!-- L4 风格规则 -->
            <div class="layer-block">
              <div class="layer-block__title">
                <span class="layer-tag layer-tag--l4">L4</span>
                风格规则 · 回复语气
              </div>
              <p class="layer-block__desc">回复的语气、措辞风格、格式化要求，影响输出观感。</p>
              <el-input
                v-model="createContent.tone_style"
                type="textarea"
                :rows="4"
                placeholder="如：语气友好、专业，简洁明了；分点陈述复杂内容；不使用绝对化表述。"
              />
            </div>
          </template>

          <!-- 意图档：本意图规则 + 少样本示例 + 输出附加要求 -->
          <template v-else>
            <el-alert
              class="intent-tip"
              type="info"
              :closable="false"
              show-icon
              title="意图档提示词只组装该意图的对话，基础身份/合规规则由 base 档统一提供"
            />

            <!-- 意图业务规则 -->
            <div class="layer-block">
              <div class="layer-block__title">
                <span class="layer-tag layer-tag--intent">意图</span>
                本意图业务规则
              </div>
              <p class="layer-block__desc">该意图特有的处理流程、工具调用策略与边界约束。</p>
              <el-input
                v-model="createContent.business_rules"
                type="textarea"
                :rows="5"
                placeholder="如：用户可能想查询自己的贷款申请进度，该查询由系统 Workflow 自动处理，Agent 不负责查询个人数据..."
              />
            </div>

            <!-- 少样本示例 -->
            <div class="layer-block">
              <div class="layer-block__title">
                <span class="layer-tag layer-tag--l2">L2</span>
                少样本示例（few_shots）
              </div>
              <p class="layer-block__desc">提供典型输入与期望处理方式，引导模型遵循相同模式。</p>
              <div class="few-shot-list">
                <div v-for="(shot, i) in createContent.few_shots" :key="i" class="few-shot-row">
                  <el-input v-model="shot.input" placeholder="用户示例消息" />
                  <el-input v-model="shot.expect" placeholder="期望的处理做法" />
                  <el-button text type="danger" @click="createContent.few_shots.splice(i, 1)">删除</el-button>
                </div>
                <el-button
                  text
                  type="primary"
                  @click="createContent.few_shots.push({ input: '', expect: '' })"
                >
                  + 添加示例
                </el-button>
              </div>
            </div>

            <!-- 输出附加要求 -->
            <div class="layer-block">
              <div class="layer-block__title">
                <span class="layer-tag layer-tag--l4">L4</span>
                输出附加要求（output_extra）
              </div>
              <p class="layer-block__desc">该意图回复的额外格式化要求、提示语或免责声明。</p>
              <el-input
                v-model="createContent.output_extra"
                type="textarea"
                :rows="3"
                placeholder="如：只展示与当前用户本人申请相关的信息，内部审核备注一律不展示。"
              />
            </div>
          </template>
        </el-form>
      </el-card>

      <!-- 右栏：提示词列表（按 category 分组 + 激活版本标识 + 语义色边） -->
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
            v-for="group in groupedPrompts"
            :key="group.category"
            class="prompt-group"
          >
            <div class="prompt-group__header">
              <span class="prompt-group__name">{{ categoryLabel(group.category) }}</span>
              <el-tag
                v-if="group.active"
                size="small"
                type="success"
                effect="dark"
                class="prompt-group__active-tag"
              >
                激活中 v{{ group.active.version }}
              </el-tag>
              <el-tag v-else size="small" type="info" effect="plain">无激活版本</el-tag>
            </div>

            <div
              v-for="row in group.items"
              :key="row.prompt_id"
              class="prompt-item"
              :class="{ 'is-active': row.is_active }"
            >
              <div class="prompt-item__head">
                <span class="prompt-item__name">{{ row.name }}</span>
                <el-switch
                  :model-value="row.is_active"
                  @change="toggleActive(row)"
                />
              </div>
              <div class="prompt-item__meta">
                <el-tag size="small" effect="plain" :type="row.is_active ? 'success' : 'info'">
                  v{{ row.version }}
                </el-tag>
                <span v-if="row.is_active" class="prompt-item__active-badge">当前生效</span>
                <span class="prompt-item__time">更新于 {{ row.updated_at || '—' }}</span>
              </div>
              <div class="prompt-item__actions">
                <el-button size="small" type="primary" text @click="openEditDialog(row)">编辑</el-button>
                <el-button
                  v-if="!row.is_active"
                  size="small"
                  type="success"
                  text
                  @click="toggleActive(row)"
                >
                  设为激活
                </el-button>
                <el-button
                  v-else
                  size="small"
                  type="warning"
                  text
                  @click="toggleActive(row)"
                >
                  停用
                </el-button>
                <el-button size="small" type="danger" text @click="deletePrompt(row)">删除</el-button>
              </div>
            </div>
          </div>
        </div>
      </el-card>
    </div>

    <el-dialog v-model="editDialogVisible" title="编辑提示词" width="700px">
      <el-form label-width="140px">
        <el-form-item label="提示词名称">
          <el-input v-model="editName" placeholder="输入提示词名称" />
        </el-form-item>
        <el-form-item label="提示词档位">
          <!-- category 创建后不可改（后端 PromptUpdate 无 category 字段），仅展示 -->
          <el-tag effect="plain">{{ categoryLabel(editCategory) }}</el-tag>
        </el-form-item>

        <!-- base/general（含旧数据 customer_service）档：三层架构 -->
        <template v-if="isEditBaseDoc">
          <div class="layer-block">
            <div class="layer-block__title">
              <span class="layer-tag layer-tag--l1">L1</span>
              系统规则 · 角色定义
            </div>
            <p class="layer-block__desc">定义 AI 的身份、角色定位与核心职责。</p>
            <el-input
              v-model="editContent.role_definition"
              type="textarea"
              :rows="6"
              placeholder="定义 AI 的身份和角色定位"
            />
          </div>
          <div class="layer-block">
            <div class="layer-block__title">
              <span class="layer-tag layer-tag--l2">L2</span>
              业务规则 · 行为约束
            </div>
            <p class="layer-block__desc">业务逻辑约束、工具使用策略、合规红线。</p>
            <el-input
              v-model="editContent.business_rules"
              type="textarea"
              :rows="4"
              placeholder="业务逻辑约束和行为规则"
            />
          </div>
          <div class="layer-block">
            <div class="layer-block__title">
              <span class="layer-tag layer-tag--l4">L4</span>
              风格规则 · 回复语气
            </div>
            <p class="layer-block__desc">回复的语气、措辞风格、格式化要求。</p>
            <el-input
              v-model="editContent.tone_style"
              type="textarea"
              :rows="3"
              placeholder="回复的语气、风格要求"
            />
          </div>
        </template>

        <!-- 意图档：三层 -->
        <template v-else>
          <div class="layer-block">
            <div class="layer-block__title">
              <span class="layer-tag layer-tag--intent">意图</span>
              本意图业务规则
            </div>
            <p class="layer-block__desc">该意图特有的处理流程、工具调用策略与边界约束。</p>
            <el-input
              v-model="editContent.business_rules"
              type="textarea"
              :rows="4"
              placeholder="该意图特有的处理规则"
            />
          </div>
          <div class="layer-block">
            <div class="layer-block__title">
              <span class="layer-tag layer-tag--l2">L2</span>
              少样本示例（few_shots）
            </div>
            <p class="layer-block__desc">提供典型输入与期望处理方式。</p>
            <div class="few-shot-list">
              <div v-for="(shot, i) in editContent.few_shots" :key="i" class="few-shot-row">
                <el-input v-model="shot.input" placeholder="用户示例消息" />
                <el-input v-model="shot.expect" placeholder="期望的处理做法" />
                <el-button text type="danger" @click="editContent.few_shots.splice(i, 1)">删除</el-button>
              </div>
              <el-button
                text
                type="primary"
                @click="editContent.few_shots.push({ input: '', expect: '' })"
              >
                + 添加示例
              </el-button>
            </div>
          </div>
          <div class="layer-block">
            <div class="layer-block__title">
              <span class="layer-tag layer-tag--l4">L4</span>
              输出附加要求
            </div>
            <p class="layer-block__desc">该意图回复的额外格式化要求。</p>
            <el-input
              v-model="editContent.output_extra"
              type="textarea"
              :rows="3"
              placeholder="该意图回复的额外要求"
            />
          </div>
        </template>
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
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import promptsAPI from '@/api/modules/prompts'
import ChatDialog from '@/components/chat/ChatDialog.vue'

const loading = ref(false)
const chatVisible = ref(false)
const promptsList = ref([])

// ====== 提示词档位（对齐后端 ALLOWED_CATEGORIES 白名单）======
// base：身份三件套；general：通用兜底；其余为意图档（规则+样例+输出要求）
const CATEGORY_OPTIONS = [
  { value: 'base', label: '基础档 base（身份/规则/风格）' },
  { value: 'general', label: '通用兜底 general' },
  { value: 'query_status', label: '意图：查申请进度' },
  { value: 'calculate', label: '意图：还款计算' },
  { value: 'list_products', label: '意图：产品列表' },
  { value: 'apply_loan', label: '意图：申请贷款' },
  { value: 'complaint', label: '意图：投诉处理' },
  { value: 'consult', label: '意图：咨询推荐' },
  { value: 'query_repayment', label: '意图：查还款计划' },
  { value: 'query_credit', label: '意图：查信誉分' }
]
// 走「身份三件套」表单的档位；customer_service 为历史数据，视同 base
const BASE_LIKE = ['base', 'general', 'customer_service']
const categoryLabel = (value) =>
  CATEGORY_OPTIONS.find((c) => c.value === value)?.label ||
  (value === 'customer_service' ? '旧版通用档' : value || '—')

// 工厂函数：create/edit 各持独立副本，避免浅拷贝共享 few_shots 数组引用
const defaultContent = () => ({
  role_definition: `你是一个友好的贷款智能客服。请根据以下规则回答用户问题：

1. 如果用户询问贷款申请状态，使用 query_application_status 工具查询
2. 如果用户询问还款计划/月还款额，使用 calculate_repayment 工具计算
3. 如果用户询问其他问题，优先从知识库中查找答案
4. 如果知识库没有相关信息，给出通用建议并引导用户提供更多信息`,
  business_rules: '',
  tone_style: `回答要求：
- 语气友好、专业
- 简洁明了
- 对于需要调用工具的问题，先调用工具再基于结果回答`,
  output_extra: '',
  few_shots: []
})

// 意图档 few_shots 清洗：去除空白行（后端 FewShot 每项 min_length=1，空值会 422）
const cleanFewShots = (shots) =>
  (shots || [])
    .map((s) => ({ input: (s.input || '').trim(), expect: (s.expect || '').trim() }))
    .filter((s) => s.input && s.expect)

// 创建用：空字段直接剔除，与后端 exclude_none 语义对齐
const buildContentForCreate = (content, isBaseDoc) => {
  const base = {
    role_definition: (content.role_definition || '').trim(),
    business_rules: (content.business_rules || '').trim(),
    tone_style: (content.tone_style || '').trim()
  }
  const intent = {
    business_rules: (content.business_rules || '').trim(),
    output_extra: (content.output_extra || '').trim(),
    few_shots: cleanFewShots(content.few_shots)
  }
  const raw = isBaseDoc ? base : intent
  return Object.fromEntries(
    Object.entries(raw).filter(
      ([, v]) => v !== '' && v != null && !(Array.isArray(v) && v.length === 0)
    )
  )
}

// 编辑用：显式传 '' 允许用户清空字段（update 语义下剔除字段等于不改）
const buildContentForUpdate = (content, isBaseDoc) => {
  if (isBaseDoc) {
    return {
      role_definition: content.role_definition || '',
      business_rules: content.business_rules || '',
      tone_style: content.tone_style || ''
    }
  }
  return {
    business_rules: content.business_rules || '',
    output_extra: content.output_extra || '',
    few_shots: cleanFewShots(content.few_shots)
  }
}

const createName = ref('')
const createCategory = ref('base')
const createContent = ref(defaultContent())
const isCreateBaseDoc = computed(() => BASE_LIKE.includes(createCategory.value))

const editDialogVisible = ref(false)
const editId = ref(null)
const editName = ref('')
const editCategory = ref('base')
const editContent = ref(defaultContent())
const isEditBaseDoc = computed(() => BASE_LIKE.includes(editCategory.value))

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
  // 轻校验：base 档至少要有角色定义，意图档至少要有业务规则
  if (isCreateBaseDoc.value && !createContent.value.role_definition.trim()) {
    ElMessage.warning('base 档请填写角色定义')
    return
  }
  if (!isCreateBaseDoc.value && !createContent.value.business_rules.trim()) {
    ElMessage.warning('意图档请填写本意图业务规则')
    return
  }

  try {
    await promptsAPI.create({
      name: createName.value,
      category: createCategory.value,
      content: buildContentForCreate(createContent.value, isCreateBaseDoc.value)
    })
    ElMessage.success('创建成功')
    resetCreateForm()
    await fetchPrompts()
  } catch {
    ElMessage.error('创建失败')
  }
}

const resetCreateForm = () => {
  createName.value = ''
  createCategory.value = 'base'
  createContent.value = defaultContent()
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
      // 档位创建后不可改，仅回显；历史 customer_service 数据视同 base 展示
      editCategory.value = res.data.category || 'customer_service'
      editContent.value = {
        role_definition: res.data.content?.role_definition || '',
        business_rules: res.data.content?.business_rules || '',
        tone_style: res.data.content?.tone_style || '',
        output_extra: res.data.content?.output_extra || '',
        few_shots: (res.data.content?.few_shots || []).map((s) => ({
          input: s.input || '',
          expect: s.expect || ''
        }))
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
    await promptsAPI.update(editId.value, {
      name: editName.value,
      content: buildContentForUpdate(editContent.value, isEditBaseDoc.value)
    })
    ElMessage.success('更新成功')
    editDialogVisible.value = false
    await fetchPrompts()
  } catch {
    ElMessage.error('保存失败')
  }
}

// ====== 切换激活状态（走后端专用接口，立即清空组装缓存） ======
const toggleActive = async (prompt) => {
  try {
    if (prompt.is_active) {
      await promptsAPI.deactivate(prompt.prompt_id)
      ElMessage.success('已停用')
    } else {
      await promptsAPI.activate(prompt.prompt_id)
      ElMessage.success('已激活，同档位旧版本已自动停用')
    }
    await fetchPrompts()
  } catch {
    ElMessage.error('操作失败')
  }
}

// ====== 按 category 分组，展示激活版本 ======
const groupedPrompts = computed(() => {
  const map = new Map()
  for (const p of promptsList.value) {
    const cat = p.category || 'general'
    if (!map.has(cat)) {
      map.set(cat, { category: cat, items: [], active: null })
    }
    const group = map.get(cat)
    group.items.push(p)
    if (p.is_active) group.active = p
  }
  // 版本降序排列，激活的排最前
  for (const g of map.values()) {
    g.items.sort((a, b) => {
      if (a.is_active !== b.is_active) return a.is_active ? -1 : 1
      return parseFloat(b.version || 0) - parseFloat(a.version || 0)
    })
  }
  // 按 CATEGORY_OPTIONS 顺序输出，没有定义的 category 排最后
  const orderMap = new Map(CATEGORY_OPTIONS.map((c, i) => [c.value, i]))
  return [...map.values()].sort((a, b) => {
    const ai = orderMap.has(a.category) ? orderMap.get(a.category) : 999
    const bi = orderMap.has(b.category) ? orderMap.get(b.category) : 999
    return ai - bi
  })
})

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
  align-items: stretch;
  gap: 16px;

  max-height: 550px;
}

.editor-card,
.list-card {
  display: flex;
  flex-direction: column;
}

/* el-card body 填充剩余高度，让内部列表/表单可滚动 */
.editor-card :deep(.el-card__body),
.list-card :deep(.el-card__body) {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.editor-card {
  flex: 0 0 65%;
  position: sticky;
  top: 84px;
}

.list-card {
  flex: 1 1 35%;
  min-width: 0;
  position: sticky;
  top: 84px;
}

/* ===== 意图档表单：说明条 + few_shots 行编辑器 ===== */
.intent-tip {
  margin-bottom: 16px;
}

.few-shot-list {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.few-shot-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.few-shot-row .el-input {
  flex: 1;
  min-width: 0;
}

/* ===== 左栏：代码编辑器感（深色灰底 + 等宽字体 + 内阴影） ===== */
.prompt-editor {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding-right: 4px;
}

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

/* ===== 分层区块（系统规则 / 业务规则 / 风格规则） ===== */
.layer-block {
  margin-bottom: 18px;
  padding: 14px 16px;
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid var(--border-color-light, #ebeef5);
  border-radius: 10px;
  border-left: 3px solid var(--brand-500, #3b82f6);
}

.layer-block__title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13.5px;
  font-weight: 600;
  color: var(--text-color, #303133);
  margin-bottom: 4px;
}

.layer-block__desc {
  font-size: 12px;
  color: var(--text-secondary, #909399);
  margin: 0 0 10px 0;
  line-height: 1.5;
}

.layer-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 26px;
  height: 20px;
  padding: 0 6px;
  border-radius: 5px;
  font-size: 11px;
  font-weight: 700;
  color: #fff;
  line-height: 1;
}

.layer-tag--l1 { background: var(--brand-purple, #2d1b69); }
.layer-tag--l2 { background: var(--brand-500, #3b82f6); }
.layer-tag--l4 { background: var(--brand-accent, #25e0bf); color: #0f172a; }
.layer-tag--intent { background: #f59e0b; }

/* ===== 列表分组 ===== */
.prompt-group {
  margin-bottom: 22px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.prompt-group__header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  margin-bottom: 10px;
  background: var(--bg-color-light, #f5f7fa);
  border-radius: 6px;
}

.prompt-group__name {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-color, #303133);
  margin-right: auto;
}

.prompt-group__active-tag {
  font-weight: 600;
}

.prompt-item__active-badge {
  font-size: 11px;
  color: var(--color-success, #67c23a);
  font-weight: 600;
}

.prompt-card-list {
  display: flex;
  flex-direction: column;
  gap: 0;
  /* 填充 el-card body 剩余高度，超出时内部滚动 */
  flex: 1;
  min-height: 0;
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

  .prompt-card-list,
  .prompt-editor {
    flex: none;
    overflow: visible;
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
