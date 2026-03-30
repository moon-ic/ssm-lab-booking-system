# 实验室设备管理系统

本仓库是实验室设备管理系统的 monorepo 工程，包含前端管理端、后端服务、需求文档与开发辅助 skill。

## 目录结构

```text
.
├─ admin-ui
├─ admin-server
├─ md
└─ .agents
```

## 技术栈

- `admin-ui`：Vue 3 + TypeScript + Vite + Element Plus
- `admin-server`：Spring Boot 3 + Spring Security + MySQL
- Redis：支持 Redis 优先、内存兜底的 token 与缓存实现

## 启动说明

### 前端

```bash
cd admin-ui
npm install
npm run dev
```

### 后端

```bash
cd admin-server
mvn spring-boot:run
```

默认数据库连接可通过以下环境变量覆盖：

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

Redis 可通过以下环境变量启用：

- `APP_REDIS_ENABLED=true`
- `REDIS_HOST`
- `REDIS_PORT`
- `REDIS_PASSWORD`
- `REDIS_DATABASE`

## 初始账号

系统会初始化 4 个测试账号，默认密码统一为 `000000`。

| 身份 | 角色编码 | 登录标识 | 账号 |
| --- | --- | --- | --- |
| 超级管理员 | `SUPER_ADMIN` | `SA001` | `superadmin` |
| 管理员 | `ADMIN` | `A001` | `admin01` |
| 教师 | `TEACHER` | `T2026001` | `teacher01` |
| 学生 | `STUDENT` | `20230001` | `student01` |

说明：

- 超级管理员和管理员可使用“登录标识”或“账号”登录
- 教师使用工号登录
- 学生使用学号登录
- 新创建账号默认密码为 `000000`
- 新创建账号或被重置密码的账号，首次登录后需要先修改密码

## 文档

- PRD：[md/equipment-borrowing-admin-prd.md](/D:/program/booking-skill-demo/md/equipment-borrowing-admin-prd.md)
- API 文档：[md/equipment-borrowing-api-doc.md](/D:/program/booking-skill-demo/md/equipment-borrowing-api-doc.md)

## 后端已实现模块

对照 API 文档，后端目前已完成以下模块：

- 认证与登录
- 用户管理
- 角色与权限
- 设备管理
- 设备导入
- 预约与审核
- 借用与归还
- 维修管理
- 个人中心
- 待确认消息 / 通知中心
- 统计分析
- 菜单 / 图标配置
- 定时任务
- 定时任务执行日志
- MySQL 持久化
- Redis 基础设施接入

## 当前工程内容

- 管理后台前后端基础工程
- 统一 `/api` 接口前缀
- MySQL 初始化脚本
- 后端集成测试与回归测试
- 需求文档与接口文档

## 测试

在后端目录执行：

```bash
cd admin-server
mvn clean test
```

当前主业务链路和主要公开接口已完成回归测试。
