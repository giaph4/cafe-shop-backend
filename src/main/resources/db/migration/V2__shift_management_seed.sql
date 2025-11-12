-- Sample data seed for shift management

-- === Shift Templates ===
INSERT INTO shift_templates (id, name, description, start_time, end_time, default_hourly_rate, default_fixed_allowance, created_by, updated_by)
SELECT 100, 'Ca sáng quầy pha chế', 'Ca tiêu chuẩn cho barista & thu ngân buổi sáng', '07:00:00', '11:00:00', 35000.00, 50000.00, 'system', 'system'
WHERE NOT EXISTS (SELECT 1 FROM shift_templates WHERE id = 100);

INSERT INTO shift_templates (id, name, description, start_time, end_time, default_hourly_rate, default_fixed_allowance, created_by, updated_by)
SELECT 101, 'Ca chiều phục vụ', 'Ca phục vụ bàn và runner buổi chiều', '12:00:00', '16:00:00', 32000.00, 40000.00, 'system', 'system'
WHERE NOT EXISTS (SELECT 1 FROM shift_templates WHERE id = 101);

INSERT INTO shift_templates (id, name, description, start_time, end_time, default_hourly_rate, default_fixed_allowance, created_by, updated_by)
SELECT 102, 'Ca tối quản lý', 'Ca quản lý ca tối, giám sát đóng quán', '17:00:00', '21:00:00', 50000.00, 60000.00, 'system', 'system'
WHERE NOT EXISTS (SELECT 1 FROM shift_templates WHERE id = 102);

-- === Template Roles ===
INSERT IGNORE INTO shift_template_roles (template_id, role) VALUES
    (100, 'BARISTA'),
    (100, 'CASHIER'),
    (101, 'SERVICE'),
    (101, 'RUNNER'),
    (102, 'SHIFT_LEAD');

-- === Shift Instances ===
INSERT INTO shift_instances (id, template_id, shift_date, start_time, end_time, status, locked_at, notes, created_by, updated_by)
SELECT 200, 100, '2025-01-15', '07:00:00', '11:00:00', 'PLANNED', NULL, 'Ca sáng thứ Tư', 'system', 'system'
WHERE NOT EXISTS (SELECT 1 FROM shift_instances WHERE id = 200);

INSERT INTO shift_instances (id, template_id, shift_date, start_time, end_time, status, locked_at, notes, created_by, updated_by)
SELECT 201, 100, '2025-01-16', '07:00:00', '11:00:00', 'PLANNED', NULL, 'Ca sáng thứ Năm', 'system', 'system'
WHERE NOT EXISTS (SELECT 1 FROM shift_instances WHERE id = 201);

INSERT INTO shift_instances (id, template_id, shift_date, start_time, end_time, status, locked_at, notes, created_by, updated_by)
SELECT 202, 101, '2025-01-15', '12:00:00', '16:00:00', 'PLANNED', NULL, 'Ca chiều phục vụ', 'system', 'system'
WHERE NOT EXISTS (SELECT 1 FROM shift_instances WHERE id = 202);

INSERT INTO shift_instances (id, template_id, shift_date, start_time, end_time, status, locked_at, notes, created_by, updated_by)
SELECT 203, 102, '2025-01-15', '17:00:00', '21:00:00', 'PLANNED', NULL, 'Ca tối quản lý', 'system', 'system'
WHERE NOT EXISTS (SELECT 1 FROM shift_instances WHERE id = 203);

-- === Shift Assignments ===
INSERT INTO shift_assignments (
    id, shift_instance_id, user_id, role_name, planned_start, planned_end, planned_minutes,
    hourly_rate, fixed_allowance, status, actual_minutes, total_orders, total_revenue,
    bonus_amount, penalty_amount, base_payroll, adjustment_total, calculated_payroll,
    notes, created_by, updated_by
)
SELECT 300, 200, 1, 'BARISTA', '07:00:00', '11:00:00', 240,
       35000.00, 50000.00, 'SCHEDULED', NULL, NULL, NULL,
       0.00, 0.00, 0.00, 0.00, 0.00,
       'Pha chế chính ca sáng', 'system', 'system'
WHERE NOT EXISTS (SELECT 1 FROM shift_assignments WHERE id = 300);

INSERT INTO shift_assignments (
    id, shift_instance_id, user_id, role_name, planned_start, planned_end, planned_minutes,
    hourly_rate, fixed_allowance, status, actual_minutes, total_orders, total_revenue,
    bonus_amount, penalty_amount, base_payroll, adjustment_total, calculated_payroll,
    notes, created_by, updated_by
)
SELECT 301, 200, 7, 'CASHIER', '07:00:00', '11:00:00', 240,
       32000.00, 40000.00, 'SCHEDULED', NULL, NULL, NULL,
       0.00, 0.00, 0.00, 0.00, 0.00,
       'Thu ngân hỗ trợ ca sáng', 'system', 'system'
WHERE NOT EXISTS (SELECT 1 FROM shift_assignments WHERE id = 301);

INSERT INTO shift_assignments (
    id, shift_instance_id, user_id, role_name, planned_start, planned_end, planned_minutes,
    hourly_rate, fixed_allowance, status, actual_minutes, total_orders, total_revenue,
    bonus_amount, penalty_amount, base_payroll, adjustment_total, calculated_payroll,
    notes, created_by, updated_by
)
SELECT 302, 201, 3, 'BARISTA', '07:00:00', '11:00:00', 240,
       30000.00, 30000.00, 'SCHEDULED', NULL, NULL, NULL,
       0.00, 0.00, 0.00, 0.00, 0.00,
       'Barista dự bị cho ca sáng', 'system', 'system'
