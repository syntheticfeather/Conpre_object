const API_CONFIG = AdminWeb.API_CONFIG
const JWT_CONFIG = AdminWeb.JWT_CONFIG
const DOM_ELEMENTS = AdminWeb.DOM_ELEMENTS
const API_CLIENT = AdminWeb.API_CLIENT
const JWT_UTILS = AdminWeb.JWT_UTILS

// ==================== 初始化函数 ====================
async function init() {
    console.log('开始初始化...')
    try {
        // 检查令牌时效
        // checkLoginStatus()

        // 默认显示待办审核面板
        switchToPanel('loan-apply')
        
        // 绑定事件监听
        bindEventListeners()
        
        // 初始化图表
        initCharts()
        
        // 初始化所有数据
        // updateData()
        // 加载待审核列表
        await loadPendingApplications(1)

        console.log('初始化完成')
    } catch (error) {
        console.error('初始化失败:', error)
        alert('页面初始化失败，请刷新页面重试')
    }
}

// 初始化图表（单独函数，便于错误处理）
function initCharts() {
    try {
        // 检查echarts是否加载
        if (typeof echarts === 'undefined') {
            console.warn('echarts未加载，跳过图表初始化');
            return;
        }
        
        // 饼图初始化
        const pieDom = document.getElementById('pie-chart');
        if (pieDom) {
            const pieChart = echarts.init(pieDom);
            const pieOption = {
                title: { text: '用户等级分布' },
                series: [{
                    type: 'pie',
                    data: [
                        { name: '等级A', value: 2500 },
                        { name: '等级B', value: 2800 },
                        { name: '等级C', value: 3000 },
                        { name: '等级D', value: 1100 }
                    ]
                }]
            };
            pieChart.setOption(pieOption);
            window.pieChart = pieChart; // 保存到全局以便后续使用
        }
        
        // 其他图表初始化...
        
    } catch (error) {
        console.error('图表初始化失败:', error);
    }
}


// ==================== 事件绑定函数 ====================
function bindEventListeners() {
    // 导航菜单切换
    console.log('绑定事件监听器...')

    // 顶部导航菜单切换 - 使用事件委托
    document.querySelector('.nav-menu').addEventListener('click', function(e) {
        if (e.target.closest('.nav-link')) {
            const link = e.target.closest('.nav-link')
            const target = link.getAttribute('data-target')
            if (target) {
                switchToPanel(target)
            }
        }
    })

    // 侧边栏导航切换
    document.querySelectorAll('.side-link').forEach(button => {
        button.addEventListener('click', function() {
            const target = this.getAttribute('data-target')
            if (target) {
                switchToPanel(target)
            }
        })
    })

    // 弹窗控制
    document.querySelectorAll('[data-modal]').forEach(button => {
        button.addEventListener('click', function() {
            const modalId = this.getAttribute('data-modal')
            document.getElementById(modalId).style.display = 'flex'
        })
    })

    // 关闭弹窗函数
    function closeModal(modal) {
        modal.style.display = 'none'
    }

    // 点击关闭按钮关闭弹窗
    document.querySelectorAll('.close-btn').forEach(button => {
        button.addEventListener('click', function() {
            const modal = this.closest('.modal') // 找到最近的外层.modal
            closeModal(modal)
        })
    })
        
    // 为代办事项添加点击事件-待完善
    document.querySelectorAll('.task-list button').forEach(button => {
        button.addEventListener('click', function() {
            const taskName = this.querySelector('span').textContent
            alert(`跳转到${taskName}页面`)
            // 实际项目中这里可以跳转到对应功能页面
        })
    })

    // 退出登录-待完善
    document.getElementById('logout-btn').addEventListener('click', async function() {
        if(confirm('确定要退出登录吗？')) {
            try {
                // 调用后端退出接口
                await API_CLIENT.post(API_CONFIG.endpoints.logout)
            } catch (error) {
                console.error('退出登录失败:', error)
            } finally {
                // 清除所有token和登录状态
                JWT_UTILS.clearTokens()
                // 跳转到登录页
                window.location.href = 'login.html'
            }
        }
    })
}

