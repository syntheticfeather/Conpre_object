-- SQL索引创建语句

-- users表索引
CREATE UNIQUE INDEX idx_users_phone ON users(phone);
CREATE INDEX idx_users_role ON users(role);

-- loan_products表索引
CREATE INDEX idx_loan_products_status ON loan_products(status);
CREATE INDEX idx_loan_products_create_update_time ON loan_products(create_time, update_time);
CREATE INDEX idx_loan_products_update_time ON loan_products(update_time);
CREATE INDEX idx_update_create ON loan_products(update_time DESC, create_time DESC);

-- loan_applications表索引
CREATE INDEX idx_loan_applications_user_id ON loan_applications(user_id);
CREATE INDEX idx_loan_applications_product_id ON loan_applications(product_id);
CREATE INDEX idx_loan_applications_status ON loan_applications(status);
CREATE INDEX idx_loan_applications_apply_time ON loan_applications(apply_time);

-- orders表索引
CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_product_id ON orders(product_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_start_time ON orders(start_time);

-- user_certification表索引
CREATE INDEX idx_user_certification_credit_score ON user_certification(credit_score);

-- black_list表索引
CREATE INDEX idx_black_list_user_id ON black_list(user_id);