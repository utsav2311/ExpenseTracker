-- ==============================================================================
-- Personal Expense Tracker Database Schema (MySQL)
-- Demonstrates:
--  - Relational Schema Design (DDL)
--  - Integrity Constraints (Primary Key, Not Null, Unique, Check)
--  - Database Indexing for fast search and range queries
--  - Analytical SQL queries (GROUP BY, JOIN, Aggregations)
-- ==============================================================================

-- 1. Create and switch to Database
CREATE DATABASE IF NOT EXISTS expense_tracker_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE expense_tracker_db;

-- 2. Drop existing tables if re-initializing
DROP TABLE IF EXISTS budgets;
DROP TABLE IF EXISTS expenses;

-- 3. Create Expenses Table
CREATE TABLE expenses (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(150) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL CHECK (amount > 0),
    category VARCHAR(50) NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    expense_date DATE NOT NULL,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- Performance Indexes
    INDEX idx_expense_category (category),
    INDEX idx_expense_date (expense_date),
    INDEX idx_expense_amount (amount)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Create Monthly Budgets Table
CREATE TABLE budgets (
    id INT AUTO_INCREMENT PRIMARY KEY,
    category VARCHAR(50) NOT NULL,
    monthly_limit DECIMAL(10, 2) NOT NULL CHECK (monthly_limit >= 0),
    month_year VARCHAR(7) NOT NULL COMMENT 'Format YYYY-MM',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- Guarantee only one budget per category per month
    UNIQUE KEY uk_cat_month (category, month_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Seed Realistic Sample Expenses
INSERT INTO expenses (title, amount, category, payment_method, expense_date, notes) VALUES
('Supermarket Groceries', 142.50, 'FOOD', 'CREDIT_CARD', CURDATE() - INTERVAL 1 DAY, 'Weekly essentials: dairy, fruits, veggies'),
('Apartment Rent', 1250.00, 'HOUSING', 'NET_BANKING', CURDATE() - INTERVAL 5 DAY, 'Monthly 1BHK apartment rent'),
('Subway Metro Pass', 55.00, 'TRANSPORTATION', 'DEBIT_CARD', CURDATE() - INTERVAL 3 DAY, 'Monthly unlimited metro transit card'),
('Electricity & Water Bill', 94.20, 'UTILITIES', 'UPI', CURDATE() - INTERVAL 4 DAY, 'Utility bills for current billing cycle'),
('IMAX Movie Night', 38.50, 'ENTERTAINMENT', 'UPI', CURDATE() - INTERVAL 2 DAY, 'Sci-Fi premiere ticket and snacks'),
('Health Checkup & Meds', 65.00, 'HEALTHCARE', 'CASH', CURDATE() - INTERVAL 6 DAY, 'Routine clinical consultation and medicines'),
('Running Shoes', 89.99, 'SHOPPING', 'CREDIT_CARD', CURDATE() - INTERVAL 7 DAY, 'Sportswear shoes from retail outlet'),
('Java Fullstack Course', 19.99, 'EDUCATION', 'CREDIT_CARD', CURDATE() - INTERVAL 8 DAY, 'Online technical certifications'),
('Morning Latte & Pastry', 8.75, 'FOOD', 'UPI', CURDATE(), 'Breakfast at neighborhood cafe'),
('Uber Airport Ride', 34.20, 'TRANSPORTATION', 'CREDIT_CARD', CURDATE() - INTERVAL 9 DAY, 'Airport terminal taxi pickup'),
('High-Speed Internet Fiber', 49.99, 'UTILITIES', 'NET_BANKING', CURDATE() - INTERVAL 10 DAY, '300 Mbps broadband monthly subscription'),
('Barber Haircut & Grooming', 25.00, 'PERSONAL', 'CASH', CURDATE() - INTERVAL 11 DAY, 'Haircut and grooming service'),
('Desk Ergonomic Chair', 189.00, 'SHOPPING', 'CREDIT_CARD', CURDATE() - INTERVAL 12 DAY, 'Home office furniture upgrade'),
('Dinner with Family', 112.40, 'FOOD', 'CREDIT_CARD', CURDATE() - INTERVAL 13 DAY, 'Italian bistro dinner');

-- 6. Seed Sample Monthly Budgets (Current Month)
INSERT INTO budgets (category, monthly_limit, month_year) VALUES
('FOOD', 450.00, DATE_FORMAT(CURDATE(), '%Y-%m')),
('HOUSING', 1300.00, DATE_FORMAT(CURDATE(), '%Y-%m')),
('TRANSPORTATION', 150.00, DATE_FORMAT(CURDATE(), '%Y-%m')),
('UTILITIES', 180.00, DATE_FORMAT(CURDATE(), '%Y-%m')),
('ENTERTAINMENT', 100.00, DATE_FORMAT(CURDATE(), '%Y-%m')),
('SHOPPING', 250.00, DATE_FORMAT(CURDATE(), '%Y-%m')),
('HEALTHCARE', 120.00, DATE_FORMAT(CURDATE(), '%Y-%m'));

-- ==============================================================================
-- ANALYTICAL QUERIES (Interview Demonstration)
-- ==============================================================================

-- Q1: Total spending grouped by category with transaction count
-- SELECT category, COUNT(*) AS count, SUM(amount) AS total_spent, AVG(amount) AS avg_spent
-- FROM expenses
-- GROUP BY category
-- ORDER BY total_spent DESC;

-- Q2: Monthly spending trends
-- SELECT DATE_FORMAT(expense_date, '%Y-%m') AS month, SUM(amount) AS monthly_total
-- FROM expenses
-- GROUP BY DATE_FORMAT(expense_date, '%Y-%m')
-- ORDER BY month DESC;

-- Q3: Budget vs Actual Spending Comparison
-- SELECT
--     b.category,
--     b.monthly_limit,
--     COALESCE(SUM(e.amount), 0.00) AS actual_spent,
--     (b.monthly_limit - COALESCE(SUM(e.amount), 0.00)) AS remaining_budget,
--     ROUND((COALESCE(SUM(e.amount), 0.00) / b.monthly_limit) * 100, 1) AS percent_used
-- FROM budgets b
-- LEFT JOIN expenses e
--     ON b.category = e.category
--     AND DATE_FORMAT(e.expense_date, '%Y-%m') = b.month_year
-- WHERE b.month_year = DATE_FORMAT(CURDATE(), '%Y-%m')
-- GROUP BY b.category, b.monthly_limit, b.month_year;