// 显示面板
function switchToPanel(target) {
    console.log('切换到面板:', target)
    
// 隐藏所有面板
    document.querySelectorAll('.dashboard').forEach(panel => {
        panel.style.display = 'none'
        panel.classList.remove('active') // 移除active类
    })
    
    // 更新导航激活状态
    document.querySelectorAll('.nav-link').forEach(link => {
        link.classList.remove('active')
    })
    
    // 激活当前导航按钮
    const activeNav = document.querySelector(`.nav-link[data-target="${target}"]`)
    if (activeNav) {
        activeNav.classList.add('active')
    }
    
    // 显示目标面板
    const targetPanel = document.getElementById(`${target}-content`)
    if (targetPanel) {
        // 根据面板类型设置显示方式
        let displayStyle = 'flex'
        
        // 特殊面板类型处理
        if (target === 'riskAndCollection-management' || target === 'dataAndSystem-management') {
            displayStyle = 'grid'
        }
        
        targetPanel.style.display = displayStyle
        targetPanel.classList.add('active')
        console.log(`成功显示 ${target} 面板`)
        
        // 面板显示后调整图表大小
        setTimeout(() => {
            resizeCharts()
        }, 100)
    } else {
        console.error('未找到目标面板:', `${target}-content`)
        // 调试信息：列出所有可用的面板
        const allPanels = document.querySelectorAll('.dashboard')
        console.log('可用面板:', Array.from(allPanels).map(panel => panel.id))
    }
}

// 图表调整大小函数
function resizeCharts() {
    if (window.pieChart) window.pieChart.resize()
    if (window.lineChart1) window.lineChart1.resize()
    if (window.lineChart2) window.lineChart2.resize()
}


