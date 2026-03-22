<template>
  <div class="user-manage-view">
    <div class="header">
      <h2 class="title">催收管理</h2>
      
      <!-- 搜索框 根据信誉分范围搜索用户并从高到底排序展示 -->
      <div class="search-section">
        <el-input
          v-model="searchText"
          placeholder="请输入信誉分范围，例如<100"
          clearable
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
          <template #append>
            <el-button @click="handleSearch" id="btn-search">搜索</el-button>
          </template>
        </el-input>
      </div>
    </div>
    
  </div>
</template>

<script setup>
</script>

<style scoped>
</style>