<template>
  <div class="product-table-container">
    <!-- 搜索区域 -->
    <div class="product-search">
      <div class="input-group">
        <label>创建时间：</label>
        <el-date-picker
          v-model="createDateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
        />
      </div>
      <div class="input-group">
        <label>更新时间：</label>
        <el-date-picker
          v-model="updateDateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
        />
      </div>
      <button class="btn" @click="searchProducts">搜索</button>
      <button class="btn" @click="resetSearch">重置</button>

      <!-- 搜索状态提示 -->
      <span v-if="hasSearchCriteria" class="search-status">
        当前显示筛选结果 ({{ loanStore.products.length }} 条)
      </span>
    </div>

    <!-- 产品表格 -->
    <div class="product-content table-content">
      <table class="product-table data-table">
        <thead>
          <tr>
            <th>序号</th>
            <th>贷款名称</th>
            <th>贷款描述</th>
            <th>贷款用途</th>
            <th>产品状态</th>
            <th>更新时间</th>
            <th>创建时间</th>
            <th>快捷操作</th>
          </tr>
        </thead>
        <tbody>
          <tr 
            v-for="(product, index) in paginatedProducts" 
            :key="product.productId"
            :class="{ 'selected-row': selectedProductId === product.productId }"
            @click="selectProduct(product.productId)"
          >
            <td>{{ (currentPage - 1) * pageSize + index + 1 }}</td>
            <td>{{ product.productName || '—' }}</td>
            <td :title="product.description" class="ellipsis">{{ product.description || '—' }}</td>
            <td>{{ product.loanUsage || '—' }}</td>
            <td>{{ product.status || '—' }}</td>
            <td>{{ formatDate(product.updateTime) }}</td>
            <td>{{ formatDate(product.createTime) }}</td>
            <td>
              <button
                v-if="product.status === '上架中'"
                class="toggle-status-btn"
                @click.stop="toggleProductStatus(product, 'deactive')"
              >
                下架
              </button>
              <button
                v-else
                class="toggle-status-btn"
                @click.stop="toggleProductStatus(product, 'active')"
              >
                上架
              </button>
              <button class="delete-prod-btn" @click.stop="deleteProduct(product)">
                删除
              </button>
            </td>
          </tr>
          <tr v-if="loanStore.products.length === 0">
            <td colspan="8" style="text-align: center;">
              {{ hasSearchCriteria ? '未找到符合条件的商品' : '暂无产品' }}
            </td>
          </tr>
        </tbody>
      </table>

      <!-- 分页 -->
      <div class="pagination">
        <button 
          :disabled="currentPage <= 1" 
          @click="currentPage--"
          class="page-btn"
        >
          上一页
        </button>
        <span>第 {{ currentPage }} 页，共 {{ totalPages }} 页</span>
        <button 
          :disabled="currentPage >= totalPages" 
          @click="currentPage++"
          class="page-btn"
        >
          下一页
        </button>
      </div>
    </div>

    <!-- 产品详情展示区域 -->
    <div v-if="selectedProductId" class="product-detail-section">
      <ProductDetailPanel
        :product-id="selectedProductId"
        @close="clearSelection"
        @saved="handleProductSaved"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useLoanStore } from '@/stores/loan'
import { ElMessage, ElMessageBox } from 'element-plus'
import { loanAPI } from '@/api'
import ProductDetailPanel from './ProductDetailPanel.vue'

const loanStore = useLoanStore()

// 搜索条件
const createDateRange = ref([])
const updateDateRange = ref([])
// 计算是否有搜索条件
const hasSearchCriteria = computed(() => {
  return createDateRange.value.length > 0 || updateDateRange.value.length > 0
})

// 分页
const currentPage = ref(1)
const pageSize = 5
const totalPages = computed(() => Math.ceil(loanStore.products.length / pageSize))
const paginatedProducts = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return loanStore.products.slice(start, start + pageSize)
})

// 选中状态
const selectedProductId = ref(null)

// 加载产品列表
onMounted(async () => {
  await loanStore.fetchProducts()
})

// 根据日期搜索产品
const searchProducts = async () => {
  const params = {}
  
  // 构建搜索参数
  if (createDateRange.value?.length === 2) {
    params.createStartDate = createDateRange.value[0]
    params.createEndDate = createDateRange.value[1]
  }
  
  if (updateDateRange.value?.length === 2) {
    params.updateStartDate = updateDateRange.value[0]
    params.updateEndDate = updateDateRange.value[1]
  }
  
  // 如果没有选择任何日期范围，提示用户
  if (!params.createStartDate && !params.updateStartDate) {
    ElMessage.warning('请选择至少一个日期范围进行搜索')
    return
  }
  
  try {
    // 直接调用 API 获取数据
    const response = await loanAPI.searchProductsByTime(params)
    
    if (response.data?.code === 200 && response.data?.data) {
      loanStore.products = response.data.data
      
      // 显示搜索结果信息
      if (loanStore.products.length > 0) {
        ElMessage.success(`找到 ${loanStore.products.length} 个符合条件的商品`)
      } else {
        ElMessage.info('未找到符合条件的商品')
      }
    } else {
      ElMessage.warning('返回数据格式不正确')
      loanStore.products = [] // 清空列表
    }
    
    currentPage.value = 1
  } catch (error) {
    console.error('搜索产品失败:', error)
    ElMessage.error('搜索失败')
    loanStore.products = [] // 清空列表
  }
}

