# CMS 内容管理系统 接口清单

- 基础路径：`http://localhost:8080/emp-mis`（IDEA 部署时可能是 `/sy2ds_war_exploded`，前端会自动识别）
- 数据格式：`application/json; charset=UTF-8`
- 认证方式：登录后由容器维护 `JSESSIONID`；除 `/login`、`/health` 外均需登录，未登录返回 401
- 鉴权方式：接口上的 `@RequirePerm("xxx")` 由 `PermAspect` 校验 Session 中的权限集合，越权返回 403
- 统一返回：`{ "code": 200, "message": "操作成功", "data": ..., "success": true }`

| code | 含义 |
| --- | --- |
| 200 | 成功 |
| 400 | 参数校验失败 / 业务规则不通过 |
| 401 | 未登录或登录状态失效 |
| 403 | 已登录但无权限 |
| 404 | 资源不存在 |
| 500 | 系统异常（返回“系统繁忙，请稍后重试”） |

演示账号（初始口令均为 `123456`，库中存 SHA-256）：

| 账号 | 角色 | 权限数 |
| --- | --- | --- |
| admin | ADMIN 系统管理员 | 16 |
| editor | EDITOR 内容编辑 | 8 |
| auditor | AUDITOR 内容审核 | 5 |

## 1. 认证

### 1.1 登录 `POST /login`

```json
{"username": "admin", "password": "123456"}
```

```json
{"code":200,"message":"登录成功","data":{
  "userId":1,"username":"admin","realName":"系统管理员",
  "roleCodes":["ADMIN"],"roleNames":["系统管理员"],
  "permissions":["dashboard:view","article:list","article:view","article:add","article:edit",
                 "article:delete","article:publish","article:audit","category:list","category:add",
                 "category:edit","category:delete","user:list","user:save","role:assign","log:list"]}}
```

失败一律 401（不区分账号是否存在）：`{"code":401,"message":"用户名或口令错误"}`

### 1.2 退出 `POST /logout` → `{"code":200,"message":"已退出登录"}`

### 1.3 当前用户 `GET /me` → 返回登录时写入 Session 的 `LoginUser`，未登录 401

> 权限快照保存在 Session 中，管理员修改某人的角色后，该用户需要重新登录才会生效。

## 2. 控制台

`GET /dashboard/stats` — 权限 `dashboard:view`

```json
{"totalArticles":4,"published":2,"pending":1,"draft":1,"offline":0,"totalViews":224,
 "categories":[{"categoryId":1,"categoryName":"公司动态","articleCount":2},
               {"categoryId":2,"categoryName":"行业资讯","articleCount":1}]}
```

## 3. 文章

| 方法 | 路径 | 说明 | 权限 |
| --- | --- | --- | --- |
| GET | `/articles` | 分页查询 | article:list |
| GET | `/articles/{id}` | 详情（阅读量 +1） | article:view |
| POST | `/articles` | 新增（状态 DRAFT） | article:add |
| PUT | `/articles/{id}` | 编辑（仅 DRAFT/REJECTED） | article:edit |
| PUT | `/articles/{id}/submit` | 提交审核（DRAFT/REJECTED → PENDING） | article:edit |
| PUT | `/articles/{id}/audit` | 审核（仅 PENDING） | article:audit |
| PUT | `/articles/{id}/offline` | 下架（仅 PUBLISHED） | article:publish |
| PUT | `/articles/batch-publish` | 批量发布 | article:publish |
| DELETE | `/articles/{id}` | 逻辑删除 | article:delete |

### 3.1 列表 `GET /articles?title=&categoryId=&status=&page=1&size=10&orderBy=publishTime&orderDir=desc`

| 参数 | 说明 |
| --- | --- |
| title | 标题模糊匹配 |
| categoryId | 分类编号 |
| status | `DRAFT` / `PENDING` / `PUBLISHED` / `REJECTED` / `OFFLINE`，非法值返回 400 |
| page / size | 页码默认 1，每页默认 10；`< 1` 归一化，`> 100` 截断 |
| orderBy | 白名单：`publishTime`、`viewCount`、`updateTime`、`createTime`，其余回落到默认排序 |
| orderDir | `asc` / `desc` |

