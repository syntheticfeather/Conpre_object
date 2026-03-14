<template>
  <div class="loan-management">
    <div class="header">
      <h2>贷款管理</h2>
      <button class="add-product-btn" @click="goToAddProduct">
        添加新项目
      </button>
    </div>

    <!-- 产品列表 -->
    <ProductTable />
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import ProductTable from '@/components/product/ProductTable.vue'

const router = useRouter()

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

.add-product-btn {
  padding: 10px 20px;
  background-color: #409EFF;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: background-color 0.3s;
}

.add-product-btn:hover {
  background-color: #66b1ff;
}
</style>