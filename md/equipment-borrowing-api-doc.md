# 设备借用后台管理系统 API 文档

## 1. 文档说明

- 基于 [`md/equipment-borrowing-admin-prd.md`](D:/program/booking-skill-demo/md/equipment-borrowing-admin-prd.md) 生成。
- 目标：为前后端联调提供结构化接口定义。
- 风格：RESTful JSON API。
- Base URL：`/api`
- 数据格式：`application/json`
- 时间格式：默认使用 `YYYY-MM-DD HH:mm:ss`
- 认证方式：`Authorization: Bearer <token>`

## 2. 角色与权限

| 角色 | 标识 | 主要权限 |
| --- | --- | --- |
| 超级管理员 | `SUPER_ADMIN` | 管理管理员、角色权限、查看全局统计，默认拥有管理员全部权限 |
| 管理员 | `ADMIN` | 创建教师、管理设备、审核预约、处理维修、查看统计 |
| 教师 | `TEACHER` | 创建/删除学生、审核预约、查看相关借用信息 |
| 学生 | `STUDENT` | 查看设备、提交预约、确认取用、归还设备、查看个人中心、提交维修 |

## 3. 通用响应结构

```json
{
  "code": 0,
  "message": "success",
  "data": {},
  "requestId": "202603281230001234"
}
```

### 3.1 分页响应

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [],
    "pageNum": 1,
    "pageSize": 10,
    "total": 100
  }
}
```

### 3.2 通用错误码

| 错误码 | 说明 |
| --- | --- |
| `0` | 成功 |
| `400` | 请求参数错误 |
| `401` | 未登录或 token 无效 |
| `403` | 无权限访问 |
| `404` | 资源不存在 |
| `409` | 状态冲突或重复预约 |
| `500` | 服务内部异常 |

## 4. 核心状态定义

### 4.1 用户状态

| 值 | 说明 |
| --- | --- |
| `ENABLED` | 启用 |
| `DISABLED` | 禁用 |

### 4.2 设备状态

| 值 | 说明 |
| --- | --- |
| `AVAILABLE` | 可用 |
| `RESERVED` | 已预约 |
| `BORROWED` | 借用中 |
| `REPAIRING` | 维修中 |
| `DAMAGED` | 已损坏 |
| `DISABLED` | 已停用 |

### 4.3 预约状态

| 值 | 说明 |
| --- | --- |
| `PENDING` | 待审核 |
| `APPROVED` | 审核通过 |
| `REJECTED` | 审核驳回 |
| `PICKUP_PENDING` | 待取用 |
| `CANCELLED` | 已取消 |
| `EXPIRED` | 已失效 |

### 4.4 借用状态

| 值 | 说明 |
| --- | --- |
| `PICKUP_PENDING` | 待取用 |
| `BORROWING` | 借用中 |
| `RETURNED` | 已归还 |
| `OVERDUE` | 已逾期 |

### 4.5 维修状态

| 值 | 说明 |
| --- | --- |
| `PENDING` | 待处理 |
| `PROCESSING` | 处理中 |
| `COMPLETED` | 已完成 |
| `UNREPAIRABLE` | 无法修复 |

## 5. 认证与登录

### 5.1 用户登录

- 名称：登录
- 方法：`POST`
- 路径：`/api/auth/login`
- 权限：公开

请求体：

```json
{
  "loginId": "20230001",
  "password": "123456"
}
```

返回示例：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "token": "jwt-token",
    "tokenType": "Bearer",
    "expiresIn": 7200,
    "userInfo": {
      "userId": 1,
      "name": "张三",
      "account": "20230001",
      "roleCode": "STUDENT",
      "status": "ENABLED"
    }
  }
}
```

规则：

- 学生使用学号登录。
- 教师使用工号登录。
- 管理员、超级管理员使用系统分配账号登录。
- 默认密码为 `123456`。
- 是否首次登录强制修改密码：`TODO`

### 5.2 获取当前登录用户信息

- 方法：`GET`
- 路径：`/api/auth/me`
- 权限：已登录用户

