// ==================== 日期范围选择器类 ====================
// 用于贷款管理面板中的日期搜索功能
class DateRangePicker {
    constructor(startDateInput, endDateInput) {
        this.startDateInput = document.getElementById(startDateInput);
        this.endDateInput = document.getElementById(endDateInput);
        this.calendar = document.createElement('div');
        this.prevMonthBtn = document.createElement('button');
        this.nextMonthBtn = document.createElement('button');
        this.monthYear = document.createElement('span');
        this.currentMonth = new Date().getMonth();
        this.currentYear = new Date().getFullYear();
        this.startSelected = null;
        this.endSelected = null;
        this.init();
    }

    init() {
        // 初始化HTML结构
        this.createCalendarStructure();
        this.attachEventListeners();
        // 渲染初始日历（修复：应该渲染连续的两个月）
        this.renderCalendar();
    }

    createCalendarStructure() {
        this.calendar.id = 'calendar';
        this.calendar.classList.add('calendar');
        
        const calendarHeader = document.createElement('div');
        calendarHeader.classList.add('calendar-header');
        this.prevMonthBtn.textContent = '<<';
        this.nextMonthBtn.textContent = '>>';
        this.monthYear.textContent = `${this.currentYear}年${this.currentMonth + 1}月 - ${this.currentYear}年${this.currentMonth + 2 > 12 ? (this.currentMonth + 2 - 12) : (this.currentMonth + 2)}月`;
        calendarHeader.appendChild(this.prevMonthBtn);
        calendarHeader.appendChild(this.monthYear);
        calendarHeader.appendChild(this.nextMonthBtn);
        
        const calendarBody = document.createElement('div');
        calendarBody.classList.add('calendar-body');
        
        // 创建两个连续的月份
        for (let i = 0; i < 2; i++) {
            const monthContainer = document.createElement('div');
            monthContainer.classList.add('month');
            
            const weekdays = document.createElement('div');
            weekdays.classList.add('weekdays');
            weekdays.innerHTML = '<div>日</div><div>一</div><div>二</div><div>三</div><div>四</div><div>五</div><div>六</div>';
            
            const daysContainer = document.createElement('div');
            daysContainer.classList.add('days');
            
            monthContainer.appendChild(weekdays);
            monthContainer.appendChild(daysContainer);
            calendarBody.appendChild(monthContainer);
        }
        
        this.calendar.appendChild(calendarHeader);
        this.calendar.appendChild(calendarBody);
        document.body.appendChild(this.calendar);
        
        // 添加样式
        this.addCalendarStyles();
    }

    addCalendarStyles() {
        const style = document.createElement('style');
        style.textContent = `
            #calendar {
                position: absolute;
                background: white;
                border: 1px solid #ccc;
                border-radius: 8px;
                padding: 10px;
                z-index: 1000;
                display: none;
                width: 500px;
            }
            .calendar-header {
                display: flex;
                justify-content: space-between;
                align-items: center;
                margin-bottom: 10px;
            }
            .calendar-header button {
                background: #409EFF;
                color: white;
                border: none;
                padding: 5px 10px;
                border-radius: 4px;
                cursor: pointer;
            }
            .calendar-body {
                display: flex;
                gap: 20px;
            }
            .month {
                flex: 1;
            }
            .weekdays {
                display: grid;
                grid-template-columns: repeat(7, 1fr);
                text-align: center;
                font-weight: bold;
                margin-bottom: 5px;
            }
            .weekdays div {
                padding: 5px;
            }
            .days {
                display: grid;
                grid-template-columns: repeat(7, 1fr);
                gap: 2px;
            }
            .day {
                text-align: center;
                padding: 5px;
                cursor: pointer;
                border-radius: 4px;
            }
            .day:hover {
                background-color: #f0f0f0;
            }
            .day.selected {
                background-color: #409EFF;
                color: white;
            }
            .day.empty {
                visibility: hidden;
            }
        `;
        document.head.appendChild(style);
    }

    attachEventListeners() {
        this.startDateInput.addEventListener('click', (e) => {
            e.stopPropagation();
            this.toggleCalendar();
        });
        this.endDateInput.addEventListener('click', (e) => {
            e.stopPropagation();
            this.toggleCalendar();
        });
        this.prevMonthBtn.addEventListener('click', () => this.changeMonth(-1));
        this.nextMonthBtn.addEventListener('click', () => this.changeMonth(1));
        
        document.addEventListener('click', (e) => {
            if (!this.calendar.contains(e.target) && 
                e.target !== this.startDateInput && 
                e.target !== this.endDateInput) {
                this.calendar.style.display = 'none';
            }
        });
    }

