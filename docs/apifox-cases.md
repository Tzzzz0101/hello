# ApiFox 测试用例与执行记录

## 1. 环境配置（可直接在 ApiFox 中新建环境）

| 变量 | 值 |
| --- | --- |
| baseUrl | http://localhost:8080/emp-mis |
| username | admin |
| password | 123456 |

前置脚本（登录并保存 Cookie，ApiFox 会自动维护 Cookie 会话）：

```javascript
pm.sendRequest({
  url: pm.environment.get("baseUrl") + "/login",
  method: "POST",
  header: { "Content-Type": "application/json" },
  body: { mode: "raw", raw: JSON.stringify({ username: "admin", password: "123456" }) }
}, function (err, res) { console.log(res.json()); });
```

公共断言：

```javascript
pm.test("HTTP 200", () => pm.response.to.have.status(200));
pm.test("code=200", () => pm.expect(pm.response.json().code).to.eql(200));
```

## 2. 用例清单与执行结果

执行时间：2026-11-17 13:06；被测实例：Tomcat 9.0.109 + emp-mis.war（真实 MySQL 8.0 数据）。

| 编号 | 用例 | 请求 | 入参 / 前置 | 预期 | 实际 | 结论 |
| --- | --- | --- | --- | --- | --- | --- |
| TC-01 | 健康检查 | GET /health | 未登录 | 200，status=UP | 200，deptCount=4 | 通过 |
| TC-02 | 未登录访问列表 | GET /emps?page=1&size=5 | 清除 Cookie | 401 未登录 | 401 `未登录，请先登录` | 通过 |
| TC-03 | 口令错误登录 | POST /login | admin / wrong-pass | 401 | 401 `用户名或口令错误` | 通过 |
| TC-04 | 正常登录 | POST /login | admin / 123456 | 200，返回用户信息 | 200，realName=系统管理员 | 通过 |
| TC-05 | 分页查询首页 | GET /emps?page=1&size=5 | 已登录 | 200，total=4 | 200，4 条记录，按 empId 倒序 | 通过 |
| TC-06 | 分页边界（page=0、size=-3） | GET /emps?page=0&size=-3 | 已登录 | 归一化为 page=1、size=5 | 200，page=1，size=5 | 通过 |
| TC-07 | 姓名模糊 + 性别过滤 | GET /emps?name=张&gender=男 | 已登录 | 仅返回张伟 | 200，total=1（张伟） | 通过 |
| TC-08 | 按主键查询 | GET /emps/1 | 已登录 | 200，含部门名 | 200，研发部 | 通过 |
| TC-09 | 查询不存在的 id | GET /emps/999999 | 已登录 | 404 | 404 `员工不存在，编号=999999` | 通过 |
| TC-10 | 部门下拉数据 | GET /depts | 已登录 | 4 个部门 | 200，4 条 | 通过 |
| TC-11 | 部门薪资统计 | GET /emps/stat/dept | 已登录 | 4 行统计 | 200，研发部 2 人 / 均值 11000 | 通过 |
| TC-12 | 姓名为空新增 | POST /emps | empName="" | 400 姓名不能为空 | 400 `员工姓名不能为空` | 通过 |
| TC-13 | 性别非法 + 薪资为负 | POST /emps | gender="中性"，salary=-5 | 400 | 400 `性别只能填写 男 或 女；薪资不能为负数` | 通过 |
| TC-14 | 部门不存在 | POST /emps | deptId=999 | 400 部门不存在 | 400 `所选部门不存在，编号=999` | 通过 |
| TC-15 | 批量删除空数组 | DELETE /emps | `[]` | 400 | 400 `请选择要删除的员工` | 通过 |
| TC-16 | 事务正常回滚 | POST /emps/tx-demo?scenario=rollback | 已登录 | persisted=false | 200，persistedCount=0，rolledBack=true | 通过 |
| TC-17 | 异常被吞 | POST /emps/tx-demo?scenario=swallow | 已登录 | persisted=true（未回滚） | 200，persistedCount=1 | 通过 |
| TC-18 | this 自调用 | POST /emps/tx-demo?scenario=self | 已登录 | persisted=true（注解失效） | 200，persistedCount=1 | 通过 |
| TC-19 | private 方法 | POST /emps/tx-demo?scenario=private | 已登录 | persisted=true（注解失效） | 200，persistedCount=1 | 通过 |
| TC-20 | 退出登录 | POST /logout | 已登录 | 200 | 200 `已退出登录` | 通过 |
| TC-21 | 退出后再次访问 | GET /emps?page=1&size=5 | session 已失效 | 401 | 401 `未登录，请先登录` | 通过 |

**合计 21 条用例，全部通过（21/21）。**

## 3. 执行方式说明

- 用例按“方法 + 路径 + JSON 请求体 + 预期断言”建模，字段与上表一致，可直接在 ApiFox 中录入或通过 OpenAPI 导入（`docs/api.md` 即接口契约）。
- 批量回归使用 PowerShell + `System.Net.Http.HttpClient`，携带同一 `CookieContainer` 模拟 ApiFox 的会话保持；每次请求打印状态码与响应体，原始记录保存在 `docs/evidence/http-regression.log`。
- 前端页面（`/emp-mis/`）另做了 5 组 UI 级回归：登录页、列表页、条件查询、新增弹窗、后端校验失败提示，见 `docs/screenshots/01~05`。

## 4. 边界与异常设计要点

1. **分页参数不做 400 拦截**：`page < 1`、`size < 1` 归一化为默认值，`size > 100` 截断为 100，避免爬取式大页查询拖垮数据库。
2. **登录失败信息统一**：账号不存在、口令错误、账号禁用对外均返回 401 与同一提示，防止账号枚举。
3. **删除采用逻辑删除**：`deleted` 字段 + MyBatis-Plus `@TableLogic`，所有查询自动追加 `deleted = 0`。
4. **业务异常与系统异常分离**：业务异常回显可读原因，未知异常统一“系统繁忙，请稍后重试”，堆栈只进日志。