返回字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `userId` | number | 用户 ID |
| `name` | string | 姓名 |
| `account` | string | 账号 |
| `jobNoOrStudentNo` | string | 工号或学号 |
| `roleCode` | string | 角色编码 |
| `creditScore` | number | 信用分 |
| `status` | string | 用户状态 |

### 5.3 修改密码

- 方法：`PUT`
- 路径：`/api/auth/password`
- 权限：已登录用户

请求体：

```json
{
  "oldPassword": "123456",
  "newPassword": "abc123456"
}
```

规则：

- 需校验旧密码。
- 新密码规则当前未在 PRD 中明确，后续可扩展。

## 6. 用户管理

### 6.1 用户列表

- 方法：`GET`
- 路径：`/api/users`
- 权限：`SUPER_ADMIN`、`ADMIN`、`TEACHER`

查询参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `keyword` | string | 否 | 姓名/账号/学号/工号模糊查询 |
| `roleCode` | string | 否 | 角色编码 |
| `status` | string | 否 | 用户状态 |
| `pageNum` | number | 否 | 页码，默认 1 |
| `pageSize` | number | 否 | 每页数量，默认 10 |

规则：

- 超级管理员可查看管理员及以下账号。
- 管理员可查看教师及以下账号。
- 教师仅可查看其管理范围内学生账号。

### 6.2 用户详情

- 方法：`GET`
- 路径：`/api/users/{userId}`
- 权限：`SUPER_ADMIN`、`ADMIN`、`TEACHER`

### 6.3 创建管理员

- 方法：`POST`
- 路径：`/api/users/admins`
- 权限：`SUPER_ADMIN`

请求体：

```json
{
  "name": "管理员A",
  "account": "admin_a",
  "phone": "13800000000"
}
```

规则：

- 默认密码为 `123456`。
- 不允许越权创建。

### 6.4 创建教师

- 方法：`POST`
- 路径：`/api/users/teachers`
- 权限：`ADMIN`

请求体：

```json
{
  "name": "李老师",
  "jobNo": "T2026001",
  "phone": "13800000001"
}
```

### 6.5 创建学生

- 方法：`POST`
- 路径：`/api/users/students`
- 权限：`TEACHER`

请求体：

```json
{
  "name": "王同学",
  "studentNo": "20230001",
  "phone": "13800000002"
}
```

### 6.6 删除学生

- 方法：`DELETE`
- 路径：`/api/users/students/{userId}`
- 权限：`TEACHER`

规则：

- 删除前需校验该学生是否存在未归还设备。
- 历史业务数据不应物理删除：`TODO` 明确为逻辑删除还是禁用。

### 6.7 更新用户状态

- 方法：`PUT`
- 路径：`/api/users/{userId}/status`
- 权限：`SUPER_ADMIN`、`ADMIN`

请求体：

```json
{
  "status": "DISABLED"
}
```

规则：

- 账号启用、禁用在 PRD 中属于保留能力，实际是否本期实现：`TODO`

### 6.8 重置密码

- 方法：`PUT`
- 路径：`/api/users/{userId}/reset-password`
- 权限：`SUPER_ADMIN`、`ADMIN`

请求体：

```json
{
  "newPassword": "123456"
}
```

规则：

- 是否支持重置密码在 PRD 中为待确认项：`TODO`

## 7. 角色与权限管理

### 7.1 角色列表

- 方法：`GET`
- 路径：`/api/roles`
- 权限：`SUPER_ADMIN`

### 7.2 新增角色

- 方法：`POST`
- 路径：`/api/roles`
- 权限：`SUPER_ADMIN`

请求体：

```json
{
  "roleName": "实验员",
  "roleCode": "LAB_ASSISTANT",
  "remark": "实验室辅助角色"
}
```

### 7.3 更新角色

- 方法：`PUT`
- 路径：`/api/roles/{roleId}`
- 权限：`SUPER_ADMIN`

### 7.4 角色详情

- 方法：`GET`
- 路径：`/api/roles/{roleId}`
- 权限：`SUPER_ADMIN`

### 7.5 为角色分配权限

- 方法：`PUT`
- 路径：`/api/roles/{roleId}/permissions`
- 权限：`SUPER_ADMIN`

