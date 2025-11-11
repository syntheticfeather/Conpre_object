import { API_CONFIG, JWT_CONFIG, DOM_ELEMENTS, API_CLIENT, JWT_UTILS } from './API.js'

// ==================== 初始化函数 ====================
function init() {
    console.log('开始初始化...')
    
    // 检查登录状态?
    // checkLoginStatus()

    // 显示首页面板
    DOM_ELEMENTS.homePageContent.style.display = 'flex'
    
    // 绑定事件监听
    bindEventListeners()
    
    // 初始化所有数据
    updateData()
    // updateLoanApplications()
    
    // 强制图表重新渲染,待优化
    setTimeout(function() {
        if (pieChart) pieChart.resize();
        if (lineChart1) lineChart1.resize();
        if (lineChart2) lineChart2.resize();
    }, 200);
    
    console.log('初始化完成')
}

// ==================== 事件绑定函数 ====================
function bindEventListeners() {
    // 导航菜单切换
    document.querySelectorAll('.nav-link').forEach(link => {
        link.addEventListener('click', function(e) {
        e.preventDefault()
        
        // 移除所有活动状态
        document.querySelectorAll('.nav-link').forEach(item => {
            item.classList.remove('active');
        })
        
        // 添加当前活动状态
        this.classList.add('active')
        
        // 更新页面标题
        const target = this.getAttribute('data-target')
        document.querySelector('.header h2').textContent = this.querySelector('span').textContent
        
        // 隐藏所有面板
        document.querySelectorAll('.home-dashboard, .loan-dashboard, .user-dashboard, .risk-dashboard, .data-dashboard').forEach(panel => {
            panel.style.display = 'none';
        });
        
        // 显示对应面板
        switch (target) {
            case 'home-page':
            document.getElementById('home-page-content').style.display = 'flex';
            console.log('切换到首页');
            break;
            case 'loan-management':
            document.getElementById('loan-management-content').style.display = 'flex';
            console.log('切换到贷款管理');
            break;
            case 'user-management':
            document.getElementById('user-management-content').style.display = 'flex';
            console.log('切换到用户管理');
            break;
            case 'riskAndCollection-management':
            document.getElementById('riskAndCollection-management-content').style.display = 'grid';
            console.log('切换到风险与催收管理');
            break;
            case 'dataAndSystem-management':
            document.getElementById('dataAndSystem-management-content').style.display = 'grid';
            console.log('切换到数据统计与系统管理');
            break;
            default:
            break;
        }
        })
    });
    // 弹窗控制
    document.querySelectorAll('[data-modal]').forEach(button => {
        button.addEventListener('click', function() {
            const modalId = this.getAttribute('data-modal')
            document.getElementById(modalId).style.display = 'flex'
        })
    })

    // 关闭弹窗
    document.querySelectorAll('.close-btn').forEach(button => {
        button.addEventListener('click', function() {
            this.closest('.modal').style.display = 'none'
        })
    })

    // 点击弹窗外部关闭
    window.addEventListener('click', function(e) {
        if(e.target.classList.contains('modal')) {
            e.target.style.display = 'none'
        }
    })

    // 为代办事项添加点击事件
    document.querySelectorAll('.task-list button').forEach(button => {
        button.addEventListener('click', function() {
            const taskName = this.querySelector('span').textContent
            alert(`跳转到${taskName}页面`)
            // 实际项目中这里可以跳转到对应功能页面
        })
    })

    // 退出登录
    document.getElementById('logout-btn').addEventListener('click', async function() {
        if(confirm('确定要退出登录吗？')) {
            try {
                // 调用后端退出接口
                await API_CLIENT.post(API_CONFIG.endpoints.logout);
            } catch (error) {
                console.error('退出登录失败:', error);
            } finally {
                // 清除所有token和登录状态
                JWT_UTILS.clearTokens();
                // 跳转到登录页
                window.location.href = 'login.html';
            }
        }
    })

}

// ==================== 首页面板数据更新 ====================
// ---------- 饼图 ----------
const pieDom = document.getElementById('pie-chart')
const pieChart = echarts.init(pieDom, null, {
  width: 450, // 强制饼图canvas宽度
  height: 225 // 强制饼图canvas高度
})
const pieOption = {
    title: { text: '' },
    series: [
    {
        type: 'pie',
        data: [
        { name: '等级A', value: 2500 },
        { name: '等级B', value: 2800 },
        { name: '等级C', value: 3000 },
        { name: '等级D', value: 1100 }
        ]
    }
    ]
}
pieChart.setOption(pieOption)

// ---------- 月度交易次数折线图 ----------
const lineDom1 = document.getElementById('line-chart-1')
const lineChart1 = echarts.init(lineDom1)
const lineOption1 = {
    title: { text: '月度交易次数趋势（折线图）' },
    legend: { data: ['交易次数'] }, // 
    xAxis: { 
        type: 'category', 
        data: ['1-3号', '4-6号', '7-9号', '10-12号', '13-15号', '16-18号', '19-21号', '22-24号', '25-27号', '28-30号'], // 修复笔误：22-14号 → 22-24号
        axisLabel: { interval: 0, rotate: 30 } // 
    },
    yAxis: { type: 'value', name: '交易次数' }, // 补充y轴名称
    series: [{ 
        name: '交易次数', 
        type: 'line', 
        data: [7000, 6000, 3700, 5000, 7600, 9000, 5900, 7500, 3500, 5500],
        smooth: true, 
        lineStyle: { width: 3 },  
        itemStyle: { color: '#1890ff' }
    }]
}
lineChart1.setOption(lineOption1)