/*
*==================== 待办审核面板处理 ====================
*/ 
// ============== 面板初始化 ===============
// 加载待审核申请列表
async function loadPendingApplications(page) {
    try {
        const response = await AdminWeb.API_CLIENT.getPendingApplications(page, 10)
        if (response.code === 200 && response.data) {
            renderApplicationTable(response.data.records)
        }
    } catch (error) {
        console.error('加载待审核列表失败:', error)
        document.getElementById('apply-table-body').innerHTML = '<tr><td colspan="6">加载失败</td></tr>'
    }
}
// 渲染申请列表（只显示关键字段）
function renderApplicationTable(applications) {
    const tbody = document.getElementById('apply-table-body')
    tbody.innerHTML = ''

    applications.forEach(app => {
        const row = document.createElement('tr')
        row.setAttribute('data-application-id', app.id) // 关键：绑定申请ID
        row.innerHTML = `
            <td>${app.userName || '未知'}</td>
            <td>${app.productName || '未命名产品'}</td>
            <td>¥${Number(app.loanAmount).toLocaleString()}</td>
            <td>${app.loanPeriod || 0}</td>
            <td>${app.term || 0}</td>
            <td>${new Date(app.applyTime).toLocaleString()}</td>
        `
        tbody.appendChild(row)

        // 绑定点击事件
        row.addEventListener('click', () => showApplicationDetail(app.id));
    })
}
// 显示申请详情
async function showApplicationDetail(applicationId) {
    try {
        const detail = await AdminWeb.API_CLIENT.getApplicationDetail(applicationId)
        if (detail.code !== 200) throw new Error(detail.message || '获取详情失败')

        const data = detail.data;

        // 显示详情容器
        document.getElementById('audition-detail').style.display = 'block';

        // 填充用户信息
        document.getElementById('real-name').textContent = data.user.realName || '—';
        document.getElementById('phone').textContent = data.user.phoneNumber || '—';
        document.getElementById('register-time').textContent = 
            new Date(data.user.registerTime).toLocaleString() || '—';
        document.getElementById('credit-score').textContent = data.user.creditScore || '—';

        // 渲染认证材料
        const materialsContainer = document.getElementById('materials-container');
        const materialMap = {
            bankCard: '银行卡',
            workProof: '工作证明',
            thirdPartyAuth: '三方认证',
            propertyCert: '不动产认证'
        };
        let html = '';
        for (const key in data.materials) {
            const uploaded = data.materials[key];
            const label = materialMap[key] || key;
            const color = uploaded ? '#27ae60' : '#e74c3c';
            html += `<div class="material-item"><span>${label}</span><span style="color:${color}">${uploaded ? '已上传' : '未上传'}</span></div>`;
        }
        materialsContainer.innerHTML = html;

        // 填充贷款信息
        document.getElementById('product-name').textContent = data.productName;
        document.getElementById('loan-amount').textContent = `¥${Number(data.loanAmount).toLocaleString()}`;
        document.getElementById('loan-term').textContent = data.loanPeriod;
        document.getElementById('term-period').textContent = data.term;

        // 绑定按钮
        document.getElementById('btn-pass').onclick = () => submitReview(applicationId, 'APPROVED')
        document.getElementById('btn-reject').onclick = () => submitReview(applicationId, 'REJECTED')

    } catch (error) {
        console.error('获取申请详情失败:', error)
        alert('获取详情失败：' + (error.message || '请重试'))
    }
}
function showDetail(id) {
  const data = mockData[id]
  if (!data) return

  // 显示详情容器
  document.getElementById('audition-detail').style.display = 'block'

  // 填充用户基本信息
  document.getElementById('real-name').textContent = data.realName
  document.getElementById('phone').textContent = data.phone
  document.getElementById('register-time').textContent = data.registerTime
  document.getElementById('credit-score').textContent = data.creditScore

  // 渲染认证材料
  const materialsContainer = document.getElementById('materials-container')
  const materialMap = {
    bankCard: '银行卡',
    workProof: '工作证明',
    thirdPartyAuth: '三方认证',
    propertyCert: '不动产认证'
  }

  let materialsHtml = ''
  for (const key in data.materials) {
    const uploaded = data.materials[key]
    const label = materialMap[key] || key
    const statusText = uploaded ? '已上传' : '未上传'
    const statusColor = uploaded ? '#27ae60' : '#e74c3c'

    materialsHtml += `
      <div class="material-item">
        <span>${label}</span>
        <span style="color: ${statusColor}; font-weight: bold;">${statusText}</span>
      </div>
    `
  }
  materialsContainer.innerHTML = materialsHtml

  // 填充贷款申请信息
  const app = data.loanApplication
  document.getElementById('product-name').textContent = app.productName;
  document.getElementById('loan-amount').textContent = app.loanAmount;
  document.getElementById('loan-term').textContent = app.loanTerm;
  document.getElementById('term-period').textContent = app.termPeriod;

  // 绑定按钮事件（可选）
  document.getElementById('btn-pass').onclick = () => handleReview(id, 'APPROVED')
  document.getElementById('btn-reject').onclick = () => handleReview(id, 'REJECTED')
}
// 提交审核结果
async function submitReview(applicationId, status) {
    let rejectReason = null
    if (status === 'REJECTED') {
        rejectReason = prompt('请输入拒绝理由：')
        if (!rejectReason) return
    }

    try {
        await AdminWeb.API_CLIENT.submitReview(applicationId, status, rejectReason)
        alert(`审核成功！状态：${status === 'APPROVED' ? '通过' : '拒绝'}`)
        // 刷新列表
        await loadPendingApplications(1)
        // 隐藏详情
        document.getElementById('audition-detail').style.display = 'none'
    } catch (error) {
        console.error('提交审核失败:', error)
        alert('提交失败：' + (error.message || '请重试'))
    }
}

