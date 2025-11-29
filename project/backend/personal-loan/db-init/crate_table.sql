
USE `person-loan`;
CREATE TABLE users(  
    id int NOT NULL PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary Key',
    user_name VARCHAR(16),
    avatar VARCHAR(255) COMMENT '头像，存路径，二期工程',
    password VARCHAR(255) COMMENT '密码',
    id_card CHAR(18) COMMENT '身份证号',
    phone CHAR(11) COMMENT '手机号',
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

CREATE TABLE user_certification(
    user_id INT PRIMARY KEY COMMENT '用户ID',
    id_card CHAR(18) COMMENT '身份证号',
    credit_score TINYINT COMMENT '信誉分',
    work_cert_id INT UNIQUE COMMENT '工作证明',
    tri_cert_id INT UNIQUE COMMENT '第三方证明',
    bank_card_id CHAR(16) COMMENT '银行卡号',
    immovable_cert_id INT UNIQUE COMMENT '不动产证明',
    Foreign Key (user_id) REFERENCES users(id)
)COMMENT '用户认证表';

/*
* 工作证明和第三方证明，和不动产证明一样，建表，然后路径部分，说清楚，存图片的本地文件路径
* 二期工程
*/
CREATE TABLE work_cert(
    work_cert_id INT PRIMARY KEY COMMENT '工作证明ID',
    employment_cert_path VARCHAR(255) COMMENT '在职证明图片路径',
    salary_cert_path VARCHAR(255) COMMENT '收入证明图片路径',
    Foreign Key (work_cert_id) REFERENCES user_certification(work_cert_id)
)COMMENT '工作认证表';

/* 二期工程 */
CREATE TABLE tri_cert(
    tri_cert_id INT PRIMARY KEY COMMENT '第三方证明ID',
    social_security_path VARCHAR(255) COMMENT '社保证明，存路径',
    credit_report_path VARCHAR(255) COMMENT '征信报告，存路径（）',
    Foreign Key (tri_cert_id) REFERENCES user_certification(tri_cert_id)
)COMMENT '第三方认证表';

CREATE TABLE immovables_cert(
    immovable_cert_id INT PRIMARY KEY COMMENT '不动产证明ID',
    property_cert_path VARCHAR(255) COMMENT '房产证，存路径', 
    car_cert_path VARCHAR(255) COMMENT '车产证明，存路径',
    total_value INT COMMENT '总资产值',
    Foreign Key (immovable_cert_id) REFERENCES user_certification(immovable_cert_id)
)COMMENT '不动产认证表';


/*
* 评论功能？二期工程
*/
CREATE TABLE loan_products(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    product_name VARCHAR(50) COMMENT '产品名称',
    description VARCHAR(255) COMMENT '产品描述',
    loan_usage VARCHAR(100) COMMENT '贷款用途(目前不需要)',
    min_term INT COMMENT '最短期数',
    max_term INT COMMENT '最长期数',
    term_step INT COMMENT '期限步长',
    promotion_details VARCHAR(255) COMMENT '促销详情',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
)COMMENT '贷款产品表';

CREATE TABLE loan_options(
    id INT PRIMARY KEY AUTO_INCREMENT,
    product_id INT NOT NULL,
    loan_amount DECIMAL(12,2) COMMENT '贷款金额',
    interest_rate DECIMAL(6,4) COMMENT '利率',
    loan_period INT COMMENT '年限',
    repaid_type VARCHAR(50) COMMENT '还款方式,等额本息, 等额本金, 先息后本, 一次性还本付息',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (product_id) REFERENCES loan_products(id)
)COMMENT '贷款选项表';


/*
* 加个合同，仍然存图片本地路径
*/
CREATE TABLE orders(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    user_id INT NOT NULL COMMENT '用户ID',
    product_id INT NOT NULL COMMENT '产品ID',
    status VARCHAR(50) COMMENT '贷款状态,NORMAL,OVERDUE,SETTLED',
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
    status VARCHAR(50) COMMENT '申请状态,PENDING,APPROVED,AI_REJECTED,MANUAL_REJECTED,CANCELLED',
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

CREATE TABLE loan_applications_manual(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    application_id INT NOT NULL COMMENT '申请ID',
    FOREIGN KEY (application_id) REFERENCES loan_applications(id)
)COMMENT '人工审核表';