```json
{"page":1,"size":10,"total":4,"pages":1,"records":[
  {"articleId":1,"title":"公司荣获市级技术创新奖","categoryId":1,"categoryName":"公司动态",
   "authorId":1,"authorName":"系统管理员","status":"PUBLISHED","statusText":"已发布",
   "viewCount":128,"publishTime":"2026-11-10 09:00:00"}]}
```

### 3.2 新增 / 编辑

```json
{"title":"测试文章","summary":"摘要","content":"<p>正文</p>","categoryId":1,"coverUrl":""}
```

校验：`title` 非空且 ≤100 字；`categoryId` 必填且分类需存在；`content` 非空。

### 3.3 审核 `PUT /articles/{id}/audit`

```json
{"status":"PUBLISHED","remark":"内容合规"}
```

- 通过：`{"status":"PUBLISHED"}`
- 驳回：`{"status":"REJECTED","remark":"缺乏数据来源"}`；驳回不填意见返回 400
- 非 PENDING 状态审核返回 400：`{"code":400,"message":"当前状态不可审核：DRAFT"}`

### 3.4 批量发布 `PUT /articles/batch-publish`

```json
[1,2,3]
```

只处理 DRAFT/PENDING，返回实际发布条数：`{"published":2}`；空数组返回 400「请选择要发布的文章」。

## 4. 分类

| 方法 | 路径 | 说明 | 权限 |
| --- | --- | --- | --- |
| GET | `/categories/tree` | 分类树 | category:list |
| GET | `/categories/options` | 下拉选项（扁平） | 登录即可 |
| POST | `/categories` | 新增 | category:add |
| PUT | `/categories/{id}` | 修改 | category:edit |
| DELETE | `/categories/{id}` | 删除（有子分类/文章时拒绝） | category:delete |

新增请求（`parentId` 为 `null` 表示一级分类，传 `0` 会被服务端归一化为 `null`）：

```json
{"parentId":null,"categoryName":"新闻中心","categoryCode":"news","sort":1,"status":1}
```

树形响应：

```json
[{"categoryId":1,"categoryName":"新闻中心","parentId":null,"children":[
   {"categoryId":11,"categoryName":"公司动态","parentId":1,"children":[]}]}]
```

业务校验：编码唯一；父分类必须存在；不能把自身或后代设为父分类（成环返回 400）；有子分类或已绑定文章时删除返回 400。

## 5. 用户与角色

| 方法 | 路径 | 说明 | 权限 |
| --- | --- | --- | --- |
| GET | `/users?keyword=&page=1&size=10` | 用户分页（含角色） | user:list |
| POST | `/users` | 新增用户 | user:save |
| PUT | `/users/{id}` | 修改用户 | user:save |
| PUT | `/users/{id}/roles` | 分配角色 | role:assign |
| GET | `/roles` | 角色列表 | role:assign |
| GET | `/permissions/tree` | 权限树 | role:assign |
| GET | `/roles/{id}/permissions` | 角色已授权限 | role:assign |
| PUT | `/roles/{id}/permissions` | 更新角色权限 | role:assign |

新增用户：

```json
{"username":"editor2","password":"123456","realName":"编辑二号","email":"e2@lab.com","roleIds":[2]}
```

口令以 SHA-256 存储，响应中不返回口令字段；用户名重复返回 400。

分配角色：

```json
{"roleIds":[2,3]}
```

## 6. 操作日志

`GET /logs?module=&operation=&page=1&size=10` — 权限 `log:list`

```json
{"page":1,"size":10,"total":71,"records":[
  {"logId":71,"username":"admin","module":"文章管理","operation":"审核",
   "method":"ArticleController.audit","params":"{\"status\":\"PUBLISHED\",\"remark\":\"内容合规\"}",
   "ip":"127.0.0.1","costMs":23,"status":1,"errorMsg":null,"createTime":"2026-11-18 23:07:51"}]}
```

参数中的 `password` 会被替换为 `***`，最长保留 900 字符。

## 7. 公共接口

- `GET /health` — 免登录健康检查：`{"code":200,"data":{"status":"UP","deptCount":4}}`
- `GET /depts` — 部门列表（实验二员工模块使用）
- 实验二员工接口 `/emps/**` 保留，详见 [api.md](api.md)