// ====================== 贷款申请处理面板 =====================
// 通过申请ID获取申请详情
async function fetchApplicationById(applicationId) {
    const url = `/api/loan-applications/${applicationId}`;
    console.log(`📡 [GET] 请求申请详情: ${url}`);
    try {
        const response = await AdminWeb.API_CLIENT.get(url);
        console.log(`✅ [响应] 申请 ${applicationId} 详情:`, response);
        return response.data;
    } catch (error) {
        console.error(`❌ [错误] 获取申请 ${applicationId} 失败:`, error);
        alert('申请详情加载失败');
    }
}
// 通过用户ID获取用户所有申请
async function fetchApplicationsByUser(userId) {
    const url = `/api/loan-applications/user/${userId}`;
    console.log(`📡 [GET] 请求用户所有申请: ${url}`);
    try {
        const response = await AdminWeb.API_CLIENT.get(url);
        console.log(`✅ [响应] 用户 ${userId} 的所有申请:`, response);
        return response.data;
    } catch (error) {
        console.error(`❌ [错误] 获取用户 ${userId} 的申请失败:`, error);
        alert('申请记录加载失败');
    }
}

/*
*==================== 贷款项目管理面板处理 ====================
*/ 
// ============== 添加贷款项目弹窗处理 ===============
// 数据输入表格的行增减处理
const addBtn = document.getElementById('add-row-btn')
const table = document.getElementById('option-table')
// 给添加按钮绑定事件
addBtn.addEventListener('click', function () {
  const tbody = table.querySelector('tbody')
  const newRow = document.createElement('tr')

  const inputs = [
    '请输入贷款额度',
    '请输入贷款期限',
    '请输入年化利率',
    '请输入还款方式'
  ]

  inputs.forEach(placeholder => {
    const td = document.createElement('td')
    const input = document.createElement('input')
    input.type = 'text'
    input.placeholder = placeholder
    td.appendChild(input)
    newRow.appendChild(td)
  })

  // 添加删除按钮
  const deleteTd = document.createElement('td')
  const deleteBtn = document.createElement('button')
  deleteBtn.textContent = '删除'
  deleteBtn.classList.add('delete-btn')
  deleteBtn.addEventListener('click', function () {
    newRow.remove() // 删除当前行
  })
  deleteTd.appendChild(deleteBtn)
  newRow.appendChild(deleteTd)
  tbody.appendChild(newRow)
})
// 给已有删除按钮绑定事件
document.querySelectorAll('.delete-btn').forEach(btn => {
  btn.addEventListener('click', function () {
    this.closest('tr').remove()
  })
})
// ============= 添加贷款项目功能实现函数 =============
// 获取弹窗中的表单数据
function handleNewLoanProductData() {
    // 获取基础信息
    const productName = document.getElementById('productName').value.trim()
    const description = document.getElementById('description').value.trim()
    const loanUsage = document.getElementById('loanUsage').value.trim()
    const minTerm = parseInt(document.getElementById('minTerm').value) || 0
    const maxTerm = parseInt(document.getElementById('maxTerm').value) || 0
    const termStep = parseInt(document.getElementById('termStep').value) || 0
    const promotionDetails = document.getElementById('promotionDetails').value.trim()

    // 获取选项表格数据
    const options = []
    const tableRows = document.querySelectorAll('#option-table tbody tr')
    
    tableRows.forEach(row => {
        const inputs = row.querySelectorAll('input[type="text"]')
        if (inputs.length >= 4) { // 确保有四个输入框
            const option = {
                loanAmount: parseFloat(inputs[0].value) || 0,
                interestRate: parseFloat(inputs[1].value) || 0,
                loanPeriod: parseInt(inputs[2].value) || 0,
                repaidType: inputs[3].value.trim()
            }
            options.push(option)
        }
    })
    // 构建完整的请求数据
    const productData = {
        productName,
        description,
        loanUsage,
        minTerm,
        maxTerm,
        termStep,
        promotionDetails,
        options
    }
    return productData
}
//表单提交按钮事件绑定
document.getElementById('add-loan-product').addEventListener('click', async function() {
    try {
        // 验证必填字段
        const productName = document.getElementById('productName').value.trim()
        if (!productName) {
            alert('请输入产品名称')
            return
        }
        const options = document.querySelectorAll('#option-table tbody tr')
        if (options.length === 0) {
            alert('至少需要添加一个贷款选项')
            return
        }
        // 获取并验证数据
        const productData = handleNewLoanProductData()
        // 调用API客户端提交数据
        const response = await API_CLIENT.addLoanProduct(productData)
        console.log('新增贷款产品请求数据:', productData)
        alert('新增贷款产品请求数据:', productData)
        console.log('新增贷款产品成功:', response)
        alert('贷款产品添加成功！')
        // 关闭弹窗
        document.getElementById('add-new-product').style.display = 'none'
        // 重置表单
        resetAddLoanProductForm()
    } catch (error) {
        console.error('新增贷款产品失败:', error)
        alert('添加失败，请检查输入信息或稍后重试')
    }
})
// 提交后重置添加贷款产品表单函数 
function resetAddLoanProductForm() {
    // 重置基础输入框
    document.getElementById('productName').value = ''
    document.getElementById('description').value = ''
    document.getElementById('loanUsage').value = ''
    document.getElementById('minTerm').value = ''
    document.getElementById('maxTerm').value = ''
    document.getElementById('termStep').value = ''
    document.getElementById('promotionDetails').value = ''
    
    // 重置表格，只保留初始行
    const tbody = document.querySelector('#option-table tbody')
    tbody.innerHTML = `
        <tr>  
            <td><input type="text" placeholder="请输入贷款额度"></td>
            <td><input type="text" placeholder="请输入贷款期限"></td>
            <td><input type="text" placeholder="请输入年化利率"></td>
            <td><input type="text" placeholder="请输入还款方式"></td>
            <td><button class="delete-btn">删除</button></td>
        </tr>
    `
    // 重新绑定删除按钮事件
    document.querySelectorAll('.delete-btn').forEach(btn => {
        btn.addEventListener('click', function() {
            this.closest('tr').remove()
        })
    })
}

