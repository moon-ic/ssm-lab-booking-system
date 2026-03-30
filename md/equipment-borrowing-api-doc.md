# 设备借用后台管理系统 API 文档

## 1. 文档说明

- 基于 `md/equipment-borrowing-admin-prd.md` 生成
- 风格：RESTful JSON API
- Base URL：`/api`
- 认证方式：`Authorization: Bearer <token>`
- 时间格式：`YYYY-MM-DD HH:mm:ss`

## 2. 角色与权限

| 角色 | 标识 | 主要权限 |
| --- | --- | --- |
| 超级管理员 | `SUPER_ADMIN` | 管理管理员、角色权限、菜单图标、设备导入、全局统计 |
| 管理员 | `ADMIN` | 创建教师、管理设备、导入设备、审核预约、处理维修、查看统计 |
| 教师 | `TEACHER` | 创建/删除学生、审核预约、查看学生借用与消息 |
| 学生 | `STUDENT` | 查看设备、预约、取用、归还、查看个人中心、确认消息、提交维修 |

## 3. 通用响应结构

```json
{
  "code": 0,
  "message": "success",
  "data": {},
  "requestId": "202603301200001234"
}
```

分页结构：

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

## 4. 核心状态定义

### 4.1 用户状态

- `ENABLED`
- `DISABLED`

### 4.2 设备状态

- `AVAILABLE`
- `RESERVED`
- `BORROWED`
- `REPAIRING`
- `DAMAGED`
- `DISABLED`

### 4.3 预约状态

- `PENDING`
- `APPROVED`
- `REJECTED`
- `PICKUP_PENDING`
- `EXPIRED`

### 4.4 借用状态

- `PICKUP_PENDING`
- `BORROWING`
- `RETURNED`
- `OVERDUE`

### 4.5 维修状态

- `PENDING`
- `PROCESSING`
- `COMPLETED`
- `UNREPAIRABLE`

### 4.6 消息确认状态

- `UNCONFIRMED`
- `CONFIRMED`

## 5. 认证与登录

### 5.1 登录

- `POST /api/auth/login`
- 权限：公开

请求：

```json
{
  "loginId": "20230001",
  "password": "000000"
}
```

