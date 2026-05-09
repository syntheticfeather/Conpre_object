<template>
  <div class="add-product-view">
    <div class="header">
      <h1>新增贷款产品</h1>
      <button class="btn-back btn" @click="goBack">
        返回产品列表
      </button>
    </div>

    <!-- 步骤指示器 -->
    <div class="step-indicator">
      <div class="step-item" :class="{ active: currentStep === 1 }">
        <div class="step-number">1</div>
        <div class="step-label">基本信息</div>
      </div>
      <div class="step-line"></div>
      <div class="step-item" :class="{ active: currentStep === 2 }">
        <div class="step-number">2</div>
        <div class="step-label">详细设置</div>
      </div>
    </div>

    <!-- 基础信息表单 -->
    <div class="form-container">
      <!-- 统一表单容器 -->
      <el-form ref="formRef" :model="form" :rules="rules">
        <!-- 第一步：基本信息 -->
        <div v-if="currentStep === 1" class="step-1">
          <el-form-item label="产品名称" prop="productName">
            <el-input 
              v-model="form.productName" 
              placeholder="请输入产品名称" 
            />
          </el-form-item>
          
          <el-form-item label="产品描述" prop="description">
            <el-input
              v-model="form.description"
              type="textarea"
              :rows="3"
              placeholder="请输入产品描述"
            />
          </el-form-item>
          
          <el-form-item label="贷款用途" prop="loanUsage">
            <el-input 
              v-model="form.loanUsage" 
              placeholder="请输入贷款用途"
            />
          </el-form-item>
          
          <el-form-item label="促销信息" prop="promotionDetails">
            <el-input
              v-model="form.promotionDetails"
              type="textarea"
              :rows="2"
              placeholder="请输入促销描述"
            />
          </el-form-item>
        </div>
        
        <!-- 第二步：详细设置 -->
        <div v-if="currentStep === 2" class="step-2">
          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="最短期限" prop="minTerm">
                <el-input-number
                  v-model="form.minTerm"
                  :min="1"
                  :max="form.maxTerm"
                  controls-position="right"
                  placeholder="最短期限"
                  @change="updateTermStepOptions"
                />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="最长期限" prop="maxTerm">
                <el-input-number
                  v-model="form.maxTerm"
                  :min="form.minTerm"
                  controls-position="right"
                  placeholder="最长期限"
                  @change="updateTermStepOptions"
                />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="期限步长" prop="termStep">
                <el-select
                  v-model="form.termStep"
                  placeholder="请选择步长"
                  @change="validateTermStep"
                >
                  <el-option
                    v-for="step in termStepOptions"
                    :key="step"
                    :label="step + '个月'"
                    :value="step"
                  />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="最小金额" prop="minAmount">
                <el-input-number
                  v-model="form.minAmount"
                  :min="0"
                  :step="1000"
                  controls-position="right"
                  placeholder="最小金额"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="最大金额" prop="maxAmount">
                <el-input-number
                  v-model="form.maxAmount"
                  :min="form.minAmount"
                  :step="1000"
                  controls-position="right"
                  placeholder="最大金额"
                />
              </el-form-item>
            </el-col>
          </el-row>
          
          <!-- 可选方案表格 -->
          <div class="options-section">
            <div class="section-header">
              <h3>贷款可选方案</h3>
              <button type="button" class="btn-add" @click="addOptionRow">
                <span style="font-size: 18px; margin-right: 4px;">+</span> 增加方案
              </button>
            </div>
            
            <div class="table-container">
              <table class="options-table">
                <thead>
                  <tr>
                    <th>期限(月)</th>
                    <th>利率(%)</th>
                    <th>还款方式</th>
                    <th>操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(option, index) in form.options" :key="index">
                    <td>
                      <el-input-number
                        v-model="option.loanPeriod"
                        :min="1"
                        controls-position="right"
                        style="width: 100px;"
                        placeholder="期限"
                      />
                    </td>
                    <td>
                      <el-input-number
                        v-model="option.interestRate"
                        :min="0"
                        :step="0.1"
                        :precision="2"
                        controls-position="right"
                        style="width: 140px;"
                        placeholder="利率"
                      >
                        <template #suffix>%</template>
                      </el-input-number>
                    </td>
                    <td>
                      <el-select 
                        v-model="option.repaidType" 
                        placeholder="选择还款方式"
                        style="width: 140px;"
                      >
                        <el-option label="等额本息" value="等额本息" />
                        <el-option label="等额本金" value="等额本金" />
                        <el-option label="先息后本" value="先息后本" />
                        <el-option label="一次性还本付息" value="一次性还本付息" />
                      </el-select>
                    </td>
                    <td>
                      <button 
                        type="button" 
                        class="btn-delete" 
                        :disabled="form.options.length === 1"
                        @click="removeOption(index)"
                      >
                        删除
                      </button>
                    </td>
                  </tr>
                  <tr v-if="form.options.length === 0">
                    <td colspan="4" class="empty-table">
                      <div>
                        <span style="color: var(--text-color-caption); display: block; margin-bottom: 10px;">
                          暂无方案，请点击"增加方案"按钮添加
                        </span>
                        <button type="button" class="btn-add-empty" @click="addOptionRow">
                          <span style="font-size: 18px; margin-right: 4px;">+</span> 添加第一个方案
                        </button>
                      </div>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
            
            <div class="form-tips">
              <p>💡 提示：至少需要添加一个完整的贷款方案才能提交</p>
            </div>
          </div>
        </div>

      </el-form>
    </div>

    <!-- 表单操作按钮 -->
    <div class="form-actions-container">
      <!-- 第一步按钮 -->
      <div v-if="currentStep === 1" class="form-actions">
        <button class="btn-cancel btn" @click="handleCancel">
          取消
        </button>
        <button class="btn-next btn" @click="goToStep2">
          下一步
        </button>
      </div>
      <!-- 第二步按钮 -->
      <div v-if="currentStep === 2" class="form-actions">
        <button class="btn-prev btn" @click="goToStep1">
          上一步
        </button>
        <button class="btn-cancel btn" @click="handleCancel">
          取消
        </button>
        <button class="btn-confirm btn" @click="handleSubmit">
          确认添加
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useLoanStore } from '@/stores/loan'

