const API_CONFIG = AdminWeb.API_CONFIG
const JWT_CONFIG = AdminWeb.JWT_CONFIG
const DOM_ELEMENTS = AdminWeb.DOM_ELEMENTS
const API_CLIENT = AdminWeb.API_CLIENT
const JWT_UTILS = AdminWeb.JWT_UTILS

// ==================== 全局变量 ====================
let _allPendingApps = [] // 待办申请
let _currentPage = 1
const PENDING_PAGE_SIZE = 5
let _allPendedApps = [] // 已办申请
const PENDED_PAGE_SIZE = 5

let userListInstance = null     // 用户列表实例
let blacklistInstance = null    // 黑名单列表实例
let productListInstance = null  // 产品列表实例

// ==================== 初始化函数 ====================
async function init() {
    console.log('开始初始化...')
    try {
        // 检查令牌时效
        // checkLoginStatus()

        // 检查是否有上次保存的面板
        const savedPanel = sessionStorage.getItem('activePanel') || 'loan-apply'
        
        // 默认显示待办审核面板
        switchToPanel(savedPanel)

        // 绑定事件监听
        bindEventListeners()
        
        // 初始化所有数据
        // 加载待审核列表
        await fetchAndRenderPendingList()
        await fetchAndRenderPendedList()

        console.log('初始化完成')
    } catch (error) {
        console.error('初始化失败:', error)
        alert('页面初始化失败，请刷新页面重试')
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
    document.querySelectorAll('.side-item').forEach(item => {
        item.addEventListener('click', function(e) {
            e.preventDefault()
            const target = this.getAttribute('data-target')
            if (target) {
                switchToContent(target)
            }
        })
    })

    // 侧边栏导航展开/收起
    document.querySelectorAll('.side-menu > .side-link').forEach(btn => {
      btn.addEventListener('click', function(e) {
        e.preventDefault()
        const menu = this.closest('.side-menu')
        menu.classList.toggle('expanded')
        // 旋转箭头
        const icon = menu.querySelector('.toggle-icon')
        if (icon) {
          icon.style.transform = menu.classList.contains('expanded') ? 'rotate(90deg)' : 'rotate(0deg)'
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

    // 待办审核分页
    document.getElementById('prev-pending-page').addEventListener('click', () => {
      if (_currentPage > 1) {
        _currentPage--
        renderPendingApplications(_allPendingApps.length)
      }
    })
    document.getElementById('next-pending-page').addEventListener('click', () => {
      const totalPages = Math.ceil(_allPendingApps.length / PENDING_PAGE_SIZE) || 1
      if (_currentPage < totalPages) {
        _currentPage++
        renderPendingApplications(_allPendingApps.length)
      }
    })

    // 用户列表分页
    document.getElementById('prev-user-page').addEventListener('click', () => {
    if (userListInstance && userListInstance.currentPage > 1) {
        userListInstance.currentPage--
        userListInstance.loadData()
    }
    })
    document.getElementById('next-user-page').addEventListener('click', () => {
    if (userListInstance && userListInstance.currentPage < userListInstance.totalPages) {
        userListInstance.currentPage++
        userListInstance.loadData()
    }
    })

    // 产品列表分页
    document.getElementById('prev-product-page').addEventListener('click', () => {
    if (productListInstance && productListInstance.currentPage > 1) {
        productListInstance.currentPage--
        productListInstance.loadData()
    }
    })
    document.getElementById('next-product-page').addEventListener('click', () => {
    if (productListInstance && productListInstance.currentPage < productListInstance.totalPages) {
        productListInstance.currentPage++
        productListInstance.loadData()
    }
    })

    // 关闭产品详情
    document.querySelector('#product-detail .close-btn')?.addEventListener('click', () => {
      document.getElementById('product-detail').style.display = 'none'
    })

    // 关闭用户详情
    document.querySelector('#user-detail .close-btn')?.addEventListener('click', () => {
      document.getElementById('user-detail').style.display = 'none'
    })

    // 增加项目按钮绑定
    document.getElementById('add-product-btn').addEventListener('click', () => {
      window.location.href = '/addProduct'
    })

    // 搜索产品按钮
    document.getElementById('search-products-btn').addEventListener('click', async () => {
      const createStart = document.getElementById('create-start-date').value
      const createEnd = document.getElementById('create-end-date').value
      const updateStart = document.getElementById('update-start-date').value
      const updateEnd = document.getElementById('update-end-date').value

      // 构建查询参数
      const params = new URLSearchParams()
      if (createStart) params.append('createStartDate', createStart)
      if (createEnd) params.append('createEndDate', createEnd)
      if (updateStart) params.append('updateStartDate', updateStart)
      if (updateEnd) params.append('updateEndDate', updateEnd)

      try {
        const url = `/api/loan-products?${params.toString()}`
        const response = await AdminWeb.API_CLIENT.get(url)

        if (response.code === 200) {
          const products = response.data || []

          // 渲染到现有表格（复用 product-table）
          const tbody = document.querySelector('#product-table tbody')
          tbody.innerHTML = ''

          if (products.length === 0) {
            tbody.innerHTML = '<tr><td colspan="7" style="text-align:center;">未找到匹配的产品</td></tr>'
          } else {
            products.forEach(prod => {
              const row = renderProductRow(prod) // 复用现有函数
              tbody.appendChild(row)
            })
          }

        } else {
          throw new Error(response.message || '搜索失败')
        }
      } catch (error) {
        console.error('搜索产品失败:', error)
        alert('搜索失败：' + error.message)
      }
    })

    // 重置按钮：清空输入并刷新全部列表
    document.getElementById('reset-search-btn').addEventListener('click', () => {
      ['create-start-date', 'create-end-date', 'update-start-date', 'update-end-date'].forEach(id => {
        document.getElementById(id).value = ''
      })
      // 重新加载全部产品
      if (productListInstance) {
        productListInstance.currentPage = 1
        productListInstance.loadData()
      }
    })
    
    // 退出登录
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
                window.location.href = '/login'
            }
        }
    })

    // ============ 日期输入互斥逻辑 ============
    document.addEventListener('DOMContentLoaded', () => {
      const createStartInput = document.getElementById('create-start-date')
      const createEndInput = document.getElementById('create-end-date')
      const updateStartInput = document.getElementById('update-start-date')
      const updateEndInput = document.getElementById('update-end-date')

      // 安全检查：确保所有元素都存在
      if (!createStartInput || !createEndInput || !updateStartInput || !updateEndInput) {
        console.error('日期输入框未找到，请检查 HTML 结构和 ID 是否正确')
        return
      }

      [createStartInput, createEndInput].forEach(input => {
        input.addEventListener('focus', () => {
          updateStartInput.value = ''
          updateEndInput.value = ''
        })
      })

      [updateStartInput, updateEndInput].forEach(input => {
        input.addEventListener('focus', () => {
          createStartInput.value = ''
          createEndInput.value = ''
        })
      })
    })
}

// 面板显示切换
function switchToPanel(target) {
  console.log('切换到面板:', target)
  // 保存当前面板到 sessionStorage
  sessionStorage.setItem('activePanel', target)
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
    // 更新侧边栏激活状态
    document.querySelectorAll('.side-item').forEach(item => {
        item.classList.remove('active')
    })
    const activeItem = document.querySelectorAll(`#${target}-content .side-item`)
    if (activeItem) {
      activeItem[0].classList.add('active')
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

    } else {
        console.error('未找到目标面板:', `${target}-content`)
        // 调试信息：列出所有可用的面板
        const allPanels = document.querySelectorAll('.dashboard')
        console.log('可用面板:', Array.from(allPanels).map(panel => panel.id))
    }

    if (target === 'user-management') {
      initUserList() // 初始化用户列表
      document.getElementById('user-main-content').style.display = 'flex'
      document.getElementById('black-main-content').style.display = 'none'
    } else if (target === 'loan-management') {
      initProductList() 
    }
}
// 子面板显示切换
function switchToContent(target) {
    document.querySelectorAll('.side-item').forEach(item => {
        item.classList.remove('active')
    })
    const activeItem = document.querySelector(`.side-item[data-target="${target}"]`)
    if (activeItem) activeItem.classList.add('active')

  switch (target) {
    // 待办审核面板
    case 'pending-apply-content':
      document.getElementById('pending-apply-content').style.display = 'flex'
      document.getElementById('pended-apply-content').style.display = 'none'
      break
    case 'pended-apply-content':
      document.getElementById('pending-apply-content').style.display = 'none'
      document.getElementById('pended-apply-content').style.display = 'flex'
      break
    
      // 用户管理面板
    case 'user-main-content':
      document.getElementById('user-main-content').style.display = 'flex'
      document.getElementById('black-main-content').style.display = 'none'
      break
    case 'black-main-content':
      document.getElementById('user-main-content').style.display = 'none'
      document.getElementById('black-main-content').style.display = 'flex'
      // 初始化黑名单列表
      if (!blacklistInstance) initBlacklist()
      break
    
      // 贷款管理面板
    case 'product-content':
      document.getElementById('product-content').style.display = 'flex'
      document.getElementById('product-content1').style.display = 'none'
      break
    case 'product-content1':
      document.getElementById('product-content').style.display = 'none'
      document.getElementById('product-content1').style.display = 'flex'
      break

      // 风控管理面板
    case 'risk-content1':
      document.getElementById('risk-content1').style.display = 'none'
      document.getElementById('risk-content2').style.display = 'flex'
      break
    case 'risk-content2':
      document.getElementById('risk-content1').style.display = 'none'
      document.getElementById('risk-content2').style.display = 'flex'
      break
    case 'collection-content1':
      document.getElementById('collection-content1').style.display = 'none'
      document.getElementById('collection-content2').style.display = 'flex'
      break
    case 'collection-content2':
      document.getElementById('collection-content1').style.display = 'none'
      document.getElementById('collection-content2').style.display = 'flex'
      break

      // 数据管理面板
    case 'data-content1':
      document.getElementById('data-content1').style.display = 'none'
      document.getElementById('data-content2').style.display = 'flex'
      break
    case 'data-content2':
      document.getElementById('data-content1').style.display = 'none'
      document.getElementById('data-content2').style.display = 'flex'
      break
    case 'system-content1':
      document.getElementById('system-content1').style.display = 'none'
      document.getElementById('system-content2').style.display = 'flex'
      break
    case 'system-content2':
      document.getElementById('system-content1').style.display = 'none'
      document.getElementById('system-content2').style.display = 'flex'
      break
    default:
      console.error('未知内容:', target)
  }
}

/*
*
* 待办审核面板处理 
*
*/ 
// ============== 待办审核列表初始化 ===============
// 加载全部待办申请
async function fetchAndRenderPendingList() {
    try {
        const response = await AdminWeb.API_CLIENT.getPendingApplications()

        if (response.code === 200 && Array.isArray(response.data)) {
            _allPendingApps = response.data
            _currentPage = 1
            renderPendingApplications(_allPendingApps.length)
        } else {
            throw new Error(response.message || '数据格式异常')
        }
    } catch (error) {
      console.error('获取待办列表失败:', error)
      document.getElementById('apply-table').querySelector('tbody').innerHTML = '<tr><td colspan="6" style="text-align:centercolor:#e74c3c;">加载失败</td></tr>'
      updatePendingPagination(0)
    }
}

// 渲染分页信息
function updatePendingPagination(total) {
  const totalPages = Math.ceil(total / PENDING_PAGE_SIZE) || 1
  const pageInfoEl = document.getElementById('pending-page-info')
  if (pageInfoEl) {
    pageInfoEl.textContent = total === 0 ? '共 0 条' : `第 ${_currentPage} 页，共 ${totalPages} 页`
  }

  // 控制按钮状态
  const prevBtn = document.getElementById('prev-pending-page')
  const nextBtn = document.getElementById('next-pending-page')
  if (prevBtn) prevBtn.disabled = (_currentPage <= 1)
  if (nextBtn) nextBtn.disabled = (_currentPage >= totalPages)
}

// 渲染当前页表格
function renderPendingApplications(total) {
    const start = (_currentPage - 1) * PENDING_PAGE_SIZE
    const pageData = _allPendingApps.slice(start, start + PENDING_PAGE_SIZE)
    const tbody = document.getElementById('apply-table').querySelector('tbody')
    tbody.innerHTML = '' // 清空旧内容

    if (pageData.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" style="text-align:center;">暂无待审核申请</td></tr>'
        updatePendingPagination(0)
        return
    }
    pageData.forEach(app => {
        const row = document.createElement('tr')
        row.setAttribute('data-application-id', app.applicationId)

        // 格式化金额
        const amount = new Intl.NumberFormat('zh-CN', {
            style: 'currency',
            currency: 'CNY',
            minimumFractionDigits: 2
        }).format(app.loanAmount)

        // 格式化时间
        const time = new Date(app.applyTime).toLocaleString('zh-CN')

        row.innerHTML = `
            <td>${app.userName || '—'}</td>
            <td>${app.productName || '—'}</td>
            <td>${amount}</td>
            <td>${app.loanPeriod || 0} 年</td>
            <td>${app.term || 0} 期</td>
            <td>${time}</td>
        `
        row.addEventListener('click', () => showApplicationDetail(app.applicationId))
        tbody.appendChild(row)
    })
    updatePendingPagination(total) // 更新分页信息
}

// 显示申请详情
async function showApplicationDetail(applicationId) {
  console.log('显示申请详情:', applicationId)
  try {
    const detail = await AdminWeb.API_CLIENT.getApprovalDetail(applicationId)
    if (detail.code !== 200) throw new Error(detail.message || '获取详情失败')

    const data = detail.data
    const user = data.user || {}
    const userCert = data.userCert || {}
    const app = data.application || {}

    // 显示详情容器
    document.getElementById('audition-detail').style.display = 'block'

    // 用户信息
    document.getElementById('realName').textContent = user.userName || '—'
    document.getElementById('phone').textContent = user.phone || '—'

    // 注册时间
    const registerTime = user.createTime 
        ? new Date(user.createTime).toLocaleString('zh-CN') 
        : '—'
    document.getElementById('register-time').textContent = registerTime

    // 信誉分
    document.getElementById('credit-score').textContent = 
        (userCert.creditScore != null) ? userCert.creditScore : '—'

    // 渲染认证材料（字段都在 userCert 下）
    const materialsContainer = document.getElementById('materials-container')
    const materialMap = {
        idCard: '身份证',
        bankCardId: '银行卡',         
        workCertId: '工作证明',
        triCertId: '三证合一',
        immovableCertId: '不动产证明'
    }

    let html = ''
    for (const [key, label] of Object.entries(materialMap)) {
        const uploaded = userCert[key] != null
        const color = uploaded ? '#27ae60' : '#e74c3c'
        const statusText = uploaded ? '已上传' : '未上传'
        html += `<div class="detail-item"><span>${label}</span><span style="color:${color};">${statusText}</span></div>`
    }
    materialsContainer.innerHTML = html

    // 贷款信息
    // 如果后端不能提供 productName，可暂时显示 productId 或留空
    document.getElementById('product-name').textContent = 
      app.productId != null ? app.productId : '—'

    document.getElementById('loan-amount').textContent = 
      app.loanAmount != null 
        ? `¥${Number(app.loanAmount).toLocaleString('zh-CN')}` 
        : '—'

    document.getElementById('term-period').textContent = app.loanPeriod != null ? `${app.loanPeriod}年` : '—'
    document.getElementById('loan-term').textContent = app.term != null ? `${app.term}期` : '—'

    // 利率和还款方式
    const interestRateEl = document.getElementById('interest-rate')
    if (interestRateEl) {
      interestRateEl.textContent = app.interestRate != null ? `${(app.interestRate * 100).toFixed(2)}%` : '—'
    }

    const repayTypeEl = document.getElementById('repay-type')
    if (repayTypeEl) {
      repayTypeEl.textContent = app.repaidType || '—'
    }

    // 拒绝原因
    const rejectReasonEl = document.getElementById('reject-reason')
    if (rejectReasonEl) {
        rejectReasonEl.textContent = app.rejectReason || '暂无'
    }

    // 绑定按钮
    document.getElementById('btn-pass').onclick = () => submitReview(applicationId, true)  // 通过
    document.getElementById('btn-reject').onclick = () => submitReview(applicationId, false) // 拒绝

  } catch (error) {
    console.error('获取申请详情失败:', error)
    alert('获取详情失败：' + (error.message || '请重试'))
  }
}

// 提交审核结果
async function submitReview(applicationId, isApproved) {
    try {
        const response = await AdminWeb.API_CLIENT.submitReview(applicationId, isApproved)
        if (response.code === 200) {
            alert(`审核${isApproved ? '通过' : '拒绝'}成功！`)
            fetchAndRenderPendingList() // 刷新列表
            document.getElementById('audition-detail').style.display = 'none'
        } else {
            throw new Error(response.message || '操作失败')
        }
    } catch (error) {
        console.error('提交审核失败:', error)
        alert('提交失败：' + (error.message || '请重试'))
    }
}

// 加载已审核列表
async function fetchAndRenderPendedList() {
  try {
    const response = await AdminWeb.API_CLIENT.getPendedApplications()
    if (response.code === 200 && Array.isArray(response.data)) {
        _allPendedApps = response.data
        _currentPage = 1
        renderPendedApplications(_allPendedApps.length)
    } else {
        throw new Error(response.message || '数据格式异常')
    }
  } catch (error) {
    console.error('获取已办列表失败:', error)
    document.getElementById('applied-table').querySelector('tbody').innerHTML = '<tr><td colspan="6" style="text-align:centercolor:#e74c3c;">加载失败</td></tr>'
    updatePendingPagination(0)
  }
}

// 渲染已审核列表分页信息
function updatePendedPagination(total) {
  const totalPages = Math.ceil(total / PENDED_PAGE_SIZE) || 1
  const pageInfoEl = document.getElementById('pended-page-info')
  if (pageInfoEl) {
    pageInfoEl.textContent = total === 0 ? '共 0 条' : `第 ${_currentPage} 页，共 ${totalPages} 页`
  }

  // 控制按钮状态
  const prevBtn = document.getElementById('prev-pended-page')
  const nextBtn = document.getElementById('next-pended-page')
  if (prevBtn) prevBtn.disabled = (_currentPage <= 1)
  if (nextBtn) nextBtn.disabled = (_currentPage >= totalPages)
}

// 渲染已审核列表当前页表格
function renderPendedApplications(total) {
    const start = (_currentPage - 1) * PENDED_PAGE_SIZE
    const pageData = _allPendedApps.slice(start, start + PENDED_PAGE_SIZE)
    const tbody = document.getElementById('applied-table').querySelector('tbody')
    tbody.innerHTML = '' // 清空旧内容

    if (pageData.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" style="text-align:center;">暂无已审核申请</td></tr>'
        updatePendedPagination(0)
        return
    }
    pageData.forEach(app => {
        const row = document.createElement('tr')
        row.setAttribute('data-application-id', app.applicationId)

        // 格式化金额
        const amount = new Intl.NumberFormat('zh-CN', {
            style: 'currency',
            currency: 'CNY',
            minimumFractionDigits: 2
        }).format(app.loanAmount)

        // 格式化时间
        const time = new Date(app.applyTime).toLocaleString('zh-CN')

        row.innerHTML = `
            <td>${app.userName || '—'}</td>
            <td>${app.productName || '—'}</td>
            <td>${app.status || '—'}</td>
            <td>${amount}</td>
            <td>${app.loanPeriod || 0} 年</td>
            <td>${app.term || 0} 期</td>
            <td>${time}</td>
        `
        row.addEventListener('click', () => showPendedApplicationDetail(app.applicationId))
        tbody.appendChild(row)
    })
    updatePendedPagination(total) // 更新分页信息
}

// 显示已审核申请详情
async function showPendedApplicationDetail(applicationId) {
  console.log('显示申请详情:', applicationId)
  try {
    const detail = await AdminWeb.API_CLIENT.getApprovalDetail(applicationId)
    if (detail.code !== 200) throw new Error(detail.message || '获取详情失败')

    const data = detail.data
    const user = data.user || {}
    const userCert = data.userCert || {}
    const app = data.application || {}

    // 显示详情容器
    document.getElementById('applied-detail').style.display = 'block'

    // 用户信息
    document.getElementById('realName1').textContent = user.userName || '—'
    document.getElementById('phone1').textContent = user.phone || '—'

    // 注册时间
    const registerTime = user.createTime 
        ? new Date(user.createTime).toLocaleString('zh-CN') 
        : '—'
    document.getElementById('register-time1').textContent = registerTime

    // 信誉分
    document.getElementById('credit-score1').textContent = 
        (userCert.creditScore != null) ? userCert.creditScore : '—'

    // 渲染认证材料（字段都在 userCert 下）
    const materialsContainer = document.getElementById('materials-container1')
    const materialMap = {
        idCard: '身份证',
        bankCardId: '银行卡',         
        workCertId: '工作证明',
        triCertId: '三证合一',
        immovableCertId: '不动产证明'
    }

    let html = ''
    for (const [key, label] of Object.entries(materialMap)) {
        const uploaded = userCert[key] != null
        const color = uploaded ? '#27ae60' : '#e74c3c'
        const statusText = uploaded ? '已上传' : '未上传'
        html += `<div class="detail-item"><span>${label}</span><span style="color:${color};">${statusText}</span></div>`
    }
    materialsContainer.innerHTML = html

    // 贷款信息
    // 如果后端不能提供 productName，可暂时显示 productId 或留空
    document.getElementById('product-name1').textContent = 
      app.productId != null ? app.productId : '—'

    document.getElementById('loan-amount1').textContent = 
      app.loanAmount != null 
        ? `¥${Number(app.loanAmount).toLocaleString('zh-CN')}` 
        : '—'

    document.getElementById('term-period1').textContent = app.loanPeriod != null ? `${app.loanPeriod}年` : '—'
    document.getElementById('loan-term1').textContent = app.term != null ? `${app.term}期` : '—'

    // 利率和还款方式
    const interestRateEl = document.getElementById('interest-rate1')
    if (interestRateEl) {
      interestRateEl.textContent = app.interestRate != null ? `${(app.interestRate * 100).toFixed(2)}%` : '—'
    }

    const repayTypeEl = document.getElementById('repay-type1')
    if (repayTypeEl) {
      repayTypeEl.textContent = app.repaidType || '—'
    }

    // 拒绝原因
    const rejectReasonEl = document.getElementById('reject-reason1')
    if (rejectReasonEl) {
      const reason = app.rejectReason || '—'
      rejectReasonEl.innerHTML = reason.replace(/\n/g, '<br>')
    }
  } catch (error) {
    console.error('获取申请详情失败:', error)
    alert('获取详情失败：' + (error.message || '请重试'))
  }
}
/*
*==================== 贷款项目管理面板处理 ====================
*/ 
// ============== 贷款项目展示 ===============
// 贷款产品列表事件绑定
function renderProductRow(product) {
  const tr = document.createElement('tr')
  tr.setAttribute('data-product-id', product.productId)
  
  tr.innerHTML = `
    <td>${product.productId}</td>
    <td>${product.productName || '—'}</td>
    <td>${product.description || '—'}</td>
    <td>${product.LoanUsage || '—'}</td>
    <td>${product.status || '—'}</td>
    <td>${product.updateTime ? new Date(product.updateTime).toLocaleString() : '—'}</td>
    <td>${product.createTime ? new Date(product.createTime).toLocaleString() : '—'}</td>
    <td>
      ${product.status === '上架中' 
        ? '<button class="toggle-status-btn" data-action="deactive">下架</button>' 
        : '<button class="toggle-status-btn" data-action="active">上架</button>'}
      <button class="delete-prod-btn">删除</button>
    </td>
  `

  tr.addEventListener('click', (e) => {
    if (!e.target.closest('.toggle-status-btn') && !e.target.closest('.delete-prod-btn')) {
      showProductDetail(product.productId)
    }
  })

  // 上架/下架按钮
  const toggleBtn = tr.querySelector('.toggle-status-btn')
  toggleBtn?.addEventListener('click', async (e) => {
    e.stopPropagation()
    const action = toggleBtn.dataset.action // 'active' 或 'deactive'
    initProductList() // 刷新列表
    try {
      await toggleLoanProductStatus(product.productId, action)
      initProductList() // 刷新列表
    } catch (error) {
      alert(`${action === 'active' ? '上架' : '下架'}失败：`+ error.message)
    }
  })

  // 删除按钮
  const deleteBtn = tr.querySelector('.delete-prod-btn')
  deleteBtn?.addEventListener('click', async (e) => {
    e.stopPropagation()
    try {
        // 获取待审申请列表
        const response = await AdminWeb.API_CLIENT.getPendingApplications()
        const applications = response.data

        // 检查是否存在关联的待审申请（通过产品名称匹配）
        const hasPending = applications.some(app => 
          app.productName === product.productName
        )

        if (hasPending) {
          alert('该产品存在待审核申请，无法删除！')
          return
        }

        // 无待审申请时弹出确认框
        if (confirm(`确定删除产品【${product.productName}】？`)) {
          await AdminWeb.API_CLIENT.deleteLoanProduct(product.productId)
          initProductList()
        }
      } catch (error) {
        console.error('删除检查失败:', error)
        alert('操作失败：' + (error.message || '网络错误'))
      }
    })

  return tr
}

// 显示产品详情（仅展示，不含编辑）
async function showProductDetail(productId) {
  try {
    const detail = await AdminWeb.API_CLIENT.getLoanProductById(productId)
    if (detail.code !== 200) throw new Error(detail.message)

    const data = detail.data
    currentProductId = productId // 全局缓存用于后续操作

    // 渲染基本信息（只读）
    document.getElementById('prod-name').textContent = data.productName || '—'
    document.getElementById('prod-status').textContent = data.status || '—'
    document.getElementById('prod-term').textContent = 
      `${data.terms || 0}个月 `
    document.getElementById('prod-promo').textContent = data.promotionDetails || '—'
    document.getElementById('prod-desc').textContent = data.description || '—'
    document.getElementById('prod-usage').textContent = data.loanUsage || '—'
    document.getElementById('prod-create-time').textContent = data.createTime ? new Date(data.createTime).toLocaleString() : '—'
    document.getElementById('prod-update-time').textContent = data.updateTime ? new Date(data.updateTime).toLocaleString() : '—'

    // 渲染可编辑的可选方案（即初始数据）
    renderEditableOptionTable(data.options || [])

    // 绑定事件（现在只需绑定一次，且无状态切换）
    bindOptionManagementEvents(data.options || [])

    // 绑定编辑按钮
    // document.getElementById('edit-product-btn').addEventListener('click', () => {
    //   document.getElementById('product-detail').style.display = 'none'
    //   showProductEditForm(data)
    // })

    document.getElementById('product-detail').style.display = 'block'
  } catch (error) {
    console.error('获取产品详情失败:', error)
    alert('加载失败')
  }
}
//渲染可编辑的可选方案列表
function renderEditableOptionTable(options) {
  const tbody = document.getElementById('options-table').querySelector('tbody')
  tbody.innerHTML = ''

  if (options.length === 0) {
    // 如果无方案，默认加一行空白（可选）
    tbody.appendChild(createOptionRow())
  } else {
    options.forEach(opt => {
      tbody.appendChild(createOptionRow(opt))
    })
  }
}
// 绑定可选方案的独立增删改逻辑
function bindOptionManagementEvents() {
  const addBtn = document.getElementById('add-option-btn')
  const confirmBtn = document.getElementById('confirm-changes-btn')
  const table = document.getElementById('options-table')

  if (!addBtn || !confirmBtn || !table) return

  // 增加新行
  addBtn.onclick = () => {
    const tbody = table.querySelector('tbody')
    const newRow = createOptionRow()
    tbody.appendChild(newRow)
  }

  // 删除行（事件委托）
  table.onclick = async function(e) {
    if (e.target.classList.contains('delete-option-row')) {
      const row = e.target.closest('tr')
      const optionId = row.dataset.optionId

      if (optionId=='-') {
        row.remove()
        return
      }else{
        const response = await AdminWeb.API_CLIENT.deleteOption(optionId)
        if (response.code === 200) {
          row.remove()
          console.log('删除成功')
        } else {
          console.error('删除失败，请重试')
        }
      }
    }
  }

  // 确认提交
  confirmBtn.onclick = async () => {
    // 在 showProductDetail 中获取数据
    const originalOptions = window.originalProductOptions || []

    const rows = table.querySelectorAll('tbody tr')
    const toCreate = []
    const toDeleteIds = []

    // 收集当前数据
    const currentData = []
    for (const row of rows) {
      const [inpAmt, inpTerm, inpRate, sel] = row.querySelectorAll('input, select')
      const loanAmount = parseFloat(inpAmt.value)
      const loanPeriod = parseInt(inpTerm.value)
      const interestRate = parseFloat(inpRate.value)
      const repaidType = sel.value?.trim()

      if (isNaN(loanAmount) && isNaN(loanPeriod) && isNaN(interestRate) && !repaidType) {
        continue // 跳过空白行
      }
      if (isNaN(loanAmount) || isNaN(loanPeriod) || isNaN(interestRate) || !repaidType) {
        alert('请填写完整的方案信息')
        return
      }

      currentData.push({
        id: row.dataset.optionId || null,
        loanAmount,
        loanPeriod,
        interestRate,
        repaidType
      })
    }

    // 当前存在的有效 ID 集合
    const currentIds = new Set(
      currentData
        .filter(d => d.id != null)
        .map(d => Number(d.id))
        .filter(n => !isNaN(n) && n > 0)
    )

    // 找出要删除的（仅限原始数据中 id 有效的项）
    originalOptions.forEach(opt => {
      if (opt.id != null) {
        const idNum = Number(opt.id)
        if (!isNaN(idNum) && idNum > 0 && !currentIds.has(idNum)) {
          toDeleteIds.push(opt.id)
        }
      }
    })

    // 过滤待删除 ID
    const validDeleteIds = toDeleteIds.filter(id => {
      const n = Number(id)
      return !isNaN(n) && n > 0 && Number.isInteger(n)
    })

    // 找出要新增的
    currentData.forEach(item => {
      if (!item.id) {
        toCreate.push({
          loanAmount: item.loanAmount,
          loanPeriod: item.loanPeriod,
          interestRate: item.interestRate,
          repaidType: item.repaidType
        })
      }
    })

    try {
      if (validDeleteIds.length > 0) {
        if (validDeleteIds.length === 1) {
          await AdminWeb.API_CLIENT.deleteOption(validDeleteIds[0])
        } else {
          await AdminWeb.API_CLIENT.batchDeleteOptions(validDeleteIds)
        }
      }

      if (toCreate.length > 0) {
        await AdminWeb.API_CLIENT.batchCreateOptions(currentProductId, toCreate)
      }

      alert('更新成功')
      optionEventsInitialized = false // 重置
      showProductDetail(currentProductId)
    } catch (err) {
      console.error('失败:', err)
      alert('操作失败：' + (err.message || '请重试'))
      optionEventsInitialized = false
    }
  }
}
// 渲染可编辑的可选方案行
function renderProductOptions(options) {
  const tbody = document.getElementById('prod-option-table').querySelector('tbody')
  tbody.innerHTML = ''

  if (!options || options.length === 0) {
    // 默认空行
    addOptionRow(tbody)
    return
  }

  options.forEach(opt => {
    const row = createOptionRow(opt)
    tbody.appendChild(row)
  })
}
// 创建可编辑的可选方案行
function createOptionRow(opt = {}){
  // 原始值（用于 placeholder）
  const origAmount = opt.loanAmount != null ? opt.loanAmount : ''
  const origPeriod = opt.loanPeriod != null ? opt.loanPeriod : ''
  const origRate = opt.interestRate != null ? opt.interestRate : ''
  const origRepay = opt.repaidType || '等额本息'
  const placeholderRate = isNaN(origRate) 
      ? "如 0.05" 
      : (origRate * 100).toFixed(2)

  const tr = document.createElement('tr')
  tr.dataset.optionId = opt.optionId || '-'

  tr.innerHTML = `
    <td>
      <input 
        type="number" 
        step="0.01" 
        placeholder="${origAmount.toLocaleString() || "如 10000"}" 
        value="${opt.loanAmount || ''}" 
      />
    </td>
    <td>
      <input 
        type="number" 
        placeholder="${origPeriod||"如 12"}" 
        value="${opt.loanPeriod || ''}" 
      />
    </td>
    <td>
      <input 
        type="number" 
        step="0.0001" 
        placeholder="${placeholderRate}%"
        value="${opt.interestRate || ''}" 
      />
    </td>
    <td>
      <select>
        <option value="等额本息" ${opt.repaidType === '等额本息' ? 'selected' : ''}>等额本息</option>
        <option value="等额本金" ${opt.repaidType === '等额本金' ? 'selected' : ''}>等额本金</option>
        <option value="先息后本" ${opt.repaidType === '先息后本' ? 'selected' : ''}>先息后本</option>
        <option value="一次性还本付息" ${opt.repaidType === '一次性还本付息' ? 'selected' : ''}>一次性还本付息</option>
      </select>
    </td>
    <td><button class="delete-option-row">删除</button></td>
  `
  return tr
}
function addOptionRow(tbody) {
  const row = createOptionRow()
  tbody.appendChild(row)
}


// 产品列表初始化
async function initProductList() {
  if (productListInstance) return
  
  let allProducts = []
  try {
    const res = await AdminWeb.API_CLIENT.getAllLoanProducts()
    if (res.code === 200) allProducts = res.data
  } catch (err) {
    console.error('获取产品列表失败', err)
  }

  const pageSize = 5
  const totalPages = Math.ceil(allProducts.length / pageSize)

  productListInstance = {
    currentPage: 1,
    totalPages: totalPages,
    allData: allProducts,
    pageSize: pageSize,
    render: function() {
      const start = (this.currentPage - 1) * this.pageSize
      const pageData = this.allData.slice(start, start + this.pageSize)
      const tbody = document.getElementById('product-table').querySelector('tbody')
      tbody.innerHTML = ''
      
      if (pageData.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" style="text-align:center;">暂无产品</td></tr>'
        return
      }
      
      pageData.forEach(prod => {
        const row = renderProductRow(prod)
        tbody.appendChild(row)
      })
      
      document.getElementById('product-page-info').textContent = 
        `第 ${this.currentPage} 页，共 ${this.totalPages} 页`
      document.getElementById('prev-product-page').disabled = (this.currentPage <= 1)
      document.getElementById('next-product-page').disabled = (this.currentPage >= this.totalPages)
    },
    loadData: function() {
      this.render()
    }
  }
  
  productListInstance.render()
}
// 更新单个贷款产品
async function updateLoanProduct(productId, updateData) {
    const url = `/api/loan-products/admin/${productId}`
    console.log(`[PATCH] 更新产品: ${url}`, '请求体:', updateData)
    try {
        const response = await AdminWeb.API_CLIENT.request(url, {
            method: 'PATCH',
            body: JSON.stringify(updateData)
        })
        console.log(`✅ [响应] 产品 ${productId} 更新成功:`, response)
        alert('产品信息更新成功')
        return response.data
    } catch (error) {
        console.error(`❌ [错误] 更新产品 ${productId} 失败:`, error)
        alert('更新失败')
    }
}
// 切换产品状态（上架/下架）
async function toggleLoanProductStatus(productId, action) {
  // action: 'active' 或 'deactive'
  const url = `${AdminWeb.API_CONFIG.baseUrl}/api/loan-products/admin/${productId}/${action}`
  
  try {
    const response = await AdminWeb.API_CLIENT.post(url, null)
    alert(`产品${action === 'active' ? '上架' : '下架'}成功`)
    // refreshProductList()
    initProductList()
    return response
  } catch (error) {
    console.error('操作失败:', error)
    alert('操作失败：' + (error.message || '请重试'))
  }
}


// ==================== 用户管理面板处理 ====================
// 渲染单行用户
function renderUserRow(user) {
  const tr = document.createElement('tr')
  tr.setAttribute('data-user-id', user.userId)
  
  tr.innerHTML = `
    <td>${user.userId}</td>
    <td>${user.userName || '—'}</td>
    <td>${user.creditScore || '—'}</td>
    <td>${user.loanStatus || '—'}</td>
    <td>${user.totalTransactionCount || 0}</td>
    <td>¥${(user.totalLoanAmount || 0).toLocaleString()}</td>
    <td>¥${(user.totalRepaidAmount || 0).toLocaleString()}</td>
    <td><button id="black-btn">加入黑名单</button></td>
  `
  // 行点击事件：查看详情
  tr.addEventListener('click', () => showUserDetail(user.userId))
  
  // 加入黑名单按钮
  const deleteBtn = tr.querySelector('#black-btn')
  deleteBtn?.addEventListener('click', async (e) => {
    e.stopPropagation()
    if (confirm(`确定加入黑名单用户【${user.userName}】？`)) {
      try {
        const level = prompt('请输入黑名单等级:')
        await AdminWeb.API_CLIENT.addToBlacklist(user.userId,level)
        alert('加入黑名单成功')
        initUserList() // 刷新列表
      } catch (error) {
        alert('加入黑名单失败：' + error.message)
      }
    }
  })

  return tr
}
// 初始化用户列表（在 switchToPanel 中调用）
async function initUserList() {
  if (userListInstance) return // 避免重复初始化
  
  const fetchData = async (page, pageSize) => {
    const response = await AdminWeb.API_CLIENT.getUserStats()
    if (response.code === 200) {
      return response.data // 接口返回的是全量
    } else {
      throw new Error(response.message)
    }
  }

  // 分页
  let allUsers = []
  try {
    const res = await AdminWeb.API_CLIENT.getUserStats()
    if (res.code === 200) allUsers = res.data
  } catch (err) {
    console.error('获取用户列表失败', err)
  }

  // 自定义分页逻辑
  const pageSize = 5
  const totalPages = Math.ceil(allUsers.length / pageSize)

  userListInstance = {
    currentPage: 1,
    totalPages: totalPages,
    allData: allUsers,
    pageSize: pageSize,
    render: function() {
      const start = (this.currentPage - 1) * this.pageSize
      const pageData = this.allData.slice(start, start + this.pageSize)
      const tbody = document.getElementById('user-table').querySelector('tbody')
      tbody.innerHTML = ''
      
      if (pageData.length === 0) {
        tbody.innerHTML = '<tr><td colspan="8" style="text-align:center;">暂无用户</td></tr>'
        return
      }
      
      pageData.forEach(user => {
        const row = renderUserRow(user)
        tbody.appendChild(row)
      })
      
      // 更新分页信息
      document.getElementById('user-page-info').textContent = 
        `第 ${this.currentPage} 页，共 ${this.totalPages} 页`
      document.getElementById('prev-user-page').disabled = (this.currentPage <= 1)
      document.getElementById('next-user-page').disabled = (this.currentPage >= this.totalPages)
    },
    loadData: function() {
      this.render()
    }
  }
  userListInstance.render()
}
// 显示用户详情
async function showUserDetail(userId) {
  // 显示模态框
  document.getElementById('user-detail').style.display = 'block'

  try {
    const detail = await AdminWeb.API_CLIENT.getUserDetail(userId)
    if (detail.code !== 200) throw new Error(detail.message || '获取失败')
    
    const user = detail.data.user
    const userCert = detail.data.userCert||{}
    const loanApplications = detail.data.loanApplication||{}
    const orders = detail.data.order||{}
    // 填充基本信息
    document.getElementById('user-real-name').textContent = user.userName || '—'
    document.getElementById('user-phone').textContent = user.phone || '—'
    document.getElementById('user-register-time').textContent = 
      user.createTime ? new Date(user.createTime).toLocaleString() : '—'
        document.getElementById('user-update-time').textContent = 
      user.updateTime ? new Date(user.updateTime).toLocaleString() : '—'
    
    // 认证材料（简化：只显示是否上传）
    const materialsContainer = document.getElementById('user-auth-section')
    const materialMap = {
      idCard: '身份证',
      bankCardId: '银行卡',
      workCertId: '工作证明',
      triCertId: '三证合一',
      immovableCertId: '不动产证明'
    }
    let html = `<div class="material-item"><span>信誉分:</span><span style="color:red;"> ${(userCert.creditScore != null) ? userCert.creditScore : '—  '}</span></div>`
    for (const [key, label] of Object.entries(materialMap)) {
      const uploaded = userCert[key] != null
      const color = uploaded ? '#27ae60' : '#e74c3c'
      const statusText = uploaded ? '已上传' : '未上传'
      html += `<div class="material-item"><span>${label}</span><span style="color:${color};">${statusText}</span></div>`
    }
    materialsContainer.innerHTML = html
    
    // 显示贷款申请
    if (loanApplications) {
      const tbody = document.getElementById('application-table').querySelector('tbody')
      tbody.innerHTML = ''
      loanApplications.forEach((app, i) => {  
        const row = document.createElement('tr')
        const rejectReason = app.rejectReason?.trim() || '—'
        row.innerHTML = `
          <td>${i+1}</td>
          <td>${app.productId}</td>
          <td>${app.loanAmount || '—'}</td>
          <td>${app.term || '—'}</td>
          <td>${app.repaidType || '—'}</td>
          <td>${app.interestRate || '—'}</td>
          <td>${app.applyTime || '—'}</td>
          <td>${app.status || '—'}</td>
          <td>${rejectReason || '—'}</td>
          <td>${app.reviewTime || '—'}</td>
        `
        tbody.appendChild(row)
      })
    } else {
      const tbody = document.getElementById('application-table').querySelector('tbody')
      tbody.innerHTML = '<tr><td colspan="8" style="text-align:center;">暂无申请</td></tr>'
    }
  
    // 显示订单列表
    if (orders) {
      const tbody = document.getElementById('order-table').querySelector('tbody')
      tbody.innerHTML = ''
      orders.forEach((order, id) => {  
        const row = document.createElement('tr')
        row.innerHTML = `
          <td>${id+1}</td>
          <td>${order.productId}</td>
          <td>${order.status || '—'}</td>
          <td>${order.repaidAmount || '—'}</td>
          <td>${order.loanAmount || '—'}</td>
          <td>${order.interestRate || '—'}</td>
          <td>${order.repaidType || '—'}</td>
          <td>${order.loanPeriod || '—'}</td>
          <td>${order.term || '—'}</td>
          <td>${order.currentTerm || '—'}</td>
          <td>${order.contract || '—'}</td>
          <td>${order.overdueDays || '—'}</td>
          <td>${order.startTime || '—'}</td>
        `
        tbody.appendChild(row)
      })
    } else {
      const tbody = document.getElementById('application-table').querySelector('tbody')
      tbody.innerHTML = '<tr><td colspan="8" style="text-align:center;">暂无申请</td></tr>'
    }
  } catch (error) {
    console.error('获取用户详情失败:', error)
    alert('加载用户详情失败')
  }
}

// 通过申请ID获取用户申请详情
async function fetchApplicationById(applicationId) {
    const url = `/api/loan-applications/${applicationId}`
    console.log(`📡 [GET] 请求申请详情: ${url}`)
    try {
        const response = await AdminWeb.API_CLIENT.get(url)
        console.log(`✅ [响应] 申请 ${applicationId} 详情:`, response)
        return response.data
    } catch (error) {
        console.error(`❌ [错误] 获取申请 ${applicationId} 失败:`, error)
        alert('申请详情加载失败')
    }
}
// 通过用户ID获取用户所有申请
async function fetchApplicationsByUser(userId) {
    const url = `/api/loan-applications/user/${userId}`
    console.log(`📡 [GET] 请求用户所有申请: ${url}`)
    try {
        const response = await AdminWeb.API_CLIENT.get(url)
        console.log(`✅ [响应] 用户 ${userId} 的所有申请:`, response)
        return response.data
    } catch (error) {
        console.error(`❌ [错误] 获取用户 ${userId} 的申请失败:`, error)
        alert('申请记录加载失败')
    }
}

// 初始化黑名单列表
async function initBlacklist() {
  if (blacklistInstance) return // 避免重复初始化

  let allBlacklist = []
  try {
    // 调用黑名单接口
    const res = await AdminWeb.API_CLIENT.getBlacklist()
    if (res.code === 200) allBlacklist = res.data
  } catch (err) {
    console.error('获取黑名单列表失败', err)
    alert('加载黑名单失败')
    return
  }

  // 分页逻辑（复用 userListInstance 结构，或新建 blacklistInstance）
  const pageSize = 5
  const totalPages = Math.ceil(allBlacklist.length / pageSize)

  blacklistInstance = {
    currentPage: 1,
    totalPages,
    allData: allBlacklist,
    pageSize,
    render: function() {
      const start = (this.currentPage - 1) * this.pageSize
      const pageData = this.allData.slice(start, start + this.pageSize)
      const tbody = document.getElementById('black-user-table').querySelector('tbody') // 注意：需确认表格 ID
      tbody.innerHTML = ''

      if (pageData.length === 0) {
        tbody.innerHTML = '<tr><td colspan="9" style="text-align:center;">暂无黑名单用户</td></tr>'
        return
      }

      pageData.forEach(item => {
        const row = renderBlacklistRow(item)
        tbody.appendChild(row)
      })

      // 更新分页信息（需对应黑名单的分页元素 ID）
      const pageInfoEl = document.getElementById('blacklist-page-info')
      if (pageInfoEl) {
        pageInfoEl.textContent = `第 ${this.currentPage} 页，共 ${this.totalPages} 页`
      }
      const prevBtn = document.getElementById('prev-blacklist-page')
      const nextBtn = document.getElementById('next-blacklist-page')
      if (prevBtn) prevBtn.disabled = this.currentPage <= 1
      if (nextBtn) nextBtn.disabled = this.currentPage >= this.totalPages
    },
    loadData: function() {
      this.render()
    }
  }

  blacklistInstance.render()

  // 绑定分页按钮事件（仅首次初始化时绑定）
  const prevBtn = document.getElementById('prev-blacklist-page')
  const nextBtn = document.getElementById('next-blacklist-page')

  if (prevBtn && !prevBtn.hasBlacklistListener) {
    prevBtn.addEventListener('click', () => {
      if (blacklistInstance.currentPage > 1) {
        blacklistInstance.currentPage--
        blacklistInstance.loadData()
      }
    })
    prevBtn.hasBlacklistListener = true
  }

  if (nextBtn && !nextBtn.hasBlacklistListener) {
    nextBtn.addEventListener('click', () => {
      if (blacklistInstance.currentPage < blacklistInstance.totalPages) {
        blacklistInstance.currentPage++
        blacklistInstance.loadData()
      }
    })
    nextBtn.hasBlacklistListener = true
  }
}

// 渲染单行黑名单
function renderBlacklistRow(item) {
  const tr = document.createElement('tr')
  tr.setAttribute('data-user-id', item.userId)
  tr.innerHTML = `
    <td>${item.id}</td>
    <td>${item.userId || '—'}</td>
    <td>${item.userName || '—'}</td>
    <td>${item.phone || '—'}</td>
    <td>${item.blackLevel || '—'}</td>
    <td>${item.createTime ? new Date(item.createTime).toLocaleString() : '—'}</td>
    <td>${item.updateTime ? new Date(item.updateTime).toLocaleString() : '—'}</td>
    <td>${item.removeTime ? new Date(item.removeTime).toLocaleString() : '—'}</td>
    <td>
      <button class="remove-black-btn" data-user-id="${item.userId}">解除黑名单</button>
    </td>
  `

  // 绑定解除黑名单事件
  const removeBtn = tr.querySelector('.remove-black-btn')
  removeBtn?.addEventListener('click', async (e) => {
    e.stopPropagation()
    if (confirm(`确定解除用户【${item.userName}】的黑名单？`)) {
      try {
        await AdminWeb.API_CLIENT.removeFromBlacklist(item.userId)
        alert('已解除黑名单')
        // 重新加载黑名单列表
        blacklistInstance = null
        initBlacklist()
      } catch (error) {
        alert('操作失败：' + error.message)
      }
    }
  })

  // 可选：点击行查看详情
  tr.addEventListener('click', () => showBlackUserDetail(item.userId))

  return tr
}

// 显示黑名单用户详情
async function showBlackUserDetail(userId) {
  // 显示模态框
  document.getElementById('black-user-detail').style.display = 'block'

  try {
    const detail = await AdminWeb.API_CLIENT.getUserDetail(userId)
    if (detail.code !== 200) throw new Error(detail.message || '获取失败')
    
    const user = detail.data.user || {}
    const userCert = detail.data.userCert||{}
    const loanApplications = detail.data.loanApplication||{}
    const orders = detail.data.order||{}
    // 填充基本信息
    document.getElementById('user-real-name1').textContent = user.userName || '—'
    document.getElementById('user-phone1').textContent = user.phone || '—'
    document.getElementById('user-register-time1').textContent = 
      user.createTime ? new Date(user.createTime).toLocaleString('zh-CN') : '—'
        document.getElementById('user-update-time1').textContent = 
      user.updateTime ? new Date(user.updateTime).toLocaleString('zh-CN') : '—'
    
    // 认证材料（简化：只显示是否上传）
    // document.getElementById('user-credit-score').textContent = 
    //   (userCert.creditScore != null) ? userCert.creditScore : '—'
    const materialsContainer = document.getElementById('user-auth-section1')
    const materialMap = {
      creditScore: '信誉分',
      idCard: '身份证',
      bankCardId: '银行卡',
      workCertId: '工作证明',
      triCertId: '三证合一',
      immovableCertId: '不动产证明'
    }
    let html = ''
    for (const [key, label] of Object.entries(materialMap)) {
      if (key === 'creditScore') {
        const score = userCert.creditScore != null ? userCert.creditScore : '—'
        html += `<div class="material-item"><span>${label}:</span><span style="color:${score === '—' ? '#e74c3c' : '#27ae60'};">${score}</span></div>`
      } else {
        const uploaded = userCert[key] != null
        const color = uploaded ? '#27ae60' : '#e74c3c'
        const statusText = uploaded ? '已上传' : '未上传'
        html += `<div class="material-item"><span>${label}</span><span style="color:${color};">${statusText}</span></div>`
      }
    }
    materialsContainer.innerHTML = html
    
    // 显示贷款申请
    const appTbody = document.getElementById('application-table1').querySelector('tbody')
    appTbody.innerHTML = ''
    if (loanApplications.length > 0) {
      loanApplications.forEach((app, i) => {
        const row = document.createElement('tr')
        const amount = app.loanAmount != null ? `¥${Number(app.loanAmount).toLocaleString('zh-CN')}` : '—'
        const rate = app.interestRate != null ? `${(app.interestRate * 100).toFixed(2)}%` : '—'
        const rejectReason = (app.rejectReason || '').trim() || '—'
        row.innerHTML = `
          <td>${i + 1}</td>
          <td>${app.productId || '—'}</td>
          <td>${amount}</td>
          <td>${app.term || '—'} 期</td>
          <td>${app.repaidType || '—'}</td>
          <td>${rate}</td>
          <td>${app.applyTime ? new Date(app.applyTime).toLocaleString('zh-CN') : '—'}</td>
          <td>${app.status || '—'}</td>
          <td>${rejectReason}</td>
          <td>${app.reviewTime ? new Date(app.reviewTime).toLocaleString('zh-CN') : '—'}</td>
        `
        appTbody.appendChild(row)
      })
      } else {
        appTbody.innerHTML = '<tr><td colspan="10" style="text-align:center;">暂无贷款申请</td></tr>'
      }
    
      // 显示订单列表
    const orderTbody = document.getElementById('order-table1').querySelector('tbody')
    orderTbody.innerHTML = ''
    if (orders.length > 0) {
      orders.forEach((order, i) => {
        const repaidAmount = order.repaidAmount != null ? `¥${Number(order.repaidAmount).toLocaleString('zh-CN')}` : '—'
        const loanAmount = order.loanAmount != null ? `¥${Number(order.loanAmount).toLocaleString('zh-CN')}` : '—'
        const rate = order.interestRate != null ? `${(order.interestRate * 100).toFixed(2)}%` : '—'
        row.innerHTML = `
          <td>${i + 1}</td>
          <td>${order.productId || '—'}</td>
          <td>${order.status || '—'}</td>
          <td>${repaidAmount}</td>
          <td>${loanAmount}</td>
          <td>${rate}</td>
          <td>${order.repaidType || '—'}</td>
          <td>${order.loanPeriod || '—'} 年</td>
          <td>${order.term || '—'} 期</td>
          <td>${order.currentTerm || '—'}</td>
          <td>${order.contract || '—'}</td>
          <td>${order.overdueDays || '—'}</td>
          <td>${order.startTime ? new Date(order.startTime).toLocaleString('zh-CN') : '—'}</td>
        `
        orderTbody.appendChild(row)
      })
    } else {
      orderTbody.innerHTML = '<tr><td colspan="13" style="text-align:center;">暂无贷款订单</td></tr>'
    }
  } catch (error) {
    console.error('获取黑名单用户详情失败:', error)
    alert('加载黑名单用户详情失败')
  }
}

// ========================= 可复用部件 ================================
/*
 * 分页列表切页控制
 */
class PaginatedList {
  constructor({
    containerId,
    tableBodyId,
    renderRow,
    fetchData,
    detailHandler,
    pageSize = 10
  }) {
    this.container = document.getElementById(containerId)
    this.tbody = document.getElementById(tableBodyId)
    this.renderRow = renderRow
    this.fetchData = fetchData
    this.detailHandler = detailHandler
    this.pageSize = pageSize
    this.currentPage = 1

    this.initPagination()
    this.bindEvents()
    this.loadData()
  }

  async loadData() {
    try {
      const data = await this.fetchData(this.currentPage, this.pageSize)
      this.render(data.records || data) // 兼容两种结构
    } catch (err) {
      console.error('加载失败', err)
      this.tbody.innerHTML = `<tr><td colspan="6">加载失败</td></tr>`
    }
  }

  render(records) {
    this.tbody.innerHTML = ''
    records.forEach(item => {
      const row = this.renderRow(item)
      row.addEventListener('click', () => this.detailHandler(item))
      this.tbody.appendChild(row)
    })
  }

  initPagination() {
    this.paginationEl = this.container.querySelector('.pagination')
    if (!this.paginationEl) {
      this.paginationEl = document.createElement('div')
      this.paginationEl.className = 'pagination'
      this.container.appendChild(this.paginationEl)
    }
    this.updatePagination()
  }

  updatePagination() {
    this.paginationEl.innerHTML = `
      <button id="prev-page" ${this.currentPage <= 1 ? 'disabled' : ''}>上一页</button>
      <span>第 ${this.currentPage} 页</span>
      <button id="next-page">下一页</button>
    `
  }

  bindEvents() {
    this.container.addEventListener('click', (e) => {
      if (e.target.id === 'prev-page' && this.currentPage > 1) {
        this.currentPage--
        this.loadData()
        this.updatePagination()
      }
      if (e.target.id === 'next-page') {
        this.currentPage++
        this.loadData()
        this.updatePagination()
      }
    })
  }
}


// =========================页面加载完成后初始化=========================
document.addEventListener('DOMContentLoaded', function() {
  init()

  new DateRangePicker('create-start-date', 'create-end-date')
  new DateRangePicker('update-start-date', 'update-end-date')
})