// // 获取所有贷款产品
// async function fetchAllLoanProducts() {
//     const url = '/api/loan-products/admin/';
//     console.log(`📡 [GET] 请求所有贷款产品: ${url}`);
//     try {
//         const response = await AdminWeb.API_CLIENT.get(url);
//         console.log(`✅ [响应] 所有贷款产品:`, response);
//         return response.data;
//     } catch (error) {
//         console.error(`❌ [错误] 获取产品列表失败:`, error);
//         alert('加载产品失败');
//     }
// }
// // 获取单个贷款产品详情
// async function fetchLoanProductById(productId) {
//     const url = `/api/loan-products/admin/${productId}`;
//     console.log(`📡 [GET] 请求产品详情: ${url}`);
//     try {
//         const response = await AdminWeb.API_CLIENT.get(url);
//         console.log(`✅ [响应] 产品 ${productId} 详情:`, response);
//         return response.data;
//     } catch (error) {
//         console.error(`❌ [错误] 获取产品 ${productId} 失败:`, error);
//     }
// }
// // 更新单个贷款产品
// async function updateLoanProduct(productId, updateData) {
//     const url = `/api/loan-products/admin/products/${productId}`;
//     console.log(`📡 [PATCH] 更新产品: ${url}`, '请求体:', updateData);
//     try {
//         const response = await AdminWeb.API_CLIENT.request(url, {
//             method: 'PATCH',
//             body: JSON.stringify(updateData)
//         });
//         console.log(`✅ [响应] 产品 ${productId} 更新成功:`, response);
//         alert('产品信息更新成功');
//         return response.data;
//     } catch (error) {
//         console.error(`❌ [错误] 更新产品 ${productId} 失败:`, error);
//         alert('更新失败');
//     }
// }
// // 删除单个贷款产品
// async function deleteLoanProduct(productId) {
//     if (!confirm(`确定删除产品 ID=${productId}？此操作不可逆！`)) return;
//     const url = `/api/loan-products/admin/products/${productId}`;
//     console.log(`📡 [DELETE] 删除产品: ${url}`);
//     try {
//         const response = await AdminWeb.API_CLIENT.request(url, { method: 'DELETE' });
//         console.log(`✅ [响应] 产品 ${productId} 删除成功:`, response);
//         alert('删除成功');
//         return true;
//     } catch (error) {
//         console.error(`❌ [错误] 删除产品 ${productId} 失败:`, error);
//         alert('删除失败');
//     }
// }
// // 批量删除贷款产品
// async function batchDeleteLoanProducts(productIds) {
//     const url = '/api/loan-products/admin/products/batch-delete';
//     const payload = { productIds };
//     console.log(`📡 [POST] 批量删除产品: ${url}`, '请求体:', payload);
//     try {
//         const response = await AdminWeb.API_CLIENT.post(url, payload);
//         console.log(`✅ [响应] 批量删除产品成功:`, response);
//         alert('批量删除成功');
//         return response.data;
//     } catch (error) {
//         console.error(`❌ [错误] 批量删除产品失败:`, error);
//         alert('批量删除失败');
//     }
// }
// // 批量创建产品选项
// async function batchCreateProductOptions(productId, options) {
//     const url = '/api/loan-products/admin/options/batch-create';
//     const payload = { productId, options };
//     console.log(`📡 [POST] 批量添加选项: ${url}`, '请求体:', payload);
//     try {
//         const response = await AdminWeb.API_CLIENT.post(url, payload);
//         console.log(`✅ [响应] 批量添加选项成功:`, response);
//         return response.data;
//     } catch (error) {
//         console.error(`❌ [错误] 批量添加选项失败:`, error);
//         alert('添加选项失败');
//     }
// }
// // 批量更新产品选项
// async function deleteProductOption(optionId) {
//     const url = `/api/loan-products/admin/options/${optionId}`;
//     console.log(`📡 [DELETE] 删除选项: ${url}`);
//     try {
//         const response = await AdminWeb.API_CLIENT.request(url, { method: 'DELETE' });
//         console.log(`✅ [响应] 选项 ${optionId} 删除成功:`, response);
//         return response;
//     } catch (error) {
//         console.error(`❌ [错误] 删除选项 ${optionId} 失败:`, error);
//     }
// }


