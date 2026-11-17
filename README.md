# EMP 员工管理 MIS 系统（实验二：AI 赋能 SSM 框架整合）

基于 **Spring 5.3 + SpringMVC 5.3 + MyBatis-Plus 3.5** 的全注解整合示例项目，实现员工信息的增删改查、多条件模糊分页查询、参数校验、统一返回、全局异常处理、登录拦截、请求日志与声明式事务演示。

## 1. 技术栈

| 组件 | 版本 | 说明 |
| --- | --- | --- |
| JDK | 25（`--release 8` 编译） | 字节码目标版本 1.8，兼容实验要求的 JDK 8/11 运行环境 |
| Maven | 3.9.6（`mvnw` 包装器） | 无需本地安装 Maven |
| Spring / SpringMVC | 5.3.39 | `javax.servlet` 体系，配合 Tomcat 9 |
| MyBatis-Plus | 3.5.3.1 | `BaseMapper` + `LambdaQueryWrapper` + 分页插件 |
| Druid | 1.2.20 | 连接池与监控（`stat-view-servlet` 未开启） |
| MySQL | 8.0.45 | 库名 `ssm_emp` |
| Tomcat | 9.0.109 | Servlet 4.0，`contextPath = /emp-mis` |
| Hibernate Validator | 6.2.5.Final | JSR-303 参数校验 |
| Jackson | 2.15.3 | JSON 序列化，`JavaTimeModule` 支持 `LocalDate` |
| Logback | 1.2.13 | 控制台 + 文件日志，Mapper 包 DEBUG 输出 SQL |

## 2. 目录结构

```
sy2ds
├── pom.xml                     # war 打包，finalName=emp-mis
├── sql/init.sql                # 建库、建表、索引、约束、种子数据、最小权限账号
├── docs/                       # 接口清单、ApiFox 用例、规范清单、截图
├── src/main/java/com/lab/emp
│   ├── config/                 # SpringConfig / SpringMvcConfig / ServletContainersInitConfig / MybatisPlusConfig
│   ├── controller/             # EmpController / AuthController / DeptController / HealthController
│   ├── service/ + service/impl # 业务接口与实现（事务边界）
│   ├── mapper/                 # MyBatis-Plus Mapper 接口
│   ├── entity/                 # 数据库实体（Emp / Dept / SysUser）
│   ├── dto/                    # 入参 DTO 与出参 VO
│   ├── common/                 # Result / ResultCode / BusinessException / GlobalExceptionHandler
│   ├── interceptor/            # LoginInterceptor
│   ├── aspect/                 # WebLogAspect
│   └── util/                   # PasswordUtil（SHA-256）
├── src/main/resources
│   ├── jdbc.properties         # 数据源配置（应用专用账号）
│   ├── logback.xml
│   └── mapper/EmpMapper.xml
├── src/main/webapp
│   ├── index.html              # 单页前端（登录 + 列表 + 增删改 + 分页）
│   └── WEB-INF/web.xml         # Servlet 3.0+ 可省略，仅保留容器约定
└── src/test/java/com/lab/emp   # 单元/集成测试（12 个用例）
```

## 3. 快速开始

```bash
# 1) 初始化数据库（会重建 ssm_emp 库，谨慎执行）
mysql -uroot -p < sql/init.sql

# 2) 配置数据源：src/main/resources/jdbc.properties
#    jdbc.username=emp_app  jdbc.password=Emp@2024（仅 DML 权限）

# 3) 打包
./mvnw -B -DskipTests clean package     # 产物 target/emp-mis.war

# 4) 部署到 Tomcat 9
copy target/emp-mis.war %CATALINA_HOME%\webapps\
%CATALINA_HOME%\bin\startup.bat

# 5) 访问
#    http://localhost:8080/emp-mis/        页面
#    http://localhost:8080/emp-mis/health  健康检查
```

实验账号：`admin / 123456`（口令以 SHA-256 摘要存入库中，明文不落库、不进日志）。

## 4. 接口一览

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/login` | 登录，成功后写入 session |
| POST | `/logout` | 退出登录 |
| GET | `/me` | 获取当前登录用户 |
| GET | `/emps` | 多条件分页查询（`name`、`gender`、`deptId`、`status`、`page`、`size`） |
| GET | `/emps/{id}` | 按主键查询（含部门名称） |
| POST | `/emps` | 新增员工（JSR-303 校验） |
| PUT | `/emps` | 修改员工 |
| DELETE | `/emps/{id}` | 删除员工（逻辑删除） |
| DELETE | `/emps` | 批量删除（请求体为 id 数组） |
| GET | `/emps/stat/dept` | 按部门统计人数与薪资 |
| POST | `/emps/tx-demo` | 事务失效场景演示（`scenario=rollback/swallow/self/private`） |
| GET | `/depts` | 部门列表 |
| GET | `/health` | 健康检查（免登录） |

详细字段与示例见 [docs/api.md](docs/api.md)，测试用例见 [docs/apifox-cases.md](docs/apifox-cases.md)。

## 5. 测试

```bash
./mvnw -B test
# Tests run: 12, Failures: 0, Errors: 0
```

- `EmpServiceTest`：分页与部门名回填、分页参数归一化、新增与修改往返、批量删除空列表、查询不存在 id
- `TransactionDemoTest`：正常回滚（0 条）与三类事务失效（各持久化 1 条）
- `PasswordUtilTest`：口令摘要与校验

## 6. 事务演示接口

```bash
curl -c c.txt -H "Content-Type: application/json" \
     -d '{"username":"admin","password":"123456"}' http://localhost:8080/emp-mis/login
curl -b c.txt -X POST "http://localhost:8080/emp-mis/emps/tx-demo?scenario=rollback&empName=测试"
```

返回体中 `persisted` 表示数据是否最终落库，`rolledBack` 表示事务是否回滚：

| scenario | 现象 | 原因 |
| --- | --- | --- |
| `rollback` | `persisted=false` | 运行时异常触发 `rollbackFor` 回滚 |
| `swallow` | `persisted=true` | 异常被 try/catch 吞掉，事务管理器感知不到 |
| `self` | `persisted=true` | `this` 自调用绕过代理，注解不生效 |
| `private` | `persisted=true` | private 方法无法被 CGLIB 代理重写 |

## 7. 说明

本项目为课程实验作品：AI 工具参与代码骨架生成与文档整理，全部代码经本地编译、单元测试与真实 HTTP 调用验证；数据源口令使用实验专用最小权限账号，不含任何真实个人或生产环境凭据。