-- Payroll cycle and summary tables

CREATE TABLE IF NOT EXISTS payroll_cycles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL,
    notes TEXT,
    approved_by VARCHAR(100),
    approved_at TIMESTAMP NULL,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS payroll_summaries (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    cycle_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    assignment_count INT NOT NULL DEFAULT 0,
    attendance_count INT NOT NULL DEFAULT 0,
    total_actual_minutes INT NOT NULL DEFAULT 0,
    total_orders INT NOT NULL DEFAULT 0,
    total_revenue DECIMAL(18,2) NOT NULL DEFAULT 0,
    total_base_payroll DECIMAL(18,2) NOT NULL DEFAULT 0,
    total_bonus DECIMAL(18,2) NOT NULL DEFAULT 0,
    total_penalty DECIMAL(18,2) NOT NULL DEFAULT 0,
    total_adjustment DECIMAL(18,2) NOT NULL DEFAULT 0,
    total_net_payroll DECIMAL(18,2) NOT NULL DEFAULT 0,
    notes TEXT,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_payroll_summary_cycle FOREIGN KEY (cycle_id) REFERENCES payroll_cycles (id),
    CONSTRAINT fk_payroll_summary_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE UNIQUE INDEX ux_payroll_summary_cycle_user ON payroll_summaries (cycle_id, user_id);
CREATE INDEX ix_payroll_summary_user ON payroll_summaries (user_id);

