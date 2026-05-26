-- ==============================================
-- JMeter 测试数据初始化脚本
-- 基于实际表结构生成测试数据
-- ==============================================

-- 1. 创建测试用户（100个测试账号）
DELIMITER //
CREATE PROCEDURE CreateTestUsers()
BEGIN
    DECLARE i INT DEFAULT 1;
    WHILE i <= 100 DO
        INSERT INTO users (user_name, password, phone, role, create_time)
        VALUES (
            CONCAT('测试用户', i),
            '$2a$10$5Tv4TO0nt1JU/8xG2tX3/.eCU2wtkNkTzHwK.nSQoZTGh36sk3F8y', -- BCrypt加密的 'Test@123'
            CONCAT('135', LPAD(i, 8, '0')), -- 生成手机号：13500000001 ~ 13500000100
            0, -- 普通用户
            NOW()
        );
        SET i = i + 1;
    END WHILE;
END //
DELIMITER ;

-- 执行存储过程创建测试用户
CALL CreateTestUsers();

-- 删除存储过程
DROP PROCEDURE CreateTestUsers;

-- 2. 创建用户认证信息
DELIMITER //
CREATE PROCEDURE CreateUserCertifications()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE credit_score INT;
    DECLARE max_loan_amount DECIMAL(12,2);
    DECLARE bank_card VARCHAR(19);
    
    WHILE i <= 100 DO
        SET credit_score = FLOOR(RAND() * 200) + 550; -- 550-749
        SET max_loan_amount = (credit_score - 500) * 1000; -- 5万-24.9万
        SET bank_card = CONCAT('6222', LPAD(FLOOR(RAND() * 999999999999), 12, '0'));
        
        INSERT INTO user_certification (user_id, real_name, id_card, credit_score, max_loan_amount, bank_card_id)
        VALUES (
            i,
            CONCAT('测试真实姓名', i),
            CONCAT('110101', LPAD(i, 12, '0')), -- 身份证号
            credit_score,
            max_loan_amount,
            bank_card
        );
        SET i = i + 1;
    END WHILE;
END //
DELIMITER ;

-- 执行存储过程创建用户认证信息
CALL CreateUserCertifications();

-- 删除存储过程
DROP PROCEDURE CreateUserCertifications;

-- 3. 创建贷款申请（已通过的申请）
DELIMITER //
CREATE PROCEDURE CreateApprovedApplications()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE user_id INT;
    DECLARE product_id INT;
    DECLARE loan_amount DECIMAL(12,2);
    DECLARE interest_rate DECIMAL(6,4);
    DECLARE loan_period INT;
    DECLARE repaid_type VARCHAR(50);
    
    WHILE i <= 50 DO
        SET user_id = FLOOR(RAND() * 100) + 1; -- 用户ID 1-100
        SET product_id = FLOOR(RAND() * 4) + 1; -- 产品ID 1-4（前4个产品是上架中）
        SET loan_amount = FLOOR(RAND() * 49000) + 1000; -- 1000-50000
        SET interest_rate = (FLOOR(RAND() * 30) + 55) / 1000; -- 0.055-0.084
        SET loan_period = FLOOR(RAND() * 3) + 1; -- 1-3年
        SET repaid_type = CASE FLOOR(RAND() * 2) WHEN 0 THEN '等额本息' WHEN 1 THEN '等额本金' END;
        
        INSERT INTO loan_applications (user_id, product_id, status, loan_amount, interest_rate, loan_period, term, repaid_type, apply_time, review_time)
        VALUES (
            user_id,
            product_id,
            '已通过',
            loan_amount,
            interest_rate,
            loan_period,
            loan_period * 12, -- 转换为月
            repaid_type,
            DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 30) DAY),
            DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 15) DAY)
        );
        SET i = i + 1;
    END WHILE;
END //
DELIMITER ;

-- 执行存储过程创建已通过的贷款申请
CALL CreateApprovedApplications();

-- 删除存储过程
DROP PROCEDURE CreateApprovedApplications;

