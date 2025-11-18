CREATE TABLE roles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(200) NOT NULL,
    full_name VARCHAR(100),
    status VARCHAR(20),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE login_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    success BOOLEAN NOT NULL,
    ip_address VARCHAR(45),
    user_agent VARCHAR(255),
    message VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE users_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_users_roles_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_users_roles_role FOREIGN KEY (role_id) REFERENCES roles(id)
);

CREATE TABLE customers (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    phone VARCHAR(20) NOT NULL UNIQUE,
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(100),
    loyalty_points INT DEFAULT 0,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE tables (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL UNIQUE,
    capacity INT NOT NULL,
    status VARCHAR(20) NOT NULL
);

CREATE TABLE categories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255)
);

CREATE TABLE vouchers (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    type VARCHAR(30) NOT NULL,
    discount_value DECIMAL(12,2) NOT NULL,
    minimum_order_amount DECIMAL(12,2),
    maximum_discount_amount DECIMAL(12,2),
    valid_from TIMESTAMP,
    valid_to TIMESTAMP,
    usage_limit INT,
    times_used INT DEFAULT 0,
    active BOOLEAN DEFAULT TRUE
);

CREATE TABLE products (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(150) NOT NULL,
    code VARCHAR(50) UNIQUE,
    price DECIMAL(12,2) NOT NULL,
    cost DECIMAL(12,2),
    description TEXT,
    image_url VARCHAR(255),
    is_available BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    category_id BIGINT NOT NULL,
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories(id)
);

CREATE TABLE orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    table_id BIGINT,
    user_id BIGINT NOT NULL,
    customer_id BIGINT,
    voucher_id BIGINT,
    type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    sub_total DECIMAL(12,2) NOT NULL,
    discount_amount DECIMAL(12,2) DEFAULT 0,
    total_amount DECIMAL(12,2) NOT NULL,
    voucher_code VARCHAR(50),
    created_at TIMESTAMP,
    paid_at TIMESTAMP,
    payment_method VARCHAR(20),
    CONSTRAINT fk_orders_table FOREIGN KEY (table_id) REFERENCES tables(id),
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
    CONSTRAINT fk_orders_voucher FOREIGN KEY (voucher_id) REFERENCES vouchers(id)
);

CREATE TABLE order_details (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    price_at_order DECIMAL(12,2) NOT NULL,
    notes VARCHAR(255),
    CONSTRAINT fk_order_details_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_order_details_product FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE TABLE ingredients (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL,
    quantity_on_hand DECIMAL(12,2) DEFAULT 0,
    reorder_level DECIMAL(12,2) DEFAULT 0
);

CREATE TABLE product_ingredients (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    ingredient_id BIGINT NOT NULL,
    quantity_needed DECIMAL(12,2) NOT NULL,
    CONSTRAINT fk_product_ingredients_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_product_ingredients_ingredient FOREIGN KEY (ingredient_id) REFERENCES ingredients(id)
);

CREATE TABLE payroll_cycles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(50) NOT NULL UNIQUE,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(30)
);

CREATE TABLE payroll_summaries (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    cycle_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    total_net_payroll DECIMAL(12,2) DEFAULT 0,
    total_bonus DECIMAL(12,2) DEFAULT 0,
    total_penalty DECIMAL(12,2) DEFAULT 0,
    total_adjustment DECIMAL(12,2) DEFAULT 0,
    CONSTRAINT fk_payroll_summaries_cycle FOREIGN KEY (cycle_id) REFERENCES payroll_cycles(id),
    CONSTRAINT fk_payroll_summaries_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE attendance_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    assignment_id BIGINT,
    check_in_at TIMESTAMP,
    check_out_at TIMESTAMP,
    late_minutes INT,
    early_leave_minutes INT
);

CREATE TABLE shift_templates (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL UNIQUE,
    start_time TIME,
    end_time TIME
);

CREATE TABLE shift_instances (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    template_id BIGINT,
    shift_date DATE,
    start_time TIME,
    end_time TIME,
    status VARCHAR(20),
    CONSTRAINT fk_shift_instances_template FOREIGN KEY (template_id) REFERENCES shift_templates(id)
);

CREATE TABLE shift_assignments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    shift_instance_id BIGINT,
    user_id BIGINT,
    status VARCHAR(20),
    total_orders INT,
    total_revenue DECIMAL(12,2),
    CONSTRAINT fk_shift_assignments_instance FOREIGN KEY (shift_instance_id) REFERENCES shift_instances(id),
    CONSTRAINT fk_shift_assignments_user FOREIGN KEY (user_id) REFERENCES users(id)
);
