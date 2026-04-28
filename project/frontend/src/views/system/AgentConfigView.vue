<template>
  <div class="agent-config">
    <h2>Agent 配置管理</h2>

    <!-- <el-row :gutter="20"> -->
      <!-- 基础配置 -->
      <!-- <el-col :span="12">
        <el-card class="config-card">
          <template #header>
            <div class="card-header">
              <span>LLM 模型配置</span>
              <el-button type="primary" @click="saveConfig">保存配置</el-button>
            </div>
          </template>
          
          <el-form :model="config.llm" label-width="120px">
            <el-form-item label="模型选择">
              <el-select v-model="config.llm.model" placeholder="选择模型">
                <el-option label="MiniMax-M2.7" value="MiniMax-M2.7" />
                <el-option label="MiniMax-ABAB6.5" value="MiniMax-ABAB6.5" />
                <el-option label="GPT-4" value="gpt-4" />
                <el-option label="GPT-3.5-turbo" value="gpt-3.5-turbo" />
              </el-select>
            </el-form-item>
            
            <el-form-item label="API Key">
              <el-input 
                v-model="config.llm.apiKey" 
                type="password" 
                placeholder="请输入 API Key"
                show-password
              />
            </el-form-item>
            
            <el-form-item label="API Base URL">
              <el-input v-model="config.llm.baseUrl" placeholder="https://api.minimaxi.com/anthropic" />
            </el-form-item>
            
            <el-form-item label="温度参数">
              <el-slider 
                v-model="config.llm.temperature" 
                :min="0" 
                :max="1" 
                :step="0.1"
                show-input
              />
              <div class="form-tip">控制随机性：0=确定，1=随机</div>
            </el-form-item>
            
            <el-form-item label="最大 Token 数">
              <el-input-number 
                v-model="config.llm.maxTokens" 
                :min="100" 
                :max="4096" 
                :step="100"
              />
            </el-form-item>
          </el-form>
        </el-card>
      </el-col> -->

      <!-- 知识库检索配置 -->
      <!-- <el-col :span="12">
        <el-card class="config-card">
          <template #header>
            <div class="card-header">
              <span>知识库检索配置</span>
            </div>
          </template>
          
          <el-form :model="config.retrieval" label-width="120px">
            <el-form-item label="检索数量">
              <el-input-number 
                v-model="config.retrieval.topK" 
                :min="1" 
                :max="10" 
                :step="1"
              />
              <div class="form-tip">每次检索返回的相似问题数量</div>
            </el-form-item>
            
            <el-form-item label="相似度阈值">
              <el-slider 
                v-model="config.retrieval.threshold" 
                :min="0" 
                :max="1" 
                :step="0.05"
                show-input
              />
              <div class="form-tip">低于此阈值的检索结果将被忽略</div>
            </el-form-item>
            
            <el-form-item label="启用知识库">
              <el-switch v-model="config.retrieval.enabled" />
            </el-form-item>
          </el-form>
        </el-card>
      </el-col> -->
    <!-- </el-row> -->

    <!-- System Prompt 配置 -->
    <el-card class="config-card">
      <template #header>
        <div class="card-header">
          <span>System Prompt 配置</span>
          <el-button type="success" @click="resetPrompt">恢复默认</el-button>
        </div>
      </template>
      
      <el-form :model="config" label-width="120px">
        <el-form-item label="系统提示词">
          <el-input
            v-model="config.systemPrompt"
            type="textarea"
            :rows="10"
            placeholder="输入 System Prompt"
          />
          <div class="form-tip">
            定义 Agent 的角色、行为和回答规则。使用 {'{'}{'}'} 占位符可插入动态内容。
          </div>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 会话配置 -->
    <el-card class="config-card">
      <template #header>
        <div class="card-header">
          <span>会话管理配置</span>
        </div>
      </template>
      
      <el-form :model="config.session" label-width="140px" inline>
        <el-form-item label="会话超时时间（分钟）">
          <el-input-number v-model="config.session.timeout" :min="5" :max="1440" :step="5" />
        </el-form-item>
        
        <el-form-item label="历史消息保留数">
          <el-input-number v-model="config.session.maxHistory" :min="5" :max="100" :step="5" />
        </el-form-item>
        
        <el-form-item label="存储方式">
          <el-select v-model="config.session.storage">
            <el-option label="内存存储" value="memory" />
            <el-option label="Redis" value="redis" />
            <el-option label="数据库" value="database" />
          </el-select>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 配置历史 -->
    <el-card class="config-card">
      <template #header>
        <div class="card-header">
          <span>配置历史记录</span>
          <el-button type="warning" @click="loadHistory">刷新历史</el-button>
        </div>
      </template>
      
      <el-table :data="configHistory" stripe>
        <el-table-column prop="version" label="版本" width="100" />
        <el-table-column prop="updated_at" label="更新时间" width="180" sortable />
        <el-table-column prop="updated_by" label="操作人" width="120" />
        <el-table-column prop="changes" label="变更内容" show-overflow-tooltip />
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="rollback(row)">回滚</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>

  <button class="chat-fab" @click="chatVisible = true" title="智能对话调试">💬</button>
  <ChatDialog v-if="chatVisible" @close="chatVisible = false" />
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'
import ChatDialog from '@/components/chat/ChatDialog.vue'

