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
        // 加载待审核列表
        await fetchAndRenderPendingList()

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
                window.location.href = '/login'
            }
        }
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
        userListInstance.currentPage--;
        userListInstance.loadData();
    }
    })
    document.getElementById('next-user-page').addEventListener('click', () => {
    if (userListInstance && userListInstance.currentPage < userListInstance.totalPages) {
        userListInstance.currentPage++;
        userListInstance.loadData();
    }
    })

    // 产品列表分页
    document.getElementById('prev-product-page').addEventListener('click', () => {
    if (productListInstance && productListInstance.currentPage > 1) {
        productListInstance.currentPage--;
        productListInstance.loadData();
    }
    })
    document.getElementById('next-product-page').addEventListener('click', () => {
    if (productListInstance && productListInstance.currentPage < productListInstance.totalPages) {
        productListInstance.currentPage++;
        productListInstance.loadData();
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

    if (target === 'user-management') {
      initUserList(); // 初始化用户列表
    } else if (target === 'loan-management') {
      initProductList(); // 后面会写
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
let _allPendingApps = [] // 全量数据
let _currentPage = 1
const PENDING_PAGE_SIZE = 5

// 渲染分页信息
function updatePendingPagination(total) {
  const totalPages = Math.ceil(total / PENDING_PAGE_SIZE) || 1;
  const pageInfoEl = document.getElementById('pending-page-info');
  if (pageInfoEl) {
    pageInfoEl.textContent = total === 0 ? '共 0 条' : `第 ${_currentPage} 页，共 ${totalPages} 页`;
  }

  // 控制按钮状态
  const prevBtn = document.getElementById('prev-pending-page');
  const nextBtn = document.getElementById('next-pending-page');
  if (prevBtn) prevBtn.disabled = (_currentPage <= 1);
  if (nextBtn) nextBtn.disabled = (_currentPage >= totalPages);
}

// 加载全部待办申请
async function fetchAndRenderPendingList() {
    try {
        const response = await AdminWeb.API_CLIENT.getPendingApplications()

        if (response.code === 200 && Array.isArray(response.data)) {
            _allPendingApps = response.data;
            _currentPage = 1;
            renderPendingApplications(_allPendingApps.length)
        } else {
            throw new Error(response.message || '数据格式异常')
        }
    } catch (error) {
      console.error('获取待办列表失败:', error)
      document.getElementById('apply-table').querySelector('tbody').innerHTML = '<tr><td colspan="6" style="text-align:center;color:#e74c3c;">加载失败</td></tr>'
      updatePendingPagination(0)
    }
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

        const data = detail.data;

        // 显示详情容器
        document.getElementById('audition-detail').style.display = 'block';

        // 填充用户信息（字段都在 data 根层级）
        document.getElementById('realName').textContent = data.userName || '—';
        document.getElementById('phone').textContent = data.phone || '—';

        // 注册时间：使用 createTime，注意可能为 null
        const registerTime = data.createTime 
            ? new Date(data.createTime).toLocaleString('zh-CN') 
            : '—';
        document.getElementById('register-time').textContent = registerTime;

        // 信誉分：字段名为 creditsScore（注意拼写）
        document.getElementById('credit-score').textContent = 
            (data.creditsScore != null) ? data.creditsScore : '—';

        // 渲染认证材料（根据独立字段构建）
        const materialsContainer = document.getElementById('materials-container');
        const materialMap = {
            idCard: '身份证',
            workCertId: '工作证明',
            triCertId: '三证合一',
            immovableCertId: '不动产证明'
        };

        let html = '';
        for (const [key, label] of Object.entries(materialMap)) {
            const uploaded = data[key] != null; // 如果字段不为 null，视为已上传
            const color = uploaded ? '#27ae60' : '#e74c3c';
            const statusText = uploaded ? '已上传' : '未上传';
            html += `<div class="material-item"><span>${label}</span><span style="color:${color}">${statusText}</span></div>`;
        }
        materialsContainer.innerHTML = html;

        // 填充贷款信息
        document.getElementById('product-name').textContent = data.productName || '—';
        document.getElementById('loan-amount').textContent = 
            data.loanAmount != null 
                ? `¥${Number(data.loanAmount).toLocaleString('zh-CN')}` 
                : '—';
        document.getElementById('loan-term').textContent = data.loanPeriod || '—';
        document.getElementById('term-period').textContent = data.term || '—';
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
  window.location.href = '/addProduct';
})


// 更新单个贷款产品
async function updateLoanProduct(productId, updateData) {
    const url = `/api/loan-products/admin/products/${productId}`;
    console.log(`📡 [PATCH] 更新产品: ${url}`, '请求体:', updateData);
    try {
        const response = await AdminWeb.API_CLIENT.request(url, {
            method: 'PATCH',
            body: JSON.stringify(updateData)
        });
        console.log(`✅ [响应] 产品 ${productId} 更新成功:`, response);
        alert('产品信息更新成功');
        return response.data;
    } catch (error) {
        console.error(`❌ [错误] 更新产品 ${productId} 失败:`, error);
        alert('更新失败');
    }
}
// 删除单个贷款产品
async function deleteLoanProduct(productId) {
    if (!confirm(`确定删除产品 ID=${productId}？此操作不可逆！`)) return;
    const url = `/api/loan-products/admin/products/${productId}`;
    console.log(`📡 [DELETE] 删除产品: ${url}`);
    try {
        const response = await AdminWeb.API_CLIENT.request(url, { method: 'DELETE' });
        console.log(`✅ [响应] 产品 ${productId} 删除成功:`, response);
        alert('删除成功');
        return true;
    } catch (error) {
        console.error(`❌ [错误] 删除产品 ${productId} 失败:`, error);
        alert('删除失败');
    }
}
// 批量删除贷款产品
async function batchDeleteLoanProducts(productIds) {
    const url = '/api/loan-products/admin/products/batch-delete';
    const payload = { productIds };
    console.log(`📡 [POST] 批量删除产品: ${url}`, '请求体:', payload);
    try {
        const response = await AdminWeb.API_CLIENT.post(url, payload);
        console.log(`✅ [响应] 批量删除产品成功:`, response);
        alert('批量删除成功');
        return response.data;
    } catch (error) {
        console.error(`❌ [错误] 批量删除产品失败:`, error);
        alert('批量删除失败');
    }
}
// 批量创建产品选项
async function batchCreateProductOptions(productId, options) {
    const url = '/api/loan-products/admin/options/batch-create';
    const payload = { productId, options };
    console.log(`📡 [POST] 批量添加选项: ${url}`, '请求体:', payload);
    try {
        const response = await AdminWeb.API_CLIENT.post(url, payload);
        console.log(`✅ [响应] 批量添加选项成功:`, response);
        return response.data;
    } catch (error) {
        console.error(`❌ [错误] 批量添加选项失败:`, error);
        alert('添加选项失败');
    }
}
// 批量更新产品选项
async function deleteProductOption(optionId) {
    const url = `/api/loan-products/admin/options/${optionId}`;
    console.log(`📡 [DELETE] 删除选项: ${url}`);
    try {
        const response = await AdminWeb.API_CLIENT.request(url, { method: 'DELETE' });
        console.log(`✅ [响应] 选项 ${optionId} 删除成功:`, response);
        return response;
    } catch (error) {
        console.error(`❌ [错误] 删除选项 ${optionId} 失败:`, error);
    }
}
// ============== 贷款项目展示 ===============
let productListInstance = null;

function renderProductRow(product) {
  const tr = document.createElement('tr');
  tr.setAttribute('data-product-id', product.productId);
  
  // 截断长文本
  const usage = (product.usage || '').length > 15 
    ? product.usage.substring(0, 15) + '...' 
    : product.usage;

  tr.innerHTML = `
    <td>${product.productId}</td>
    <td>${product.productName || '—'}</td>
    <td>${product.description || '—'}</td>
    <td>${usage}</td>
    <td>${product.updateTime ? new Date(product.updateTime).toLocaleString() : '—'}</td>
    <td>${product.createTime ? new Date(product.createTime).toLocaleString() : '—'}</td>
    <td><button class="view-prod-btn">查看详情</button></td>
  `;
  
  tr.querySelector('.view-prod-btn').addEventListener('click', (e) => {
    e.stopPropagation();
    showProductDetail(product.productId);
  });
  
  return tr;
}

async function showProductDetail(productId) {
  try {
    const detail = await AdminWeb.API_CLIENT.getLoanProductById(productId);
    if (detail.code !== 200) throw new Error(detail.message);
    
    const data = detail.data;
    document.getElementById('prod-name').textContent = data.productName || '—';
    document.getElementById('prod-status').textContent = data.status || '—';
    document.getElementById('prod-term').textContent = 
      `${data.minTerm || 0} ~ ${data.maxTerm || 0} 月 (步长: ${data.termStep || 1})`;
    document.getElementById('prod-promo').textContent = data.promotionDetails || '—';
    
    // 渲染选项
    const tbody = document.getElementById('prod-options-table').querySelector('tbody');
    tbody.innerHTML = '';
    if (data.options && data.options.length) {
      data.options.forEach(opt => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
          <td>¥${opt.loanAmount.toLocaleString()}</td>
          <td>${opt.loanPeriod}</td>
          <td>${(opt.interestRate * 100).toFixed(2)}%</td>
          <td>${opt.repaidType}</td>
        `;
        tbody.appendChild(tr);
      });
    } else {
      tbody.innerHTML = '<tr><td colspan="4" style="text-align:center;">无方案</td></tr>';
    }
    
    document.getElementById('product-detail').style.display = 'block';
  } catch (error) {
    console.error('获取产品详情失败:', error);
    alert('加载产品详情失败');
  }
}

async function initProductList() {
  if (productListInstance) return;
  
  let allProducts = [];
  try {
    const res = await AdminWeb.API_CLIENT.getAllLoanProducts();
    if (res.code === 200) allProducts = res.data;
  } catch (err) {
    console.error('获取产品列表失败', err);
  }

  const pageSize = 5;
  const totalPages = Math.ceil(allProducts.length / pageSize);

  productListInstance = {
    currentPage: 1,
    totalPages: totalPages,
    allData: allProducts,
    pageSize: pageSize,
    render: function() {
      const start = (this.currentPage - 1) * this.pageSize;
      const pageData = this.allData.slice(start, start + this.pageSize);
      const tbody = document.getElementById('product-table').querySelector('tbody');
      tbody.innerHTML = '';
      
      if (pageData.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" style="text-align:center;">暂无产品</td></tr>';
        return;
      }
      
      pageData.forEach(prod => {
        const row = renderProductRow(prod);
        tbody.appendChild(row);
      });
      
      document.getElementById('product-page-info').textContent = 
        `第 ${this.currentPage} 页，共 ${this.totalPages} 页`;
      document.getElementById('prev-product-page').disabled = (this.currentPage <= 1);
      document.getElementById('next-product-page').disabled = (this.currentPage >= this.totalPages);
    },
    loadData: function() {
      this.render();
    }
  };
  
  productListInstance.render();
}
// ============== 添加贷款项目标签页处理 ===============
// ============= 添加贷款项目功能实现函数 =============
// 获取弹窗中的表单数据
// function handleNewLoanProductData() {
//     // 获取基础信息
//     const productName = document.getElementById('productName').value.trim()
//     const description = document.getElementById('description').value.trim()
//     const loanUsage = document.getElementById('loanUsage').value.trim()
//     const minTerm = parseInt(document.getElementById('minTerm').value) || 0
//     const maxTerm = parseInt(document.getElementById('maxTerm').value) || 0
//     const termStep = parseInt(document.getElementById('termStep').value) || 0
//     const promotionDetails = document.getElementById('promotionDetails').value.trim()

//     // 获取选项表格数据
//     const options = []
//     const tableRows = document.querySelectorAll('#option-table tbody tr')
    
//     tableRows.forEach(row => {
//         const inputs = row.querySelectorAll('input[type="text"]')
//         if (inputs.length >= 4) { // 确保有四个输入框
//             const option = {
//                 loanAmount: parseFloat(inputs[0].value) || 0,
//                 interestRate: parseFloat(inputs[1].value) || 0,
//                 loanPeriod: parseInt(inputs[2].value) || 0,
//                 repaidType: inputs[3].value.trim()
//             }
//             options.push(option)
//         }
//     })
//     // 构建完整的请求数据
//     const productData = {
//         productName,
//         description,
//         loanUsage,
//         minTerm,
//         maxTerm,
//         termStep,
//         promotionDetails,
//         options
//     }
//     return productData
// }
// //表单提交按钮事件绑定
// document.getElementById('add-loan-product').addEventListener('click', async function() {
//     try {
//         // 验证必填字段
//         const productName = document.getElementById('productName').value.trim()
//         if (!productName) {
//             alert('请输入产品名称')
//             return
//         }
//         const options = document.querySelectorAll('#option-table tbody tr')
//         if (options.length === 0) {
//             alert('至少需要添加一个贷款选项')
//             return
//         }
//         // 获取并验证数据
//         const productData = handleNewLoanProductData()
//         // 调用API客户端提交数据
//         const response = await API_CLIENT.addLoanProduct(productData)
//         console.log('新增贷款产品请求数据:', productData)
//         alert('新增贷款产品请求数据:', productData)
//         console.log('新增贷款产品成功:', response)
//         alert('贷款产品添加成功！')
//         // 关闭弹窗
//         document.getElementById('add-new-product').style.display = 'none'
//         // 重置表单
//         resetAddLoanProductForm()
//     } catch (error) {
//         console.error('新增贷款产品失败:', error)
//         alert('添加失败，请检查输入信息或稍后重试')
//     }
// })
// // 提交后重置添加贷款产品表单函数 
// function resetAddLoanProductForm() {
//     // 重置基础输入框
//     document.getElementById('productName').value = ''
//     document.getElementById('description').value = ''
//     document.getElementById('loanUsage').value = ''
//     document.getElementById('minTerm').value = ''
//     document.getElementById('maxTerm').value = ''
//     document.getElementById('termStep').value = ''
//     document.getElementById('promotionDetails').value = ''
    
//     // 重置表格，只保留初始行
//     const tbody = document.querySelector('#option-table tbody')
//     tbody.innerHTML = `
//         <tr>  
//             <td><input type="text" placeholder="请输入贷款额度"></td>
//             <td><input type="text" placeholder="请输入贷款期限"></td>
//             <td><input type="text" placeholder="请输入年化利率"></td>
//             <td><input type="text" placeholder="请输入还款方式"></td>
//             <td><button class="delete-btn">删除</button></td>
//         </tr>
//     `
//     // 重新绑定删除按钮事件
//     document.querySelectorAll('.delete-btn').forEach(btn => {
//         btn.addEventListener('click', function() {
//             this.closest('tr').remove()
//         })
//     })
// }



// ==================== 用户管理面板处理 ====================
let userListInstance = null;

// 渲染单行用户
function renderUserRow(user) {
  const tr = document.createElement('tr');
  tr.setAttribute('data-user-id', user.userId);
  
  // 是否在线（示例：随机或根据字段）
  const isOnline = '—'; // 假设后端未提供，在线状态需额外字段
  // 目前借贷情况（示例）
  const loanStatus = user.loanStatus || '无借贷';

  tr.innerHTML = `
    <td>${user.userId}</td>
    <td>${user.userName || '—'}</td>
    <td>${isOnline}</td>
    <td>${loanStatus}</td>
    <td>${user.totalTransactionCount || 0}</td>
    <td>¥${(user.totalLoanAmount || 0).toLocaleString()}</td>
    <td>¥${(user.totalRepaidAmount || 0).toLocaleString()}</td>
    <td><button class="view-user-btn">查看详情</button></td>
  `;
  
  // 绑定点击事件（委托也可，这里直接绑）
  tr.querySelector('.view-user-btn').addEventListener('click', (e) => {
    e.stopPropagation(); // 防止触发行点击
    showUserDetail(user.userId);
  });
  
  // 行点击也看详情（可选）
  // tr.addEventListener('click', () => showUserDetail(user.userId));
  
  return tr;
}
// 初始化用户列表（在 switchToPanel 中调用）
async function initUserList() {
  if (userListInstance) return; // 避免重复初始化
  
  const fetchData = async (page, pageSize) => {
    const response = await AdminWeb.API_CLIENT.getUserStats();
    if (response.code === 200) {
      return response.data; // 注意：这个接口返回的是全量，不分页！
    } else {
      throw new Error(response.message);
    }
  };

  // 分页
  let allUsers = [];
  try {
    const res = await AdminWeb.API_CLIENT.getUserStats();
    if (res.code === 200) allUsers = res.data;
  } catch (err) {
    console.error('获取用户列表失败', err);
  }

  // 自定义分页逻辑
  const pageSize = 5;
  const totalPages = Math.ceil(allUsers.length / pageSize);

  userListInstance = {
    currentPage: 1,
    totalPages: totalPages,
    allData: allUsers,
    pageSize: pageSize,
    render: function() {
      const start = (this.currentPage - 1) * this.pageSize;
      const pageData = this.allData.slice(start, start + this.pageSize);
      const tbody = document.getElementById('user-table').querySelector('tbody');
      tbody.innerHTML = '';
      
      if (pageData.length === 0) {
        tbody.innerHTML = '<tr><td colspan="8" style="text-align:center;">暂无用户</td></tr>';
        return;
      }
      
      pageData.forEach(user => {
        const row = renderUserRow(user);
        tbody.appendChild(row);
      });
      
      // 更新分页信息
      document.getElementById('user-page-info').textContent = 
        `第 ${this.currentPage} 页，共 ${this.totalPages} 页`;
      document.getElementById('prev-user-page').disabled = (this.currentPage <= 1);
      document.getElementById('next-user-page').disabled = (this.currentPage >= this.totalPages);
    },
    loadData: function() {
      this.render();
    }
  };
  
  userListInstance.render();
}
// 显示用户详情
async function showUserDetail(userId) {
  try {
    const detail = await AdminWeb.API_CLIENT.getUserDetail(userId);
    if (detail.code !== 200) throw new Error(detail.message || '获取失败');
    
    const data = detail.data;
    // 填充基本信息
    document.getElementById('user-real-name').textContent = data.userName || '—';
    document.getElementById('user-phone').textContent = data.phone || '—';
    document.getElementById('user-register-time').textContent = 
      data.createTime ? new Date(data.createTime).toLocaleString() : '—';
    document.getElementById('user-credit-score').textContent = 
      (data.creditScore != null) ? data.creditScore : '—';
    
    // 认证材料（简化：只显示是否上传）
    const materialsContainer = document.getElementById('user-materials-container');
    const materialMap = {
      idCard: '身份证',
      workCertId: '工作证明',
      triCertId: '三证合一',
      immovableCertId: '不动产证明'
    };
    let html = '';
    for (const [key, label] of Object.entries(materialMap)) {
      const uploaded = data[key] != null;
      const color = uploaded ? '#27ae60' : '#e74c3c';
      const statusText = uploaded ? '已上传' : '未上传';
      html += `<div class="material-item"><span>${label}</span><span style="color:${color}">${statusText}</span></div>`;
    }
    materialsContainer.innerHTML = html;
    
    // 显示模态框
    document.getElementById('user-detail').style.display = 'block';
  } catch (error) {
    console.error('获取用户详情失败:', error);
    alert('加载用户详情失败');
  }
}


// 通过申请ID获取用户申请详情
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


// ========================= 可复用部件 ================================
/**
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
    this.container = document.getElementById(containerId);
    this.tbody = document.getElementById(tableBodyId);
    this.renderRow = renderRow;
    this.fetchData = fetchData;
    this.detailHandler = detailHandler;
    this.pageSize = pageSize;
    this.currentPage = 1;

    this.initPagination();
    this.bindEvents();
    this.loadData();
  }

  async loadData() {
    try {
      const data = await this.fetchData(this.currentPage, this.pageSize);
      this.render(data.records || data); // 兼容两种结构
    } catch (err) {
      console.error('加载失败', err);
      this.tbody.innerHTML = `<tr><td colspan="6">加载失败</td></tr>`;
    }
  }

  render(records) {
    this.tbody.innerHTML = '';
    records.forEach(item => {
      const row = this.renderRow(item);
      row.addEventListener('click', () => this.detailHandler(item));
      this.tbody.appendChild(row);
    });
  }

  initPagination() {
    this.paginationEl = this.container.querySelector('.pagination');
    if (!this.paginationEl) {
      this.paginationEl = document.createElement('div');
      this.paginationEl.className = 'pagination';
      this.container.appendChild(this.paginationEl);
    }
    this.updatePagination();
  }

  updatePagination() {
    this.paginationEl.innerHTML = `
      <button id="prev-page" ${this.currentPage <= 1 ? 'disabled' : ''}>上一页</button>
      <span>第 ${this.currentPage} 页</span>
      <button id="next-page">下一页</button>
    `;
  }

  bindEvents() {
    this.container.addEventListener('click', (e) => {
      if (e.target.id === 'prev-page' && this.currentPage > 1) {
        this.currentPage--;
        this.loadData();
        this.updatePagination();
      }
      if (e.target.id === 'next-page') {
        this.currentPage++;
        this.loadData();
        this.updatePagination();
      }
    });
  }
}


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