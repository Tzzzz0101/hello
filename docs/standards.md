# 企业开发规范清单（本实验落地情况）

> 说明：以下 12 条为本项目实际执行并可在代码/脚本中逐条追溯的规范，不是通用条款抄录。每条附“落地位置”。

## 一、分层与职责

**1. 严格分层，禁止跨层调用**
Controller 只做参数接收、校验、结果封装；业务规则与事务在 Service；SQL 在 Mapper。Controller 不注入 Mapper，Service 不感知 `HttpServletRequest`。
落地：`controller/EmpController.java`、`service/impl/EmpServiceImpl.java`、`mapper/EmpMapper.java`。

**2. 包结构按“技术层 + 业务域”组织**
`com.lab.emp.{config,controller,service,service.impl,mapper,entity,dto,common,interceptor,aspect,util}`，DTO 与实体分离，实体不直接作为接口出参。
落地：`src/main/java/com/lab/emp/**`；出参统一 `dto/EmpVO.java`。

## 二、接口与异常

**3. 统一返回体**
所有接口返回 `Result<T>{code,message,data,success}`，禁止直接返回 `Map`、`String` 或裸实体；分页统一封装 `PageResult<T>`。
落地：`common/Result.java`、`common/PageResult.java`。

**4. 业务异常与系统异常分离**
可预期的业务错误抛 `BusinessException(code,message)`，由全局处理器回显；未知异常统一“系统繁忙，请稍后重试”，堆栈只写日志不外泄。
落地：`common/BusinessException.java`、`common/GlobalExceptionHandler.java`（`@RestControllerAdvice`）。

**5. 入参一律走 JSR-303 校验**
DTO 上声明约束，Controller 使用 `@Valid + BindingResult`，校验失败汇总字段消息后以 400 返回；不使用 `if (name == null)` 手工堆叠判断。
落地：`dto/EmpSaveDTO.java`、`controller/EmpController.java#checkBinding`。

## 三、数据与事务

**6. 事务边界在 Service，且显式声明回滚异常**
写操作统一 `@Transactional(rollbackFor = Exception.class)`；Controller 不开事务；跨表写操作放在同一 Service 方法内。
落地：`service/impl/EmpServiceImpl.java`。

**7. 预编译参数，禁止字符串拼接 SQL**
全部使用 MyBatis-Plus Wrapper 或 `#{}` 占位符，未出现 `${}` 直接拼接用户输入，避免 SQL 注入。
落地：`service/impl/EmpServiceImpl.java#pageQuery`、`resources/mapper/EmpMapper.xml`。

**8. 索引与约束随建表脚本一起版本化**
姓名、部门、状态+逻辑删除列建索引，部门外键约束、`dept_name` 唯一约束写入 `sql/init.sql`，脚本随代码入库，新环境一条命令可重建。
落地：`sql/init.sql`。

**9. 逻辑删除，不做物理删除**
`deleted` 字段 + `@TableLogic`，查询自动追加 `deleted = 0`，删除只置位，保留审计线索。
落地：`entity/Emp.java`、`config/MybatisPlusConfig.java`。

## 四、安全与配置

**10. 敏感信息不落库、不进日志、不进仓库**
口令以 SHA-256 摘要存储，`SysUser.password` 标注 `@JsonIgnore`，`LoginDTO.toString()` 不输出口令；数据库使用仅 DML 权限的专用账号，root 口令不出现在任何源码、文档、提交记录中。
落地：`util/PasswordUtil.java`、`entity/SysUser.java`、`dto/LoginDTO.java`、`resources/jdbc.properties`、`sql/init.sql`。

**11. 配置外置**
连接串、账号、连接池参数放 `jdbc.properties`，通过 `@PropertySource` 注入，代码中无硬编码地址与口令；日志路径按容器变量 `${catalina.base}` 解析。
落地：`resources/jdbc.properties`、`config/SpringConfig.java`、`resources/logback.xml`。

## 五、可观测与协作

**12. 日志分级 + 统一请求日志 + 规范提交**
Mapper 包 DEBUG 打印 SQL 与参数，业务关键节点 INFO，异常 ERROR；请求日志由 `WebLogAspect` 统一切面记录 URI、方法、参数（脱敏、截断 300 字符）、耗时与异常；Git 按 `feat/fix/docs/chore` 拆分提交并打版本 tag，`.gitignore` 排除 `target/`、`.idea/`、`.runtime/`、`logs/`。
落地：`resources/logback.xml`、`aspect/WebLogAspect.java`、`.gitignore`、Git 提交历史（tag `v1.0.0`）。