    renderCalendar() {
        const daysContainers = this.calendar.querySelectorAll('.days');
        const months = [this.currentMonth, this.currentMonth + 1];
        const years = [
            this.currentYear,
            this.currentMonth + 1 >= 12 ? this.currentYear + 1 : this.currentYear
        ];

        daysContainers.forEach((container, index) => {
            const month = months[index] % 12;
            const year = years[index];
            
            container.innerHTML = '';
            
            // 获取该月第一天是星期几
            const firstDay = new Date(year, month, 1);
            const startDayOfWeek = firstDay.getDay();
            
            // 获取该月最后一天
            const lastDay = new Date(year, month + 1, 0).getDate();
            
            // 添加空白格子（上个月的日期）
            for (let i = 0; i < startDayOfWeek; i++) {
                const emptyCell = document.createElement('div');
                emptyCell.classList.add('empty');
                container.appendChild(emptyCell);
            }
            
            // 添加当前月的日期
            for (let day = 1; day <= lastDay; day++) {
                const cell = document.createElement('div');
                cell.textContent = day;
                cell.classList.add('day');
                cell.dataset.date = `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
                
                if (this.startSelected === cell.dataset.date || this.endSelected === cell.dataset.date) {
                    cell.classList.add('selected');
                }
                
                cell.addEventListener('click', () => this.selectDate(cell.dataset.date));
                container.appendChild(cell);
            }
        });
        
        // 更新标题显示
        const nextMonth = this.currentMonth + 1;
        const nextYear = nextMonth >= 12 ? this.currentYear + 1 : this.currentYear;
        const displayNextMonth = nextMonth % 12 + 1;
        this.monthYear.textContent = `${this.currentYear}年${this.currentMonth + 1}月 - ${nextYear}年${displayNextMonth}月`;
    }

    selectDate(date) {
        if (!this.startSelected) {
            this.startSelected = date;
        } else if (!this.endSelected) {
            // 确保结束日期不早于开始日期
            if (new Date(date) >= new Date(this.startSelected)) {
                this.endSelected = date;
            } else {
                this.startSelected = date;
                this.endSelected = null;
            }
        } else {
            // 重新选择范围
            this.startSelected = date;
            this.endSelected = null;
        }
        
        this.updateInputs();
        this.highlightSelectedRange();
        
        // 如果选择了完整的范围，自动关闭日历
        if (this.startSelected && this.endSelected) {
            setTimeout(() => {
                this.calendar.style.display = 'none';
            }, 300);
        }
    }

    highlightSelectedRange() {
        // 移除所有选中状态
        document.querySelectorAll('.day').forEach(d => {
            d.classList.remove('selected');
        });
        
        if (this.startSelected && this.endSelected) {
            const startDate = new Date(this.startSelected);
            const endDate = new Date(this.endSelected);
            
            document.querySelectorAll('.day').forEach(cell => {
                const cellDate = new Date(cell.dataset.date);
                if (cellDate >= startDate && cellDate <= endDate) {
                    cell.classList.add('selected');
                }
            });
        } else if (this.startSelected) {
            document.querySelector(`[data-date="${this.startSelected}"]`)?.classList.add('selected');
        }
    }

    updateInputs() {
        if (this.startSelected) {
            this.startDateInput.value = this.startSelected;
        }
        if (this.endSelected) {
            this.endDateInput.value = this.endSelected;
        }
    }

    toggleCalendar() {
        this.calendar.style.display = this.calendar.style.display === 'block' ? 'none' : 'block';
    }

    changeMonth(offset) {
        this.currentMonth += offset;
        
        if (this.currentMonth < 0) {
            this.currentMonth = 11;
            this.currentYear--;
        } else if (this.currentMonth >= 12) {
            this.currentMonth = 0;
            this.currentYear++;
        }
        
        this.renderCalendar();
    }

    resetSelection() {
        this.startSelected = null;
        this.endSelected = null;
        this.updateInputs();
        document.querySelectorAll('.day').forEach(d => d.classList.remove('selected'));
    }
}

// 导出模块
if (typeof module !== 'undefined' && typeof module.exports !== 'undefined') {
    module.exports = DateRangePicker;
}