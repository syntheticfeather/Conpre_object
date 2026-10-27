<template>
  <div class="add-product-view">
    <!-- 页头：标题 + 返回按钮 -->
    <div class="apv-header">
      <h1 class="apv-header__title">新增贷款产品</h1>

      <button class="apv-header__back" @click="goBack">返回产品列表</button>
    </div>

    <div class="apv-shell">
      <!-- ============ 左栏：完成度 + 上下文提示 ============ -->
      <aside class="apv-rail">
        <div class="apv-panel apv-completion">
          <el-progress
            type="circle"
            :percentage="completionPercent"
            :width="62"
            :stroke-width="7"
            :show-text="false"
          />
          <div class="apv-completion__meta">
            <strong>{{ completionPercent }}%</strong>
            <span>已填 {{ completedCount }} / {{ totalCount }} 项</span>
          </div>
        </div>

        <div class="apv-tip">
          <div class="apv-tip__head">
            <svg class="apv-ic" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
              <circle cx="12" cy="12" r="4" />
              <path d="M12 3v1.5M12 19.5V21M4.2 4.2l1.1 1.1M18.7 18.7l1.1 1.1M3 12h1.5M19.5 12H21M4.2 19.8l1.1-1.1M18.7 5.3l1.1-1.1" />
            </svg>
            步长决定可选档位
          </div>
          <p>
            最短 {{ form.minTerm }} 个月、最长 {{ form.maxTerm }} 个月、步长 {{ form.termStep }}。
            用户可选 <b>{{ tiers.length }}</b> 档：{{ tiers.join(' / ') }}
          </p>
        </div>

        <!-- 营销信息（左栏常驻，不参与步骤切换） -->
        <div class="apv-panel apv-promo">
          <div class="apv-card__head apv-promo__head">
            <span class="apv-card__icon">
              <svg class="apv-ic" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M9 20.5c-3-.6-5.2-2.6-5.2-5.4 0-1.4.5-2.7 1.4-3.8-.4-1.6.2-3.5 1.5-4.4-.1-1.9 1.3-3.6 3.2-3.9 1.9-.4 3.8.6 4.6 2.4 1.4.2 2.6 1.1 3.2 2.4.9-.2 1.9.1 2.6.7" />
                <path d="M12 20.5c4.4 0 8-1.6 8-3.6 0-1.3-1.2-2.4-3.1-3.1" />
              </svg>
            </span>
            <span class="apv-card__title">营销信息</span>
          </div>
          <span class="apv-promo__label">促销信息</span>
          <el-input
            v-model="form.promotionDetails"
            type="textarea"
            :rows="4"
            placeholder="请输入促销描述"
          />
        </div>
      </aside>

      <!-- ============ 中栏：表单主体 ============ -->
      <div class="apv-main">
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
          <!-- 01 产品标识 · 第 1 步 -->
          <section v-if="currentStep === 1" class="apv-card">
            <div class="apv-card__head">
              <span class="apv-card__icon">
                <svg class="apv-ic" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M20.6 13.4L12.9 5.7a2 2 0 0 0-1.4-.6H5.5A1.5 1.5 0 0 0 4 6.6v6a2 2 0 0 0 .6 1.4l7.7 7.7a2 2 0 0 0 2.8 0l5.5-5.5a2 2 0 0 0 0-2.8z" />
                  <circle cx="8.2" cy="9.2" r="1.3" />
                </svg>
              </span>
              <span class="apv-card__title">产品标识</span>
              <span class="apv-card__index">01 / 03</span>
            </div>

            <div class="apv-grid apv-grid--2">
              <el-form-item label="产品名称" prop="productName">
                <el-input v-model="form.productName" placeholder="请输入产品名称" />
              </el-form-item>

              <el-form-item label="产品类型" prop="productType">
                <el-select v-model="form.productType" placeholder="请选择产品类型" style="width: 100%">
                  <el-option label="个人消费" value="个人消费" />
                  <el-option label="农业生产" value="农业生产" />
                  <el-option label="企业经营" value="企业经营" />
                </el-select>
              </el-form-item>

              <el-form-item label="产品用途" prop="loanUsage" class="apv-span-2">
                <el-input v-model="form.loanUsage" placeholder="请输入贷款用途" />
              </el-form-item>

              <el-form-item label="产品描述" prop="description" class="apv-span-2">
                <el-input
                  v-model="form.description"
                  type="textarea"
                  :rows="3"
                  placeholder="请输入产品描述"
                />
              </el-form-item>
            </div>
          </section>

          <!-- 02 额度与期限 · 第 2 步 -->
          <section v-if="currentStep === 2" class="apv-card">
            <div class="apv-card__head">
              <span class="apv-card__icon">
                <svg class="apv-ic" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <rect x="3" y="4.5" width="18" height="16" rx="2.5" />
                  <path d="M3 9.5h18M8 3v3M16 3v3" />
                </svg>
              </span>
              <span class="apv-card__title">额度与期限</span>
              <span class="apv-card__index">02 / 03</span>
            </div>

            <div class="apv-grid apv-grid--2">
              <el-form-item label="最短期限" prop="minTerm" hide-required-asterisk>
                <el-input-number
                  v-model="form.minTerm"
                  :min="1"
                  controls-position="right"
                  style="width: 100%"
                  @change="updateTermStepOptions"
                >
                  <template #suffix>个月</template>
                </el-input-number>
              </el-form-item>

              <el-form-item label="最长期限" prop="maxTerm" hide-required-asterisk>
                <el-input-number
                  v-model="form.maxTerm"
                  :min="form.minTerm"
                  controls-position="right"
                  style="width: 100%"
                  @change="updateTermStepOptions"
                >
                  <template #suffix>个月</template>
                </el-input-number>
              </el-form-item>

              <el-form-item label="期限步长" prop="termStep" hide-required-asterisk>
                <el-select
                  v-model="form.termStep"
                  placeholder="请选择期限步长"
                  style="width: 100%"
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

              <el-form-item label="可选档位数" hide-required-asterisk>
                <div class="apv-readonly">{{ tiers.length }} 档</div>
              </el-form-item>
            </div>

            <!-- 期限档位可视化 -->
            <div class="apv-axis">
              <span class="apv-axis__label">期限档位分布（由步长自动推导）</span>
              <div class="apv-axis__track">
                <span
                  v-for="(t, i) in tiers"
                  :key="t"
                  class="apv-axis__tick"
                  :class="{ 'is-first': i === 0 }"
                  :style="{ left: tierPercent(t) }"
                >
                  <b>{{ t }}</b>
                </span>
              </div>
            </div>

            <div class="apv-grid apv-grid--2">
              <el-form-item label="最小金额" prop="minAmount" hide-required-asterisk>
                <el-input-number
                  v-model="form.minAmount"
                  :min="1"
                  :step="1000"
                  controls-position="right"
                  style="width: 100%"
                >
                  <template #suffix>元</template>
                </el-input-number>
                <div class="apv-chips">
                  <button
                    v-for="v in minAmountPresets"
                    :key="'min' + v"
                    type="button"
                    class="apv-chip"
                    @click="form.minAmount = v"
                  >
                    {{ formatWan(v) }}
                  </button>
                </div>
              </el-form-item>

              <el-form-item label="最大金额" prop="maxAmount" hide-required-asterisk>
                <el-input-number
                  v-model="form.maxAmount"
                  :min="form.minAmount"
                  :step="1000"
                  controls-position="right"
                  style="width: 100%"
                >
                  <template #suffix>元</template>
                </el-input-number>
                <div class="apv-chips">
                  <button
                    v-for="v in maxAmountPresets"
                    :key="'max' + v"
                    type="button"
                    class="apv-chip"
                    @click="form.maxAmount = v"
                  >
                    {{ formatWan(v) }}
                  </button>
                </div>
              </el-form-item>
            </div>

            <!-- 利率区间（由方案利率推导，只读展示） -->
            <div class="apv-axis">
              <span class="apv-axis__label">参考利率区间（由下方方案利率自动汇总）</span>
              <div class="apv-rateband">
                <span class="apv-rateband__track"></span>
                <span
                  v-if="rateRange.has"
                  class="apv-rateband__fill"
                  :style="rateFillStyle"
                ></span>
                <span
                  v-for="(r, i) in optionRates"
                  :key="'rate' + i"
                  class="apv-rateband__tick"
                  :style="{ left: ratePercent(r) }"
                ></span>
              </div>
              <div class="apv-axis__legend">
                <span class="apv-mono">{{ rateRangeText }}</span>
                <span class="apv-hint">等额本息月供随利率上升</span>
              </div>
            </div>
          </section>

          <!-- 03 贷款可选方案 · 第 3 步 -->
          <section v-if="currentStep === 3" class="apv-card">
            <div class="apv-card__head">
              <span class="apv-card__icon">
                <svg class="apv-ic" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
                  <path d="M4 6h16M4 12h16M4 18h10" />
                </svg>
              </span>
              <span class="apv-card__title">贷款可选方案</span>
              <span class="apv-card__index">03 / 03</span>
              <span class="apv-card__count">{{ form.options.length }} 个方案</span>
            </div>

            <div class="apv-tablewrap">
              <table class="apv-table">
                <colgroup>
                  <col class="apv-col-term" />
                  <col class="apv-col-rate" />
                  <col class="apv-col-type" />
                  <col class="apv-col-op" />
                </colgroup>
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
                        placeholder="利率"
                      >
                        <template #suffix>%</template>
                      </el-input-number>
                    </td>
                    <td>
                      <el-select v-model="option.repaidType" placeholder="选择还款方式">
                        <el-option label="等额本息" value="等额本息" />
                        <el-option label="等额本金" value="等额本金" />
                        <el-option label="先息后本" value="先息后本" />
                        <el-option label="一次性还本付息" value="一次性还本付息" />
                      </el-select>
                    </td>
                    <td>
                      <button
                        type="button"
                        class="apv-opdel"
                        :disabled="form.options.length === 1"
                        title="删除"
                        @click="removeOption(index)"
                      >
                        <svg class="apv-ic" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
                          <path d="M5 7h14M9.5 7V5h5v2M7 7l1 12h8l1-12" />
                        </svg>
                      </button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>

            <div class="apv-optbar">
              <button type="button" class="apv-btn apv-btn--ghost" @click="generateTierOptions">
                按步长一键生成全部档位
              </button>
              <button type="button" class="apv-btn apv-btn--ghost" @click="addOptionRow">
                + 增加方案
              </button>
            </div>

            <div class="apv-note">至少需要有一个完整的贷款方案</div>
          </section>
        </el-form>
      </div>

      <!-- ============ 右栏：实时预览 + 步骤切换 ============ -->
      <aside class="apv-side">
        <div class="apv-preview">
          <span class="apv-preview__tag">
            <svg class="apv-ic" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
              <circle cx="12" cy="12" r="9" />
              <path d="M12 7.5V12l3 2" />
            </svg>
            实时预览
          </span>
          <div class="apv-preview__name">{{ form.productName || '未命名产品' }}</div>
          <div class="apv-preview__desc">{{ form.description || '暂无产品描述' }}</div>

          <div class="apv-preview__stats">
            <div class="apv-stat">
              <em>利率区间</em>
              <b class="apv-mono">{{ rateRangeText }}</b>
            </div>
            <div class="apv-stat">
              <em>额度范围</em>
              <b class="apv-mono">{{ amountRangeText }}</b>
            </div>
            <div class="apv-stat">
              <em>期限档位</em>
              <b class="apv-mono">{{ tiers.length }} 档</b>
            </div>
            <div class="apv-stat">
              <em>产品类型</em>
              <b>{{ form.productType || '—' }}</b>
            </div>
          </div>

          <div class="apv-preview__foot">
            <span>{{ form.promotionDetails || '暂无促销信息' }}</span>
            <span>草稿 · 未上架</span>
          </div>
        </div>

        <!-- 步骤切换（原月供试算的位置） -->
        <div class="apv-panel apv-stepbar">
          <span class="apv-stepbar__pos">第 {{ currentStep }} / 3 步</span>
          <div class="apv-stepbar__btns">
            <button
              v-if="currentStep === 1"
              class="apv-btn apv-btn--ghost"
              @click="handleCancel"
            >
              取消
            </button>
            <button v-else class="apv-btn apv-btn--ghost" @click="goPrev">上一步</button>

            <button
              v-if="currentStep < 3"
              class="apv-btn apv-btn--primary"
              @click="goNext"
            >
              下一步
            </button>
            <button v-else class="apv-btn apv-btn--primary" @click="handleSubmit">
              确认添加
            </button>
          </div>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useLoanStore } from '@/stores/loan'

