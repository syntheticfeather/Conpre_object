<template>
  <div class="product-detail">
    <!-- 顶部导航栏 -->
    <div class="top-nav">
      <div class="nav-left">
        <el-icon class="back-icon" @click="handleClose"><ArrowLeft /></el-icon>
        <div class="nav-title-group">
          <div class="title-row">
            <h1 class="product-name">{{ productData.productName || '—' }}</h1>
            <span class="category-tag">{{ productData.loanUsage || '个人贷款' }}</span>
            <span class="status-badge" :class="productData.status === '上架中' ? 'online' : 'offline'">
              <span class="status-dot"></span>
              {{ productData.status === '上架中' ? '上线中' : '已下线' }}
            </span>
          </div>
          <span class="product-meta">产品ID: {{ productData.productId || '—' }} | 创建时间: {{ formatDate(productData.createTime) }}</span>
        </div>
      </div>
      <div class="nav-right">
        <el-button size="default" @click="enterEditMode">
          <el-icon><Edit /></el-icon>
          编辑
        </el-button>
        <el-button 
          v-if="productData.status === '上架中'"
          size="default" 
          type="danger" 
          plain
          @click="toggleStatus"
        >
          <el-icon><CircleClose /></el-icon>
          下架产品
        </el-button>
        <el-button 
          v-else
          size="default" 
          type="success" 
          plain
          @click="toggleStatus"
        >
          <el-icon><Check /></el-icon>
          上架产品
        </el-button>
      </div>
    </div>

    <!-- 产品概览卡片 -->
    <div class="hero-card">
      <div class="hero-left">
        <span class="hero-category">{{ productData.loanUsage || '个人贷款' }}</span>
        <h2 class="hero-title">{{ productData.productName || '—' }}</h2>
        <p class="hero-desc">{{ (productData.description || '—') +' | '+ (productData.promotionDetails || '') }}</p>
      </div>
      <div class="hero-right">
        <span class="hero-rate">{{ getAvgRate() }}%</span>
        <span class="hero-rate-label">平均年化利率</span>
      </div>
    </div>

    <!-- 产品关键信息卡片 -->
    <div class="stats-grid">
      <div class="stat-card blue">
        <div class="stat-icon">
          <el-icon><Money /></el-icon>
        </div>
        <div class="stat-value">¥{{ formatAmount(productData.minAmount) }} - ¥{{ formatAmount(productData.maxAmount) }}</div>
        <div class="stat-label">贷款额度范围</div>
        <div class="stat-sub">最低 ¥{{ productData.minAmount || 0 }}</div>
      </div>
      <div class="stat-card purple">
        <div class="stat-icon">
          <el-icon><Clock /></el-icon>
        </div>
        <div class="stat-value">{{ getTermRange() }}</div>
        <div class="stat-label">贷款期限范围</div>
        <div class="stat-sub">可灵活选择</div>
      </div>
      
      <div class="stat-card green">
        <div class="stat-icon">
          <el-icon><List /></el-icon>
        </div>
        <div class="stat-value">{{ options.length || 0 }} 个</div>
        <div class="stat-label">可选方案数量</div>
        <div class="stat-sub">利率: {{ getMinRate() }}% ~ {{ getMaxRate() }}%</div>
      </div>
      <div class="stat-card orange">
        <div class="stat-icon">
          <el-icon><TrendCharts /></el-icon>
        </div>
        <div class="stat-value">{{ getAvgRate() }}%</div>
        <div class="stat-label">平均年化利率</div>
        <div class="stat-sub">最低 {{ getMinRate() }}%</div>
      </div>
      <div class="stat-card red">
        <div class="stat-icon">
          <el-icon><Calendar /></el-icon>
        </div>
        <div class="stat-value">{{ getOnlineDays() }} 天</div>
        <div class="stat-label">产品上线天数</div>
        <div class="stat-sub">上线于 {{ formatDate(productData.createTime) }}</div>
      </div>
    </div>

    <!-- 可选方案表格 -->
    <div class="options-section">
      <div class="section-header">
        <h4>贷款可选方案</h4>
        <button v-if="isEditMode" class="btn-add-option" @click="addOptionRow">
          + 增加方案
        </button>
      </div>
      
      <table class="options-table">
        <thead>
          <tr>
            <th>期限(月)</th>
            <th>利率</th>
            <th>还款方式</th>
            <th v-if="isEditMode">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(option, index) in options" :key="index">
            <td v-if="isEditMode">
              <el-input-number
                v-model="option.loanPeriod"
                :min="1"
              />
            </td>
            <td v-else>{{ option.loanPeriod }}</td>
            
            <td v-if="isEditMode">
              <el-input-number
                v-model="option.interestRate"
                :min="0"
                :step="0.0001"
                :precision="4"
              />
            </td>
            <td v-else>{{ (option.interestRate * 100).toFixed(2) }}%</td>
            
            <td v-if="isEditMode">
              <el-select v-model="option.repaidType">
                <el-option value="等额本息" />
                <el-option value="等额本金" />
                <el-option value="先息后本" />
                <el-option value="一次性还本付息" />
              </el-select>
            </td>
            <td v-else>{{ option.repaidType }}</td>
            
            <td v-if="isEditMode">
              <button class="btn-delete" @click="removeOption(index)">
                删除
              </button>
            </td>
          </tr>
          <tr v-if="options.length === 0">
            <td :colspan="isEditMode ? 4 : 3" style="text-align: center;">
              暂无可选方案
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 编辑表单（编辑模式下显示） -->
    <el-form
      v-if="isEditMode"
      ref="editFormRef"
      :model="editForm"
      label-width="100px"
      :rules="editRules"
      class="edit-form"
    >
      <el-row :gutter="20">
        <!-- 左侧 2/3 区域 -->
        <el-col :span="16">
          <el-form-item label="产品名称" prop="productName">
            <el-input v-model="editForm.productName" />
          </el-form-item>
          <el-form-item label="产品描述" prop="description">
            <el-input v-model="editForm.description" type="textarea" :rows="3" />
          </el-form-item>
          <el-form-item label="产品用途" prop="loanUsage">
            <el-input v-model="editForm.loanUsage" />
          </el-form-item>
          <el-form-item label="促销信息">
            <el-input v-model="editForm.promotionDetails" type="textarea" :rows="2" />
          </el-form-item>
        </el-col>

        <!-- 右侧 1/3 区域 -->
        <el-col :span="8">
          <el-form-item label="最短期限" prop="minTerm">
            <el-input-number v-model="editForm.minTerm" :min="0" @change="onTermRangeChange" style="width: 100%" />
          </el-form-item>
          <el-form-item label="最长期限" prop="maxTerm">
            <el-input-number v-model="editForm.maxTerm" :min="editForm.minTerm" @change="onTermRangeChange" style="width: 100%" />
          </el-form-item>
          <el-form-item label="期限步长" prop="termStep">
            <el-select v-model="editForm.termStep" placeholder="请选择期限步长" :disabled="!canSelectStep" style="width: 100%">
              <el-option
                v-for="step in validSteps"
                :key="step"
                :label="`${step} 个月`"
                :value="step"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="最小金额" prop="minAmount">
            <el-input-number v-model="editForm.minAmount" :min="0" :step="1000" style="width: 100%" />
          </el-form-item>
          <el-form-item label="最大金额" prop="maxAmount">
            <el-input-number v-model="editForm.maxAmount" :min="editForm.minAmount" :step="1000" style="width: 100%" />
          </el-form-item>
        </el-col>
      </el-row>

      <div class="edit-actions">
        <el-button @click="cancelEdit">取消</el-button>
        <el-button type="primary" @click="saveChanges">保存修改</el-button>
      </div>
    </el-form>
  </div>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Edit, CircleClose, Check, Money, Clock, TrendCharts, List, Calendar } from '@element-plus/icons-vue'
