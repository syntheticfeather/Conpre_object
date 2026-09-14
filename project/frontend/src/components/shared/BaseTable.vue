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
    <div class="data-table" :class="{ 'is-empty': !dataSource || dataSource.length === 0 }">
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

        <!-- 装饰化空态：插画 + 引导文案 + CTA -->
        <template #emptyText>
          <div class="empty-state">
            <svg class="empty-state__art" viewBox="0 0 120 90" fill="none" xmlns="http://www.w3.org/2000/svg">
              <rect x="18" y="20" width="84" height="56" rx="8" fill="var(--brand-100)" opacity="0.6"/>
              <rect x="30" y="34" width="60" height="6" rx="3" fill="var(--brand-500)" opacity="0.45"/>
              <rect x="30" y="48" width="44" height="6" rx="3" fill="var(--brand-500)" opacity="0.3"/>
              <circle cx="88" cy="66" r="14" fill="var(--brand-accent)" opacity="0.25"/>
              <path d="M82 66l4.5 4.5L94 61" stroke="var(--brand-accent)" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <p class="empty-state__title">{{ emptyTitle }}</p>
            <p class="empty-state__desc">{{ emptyDescription }}</p>
            <a-button v-if="emptyCta" type="primary" shape="round" size="small" @click="$emit('empty-action')">
              {{ emptyCta }}
            </a-button>
          </div>
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
  },
  emptyTitle: {
    type: String,
    default: '暂无数据'
  },
  emptyDescription: {
    type: String,
    default: '数据会在这里展示，稍后再来看看吧'
  },
  emptyCta: {
    type: String,
    default: ''
  }
})

const emit = defineEmits([
  'page-change',
  'edit',
  'delete',
  'batch-delete',
  'selection-change',
  'table-change',
  'row-click',
  'empty-action'
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
  border-radius: 12px;
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
  border-radius: 12px;
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
  border-radius: var(--radius-md, 12px);
  overflow: hidden;
  /* 数字列等宽对齐：对文本无副作用，仅需作用于表格根节点 */
  font-variant-numeric: tabular-nums;
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

/* hover：品牌蓝浅底（brand-50 亮暗自适应）+ 首列品牌青左边框（inset 阴影避免布局位移） */
:deep(.ant-table-tbody > tr:hover ) {
  background-color: var(--brand-50) !important;
  transition: background-color var(--duration-fast, 150ms) var(--ease-out-expo, ease),
              box-shadow var(--duration-fast, 150ms) var(--ease-out-expo, ease);
}

:deep(.ant-table-tbody > tr:hover > td:first-child) {
  box-shadow: inset 3px 0 0 0 var(--brand-accent);
}

:deep(.ant-table-tbody > tr.ant-table-row-selected > td) {
  background-color: var(--table-tr-hover) !important;
}

:deep(.ant-table-tbody > tr) {
  cursor: pointer;
}

/* 每 5 行一条视觉分割线 */
:deep(.ant-table-tbody > tr:nth-child(5n)) > td {
  border-bottom: 1px solid rgba(15, 23, 42, 0.06) !important;
}

/* 数字列等宽对齐 */
:deep(.tabular-nums) {
  font-variant-numeric: tabular-nums;
  font-family: "PingFang SC", "Microsoft YaHei", system-ui;
}

:deep(.ant-btn-link) {
  padding: 0 4px;
}

:deep(.ant-empty) {
  padding: 20px 0;
}

/* ===== 空态装饰：点阵背景只作用于空区，不干扰有数据时的可读性 ===== */
.data-table.is-empty {
  background-image: var(--list-pattern);
  background-repeat: repeat;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-xl) 0 var(--space-lg);
}

.empty-state__art {
  width: 120px;
  height: 90px;
}

.empty-state__title {
  margin: 0;
  font-size: var(--text-lg);
  font-weight: 600;
  color: var(--text-color);
}

.empty-state__desc {
  margin: 0;
  font-size: var(--text-sm);
  color: var(--text-color-placeholder);
}
</style>
