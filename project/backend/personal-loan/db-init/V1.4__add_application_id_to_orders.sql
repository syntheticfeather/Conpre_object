-- ============================================================
-- V1.4: orders 表增加 application_id，连接订单与申请
-- ============================================================

ALTER TABLE orders
  ADD COLUMN application_id INT NULL COMMENT '关联的贷款申请ID'
  AFTER user_id;

ALTER TABLE orders
  ADD FOREIGN KEY (application_id) REFERENCES loan_applications(id);
