# CMS 内容管理系统 · 设计说明

> 课程：《JAVA 框架技术（一）》课程项目设计  
> 基础：实验二 SSM 整合与 EMP 员工管理 MIS 系统  
> 技术栈：Spring 5.3.39 / SpringMVC 5.3.39 / MyBatis-Plus 3.5.3.1 / Druid 1.2.20 / MySQL 8.0 / Tomcat 9

## 1. 背景与目标

实验二完成的是单角色、单业务的员工管理模块，解决了 SSM 全注解整合、分页、统一返回、全局异常、拦截器与声明式事务等基础问题。课程项目要求在此基础上做一套"企业级"应用，核心考察点从"能不能跑"转向"结构是否合理、权限是否可控、数据是否安全、过程是否可交付"。

因此本项目的目标是：

1. 用 RBAC 模型把"人"和"能做什么"解耦，支持多角色、细粒度接口鉴权；
2. 用多级分类 + 文章状态机承载真实的内容生产流程（投稿 → 审核 → 发布 → 下架）；
3. 用统一的参数校验、异常处理、操作审计把非功能需求补齐；
4. 用可复现的脚本、种子数据和测试用例保证交付质量。

## 2. 角色与用例

| 角色 | 典型用户 | 主要用例 |
| --- | --- | --- |
| ADMIN 系统管理员 | 系统负责人 | 用户增改、角色分配、权限矩阵维护、全部内容操作、日志审计 |
| EDITOR 内容编辑 | 网站编辑 | 新建/编辑文章、提交审核、发布、下架、查看分类 |
| AUDITOR 内容审核 | 审核员 | 审核通过/驳回、查看日志 |

用例图（文字版）：

```
            ┌──────────── 登录 / 退出 / 查看个人信息 ────────────┐
            │                                                    │
   ADMIN ───┼── 用户管理 ── 角色分配 ── 权限矩阵                  │
            ├── 文章管理 ── 审核 ── 发布 / 下架 ── 删除           │
            ├── 分类管理（增删改、树查询）                         │
            └── 操作日志查询                                       │
   EDITOR ──┼── 文章新建 / 编辑 / 提交审核 / 发布 / 下架            │
            └── 分类查看                                           │
   AUDITOR ─┼── 文章审核（通过 / 驳回）                            │
            └── 操作日志查询                                       │
```

## 3. 功能需求

### 3.1 身份认证

- 登录：用户名 + 口令，口令以 SHA-256 摘要比对，失败统一提示"用户名或口令错误"（不暴露账号是否存在）。
- 会话：登录成功后服务端把 `LoginUser`（含角色编码、角色名、权限集合）写入 Session。
- 退出：销毁 Session。
- 拦截：`LoginInterceptor` 拦截除 `/login`、`/health`、静态资源外的全部请求，未登录返回 401 JSON。

### 3.2 权限控制

- 角色与权限为多对多，权限分 MENU（导航）与 API（接口）两类。
- 后端：接口标注 `@RequirePerm("article:audit")`，`PermAspect` 从 Session 取权限集合校验，越权返回 403。
- 前端：登录后按 `permissions` 显隐菜单与按钮，仅作为体验优化，不作为安全边界。
- 已知取舍：权限快照存 Session，角色变更后需重新登录生效。

### 3.3 内容分类

- 支持多级（当前演示两级，模型不限层数），`parent_id` 为 `NULL` 表示一级分类。
- 功能：树查询、下拉选项、新增、修改、删除。
- 规则：编码唯一；父分类必须存在；禁止把自身或后代设为父分类；存在子分类或已绑定文章时禁止删除。

### 3.4 文章

