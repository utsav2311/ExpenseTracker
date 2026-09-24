-- ==============================================================================
-- Personal Income & Expense Tracker Database Schema (MySQL)
-- Demonstrates:
--  - Relational Database Design with Primary & Foreign Key Constraints
--  - Data Integrity (NOT NULL, UNIQUE, CHECK constraints)
--  - Useful Indexing (Foreign keys, transaction date, transaction type)
--  - SQL Aggregations, JOINs, and Analytical Reporting
-- ==============================================================================

-- 1. Create and switch to Database
CREATE DATABASE IF NOT EXISTS expense_tracker_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE expense_tracker_db;

-- 2. Drop existing tables if re-initializing (Clean migration from legacy schema)
DROP TABLE IF EXISTS transactions;
DROP TABLE IF EXISTS budgets;
DROP TABLE IF EXISTS categories;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS expenses; -- legacy table cleanup

-- 3. Create Users Table
CREATE TABLE users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Create Categories Table (Supports both INCOME and EXPENSE)
CREATE TABLE categories (
    category_id INT AUTO_INCREMENT PRIMARY KEY,
    category_name VARCHAR(100) NOT NULL,
    category_type ENUM('INCOME', 'EXPENSE') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_cat_name_type (category_name, category_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Create Transactions Table (Unified Income & Expense)
CREATE TABLE transactions (
    transaction_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    category_id INT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL CHECK (amount > 0),
    transaction_type ENUM('INCOME', 'EXPENSE') NOT NULL,
    description VARCHAR(150) NOT NULL,
    notes TEXT,
    payment_method VARCHAR(50) NOT NULL,
    transaction_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Foreign Key Constraints
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES categories(category_id) ON DELETE RESTRICT,

    -- Performance Indexes
    INDEX idx_user_tx (user_id),
    INDEX idx_category_tx (category_id),
    INDEX idx_tx_date (transaction_date),
    INDEX idx_tx_type (transaction_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Create Monthly Budgets Table (Applies to Monthly Expenses)
CREATE TABLE budgets (
    budget_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    month INT NOT NULL CHECK (month BETWEEN 1 AND 12),
    year INT NOT NULL CHECK (year >= 2000),
    budget_amount DECIMAL(10, 2) NOT NULL CHECK (budget_amount >= 0),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uk_user_month_year (user_id, month, year),
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================================================
-- INITIAL SEED DATA
-- ==============================================================================

-- Default User
INSERT INTO users (user_id, name, email) VALUES
(1, 'Utsav Kumar', 'utsav@example.com');

-- Default Categories (Income)
INSERT INTO categories (category_name, category_type) VALUES
('Salary', 'INCOME'),
('Freelance', 'INCOME'),
('Bonus', 'INCOME'),
('Investment', 'INCOME'),
('Other Income', 'INCOME');

-- Default Categories (Expense)
INSERT INTO categories (category_name, category_type) VALUES
('Food', 'EXPENSE'),
('Travel', 'EXPENSE'),
('Shopping', 'EXPENSE'),
('Bills', 'EXPENSE'),
('Entertainment', 'EXPENSE'),
('Education', 'EXPENSE'),
('Health', 'EXPENSE'),
('Rent', 'EXPENSE'),
('Other Expense', 'EXPENSE');

-- Seed Sample Realistic Transactions (INR / ₹) for User 1
-- Income: ₹50,000 + ₹15,000 = ₹65,000
-- Expenses: ₹15,000 + ₹750 + ₹1,200 + ₹2,400 + ₹3,150 = ₹22,500
-- Balance: ₹65,000 - ₹22,500 = ₹42,500
INSERT INTO transactions (user_id, category_id, amount, transaction_type, description, notes, payment_method, transaction_date) VALUES
(1, (SELECT category_id FROM categories WHERE category_name='Salary' AND category_type='INCOME'), 50000.00, 'INCOME', 'Monthly Tech Salary', 'Direct employer credit', 'Net Banking', CURDATE() - INTERVAL 5 DAY),
(1, (SELECT category_id FROM categories WHERE category_name='Freelance' AND category_type='INCOME'), 15000.00, 'INCOME', 'Frontend Consulting Project', 'Milestone 1 payment', 'UPI', CURDATE() - INTERVAL 2 DAY),
(1, (SELECT category_id FROM categories WHERE category_name='Rent' AND category_type='EXPENSE'), 15000.00, 'EXPENSE', 'Apartment Rent', 'Monthly residential rent', 'Net Banking', CURDATE() - INTERVAL 4 DAY),
(1, (SELECT category_id FROM categories WHERE category_name='Food' AND category_type='EXPENSE'), 750.00, 'EXPENSE', 'Dinner with Friends', 'Weekend dining outing', 'UPI', CURDATE() - INTERVAL 1 DAY),
(1, (SELECT category_id FROM categories WHERE category_name='Travel' AND category_type='EXPENSE'), 1200.00, 'EXPENSE', 'Metro & Cab Travel', 'Weekly office commute', 'Debit Card', CURDATE() - INTERVAL 3 DAY),
(1, (SELECT category_id FROM categories WHERE category_name='Bills' AND category_type='EXPENSE'), 2400.00, 'EXPENSE', 'Electricity & Wi-Fi Bills', 'Utility payments', 'UPI', CURDATE() - INTERVAL 4 DAY),
(1, (SELECT category_id FROM categories WHERE category_name='Shopping' AND category_type='EXPENSE'), 3150.00, 'EXPENSE', 'Clothing & Essentials', 'Weekend shopping mall', 'Credit Card', CURDATE() - INTERVAL 6 DAY);

-- Seed Monthly Budget for User 1 (Current Month & Year): ₹30,000
INSERT INTO budgets (user_id, month, year, budget_amount) VALUES
(1, MONTH(CURDATE()), YEAR(CURDATE()), 30000.00);

-- ==============================================================================
-- ANALYTICAL QUERIES (Interview Demonstration)
-- ==============================================================================

-- Q1: Current Balance (Total Income - Total Expenses)
-- SELECT
--     COALESCE(SUM(CASE WHEN transaction_type = 'INCOME' THEN amount ELSE 0 END), 0.00) AS total_income,
--     COALESCE(SUM(CASE WHEN transaction_type = 'EXPENSE' THEN amount ELSE 0 END), 0.00) AS total_expense,
--     (COALESCE(SUM(CASE WHEN transaction_type = 'INCOME' THEN amount ELSE 0 END), 0.00) -
--      COALESCE(SUM(CASE WHEN transaction_type = 'EXPENSE' THEN amount ELSE 0 END), 0.00)) AS current_balance
-- FROM transactions
-- WHERE user_id = 1;

-- Q2: Expense Breakdown by Category with SQL JOIN
-- SELECT
--     c.category_id,
--     c.category_name,
--     SUM(t.amount) AS total_amount,
--     COUNT(t.transaction_id) AS transaction_count
-- FROM transactions t
-- JOIN categories c ON t.category_id = c.category_id
-- WHERE t.user_id = 1 AND t.transaction_type = 'EXPENSE'
-- GROUP BY c.category_id, c.category_name
-- ORDER BY total_amount DESC;

-- Q3: Top 5 Highest Expenses
-- SELECT t.transaction_id, t.description, t.amount, c.category_name, t.transaction_date
-- FROM transactions t
-- JOIN categories c ON t.category_id = c.category_id
-- WHERE t.user_id = 1 AND t.transaction_type = 'EXPENSE'
-- ORDER BY t.amount DESC
-- LIMIT 5;
