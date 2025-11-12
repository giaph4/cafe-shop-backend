-- Flyway migration for shift management feature

CREATE TABLE IF NOT EXISTS shift_templates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    description TEXT,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    default_hourly_rate DECIMAL(15,2),
    default_fixed_allowance DECIMAL(15,2),
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_shift_templates_name UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS shift_template_roles (
    template_id BIGINT NOT NULL,
    role VARCHAR(50) NOT NULL,
    PRIMARY KEY (template_id, role),
    CONSTRAINT fk_template_roles_template FOREIGN KEY (template_id) REFERENCES shift_templates (id)
);

CREATE TABLE IF NOT EXISTS shift_instances (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id BIGINT,
    shift_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    status VARCHAR(20) NOT NULL,
    locked_at TIMESTAMP NULL,
    notes TEXT,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_shift_instances_template FOREIGN KEY (template_id) REFERENCES shift_templates (id),
    INDEX idx_shift_instances_date (shift_date),
    INDEX idx_shift_instances_status (status),
    INDEX idx_shift_instances_locked_at (locked_at)
);

CREATE TABLE IF NOT EXISTS shift_assignments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    shift_instance_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role_name VARCHAR(50),
    planned_start TIME NOT NULL,
    planned_end TIME NOT NULL,
    planned_minutes INT NOT NULL,
    hourly_rate DECIMAL(15,2),
    fixed_allowance DECIMAL(15,2),
    status VARCHAR(20) NOT NULL,
    actual_minutes INT,
    total_orders INT,
    total_revenue DECIMAL(15,2),
    bonus_amount DECIMAL(15,2),
    penalty_amount DECIMAL(15,2),
    base_payroll DECIMAL(15,2),
    adjustment_total DECIMAL(15,2),
    calculated_payroll DECIMAL(15,2),
    notes TEXT,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_assignments_instance FOREIGN KEY (shift_instance_id) REFERENCES shift_instances (id),
    CONSTRAINT fk_assignments_user FOREIGN KEY (user_id) REFERENCES users (id),
    INDEX idx_assignments_shift (shift_instance_id),
    INDEX idx_assignments_user (user_id),
    INDEX idx_assignments_status (status)
);

CREATE TABLE IF NOT EXISTS attendance_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    assignment_id BIGINT NOT NULL,
    check_in_at TIMESTAMP NOT NULL,
    check_out_at TIMESTAMP NULL,
    source VARCHAR(20) NOT NULL,
    late_minutes INT DEFAULT 0,
    early_leave_minutes INT DEFAULT 0,
    note TEXT,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_attendance_assignment FOREIGN KEY (assignment_id) REFERENCES shift_assignments (id),
    INDEX idx_attendance_assignment (assignment_id),
    INDEX idx_attendance_check_in (check_in_at),
    INDEX idx_attendance_check_out (check_out_at)
);

CREATE TABLE IF NOT EXISTS shift_performance_adjustments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    assignment_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    reason TEXT,
    effective_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    revoked_at TIMESTAMP NULL,
    revoked_by VARCHAR(100),
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_adjustments_assignment FOREIGN KEY (assignment_id) REFERENCES shift_assignments (id),
    INDEX idx_adjustments_assignment (assignment_id),
    INDEX idx_adjustments_effective_at (effective_at)
);
