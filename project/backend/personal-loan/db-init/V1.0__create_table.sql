
CREATE TABLE users(  
    id int NOT NULL PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary Key',
    user_name VARCHAR(16),
    avatar VARCHAR(255) COMMENT '头像，存路径，二期工程',
    password VARCHAR(255) COMMENT '密码',
    phone CHAR(11) COMMENT '手机号',
    area VARCHAR(16) COMMENT '地区',
    role INT NOT NULL DEFAULT 0 COMMENT '用户权限',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT '用户表';

CREATE Table black_list(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    user_id INT NOT NULL COMMENT '关联用户ID',
    black_level TINYINT COMMENT '黑名单等级',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    remove_time DATETIME COMMENT '移除时间',
    FOREIGN KEY (user_id) REFERENCES users(id)
)COMMENT '黑名单表';

CREATE TABLE work_cert(
      work_cert_id INT PRIMARY KEY AUTO_INCREMENT COMMENT '工作证明ID',
      employment_cert_path VARCHAR(255) COMMENT '在职证明图片路径',
      salary_cert_path VARCHAR(255) COMMENT '收入证明图片路径'
)COMMENT '工作认证表';

/* 二期工程 */
CREATE TABLE tri_cert(
     tri_cert_id INT PRIMARY KEY AUTO_INCREMENT COMMENT '第三方证明ID',
     social_security_path VARCHAR(255) COMMENT '社保证明，存路径',
     credit_report_path VARCHAR(255) COMMENT '征信报告，存路径（）'
)COMMENT '第三方认证表';

CREATE TABLE immovables_cert(
    immovable_cert_id INT PRIMARY KEY AUTO_INCREMENT COMMENT '不动产证明ID',
    property_cert_path VARCHAR(255) COMMENT '房产证，存路径',
    car_cert_path VARCHAR(255) COMMENT '车产证明，存路径',
    total_value INT COMMENT '总资产值'
)COMMENT '不动产认证表';


/* bank_card_id 改为 VARCHAR(19) 国内借记卡有16 19位 ,添加了real_name字段 */
CREATE TABLE user_certification(
    user_id INT PRIMARY KEY COMMENT '用户ID',
    real_name VARCHAR(16) COMMENT '真实姓名',
    id_card CHAR(18) COMMENT '身份证号',
    credit_score INT DEFAULT 0 COMMENT '信誉分',
    max_loan_amount DECIMAL(12,2) COMMENT '最高额度',
    bank_card_id VARCHAR(19) COMMENT '银行卡号',
    work_cert_id INT UNIQUE COMMENT '工作证明',
    tri_cert_id INT UNIQUE COMMENT '第三方证明',
    immovable_cert_id INT UNIQUE COMMENT '不动产证明',
    Foreign Key (user_id) REFERENCES users(id),
    Foreign Key (work_cert_id) REFERENCES work_cert(work_cert_id),
    Foreign Key (tri_cert_id) REFERENCES tri_cert(tri_cert_id),
    Foreign Key (immovable_cert_id) REFERENCES immovables_cert(immovable_cert_id)
)COMMENT '用户认证表';

/*
* 评论功能？二期工程
新增了最大和最小额度
*/
CREATE TABLE loan_products(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    product_name VARCHAR(50) COMMENT '产品名称',
    description VARCHAR(255) COMMENT '产品描述',
    loan_usage VARCHAR(100) COMMENT '贷款用途(目前不需要)',
    status VARCHAR(100) COMMENT '状态：上架中，已下架',
    min_term INT COMMENT '最短期数',
    max_term INT COMMENT '最长期数',
    term_step INT COMMENT '期限步长',
    min_amount DECIMAL(12,2) NOT NULL COMMENT '最小贷款金额',
    max_amount DECIMAL(12,2) NOT NULL COMMENT '最大贷款金额',
    promotion_details VARCHAR(255) COMMENT '促销详情',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
)COMMENT '贷款产品表';

/* 
* loan_period是否应该改为 DECIMAL？例如期限6个月，0.5年
 */
CREATE TABLE loan_options(
    id INT PRIMARY KEY AUTO_INCREMENT,
    product_id INT NOT NULL,
    interest_rate DECIMAL(6,4) COMMENT '利率',
    loan_period INT COMMENT '年限',
    repaid_type VARCHAR(50) COMMENT '还款方式,等额本息, 等额本金, 先息后本, 一次性还本付息',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (product_id) REFERENCES loan_products(id)
)COMMENT '贷款选项表';


/*
* 应该添加application_id字段，消除重复下单风险
*/
CREATE TABLE orders(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    user_id INT NOT NULL COMMENT '用户ID',
    product_id INT NOT NULL COMMENT '产品ID',
    status VARCHAR(50) COMMENT '贷款状态,正常,已逾期,已完成',
    repaid_amount DECIMAL(12,2) COMMENT '已还金额',
    loan_amount DECIMAL(12,2) COMMENT '总贷款金额',
    interest_rate DECIMAL(6,4) COMMENT '利率',
    repaid_type VARCHAR(50) NOT NULL,
    loan_period INT COMMENT '贷款期限',
    term INT COMMENT '实际贷款期数',
    current_term INT COMMENT '当前期数',
    contract VARCHAR(255) COMMENT '合同路径',
    overdue_days INT COMMENT '逾期天数',
    start_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '开始日期',
    Foreign Key (user_id) REFERENCES users(id),
    Foreign Key (product_id) REFERENCES loan_products(id)
)COMMENT '用户已贷款项目表';


CREATE TABLE loan_applications(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT COMMENT '申请ID',
    user_id INT NOT NULL COMMENT '用户id',
    product_id INT NOT NULL COMMENT '产品id',
    status VARCHAR(50) COMMENT '申请状态,审核中,已通过,AI拒绝,人工拒绝,已取消',
    loan_amount DECIMAL(12,2) NOT NULL COMMENT '申请金额',
    interest_rate DECIMAL(6,4) COMMENT '申请时的利率 (审核后填写)',
    loan_period INT NOT NULL COMMENT '年限',
    term INT COMMENT '实际贷款期数',
    repaid_type VARCHAR(50) NOT NULL COMMENT '还款方式,等额本息, 等额本金, 先息后本, 一次性还本付息',
    reject_reason VARCHAR(255) COMMENT '拒绝原因',
    apply_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    review_time DATETIME COMMENT '审核完成时间',
    FOREIGN KEY (product_id) REFERENCES loan_products(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
)COMMENT '贷款申请表';

CREATE TABLE outbox_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增主键',
    message_id VARCHAR(64) NOT NULL UNIQUE COMMENT '全局唯一ID，如 UUID 或 loan_app_123_1712345678901',
    business_type VARCHAR(50) NOT NULL COMMENT '业务类型，如 LOAN_APPLICATION',
    business_id BIGINT NOT NULL COMMENT '关联的业务主键，如 loan_application.id',
    topic VARCHAR(255) NOT NULL COMMENT 'RabbitMQ routing key，如 loan.application.submitted',
    payload JSON NOT NULL COMMENT '消息体，通常是 LoanApplication 的 JSON 表示',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING(待发送), SENT(已发送), FAILED(发送失败)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    sent_at DATETIME NULL COMMENT '实际发送时间',
    INDEX idx_status_created (status, created_at),
    INDEX idx_business (business_type, business_id),
    INDEX idx_message_id (message_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='本地消息表（Outbox Pattern）';

CREATE TABLE payment_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增主键',
    tx_id VARCHAR(64) NOT NULL UNIQUE COMMENT '支付网关交易流水号/支付订单号',
    order_id BIGINT NOT NULL COMMENT '关联订单ID',
    amount DECIMAL(12,2) NOT NULL COMMENT '支付金额',
    status VARCHAR(20) NOT NULL COMMENT '支付状态: SUCCESS/FAILED',
    paid_at DATETIME NULL COMMENT '支付完成时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_order_id (order_id),
    INDEX idx_status_created (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付记录表';

CREATE TABLE processed_message (
   message_id VARCHAR(64) PRIMARY KEY,
   business_type VARCHAR(50) NOT NULL,
   business_id BIGINT NOT NULL,
   processed_at DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT '幂等性记录表';

CREATE TABLE notifications(
    id BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT COMMENT '通知ID',
    user_id INT NOT NULL COMMENT '用户ID',
    business_id BIGINT COMMENT '关联业务ID',
    business_type VARCHAR(50) COMMENT '业务类型,如LOAN_APPLICATION, REPAYMENT',
    title VARCHAR(100) NOT NULL COMMENT '通知标题',
    content VARCHAR(255) NOT NULL COMMENT '通知内容',
    read_flag TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读 0未读 1已读',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    read_at DATETIME COMMENT '已读时间',
    INDEX idx_notifications_user_created (user_id, created_at),
    INDEX idx_notifications_user_read (user_id, read_flag, created_at),
    INDEX idx_notifications_business (business_type, business_id),
    FOREIGN KEY (user_id) REFERENCES users(id)
) COMMENT '站内通知表';

/* 
* 插入管理员，明文密码分别是 @Gcc1234  @Zff1234  @Wting1234  @Qyx1234
 */
INSERT INTO users (user_name, password, phone, role) VALUES
('gcc', '$2a$10$tgL5vVw82nDYyqMhiS0BJeYyl.Ar7ox6N6RMEUmggBD1Tvo5Z0FiK', '17777777777', 1),
('zff', '$2a$10$60kyX/HezEaHChvKpZB9j.fByPl8is6etEiay0oqusri6zA5/Pl1C', '19999999999', 1),
('wt', '$2a$10$ODq7dU6Bvz8Ks9b7mNGt5OLExUV3gmY2wPHXrXICJi0j.pjCfyuYC', '18888888888', 1),
('qyx', '$2a$10$UDcyeEjze0A3K8pNSzMoFOyoaN.G3/v/ilQpMv2/6J28.6GxvhhTK', '16666666666', 1)

/* 
* 插入普通用户,明文密码分别是 WangFang@2025 ZhangWei@2025 LiMing@2025 Alice2025!
 */
INSERT INTO users(user_name, password, phone, role) VALUES
('王芳', '$2a$10$LjWKXLLidSLSdr2iS6.RZe785XyAy.LNpO2AlHKpj4x0WLLqOafO.', '13100001111', 0),
('张伟', '$2a$10$J2gSAyblu0zxSMiKMXamF.wjn2z4yFDJNfBclb/CyPtQyYWY08eQe', '15098765432', 0),
('李明', '$2a$10$n2eK0EQQBpBexTGD7KqMPuMI7bU149gMwq1W55.l48.pmO1NySmga', '13912345678', 0),
('Alice', '$2a$10$MdGHJwB6IbBPrl7dbogSMOV2qsMkKT/u2Nd9DqF39C5EE5rS6CIVK', '13800138000', 0);

/* 
* 插入上面四个用户的认证记录
 */
INSERT INTO user_certification(user_id) VALUES
(5),
(6),
(7),
(8);

-- 插入产品
INSERT INTO loan_products (
    product_name, description, loan_usage, status,
    min_term, max_term, term_step,
    min_amount, max_amount, promotion_details
) VALUES (
    '小微经营贷',
    '助力小微企业发展，快速审批，灵活还款',
    '进货周转、设备采购、门店扩张',
    '上架中',
    12, 36, 6,
    10000.00, 500000.00,
    '前2期只还利息'
);

INSERT INTO loan_options (product_id, interest_rate, loan_period, repaid_type) VALUES
(1, 0.0650, 1, '先息后本'),
(1, 0.0720, 2, '等额本息');

INSERT INTO loan_products (
    product_name, description, loan_usage, status,
    min_term, max_term, term_step,
    min_amount, max_amount, promotion_details
) VALUES (
    '创业启航贷',
    '专为初创企业设计，低门槛准入，快速放款',
    '办公租赁、人员工资、品牌推广',
    '上架中',
    12, 36, 6,
    20000.00, 100000.00,
    '首月免息，赠财务咨询'
);

INSERT INTO loan_options (product_id, interest_rate, loan_period, repaid_type) VALUES
(2, 0.0720, 1, '等额本息'),
(2, 0.0780, 2, '等额本息');

INSERT INTO loan_products (
    product_name, description, loan_usage, status,
    min_term, max_term, term_step,
    min_amount, max_amount, promotion_details
) VALUES (
    '灵活周转贷',
    '随借随还，按日计息，满足短期流动性需求',
    '应付账款垫付、临时补货',
    '上架中',
    1, 12, 1,
    5000.00, 30000.00,
    '首次借款7天免息'
);

INSERT INTO loan_options (product_id, interest_rate, loan_period, repaid_type) VALUES
(3, 0.0800, 1, '先息后本');

INSERT INTO loan_products (
    product_name, description, loan_usage, status,
    min_term, max_term, term_step,
    min_amount, max_amount, promotion_details
) VALUES (
    '设备升级贷',
    '专项用于购置或更新生产设备，支持制造业转型',
    '购买数控机床、自动化设备等',
    '上架中',
    20, 60, 4,
    50000.00, 1000000.00,
    '合作厂商可享利率优惠0.5%'
);

INSERT INTO loan_options (product_id, interest_rate, loan_period, repaid_type) VALUES
(4, 0.0580, 5, '等额本息'),
(4, 0.0620, 10, '等额本息'),
(4, 0.0650, 5, '等额本金');

INSERT INTO loan_products (
    product_name, description, loan_usage, status,
    min_term, max_term, term_step,
    min_amount, max_amount, promotion_details
) VALUES (
    '季节备货贷',
    '针对零售、餐饮等行业旺季前的集中备货需求',
    '节日商品采购、原材料囤积',
    '已下架',
    3, 12, 3,
    10000.00, 200000.00,
    '旺季专享，审批加急通道'
);

INSERT INTO loan_options (product_id, interest_rate, loan_period, repaid_type) VALUES
(5, 0.0750, 6, '先息后本');
