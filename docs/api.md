# EMP 员工管理 MIS 系统 接口清单

- 基础路径：`http://localhost:8080/emp-mis`
- 数据格式：`application/json; charset=UTF-8`
- 认证方式：登录后由容器维护 `JSESSIONID` Cookie；除 `/login`、`/health` 外均需登录，未登录返回 401
- 统一返回结构：`{ "code": 200, "message": "操作成功", "data": ..., "success": true }`

| code | 含义 |
| --- | --- |
| 200 | 成功 |
| 400 | 参数校验失败 / 业务参数不合法 |
| 401 | 未登录或登录状态失效 |
| 404 | 资源不存在 |
| 405 | 请求方法不支持 |
| 500 | 系统异常（返回“系统繁忙，请稍后重试”） |

## 1. 认证

### 1.1 登录

`POST /login`

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| username | string | 是 | 用户名 |
| password | string | 是 | 口令（6–32 位），服务端比对 SHA-256 摘要 |

```json
{"username": "admin", "password": "123456"}
```

成功：

```json
{"code":200,"message":"登录成功","data":{"userId":1,"username":"admin","realName":"系统管理员","role":"ADMIN","status":1},"success":true}
```

失败（口令错误 / 账号禁用均为 401，避免暴露账号是否存在）：

```json
{"code":401,"message":"用户名或口令错误","data":null,"success":false}
```

### 1.2 退出登录

`POST /logout` → `{"code":200,"message":"已退出登录","data":null}`

### 1.3 当前登录用户

`GET /me` → 返回登录时写入 session 的用户信息；未登录返回 401。

## 2. 员工管理

### 2.1 分页 + 多条件查询

`GET /emps?name=&gender=&deptId=&status=&page=1&size=5`

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| name | string | 否 | 姓名模糊匹配（`LIKE %name%`） |
| gender | string | 否 | `男` / `女` |
| deptId | int | 否 | 部门编号 |
| status | int | 否 | 1 在职 / 0 离职 |
| page | int | 否 | 页码，默认 1；`< 1` 归一化为 1 |
| size | int | 否 | 每页条数，默认 5；`< 1` 归一化为 5，`> 100` 归一化为 100 |

响应 `data` 为分页对象：

```json
{"page":1,"size":5,"total":4,"pages":1,"records":[
  {"empId":4,"empName":"赵敏","gender":"女","deptId":3,"deptName":"人事部","post":"人事专员","salary":7500.00,"hireDate":"2022-06-20","status":0}
]}
```

### 2.2 按主键查询

`GET /emps/{id}` → 200 返回 `EmpVO`；不存在时 404：

```json
{"code":404,"message":"员工不存在，编号=999999","data":null,"success":false}
```

### 2.3 新增

`POST /emps`

| 字段 | 类型 | 校验 |
| --- | --- | --- |
| empName | string | `@NotBlank`，`@Size(max=50)` |
| gender | string | `@NotBlank`，`@Pattern(^[男女]$)` |
| deptId | int | `@NotNull`，且部门必须存在 |
| post | string | `@Size(max=50)` |
| salary | decimal | `@NotNull`，`@DecimalMin("0")`，`@Digits(integer=8, fraction=2)` |
| hireDate | date | `@PastOrPresent`，格式 `yyyy-MM-dd` |
| status | int | `@Min(0)` `@Max(1)`，默认 1 |

```json
{"empName":"孙悦","gender":"女","deptId":1,"post":"测试工程师","salary":9000,"hireDate":"2026-11-17","status":1}
```

校验失败（400）：

```json
{"code":400,"message":"员工姓名不能为空","data":null,"success":false}
{"code":400,"message":"性别只能填写 男 或 女；薪资不能为负数","data":null,"success":false}
{"code":400,"message":"所选部门不存在，编号=999","data":null,"success":false}
```

### 2.4 修改

`PUT /emps`，请求体在新增字段基础上必须包含 `empId`。

### 2.5 删除

- `DELETE /emps/{id}`：单条删除（逻辑删除，`deleted` 置 1）
- `DELETE /emps`：批量删除，请求体为 id 数组

```json
[1,2,3]
```

空数组返回 400：

```json
{"code":400,"message":"请选择要删除的员工","data":null,"success":false}
```

### 2.6 部门统计

`GET /emps/stat/dept`

```json
[{"deptId":1,"deptName":"研发部","empCount":2,"avgSalary":11000.00,"maxSalary":12000.00}]
```

### 2.7 事务演示

`POST /emps/tx-demo?scenario=rollback|swallow|self|private&empName=测试`

```json
{"scenario":"rollback","empName":"UT-HTTP-回滚","exception":"ArithmeticException: / by zero",
 "persistedCount":0,"persisted":false,"rolledBack":true}
```

## 3. 基础数据

### 3.1 部门列表

`GET /depts`

```json
[{"deptId":1,"deptName":"研发部","location":"A座3层"}]
```

### 3.2 健康检查（免登录）

`GET /health`

```json
{"code":200,"message":"操作成功","data":{"status":"UP","deptCount":4,"timestamp":1794891936375},"success":true}
```