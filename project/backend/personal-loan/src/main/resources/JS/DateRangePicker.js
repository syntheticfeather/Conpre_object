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

        // 渲染初始日历
        this.renderCalendar();
    }

    createCalendarStructure() {
        this.calendar.id = 'calendar';
        this.calendar.classList.add('calendar');

        const calendarHeader = document.createElement('div');
        calendarHeader.classList.add('calendar-header');
        this.prevMonthBtn.textContent = '<<';
        this.nextMonthBtn.textContent = '>>';
        this.monthYear.textContent = `${this.currentYear}年${this.currentMonth + 1}月`;
        calendarHeader.appendChild(this.prevMonthBtn);
        calendarHeader.appendChild(this.monthYear);
        calendarHeader.appendChild(this.nextMonthBtn);

        const calendarBody = document.createElement('div');
        calendarBody.classList.add('calendar-body');
        for (let i = 0; i < 2; i++) {
            const monthContainer = document.createElement('div');
            monthContainer.classList.add('month');
            const weekdays = document.createElement('div');
            weekdays.classList.add('weekdays');
            weekdays.textContent = '日 一 二 三 四 五 六';
            const daysContainer = document.createElement('div');
            daysContainer.classList.add('days');
            monthContainer.appendChild(weekdays);
            monthContainer.appendChild(daysContainer);
            calendarBody.appendChild(monthContainer);
        }
        this.calendar.appendChild(calendarHeader);
        this.calendar.appendChild(calendarBody);

        document.body.appendChild(this.calendar);
    }

    attachEventListeners() {
        this.startDateInput.addEventListener('click', () => this.toggleCalendar());
        this.endDateInput.addEventListener('click', () => this.toggleCalendar());
        this.prevMonthBtn.addEventListener('click', () => this.changeMonth(-2));
        this.nextMonthBtn.addEventListener('click', () => this.changeMonth(2));
        document.addEventListener('click', (e) => {
            if (!this.calendar.contains(e.target) && e.target !== this.startDateInput && e.target !== this.endDateInput) {
                this.calendar.style.display = 'none';
            }
        });
    }

    renderCalendar() {
        const daysContainers = this.calendar.querySelectorAll('.days');
        const months = [this.currentMonth, this.currentMonth + 1];

        daysContainers.forEach((container, index) => {
            const month = months[index];
            const year = this.currentYear;
            const firstDay = new Date(year, month, 1);
            const lastDay = new Date(year, month + 1, 0);

            let day = 1;
            let date = new Date(firstDay);

            container.innerHTML = '';

            while (date.getDay() !== 0) {
                container.innerHTML += '<div></div>';
                date.setDate(date.getDate() - 1);
            }

            while (day <= lastDay.getDate()) {
                const cell = document.createElement('div');
                cell.textContent = day;
                cell.dataset.date = `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
                cell.classList.add('day');
                if (this.startSelected === cell.dataset.date || this.endSelected === cell.dataset.date) {
                    cell.classList.add('selected');
                }
                cell.addEventListener('click', () => this.selectDate(cell.dataset.date));
                container.appendChild(cell);
                day++;
            }
        });

        this.monthYear.textContent = `${this.currentYear}年${months[0] + 1}月`;
    }

    selectDate(date) {
        if (!this.startSelected) {
            this.startSelected = date;
        } else if (!this.endSelected) {
            this.endSelected = date;
        } else {
            this.startSelected = date;
            this.endSelected = null;
            this.resetSelection();
        }
        this.updateInputs();
        this.toggleCalendar();
    }

    updateInputs() {
        if (this.startSelected && this.endSelected) {
            this.startDateInput.value = this.startSelected;
            this.endDateInput.value = this.endSelected;
        } else if (this.startSelected) {
            this.startDateInput.value = this.startSelected;
            this.endDateInput.value = '';
        }
    }

    toggleCalendar() {
        this.calendar.style.display = this.calendar.style.display === 'block' ? 'none' : 'block';
    }

    changeMonth(offset) {
        this.currentMonth += offset;
        if (this.currentMonth < 0) {
            this.currentMonth += 12;
            this.currentYear--;
        }
        if (this.currentMonth >= 12) {
            this.currentMonth -= 12;
            this.currentYear++;
        }
        this.renderCalendar();
    }

    resetSelection() {
        document.querySelectorAll('.day').forEach(d => d.classList.remove('selected'));
    }
}

// 导出模块（如果在Node环境中使用）
if (typeof module !== 'undefined' && typeof module.exports !== 'undefined') {
    module.exports = DateRangePicker;
}