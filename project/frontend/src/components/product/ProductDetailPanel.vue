<template>
  <div class="product-detail-panel">
    <!-- 面板头部 -->
    <div class="panel-header">
      <h3>{{ isEditMode ? '编辑产品' : '产品详情' }}</h3>
      <button class="btn-close-panel" @click="handleClose">
        <i class="el-icon-close"></i>
      </button>
    </div>

    <div class="panel-content">
      <!-- 基本信息 -->
      <div v-if="!isEditMode" class="prod-base-info">
        <div class="info-row">
          <span class="label">产品名称：</span>
          <span class="value">{{ productData.productName || '—' }}</span>
        </div>
        <div class="info-row">
          <span class="label">产品描述：</span>
          <span class="value">{{ productData.description || '—' }}</span>
        </div>
        <div class="info-row">
          <span class="label">产品用途：</span>
          <span class="value">{{ productData.loanUsage || '—' }}</span>
        </div>
        <div class="info-row">
          <span class="label">期限范围：</span>
          <span class="value">{{ productData.minTerm || 0 }} - {{ productData.maxTerm || 0 }} 个月</span>
        </div>
        <div class="info-row">
          <span class="label">金额范围：</span>
          <span class="value">{{ productData.minAmount || 0 }} - {{ productData.maxAmount || 0 }} 元</span>
        </div>
        <div class="info-row">
          <span class="label">促销信息：</span>
          <span class="value">{{ productData.promotionDetails || '—' }}</span>
        </div>
        <div class="info-row">
          <span class="label">状态：</span>
          <span class="value">{{ productData.status || '—' }}</span>
        </div>
        <div class="info-row">
          <span class="label">创建时间：</span>
          <span class="value">{{ formatDate(productData.createTime) }}</span>
        </div>
        <div class="info-row">
          <span class="label">更新时间：</span>
          <span class="value">{{ formatDate(productData.updateTime) }}</span>
        </div>
      </div>

      <!-- 编辑表单 -->
      <el-form
        v-else
        ref="editFormRef"
        :model="editForm"
        label-width="100px"
        :rules="editRules"
      >
        <el-form-item label="产品名称" prop="productName">
          <el-input v-model="editForm.productName" />
        </el-form-item>
        <el-form-item label="产品描述" prop="description">
          <el-input v-model="editForm.description" type="textarea" rows="3" />
        </el-form-item>
        <el-form-item label="产品用途" prop="loanUsage">
          <el-input v-model="editForm.loanUsage" />
        </el-form-item>
        <el-form-item label="最短期限" prop="minTerm">
          <el-input-number v-model="editForm.minTerm" :min="0" />
        </el-form-item>
        <el-form-item label="最长期限" prop="maxTerm">
          <el-input-number v-model="editForm.maxTerm" :min="editForm.minTerm" />
        </el-form-item>
        <el-form-item label="期限步长" prop="termStep">
          <el-input-number v-model="editForm.termStep" :min="1" />
        </el-form-item>
        <el-form-item label="最小金额" prop="minAmount">
          <el-input-number v-model="editForm.minAmount" :min="0" :step="1000" />
        </el-form-item>
        <el-form-item label="最大金额" prop="maxAmount">
          <el-input-number v-model="editForm.maxAmount" :min="editForm.minAmount" :step="1000" />
        </el-form-item>
        <el-form-item label="促销信息">
          <el-input v-model="editForm.promotionDetails" type="textarea" rows="2" />
        </el-form-item>
      </el-form>

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
    </div>

    <!-- 操作按钮 -->
    <div class="panel-footer">
      <button v-if="!isEditMode" class="btn-edit" @click="enterEditMode">
        编辑
      </button>
      <button v-if="isEditMode" class="btn-cancel" @click="cancelEdit">
        取消
      </button>
      <button v-if="isEditMode" class="btn-save" @click="saveChanges">
        保存修改
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
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
  termStep: [{ required: true, message: '请输入期限步长', trigger: 'blur' }]
}

// 加载产品详情 - 使用正确的API函数
const loadProductDetail = async () => {
  try {
    const response = await loanAPI.getProduct(props.productId)
    if (response.code === 200) {
      productData.value = response.data || {}
      editForm.value = { ...productData.value }
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

// 工具函数
const formatDate = (dateString) => {
  if (!dateString) return '—'
  return new Date(dateString).toLocaleString('zh-CN')
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
.product-detail-panel {
  border-radius: 8px;
  background: var(--detail-bg);
  color: var(--detail-color);
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid var(--detail-border);
  background-color: var(--detail-head-bg);
  border-radius: 8px 8px 0 0;
}

.panel-header h3 {
  margin: 0;
  font-size: 18px;
  color: var(--detail-color);
  font-weight: 600;
}

.btn-close-panel {
  background: none;
  border: none;
  font-size: 24px;
  color: #666;
  cursor: pointer;
  padding: 0;
  width: 30px;
  height: 30px;
  line-height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.btn-close-panel:hover {
  color: #333;
  background-color: #e9ecef;
  border-radius: 4px;
}

.panel-content {
  padding: 20px;
  overflow-y: auto;
}

.panel-footer {
  padding: 16px 20px;
  border-top: 1px solid var(--detail-border);
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  background-color: var(--detail-bg);
  border-radius: 0 0 8px 8px;
}

.btn-edit,
.btn-cancel,
.btn-save {
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}

.btn-edit {
  background-color: #409EFF;
  color: white;
}

.btn-edit:hover {
  background-color: #66b1ff;
}

.btn-cancel {
  background-color: #909399;
  color: white;
}

.btn-cancel:hover {
  background-color: #a6a9ad;
}

.btn-save {
  background-color: #67C23A;
  color: white;
}

.btn-save:hover {
  background-color: #85ce61;
}

/* 原有样式保持不变 */
.prod-base-info {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.info-row {
  display: flex;
  line-height: 1.6;
}

.label {
  font-weight: 600;
  min-width: 100px;
  color: var(--detail-subtitle-color);
}

.value {
  flex: 1;
  color: var(--detail-color);
}

.options-section {
  margin-top: 24px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--detail-border);
}

.section-header h4 {
  margin: 0;
  font-size: 16px;
  color: var(--detail-subtitle-color);
  font-weight: 600;
}

.btn-add-option {
  padding: 5px 10px;
  background-color: #409EFF;
  color: white;
  border: none;
  border-radius: 3px;
  cursor: pointer;
  font-size: 13px;
}

.btn-add-option:hover {
  background-color: #66b1ff;
}

.options-table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 10px;
}

.options-table th {
  background: #f8f9fa;
  padding: 12px;
  text-align: center;
  border-bottom: 2px solid var(--detail-border);
  font-weight: 600;
  font-size: 14px;
  color: var(--detail-color);
}

.options-table td {
  padding: 10px 12px;
  border-bottom: 1px solid #eee;
  font-size: 14px;
  text-align: center;
  color: var(--detail-color);
}

.btn-delete {
  padding: 4px 8px;
  background-color: #F56C6C;
  color: white;
  border: none;
  border-radius: 3px;
  cursor: pointer;
  font-size: 13px;
}

.btn-delete:hover {
  background-color: #f78989;
}
</style>