import { loanAPI } from '@/api'

const props = defineProps({
  productId: {
    type: [String, Number],
    required: true
  }
})

const emit = defineEmits(['close', 'saved'])

// 产品数据
const productData = ref({})
const editForm = ref({})
const originalOptions = ref([])
const options = ref([])
const isEditMode = ref(false)

// 表单验证规则
const editRules = {
  productName: [{ required: true, message: '请输入产品名称', trigger: 'blur' }],
  description: [{ required: true, message: '请输入产品描述', trigger: 'blur' }],
  loanUsage: [{ required: true, message: '请输入产品用途', trigger: 'blur' }],
  minTerm: [{ required: true, message: '请输入最短期限', trigger: 'blur' }],
  maxTerm: [{ required: true, message: '请输入最长期限', trigger: 'blur' }],
  termStep: [{ required: true, message: '请选择期限步长', trigger: 'change' }]
}

// 计算期限范围
const termRange = computed(() => {
  const min = editForm.value.minTerm || 0
  const max = editForm.value.maxTerm || 12
  return max - min
})

// 计算所有可能的步长因子
const validSteps = computed(() => {
  const range = termRange.value
  if (range <= 0) return []
  
  const steps = []
  for (let i = 1; i <= range; i++) {
    if (range % i === 0) {
      steps.push(i)
    }
  }
  return steps
})

// 是否可以选择步长
const canSelectStep = computed(() => {
  return termRange.value > 0
})

