-- 项目关键查询的EXPLAIN分析

-- 1. 贷款产品时间范围查询
EXPLAIN SELECT
    id,
    product_name,
    description,
    loan_usage, 
    status,
    min_amount,
    max_amount,
    create_time,
    update_time
FROM loan_products
WHERE create_time >= '2026-01-01'
  AND create_time < DATE_ADD('2026-01-31', INTERVAL 1 DAY)
ORDER BY create_time DESC, update_time DESC;

-- 2. 用户贷款申请列表查询（带产品信息）
EXPLAIN SELECT
    a.id,
    lp.product_name,
    a.loan_amount,
    a.status,
    a.apply_time,
    a.reject_reason
FROM loan_applications a
JOIN loan_products lp ON a.product_id = lp.id
WHERE a.user_id = 1
ORDER BY a.apply_time DESC;

-- 3. 订单列表查询
EXPLAIN SELECT
    id,
    loan_amount,
    status,
    start_time,
    term,
    current_term,
    overdue_days
FROM orders
WHERE user_id = 1
ORDER BY start_time DESC;

-- 4. 贷款申请详情查询
EXPLAIN SELECT
    u.id AS user_id,
    u.user_name,
    u.avatar,
    u.phone,
    u.password, 
    u.role,
    u.create_time AS user_create_time,
    u.update_time AS user_update_time,

    uc.user_id AS cert_user_id,
    uc.id_card,
    uc.credit_score,
    uc.bank_card_id,
    uc.work_cert_id,
    uc.tri_cert_id,
    uc.immovable_cert_id,

    a.id AS app_id,
    a.user_id AS app_user_id,
    a.product_id,
    a.status,
    a.loan_amount,
    a.interest_rate,
    a.loan_period,
    a.term,
    a.repaid_type,
    a.reject_reason,
    a.apply_time,
    a.review_time
FROM loan_applications a
INNER JOIN users u ON a.user_id = u.id
LEFT JOIN user_certification uc ON u.id = uc.user_id
WHERE a.id = 1;

-- 5. 待审批列表查询
EXPLAIN SELECT
    la.id AS applicationId,
    u.user_name AS userName,
    lp.product_name AS productName,
    la.loan_amount AS loanAmount,
    la.loan_period AS loanPeriod,
    la.term AS term,
    la.status AS status,
    la.apply_time AS applyTime
FROM loan_applications la
JOIN users u ON la.user_id = u.id
JOIN loan_products lp ON la.product_id = lp.id
WHERE la.status = 'AI拒绝'
ORDER BY la.apply_time DESC;

-- 6. 已完成审批列表查询
EXPLAIN SELECT
    la.id AS applicationId,
    u.user_name AS userName,
    lp.product_name AS productName,
    la.loan_amount AS loanAmount,
    la.loan_period AS loanPeriod,
    la.term AS term,
    la.status AS status,
    la.apply_time AS applyTime
FROM loan_applications la
JOIN users u ON la.user_id = u.id
JOIN loan_products lp ON la.product_id = lp.id
WHERE la.status = '已通过' OR la.status = '人工拒绝'
ORDER BY la.apply_time DESC;

-- 7. 按信用分搜索用户
EXPLAIN SELECT
    u.id,
    u.user_name AS userName,
    u.phone,
    u.avatar,
    u.role,
    uc.credit_score AS creditScore
FROM users u
LEFT JOIN user_certification uc ON u.id = uc.user_id
WHERE uc.credit_score > 600
ORDER BY uc.credit_score DESC;

-- 8. 用户详情查询
EXPLAIN SELECT
    u.id AS user_id,
    u.user_name,
    u.avatar,
    u.phone,
    u.password,
    u.role,
    u.create_time AS user_create_time,
    u.update_time AS user_update_time,

    uc.user_id AS cert_user_id,
    uc.id_card,
    uc.credit_score,
    uc.bank_card_id,
    uc.work_cert_id,
    uc.tri_cert_id,
    uc.immovable_cert_id,

    la.id AS app_id,
    la.user_id AS app_user_id,
    la.product_id AS app_product_id,
    la.status AS app_status,
    la.loan_amount AS app_loan_amount,
    la.interest_rate AS app_interest_rate,
    la.loan_period AS app_loan_period,
    la.term AS app_term,
    la.repaid_type AS app_repaid_type,
    la.reject_reason AS app_reject_reason,
    la.apply_time AS app_apply_time,
    la.review_time AS app_review_time,

    o.id AS order_id,
    o.user_id AS order_user_id,
    o.product_id AS order_product_id,
    o.status AS order_status,
    o.repaid_amount,
    o.loan_amount AS order_loan_amount,
    o.interest_rate AS order_interest_rate,
    o.repaid_type AS order_repaid_type,
    o.loan_period AS order_loan_period,
    o.term AS order_term,
    o.current_term,
    o.contract,
    o.overdue_days,
    o.start_time
FROM users u
LEFT JOIN user_certification uc ON u.id = uc.user_id
LEFT JOIN loan_applications la ON u.id = la.user_id
LEFT JOIN orders o ON u.id = o.user_id
WHERE u.id = 1
ORDER BY la.apply_time DESC, o.start_time DESC;

-- ========== 以下为遗漏的查询分析 ==========

-- 9. 按ID查询单个产品
EXPLAIN SELECT
    id,
    product_name as productName,
    description,
    loan_usage as loanUsage,
    status as status,
    min_term as minTerm,
    max_term as maxTerm,
    term_step as termStep,
    min_amount AS minAmount,
    max_amount AS maxAmount,
    promotion_details as promotionDetails,
    create_time as createTime,
    update_time as updateTime