-- 4. 创建贷款申请（审核中的申请）
DELIMITER //
CREATE PROCEDURE CreatePendingApplications()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE user_id INT;
    DECLARE product_id INT;
    DECLARE loan_amount DECIMAL(12,2);
    DECLARE loan_period INT;
    DECLARE repaid_type VARCHAR(50);
    
    WHILE i <= 20 DO
        SET user_id = FLOOR(RAND() * 100) + 9;
        SET product_id = FLOOR(RAND() * 4) + 1;
        SET loan_amount = FLOOR(RAND() * 49000) + 1000;
        SET loan_period = FLOOR(RAND() * 3) + 1;
        SET repaid_type = CASE FLOOR(RAND() * 2) WHEN 0 THEN '等额本息' WHEN 1 THEN '等额本金' END;
        
        INSERT INTO loan_applications (user_id, product_id, status, loan_amount, loan_period, term, repaid_type, apply_time)
        VALUES (
            user_id,
            product_id,
            '审核中',
            loan_amount,
            loan_period,
            loan_period * 12,
            repaid_type,
            DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 3) DAY)
        );
        SET i = i + 1;
    END WHILE;
END //
DELIMITER ;

-- 执行存储过程创建审核中的贷款申请
CALL CreatePendingApplications();

-- 删除存储过程
DROP PROCEDURE CreatePendingApplications;

-- 5. 创建订单（基于已通过的贷款申请）
DELIMITER //
CREATE PROCEDURE CreateOrders()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE app_id INT;
    DECLARE user_id INT;
    DECLARE product_id INT;
    DECLARE loan_amount DECIMAL(12,2);
    DECLARE interest_rate DECIMAL(6,4);
    DECLARE loan_period INT;
    DECLARE term INT;
    DECLARE repaid_type VARCHAR(50);
    DECLARE status VARCHAR(50);
    DECLARE current_term INT;
    DECLARE repaid_amount DECIMAL(12,2);
    
    -- 游标用于遍历已通过的申请
    DECLARE cur CURSOR FOR 
        SELECT id, user_id, product_id, loan_amount, interest_rate, loan_period, term, repaid_type 
        FROM loan_applications 
        WHERE status = '已通过' 
        ORDER BY id LIMIT 50;
    
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET i = 0;
    
    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO app_id, user_id, product_id, loan_amount, interest_rate, loan_period, term, repaid_type;
        IF i = 0 THEN
            LEAVE read_loop;
        END IF;
        
        -- 随机生成订单状态
        SET status = CASE FLOOR(RAND() * 3) 
            WHEN 0 THEN '正常' 
            WHEN 1 THEN '已完成' 
            WHEN 2 THEN '正常' 
        END;
        
        -- 如果是已完成状态，设置当前期数和已还金额
        IF status = '已完成' THEN
            SET current_term = term;
            SET repaid_amount = loan_amount * (1 + interest_rate * loan_period);
        ELSE
            SET current_term = FLOOR(RAND() * term) + 1;
            -- 计算已还金额（简化计算）
            SET repaid_amount = (loan_amount / term) * current_term;
        END IF;
        
        INSERT INTO orders (user_id, product_id, status, repaid_amount, loan_amount, interest_rate, repaid_type, loan_period, term, current_term, start_time)
        VALUES (
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
            DATE_SUB(NOW(), INTERVAL term - current_term MONTH)
        );
        
        SET i = i + 1;
    END LOOP;
    CLOSE cur;
END //
DELIMITER ;

-- 执行存储过程创建订单
CALL CreateOrders();

-- 删除存储过程
DROP PROCEDURE CreateOrders;

