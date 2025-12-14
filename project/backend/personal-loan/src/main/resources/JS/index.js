const API_CONFIG = AdminWeb.API_CONFIG
const JWT_CONFIG = AdminWeb.JWT_CONFIG
const DOM_ELEMENTS = AdminWeb.DOM_ELEMENTS
const API_CLIENT = AdminWeb.API_CLIENT
const JWT_UTILS = AdminWeb.JWT_UTILS

// ==================== 全局变量 ====================
let _allPendingApps = [] // 待办申请
let _currentPage = 1
const PENDING_PAGE_SIZE = 5

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
    document.querySelectorAll('.side-link.side-menu').forEach(button => {
        button.addEventListener('click', function(e) {
            e.preventDefault()
            const sideItem = this.closest('.side-item')
            // 切换 expanded 类
            sideItem.classList.toggle('expanded')
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
          
          // 显示搜索结果数量
          // const infoEl = document.getElementById('search-result-info')
          // infoEl.textContent = `搜索到 ${products.length} 个产品`
          // infoEl.style.display = 'block'

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

          // 隐藏分页（因为不分页），显示关闭按钮
          // document.querySelector('.product-pagination')?.style.display = 'none'
          document.getElementById('close-search-result-btn').style.display = 'inline-block'
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
      document.getElementById('search-result-info').style.display = 'none'
      document.getElementById('close-search-result-btn').style.display = 'none'
    })

    // 关闭产品搜索结果按钮
    document.getElementById('close-search-result-btn').addEventListener('click', () => {
      // 重新加载全部产品列表
      if (productListInstance) {
        productListInstance.currentPage = 1
        productListInstance.loadData()
      }
      // document.getElementById('search-result-info').style.display = 'none'
      document.getElementById('close-search-result-btn').style.display = 'none'
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
    const createStartInput = document.getElementById('create-start-date')
    const createEndInput = document.getElementById('create-end-date')
    const updateStartInput = document.getElementById('update-start-date')
    const updateEndInput = document.getElementById('update-end-date')

    // // 点击创建时间输入框 → 清空更新时间
    // [createStartInput, createEndInput].forEach(input => {
    //   input.addEventListener('focus', () => {
    //     updateStartInput.value = ''
    //     updateEndInput.value = ''
    //   })
    // })

    // // 点击更新时间输入框 → 清空创建时间
    // [updateStartInput, updateEndInput].forEach(input => {
    //   input.addEventListener('focus', () => {
    //     createStartInput.value = ''
    //     createEndInput.value = ''
    //   })
    //   })
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
  switch (target) {
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
    case 'pending-apply-content':
      document.getElementById('pending-apply-content').style.display = 'flex'
      document.getElementById('pended-apply-content').style.display = 'none'
      break
    case 'pended-apply-content':
      document.getElementById('pending-apply-content').style.display = 'none'
      document.getElementById('pended-apply-content').style.display = 'flex'
      break
    // case 'product-main-content':
    //   document.getElementById('user-main-content').style.display = 'none'
    //   document.getElementById('user-detail').style.display = 'flex'
    //   break
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

/*
*==================== 贷款项目管理面板处理 ====================
*/ 
// 增加项目按钮绑定
document.getElementById('add-product-btn').addEventListener('click', () => {
  window.location.href = '/addProduct'
})
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
// 显示产品详情
async function showProductDetail(productId) {
  try {
    const detail = await AdminWeb.API_CLIENT.getLoanProductById(productId)
    if (detail.code !== 200) throw new Error(detail.message)
    
    const data = detail.data
    document.getElementById('prod-name').textContent = data.productName || '—'
    document.getElementById('prod-status').textContent = data.status || '—'
    document.getElementById('prod-term').textContent = 
      `${data.minTerm || 0} ~ ${data.maxTerm || 0} 月 (步长: ${data.termStep || 1})`
    document.getElementById('prod-promo').textContent = data.promotionDetails || '—'
    
    // 渲染选项
    const tbody = document.getElementById('prod-options-table').querySelector('tbody')
    tbody.innerHTML = ''
    if (data.options && data.options.length) {
      data.options.forEach(opt => {
        const tr = document.createElement('tr')
        tr.innerHTML = `
          <td>¥${opt.loanAmount.toLocaleString()}</td>
          <td>${opt.loanPeriod}</td>
          <td>${(opt.interestRate * 100).toFixed(2)}%</td>
          <td>${opt.repaidType}</td>
        `
        tbody.appendChild(tr)
      })
    } else {
      tbody.innerHTML = '<tr><td colspan="4" style="text-align:center;">无方案</td></tr>'
    }
    
    document.getElementById('product-detail').style.display = 'block'
  } catch (error) {
    console.error('获取产品详情失败:', error)
    alert('加载产品详情失败')
  }
  // 渲染选项复选框用于批量删除
  const checkboxContainer = document.getElementById('option-checkbox-container')
  checkboxContainer.innerHTML = ''
  if (data.options && data.options.length) {
    data.options.forEach(opt => {
      const label = document.createElement('label')
      label.style.display = 'block'
      label.innerHTML = `
        <input type="checkbox" value="${opt.optionId}"> 额度: ¥${opt.loanAmount}, 期限: ${opt.loanPeriod}月, 利率: ${(opt.interestRate * 100).toFixed(2)}%, 方式: ${opt.repaidType}
      `
      checkboxContainer.appendChild(label)

      // 单独删除按钮（可加在每行）
      const row = tbody.querySelector(`tr:nth-child(${opt.index || 1})`)
      // 或者在表格中加一列，这里简化：在复选框旁加
      const delBtn = document.createElement('button')
      delBtn.textContent = '删除'
      delBtn.style.marginLeft = '10px'
      delBtn.onclick = async () => {
        if (confirm('确定删除该选项？')) {
          await deleteProductOption(opt.optionId)
          showProductDetail(productId) // 刷新详情
        }
      }
      label.appendChild(delBtn)
    })
  }

  // 批量添加选项 - 动态添加行
  document.querySelectorAll('.add-option-row').forEach(btn => btn.remove()) // 清理旧按钮
  const batchTableBody = document.querySelector('#batch-option-table tbody')
  const addRowBtn = batchTableBody.querySelector('button.add-option-row')
  if (addRowBtn) addRowBtn.remove()

  const newRowBtn = document.createElement('button')
  newRowBtn.textContent = '+'
  newRowBtn.className = 'add-option-row'
  newRowBtn.style.marginLeft = '5px'
  newRowBtn.onclick = () => {
    const newRow = document.createElement('tr')
    newRow.innerHTML = `
      <td><input type="number" step="0.01" placeholder="如 10000.00"></td>
      <td><input type="number" placeholder="如 12"></td>
      <td><input type="number" step="0.0001" placeholder="如 0.049"></td>
      <td>
        <select>
          <option value="等额本息">等额本息</option>
          <option value="等额本金">等额本金</option>
          <option value="先息后本">先息后本</option>
          <option value="一次性还本付息">一次性还本付息</option>
        </select>
      </td>
      <td><button class="add-option-row">+</button></td>
    `
    batchTableBody.appendChild(newRow)
    newRow.querySelector('.add-option-row').onclick = () => {
      const tr = newRow.cloneNode(true)
      batchTableBody.appendChild(tr)
      tr.querySelector('.add-option-row').onclick = addRowBtn.onclick
    }
  }
  batchTableBody.lastElementChild?.querySelector('td:last-child')?.appendChild(newRowBtn)

  // 提交批量选项
  document.getElementById('submit-batch-options').onclick = async () => {
    const rows = batchTableBody.querySelectorAll('tr')
    const options = []
    rows.forEach(row => {
      const inputs = row.querySelectorAll('input, select')
      if (inputs.length >= 4) {
        const loanAmount = parseFloat(inputs[0].value)
        const loanPeriod = parseInt(inputs[1].value)
        const interestRate = parseFloat(inputs[2].value)
        const repaidType = inputs[3].value
        if (!isNaN(loanAmount) && !isNaN(loanPeriod) && !isNaN(interestRate)) {
          options.push({ loanAmount, loanPeriod, interestRate, repaidType })
        }
      }
    })
    if (options.length === 0) {
      alert('请填写至少一个有效选项')
      return
    }
    await batchCreateProductOptions(productId, options)
    showProductDetail(productId) // 刷新
  }

  // 批量删除选项
  document.getElementById('batch-delete-options-btn').onclick = async () => {
  const checked = checkboxContainer.querySelectorAll('input[type="checkbox"]:checked')
  const ids = Array.from(checked).map(cb => parseInt(cb.value))
  if (ids.length === 0) {
    alert('请选择要删除的选项')
    return
  }
  if (confirm(`确定删除 ${ids.length} 个选项？`)) {
    await AdminWeb.API_CLIENT.batchDeleteOptions(ids)
    showProductDetail(productId)
  }
}

  // 编辑产品信息
  document.getElementById('edit-product-btn').onclick = () => {
    document.getElementById('edit-product-form').style.display = 'block'
    document.getElementById('edit-productName').value = data.productName || ''
    document.getElementById('edit-description').value = data.description || ''
    document.getElementById('edit-loanUsage').value = data.usage || ''
    document.getElementById('edit-minTerm').value = data.minTerm || ''
    document.getElementById('edit-maxTerm').value = data.maxTerm || ''
    document.getElementById('edit-termStep').value = data.termStep || ''
    document.getElementById('edit-promotionDetails').value = data.promotionDetails || ''
  }

  document.getElementById('cancel-edit-btn').onclick = () => {
    document.getElementById('edit-product-form').style.display = 'none'
  }

  document.getElementById('confirm-edit-btn').onclick = async () => {
    const updateData = {
      productName: document.getElementById('edit-productName').value.trim(),
      description: document.getElementById('edit-description').value.trim(),
      loanUsage: document.getElementById('edit-loanUsage').value.trim(),
      minTerm: parseInt(document.getElementById('edit-minTerm').value) || undefined,
      maxTerm: parseInt(document.getElementById('edit-maxTerm').value) || undefined,
      termStep: parseInt(document.getElementById('edit-termStep').value) || undefined,
      promotionDetails: document.getElementById('edit-promotionDetails').value.trim() || undefined
    }
    // 过滤空值
    Object.keys(updateData).forEach(key => updateData[key] === undefined && delete updateData[key])
    await AdminWeb.API_CLIENT.updateLoanProduct(productId, updateData)
    document.getElementById('edit-product-form').style.display = 'none'
    showProductDetail(productId)
  }
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
// async function initProductList() {
//   refreshProductList()
// }
// // 获取并渲染产品列表（可多次调用）
// async function refreshProductList() {
//   let allProducts = []
//   try {
//     const res = await AdminWeb.API_CLIENT.getAllLoanProducts()
//     if (res.code === 200) allProducts = res.data
//   } catch (err) {
//     console.error('获取产品列表失败', err)
//     allProducts = [] // 确保即使出错也能清空或保留旧数据
//   }

//   // 如果是首次初始化，创建实例
//   if (!productListInstance) {
//     const pageSize = 5
//     const totalPages = Math.ceil(allProducts.length / pageSize)
//     productListInstance = {
//       currentPage: 1,
//       totalPages,
//       allData: all_products,
//       pageSize,
//       render: function() { /*...*/ },
//       loadData: function() { this.render() }
//     }
//     productListInstance.render()
//   } else {
//     // 后续刷新：更新数据并重新渲染当前页
//     productListInstance.allData = allProducts
//     productListInstance.totalPages = Math.ceil(allProducts.length / productListInstance.pageSize)
//     productListInstance.currentPage = Math.min(productListInstance.currentPage, productListInstance.totalPages) || 1
//     productListInstance.render() // 重新渲染
//   }
// }
// ============== 其他功能函数 ===============
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
// 删除单个贷款产品
// async function deleteLoanProduct(productId) {
//   if (!confirm(`确定删除产品 ID=${productId}？`)) return
//   try {
//     await AdminWeb.API_CLIENT.deleteLoanProduct(productId) 
//     alert('删除成功')
//     initProductList()
//   } catch (error) {
//     console.error('删除失败:', error)
//     alert('删除失败：' + (error.message || '请重试'))
//   }
// }
// 上下架单个贷款产品
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
// 批量删除贷款产品
async function batchDeleteLoanProducts(productIds) {
    const url = '/api/loan-products/admin/products/batch-delete'
    const payload = { productIds }
    console.log(`📡 [POST] 批量删除产品: ${url}`, '请求体:', payload)
    try {
        const response = await AdminWeb.API_CLIENT.post(url, payload)
        console.log(`✅ [响应] 批量删除产品成功:`, response)
        alert('批量删除成功')
        return response.data
    } catch (error) {
        console.error(`❌ [错误] 批量删除产品失败:`, error)
        alert('批量删除失败')
    }
}
// 批量创建产品选项
async function batchCreateProductOptions(productId, options) {
    const url = '/api/loan-products/admin/options/batch-create'
    const payload = { productId, options }
    console.log(`[POST] 批量添加选项: ${url}`, '请求体:', payload)
    try {
        const response = await AdminWeb.API_CLIENT.post(url, payload)
        console.log(`✅ [响应] 批量添加选项成功:`, response)
        return response.data
    } catch (error) {
        console.error(`❌ [错误] 批量添加选项失败:`, error)
        alert('添加选项失败')
    }
}
// 批量更新产品选项
async function deleteProductOption(optionId) {
    const url = `/api/loan-products/admin/options/${optionId}`
    console.log(`[DELETE] 删除选项: ${url}`)
    try {
        const response = await AdminWeb.API_CLIENT.request(url, { method: 'DELETE' })
        console.log(`✅ [响应] 选项 ${optionId} 删除成功:`, response)
        return response
    } catch (error) {
        console.error(`❌ [错误] 删除选项 ${optionId} 失败:`, error)
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
      return response.data // 注意：这个接口返回的是全量，不分页！
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
    document.getElementById('user-credit-score').textContent = 
      (userCert.creditScore != null) ? userCert.creditScore : '—'
    const materialsContainer = document.getElementById('user-materials-container')
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
          <td>${app.loanAmount}</td>
          <td>${app.term}</td>
          <td>${app.repaidType}</td>
          <td>${app.interestRate}</td>
          <td>${app.applyTime}</td>
          <td>${app.status}</td>
          <td>${rejectReason}</td>
          <td>${app.reviewTime}</td>
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
          <td>${order.status}</td>
          <td>${order.repaidAmount}</td>
          <td>${order.loanAmount}</td>
          <td>${order.interestRate}</td>
          <td>${order.repaidType}</td>
          <td>${order.loanPeriod}</td>
          <td>${order.term}</td>
          <td>${order.currentTerm}</td>
          <td>${order.contract}</td>
          <td>${order.overdueDays}</td>
          <td>${order.startTime}</td>
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
        tbody.innerHTML = '<tr><td colspan="8" style="text-align:center;">暂无黑名单用户</td></tr>'
        return
      }

      pageData.forEach(item => {
        const row = renderBlacklistRow(item)
        tbody.appendChild(row)
      })

      // 更新分页信息（需对应黑名单的分页元素 ID）
      document.getElementById('blacklist-page-info').textContent = `第 ${this.currentPage} 页，共 ${this.totalPages} 页`
      document.getElementById('prev-blacklist-page').disabled = (this.currentPage <= 1)
      document.getElementById('next-blacklist-page').disabled = (this.currentPage >= this.totalPages)
    },
    loadData: function() {
      this.render()
    }
  }

  blacklistInstance.render()
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
    document.getElementById('user-credit-score').textContent = 
      (userCert.creditScore != null) ? userCert.creditScore : '—'
    const materialsContainer = document.getElementById('user-materials-container')
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
          <td>${app.loanAmount}</td>
          <td>${app.term}</td>
          <td>${app.repaidType}</td>
          <td>${app.interestRate}</td>
          <td>${app.applyTime}</td>
          <td>${app.status}</td>
          <td>${rejectReason}</td>
          <td>${app.reviewTime}</td>
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
          <td>${order.status}</td>
          <td>${order.repaidAmount}</td>
          <td>${order.loanAmount}</td>
          <td>${order.interestRate}</td>
          <td>${order.repaidType}</td>
          <td>${order.loanPeriod}</td>
          <td>${order.term}</td>
          <td>${order.currentTerm}</td>
          <td>${order.contract}</td>
          <td>${order.overdueDays}</td>
          <td>${order.startTime}</td>
        `
        tbody.appendChild(row)
      })
    } else {
      const tbody = document.getElementById('application-table').querySelector('tbody')
      tbody.innerHTML = '<tr><td colspan="8" style="text-align:center;">暂无申请</td></tr>'
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