请求体：

```json
{
  "permissionIds": [101, 102, 103],
  "menuIds": [201, 202]
}
```

规则：

- 菜单权限与功能权限是否完全一致：`TODO`

### 7.6 权限列表

- 方法：`GET`
- 路径：`/api/permissions`
- 权限：`SUPER_ADMIN`

查询参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `type` | string | 否 | `MENU` / `ACTION` |

## 8. 个人中心

### 8.1 个人信息

- 方法：`GET`
- 路径：`/api/profile`
- 权限：已登录用户

返回字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `userId` | number | 用户 ID |
| `name` | string | 姓名 |
| `account` | string | 账号 |
| `jobNoOrStudentNo` | string | 工号或学号 |
| `creditScore` | number | 信用分 |
| `roleCode` | string | 角色 |
| `status` | string | 用户状态 |

规则：

- 信用分初始值、扣减规则、恢复规则：`TODO`

### 8.2 我的借阅记录

- 方法：`GET`
- 路径：`/api/profile/borrow-records`
- 权限：已登录用户

查询参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `status` | string | 否 | `RETURNED` / `BORROWING` / `OVERDUE` |
| `pageNum` | number | 否 | 页码 |
| `pageSize` | number | 否 | 每页数量 |

## 9. 设备管理

### 9.1 设备列表

- 方法：`GET`
- 路径：`/api/devices`
- 权限：已登录用户

查询参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `keyword` | string | 否 | 设备名称/编号 |
| `category` | string | 否 | 设备分类 |
| `status` | string | 否 | 设备状态 |
| `pageNum` | number | 否 | 页码 |
| `pageSize` | number | 否 | 每页数量 |

返回字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `deviceId` | number | 设备 ID |
| `deviceName` | string | 设备名称 |
| `deviceCode` | string | 设备编号 |
| `category` | string | 设备分类 |
| `status` | string | 当前状态 |
| `location` | string | 存放位置 |

### 9.2 设备详情

- 方法：`GET`
- 路径：`/api/devices/{deviceId}`
- 权限：已登录用户

### 9.3 新增设备

- 方法：`POST`
- 路径：`/api/devices`
- 权限：`ADMIN`

请求体：

```json
{
  "deviceName": "投影仪A",
  "deviceCode": "EQ-2026-0001",
  "category": "投影设备",
  "location": "实验楼101",
  "description": "便携式投影仪"
}
```

规则：

- 设备编号需唯一。

### 9.4 更新设备

- 方法：`PUT`
- 路径：`/api/devices/{deviceId}`
- 权限：`ADMIN`

### 9.5 更新设备状态

- 方法：`PUT`
- 路径：`/api/devices/{deviceId}/status`
- 权限：`ADMIN`

请求体：

```json
{
  "status": "DISABLED"
}
```

### 9.6 批量导入设备

- 方法：`POST`
- 路径：`/api/devices/import`
- 权限：`ADMIN`

说明：

- 设备上传形式在 PRD 中未明确。
- 当前仅预留接口，导入方式为文件导入、模板导入还是附件上传：`TODO`

## 10. 预约与审核

### 10.1 提交预约申请

- 方法：`POST`
- 路径：`/api/reservations`
- 权限：`STUDENT`

请求体：

```json
{
  "deviceId": 1001,
  "startTime": "2026-03-29 09:00:00",
  "endTime": "2026-03-29 18:00:00",
  "purpose": "课堂演示"
}
```

返回字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `reservationId` | number | 预约 ID |
| `status` | string | `PENDING` |

规则：

- 必填预约时间和用途。
- 同一时间段重复预约同一设备直接拒绝。
- 设备在 `RESERVED`、`BORROWED`、`REPAIRING` 状态下默认不可重复预约。

### 10.2 预约列表

- 方法：`GET`
- 路径：`/api/reservations`
- 权限：`ADMIN`、`TEACHER`、`STUDENT`

查询参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `status` | string | 否 | 预约状态 |
| `deviceId` | number | 否 | 设备 ID |
| `applicantId` | number | 否 | 申请人 ID |
| `pageNum` | number | 否 | 页码 |
| `pageSize` | number | 否 | 每页数量 |

