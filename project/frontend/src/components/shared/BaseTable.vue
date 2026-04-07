<template>
  <div class="table-content">
    <!-- 批量操作区域 -->
    <div v-if="showBatchActions && selectedRowKeys.length > 0" class="batch-actions">
      <span class="selected-info">已选择 {{ selectedRowKeys.length }} 项</span>
      <slot name="batch-actions" :selected-rows="selectedRows" :selected-keys="selectedRowKeys">
        <a-button type="primary" size="small" @click="handleBatchDelete">批量删除</a-button>
      </slot>
    </div>

    <!-- 表格主体 -->
    <div class="data-table">
      <a-table
        :columns="processedColumns"
        :data-source="dataSource"
        :loading="loading"
        :pagination="false"
        :row-key="rowKey"
        :row-selection="showRowSelection ? rowSelectionConfig : undefined"
        :scroll="scroll"
        :custom-row="customRow"
        @change="handleTableChange"
      >
        <!-- 自定义列内容 -->
        <template #bodyCell="{ column, record, index }">
          <!-- 索引列 -->
          <template v-if="column.key === 'index'">
            {{ (currentPage - 1) * pageSize + index + 1 }}
          </template>

          <!-- 操作列 -->
          <template v-else-if="column.key === 'action'">
            <slot name="action" :record="record" :index="index">
              <a-space>
                <a-button type="link" size="small" @click.stop="handleEdit(record)">编辑</a-button>
                <a-button type="link" size="small" danger @click.stop="handleDelete(record)">删除</a-button>
              </a-space>
            </slot>
          </template>

          <!-- 自定义列插槽 -->
          <template v-else-if="column.slotName">
            <slot :name="column.slotName" :record="record" :column="column" :index="index" />
          </template>

          <!-- 默认显示 -->
          <template v-else>
            {{ record[column.dataIndex] }}
          </template>
        </template>

        <!-- 自定义筛选下拉 -->
        <template #customFilterDropdown="{ setSelectedKeys, selectedKeys, confirm, clearFilters, column }">
          <div style="padding: 8px">
            <a-input
              :value="selectedKeys[0]"
              :placeholder="`搜索${column.title}`"
              style="width: 188px; margin-bottom: 8px; display: block"
              @change="e => setSelectedKeys(e.target.value ? [e.target.value] : [])"
              @pressEnter="handleSearch(selectedKeys, confirm)"
            />
            <a-space>
              <a-button type="primary" size="small" @click="handleSearch(selectedKeys, confirm)">
                搜索
              </a-button>
              <a-button size="small" @click="handleReset(clearFilters)">重置</a-button>
            </a-space>
          </div>
        </template>
      </a-table>
    </div>

    <!-- 分页组件 -->
    <BasePagination
      v-if="showPagination "
      :current-page="currentPage"
      :total="total"
      :page-size="pageSize"
      @page-change="handlePageChange"
    />
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import BasePagination from './BasePagination.vue'

const props = defineProps({
  dataSource: {
    type: Array,
    default: () => []
  },
  columns: {
    type: Array,
    required: true
  },
  loading: {
    type: Boolean,
    default: false
  },
  currentPage: {
    type: Number,
    default: 1
  },
  pageSize: {
    type: Number,
    default: 10
  },
  total: {
    type: Number,
    default: 0
  },
  rowKey: {
    type: String,
    default: 'id'
  },
  showPagination: {
    type: Boolean,
    default: true
  },
  showRowSelection: {
    type: Boolean,
    default: false
  },
  showBatchActions: {
    type: Boolean,
    default: false
  },
  showIndex: {
    type: Boolean,
    default: false
  },
  showAction: {
    type: Boolean,
    default: false
  },
  scroll: {
    type: Object,
    default: undefined
  },
  rowClickable: {
    type: Boolean,
    default: true
  },
  minHeight: {
    type: String,
    default: '220px'
  }
})

const emit = defineEmits([
  'page-change',
  'edit',
  'delete',
  'batch-delete',
  'selection-change',
  'table-change',
  'row-click'
])