const router = useRouter()
const loanStore = useLoanStore()
const formRef = ref()
const currentStep = ref(1) // 当前步骤，1为基本信息，2为详细设置

// 步长选项
const termStepOptions = ref([])

// 表单数据
const form = reactive({
  productName: '',
  description: '',
  loanUsage: '',
  minTerm: 1,
  maxTerm: 12,
  termStep: 1,
  minAmount: 10000,
  maxAmount: 50000,
  promotionDetails: '',
  options: []
})

// 计算并更新步长选项
const updateTermStepOptions = () => {
  const min = form.minTerm || 1
  const max = form.maxTerm || 12
  
  // 确保 max >= min
  if (max < min) {
    form.maxTerm = min
  }
  
  // 计算差值
  const diff = form.maxTerm - form.minTerm
  
  // 找出所有可能的步长值（能整除差值的数）
  const options = []
  for (let i = 1; i <= diff; i++) {
    if (diff % i === 0) {
      options.push(i)
    }
  }
  
  // 如果差值为 0，只提供步长 1
  if (diff === 0) {
    termStepOptions.value = [1]
    form.termStep = 1
  } else {
    termStepOptions.value = options
    
    // 如果当前步长不在选项中，选择第一个（最小的）
    if (!options.includes(form.termStep)) {
      form.termStep = options[0]
    }
  }
}

// 验证步长
const validateTermStep = () => {
  const diff = form.maxTerm - form.minTerm
  if (diff % form.termStep !== 0) {
    ElMessage.warning('步长必须能整除期限差值，已自动调整为最接近的有效值')
    form.termStep = termStepOptions.value[0]
  }
}

// 初始化时添加一个默认方案
onMounted(() => {
  // 初始化步长选项
  updateTermStepOptions()
  addOptionRow()
})

// 表单验证规则
const rules = {
  productName: [
    { required: true, message: '请输入产品名称', trigger: 'blur' }
  ],
  description: [
    { required: true, message: '请输入产品描述', trigger: 'blur' }
  ],
  loanUsage: [
    { required: true, message: '请输入贷款用途', trigger: 'blur' }
  ],
  minTerm: [
    { required: true, message: '请输入最短期限', trigger: 'blur' }
  ],
  maxTerm: [
    { required: true, message: '请输入最长期限', trigger: 'blur' }
  ],
  termStep: [
    { required: true, message: '请选择期限步长', trigger: 'change' }
  ]
}

// 返回产品列表
const goBack = () => {
  router.push('/dashboard/products')
}

// 步骤导航
const goToStep1 = () => {
  currentStep.value = 1
}

const goToStep2 = () => {
  // 验证第一步的必填字段
  if (!form.productName.trim()) {
    ElMessage.warning('请输入产品名称')
    return
  }
  if (!form.description.trim()) {
    ElMessage.warning('请输入产品描述')
    return
  }
  if (!form.loanUsage.trim()) {
    ElMessage.warning('请输入贷款用途')
    return
  }
  currentStep.value = 2
}

