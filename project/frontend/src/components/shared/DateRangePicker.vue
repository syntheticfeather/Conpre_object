<template>
  <div class="date-range-picker">
    <div class="input-group">
      <input 
        type="text" 
        :value="startDate" 
        placeholder="开始日期"
        readonly
        @focus="openCalendar"
      />
      <span>至</span>
      <input 
        type="text" 
        :value="endDate" 
        placeholder="结束日期"
        readonly
        @focus="openCalendar"
      />
    </div>
    
    <div v-show="showCalendar" class="calendar">
      <div class="calendar-header">
        <button @click="prevMonth">‹</button>
        <span>{{ currentMonth.format('YYYY年MM月') }}</span>
        <button @click="nextMonth">›</button>
      </div>
      <div class="calendar-body">
        <div class="month">
          <div class="weekdays">
            <div>日</div><div>一</div><div>二</div><div>三</div><div>四</div><div>五</div><div>六</div>
          </div>
          <div class="days">
            <div 
              v-for="day in days" 
              :key="day.date"
              :class="{ 
                'selected': isSelected(day.date),
                'active': isInRange(day.date)
              }"
              @click="selectDate(day.date)"
            >
              {{ day.day }}
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import dayjs from 'dayjs'

const props = defineProps({
  modelValue: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['update:modelValue'])

const showCalendar = ref(false)
const currentMonth = ref(dayjs())
const startDate = computed(() => props.modelValue[0] || '')
const endDate = computed(() => props.modelValue[1] || '')

const days = computed(() => {
  const start = currentMonth.value.startOf('month').startOf('week')
  const end = currentMonth.value.endOf('month').endOf('week')
  const days = []
  
  for (let date = start; date.isBefore(end) || date.isSame(end); date = date.add(1, 'day')) {
    days.push({
      date: date.format('YYYY-MM-DD'),
      day: date.date(),
      isCurrentMonth: date.month() === currentMonth.value.month()
    })
  }
  
  return days
})

const isSelected = (date) => {
  return props.modelValue.includes(date)
}

const isInRange = (date) => {
  if (!startDate.value || !endDate.value) return false
  return date >= startDate.value && date <= endDate.value
}

const selectDate = (date) => {
  let newRange = [...props.modelValue]
  
  if (newRange.length === 0) {
    newRange = [date, '']
  } else if (newRange.length === 1 || newRange[1] === '') {
    if (date < newRange[0]) {
      newRange = [date, newRange[0]]
    } else {
      newRange[1] = date
    }
    showCalendar.value = false
  } else {
    newRange = [date, '']
  }
  
  emit('update:modelValue', newRange)
}

const openCalendar = () => {
  showCalendar.value = true
}

const prevMonth = () => {
  currentMonth.value = currentMonth.value.subtract(1, 'month')
}

const nextMonth = () => {
  currentMonth.value = currentMonth.value.add(1, 'month')
}

onMounted(() => {
  document.addEventListener('click', (e) => {
    if (!e.target.closest('.date-range-picker')) {
      showCalendar.value = false
    }
  })
})
</script>

<style scoped>
 @import '@/assets/css/dateRangePicker.css'
</style>