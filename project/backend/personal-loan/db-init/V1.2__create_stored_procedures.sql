SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;
-- ============================================
-- V1.2: 存储过程
-- 1. sp_generate_repayment_schedule  — 生成还款计划
-- 2. sp_update_due_date_after_postpone — 延期后更新到期日
-- ============================================

DELIMITER //

-- --------------------------------------------
-- 存储过程 1：生成还款计划
-- 输入：order_id
-- 输出：p_success = 1 成功 / 0 失败
-- 事务：内部自管理，调用方不需 @Transactional
-- --------------------------------------------
CREATE PROCEDURE sp_generate_repayment_schedule(
    IN  p_order_id INT,
    OUT p_success  INT
)
BEGIN
    DECLARE v_loan_amount   DECIMAL(12,2);
    DECLARE v_interest_rate DECIMAL(6,4);
    DECLARE v_term          INT;
    DECLARE v_repaid_type   VARCHAR(50);
    DECLARE v_start_time    DATETIME;

    DECLARE v_monthly_rate  DECIMAL(20,10);
    DECLARE v_i             INT DEFAULT 1;

    DECLARE v_principal           DECIMAL(12,2);
    DECLARE v_interest            DECIMAL(12,2);
    DECLARE v_total               DECIMAL(12,2);
    DECLARE v_remaining_principal DECIMAL(12,2);
    DECLARE v_remaining_interest  DECIMAL(12,2);
    DECLARE v_due_date            DATE;

    DECLARE v_pow_factor  DECIMAL(20,10);
    DECLARE v_monthly_pay DECIMAL(12,2);
    DECLARE v_per_period   DECIMAL(12,2);

    -- 异常回滚
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET p_success = 0;
    END;

    SET p_success = 0;
    START TRANSACTION;

    -- 1. 查询 order 数据
    SELECT loan_amount, interest_rate, term, repaid_type, start_time
      INTO v_loan_amount, v_interest_rate, v_term, v_repaid_type, v_start_time
      FROM orders
     WHERE id = p_order_id;

    IF v_loan_amount IS NULL OR v_term <= 0 THEN
        ROLLBACK;
        SET p_success = 0;
    END IF;

    -- 2. 年利率 -> 月利率
    SET v_monthly_rate = v_interest_rate / 12.0;
    SET v_remaining_principal = v_loan_amount;

    -- =========================================
    -- 等额本息
    -- =========================================
    IF v_repaid_type = '等额本息' THEN
        SET v_pow_factor  = POW(1 + v_monthly_rate, v_term);
        SET v_monthly_pay = ROUND(
            v_loan_amount * v_monthly_rate * v_pow_factor / (v_pow_factor - 1), 2);

        WHILE v_i <= v_term DO
            SET v_interest  = ROUND(v_remaining_principal * v_monthly_rate, 2);
            SET v_principal = ROUND(v_monthly_pay - v_interest, 2);
            IF v_i = v_term THEN
                SET v_principal = v_remaining_principal;
            END IF;

            SET v_total       = v_principal + v_interest;
            SET v_remaining_principal = v_remaining_principal - v_principal;
            SET v_remaining_interest  = ROUND(v_remaining_principal * v_monthly_rate * (v_term - v_i), 2);
            SET v_due_date = DATE_ADD(DATE(v_start_time), INTERVAL v_i MONTH);

            INSERT INTO repayment_schedule
                (order_id, term, principal, interest, total_amount, status,
                 remaining_principal, remaining_interest, due_date)
            VALUES
                (p_order_id, v_i, v_principal, v_interest, v_total, '未还',
                 v_remaining_principal, v_remaining_interest, v_due_date);

            SET v_i = v_i + 1;
        END WHILE;

    -- =========================================
    -- 等额本金
    -- =========================================
    ELSEIF v_repaid_type = '等额本金' THEN
        SET v_per_period = ROUND(v_loan_amount / v_term, 2);

        WHILE v_i <= v_term DO
            SET v_interest  = ROUND(v_remaining_principal * v_monthly_rate, 2);
            SET v_principal = v_per_period;
            IF v_i = v_term THEN
                SET v_principal = v_remaining_principal;
            END IF;

            SET v_total       = v_principal + v_interest;
            SET v_remaining_principal = v_remaining_principal - v_principal;
            SET v_remaining_interest  = ROUND(
                v_remaining_principal * v_monthly_rate * (v_term - v_i + 1) / 2, 2);
            SET v_due_date = DATE_ADD(DATE(v_start_time), INTERVAL v_i MONTH);

            INSERT INTO repayment_schedule
                (order_id, term, principal, interest, total_amount, status,
                 remaining_principal, remaining_interest, due_date)
            VALUES
                (p_order_id, v_i, v_principal, v_interest, v_total, '未还',
                 v_remaining_principal, v_remaining_interest, v_due_date);

            SET v_i = v_i + 1;
        END WHILE;

    -- =========================================
    -- 先息后本
    -- =========================================
    ELSEIF v_repaid_type = '先息后本' THEN
        SET v_per_period = ROUND(v_loan_amount * v_monthly_rate, 2);

        WHILE v_i < v_term DO
            SET v_interest           = v_per_period;
            SET v_principal          = 0;
            SET v_total              = v_per_period;
            SET v_remaining_interest = ROUND(
                v_remaining_principal * v_monthly_rate * (v_term - v_i), 2);
            SET v_due_date = DATE_ADD(DATE(v_start_time), INTERVAL v_i MONTH);

            INSERT INTO repayment_schedule
                (order_id, term, principal, interest, total_amount, status,
                 remaining_principal, remaining_interest, due_date)
            VALUES
                (p_order_id, v_i, 0, v_interest, v_total, '未还',
                 v_remaining_principal, v_remaining_interest, v_due_date);

            SET v_i = v_i + 1;
        END WHILE;

        SET v_interest  = v_per_period;
        SET v_principal = v_loan_amount;
        SET v_total     = v_principal + v_interest;
        SET v_due_date  = DATE_ADD(DATE(v_start_time), INTERVAL v_term MONTH);

        INSERT INTO repayment_schedule
            (order_id, term, principal, interest, total_amount, status,
             remaining_principal, remaining_interest, due_date)
        VALUES
            (p_order_id, v_term, v_principal, v_interest, v_total, '未还', 0, 0, v_due_date);

    -- =========================================
    -- 一次性还本付息
    -- =========================================
    ELSEIF v_repaid_type = '一次性还本付息' THEN
        SET v_interest  = ROUND(v_loan_amount * v_monthly_rate * v_term, 2);
        SET v_principal = v_loan_amount;
        SET v_total     = v_principal + v_interest;
        SET v_due_date  = DATE_ADD(DATE(v_start_time), INTERVAL v_term MONTH);

        INSERT INTO repayment_schedule
            (order_id, term, principal, interest, total_amount, status,
             remaining_principal, remaining_interest, due_date)
        VALUES
            (p_order_id, 1, v_principal, v_interest, v_total, '未还', 0, 0, v_due_date);

    END IF;

    COMMIT;
    SET p_success = 1;
END //


-- --------------------------------------------
-- 存储过程 2：延期后更新还款计划到期日
-- 输出：p_success = 1 成功 / 0 失败
-- 事务：内部自管理
-- --------------------------------------------
CREATE PROCEDURE sp_update_due_date_after_postpone(
    IN  p_order_id INT,
    IN  p_term     INT,
    OUT p_success  INT
)
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET p_success = 0;
    END;

    SET p_success = 0;
    START TRANSACTION;

    UPDATE repayment_schedule
       SET due_date   = DATE_ADD(due_date, INTERVAL 1 MONTH),
           updated_at = NOW()
     WHERE order_id = p_order_id
       AND term >= p_term;

    COMMIT;
    SET p_success = 1;
END //

DELIMITER ;
