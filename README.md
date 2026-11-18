# 企业级 CMS 内容管理系统

> 《JAVA 框架技术（一）》课程项目设计 · 在实验二 SSM 整合工程上迭代升级

基于 **Spring 5.3 + SpringMVC 5.3 + MyBatis-Plus 3.5** 的全注解零 `web.xml` 项目：在实验二的员工管理模块之上，新增 RBAC 权限体系（用户 / 角色 / 权限）、多级内容分类、文章全生命周期管理（草稿 → 待审 → 发布 / 驳回 → 下架）、操作日志审计与统一异常处理。实验二的员工模块完整保留，页面入口为 `/employee.html`。

## 1. 技术栈

| 组件 | 版本 | 说明 |
| --- | --- | --- |
| JDK | 25（`--release 8` 编译） | 字节码目标版本 1.8，兼容课程要求的 JDK 8/11 运行环境 |
| Maven | 3.9.6（`mvnw` 包装器） | 无需本地安装 Maven |
| Spring / SpringMVC | 5.3.39 | `javax.servlet` 体系，配合 Tomcat 9 |
| MyBatis-Plus | 3.5.3.1 | `BaseMapper` + `LambdaQueryWrapper` + 分页插件 + 逻辑删除 + 自动填充 |
| Druid | 1.2.20 | 连接池（`initialSize=5`、`maxActive=20`） |
| MySQL | 8.0.45 | 库名 `ssm_emp`，InnoDB、utf8mb4 |
| Tomcat | 9.0.109 | Servlet 4.0，`contextPath = /emp-mis` |
| Hibernate Validator | 6.2.5.Final | JSR-303 参数校验 |
| Jackson | 2.15.3 | JSON 序列化，`JavaTimeModule` 支持 `LocalDate` / `LocalDateTime` |
| Logback | 1.2.13 | 控制台 + 文件日志，Mapper 包 DEBUG 输出 SQL 与参数 |
| JUnit 4 + Spring Test | 4.13.2 / 5.3.39 | 服务层用例在真实 Spring 容器与事务中运行 |

## 2. 功能概览

| 模块 | 能力 |
| --- | --- |
| 身份认证 | 登录 / 退出 / 当前用户；Session 保存 `LoginUser`（含角色与权限快照） |
| 权限控制 | `@RequirePerm` + `PermAspect` 接口级鉴权，越权返回 403；前端按权限显隐菜单 |
| 内容分类 | 多级树、增删改、排序、启停；防环；有子分类或文章时禁止删除 |
| 文章管理 | 列表（标题模糊 / 分类 / 状态 / 分页 / 白名单排序）、详情、新增、编辑、批量发布、下架、逻辑删除 |
| 审核流程 | 提交审核（DRAFT/REJECTED → PENDING）、通过（→ PUBLISHED）、驳回（必须填意见） |
| 用户与角色 | 用户分页（含角色）、增改、角色分配、角色权限矩阵 |
| 操作日志 | `@OpLog` 自动记录操作人、参数（口令打码）、IP、耗时、结果；分页查询 |
| 工程能力 | 统一返回 `Result<T>`、全局异常、JSR-303 校验、逻辑删除、N+1 优化 |
| 实验二模块 | 员工增删改查、多条件分页、部门统计、事务失效演示（`/employee.html`） |

权限模型（16 条权限、3 个内置角色）：

| 角色 | 编码 | 权限 |
| --- | --- | --- |
| 系统管理员 | `ADMIN` | 全部 16 条 |
| 内容编辑 | `EDITOR` | 文章 list/view/add/edit/delete/publish、分类 list、控制台 |
| 内容审核 | `AUDITOR` | 文章 list/view/audit、日志 list、控制台 |

## 3. 目录结构

