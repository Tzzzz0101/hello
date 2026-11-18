-- ============================================================================
--  企业级 CMS 内容管理系统 · 数据库初始化脚本（在实验二 ssm_emp 库上升级）
--  六表核心：sys_user / sys_role / sys_permission / cms_article / cms_category / sys_log
--  关联表：sys_user_role / sys_role_permission
--  执行方式：mysql -u root -p < sql/cms_init.sql
-- ============================================================================
USE ssm_emp;

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------------
-- 1. 用户表（口令只存 SHA-256 摘要，禁止明文）
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS sys_user_role;
DROP TABLE IF EXISTS sys_role_permission;
DROP TABLE IF EXISTS sys_log;
DROP TABLE IF EXISTS cms_article;
DROP TABLE IF EXISTS cms_category;
DROP TABLE IF EXISTS sys_permission;
DROP TABLE IF EXISTS sys_role;
DROP TABLE IF EXISTS sys_user;

CREATE TABLE sys_user (
    user_id     INT           NOT NULL AUTO_INCREMENT COMMENT '用户编号',
    username    VARCHAR(32)   NOT NULL COMMENT '登录名',
    password    CHAR(64)      NOT NULL COMMENT 'SHA-256 口令摘要',
    real_name   VARCHAR(50)   NOT NULL COMMENT '姓名',
    email       VARCHAR(100)  DEFAULT NULL COMMENT '邮箱',
    phone       VARCHAR(20)   DEFAULT NULL COMMENT '手机号',
    status      TINYINT       NOT NULL DEFAULT 1 COMMENT '1 启用 / 0 禁用',
    create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删 / 1 已删',
    PRIMARY KEY (user_id),
    UNIQUE KEY uk_user_username (username),
    KEY idx_user_status (status, deleted)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '系统用户';

-- ---------------------------------------------------------------------------
-- 2. 角色表
-- ---------------------------------------------------------------------------
CREATE TABLE sys_role (
    role_id     INT          NOT NULL AUTO_INCREMENT,
    role_code   VARCHAR(32)  NOT NULL COMMENT '角色编码，如 ADMIN / EDITOR / AUDITOR',
    role_name   VARCHAR(50)  NOT NULL,
    description VARCHAR(200) DEFAULT NULL,
    status      TINYINT      NOT NULL DEFAULT 1,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (role_id),
    UNIQUE KEY uk_role_code (role_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '角色';

-- ---------------------------------------------------------------------------
-- 3. 权限表（菜单 / 接口按钮级权限，parent_id 组织成树）
-- ---------------------------------------------------------------------------
CREATE TABLE sys_permission (
    perm_id     INT          NOT NULL AUTO_INCREMENT,
    perm_code   VARCHAR(64)  NOT NULL COMMENT '权限标识，如 article:audit',
    perm_name   VARCHAR(50)  NOT NULL,
    perm_type   VARCHAR(10)  NOT NULL DEFAULT 'API' COMMENT 'MENU / API',
    parent_id   INT          NOT NULL DEFAULT 0,
    path        VARCHAR(100) DEFAULT NULL COMMENT '前端路由或接口前缀',
    sort        INT          NOT NULL DEFAULT 0,
    status      TINYINT      NOT NULL DEFAULT 1,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (perm_id),
    UNIQUE KEY uk_perm_code (perm_code),
    KEY idx_perm_parent (parent_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '权限';

-- ---------------------------------------------------------------------------
-- 4. 用户-角色、角色-权限（联合主键，多对多）
-- ---------------------------------------------------------------------------
CREATE TABLE sys_user_role (
    user_id INT NOT NULL,
    role_id INT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    KEY idx_ur_role (role_id),
    CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES sys_user (user_id),
    CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES sys_role (role_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '用户角色关联';

CREATE TABLE sys_role_permission (
    role_id INT NOT NULL,
    perm_id INT NOT NULL,
    PRIMARY KEY (role_id, perm_id),
    KEY idx_rp_perm (perm_id),
    CONSTRAINT fk_rp_role FOREIGN KEY (role_id) REFERENCES sys_role (role_id),
    CONSTRAINT fk_rp_perm FOREIGN KEY (perm_id) REFERENCES sys_permission (perm_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '角色权限关联';

-- ---------------------------------------------------------------------------
-- 5. 内容分类（parent_id 为 NULL 表示一级分类，支持多级）
--    parent_id 用 NULL 而不是 0 作为“无上级”，才能让自引用外键成立
-- ---------------------------------------------------------------------------
CREATE TABLE cms_category (
    category_id   INT          NOT NULL AUTO_INCREMENT,
    parent_id     INT          DEFAULT NULL COMMENT '父分类；NULL 为一级分类',
    category_name VARCHAR(50)  NOT NULL,
    category_code VARCHAR(50)  NOT NULL,
    sort          INT          NOT NULL DEFAULT 0,
    status        TINYINT      NOT NULL DEFAULT 1 COMMENT '1 启用 / 0 停用',
    description   VARCHAR(200) DEFAULT NULL,
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted       TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (category_id),
    UNIQUE KEY uk_category_code (category_code),
    KEY idx_category_parent (parent_id, sort),
    CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES cms_category (category_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '内容分类';

-- ---------------------------------------------------------------------------
-- 6. 文章（状态机：DRAFT 草稿 → PENDING 待审 → PUBLISHED 已发布 / REJECTED 驳回，
--    PUBLISHED 可 OFFLINE 下架）
-- ---------------------------------------------------------------------------
CREATE TABLE cms_article (
    article_id   BIGINT       NOT NULL AUTO_INCREMENT,
    title        VARCHAR(120) NOT NULL,
    summary      VARCHAR(255) DEFAULT NULL,
    content      MEDIUMTEXT   NOT NULL,
    category_id  INT          NOT NULL,
    author_id    INT          NOT NULL,
    status       VARCHAR(16)  NOT NULL DEFAULT 'DRAFT',
    audit_remark VARCHAR(255) DEFAULT NULL,
    view_count   INT          NOT NULL DEFAULT 0,
    publish_time DATETIME     DEFAULT NULL,
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted      TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (article_id),
    KEY idx_article_cat (category_id, status, deleted),
    KEY idx_article_author (author_id),
    KEY idx_article_publish (publish_time),
    CONSTRAINT fk_article_category FOREIGN KEY (category_id) REFERENCES cms_category (category_id),
    CONSTRAINT fk_article_author FOREIGN KEY (author_id) REFERENCES sys_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '文章内容';

-- ---------------------------------------------------------------------------
-- 7. 操作日志
-- ---------------------------------------------------------------------------
CREATE TABLE sys_log (
    log_id      BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     INT          DEFAULT NULL,
    username    VARCHAR(32)  DEFAULT NULL,
    module      VARCHAR(32)  NOT NULL,
    operation   VARCHAR(64)  NOT NULL,
    method      VARCHAR(160) DEFAULT NULL,
    params      VARCHAR(1000) DEFAULT NULL,
    ip          VARCHAR(45)  DEFAULT NULL,
    cost_ms     INT          DEFAULT NULL,
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1 成功 / 0 失败',
    error_msg   VARCHAR(500) DEFAULT NULL,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (log_id),
    KEY idx_log_time (create_time),
    KEY idx_log_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '操作日志';

-- ---------------------------------------------------------------------------
-- 8. 种子数据：3 个角色、15 条权限、3 个账号（口令均为 123456 的 SHA-256）
-- ---------------------------------------------------------------------------
INSERT INTO sys_role (role_code, role_name, description) VALUES
('ADMIN',   '系统管理员', '全部权限，含用户角色管理与日志审计'),
('EDITOR',  '内容编辑',   '文章与分类的增删改查，可提交审核'),
('AUDITOR', '内容审核',   '审核文章、查看操作日志');

INSERT INTO sys_permission (perm_code, perm_name, perm_type, parent_id, path, sort) VALUES
('dashboard:view',   '查看控制台',   'MENU', 0, '/dashboard',  1),
('article:list',     '文章列表',     'MENU', 0, '/articles',   2),
('article:view',     '文章详情',     'API',  2, NULL,          3),
('article:add',      '新增文章',     'API',  2, NULL,          4),
('article:edit',     '编辑文章',     'API',  2, NULL,          5),
('article:delete',   '删除文章',     'API',  2, NULL,          6),
('article:publish',  '发布/下架',    'API',  2, NULL,          7),
('article:audit',    '审核文章',     'API',  2, NULL,          8),
('category:list',    '分类列表',     'MENU', 0, '/categories', 9),
('category:add',     '新增分类',     'API',  9, NULL,         10),
('category:edit',    '编辑分类',     'API',  9, NULL,         11),
('category:delete',  '删除分类',     'API',  9, NULL,         12),
('user:list',        '用户管理',     'MENU', 0, '/users',     13),
('user:save',        '新增/编辑用户','API', 13, NULL,         14),
('role:assign',      '分配角色',     'API', 13, NULL,         15),
('log:list',         '操作日志',     'MENU', 0, '/logs',      16);

INSERT INTO sys_user (username, password, real_name, email, phone, status) VALUES
('admin',   '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', '系统管理员', 'admin@lab.local',   '13800000001', 1),
('editor',  '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', '内容编辑',   'editor@lab.local',  '13800000002', 1),
('auditor', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', '内容审核员', 'auditor@lab.local', '13800000003', 1);

-- admin → ADMIN；editor → EDITOR；auditor → AUDITOR
INSERT INTO sys_user_role (user_id, role_id)
SELECT u.user_id, r.role_id FROM sys_user u JOIN sys_role r
  ON (u.username = 'admin' AND r.role_code = 'ADMIN')
  OR (u.username = 'editor' AND r.role_code = 'EDITOR')
  OR (u.username = 'auditor' AND r.role_code = 'AUDITOR');

-- ADMIN 拥有全部权限
INSERT INTO sys_role_permission (role_id, perm_id)
SELECT r.role_id, p.perm_id FROM sys_role r CROSS JOIN sys_permission p WHERE r.role_code = 'ADMIN';

-- EDITOR：控制台 + 文章读写 + 分类只读
INSERT INTO sys_role_permission (role_id, perm_id)
SELECT r.role_id, p.perm_id FROM sys_role r JOIN sys_permission p
  ON p.perm_code IN ('dashboard:view', 'article:list', 'article:view', 'article:add', 'article:edit',
                     'article:delete', 'article:publish', 'category:list')
WHERE r.role_code = 'EDITOR';

-- AUDITOR：控制台 + 文章查看/审核 + 日志
INSERT INTO sys_role_permission (role_id, perm_id)
SELECT r.role_id, p.perm_id FROM sys_role r JOIN sys_permission p
  ON p.perm_code IN ('dashboard:view', 'article:list', 'article:view', 'article:audit', 'log:list')
WHERE r.role_code = 'AUDITOR';

INSERT INTO cms_category (parent_id, category_name, category_code, sort, status, description) VALUES
(NULL, '新闻中心', 'news',   1, 1, '公司新闻与行业资讯'),
(NULL, '产品技术', 'tech',   2, 1, '产品与技术文档'),
(NULL, '帮助中心', 'help',   3, 1, '常见问题与使用指引');

INSERT INTO cms_category (parent_id, category_name, category_code, sort, status, description)
SELECT c.category_id, '公司动态', 'news-company', 1, 1, '公司内部动态' FROM cms_category c WHERE c.category_code = 'news';
INSERT INTO cms_category (parent_id, category_name, category_code, sort, status, description)
SELECT c.category_id, '行业资讯', 'news-industry', 2, 1, '行业新闻' FROM cms_category c WHERE c.category_code = 'news';
INSERT INTO cms_category (parent_id, category_name, category_code, sort, status, description)
SELECT c.category_id, '技术文档', 'tech-doc', 1, 1, '开发与运维文档' FROM cms_category c WHERE c.category_code = 'tech';

INSERT INTO cms_article (title, summary, content, category_id, author_id, status, view_count, publish_time)
SELECT 'SSM 全注解整合实践', '从 web.xml 到全注解的迁移要点',
       '<p>使用 AbstractAnnotationConfigDispatcherServletInitializer 替代 web.xml，父容器负责数据源与事务，子容器负责 MVC。</p>',
       c.category_id, u.user_id, 'PUBLISHED', 128, NOW()
FROM cms_category c, sys_user u WHERE c.category_code = 'tech-doc' AND u.username = 'editor';

INSERT INTO cms_article (title, summary, content, category_id, author_id, status, view_count, publish_time)
SELECT 'MyBatis-Plus 分页与逻辑删除', '分页插件与 @TableLogic 的组合用法',
       '<p>分页插件改写 COUNT 与 LIMIT，逻辑删除自动追加 deleted = 0。</p>',
       c.category_id, u.user_id, 'PUBLISHED', 96, NOW()
FROM cms_category c, sys_user u WHERE c.category_code = 'tech-doc' AND u.username = 'editor';

INSERT INTO cms_article (title, summary, content, category_id, author_id, status, view_count)
SELECT '内容审核流程说明', '草稿 → 待审 → 发布 / 驳回',
       '<p>编辑提交审核后由审核员处理，驳回需填写原因。</p>',
       c.category_id, u.user_id, 'PENDING', 0
FROM cms_category c, sys_user u WHERE c.category_code = 'help' AND u.username = 'editor';

INSERT INTO cms_article (title, summary, content, category_id, author_id, status, view_count)
SELECT '2024 年度技术分享会', '活动预告',
       '<p>本周五下午在 B205 举办技术分享会。</p>',
       c.category_id, u.user_id, 'DRAFT', 0
FROM cms_category c, sys_user u WHERE c.category_code = 'news-company' AND u.username = 'editor';

-- 校验
SELECT 'user' AS tbl, COUNT(*) AS cnt FROM sys_user
UNION ALL SELECT 'role', COUNT(*) FROM sys_role
UNION ALL SELECT 'permission', COUNT(*) FROM sys_permission
UNION ALL SELECT 'category', COUNT(*) FROM cms_category
UNION ALL SELECT 'article', COUNT(*) FROM cms_article;