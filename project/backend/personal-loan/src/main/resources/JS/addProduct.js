
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
//============= 添加贷款项目功能实现函数 =============
获取弹窗中的表单数据
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