const API_BASE = 'http://localhost:8000/config'

const config = ref({
  llm: {
    model: 'MiniMax-M2.7',
    apiKey: '',
    baseUrl: 'https://api.minimaxi.com/anthropic',
    temperature: 0.7,
    maxTokens: 2048
  },
  retrieval: {
    topK: 3,
    threshold: 0.5,
    enabled: true
  },
  systemPrompt: `你是一个友好的贷款智能客服。请根据以下规则回答用户问题：

1. 如果用户询问贷款申请状态，使用 query_application_status 工具查询
2. 如果用户询问还款计划/月还款额，使用 calculate_repayment 工具计算
3. 如果用户询问其他问题，优先从知识库中查找答案
4. 如果知识库没有相关信息，给出通用建议并引导用户提供更多信息

回答要求：
- 语气友好、专业
- 简洁明了
- 对于需要调用工具的问题，先调用工具再基于结果回答`,
  tools: [
    {
      name: 'query_application_status',
      description: '查询用户的贷款申请状态',
      enabled: true,
      config: {}
    },
    {
      name: 'calculate_repayment',
      description: '计算还款计划',
      enabled: true,
      config: {}
    }
  ],
  session: {
    timeout: 30,
    maxHistory: 20,
    storage: 'memory'
  }
})

const configHistory = ref([])
const chatVisible = ref(false)

const fetchConfig = async () => {
  try {
    const res = await axios.get(API_BASE)
    if (res.data) {
      config.value = { ...config.value, ...res.data }
    }
  } catch (error) {
    console.error('Error fetching config:', error)
  }
}

const saveConfig = async () => {
  try {
    await axios.post(API_BASE, config.value)
    ElMessage.success('配置保存成功')
    fetchHistory()
  } catch (error) {
    ElMessage.error('配置保存失败'+error)
  }
}

const resetPrompt = () => {
  config.value.systemPrompt = `你是一个友好的贷款智能客服。请根据以下规则回答用户问题：

1. 如果用户询问贷款申请状态，使用 query_application_status 工具查询
2. 如果用户询问还款计划/月还款额，使用 calculate_repayment 工具计算
3. 如果用户询问其他问题，优先从知识库中查找答案
4. 如果知识库没有相关信息，给出通用建议并引导用户提供更多信息

回答要求：
- 语气友好、专业
- 简洁明了
- 对于需要调用工具的问题，先调用工具再基于结果回答`
  ElMessage.success('已恢复默认 System Prompt')
}

const editToolConfig = (tool) => {
  ElMessageBox.prompt('请输入工具配置（JSON 格式）', `配置 ${tool.name}`, {
    inputValue: JSON.stringify(tool.config, null, 2),
    inputType: 'textarea'
  }).then(({ value }) => {
    try {
      tool.config = JSON.parse(value)
      ElMessage.success('配置更新成功')
    } catch (error) {
      ElMessage.error('JSON 格式错误'+error)
    }
  })
}

const testTool = async (tool) => {
  ElMessage.info(`测试工具 ${tool.name} 功能开发中...`)
}

const fetchHistory = async () => {
  try {
    const res = await axios.get(`${API_BASE}/history`)
    configHistory.value = res.data || []
  } catch (error) {
    console.error('Error fetching history:', error)
  }
}

const loadHistory = () => {
  fetchHistory()
}

const rollback = async (version) => {
  try {
    await ElMessageBox.confirm('确定要回滚到此版本吗？', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await axios.post(`${API_BASE}/rollback`, { version: version.version })
    ElMessage.success('回滚成功')
    fetchConfig()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('回滚失败')
    }
  }
}

onMounted(() => {
  fetchConfig()
  fetchHistory()
})
</script>

<style scoped>
.agent-config {
  padding: 20px;
}
.config-card {
  margin-bottom: 20px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
}

.chat-fab {
  position: fixed;
  right: 32px;
  bottom: 32px;
  width: 52px;
  height: 52px;
  border: none;
  border-radius: 50%;
  background: linear-gradient(135deg, #1a73e8, #1557b0);
  color: #fff;
  font-size: 22px;
  cursor: pointer;
  box-shadow: 0 4px 16px rgba(26, 115, 232, 0.4);
  z-index: 9998;
  transition: transform 0.2s, box-shadow 0.2s;
}

.chat-fab:hover {
  transform: scale(1.08);
  box-shadow: 0 6px 24px rgba(26, 115, 232, 0.5);
}
</style>