FROM loan_products
WHERE id = 1;

-- 10. 查询所有产品
EXPLAIN SELECT
    id,
    product_name as productName,
    description,
    loan_usage as loanUsage,
    status as status,
    min_term as minTerm,
    max_term as maxTerm,
    term_step as termStep,
    min_amount AS minAmount,
    max_amount AS maxAmount,
    promotion_details as promotionDetails,
    create_time as createTime,
    update_time as updateTime
FROM loan_products
ORDER BY create_time DESC, update_time DESC;

-- 11. 查询上架产品
EXPLAIN SELECT
    id,
    product_name as productName,
    description,
    loan_usage as loanUsage,
    min_term as minTerm,
    max_term as maxTerm,
    term_step as termStep,
    min_amount AS minAmount,
    max_amount AS maxAmount,
    promotion_details as promotionDetails,
    create_time as createTime,
    update_time as updateTime
FROM loan_products
WHERE status = '上架中'
ORDER BY update_time DESC, create_time DESC;

-- 12. 按产品名称模糊搜索上架产品
EXPLAIN SELECT
    id,
    product_name AS productName,
    description,
    loan_usage AS loanUsage,
    status,
    min_term AS minTerm,
    max_term AS maxTerm,
    term_step AS termStep,
    min_amount AS minAmount,
    max_amount AS maxAmount,
    promotion_details AS promotionDetails,
    create_time AS createTime,
    update_time AS updateTime
FROM loan_products
WHERE product_name LIKE CONCAT('%', '消费', '%')
  AND status = '上架中';

-- 13. 按ID查询贷款申请
EXPLAIN SELECT
    id,
    user_id as userId,
    product_id as productId,
    status,
    loan_amount as loanAmount,
    interest_rate as interestRate,
    loan_period as loanPeriod,
    term as term,
    repaid_type as repaidType,
    reject_reason as rejectReason,
    apply_time as applyTime,
    review_time as reviewTime
FROM loan_applications
WHERE id = 1;

-- 14. 按用户ID查询所有申请（简单版）
EXPLAIN SELECT
    id,
    user_id as userId,
    product_id as productId,
    status,
    loan_amount as loanAmount,
    interest_rate as interestRate,
    loan_period as loanPeriod,
    term as term,
    repaid_type as repaidType,
    reject_reason as rejectReason,
    apply_time as applyTime,
    review_time as reviewTime
FROM loan_applications
WHERE user_id = 1
ORDER BY apply_time DESC;

-- 15. 检查产品是否被贷款申请引用
EXPLAIN SELECT COUNT(*)
FROM loan_applications
WHERE product_id = 1;

-- 16. 批量检查产品是否被贷款申请引用
EXPLAIN SELECT COUNT(*)
FROM loan_applications
WHERE product_id IN (1, 2, 3);

-- 17. 按ID查询用户
EXPLAIN SELECT
    id,
    user_name as userName,
    avatar,
    password,
    phone,
    role,
    create_time as createTime,
    update_time as updateTime
FROM users
WHERE id = 1;

-- 18. 查询所有普通用户（role=0）
EXPLAIN SELECT
    id,
    user_name as userName,
    avatar,
    password,
    phone,
    role,
    create_time as createTime,
    update_time as updateTime
FROM users
WHERE role = 0;

-- 19. 按手机号查询用户
EXPLAIN SELECT
    id,
    user_name,
    avatar,
    password,
    phone,
    role,
    create_time,
    update_time
FROM users
WHERE phone = '13800138000';

-- 20. 按ID查询订单
EXPLAIN SELECT
    id,
    user_id,
    product_id,
    status,
    repaid_amount,
    loan_amount,
    interest_rate,
    repaid_type,
    loan_period,
    term,
    current_term,
    contract,
    overdue_days,
    start_time
FROM orders
WHERE id = 1;

-- 21. 按订单ID和用户ID查询订单
EXPLAIN SELECT
    id,
    user_id,
    product_id,
    status,
    repaid_amount,
    loan_amount,
    interest_rate,
    repaid_type,
    loan_period,
    term,
    current_term,
    contract,
    overdue_days,
    start_time
FROM orders
WHERE id = 1 AND user_id = 1;

-- 22. 查询用户所有订单（简单版）
EXPLAIN SELECT
    id,
    user_id,
    product_id,
    status,
    repaid_amount,
    loan_amount,
    interest_rate,
    repaid_type,
    loan_period,
    term,
    current_term,
    contract,
    overdue_days,
    start_time
FROM orders
WHERE user_id = 1;

-- 23. 检查产品是否被订单引用
EXPLAIN SELECT COUNT(*)
FROM orders
WHERE product_id = 1;

-- 24. 批量检查产品是否被订单引用
EXPLAIN SELECT COUNT(*)
FROM orders
WHERE product_id IN (1, 2, 3);

-- 25. 按ID查询贷款选项
EXPLAIN SELECT
    id as optionId,
    product_id as productId,
    loan_period as loanPeriod,
    interest_rate as interestRate,
    repaid_type as repaidType,
    create_time as createTime,
    update_time as updateTime
FROM loan_options
WHERE id = 1;

-- 26. 按产品ID查询所有选项
EXPLAIN SELECT
    id as optionId,
    product_id as productId,
    loan_period as loanPeriod,
    interest_rate as interestRate,
    repaid_type as repaidType,
    create_time as createTime,
    update_time as updateTime
FROM loan_options
WHERE product_id = 1;