// ---------- 月度贷款与还款总额折线图 ----------
const lineDom2 = document.getElementById('line-chart-2')
const lineChart2 = echarts.init(lineDom2)
const lineOption2 = {
    title: { text: '月度贷款与还款总额趋势（折线图）' },
    legend: { data: ['贷款总额', '还款总额'] }, 
    xAxis: { 
        type: 'category', 
        data: ['1-3号', '4-6号', '7-9号', '10-12号', '13-15号', '16-18号', '19-21号', '22-24号', '25-27号', '28-30号'], // 修复笔误+统一x轴数据长度
        axisLabel: { interval: 0, rotate: 30 }
    },
    yAxis: { type: 'value', name: '金额（元）' }, 
    series: [
        { 
            name: '贷款总额',
            type: 'line', 
            data: [7000, 6000, 3700, 5000, 7600, 9000, 5900, 7500, 3500, 5500],
            smooth: true,
            lineStyle: { width: 3 },
            itemStyle: { color: '#ff4d4f' }, 
            areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(255,77,79,0.3)' }, { offset: 1, color: 'rgba(255,77,79,0)' }]) }
        },
        { 
            name: '还款总额', 
            type: 'line', 
            data: [5000, 4500, 2800, 3800, 6000, 7200, 4800, 6200, 2700, 4200], 
            smooth: true,
            lineStyle: { width: 3 },
            itemStyle: { color: '#52c41a' }, 
            areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(82,196,26,0.3)' }, { offset: 1, color: 'rgba(82,196,26,0)' }]) }
        }
    ]
};
lineChart2.setOption(lineOption2)

// 实时更新数据的函数
function updateData() {
    // -------------- 首页数据更新 ----------
    // 更新用户概览数据
    const newTotal = Math.floor(Math.random() * 5000) + 2000; // 总用户数随机
    const newNewUser = Math.floor(Math.random() * 200); // 新增用户随机
    const newOnline = Math.floor(Math.random() * 2000) + 1000; // 在线用户随机
    const newVisitor = Math.floor(Math.random() * 500); // 游客数量随机
    // 修改文本内容
    // totalUserLi.textContent = `总用户数:${newTotal}`;
    // newUserLi.textContent = `新增用户:${newNewUser}`;
    // onlineUserLi.textContent = `在线用户:${newOnline}`;
    // visitorLi.textContent = `游客数量:${newVisitor}`;

    // 用户等级分布饼图数据更新
    // 生成随机数据
    const randomPieData = [
    { name: '等级A', value: Math.floor(Math.random() * 3000) + 1000 },
    { name: '等级B', value: Math.floor(Math.random() * 2500) + 1000 },
    { name: '等级C', value: Math.floor(Math.random() * 2000) + 1000 },
    { name: '等级D', value: Math.floor(Math.random() * 1500) + 1000 }
    ]

    // 1. 生成折线图1（交易次数）的随机数据（
    const randomLine1Data = lineOption1.series[0].data.map(() => Math.floor(Math.random() * 6000) + 3000);
    // 2. 生成折线图2（贷款总额+还款总额）的随机数据
    const randomLoanData = lineOption2.series[0].data.map(() => Math.floor(Math.random() * 6000) + 3000);
    const randomRepayData = randomLoanData.map(num => Math.floor(num * 0.7) + 1000); // 还款总额 = 贷款总额的70% + 基础值
    
    // 更新饼图数据
    pieOption.series[0].data = randomPieData;
    pieChart.setOption(pieOption);
    // 更新折线图1数据
    lineOption1.series[0].data = randomLine1Data;
    lineChart1.setOption(lineOption1);
    
    // 更新折线图2数据
    lineOption2.series[0].data = randomLoanData; // 贷款总额
    lineOption2.series[1].data = randomRepayData; // 还款总额
    lineChart2.setOption(lineOption2);
}

// 添加动态申请列表功能
// function updateLoanApplications() {
//     const applications = [
//         { time: '2024-01-15 10:30', user: '张先生', amount: '50,000' },
//         { time: '2024-01-15 09:15', user: '李女士', amount: '30,000' },
//         // ...更多模拟数据
//     ];
    
//     const listContainer = document.querySelector('.application-list');
//     listContainer.innerHTML = '';
    
//     applications.forEach(app => {
//         const li = document.createElement('li');
//         li.className = 'feature-item';
//         li.innerHTML = `
//             <span>${app.time}</span>
//             <span>${app.user}</span>
//             <span>¥${app.amount}</span>
//             <button class="btn btn-primary">查看详情</button>
//         `;
//         listContainer.appendChild(li);
//     });
// }

// 窗口大小变化时，图表自适应
window.addEventListener('resize', () => {
    lineChart1.resize();
    lineChart2.resize();
    pieChart.resize(); 
    // barChart.resize();
})

// 页面加载完成后初始化
document.addEventListener('DOMContentLoaded', function() {
    init()
})