返回：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "token": "jwt-token",
    "tokenType": "Bearer",
    "expiresIn": 7200,
    "firstLoginRequired": true,
    "userInfo": {
      "userId": 4,
      "name": "Student Wang",
      "account": "student01",
      "roleCode": "STUDENT",
      "status": "ENABLED"
    }
  }
}
```

规则：

- 新建账号默认密码为 `000000`
- 首次登录后返回 `firstLoginRequired=true`
- 首次登录用户必须先修改密码后再继续使用业务功能

### 5.2 获取当前用户信息

- `GET /api/auth/me`
- 权限：已登录用户

返回字段：

- `userId`
- `name`
- `account`
- `jobNoOrStudentNo`
- `roleCode`
- `status`
- `creditScore`
- `firstLoginRequired`

### 5.3 修改密码

- `PUT /api/auth/password`
- 权限：已登录用户

请求：

```json
{
  "oldPassword": "000000",
  "newPassword": "abc123456"
}
```

规则：

- 必须校验旧密码
- 新密码长度不少于 8 位
- 首次登录用户修改成功后，`firstLoginRequired` 置为 `false`

## 6. 用户管理

### 6.1 用户列表

- `GET /api/users`
- 权限：`SUPER_ADMIN` / `ADMIN` / `TEACHER`

查询参数：

- `keyword`
- `roleCode`
- `status`
- `pageNum`
- `pageSize`

### 6.2 用户详情

- `GET /api/users/{userId}`
- 权限：`SUPER_ADMIN` / `ADMIN` / `TEACHER`

### 6.3 创建管理员

- `POST /api/users/admins`
- 权限：`SUPER_ADMIN`

请求：

```json
{
  "name": "管理员A",
  "account": "admin_a",
  "phone": "13800000000"
}
```

规则：

- 默认密码为 `000000`
- `firstLoginRequired=true`

### 6.4 创建教师

- `POST /api/users/teachers`
- 权限：`ADMIN`

### 6.5 创建学生

- `POST /api/users/students`
- 权限：`TEACHER`

### 6.6 删除学生

- `DELETE /api/users/students/{userId}`
- 权限：`TEACHER`

规则：

- 删除前必须校验该学生无未归还设备
- 采用逻辑删除，不物理删除历史业务数据

### 6.7 更新用户状态

- `PUT /api/users/{userId}/status`
- 权限：`SUPER_ADMIN` / `ADMIN`

请求：

```json
{
  "status": "DISABLED"
}
```

### 6.8 重置密码

- `PUT /api/users/{userId}/reset-password`
- 权限：`SUPER_ADMIN` / `ADMIN`

请求：

```json
{
  "newPassword": "000000"
}
```

规则：

- 重置后目标账号 `firstLoginRequired=true`
- 超级管理员可重置管理员、教师、学生
- 管理员可重置教师、学生

## 7. 角色与权限

### 7.1 角色列表

- `GET /api/roles`
- 权限：`SUPER_ADMIN`

### 7.2 新增角色

- `POST /api/roles`
- 权限：`SUPER_ADMIN`

### 7.3 更新角色

- `PUT /api/roles/{roleId}`
- 权限：`SUPER_ADMIN`

### 7.4 角色详情

- `GET /api/roles/{roleId}`
- 权限：`SUPER_ADMIN`

### 7.5 分配权限

- `PUT /api/roles/{roleId}/permissions`
- 权限：`SUPER_ADMIN`

请求：

```json
{
  "permissionIds": [1, 2, 3],
  "menuIds": [201, 202]
}
```

规则：

- 菜单权限与功能权限分开维护
- 菜单可见不等于自动拥有功能操作权限

### 7.6 权限列表

- `GET /api/permissions`
- 权限：`SUPER_ADMIN`

查询参数：

- `type=MENU|ACTION`

## 8. 个人中心

### 8.1 个人信息

- `GET /api/profile`
- 权限：已登录用户

### 8.2 我的借阅记录

- `GET /api/profile/borrow-records`
- 权限：已登录用户

查询参数：

- `status`
- `pageNum`
- `pageSize`

### 8.3 我的消息列表

- `GET /api/profile/messages`
- 权限：已登录用户

查询参数：

- `confirmStatus=UNCONFIRMED|CONFIRMED`
- `type`
- `pageNum`
- `pageSize`

### 8.4 确认消息

- `PUT /api/profile/messages/{messageId}/confirm`
- 权限：已登录用户

返回：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "messageId": 10001,
    "confirmStatus": "CONFIRMED",
    "confirmedAt": "2026-03-30 12:00:00"
  }
}
```

## 9. 设备管理

### 9.1 设备列表

- `GET /api/devices`
- 权限：已登录用户

查询参数：

- `keyword`
- `category`
- `status`
- `pageNum`
- `pageSize`

### 9.2 设备详情

- `GET /api/devices/{deviceId}`
- 权限：已登录用户

### 9.3 新增设备

- `POST /api/devices`
- 权限：`ADMIN`

### 9.4 更新设备

- `PUT /api/devices/{deviceId}`
- 权限：`ADMIN`

### 9.5 更新设备状态

- `PUT /api/devices/{deviceId}/status`
- 权限：`ADMIN`

## 10. 设备导入

### 10.1 导入设备

- `POST /api/device-imports`
- 权限：`ADMIN` / `SUPER_ADMIN`
- `Content-Type: multipart/form-data`

表单字段：

- `deviceName`：必填
- `category`：选填
- `location`：选填
- `description`：选填
- `image`：必传，设备图片

