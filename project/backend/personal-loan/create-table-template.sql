CREATE TABLE users(  
    id int NOT NULL PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary Key',
    user_name VARCHAR(16),
    password VARCHAR(20) COMMENT '密码',
    id_card CHAR(18) COMMENT '身份证号',
    phone CHAR(11) COMMENT '手机号',
    credit_score TINYINT COMMENT '信誉分',
    create_time DATETIME COMMENT '创建时间',
    update_time DATETIME COMMENT '更新时间'
) COMMENT '用户表';

CREATE Table black_list(
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    level TINYINT COMMENT '黑名单等级',
    create_time DATETIME COMMENT '创建时间',
    update_time DATETIME COMMENT '更新时间',
    user_id INT NOT NULL COMMENT '关联用户ID',
    FOREIGN KEY (user_id) REFERENCES user(id)
)COMMENT '黑名单表';

CREATE TABLE user_certification(
    user_id INT PRIMARY KEY COMMENT '用户ID',
    id_card CHAR(18) COMMENT '身份证号r',
    work_cert_id VARCHAR(50) COMMENT '工作证明',
    tri_cert_id VARCHAR(50) COMMENT '第三方证明',
    bank_card_id CHAR(16) COMMENT '银行卡号',
    immovable_cert_id VARCHAR(50) COMMENT '不动产证明',
    Foreign Key (user_id) REFERENCES user(id)
)COMMENT '用户认证表';

CREATE TABLE immovables_cert(
    immovable_cert_id VARCHAR(50) PRIMARY KEY COMMENT '不动产证明ID',
    property_cert_id VARCHAR(50) COMMENT '房产证，存路径', 
    car_cert_id VARCHAR(50) COMMENT '车产证明，存路径',
    total_value INT COMMENT '总资产值',
    Foreign Key (immovable_cert_id) REFERENCES user_certification(immovable_cert_id)
)COMMENT '不动产认证表';

// 管理员未设计