- 状态机：`DRAFT → PENDING → PUBLISHED / REJECTED`，`PUBLISHED → OFFLINE`；`REJECTED` 可编辑后重新提交。
- 列表：标题模糊、分类、状态过滤，分页，白名单排序。
- 详情：返回正文，阅读量自增。
- 写操作：新增、编辑、提交审核、审核、批量发布、下架、删除（逻辑删除）。
- 规则：仅 DRAFT/REJECTED 可编辑；仅 PENDING 可审核；驳回必须填写意见；批量发布只处理 DRAFT/PENDING 并返回条数。

### 3.5 用户与角色

- 用户分页（含角色回填）、新增、修改（口令可选，填写则重新摘要）。
- 角色分配、角色列表、权限树、角色权限矩阵的读取与保存。
- 规则：用户名唯一；口令不明文存储、不返回前端。

### 3.6 操作日志

- 写操作自动记录：操作人、模块、动作、方法、参数（口令打码）、IP、耗时、结果、错误信息。
- 查询：分页 + 按模块/动作过滤。
- 规则：日志写入失败不影响主流程。

## 4. 非功能需求

| 类别 | 要求 | 实现 |
| --- | --- | --- |
| 安全 | 口令不可逆、敏感信息不出库 | SHA-256 + `@JsonIgnore`；日志参数打码 |
| 健壮 | 任何异常都返回统一结构，不暴露堆栈 | `GlobalExceptionHandler` 分类处理 |
| 性能 | 列表不出现 N+1 | `selectBatchIds` 批量回填 |
| 可维护 | 配置外置、依赖版本集中 | `jdbc.properties`、`pom.xml` properties |
| 可交付 | 一条命令重建环境 | `sql/cms_init.sql` + `scripts/run.ps1` |
| 可测试 | 正常与异常路径都有用例 | 12 条单测 + 44 条 HTTP 回归 |

## 5. 架构设计

采用实验二的父子容器结构：

- 父容器 `SpringConfig`：数据源、MyBatis-Plus、事务管理器、Service、Mapper 扫描；
- 子容器 `SpringMvcConfig`：`@EnableWebMvc`、消息转换器、拦截器、Controller、切面；
- 子容器可注入父容器 Bean，父容器看不到 Controller。

请求链路：

```
浏览器 / ApiFox
   → CharacterEncodingFilter(UTF-8)
   → DispatcherServlet
   → LoginInterceptor（未登录 401）
   → Controller（@Valid 校验、@RequirePerm 鉴权、@OpLog 审计）
   → Service（事务边界、业务规则）
   → Mapper（LambdaQueryWrapper / BaseMapper）
   → Druid → MySQL
```

横切关注点：`Result<T>` 统一返回、`GlobalExceptionHandler` 统一异常、`ValidationSupport` 校验消息拼接、`WebLogAspect` 请求日志、`OpLogAspect` 审计入库。

分层约定：Controller 不写业务判断、不直接调 Mapper；Service 负责事务与规则；DTO/VO 与实体分离，避免 `password`、`deleted` 泄露。

## 6. 数据库设计

### 6.1 表清单

| 表 | 说明 | 关键约束 |
| --- | --- | --- |
| `sys_user` | 用户 | `username` 唯一；`deleted` 逻辑删除 |
| `sys_role` | 角色 | `role_code` 唯一 |
| `sys_permission` | 权限 | `perm_code` 唯一；`parent_id` 组织菜单树 |
| `sys_user_role` | 用户-角色 | 联合主键 `(user_id, role_id)`，双外键 |
| `sys_role_permission` | 角色-权限 | 联合主键 `(role_id, perm_id)`，双外键 |
| `cms_category` | 分类 | 自引用外键 `parent_id`，NULL 为一级 |
| `cms_article` | 文章 | 外键 → 分类、作者；状态枚举 |
| `sys_log` | 操作日志 | 弱关联 `user_id`（不建外键） |

索引：各外键列、`cms_article(status, publish_time)`、`sys_log(module, create_time)`。

### 6.2 关系