// 重置搜索
const resetSearch = async () => {
  createDateRange.value = []
  updateDateRange.value = []
  currentPage.value = 1
  
  try {
    await loanStore.fetchProducts()
    ElMessage.success('已重置所有筛选条件')
  } catch (error) {
    console.error('重置搜索失败:', error)
    ElMessage.error('重置失败')
  }
}

// 选择产品
const selectProduct = (productId) => {
  if (selectedProductId.value === productId) {
    // 如果点击已选中的行，则取消选择
    selectedProductId.value = null
  } else {
    selectedProductId.value = productId
  }
}

// 清除选择
const clearSelection = () => {
  selectedProductId.value = null
}

// 产品保存后的处理
const handleProductSaved = () => {
  loanStore.fetchProducts()
  selectedProductId.value = null
}

// 切换产品状态
const toggleProductStatus = async (product, action, event) => {
  event.stopPropagation()
  try {
    await ElMessageBox.confirm(
      `确定要${action === 'active' ? '上架' : '下架'}产品【${product.productName}】吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await loanStore.toggleProductStatus(product.productId, action)
    ElMessage.success(`${action === 'active' ? '上架' : '下架'}成功`)
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

// 删除产品
const deleteProduct = async (product, event) => {
  event.stopPropagation()
  try {
    await ElMessageBox.confirm(
      `确定删除产品【${product.productName}】？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    // 检查是否有待审申请
    const hasPending = await checkPendingApplications(product.productId)
    if (hasPending) {
      ElMessage.warning('该产品存在待审核申请，无法删除！')
      return
    }
    
    await loanStore.deleteProduct(product.productId)
    ElMessage.success('删除成功')
    // 如果删除的是当前选中的产品，清除选中状态
    if (selectedProductId.value === product.productId) {
      selectedProductId.value = null
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 检查待审申请
const checkPendingApplications = async (productId) => {
  try {
    const response = await loanAPI.getPendingApplications()
    return response.data?.some(app => app.productId === productId) || false
  } catch (error) {
    console.error('检查待审申请失败:', error)
    return false
  }
}

// 工具函数
const formatDate = (dateString) => {
  if (!dateString) return '—'
  return new Date(dateString).toLocaleString('zh-CN')
}
</script>

<style scoped>
.product-table-container {
  width: 100%;
}

.product-search {
  display: flex;
  gap: 10px;
  margin-bottom: 15px;
  align-items: center;
}

.input-group {
  display: flex;
  align-items: center;
  gap: 5px;
}

.btn {
  padding: 8px 16px;
  background-color: #409EFF;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.btn:hover {
  background-color: #66b1ff;
}

.ellipsis {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 150px;
}

/* 搜索状态提示 */
.search-status {
  font-size: 14px;
  color: #28a745;
  margin-left: 10px;
  padding: 5px 10px;
  background: #e8f5e8;
  border-radius: 4px;
}

/* 添加选中行的样式 */
.selected-row {
  background-color: #f0f9ff;
  border-left: 3px solid #409EFF;
}

.selected-row:hover {
  background-color: #e6f7ff;
}

/* 点击行的样式 */
.product-table tbody tr {
  cursor: pointer;
  transition: background-color 0.2s;
}

.product-table tbody tr:hover {
  background-color: #f5f5f5;
}

.selected-row:hover {
  background-color: #e6f7ff;
}

/* 详情区域样式 */
.product-detail-section {
  margin-top: 30px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 10px;
  margin-top: 15px;
}

.page-btn {
  padding: 6px 12px;
  background-color: #409EFF;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.toggle-status-btn,
.delete-prod-btn {
  padding: 4px 8px;
  margin: 0 2px;
  border: none;
  border-radius: 3px;
  cursor: pointer;
  font-size: 12px;
}

.toggle-status-btn {
  background-color: #409EFF;
  color: white;
}

.delete-prod-btn {
  background-color: #F56C6C;
  color: white;
}

/* 确保表格单元格中的按钮不会触发行点击 */
.product-table tbody td button {
  pointer-events: auto;
}

/* 确保其他单元格可以点击 */
.product-table tbody td:not(:last-child) {
  cursor: pointer;
}
</style>