const router = useRouter()
const loanStore = useLoanStore()
const formRef = ref()

// 当前步骤：1 产品标识 → 2 额度与期限 → 3 贷款可选方案
const currentStep = ref(1)

// 完成度用的“用户改动标记”：期限/金额/方案都有默认值，
// 只有被用户真正改动过才计入进度，否则一进页面就会凭空算出 60%
const touched = reactive({ term: false, amount: false, options: false })
let trackingReady = false

// 步长选项
const termStepOptions = ref([])

// 表单数据
const form = reactive({
  productName: '',
  description: '',
  loanUsage: '',
  productType: '',
  minTerm: 1,
  maxTerm: 13,
  termStep: 2,
  minAmount: 1000,
  maxAmount: 500000,
  promotionDetails: '限时优惠：新客户首笔贷款可享利率 9 折，提前还款免违约金。',
  options: []
})

// 金额快捷档位
const minAmountPresets = [500, 1000, 2000, 5000]
const maxAmountPresets = [10000, 50000, 100000, 500000]

// 计算并更新步长选项
const updateTermStepOptions = () => {
  const min = form.minTerm || 1
  const max = form.maxTerm || 12

  if (max < min) {
    form.maxTerm = min
  }

  const diff = form.maxTerm - form.minTerm

  // 步长必须大于最短期限，且能整除差值
  const options = []
  for (let i = min + 1; i <= diff; i++) {
    if (diff % i === 0) {
      options.push(i)
    }
  }

  if (diff === 0 || options.length === 0) {
    termStepOptions.value = [diff > 0 ? Math.min(diff, min + 1) : 1]
    form.termStep = termStepOptions.value[0]
  } else {
    termStepOptions.value = options

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

  // 初始化写入完成后再开始记录“用户改动”
  nextTick(() => {
    trackingReady = true
  })
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
  productType: [
    { required: true, message: '请选择产品类型', trigger: 'change' }
  ],
  minTerm: [
    { required: true, message: '请输入最短期限', trigger: 'blur' }
  ],
  minAmount: [
    { required: true, message: '请输入最小金额', trigger: 'blur' }
  ],
  maxAmount: [
    { required: true, message: '请输入最大金额', trigger: 'blur' }
  ],
  maxTerm: [
    { required: true, message: '请输入最长期限', trigger: 'blur' }
  ],
  termStep: [
    { required: true, message: '请选择期限步长', trigger: 'change' }
  ]
}

/* ---------------- 展示层派生数据（纯计算，不改动提交逻辑） ---------------- */

// 可选档位：由最短/最长/步长推导
const tiers = computed(() => {
  const step = form.termStep > 0 ? form.termStep : 1
  const list = []
  for (let t = form.minTerm; t <= form.maxTerm; t += step) {
    list.push(t)
  }
  if (list.length && list[list.length - 1] !== form.maxTerm) {
    list.push(form.maxTerm)
  }
  return list
})

// 档位在数轴上的百分比位置
const tierPercent = (t) => {
  const span = Math.max(form.maxTerm - form.minTerm, 1)
  return ((t - form.minTerm) / span) * 100 + '%'
}

// 各方案的利率（用于利率区间可视化）
const optionRates = computed(() =>
  form.options
    .map((opt) => Number(opt.interestRate))
    .filter((r) => r > 0)
)

const RATE_SCALE_MAX = 24

const ratePercent = (r) => Math.min((r / RATE_SCALE_MAX) * 100, 100) + '%'

const rateRange = computed(() => {
  const rates = optionRates.value
  if (!rates.length) {
    return { has: false, min: 0, max: 0 }
  }
  return { has: true, min: Math.min(...rates), max: Math.max(...rates) }
})

const rateRangeText = computed(() =>
  rateRange.value.has
    ? `${rateRange.value.min.toFixed(2)}% ~ ${rateRange.value.max.toFixed(2)}%`
    : '—'
)

const rateFillStyle = computed(() => {
  if (!rateRange.value.has) return { display: 'none' }
  const left = (rateRange.value.min / RATE_SCALE_MAX) * 100
  const width = ((rateRange.value.max - rateRange.value.min) / RATE_SCALE_MAX) * 100
  return { left: left + '%', width: width + '%' }
})

// 金额区间文案
const amountRangeText = computed(
  () => `${formatWan(form.minAmount)} ~ ${formatWan(form.maxAmount)}`
)

// 金额缩写：1000 → 1千，50000 → 5万
function formatWan(num) {
  const n = Number(num)
  if (!n && n !== 0) return '—'
  if (n >= 10000) {
    const w = n / 10000
    return (Number.isInteger(w) ? w : w.toFixed(1)) + '万'
  }
  if (n >= 1000) {
    const k = n / 1000
    return (Number.isInteger(k) ? k : k.toFixed(1)) + '千'
  }
  return n.toLocaleString()
}

// 完成度：7 项 —— 只统计用户真正提供/确认的内容（促销为选填不计入）
const totalCount = 7
const completedCount = computed(() => {
  const flags = [
    !!form.productName.trim(),
    !!form.productType,
    !!form.description.trim(),
    !!form.loanUsage.trim(),
    touched.term &&
      Number(form.minTerm) > 0 &&
      Number(form.maxTerm) >= Number(form.minTerm) &&
      Number(form.termStep) > 0,
    touched.amount &&
      Number(form.minAmount) > 0 &&
      Number(form.maxAmount) > Number(form.minAmount),
    touched.options &&
      form.options.length > 0 &&
      form.options.every(
        (opt) => opt.loanPeriod && opt.interestRate && opt.repaidType
      )
  ]
  return flags.filter(Boolean).length
})
const completionPercent = computed(() =>
  Math.floor((completedCount.value / totalCount) * 100)
)

// 返回产品列表
const goBack = () => {
  router.push('/dashboard/products')
}

// 监听期限变化，自动更新步长选项
watch(() => [form.minTerm, form.maxTerm], () => {
  updateTermStepOptions()
}, { deep: true })

// 用户改动期限 / 金额 / 方案 → 计入完成度
watch(
  () => [form.minTerm, form.maxTerm, form.termStep],
  () => { if (trackingReady) touched.term = true }
)
watch(
  () => [form.minAmount, form.maxAmount],
  () => { if (trackingReady) touched.amount = true }
)
watch(
  () => form.options,
  () => { if (trackingReady) touched.options = true },
  { deep: true }
)

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
    loanPeriod: form.minTerm,
    interestRate: 5.0,
    repaidType: '等额本息'
  })
}

