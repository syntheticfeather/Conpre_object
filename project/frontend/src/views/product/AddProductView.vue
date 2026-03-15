<template>
  <div class="add-product-view">
    <div class="header">
      <h1>新增贷款产品</h1>
      <button class="btn-back" @click="goBack">
        返回产品列表
      </button>
    </div>

    <div class="form-container">
      <!-- 基础信息表单 -->
      <div class="form-section">
        <h3>基础信息</h3>
        <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
          <el-form-item label="产品名称" prop="productName">
            <el-input 
              v-model="form.productName" 
              placeholder="请输入产品名称" 
              style="width: 100%; max-width: 400px;"
            />
          </el-form-item>
          
          <el-form-item label="产品描述" prop="description">
            <el-input
              v-model="form.description"
              type="textarea"
              :rows="3"
              placeholder="请输入产品描述"
              style="width: 100%; max-width: 400px;"
            />
          </el-form-item>
          
          <el-form-item label="贷款用途" prop="loanUsage">
            <el-input 
              v-model="form.loanUsage" 
              placeholder="请输入贷款用途" 
              style="width: 100%; max-width: 400px;"
            />
          </el-form-item>
          
          <el-row :gutter="20" style="max-width: 500px;">
            <el-col :span="8">
              <el-form-item label="最短期限" prop="minTerm">
                <el-input-number
                  v-model="form.minTerm"
                  :min="0"
                  controls-position="right"
                  style="width: 100%;"
                  placeholder="最短期限"
                />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="最长期限" prop="maxTerm">
                <el-input-number
                  v-model="form.maxTerm"
                  :min="form.minTerm"
                  controls-position="right"
                  style="width: 100%;"
                  placeholder="最长期限"
                />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="期限步长" prop="termStep">
                <el-input-number
                  v-model="form.termStep"
                  :min="1"
                  controls-position="right"
                  style="width: 100%;"
                  placeholder="期限步长"
                />
              </el-form-item>
            </el-col>
          </el-row>
          
          <el-row :gutter="20" style="max-width: 500px;">
            <el-col :span="12">
              <el-form-item label="最小金额" prop="minAmount">
                <el-input-number
                  v-model="form.minAmount"
                  :min="0"
                  :step="1000"
                  controls-position="right"
                  style="width: 100%;"
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
                  style="width: 100%;"
                  placeholder="最大金额"
                />
              </el-form-item>
            </el-col>
          </el-row>
          
          <el-form-item label="促销信息">
            <el-input
              v-model="form.promotionDetails"
              type="textarea"
              :rows="2"
              placeholder="请输入促销描述"
              style="width: 100%; max-width: 400px;"
            />
          </el-form-item>
        </el-form>
      </div>

      <!-- 可选方案表格 -->
      <div class="form-section">
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
                    style="width: 120px;"
                    placeholder="利率"
                  />
                  <span style="margin-left: 4px;">%</span>
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
                    <span style="color: #999; display: block; margin-bottom: 10px;">
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

      <!-- 表单操作按钮 -->
      <div class="form-actions">
        <button class="btn-cancel" @click="handleCancel">
          取消
        </button>
        <button class="btn-confirm" @click="handleSubmit">
          确认添加
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useLoanStore } from '@/stores/loan'

const router = useRouter()
const loanStore = useLoanStore()
const formRef = ref()

// 表单数据
const form = reactive({
  productName: '',
  description: '',
  loanUsage: '',
  minTerm: 3,
  maxTerm: 24,
  termStep: 3,
  minAmount: 10000,
  maxAmount: 50000,
  promotionDetails: '',
  options: []
})

// 初始化时添加一个默认方案
onMounted(() => {
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
    { required: true, message: '请输入期限步长', trigger: 'blur' }
  ]
}

// 返回产品列表
const goBack = () => {
  router.push('/dashboard/products')
}

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
  
  // 验证期限范围
  if (form.maxTerm < form.minTerm) {
    ElMessage.warning('最长期限不能小于最短期限')
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