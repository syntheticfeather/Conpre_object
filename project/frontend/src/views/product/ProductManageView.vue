<template>
  <div v-show="showProductTable" class="loan-management">
    <div class="header">
      <h2>贷款管理</h2>
      <button class="add-product-btn" @click="goToAddProduct">
        添加新项目
      </button>
    </div>

    <!-- 产品列表 -->
    <ProductTable @product-selected="handleProductSelected" />
  </div>

  <!-- 产品详情 -->
  <div v-if="showProductDetail && selectedProductId" class="product-detail-section">
    <ProductDetailPanel 
      :product-id="selectedProductId"
      @close="closeProductDetail"
      @saved="handleProductSaved"
    />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import ProductTable from '@/components/product/ProductTable.vue'
import ProductDetailPanel from '@/components/product/ProductDetailPanel.vue'

const router = useRouter()
const selectedProductId = ref(null)
const showProductDetail = ref(false)
const showProductTable = ref(true)

// 处理产品选择事件
const handleProductSelected = (productId) => {
  selectedProductId.value = productId
  showProductDetail.value = true
  showProductTable.value = false
}

// 关闭产品详情
const closeProductDetail = () => {
  showProductDetail.value = false
  selectedProductId.value = null
  showProductTable.value = true
}

// 产品保存后刷新
const handleProductSaved = () => {
  closeProductDetail()
}

// 跳转到添加产品页面
const goToAddProduct = () => {
  router.push('/dashboard/add-pro')
}
</script>

<style scoped>
.loan-management {
  padding: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.header h2 {
  width: auto;

  font-size: 25px;
  font-weight: 600;
  font-family: 方正小标宋，楷体，微软雅黑;
}

.add-product-btn {
  padding: 10px 20px;
  background-color: var(--product-btn-primary);
  color: var(--color-white);
  border: none;
  border-radius: 12px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: background-color 0.3s;
}

.add-product-btn:hover {
  background-color: var(--product-btn-hover);
}
</style>