// 监听期限变化，自动更新步长选项
watch(() => [form.minTerm, form.maxTerm], () => {
  updateTermStepOptions()
}, { deep: true })

// 检查是否有未保存的内容
const hasUnsavedChanges = () => {
  return form.productName.trim() || 
         form.description.trim() || 
         form.loanUsage.trim() || 
         form.options.some(opt => 
           opt.loanAmount || opt.loanPeriod || opt.interestRate
         )
}

// 取消操作
const handleCancel = () => {
  if (hasUnsavedChanges()) {
    ElMessageBox.confirm('有未保存的内容，确定要离开吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => {
      goBack()
    }).catch(() => {
      // 用户取消操作
    })
  } else {
    goBack()
  }
}

// 添加方案行
const addOptionRow = () => {
  form.options.push({
    loanPeriod: 12,
    interestRate: 5.0,
    repaidType: '等额本息'
  })
}

// 删除方案
const removeOption = (index) => {
  if (form.options.length <= 1) {
    ElMessage.warning('至少需要保留一个贷款方案')
    return
  }
  
  form.options.splice(index, 1)
}

// 验证表单
const validateForm = () => {
  // 验证基础信息
  if (!form.productName.trim()) {
    ElMessage.warning('请输入产品名称')
    return false
  }
  
  if (!form.description.trim()) {
    ElMessage.warning('请输入产品描述')
    return false
  }
  
  if (!form.loanUsage.trim()) {
    ElMessage.warning('请输入贷款用途')
    return false
  }
  
  // 验证期限和步长
  if (!form.minTerm || form.minTerm <= 0) {
    ElMessage.warning('最短期限必须大于 0')
    return false
  }
  
  if (!form.maxTerm || form.maxTerm <= 0) {
    ElMessage.warning('最长期限必须大于 0')
    return false
  }
  
  if (!form.termStep || form.termStep <= 0) {
    ElMessage.warning('期限步长必须大于 0')
    return false
  }
  
  // 验证期限范围
  if (form.maxTerm < form.minTerm) {
    ElMessage.warning('最长期限不能小于最短期限')
    return false
  }
  
  // 验证期数和步长的等差关系
  if ((form.maxTerm - form.minTerm) % form.termStep !== 0) {
    ElMessage.warning('最长期限与最短期限的差值必须是步长的整数倍')
    return false
  }
  
  // 验证至少一个方案
  if (form.options.length === 0) {
    ElMessage.warning('至少需要添加一个贷款方案')
    return false
  }
  
  // 验证方案完整性
  for (let i = 0; i < form.options.length; i++) {
    const option = form.options[i]
    
    if (!option.loanPeriod || option.loanPeriod <= 0) {
      ElMessage.warning(`第 ${i + 1} 个方案：请输入有效的期限`)
      return false
    }
    
    if (!option.interestRate || option.interestRate <= 0) {
      ElMessage.warning(`第 ${i + 1} 个方案：请输入有效的利率`)
      return false
    }
    
    if (!option.repaidType) {
      ElMessage.warning(`第 ${i + 1} 个方案：请选择还款方式`)
      return false
    }
  }
  
  return true
}

// 提交表单
const handleSubmit = async () => {
  if (!validateForm()) {
    return
  }
  
  try {
    const productData = {
      productName: form.productName.trim(),
      description: form.description.trim(),
      loanUsage: form.loanUsage.trim(),
      minTerm: Number(form.minTerm),
      maxTerm: Number(form.maxTerm),
      termStep: Number(form.termStep),
      minAmount: Number(form.minAmount),
      maxAmount: Number(form.maxAmount),
      promotionDetails: form.promotionDetails.trim(),
      options: form.options.map(opt => ({
        loanPeriod: Number(opt.loanPeriod),
        interestRate: Number(opt.interestRate) / 100, // 转换为小数
        repaidType: opt.repaidType
      }))
    }
    
    // 显示加载状态
    // const loading = ElMessage.loading('正在提交中...', 0)
    
    // 调用 store 添加产品
    const result = await loanStore.addProduct(productData)
    
    // 关闭加载提示
    // loading.close()
    
    if (result.success) {
      ElMessage.success('产品添加成功')
      router.push('/dashboard/add-pro')
    } else {
      ElMessage.error(result.message || '添加失败')
    }
  } catch (error) {
    ElMessage.error('提交失败：' + (error.message || '请检查网络连接'))
  }
}
</script>

<style scoped>
@import '@/assets/css/addProduct.css';
</style>