// // ==================== 用户管理面板处理 ====================

// // 获取用户列表
// async function fetchUserStats() {
//     const url = '/api/users/admin/stats'
//     console.log(`📡 [GET] 请求用户状态列表: ${url}`)
//     try {
//         const response = await AdminWeb.API_CLIENT.get(url)
//         console.log(`✅ [响应] 用户状态列表:`, response)
//         return response.data
//     } catch (error) {
//         console.error(`❌ [错误] 获取用户状态列表失败:`, error)
//         alert('获取用户列表失败')
//     }
// }
// // 通过用户ID获取单个用户详情
// async function fetchUserById(userId) {
//     const url = `/api/users/admin/${userId}`
//     console.log(`📡 [GET] 请求用户详情: ${url}`)
//     try {
//         const response = await AdminWeb.API_CLIENT.get(url)
//         console.log(`✅ [响应] 用户 ${userId} 详情:`, response)
//         return response.data;
//     } catch (error) {
//         console.error(`❌ [错误] 获取用户 ${userId} 失败:`, error)
//         alert('获取用户信息失败');
//     }
// }
// // 根据信用分查询用户 
// // 绑定信誉分查询按钮事件
// document.getElementById('credit-search-btn').addEventListener('click', async function() {
//     console.log(`📡 [GET] 请求按信誉分降序用户列表: ${url}`);
//     const expr = document.getElementById('creditExprInput').value.trim()
//     if (!expr) {
//         alert('请输入信誉分查询表达式，例如：<100 或 >=80')
//         return
//     }
//     try {
//         const users = await API_CLIENT.searchUsersByCredit(expr)
//         console.log(`✅ [响应] 信誉分排序用户列表:`, response)
//         const tbody = document.getElementById('searchResultBody')
//         const container = document.getElementById('searchResultContainer')
//         // 清空旧结果
//         tbody.innerHTML = ''
//         if (users.length === 0) {
//             tbody.innerHTML = `<tr><td colspan="5" style="text-align:center;">未找到符合条件的用户</td></tr>`
//         } else {
//             users.forEach(user => {
//                 const tr = document.createElement('tr')
//                 // 格式化时间
//                 const createTime = new Date(user.createTime).toLocaleString()
//                 tr.innerHTML = `
//                 <td>${user.id}</td>
//                 <td>${user.name}</td>
//                 <td>${user.phone}</td>
//                 <td>${user.creditScore}</td>
//                 <td>${createTime}</td>
//                 `
//                 tbody.appendChild(tr)
//             })
//         }
//         container.style.display = 'block'
//     } catch (error) {
//         console.error('查询用户失败:', error)
//         alert('查询失败：' + (error.message || '请检查表达式格式'))
//         document.getElementById('searchResultContainer').style.display = 'none'
//     }
// })
// // ==================== 待办审核面板处理 ====================
// // 分页实现-待完善
// let currentPage = 1;
// const pageSize = 10;