规则：

- 学生仅可查看自己的预约。
- 教师可查看其管理范围内学生预约。
- 管理员可查看全部预约。

### 10.3 预约详情

- 方法：`GET`
- 路径：`/api/reservations/{reservationId}`
- 权限：`ADMIN`、`TEACHER`、`STUDENT`

### 10.4 审核预约

- 方法：`PUT`
- 路径：`/api/reservations/{reservationId}/approve`
- 权限：`ADMIN`、`TEACHER`

请求体：

```json
{
  "action": "APPROVE",
  "comment": "同意借用"
}
```
或
```json
{
  "action": "REJECT",
  "comment": "该时间段设备已被教学活动占用"
}
```

规则：

- 审核结果至少包括通过、驳回。
- 驳回原因建议必填。
- 是否支持多级审核：`TODO`
- 审核通过后应锁定对应时间段设备资源。

### 10.5 取消预约

- 方法：`PUT`
- 路径：`/api/reservations/{reservationId}/cancel`
- 权限：`STUDENT`

规则：

- 仅本人可取消。
- 审核通过后是否允许取消及取消时机：`TODO`

### 10.6 失效未取用预约

- 方法：`PUT`
- 路径：`/api/reservations/{reservationId}/expire`
- 权限：系统任务 / `ADMIN`

规则：

- 审核通过后未取设备的预约应支持过期失效。
- 自动失效时间规则：`TODO`

## 11. 借用与归还

### 11.1 借用记录列表

- 方法：`GET`
- 路径：`/api/borrow-records`
- 权限：`ADMIN`、`TEACHER`、`STUDENT`

查询参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `status` | string | 否 | 借用状态 |
| `userId` | number | 否 | 用户 ID |
| `deviceId` | number | 否 | 设备 ID |
| `pageNum` | number | 否 | 页码 |
| `pageSize` | number | 否 | 每页数量 |

### 11.2 确认取设备

- 方法：`PUT`
- 路径：`/api/borrow-records/{recordId}/pickup`
- 权限：`STUDENT`

请求体：

```json
{
  "pickupTime": "2026-03-29 09:10:00"
}
```

规则：

- 仅审核通过或待取用的预约可确认取用。
- 取用成功后借用状态改为 `BORROWING`，设备状态改为 `BORROWED`。
- 是否允许管理员代确认取用：`TODO`

### 11.3 归还设备

- 方法：`PUT`
- 路径：`/api/borrow-records/{recordId}/return`
- 权限：`STUDENT`

请求体：

```json
{
  "returnTime": "2026-04-01 17:30:00",
  "deviceCondition": "NORMAL"
}
```

规则：

- 归还后借用记录状态更新为 `RETURNED` 或 `OVERDUE`。
- 设备状态更新为 `AVAILABLE` 或进入待维修处理流程。
- 是否允许管理员代确认归还：`TODO`

### 11.4 逾期扫描

- 方法：`PUT`
- 路径：`/api/borrow-records/{recordId}/overdue`
- 权限：系统任务 / `ADMIN`

规则：

- 到期前默认支持提前 3 天提醒。
- 逾期后需明显标记。
- 超期是否影响信用分或违规记录：`TODO`

### 11.5 到期提醒列表

- 方法：`GET`
- 路径：`/api/borrow-records/reminders`
- 权限：`ADMIN`

查询参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `type` | string | 否 | `ABOUT_TO_EXPIRE` / `OVERDUE` |

## 12. 维修管理

### 12.1 提交维修申请

- 方法：`POST`
- 路径：`/api/repairs`
- 权限：`STUDENT`

请求体：

```json
{
  "deviceId": 1001,
  "description": "设备无法开机"
}
```

规则：

- 维修申请必须关联具体设备和申请人。

### 12.2 维修申请列表

- 方法：`GET`
- 路径：`/api/repairs`
- 权限：`ADMIN`、`TEACHER`、`STUDENT`

查询参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `status` | string | 否 | 维修状态 |
| `deviceId` | number | 否 | 设备 ID |
| `applicantId` | number | 否 | 提交人 ID |
| `pageNum` | number | 否 | 页码 |
| `pageSize` | number | 否 | 每页数量 |