// 按步长一键生成全部档位方案
const generateTierOptions = () => {
  const list = tiers.value
  if (!list.length) {
    ElMessage.warning('请先设置有效的期限范围')
    return
  }
  const lo = rateRange.value.has ? rateRange.value.min : 5.0
  const hi = rateRange.value.has ? rateRange.value.max : lo
  form.options = list.map((t, i) => ({
    loanPeriod: t,
    interestRate: Number(
      (lo + (hi - lo) * (list.length > 1 ? i / (list.length - 1) : 0)).toFixed(2)
    ),
    repaidType: '等额本息'
  }))
}

// 删除方案
const removeOption = (index) => {
  if (form.options.length <= 1) {
    ElMessage.warning('至少需要保留一个贷款方案')
    return
  }
  
  form.options.splice(index, 1)
}

// 第 1 步校验：产品标识
const validateBasics = () => {
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

  if (!form.productType) {
    ElMessage.warning('请选择产品类型')
    return false
  }
  return true
}

// 第 2 步校验：额度与期限
const validateTerms = () => {
  if (!form.minTerm || form.minTerm <= 0) {
    ElMessage.warning('最短期限必须大于 0')
    return false
  }

  if (!form.maxTerm || form.maxTerm <= 0) {
    ElMessage.warning('最长期限必须大于 0')
    return false
  }

  if (form.maxTerm < form.minTerm) {
    ElMessage.warning('最长期限不能小于最短期限')
    return false
  }

  if (!form.termStep || form.termStep <= 0) {
    ElMessage.warning('期限步长必须大于 0')
    return false
  }

  if (form.termStep <= form.minTerm) {
    ElMessage.warning('期限步长必须大于最短期限')
    return false
  }

  if ((form.maxTerm - form.minTerm) % form.termStep !== 0) {
    ElMessage.warning('最长期限与最短期限的差值必须是步长的整数倍')
    return false
  }

  if (!form.minAmount || form.minAmount <= 0) {
    ElMessage.warning('最小金额必须大于 0')
    return false
  }

  if (!form.maxAmount || form.maxAmount <= form.minAmount) {
    ElMessage.warning('最大金额必须大于最小金额')
    return false
  }
  return true
}