// 期限范围变化时的处理
const onTermRangeChange = () => {
  const range = termRange.value
  if (range <= 0) {
    editForm.value.termStep = null
    return
  }
  
  // 如果当前步长不在有效列表中，重置为第一个有效值
  if (!validSteps.value.includes(editForm.value.termStep)) {
    editForm.value.termStep = validSteps.value[0]
  }
}

// 加载产品详情 - 使用正确的API函数
const loadProductDetail = async () => {
  try {
    const response = await loanAPI.getProduct(props.productId)
    if (response.code === 200) {
      productData.value = response.data || {}
      editForm.value = { ...productData.value }
      
      // 设置默认值
      if (!editForm.value.minTerm && editForm.value.minTerm !== 0) {
        editForm.value.minTerm = 0
      }
      if (!editForm.value.maxTerm) {
        editForm.value.maxTerm = 12
      }
      
      originalOptions.value = productData.value.options || []
      options.value = [...originalOptions.value]
    } else {
      ElMessage.error('加载产品详情失败: ' + (response.message || '未知错误'))
    }
  } catch (error) {
    ElMessage.error('加载产品详情失败: ' + (error.message || '请重试'))
  }
}

// 进入编辑模式
const enterEditMode = () => {
  isEditMode.value = true
}

// 取消编辑
const cancelEdit = () => {
  isEditMode.value = false
  editForm.value = { ...productData.value }
  options.value = [...originalOptions.value]
}

// 添加方案行
const addOptionRow = () => {
  options.value.push({
    loanPeriod: 12,
    interestRate: 0.05,
    repaidType: '等额本息'
  })
}

// 删除方案
const removeOption = (index) => {
  options.value.splice(index, 1)
}

// 保存修改 - 使用正确的API函数
const saveChanges = async () => {
  try {
    // 验证必填字段
    if (!editForm.value.productName?.trim()) {
      ElMessage.warning('请输入产品名称')
      return
    }

    // 收集要创建和删除的选项
    const toCreate = options.value.filter(opt => !opt.optionId)
    const toDeleteIds = originalOptions.value
      .filter(origOpt => !options.value.some(opt => opt.optionId === origOpt.optionId))
      .map(opt => opt.optionId)

    // 执行批量操作
    if (toDeleteIds.length > 0) {
      if (toDeleteIds.length === 1) {
        await loanAPI.deleteOption(toDeleteIds[0])
      } else {
        await loanAPI.batchDeleteOptions({ optionIds: toDeleteIds })
      }
    }

    if (toCreate.length > 0) {
      // 修正batchCreateOptions调用
      await loanAPI.batchCreateOptions({
        productId: props.productId,
        options: toCreate
      })
    }

    // 确保 editForm.value.options 只包含已存在的选项（有 optionId 的选项）
    editForm.value.options = options.value.filter(opt => opt.optionId)
    
    // 更新产品基本信息 - 使用正确的API函数
    await loanAPI.updateProduct(props.productId, editForm.value)

    ElMessage.success('更新成功')
    isEditMode.value = false
    emit('saved')
    loadProductDetail() // 重新加载最新数据
  } catch (error) {
    ElMessage.error('保存失败：' + (error.message || '请重试'))
  }
}

// 关闭面板
const handleClose = () => {
  if (isEditMode.value) {
    ElMessageBox.confirm('有未保存的修改，确定要关闭吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => {
      isEditMode.value = false
      emit('close')
    })
  } else {
    emit('close')
  }
}