// // 假设这是你的原始数据
// // const data = [
// //   { id: 1, name: "张三", ... },
// //   { id: 2, name: "李四", ... },
// //   // ... 共 100 条
// // ];

// function renderTable() {
//   const start = (currentPage - 1) * pageSize;
//   const end = start + pageSize;
//   const pageData = data.slice(start, end);

//   const tbody = document.getElementById('table-body');
//   tbody.innerHTML = '';

//   pageData.forEach(item => {
//     const row = document.createElement('tr');
//     row.innerHTML = `
//       <td>${item.id}</td>
//       <td>${item.name}</td>
//       <td>${item.productName}</td>
//       <td>${item.amount}</td>
//       <td>${item.rate}</td>
//       <td>${item.duration}</td>
//       <td>${item.periods}</td>
//       <td>${item.repayType}</td>
//       <td>${item.status}</td>
//       <td>${item.applyTime}</td>
//       <td>
//         <button onclick="viewDetail(${item.id})">查看详情</button>
//       </td>
//     `;
//     tbody.appendChild(row);
//   });
// }

// function nextPage() {
//   if (currentPage < totalPages) {
//     currentPage++;
//     renderTable();
//     updatePaginationInfo();
//   }
// }

// function prevPage() {
//   if (currentPage > 1) {
//     currentPage--;
//     renderTable();
//     updatePaginationInfo();
//   }
// }

// function updatePaginationInfo() {
//   document.getElementById('current-page').textContent = currentPage;
//   document.getElementById('total-pages').textContent = totalPages;
// }


// ==================== 数据统计与系统管理面板处理 ====================
// 饼图
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

// 月度交易次数折线图 
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

// 月度贷款与还款总额折线图
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
}
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

// 窗口大小变化时，图表自适应
window.addEventListener('resize', () => {
    lineChart1.resize();
    lineChart2.resize();
    pieChart.resize(); 
    // barChart.resize();
})



// =========================页面加载完成后初始化=========================
document.addEventListener('DOMContentLoaded', function() {
    init()
    new DateRangePicker('start-date', 'end-date')
    // 在页面加载完成后添加调试信息
    document.addEventListener('DOMContentLoaded', function() {
        console.log('DOM加载完成，开始初始化...')
        
        // 检查所有面板是否存在
        const panels = [
            'loan-apply-content',
            'home-page-content', 
            'loan-management-content',
            'user-management-content',
            'riskAndCollection-management-content',
            'dataAndSystem-management-content'
        ]
        
        panels.forEach(panelId => {
            const panel = document.getElementById(panelId)
            console.log(`面板 ${panelId}:`, panel ? '存在' : '不存在')
        })
        
        // 检查导航按钮
        const navButtons = document.querySelectorAll('.nav-link')
        console.log(`找到 ${navButtons.length} 个导航按钮`)
        
        navButtons.forEach(button => {
            const target = button.getAttribute('data-target')
            console.log(`导航按钮: ${target}`, document.getElementById(`${target}-content`) ? '✓' : '✗')
        })
        
        init()
    })
})