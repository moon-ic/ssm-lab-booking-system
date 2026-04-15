# 实验室设备管理系统 — Claude 项目上下文

## 项目概述

Monorepo，包含前端管理端（Vue 3）和后端服务（Spring Boot 3），用于管理实验室设备的预约、借用、归还与维修流程。

## 目录结构

```
.
├── admin-ui/          # 前端（Vue 3 + TypeScript + Vite + Element Plus）
├── admin-server/      # 后端（Spring Boot 3 + Spring Security + MySQL）
├── md/                # PRD、API 文档、验收报告
├── backups/           # 数据库备份
└── CLAUDE.md
```

## 技术栈

### 前端（admin-ui）

- Vue 3 + TypeScript
- Vite（开发服务器：5173，代理 `/api` → `http://localhost:8081`）
- Element Plus
- Vue Router（权限路由）
- Pinia（状态管理）
- Vitest（单元/集成测试）
- 默认 `VITE_ENABLE_MOCK=false`（走真实后端）

### 后端（admin-server）

- Spring Boot 3 + Spring Security
- Spring JDBC + MySQL（数据库：`lab_booking`，端口 3306）
- Redis（默认关闭，`APP_REDIS_ENABLED=false`）
- JaCoCo（覆盖率报告：`target/site/jacoco/index.html`）
- 监听端口：`8081`

## 角色与权限

| 角色 | 代码 | 登录方式 |
|------|------|---------|
| 超级管理员 | `SUPER_ADMIN` | 登录标识 `SA001` 或账号 `superadmin` |
| 管理员 | `ADMIN` | 登录标识 `A001` 或账号 `admin01` |
| 教师 | `TEACHER` | 工号 `T2026001` |
| 学生 | `STUDENT` | 学号 `20230001` |

默认密码：`000000`。新用户首次登录后必须修改密码。

## 常用命令

### 前端

```bash
cd admin-ui
pnpm dev          # 启动开发服务器
pnpm build        # 构建生产包
npm run test      # 运行测试
npm run test:coverage  # 测试覆盖率
```

### 后端

```bash
cd admin-server
mvn spring-boot:run   # 启动后端
mvn clean test        # 运行测试
```

### 数据库（Windows）

```powershell
net start MySQL
```

## 环境变量

### 后端（可通过环境变量覆盖）

- `DB_URL` — 默认 `jdbc:mysql://127.0.0.1:3306/lab_booking?...`
- `DB_USERNAME` — 默认 `root`
- `DB_PASSWORD` — 默认 `200453`
- `APP_REDIS_ENABLED` — 默认 `false`

### 前端（.env.development / .env.production）

- `VITE_API_BASE=/api`
- `VITE_ENABLE_MOCK=false`

## 关键接口

- 健康检查：`GET http://localhost:8081/api/health`（返回 `UP`）
- API 文档：`md/equipment-borrowing-api-doc.md`
- PRD：`md/equipment-borrowing-admin-prd.md`

## 已实现模块

登录、权限路由、用户管理、角色权限、个人中心、设备管理、设备导入、预约管理、借用归还、消息中心、维修管理、统计分析、菜单配置。

## 核心业务流程

学生预约 → 管理员审批 → 生成借用记录 → 学生取用 → 学生归还

## 注意事项

- 前端视图文件位于 `admin-ui/src/views/`，每个视图对应一个 `.spec.ts` 测试文件
- 后端配置文件：`admin-server/src/main/resources/application.yml`
- Vite 构建有主包体积告警，不影响功能
- Redis 默认关闭，不影响主链路
- 数据库备份在 `backups/` 目录
