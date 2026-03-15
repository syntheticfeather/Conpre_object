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

      <span v-if="hasSearchCriteria" class="search-status">
        当前显示筛选结果 ({{ loanStore.products.length }} 条)
      </span>
    </div>

    <!-- 产品表格 -->
    <BaseTable
      ref="tableRef"
      :data-source="paginatedProducts"
      :columns="columns"
      :current-page="currentPage"
      :total="loanStore.products.length"
      :page-size="pageSize"
      :row-key="'productId'"
      :show-row-selection="true"
      :show-batch-actions="true"
      :show-index="true"
      :show-action="true"
      :row-clickable="true"
      @page-change="handlePageChange"
      @row-click="selectProduct"
      @selection-change="handleSelectionChange"
      @batch-delete="handleBatchDelete"
    >
      <!-- 贷款描述列 -->
      <template #description="{ record }">
        <span :title="record.description" class="ellipsis">
          {{ record.description || '—' }}
        </span>
      </template>

      <!-- 金额范围列 -->
      <template #amountRange="{ record }">
        {{ record.minAmount || 0 }} - {{ record.maxAmount || 0 }} 元
      </template>

      <!-- 产品状态列 -->
      <template #status="{ record }">
        <a-tag :color="record.status === '上架中' ? 'green' : 'red'">
          {{ record.status || '—' }}
        </a-tag>
      </template>

      <!-- 更新时间列 -->
      <template #updateTime="{ record }">
        {{ formatDate(record.updateTime) }}
      </template>

      <!-- 创建时间列 -->
      <template #createTime="{ record }">
        {{ formatDate(record.createTime) }}
      </template>

      <!-- 操作列 -->
      <template #action="{ record }">
        <a-space>
          <a-button
            v-if="record.status === '上架中'"
            type="primary"
            size="small"
            @click.stop="toggleProductStatus(record, 'deactive')"
          >
            下架
          </a-button>
          <a-button
            v-else
            size="small"
            @click.stop="toggleProductStatus(record, 'active')"
          >
            上架
          </a-button>
          <a-button
            type="primary"
            size="small"
            danger
            @click.stop="deleteProduct(record)"
          >
            删除
          </a-button>
        </a-space>
      </template>

      <!-- 自定义批量操作 -->
      <template #batch-actions="{ selectedRows, selectedKeys }">
        <a-space>
          <a-button type="primary" danger @click="handleBatchDelete(selectedKeys, selectedRows)">
            批量删除
          </a-button>
          <a-button @click="batchOffline(selectedKeys, selectedRows)">
            批量下架
          </a-button>
        </a-space>
      </template>
    </BaseTable>

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
import { loanAPI, applicationAPI } from '@/api'
import ProductDetailPanel from './ProductDetailPanel.vue'
import BaseTable from '@/components/shared/BaseTable.vue'

const loanStore = useLoanStore()
const tableRef = ref(null)

const createDateRange = ref([])
const updateDateRange = ref([])
const currentPage = ref(1)
const pageSize = 5
const selectedProductId = ref(null)
const selectedRows = ref([])
const selectedKeys = ref([])

const hasSearchCriteria = computed(() => {
  return createDateRange.value.length > 0 || updateDateRange.value.length > 0
})

const columns = [
  {
    title: '贷款名称',
    dataIndex: 'productName',
    key: 'productName'
  },
  {
    title: '贷款描述',
    dataIndex: 'description',
    key: 'description',
    slotName: 'description'
  },
  {
    title: '贷款用途',
    dataIndex: 'loanUsage',
    key: 'loanUsage'
  },
  {
    title: '金额范围',
    key: 'amountRange',
    slotName: 'amountRange'
  },
  {
    title: '产品状态',
    dataIndex: 'status',
    key: 'status',
    slotName: 'status'
  },
  {
    title: '更新时间',
    dataIndex: 'updateTime',
    key: 'updateTime',
    slotName: 'updateTime'
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    key: 'createTime',
    slotName: 'createTime'
  }
]

const paginatedProducts = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return loanStore.products.slice(start, start + pageSize)
})

onMounted(async () => {
  await loanStore.fetchProducts()
})

const handlePageChange = (page) => {
  currentPage.value = page
}

const handleSelectionChange = (keys, rows) => {
  selectedKeys.value = keys
  selectedRows.value = rows
}