const selectedRowKeys = ref([])
const selectedRows = ref([])

const processedColumns = computed(() => {
  const cols = [...props.columns]

  if (props.showIndex) {
    cols.unshift({
      title: '序号',
      key: 'index',
      width: 70,
      align: 'center',
      fixed: 'left'
    })
  }

  if (props.showAction) {
    cols.push({
      title: '操作',
      key: 'action',
      width: 150,
      align: 'center',
      fixed: 'right'
    })
  }

  return cols
})

const rowSelectionConfig = computed(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys, rows) => {
    selectedRowKeys.value = keys
    selectedRows.value = rows
    emit('selection-change', keys, rows)
  },
  getCheckboxProps: (record) => ({
    disabled: record.disabled || false
  })
}))

const customRow = (record, index) => {
  return {
    onClick: () => {
      if (props.rowClickable) {
        emit('row-click', record, index)
      }
    },
    style: {
      cursor: props.rowClickable ? 'pointer' : 'default'
    }
  }
}

const handlePageChange = (page) => {
  emit('page-change', page)
}

const handleTableChange = (pagination, filters, sorter) => {
  emit('table-change', { pagination, filters, sorter })
}

const handleEdit = (record) => {
  emit('edit', record)
}

const handleDelete = (record) => {
  emit('delete', record)
}

const handleBatchDelete = () => {
  emit('batch-delete', selectedRowKeys.value, selectedRows.value)
}

const handleSearch = (selectedKeys, confirm) => {
  confirm()
}

const handleReset = (clearFilters) => {
  clearFilters({ confirm: true })
}

const clearSelection = () => {
  selectedRowKeys.value = []
  selectedRows.value = []
}

const setSelection = (keys, rows = []) => {
  selectedRowKeys.value = keys
  selectedRows.value = rows
}

defineExpose({
  clearSelection,
  setSelection,
  getSelection: () => ({ keys: selectedRowKeys.value, rows: selectedRows.value })
})
</script>

<style scoped>
.table-content {
  padding-bottom: 10px;

  background-color: var(--table-content-bg);
  border-radius: 8px;
  box-shadow: var(--table-content-shadow);
  transition: transform 0.3s;
}

.data-table {

  width: 100%;
  min-height: v-bind(minHeight);
  color: var(--table-color);
  overflow-y: hidden;
}

.batch-actions {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 16px;
  margin-bottom: 16px;

  height: 44px;
  
  background-color: var(--batch-actions-bg);
  border: 1px solid var(--batch-actions-border);
  border-radius: 4px;
}

.selected-info {
  font-size: 14px;
  color: var(--selected-info-color);
  font-weight: 500;
}

:deep(.ant-table) {
  font-size: 14px;
  color: var(--table-color);
  background-color: var(--table-bg);
  border: none;
}

:deep(.ant-table-thead > tr > th) { 
  padding: 7px;
  text-align: center;

  font-weight: 600;
  color: var(--table-color);  
  background-color: var(--table-head-bg);
  border-bottom: 1px solid var(--table-th-td-border);
}

:deep(.ant-table-tbody) {
  background-color: transparent !important;
}

:deep(.ant-table-tbody > tr) {
  background-color: transparent !important;
}

:deep(.ant-table-tbody > tr > td) {
  padding: 7px;
  text-align: center;
  border-bottom: 1px solid var(--table-th-td-border);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  background-color: transparent !important;
}

:deep(.ant-table-tbody > tr:hover > td) {
  background-color: var(--table-tr-hover) !important;
  transition: background-color 0.2s;
}

:deep(.ant-table-tbody > tr.ant-table-row-selected > td) {
  background-color: var(--table-tr-hover) !important;
}

:deep(.ant-table-tbody > tr) {
  cursor: pointer;
}

:deep(.ant-btn-link) {
  padding: 0 4px;
}

:deep(.ant-empty) {
  padding: 20px 0;
}
</style>