返回：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "deviceId": 1008,
    "deviceCode": "EQ-2026-1008",
    "deviceName": "投影仪A",
    "imageUrl": "/uploads/devices/1008.png",
    "status": "AVAILABLE"
  }
}
```

规则：

- 上传成功后自动生成唯一 `deviceId` 与 `deviceCode`
- 不支持 Excel 批量导入

## 11. 预约与审核

### 11.1 提交预约

- `POST /api/reservations`
- 权限：`STUDENT`

### 11.2 预约列表

- `GET /api/reservations`
- 权限：`ADMIN` / `TEACHER` / `STUDENT`

### 11.3 预约详情

- `GET /api/reservations/{reservationId}`
- 权限：`ADMIN` / `TEACHER` / `STUDENT`

### 11.4 审核预约

- `PUT /api/reservations/{reservationId}/approve`
- 权限：`ADMIN` / `TEACHER`

请求：

```json
{
  "action": "APPROVE",
  "comment": "通过"
}
```

或

```json
{
  "action": "REJECT",
  "comment": "时间冲突"
}
```

规则：

- 审核为单级审核
- 驳回必须填写原因

### 11.5 自动失效未取用预约

- `PUT /api/reservations/{reservationId}/expire`
- 权限：系统任务 / `ADMIN`

规则：

- 审核通过后超过预约开始时间 24 小时未取用，自动失效
- 失效后生成一条预约失效消息

## 12. 借用与归还

### 12.1 借用记录列表

- `GET /api/borrow-records`
- 权限：`ADMIN` / `TEACHER` / `STUDENT`

### 12.2 确认取用

- `PUT /api/borrow-records/{recordId}/pickup`
- 权限：`STUDENT`

### 12.3 确认归还

- `PUT /api/borrow-records/{recordId}/return`
- 权限：`STUDENT`

### 12.4 超期扫描

- `PUT /api/borrow-records/{recordId}/overdue`
- 权限：系统任务 / `ADMIN`

### 12.5 到期提醒列表

- `GET /api/borrow-records/reminders`
- 权限：`ADMIN`

查询参数：

- `type=ABOUT_TO_EXPIRE|OVERDUE`

## 13. 消息中心

### 13.1 消息列表

- `GET /api/messages`
- 权限：`ADMIN` / `TEACHER` / `STUDENT`

查询参数：

- `userId`
- `type`
- `confirmStatus`
- `pageNum`
- `pageSize`

### 13.2 未确认消息统计

- `GET /api/messages/unconfirmed-summary`
- 权限：已登录用户

返回：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "total": 3,
    "aboutToExpireCount": 1,
    "overdueCount": 1,
    "firstLoginCount": 1
  }
}
```

### 13.3 确认消息

- `PUT /api/messages/{messageId}/confirm`
- 权限：消息所属用户

## 14. 维修管理

### 14.1 提交维修申请

- `POST /api/repairs`
- 权限：`STUDENT`

### 14.2 维修列表

- `GET /api/repairs`
- 权限：`ADMIN` / `TEACHER` / `STUDENT`

### 14.3 维修详情

- `GET /api/repairs/{repairId}`
- 权限：`ADMIN` / `TEACHER` / `STUDENT`

### 14.4 更新维修状态

- `PUT /api/repairs/{repairId}/status`
- 权限：`ADMIN`

## 15. 统计分析

### 15.1 榜单时间维度

所有榜单接口统一支持：

- `rankScope=TOTAL`
- `rankScope=HALF_YEAR`
- `rankScope=MONTH`

### 15.2 热门设备榜

- `GET /api/statistics/devices/hot`
- 权限：`ADMIN` / `SUPER_ADMIN`

查询参数：

- `rankScope`
- `topN`

统计口径：

- 按借用次数排序

### 15.3 设备损坏榜

- `GET /api/statistics/devices/damage`
- 权限：`ADMIN` / `SUPER_ADMIN`

查询参数：

- `rankScope`
- `topN`

统计口径：

- 按维修申请次数和损坏次数排序

### 15.4 用户违规榜

- `GET /api/statistics/users/violations`
- 权限：`ADMIN` / `SUPER_ADMIN`

查询参数：

- `rankScope`
- `topN`

统计口径：

- 按逾期次数、损坏归还次数排序

### 15.5 统计概览

- `GET /api/statistics/overview`
- 权限：`ADMIN` / `SUPER_ADMIN`

## 16. 菜单 / 图标配置

### 16.1 图标列表

- `GET /api/icons`
- 权限：`SUPER_ADMIN`

规则：

- 图标来源为系统内置图标库

### 16.2 菜单配置列表

- `GET /api/menus`
- 权限：`SUPER_ADMIN`

### 16.3 更新菜单配置

- `PUT /api/menus/{menuId}`
- 权限：`SUPER_ADMIN`

请求：

```json
{
  "menuName": "设备管理",
  "path": "/device",
  "icon": "Monitor",
  "permissionCode": "device:view"
}
```

## 17. 关键业务规则汇总

- 新建账号默认密码统一为 `000000`
- 新建账号首次登录必须修改密码
- 管理员和超级管理员支持重置密码
- 到期提醒与过期提醒统一进入消息中心
- 消息分为未确认与已确认两种状态
- 设备导入采用表单录入 + 图片上传方式
- 图标库采用系统内置维护
- 榜单统一支持总榜、半年榜、月榜
