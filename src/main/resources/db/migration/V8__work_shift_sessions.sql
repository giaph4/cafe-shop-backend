-- Sprint 2: introduce POS shift sessions and reporting structures

CREATE TABLE work_shifts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    description TEXT NULL,
    start_at DATETIME NOT NULL,
    end_at DATETIME NOT NULL,
    max_employees INT NOT NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_work_shifts_name (name),
    KEY idx_work_shifts_active (is_active),
    KEY idx_work_shifts_time (start_at, end_at)
);

CREATE TABLE shift_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    work_shift_id BIGINT NULL,
    user_id BIGINT NOT NULL,
    start_at DATETIME NOT NULL,
    end_at DATETIME NULL,
    status VARCHAR(20) NOT NULL,
    is_admin_override TINYINT(1) NOT NULL DEFAULT 0,
    force_reason VARCHAR(255) NULL,
    force_by BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_shift_sessions_work_shift FOREIGN KEY (work_shift_id) REFERENCES work_shifts (id),
    CONSTRAINT fk_shift_sessions_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_shift_sessions_force_by FOREIGN KEY (force_by) REFERENCES users (id),
    KEY idx_shift_sessions_status (status),
    KEY idx_shift_sessions_user (user_id),
    KEY idx_shift_sessions_active (status, user_id)
);

CREATE TABLE shift_reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    total_orders INT NOT NULL,
    total_paid_amount DECIMAL(18,2) NOT NULL,
    total_unpaid_amount DECIMAL(18,2) NOT NULL,
    report_json LONGTEXT NOT NULL,
    generated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_shift_reports_session FOREIGN KEY (session_id) REFERENCES shift_sessions (id),
    KEY idx_shift_reports_session (session_id)
);

CREATE TABLE shift_event_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NULL,
    event_type VARCHAR(40) NOT NULL,
    payload JSON NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_shift_event_logs_session FOREIGN KEY (session_id) REFERENCES shift_sessions (id),
    KEY idx_shift_event_logs_type (event_type)
);

ALTER TABLE orders
    ADD COLUMN shift_session_id BIGINT NULL AFTER user_id,
    ADD COLUMN transferred TINYINT(1) NOT NULL DEFAULT 0 AFTER shift_session_id,
    ADD CONSTRAINT fk_orders_shift_session FOREIGN KEY (shift_session_id) REFERENCES shift_sessions (id),
    ADD KEY idx_orders_shift_session (shift_session_id),
    ADD KEY idx_orders_transferred (transferred);