// 上架/下架
const toggleStatus = async () => {
  const action = productData.value.status === '上架中' ? 'deactive' : 'active'
  const actionText = action === 'active' ? '上架' : '下架'
  
  try {
    await ElMessageBox.confirm(
      `确定要${actionText}产品【${productData.value.productName}】吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await loanAPI.toggleStatus(props.productId, action)
    ElMessage.success(`${actionText}成功`)
    loadProductDetail()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

// 工具函数
const formatDate = (dateString) => {
  if (!dateString) return '—'
  return new Date(dateString).toLocaleDateString('zh-CN')
}

const formatAmount = (amount) => {
  if (!amount) return '0'
  const num = Number(amount)
  if (num >= 100000000) return (num / 100000000).toFixed(2)
  if (num >= 10000) return (num / 10000).toFixed(1)
  return num.toLocaleString()
}

const getAvgRate = () => {
  if (!options.value || options.value.length === 0) return '0'
  const avg = options.value.reduce((sum, opt) => sum + (opt.interestRate || 0), 0) / options.value.length
  return (avg * 100).toFixed(1)
}

const getTermRange = () => {
  const terms = productData.value.terms
  if (!terms || terms.length === 0) {
    return `${productData.value.minTerm || 0}~${productData.value.maxTerm || 12}个月`
  }
  return `${terms[0]}~${terms[terms.length - 1]}个月`
}

const getMinRate = () => {
  if (!options.value || options.value.length === 0) return '0'
  const min = Math.min(...options.value.map(opt => opt.interestRate || 0))
  return (min * 100).toFixed(2)
}

const getMaxRate = () => {
  if (!options.value || options.value.length === 0) return '0'
  const max = Math.max(...options.value.map(opt => opt.interestRate || 0))
  return (max * 100).toFixed(2)
}

const getOnlineDays = () => {
  if (!productData.value.createTime) return '0'
  const createTime = new Date(productData.value.createTime)
  const now = new Date()
  const diffTime = now.getTime() - createTime.getTime()
  const diffDays = Math.floor(diffTime / (1000 * 60 * 60 * 24))
  return diffDays > 0 ? diffDays : 0
}

// 监听产品ID变化
watch(() => props.productId, (newVal) => {
  if (newVal) {
    loadProductDetail()
    isEditMode.value = false
  }
}, { immediate: true })
</script>

<style scoped>
.product-detail {
  display: flex;
  flex-direction: column;
  padding: 24px;
  background: #f0f2f5;
  min-height: 100vh;
}

.top-nav {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
  border-radius: 12px;
  margin-bottom: 16px;
}

.nav-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.back-icon {
  font-size: 20px;
  cursor: pointer;
  color: #606266;
  transition: color 0.2s;
}

.back-icon:hover {
  color: #409eff;
}

.nav-title-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.product-name {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.category-tag {
  font-size: 13px;
  color: #409eff;
  background: #ecf5ff;
  padding: 2px 10px;
  border-radius: 12px;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  padding: 2px 10px;
  border-radius: 12px;
}

.status-badge.online {
  color: #67c23a;
  background: #f0f9eb;
}

.status-badge.offline {
  color: #909399;
  background: #f4f4f5;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

.product-meta {
  font-size: 13px;
  color: #909399;
}

.nav-right {
  display: flex;
  gap: 12px;
  align-items: center;
}

.hero-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px 40px;
  background: linear-gradient(
344deg, #2c53cf 0%, #5c1daa85 100%);
  border-radius: 16px;
  margin-bottom: 20px;
  color: #fff;
}

.hero-left {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.hero-category {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
}

.hero-title {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
  color: #fff;
}

.hero-desc {
  margin: 0;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.7);
  max-width: 500px;
}

.hero-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
}

.hero-rate {
  font-size: 42px;
  font-weight: 700;
  color: #fff;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.hero-rate-label {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.7);
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background: #fff;
  border-radius: 16px;
  padding: 24px 20px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.stat-icon {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  margin-bottom: 4px;
}

.stat-card.blue .stat-icon {
  background: #ecf5ff;
  color: #409eff;
}

.stat-card.purple .stat-icon {
  background: #f3e8ff;
  color: #9c27b0;
}

.stat-card.green .stat-icon {
  background: #e8f5e9;
  color: #4caf50;
}

.stat-card.orange .stat-icon {
  background: #fff3e0;
  color: #ff9800;
}

.stat-card.red .stat-icon {
  background: #fce4ec;
  color: #f44336;
}

.stat-value {
  font-size: 22px;
  font-weight: 700;
  color: #303133;
}

.stat-card.blue .stat-value { color: #409eff; }
.stat-card.purple .stat-value { color: #9c27b0; }
.stat-card.green .stat-value { color: #4caf50; }
.stat-card.orange .stat-value { color: #ff9800; }
.stat-card.red .stat-value { color: #f44336; }

.stat-label {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

.stat-sub {
  font-size: 12px;
  color: #909399;
}

.options-section {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-header h4 {
  margin: 0;
  font-size: 16px;
  color: #303133;
  font-weight: 600;
}

.btn-add-option {
  padding: 6px 16px;
  background-color: #409EFF;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
}

.btn-add-option:hover {
  background-color: #66b1ff;
}

.options-table {
  width: 100%;
  border-collapse: collapse;
}

.options-table th {
  background: #f5f7fa;
  padding: 12px;
  text-align: center;
  border-bottom: 2px solid #ebeef5;
  font-weight: 600;
  font-size: 14px;
  color: #606266;
}

.options-table td {
  padding: 12px;
  border-bottom: 1px solid #ebeef5;
  font-size: 14px;
  text-align: center;
  color: #303133;
}

.btn-delete {
  padding: 4px 12px;
  background-color: #f56c6c;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
}

.btn-delete:hover {
  background-color: #f78989;
}

.edit-form {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  margin-top: 20px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.edit-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 16px;
}
</style>