```
sys_user 1 ── N sys_user_role N ── 1 sys_role 1 ── N sys_role_permission N ── 1 sys_permission
sys_user 1 ── N cms_article          cms_category 1 ── N cms_article
cms_category 1 ── N cms_category（自引用）        sys_log N ── 1 sys_user（逻辑弱关联）
```

可视化版本见 `docs/screenshots/cms-er.png`。

### 6.3 设计要点

- **一级分类用 NULL 而不是 0**：自引用外键要求父行真实存在，`parent_id = 0` 会触发 1452；改为 NULL 后由 `normalizeParent()` 把前端传来的 0 归一化。
- **日志不建外键**：用户删除后日志仍需可查，`user_id` 只作弱关联并冗余 `username` 快照。
- **逻辑删除**：`deleted` + `@TableLogic`，统计与列表自动过滤，历史数据可追溯。
- **审计字段**：`create_time` / `update_time` 由 `MetaObjectHandler` 自动填充。

## 7. 接口设计

完整清单见 [cms-api.md](cms-api.md)，统一约定：

- 路径名词复数、动作用 HTTP 方法表达；状态流转用子资源（`/articles/{id}/audit`）。
- 统一返回 `Result<T>{code,message,data,success}`，HTTP 状态码与业务码一致（400/401/403/404/500）。
- 分页统一 `PageResult<T>{page,size,total,pages,records}`。
- 时间格式统一 `yyyy-MM-dd HH:mm:ss`。

## 8. 关键流程

### 8.1 文章审核

```
EDITOR 新建 → DRAFT
EDITOR 提交 → PENDING（写 submit_time 语义可由 update_time 体现）
AUDITOR 审核 → PUBLISHED（写 publish_time） / REJECTED（写 audit_remark）
EDITOR 修改被驳回文章 → 仍为 REJECTED，改完重新提交 → PENDING
ADMIN/EDITOR 下架 → OFFLINE
```

每次流转校验前置状态，非法流转抛 `BusinessException(400)`。

### 8.2 权限校验

```
请求 → LoginInterceptor（Session 是否有 LoginUser）
     → PermAspect（@RequirePerm 声明的权限是否在 LoginUser.permissions 中）
     → 业务方法
```

ADMIN 在 `LoginUser.has()` 中视为通配。

## 9. 部署与运行

```powershell
# 1. 初始化数据库（会重建业务表并写入种子数据）
mysql -u root -p < sql/cms_init.sql

# 2. 构建并启动（默认 8080）
.\scripts\run.ps1

# 3. 停止
.\scripts\stop.ps1
```

访问 `http://localhost:8080/emp-mis/`，使用 admin / editor / auditor（口令 123456）登录。

手工部署：`mvnw -B -DskipTests clean package` → 复制 `target/emp-mis.war` 到 Tomcat 9 的 `webapps`。

## 10. 测试策略

| 层次 | 方式 | 覆盖 |
| --- | --- | --- |
| 服务层 | JUnit 4 + Spring Test | 分页归一化、增删改查、事务回滚（12 条） |
| 接口层 | HttpClient + CookieContainer 分角色会话 | 认证、权限、状态机、分类树、日志（44 条） |
| 界面 | Chrome headless 截图 | 登录、控制台、文章、分类、用户、日志 |
| 数据 | SQL 校验脚本 | 表条数、索引、外键 |

测试证据：`docs/evidence/cms-http-regression.log`、`docs/evidence/unit-test-output.log`。

## 11. AI 使用说明

AI 参与：建表语句初稿、分层代码骨架、报错原因分析、文档结构。

人工主导：数据模型与状态机定义、权限边界划分、排序白名单与 N+1 优化、全部代码的编译与回归验证、报告中每个数字的核对。

未直接采用 AI 输出的地方：一级分类哨兵值（0 → NULL）、Mapper 方法重载（导致 `TooManyResultsException`）、权限校验位置（从 Controller 的 if 改为 AOP）、前端接口基址（硬编码 → 运行时推断）。