```
sy2ds
├── pom.xml                     # war 打包，finalName=emp-mis
├── scripts/                    # run.ps1（打包+部署+启动）/ stop.ps1（停止）
├── sql/
│   ├── cms_init.sql            # CMS 全量建表 + 权限/角色/账号/分类/文章种子数据
│   └── init.sql                # 实验二：员工模块建库脚本
├── docs/                       # 设计文档、接口清单、测试证据、截图、报告
├── src/main/java/com/lab/emp
│   ├── config/                 # SpringConfig / SpringMvcConfig / ServletContainersInitConfig / MybatisPlusConfig
│   ├── controller/             # Auth / Article / Category / Rbac / Log / Dashboard（CMS）+ Emp / Dept / Health
│   ├── service/ + service/impl # 业务接口与实现（事务边界）
│   ├── mapper/                 # MyBatis-Plus Mapper（复杂查询用 XML / @Select）
│   ├── entity/                 # 与表一一对应（含 @TableLogic、自动填充字段）
│   ├── dto/                    # 入参 DTO（带校验注解）、出参 VO、LoginUser
│   ├── common/                 # Result / ResultCode / PageResult / BusinessException / GlobalExceptionHandler / ValidationSupport
│   ├── interceptor/            # LoginInterceptor
│   ├── aspect/                 # WebLogAspect、RequirePerm + PermAspect、OpLog + OpLogAspect
│   └── util/                   # PasswordUtil（SHA-256）
├── src/main/resources
│   ├── jdbc.properties         # 数据源（应用专用最小权限账号）
│   ├── logback.xml
│   └── mapper/EmpMapper.xml
├── src/main/webapp
│   ├── index.html              # CMS 管理后台（原生 JS 单页）
│   ├── employee.html           # 实验二员工模块页面
│   └── WEB-INF/web.xml         # Servlet 3.0+ 可省略，仅保留容器约定
└── src/test/java/com/lab/emp   # 服务层测试（12 个用例）
```

## 4. 快速开始

### 4.1 一键启动（推荐）

仓库自带 Tomcat 9（`.runtime/tomcat9`），脚本依次完成“打包 → 停止旧实例 → 部署 → 启动 → 健康检查”：

```powershell
.\scripts\run.ps1              # 打包 + 部署 + 启动（默认 8080）
.\scripts\run.ps1 -SkipBuild   # war 已是最新时跳过 Maven 打包
.\scripts\stop.ps1             # 停止服务
```

启动成功后：后台 <http://127.0.0.1:8080/emp-mis/>，健康检查 <http://127.0.0.1:8080/emp-mis/health>；
日志在 `.runtime/tomcat9/logs/emp-mis.log`（应用、SQL、WebLog）与 `catalina.<日期>.log`（容器）。

演示账号（初始口令均为 `123456`，库中只存 SHA-256 摘要）：

| 账号 | 角色 |
| --- | --- |
| `admin` | 系统管理员（16 条权限） |
| `editor` | 内容编辑 |
| `auditor` | 内容审核 |

### 4.2 手工步骤

```bash
# 1) 初始化数据库（会重建 CMS 业务表并写入种子数据；实验二脚本为 sql/init.sql）
mysql -uroot -p < sql/cms_init.sql

# 2) 数据源：src/main/resources/jdbc.properties
#    jdbc.username=emp_app  jdbc.password=Emp@2024（仅 DML 权限）

# 3) 打包
./mvnw -B -DskipTests clean package     # 产物 target/emp-mis.war

# 4) 部署到 Tomcat 9
copy target/emp-mis.war %CATALINA_HOME%\webapps\
%CATALINA_HOME%\bin\startup.bat

# 5) 访问 http://localhost:8080/emp-mis/
```

### 4.3 在 IntelliJ IDEA 里运行 / 调试

1. **让 IDEA 同步依赖**：打开 `pom.xml` → 右键 `Maven` → `Reload project`。若编译报 `程序包 com.fasterxml.jackson.datatype.jsr310 不存在`，就是依赖没同步；命令行 `.\mvnw.cmd -B -DskipTests compile` 能过说明代码与 pom 没问题。
2. **配置 Tomcat 9**（不能用 Tomcat 10/11，它们改用 `jakarta.servlet`，与 Spring 5.3 不兼容）：`Run → Edit Configurations → + → Tomcat Server → Local`，`Deployment` 添加 `emp-mis:war exploded`，`Application context` 填 `/emp-mis`，端口 8080。
3. 启动后访问 <http://localhost:8080/emp-mis/>。前端会自动识别部署上下文，因此即使 IDEA 使用 `/sy2ds_war_exploded` 也能正常调用接口。

> 只改 Java 代码时 `Build → Build Project` 即可；改了 pom 一定要重新 Reload Maven。

## 5. 接口

CMS 接口（路径、入参、返回示例、错误码）见 **[docs/cms-api.md](docs/cms-api.md)**；实验二员工接口见 [docs/api.md](docs/api.md)。