规则：

- 学生仅可查看自己提交的申请。
- 管理员可查看并处理全部申请。

### 12.3 维修申请详情

- 方法：`GET`
- 路径：`/api/repairs/{repairId}`
- 权限：`ADMIN`、`TEACHER`、`STUDENT`

### 12.4 更新维修状态

- 方法：`PUT`
- 路径：`/api/repairs/{repairId}/status`
- 权限：`ADMIN`

请求体：

```json
{
  "status": "PROCESSING",
  "comment": "已送修"
}
```

规则：

- 状态支持 `PENDING`、`PROCESSING`、`COMPLETED`、`UNREPAIRABLE`。
- 维修期间设备默认不可继续预约。

## 13. 统计分析

### 13.1 热门设备排行

- 方法：`GET`
- 路径：`/api/statistics/devices/hot`
- 权限：`ADMIN`、`SUPER_ADMIN`

查询参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `startDate` | string | 否 | 开始日期 |
| `endDate` | string | 否 | 结束日期 |
| `topN` | number | 否 | 返回条数，默认 10 |

规则：

- 统计口径基于借用次数、预约次数或其他指标：`TODO`

### 13.2 设备损坏情况统计

- 方法：`GET`
- 路径：`/api/statistics/devices/damage`
- 权限：`ADMIN`、`SUPER_ADMIN`

### 13.3 用户违规记录统计

- 方法：`GET`
- 路径：`/api/statistics/users/violations`
- 权限：`ADMIN`、`SUPER_ADMIN`

规则：

- 违规定义需明确，例如逾期未还、损坏未报、违规借用：`TODO`

### 13.4 统计概览

- 方法：`GET`
- 路径：`/api/statistics/overview`
- 权限：`ADMIN`、`SUPER_ADMIN`

返回字段建议：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `deviceTotal` | number | 设备总数 |
| `availableDeviceTotal` | number | 可用设备数 |
| `borrowingTotal` | number | 借用中数量 |
| `pendingReservationTotal` | number | 待审核预约数 |
| `pendingRepairTotal` | number | 待处理维修数 |

## 14. 菜单/图标配置

### 14.1 图标列表

- 方法：`GET`
- 路径：`/api/icons`
- 权限：`SUPER_ADMIN`

规则：

- 图标来源是内置图标库还是外部图标库：`TODO`

### 14.2 菜单配置列表

- 方法：`GET`
- 路径：`/api/menus`
- 权限：`SUPER_ADMIN`

### 14.3 更新菜单配置

- 方法：`PUT`
- 路径：`/api/menus/{menuId}`
- 权限：`SUPER_ADMIN`

请求体：

```json
{
  "menuName": "设备管理",
  "path": "/device",
  "icon": "Monitor",
  "permissionCode": "device:view"
}
```

## 15. 关键业务规则汇总

- 新建账号默认密码统一为 `123456`。
- 账号创建权限按角色逐级下放，不允许越权创建。
- 超级管理员默认拥有管理员全部权限。
- 预约申请必须填写预约时间和用途。
- 教师和管理员都有预约审核权限。
- 审核通过后才可进入取用/借用流程。
- 同一时间段设备冲突预约直接拒绝。
- 设备在已预约、借用中、维修中状态下默认不可重复预约。
- 系统支持到期前提醒和已过期提醒。
- 历史业务数据不因账号删除而物理删除。

## 16. 待确认项

- 管理员和超级管理员的登录标识规则。
- 是否要求首次登录强制修改密码。
- 用户账号是否支持启用、禁用、重置密码。
- 信用分初始值、扣减规则、恢复规则。
- 预约审核是否支持多级审核。
- 审核通过后设备锁定的具体规则。
- 未取设备预约的自动失效时间规则。
- 到期提醒和过期提醒的通知方式。
- 是否允许管理员代用户执行取用、归还确认。
- 超期是否自动影响信用分或违规记录。
- 设备导入的具体形式。
- 图标库是内置维护还是接入外部资源。
- 统计分析的时间维度和统计口径。
