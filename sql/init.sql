-- =====================================================================
--  实验二  EMP 员工管理 MIS 系统 —— 数据库初始化脚本
--  数据库：MySQL 8.0.x        字符集：utf8mb4
--  用法  ：以 root 身份执行  mysql -uroot -p < sql/init.sql
--  说明  ：脚本可重复执行（先删后建）。emp 表在实验一基础上完成精细化设计：
--          1) 以 dept_id 外键关联 dept 表，实现部门一对多；
--          2) 为 emp_name 建立普通索引，为 (status, deleted) 建立联合索引；
--          3) 补充 create_time / update_time 自动时间戳（对应 MP 自动填充）；
--          4) 增加 deleted 逻辑删除标记（对应 MP @TableLogic）；
--          5) 应用连接使用独立账号 emp_app，避免在代码/配置中使用 root 账号。
-- =====================================================================

DROP DATABASE IF EXISTS ssm_emp;
CREATE DATABASE ssm_emp DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE ssm_emp;

-- ---------------------------------------------------------------------
-- 1. 部门表
-- ---------------------------------------------------------------------
CREATE TABLE dept (
    dept_id     INT PRIMARY KEY AUTO_INCREMENT     COMMENT '部门编号',
    dept_name   VARCHAR(50)  NOT NULL              COMMENT '部门名称',
    location    VARCHAR(100) DEFAULT NULL          COMMENT '办公地点',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_dept_name (dept_name)
) ENGINE = InnoDB COMMENT '部门表';

-- ---------------------------------------------------------------------
-- 2. 员工表
-- ---------------------------------------------------------------------
CREATE TABLE emp (
    emp_id      INT PRIMARY KEY AUTO_INCREMENT     COMMENT '员工编号',
    emp_name    VARCHAR(50)   NOT NULL             COMMENT '姓名',
    gender      CHAR(1)       NOT NULL DEFAULT '男' COMMENT '性别（男/女）',
    dept_id     INT           DEFAULT NULL         COMMENT '部门编号（外键 dept.dept_id）',
    post        VARCHAR(50)   DEFAULT NULL         COMMENT '岗位',
    salary      DECIMAL(10,2) DEFAULT NULL         COMMENT '薪资',
    hire_date   DATE          DEFAULT NULL         COMMENT '入职时间',
    status      TINYINT       NOT NULL DEFAULT 1   COMMENT '状态：1 在职，0 离职',
    create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT       NOT NULL DEFAULT 0   COMMENT '逻辑删除：0 未删除，1 已删除',
    KEY idx_emp_name (emp_name),
    KEY idx_dept_id (dept_id),
    KEY idx_status_deleted (status, deleted),
    CONSTRAINT fk_emp_dept FOREIGN KEY (dept_id) REFERENCES dept (dept_id)
) ENGINE = InnoDB COMMENT '员工表';

-- ---------------------------------------------------------------------
-- 3. 系统用户表（登录与权限拦截使用）
-- ---------------------------------------------------------------------
CREATE TABLE sys_user (
    user_id     INT PRIMARY KEY AUTO_INCREMENT     COMMENT '用户编号',
    username    VARCHAR(50)  NOT NULL              COMMENT '登录名',
    password    CHAR(64)     NOT NULL              COMMENT '口令（SHA-256 十六进制，实验用；生产应用 BCrypt）',
    real_name   VARCHAR(50)  DEFAULT NULL          COMMENT '姓名',
    role        VARCHAR(20)  NOT NULL DEFAULT 'USER' COMMENT '角色：ADMIN / USER',
    status      TINYINT      NOT NULL DEFAULT 1    COMMENT '状态：1 启用，0 禁用',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB COMMENT '系统用户表';

-- ---------------------------------------------------------------------
-- 4. 初始数据
-- ---------------------------------------------------------------------
INSERT INTO dept (dept_name, location) VALUES
    ('研发部', 'A座3层'),
    ('市场部', 'B座2层'),
    ('人事部', 'A座1层'),
    ('财务部', 'C座5层');

INSERT INTO emp (emp_name, gender, dept_id, post, salary, hire_date, status) VALUES
    ('张伟', '男', 1, 'Java工程师', 12000.00, '2024-07-01', 1),
    ('李娜', '女', 1, '前端工程师', 10000.00, '2024-08-15', 1),
    ('王强', '男', 2, '市场专员',    8000.00, '2023-03-10', 1),
    ('赵敏', '女', 3, '人事专员',    7500.00, '2022-06-20', 0);

-- 口令 123456 的 SHA-256：8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92
INSERT INTO sys_user (username, password, real_name, role) VALUES
    ('admin', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', '系统管理员', 'ADMIN');

-- ---------------------------------------------------------------------
-- 5. 应用专用账号（最小权限：仅 DML）
-- ---------------------------------------------------------------------
CREATE USER IF NOT EXISTS 'emp_app'@'localhost' IDENTIFIED BY 'Emp@2024';
ALTER USER 'emp_app'@'localhost' IDENTIFIED BY 'Emp@2024';
GRANT SELECT, INSERT, UPDATE, DELETE ON ssm_emp.* TO 'emp_app'@'localhost';
FLUSH PRIVILEGES;

-- ---------------------------------------------------------------------
-- 6. 验证
-- ---------------------------------------------------------------------
SELECT 'dept' AS tbl, COUNT(*) AS cnt FROM dept
UNION ALL SELECT 'emp', COUNT(*) FROM emp
UNION ALL SELECT 'sys_user', COUNT(*) FROM sys_user;