const searchProducts = async () => {
  const params = {}
  
  if (createDateRange.value?.length === 2) {
    params.createStartDate = createDateRange.value[0]
    params.createEndDate = createDateRange.value[1]
  }
  
  if (updateDateRange.value?.length === 2) {
    params.updateStartDate = updateDateRange.value[0]
    params.updateEndDate = updateDateRange.value[1]
  }
  
  if (!params.createStartDate && !params.updateStartDate) {
    ElMessage.warning('请选择至少一个日期范围进行搜索')
    return
  }
  
  try {
    const response = await loanAPI.searchProductsByTime(params)
    
    if (response.code === 200 && response.data) {
      loanStore.products = response.data
      
      if (loanStore.products.length > 0) {
        ElMessage.success(`找到 ${loanStore.products.length} 个符合条件的商品`)
      } else {
        ElMessage.info('未找到符合条件的商品')
      }
    } else {
      ElMessage.warning('返回数据格式不正确')
      loanStore.products = []
    }
    
    currentPage.value = 1
  } catch (error) {
    console.error('搜索产品失败:', error)
    ElMessage.error('搜索失败')
    loanStore.products = []
  }
}

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

const selectProduct = (record) => {
  if (selectedProductId.value === record.productId) {
    selectedProductId.value = null
  } else {
    selectedProductId.value = record.productId
  }
}

const clearSelection = () => {
  selectedProductId.value = null
}

const handleProductSaved = () => {
  loanStore.fetchProducts()
  selectedProductId.value = null
}

const toggleProductStatus = async (product, action) => {
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

const deleteProduct = async (product) => {
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
    
    const hasPending = await checkPendingApplications(product.productId)
    if (hasPending) {
      ElMessage.warning('该产品存在待审核申请，无法删除！')
      return
    }
    
    await loanStore.deleteProduct(product.productId)
    ElMessage.success('删除成功')
    
    if (selectedProductId.value === product.productId) {
      selectedProductId.value = null
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const checkPendingApplications = async (productId) => {
  try {
    const response = await applicationAPI.getPendingApplications()
    return response.data?.some(app => app.productId === productId) || false
  } catch (error) {
    console.error('检查待审申请失败:', error)
    return false
  }
}

const getPendingProductIds = async () => {
  try {
    const response = await applicationAPI.getPendingApplications()
    const pendingProductIds = response.data?.map(app => app.productId) || []
    return [...new Set(pendingProductIds)]
  } catch (error) {
    console.error('获取待审申请产品ID失败:', error)
    return []
  }
}

const handleBatchDelete = async (keys, rows) => {
  if (!keys || keys.length === 0) {
    ElMessage.warning('请先选择要删除的产品')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定删除选中的 ${keys.length} 个产品？`,
      '批量删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const pendingProductIds = await getPendingProductIds()
    
    const productsWithPending = keys.filter(id => pendingProductIds.includes(id))
    const productsWithoutPending = keys.filter(id => !pendingProductIds.includes(id))

    if (productsWithPending.length > 0) {
      const pendingProductNames = rows
        .filter(row => productsWithPending.includes(row.productId))
        .map(row => row.productName)
        .join('、')

      if (productsWithoutPending.length === 0) {
        ElMessage.warning(`以下产品存在待审核申请，无法删除：${pendingProductNames}`)
        return
      }

      await ElMessageBox.confirm(
        `以下产品存在待审核申请，将跳过删除：\n${pendingProductNames}\n\n是否继续删除其他 ${productsWithoutPending.length} 个产品？`,
        '部分产品无法删除',
        {
          confirmButtonText: '继续删除',
          cancelButtonText: '取消',
          type: 'warning'
        }
      )
    }

    let successCount = 0
    let failCount = 0

    for (const productId of productsWithoutPending) {
      try {
        await loanStore.deleteProduct(productId)
        successCount++
      } catch {
        failCount++
      }
    }

    if (successCount > 0) {
      ElMessage.success(`成功删除 ${successCount} 个产品`)
    }
    if (failCount > 0) {
      ElMessage.error(`${failCount} 个产品删除失败`)
    }

    tableRef.value?.clearSelection()
    await loanStore.fetchProducts()
    
    if (selectedProductId.value && keys.includes(selectedProductId.value)) {
      selectedProductId.value = null
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量删除失败:', error)
    }
  }
}

const batchOffline = async (keys) => {
  if (!keys || keys.length === 0) {
    ElMessage.warning('请先选择要下架的产品')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定下架选中的 ${keys.length} 个产品？`,
      '批量下架',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    let successCount = 0
    for (const productId of keys) {
      try {
        await loanStore.toggleProductStatus(productId, 'deactive')
        successCount++
      } catch (error) {
        console.error(`下架产品 ${productId} 失败:`, error)
      }
    }

    ElMessage.success(`成功下架 ${successCount} 个产品`)
    tableRef.value?.clearSelection()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('批量下架失败')
    }
  }
}

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
  flex-wrap: wrap;
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
  display: inline-block;
}

.search-status {
  font-size: 14px;
  color: #28a745;
  margin-left: 10px;
  padding: 5px 10px;
  background: #e8f5e8;
  border-radius: 4px;
}

.product-detail-section {
  margin-top: 30px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  animation: fadeIn 0.3s ease;
}

.data-table {
  min-height: 316px;
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
</style>