// 第 3 步校验：贷款可选方案
const validateOptions = () => {
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

    if (option.loanPeriod < form.minTerm || option.loanPeriod > form.maxTerm) {
      ElMessage.warning(`第 ${i + 1} 个方案：期限需在 ${form.minTerm}~${form.maxTerm} 个月之间`)
      return false
    }

    if ((option.loanPeriod - form.minTerm) % form.termStep !== 0) {
      ElMessage.warning(`第 ${i + 1} 个方案：期限需符合 ${form.termStep} 个月的步长`)
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

// 全量校验（提交前）
const validateForm = () =>
  validateBasics() && validateTerms() && validateOptions()

// 步骤切换
const goNext = () => {
  if (currentStep.value === 1 && !validateBasics()) return
  if (currentStep.value === 2 && !validateTerms()) return
  if (currentStep.value < 3) currentStep.value++
}

const goPrev = () => {
  if (currentStep.value > 1) currentStep.value--
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
      productType: form.productType,
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
    
    // 调用 store 添加产品
    const result = await loanStore.addProduct(productData)
    
    if (result.success) {
      ElMessage.success('产品添加成功')
      router.go(0)
    } else {
      ElMessage.error(result.message || '添加失败')
    }
  } catch (error) {
    ElMessage.error('提交失败：' + (error.message || '请检查网络连接'))
  }
}
</script>

<style scoped>
@import '@/assets/css/product/addProduct.css';
</style>

<style>
/* 期限步长选择器文本居中 — 非scoped才能覆盖Element Plus内部DOM */
.el-select .el-select__wrapper {
  text-align: center;
  padding-left: 12px;
}
.el-select .el-select__wrapper .el-select__selection {
  justify-content: center;
  flex: 1;
}
.el-select .el-select__wrapper .el-select__selected-item {
  justify-content: center;
  flex: none;
  width: auto;
}
.el-select .el-select__wrapper .el-select__single {
  display: flex;
  justify-content: center;
  width: 100%;
}
.el-select .el-select__wrapper .el-select__placeholder {
  justify-content: center;
  text-align: center;
  width: 100%;
}
</style>