WHERE NOT EXISTS (SELECT 1 FROM shift_assignments WHERE id = 302);

INSERT INTO shift_assignments (
    id, shift_instance_id, user_id, role_name, planned_start, planned_end, planned_minutes,
    hourly_rate, fixed_allowance, status, actual_minutes, total_orders, total_revenue,
    bonus_amount, penalty_amount, base_payroll, adjustment_total, calculated_payroll,
    notes, created_by, updated_by
)
SELECT 303, 202, 8, 'SERVICE', '12:00:00', '16:00:00', 240,
       32000.00, 30000.00, 'SCHEDULED', NULL, NULL, NULL,
       0.00, 0.00, 0.00, 0.00, 0.00,
       'Phục vụ bàn chính buổi chiều', 'system', 'system'
WHERE NOT EXISTS (SELECT 1 FROM shift_assignments WHERE id = 303);

INSERT INTO shift_assignments (
    id, shift_instance_id, user_id, role_name, planned_start, planned_end, planned_minutes,
    hourly_rate, fixed_allowance, status, actual_minutes, total_orders, total_revenue,
    bonus_amount, penalty_amount, base_payroll, adjustment_total, calculated_payroll,
    notes, created_by, updated_by
)
SELECT 304, 202, 9, 'RUNNER', '12:00:00', '16:00:00', 240,
       28000.00, 20000.00, 'SCHEDULED', NULL, NULL, NULL,
       0.00, 0.00, 0.00, 0.00, 0.00,
       'Runner hỗ trợ phục vụ', 'system', 'system'
WHERE NOT EXISTS (SELECT 1 FROM shift_assignments WHERE id = 304);

INSERT INTO shift_assignments (
    id, shift_instance_id, user_id, role_name, planned_start, planned_end, planned_minutes,
    hourly_rate, fixed_allowance, status, actual_minutes, total_orders, total_revenue,
    bonus_amount, penalty_amount, base_payroll, adjustment_total, calculated_payroll,
    notes, created_by, updated_by
)
SELECT 305, 203, 2, 'SHIFT_LEAD', '17:00:00', '21:00:00', 240,
       55000.00, 60000.00, 'SCHEDULED', NULL, NULL, NULL,
       0.00, 0.00, 0.00, 0.00, 0.00,
       'Quản lý giám sát ca tối', 'system', 'system'
WHERE NOT EXISTS (SELECT 1 FROM shift_assignments WHERE id = 305);

-- === Attendance Records (sample check-in/out) ===
INSERT INTO attendance_records (
    id, assignment_id, check_in_at, check_out_at, source, late_minutes, early_leave_minutes,
    note, created_by, updated_by
)
SELECT 400, 300, '2025-01-15 06:55:00', '2025-01-15 11:05:00', 'QR', 0, 0,
       'Check-in sớm 5 phút, overtime 5 phút', 'staff01', 'staff01'
WHERE NOT EXISTS (SELECT 1 FROM attendance_records WHERE id = 400);

INSERT INTO attendance_records (
    id, assignment_id, check_in_at, check_out_at, source, late_minutes, early_leave_minutes,
    note, created_by, updated_by
)
SELECT 401, 301, '2025-01-15 07:05:00', '2025-01-15 10:55:00', 'APP', 5, 5,
       'Late 5 phút, về sớm 5 phút', 'staff_cashier', 'staff_cashier'
WHERE NOT EXISTS (SELECT 1 FROM attendance_records WHERE id = 401);

INSERT INTO attendance_records (
    id, assignment_id, check_in_at, check_out_at, source, late_minutes, early_leave_minutes,
    note, created_by, updated_by
)
SELECT 402, 303, '2025-01-15 11:55:00', '2025-01-15 16:05:00', 'WEB', 0, 0,
       'Đúng giờ, hỗ trợ tốt', 'service_staff', 'service_staff'
WHERE NOT EXISTS (SELECT 1 FROM attendance_records WHERE id = 402);

-- === Performance Adjustments (bonus / penalty) ===
INSERT INTO shift_performance_adjustments (
    id, assignment_id, type, amount, reason, effective_at, revoked, revoked_at,
    revoked_by, created_by, updated_by
)
SELECT 500, 300, 'BONUS', 50000.00, 'Doanh thu cao nhất ca sáng', '2025-01-15 11:10:00', FALSE, NULL,
       NULL, 'manager01', 'manager01'
WHERE NOT EXISTS (SELECT 1 FROM shift_performance_adjustments WHERE id = 500);

INSERT INTO shift_performance_adjustments (
    id, assignment_id, type, amount, reason, effective_at, revoked, revoked_at,
    revoked_by, created_by, updated_by
)
SELECT 501, 301, 'PENALTY', 10000.00, 'Late check-in 5 phút', '2025-01-15 11:00:00', FALSE, NULL,
       NULL, 'manager01', 'manager01'
WHERE NOT EXISTS (SELECT 1 FROM shift_performance_adjustments WHERE id = 501);

INSERT INTO shift_performance_adjustments (
    id, assignment_id, type, amount, reason, effective_at, revoked, revoked_at,
    revoked_by, created_by, updated_by
)
SELECT 502, 303, 'BONUS', 30000.00, 'Khách hàng khen ngợi phục vụ', '2025-01-15 16:10:00', FALSE, NULL,
       NULL, 'manager01', 'manager01'
WHERE NOT EXISTS (SELECT 1 FROM shift_performance_adjustments WHERE id = 502);
