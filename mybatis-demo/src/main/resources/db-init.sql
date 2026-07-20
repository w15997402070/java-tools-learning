-- H2 数据库初始化脚本
-- 在连接时自动执行（见 mybatis-config.xml 中的 INIT 参数）

-- 创建用户表
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(100),
    age INT,
    city VARCHAR(50)
);

-- 创建订单表
CREATE TABLE IF NOT EXISTS orders (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    order_no VARCHAR(32) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 插入初始用户数据
INSERT INTO users (username, email, age, city) VALUES
('张三', 'zhangsan@example.com', 28, '北京'),
('李四', 'lisi@example.com', 35, '上海'),
('王五', 'wangwu@example.com', 22, '广州'),
('赵六', 'zhaoliu@example.com', 30, '北京'),
('孙七', 'sunqi@example.com', 26, '深圳');

-- 插入初始订单数据
INSERT INTO orders (user_id, order_no, amount, status, create_time) VALUES
(1, 'ORD2024001', 199.99, 'PAID', '2024-01-15 10:30:00'),
(1, 'ORD2024002', 59.50, 'PAID', '2024-01-16 14:20:00'),
(1, 'ORD2024003', 299.00, 'SHIPPED', '2024-01-17 09:00:00'),
(2, 'ORD2024004', 1280.00, 'PAID', '2024-01-15 11:00:00'),
(2, 'ORD2024005', 45.00, 'CANCELLED', '2024-01-16 16:30:00'),
(3, 'ORD2024006', 399.00, 'PAID', '2024-01-15 08:45:00'),
(4, 'ORD2024007', 899.00, 'SHIPPED', '2024-01-17 13:00:00'),
(5, 'ORD2024008', 29.90, 'PAID', '2024-01-18 10:00:00');
