<template>
  <div class="mcp-tools">
    <h2>MCP 工具管理</h2>

    <!-- 工具列表 -->
    <el-card class="tools-card">
      <template #header>
        <div class="card-header">
          <span>已注册的 MCP 工具</span>
          <el-button type="primary" @click="refreshTools">刷新</el-button>
        </div>
      </template>
      
      <el-table :data="toolsList" stripe v-loading="loading">
        <el-table-column prop="name" label="工具名称" width="250">
          <template #default="{ row }">
            <el-tag type="primary">{{ row.name }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.isEnabled ? 'success' : 'info'">
              {{ row.isEnabled ? '已启用' : '已禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="callCount" label="调用次数" width="100" sortable class="use-time" />
        <el-table-column prop="successRate" label="成功率" width="100" class="cell">
          <template #default="{ row }">
            <el-progress 
              :percentage="row.successRate" 
              :color="getProgressColor(row.successRate)"
              :stroke-width="8"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-switch 
              v-model="row.isEnabled" 
              @change="toggleTool(row)"
              style="margin-right: 10px"
            />
            <el-button type="primary" size="small" @click="configureTool(row)">配置</el-button>
            <el-button type="success" size="small" @click="testTool(row)">测试</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 工具调用日志 -->
    <el-card class="logs-card">
      <template #header>
        <div class="card-header">
          <span>工具调用日志</span>
          <el-form :inline="true" style="margin-bottom: 0">
            <el-form-item label="工具名称">
              <el-select v-model="logFilter.toolName" placeholder="全部" clearable @change="fetchToolLogs">
                <el-option 
                  v-for="tool in toolsList" 
                  :key="tool.name" 
                  :label="tool.name" 
                  :value="tool.name" 
                />
              </el-select>
            </el-form-item>
            <el-form-item label="结果">
              <el-select v-model="logFilter.result" placeholder="全部" clearable @change="fetchToolLogs">
                <el-option label="成功" value="success" />
                <el-option label="失败" value="error" />
              </el-select>
            </el-form-item>
            <el-button type="primary" @click="fetchToolLogs">搜索</el-button>
          </el-form>
        </div>
      </template>
      
      <el-table :data="toolLogs" stripe v-loading="logsLoading" max-height="400">
        <el-table-column prop="timestamp" label="时间" width="180" sortable />
        <el-table-column prop="tool_name" label="工具名称" width="220">
          <template #default="{ row }">
            <el-tag size="small">{{ row.tool_name }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="input_params" label="输入参数" width="200" show-overflow-tooltip>
          <template #default="{ row }">
            {{ JSON.stringify(row.input_params) }}
          </template>
        </el-table-column>
        <el-table-column prop="output" label="输出结果" show-overflow-tooltip />
        <el-table-column prop="duration" label="耗时" width="80">
          <template #default="{ row }">
            {{ row.duration }}ms
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 'success' ? 'success' : 'danger'" size="small">
              {{ row.status === 'success' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 工具配置对话框 -->
    <el-dialog v-model="configDialogVisible" title="工具配置" width="600px">
      <el-form :model="currentTool" label-width="120px">
        <el-form-item label="工具名称">
          <el-input v-model="currentTool.name" disabled />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="currentTool.description" type="textarea" :rows="2" />
        </el-form-item>
        
        <el-divider>Java API 配置</el-divider>
        
        <el-form-item label="API 地址">
          <el-input v-model="currentTool.config.javaApiUrl" placeholder="http://localhost:8080/api" />
        </el-form-item>
        
        <el-form-item label="认证方式">
          <el-select v-model="currentTool.config.authType">
            <el-option label="无认证" value="none" />
            <el-option label="API Key" value="apiKey" />
            <el-option label="Bearer Token" value="bearer" />
          </el-select>
        </el-form-item>
        
        <el-form-item v-if="currentTool.config.authType === 'apiKey'" label="API Key">
          <el-input v-model="currentTool.config.apiKey" type="password" show-password />
        </el-form-item>
        
        <el-form-item v-if="currentTool.config.authType === 'bearer'" label="Token">
          <el-input v-model="currentTool.config.token" type="password" show-password />
        </el-form-item>
        
        <el-form-item label="超时时间（秒）">
          <el-input-number v-model="currentTool.config.timeout" :min="5" :max="300" :step="5" />
        </el-form-item>
        
        <el-form-item label="重试次数">
          <el-input-number v-model="currentTool.config.retryCount" :min="0" :max="5" />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="configDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveToolConfig">保存配置</el-button>
      </template>
    </el-dialog>

    <!-- 工具测试对话框 -->
    <el-dialog v-model="testDialogVisible" title="工具测试" width="600px">
      <el-form :model="testParams" label-width="100px">
        <el-form-item :label="param" v-for="param in currentTestTool?.params" :key="param">
          <el-input v-model="testParams[param]" :placeholder="`请输入 ${param}`" />
        </el-form-item>
      </el-form>
      
      <div v-if="testResult" class="test-result">
        <el-divider>测试结果</el-divider>
        <el-alert :type="testResult.success ? 'success' : 'error'" :title="testResult.success ? '调用成功' : '调用失败'">
          {{ testResult.output }}
        </el-alert>
      </div>
      
      <template #footer>
        <el-button @click="testDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="executeTest">执行测试</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage} from 'element-plus'
import axios from 'axios'

const API_BASE = 'http://localhost:8000/tools'

const loading = ref(false)
const logsLoading = ref(false)
const configDialogVisible = ref(false)
const testDialogVisible = ref(false)

const toolsList = ref([
  {
    name: 'query_application_status',
    description: '查询用户的贷款申请状态',
    isEnabled: true,
    callCount: 156,
    successRate: 95,
    params: ['user_name', 'phone'],
    config: {
      javaApiUrl: 'http://localhost:8080/api',
      authType: 'none',
      timeout: 30,
      retryCount: 3
    }
  },
  {
    name: 'calculate_repayment',
    description: '计算还款计划',
    isEnabled: true,
    callCount: 89,
    successRate: 98,
    params: ['amount', 'rate', 'months'],
    config: {
      javaApiUrl: 'http://localhost:8080/api',
      authType: 'none',
      timeout: 30,
      retryCount: 3
    }
  }
])

const toolLogs = ref([])
const logFilter = ref({
  toolName: '',
  result: ''
})

const currentTool = ref({
  name: '',
  description: '',
  config: {
    javaApiUrl: '',
    authType: 'none',
    apiKey: '',
    token: '',
    timeout: 30,
    retryCount: 3
  }
})

const currentTestTool = ref(null)
const testParams = ref({})
const testResult = ref(null)

// const fetchTools = async () => {
//   loading.value = true
//   try {
//     const res = await axios.get(API_BASE)
//     if (res.data && res.data.length > 0) {
//       toolsList.value = res.data
//     }
//   } catch (error) {
//     console.error('Error fetching tools:', error)
//   } finally {
//     loading.value = false
//   }
// }

const toggleTool = async (tool) => {
  try {
    await axios.post(`${API_BASE}/${tool.name}/toggle`, { enabled: tool.isEnabled })
    ElMessage.success(tool.isEnabled ? '工具已启用' : '工具已禁用')
  } catch (error) {
    tool.isEnabled = !tool.isEnabled
    ElMessage.error('操作失败'+error.message)
  }
}

const configureTool = (tool) => {
  currentTool.value = JSON.parse(JSON.stringify(tool))
  configDialogVisible.value = true
}

const saveToolConfig = async () => {
  try {
    await axios.post(`${API_BASE}/${currentTool.value.name}/config`, currentTool.value.config)
    ElMessage.success('配置保存成功')
    configDialogVisible.value = false
    // fetchTools()
  } catch (error) {
    ElMessage.error('配置保存失败'+error.message)
  }
}

const testTool = (tool) => {
  currentTestTool.value = tool
  testParams.value = {}
  testResult.value = null
  testDialogVisible.value = true
}

const executeTest = async () => {
  if (!testParams.value) {
    ElMessage.warning('请输入测试参数')
    return
  }
  
  try {
    const res = await axios.post(`${API_BASE}/${currentTestTool.value.name}/test`, testParams.value)
    testResult.value = {
      success: res.data.success,
      output: res.data.output || res.data.error
    }
  } catch (error) {
    testResult.value = {
      success: false,
      output: error.response?.data?.message || error.message
    }
  }
}

// const fetchToolLogs = async () => {
//   logsLoading.value = true
//   try {
//     const params = new URLSearchParams()
//     if (logFilter.value.toolName) params.append('tool', logFilter.value.toolName)
//     if (logFilter.value.result) params.append('status', logFilter.value.result)
    
//     // const res = await axios.get(`${API_BASE}/logs?${params}`)
//     // toolLogs.value = res.data || []
//   } catch (error) {
//     console.error('Error fetching tool logs:', error)
//   } finally {
//     logsLoading.value = false
//   }
// }

const refreshTools = () => {
  // fetchTools()
  // fetchToolLogs()
}

const getProgressColor = (percentage) => {
  if (percentage >= 90) return '#67c23a'
  if (percentage >= 70) return '#e6a23c'
  return '#f56c6c'
}

onMounted(() => {
  // fetchTools()
  // fetchToolLogs()
})
</script>

<style scoped>
.mcp-tools {
  padding: 20px;
}
.tools-card {
  margin-bottom: 20px;
}
.logs-card {
  margin-bottom: 20px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.test-result {
  margin-top: 20px;
}

:deep(.el-table .cell) {
  padding: 0px 10px;
}
</style>