| 分组 | 代表接口 |
| --- | --- |
| 认证 | `POST /login`、`POST /logout`、`GET /me` |
| 控制台 | `GET /dashboard/stats` |
| 文章 | `GET/POST /articles`、`PUT /articles/{id}`、`PUT /articles/{id}/submit|audit|offline`、`PUT /articles/batch-publish`、`DELETE /articles/{id}` |
| 分类 | `GET /categories/tree`、`GET /categories/options`、`POST/PUT/DELETE /categories...` |
| 用户角色 | `GET/POST /users`、`PUT /users/{id}/roles`、`GET /roles`、`GET /permissions/tree`、`GET/PUT /roles/{id}/permissions` |
| 日志 | `GET /logs` |
| 实验二 | `GET /emps`、`POST /emps`、`POST /emps/tx-demo`、`GET /depts`、`GET /health` |

统一返回：

```json
{ "code": 200, "message": "操作成功", "data": {}, "success": true }
```

`code`：200 成功、400 参数/业务校验失败、401 未登录、403 无权限、404 不存在、500 系统异常。

## 6. 测试

```bash
./mvnw -B test          # Tests run: 12, Failures: 0, Errors: 0
```

- `EmpServiceTest`：分页与部门名回填、分页参数归一化、新增与修改往返、批量删除空列表、查询不存在 id
- `TransactionDemoTest`：正常回滚（0 条）与三类事务失效（各持久化 1 条）
- `PasswordUtilTest`：口令摘要与校验

接口层回归使用 `HttpClient + CookieContainer` 按角色建立独立会话，共 44 条用例（认证、权限、状态机、分类树、日志），全部通过：

```
docs/evidence/cms-http-regression.log   # 44 条用例逐条 PASS
docs/evidence/unit-test-output.log      # 单元测试输出
docs/evidence/http-regression.log       # 实验二 21 条接口回归
```

## 7. 事务失效演示（实验二保留）

```bash
curl -c c.txt -H "Content-Type: application/json" \
     -d '{"username":"admin","password":"123456"}' http://localhost:8080/emp-mis/login
curl -b c.txt -X POST "http://localhost:8080/emp-mis/emps/tx-demo?scenario=rollback&empName=测试"
```

| scenario | 现象 | 原因 |
| --- | --- | --- |
| `rollback` | `persisted=false` | 运行时异常触发 `rollbackFor` 回滚 |
| `swallow` | `persisted=true` | 异常被 try/catch 吞掉，事务管理器感知不到 |
| `self` | `persisted=true` | `this` 自调用绕过代理，注解不生效 |
| `private` | `persisted=true` | private 方法无法被 CGLIB 代理重写 |

## 8. 文档与交付物

| 文件 | 内容 |
| --- | --- |
| `docs/CMS 课程项目结题报告.docx` | 结题报告：概述、选型、需求、数据库、功能详解、重难点、测试、AI 说明、自评 |
| `docs/cms-design.md` | 设计说明：需求分析、架构、ER、接口约定、部署与测试策略 |
| `docs/cms-api.md` | CMS 接口清单与示例 |
| `docs/screenshots/cms-*.png` | 架构图、ER 图、流程图与界面截图 |
| `docs/《JAVA框架技术(一)》实验二实验报告.docx` | 实验二报告（六部分 + 附录） |
| `docs/《JAVA框架技术(一)》实验二实验报告（指导书模板）.docx` | 按指导书附录 B 结构排版的实验二报告（一 实验环境 / 二 实验目标 / 三 过程 / 四 AI 记录 / 五 问题 / 六 总结） |
| `docs/《JAVA框架技术(一)》实验二实验报告（教务处模板）.docx` | 按教务处表格模板排版的实验二报告 |
| `docs/api.md` / `docs/apifox-cases.md` / `docs/standards.md` | 实验二接口清单、用例与开发规范清单 |
| `sql/cms_init.sql` | CMS 建表与种子数据（可一条命令重建环境） |

## 9. 说明

本项目为课程作品：AI 工具参与代码骨架生成、报错分析与文档整理，全部代码经本地编译、单元测试与真实 HTTP 调用验证，报告中的每个数字均可在日志或截图中核对。数据源使用实验专用最小权限账号，不含任何真实个人或生产环境凭据。