-- 6. 创建还款计划
DELIMITER //
CREATE PROCEDURE CreateRepaymentSchedules()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE order_id INT;
    DECLARE user_id INT;
    DECLARE loan_amount DECIMAL(12,2);
    DECLARE interest_rate DECIMAL(6,4);
    DECLARE term INT;
    DECLARE repaid_type VARCHAR(50);
    DECLARE current_term INT;
    DECLARE status VARCHAR(20);
    DECLARE due_date DATE;
    
    DECLARE principal DECIMAL(12,2);
    DECLARE interest DECIMAL(12,2);
    DECLARE total_amount DECIMAL(12,2);
    DECLARE remaining_principal DECIMAL(12,2);
    DECLARE remaining_interest DECIMAL(12,2);
    
    DECLARE cur CURSOR FOR 
        SELECT id, user_id, loan_amount, interest_rate, term, repaid_type, current_term 
        FROM orders;
    
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET i = 0;
    
    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO order_id, user_id, loan_amount, interest_rate, term, repaid_type, current_term;
        IF i = 0 THEN
            LEAVE read_loop;
        END IF;
        
        SET remaining_principal = loan_amount;
        
        -- 等额本息计算
        IF repaid_type = '等额本息' THEN
            SET total_amount = loan_amount * interest_rate / 12 * POWER(1 + interest_rate / 12, term) / (POWER(1 + interest_rate / 12, term) - 1);
            
            SET i = 1;
            due_loop: LOOP
                IF i > term THEN
                    LEAVE due_loop;
                END IF;
                
                SET interest = remaining_principal * interest_rate / 12;
                SET principal = total_amount - interest;
                SET remaining_principal = remaining_principal - principal;
                
                -- 计算剩余利息
                SET remaining_interest = 0;
                IF i < term THEN
                    SET remaining_interest = remaining_principal * interest_rate / 12 * (term - i);
                END IF;
                
                SET due_date = DATE_ADD(NOW(), INTERVAL i - current_term MONTH);
                
                -- 设置还款状态
                IF i < current_term THEN
                    SET status = '已还';
                ELSEIF i = current_term AND due_date <= CURDATE() THEN
                    SET status = '逾期';
                ELSE
                    SET status = '未还';
                END IF;
                
                INSERT INTO repayment_schedule (order_id, term, principal, interest, total_amount, status, remaining_principal, remaining_interest, due_date)
                VALUES (
                    order_id,
                    i,
                    ROUND(principal, 2),
                    ROUND(interest, 2),
                    ROUND(total_amount, 2),
                    status,
                    ROUND(remaining_principal, 2),
                    ROUND(remaining_interest, 2),
                    due_date
                );
                
                SET i = i + 1;
            END LOOP;
        
        -- 等额本金计算
        ELSE
            SET principal = loan_amount / term;
            
            SET i = 1;
            due_loop2: LOOP
                IF i > term THEN
                    LEAVE due_loop2;
                END IF;
                
                SET interest = (loan_amount - principal * (i - 1)) * interest_rate / 12;
                SET total_amount = principal + interest;
                SET remaining_principal = loan_amount - principal * i;
                
                -- 计算剩余利息
                SET remaining_interest = 0;
                IF i < term THEN
                    SET remaining_interest = remaining_principal * interest_rate / 12 * (term - i);
                END IF;
                
                SET due_date = DATE_ADD(NOW(), INTERVAL i - current_term MONTH);
                
                -- 设置还款状态
                IF i < current_term THEN
                    SET status = '已还';
                ELSEIF i = current_term AND due_date <= CURDATE() THEN
                    SET status = '逾期';
                ELSE
                    SET status = '未还';
                END IF;
                
                INSERT INTO repayment_schedule (order_id, term, principal, interest, total_amount, status, remaining_principal, remaining_interest, due_date)
                VALUES (
                    order_id,
                    i,
                    ROUND(principal, 2),
                    ROUND(interest, 2),
                    ROUND(total_amount, 2),
                    status,
                    ROUND(remaining_principal, 2),
                    ROUND(remaining_interest, 2),
                    due_date
                );
                
                SET i = i + 1;
            END LOOP;
        END IF;
    END LOOP;
    CLOSE cur;
END //
DELIMITER ;

-- 执行存储过程创建还款计划
CALL CreateRepaymentSchedules();

-- 删除存储过程
DROP PROCEDURE CreateRepaymentSchedules;

-- ==============================================
-- 测试数据初始化完成
-- ==============================================
-- 生成的数据统计：
-- - 100个测试用户（ID 1-100）
-- - 100条用户认证记录
-- - 50条已通过贷款申请
-- - 20条审核中贷款申请
-- - 50条订单数据
-- - 约600条还款计划记录
-- ==============================================

-- 测试用户登录信息：
-- 手机号：13500000001 ~ 13500000100
-- 密码：Test@123
-- ==============================================
