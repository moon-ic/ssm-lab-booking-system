# 实验室设备管理系统

这是一个实验室设备管理后台 monorepo，包含前端管理端、后端服务、需求与接口文档，以及用于开发协作的本地技能与辅助资源。

当前仓库已经完成：

- 前后端主业务模块开发
- 前后端联调
- 默认本地环境切换为真实后端模式
- 页面层、布局层、路由层测试补强
- 默认数据库 `lab_booking` 修复到当前代码所需结构

## 目录结构

```text
.
├─ admin-ui
├─ admin-server
├─ md
└─ .agents
```

## 技术栈

### 前端

- Vue 3
- TypeScript
- Vite
- Vue Router
- Element Plus
- Vitest

### 后端

- Spring Boot 3
- Spring Security
- Spring JDBC
- MySQL
- Redis（可选，默认关闭）
- JaCoCo

## 当前默认运行方式

当前默认本地环境已经配置为 `真实后端模式`：

- 前端默认 `VITE_ENABLE_MOCK=false`
- 前端默认 `VITE_API_BASE=/api`
- Vite 开发代理默认转发：
  - `/api -> http://localhost:8081`
- 后端默认数据库：
  - `lab_booking`

也就是说，正常情况下你本地启动前后端后，前端会直接调用真实后端，而不是 mock。

## 快速开始

### 1. 启动后端

```bash
cd admin-server
mvn spring-boot:run
```

默认监听：

- `http://127.0.0.1:8081`

健康检查：

```bash
GET http://127.0.0.1:8081/api/health
```

### 2. 启动前端

```bash
cd admin-ui
npm install
npm run dev
```

Vite 默认端口是 `5173`。如果端口被占用，会自动切换到下一个可用端口，例如 `5174`。

### 3. 打开系统

默认访问：

- `http://localhost:5173`

如果 `5173` 被占用，请以终端日志里的实际地址为准。

## 环境变量

### 后端

后端默认配置位于：

- `admin-server/src/main/resources/application.yml`

可通过环境变量覆盖：

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

默认值：

- `DB_URL=jdbc:mysql://127.0.0.1:3306/lab_booking?createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai`
- `DB_USERNAME=root`
- `DB_PASSWORD=200453`

Redis 相关开关：

- `APP_REDIS_ENABLED=false` 默认关闭
- 可选覆盖：
  - `REDIS_HOST`
  - `REDIS_PORT`
  - `REDIS_PASSWORD`
  - `REDIS_DATABASE`

### 前端

当前已配置：

- `admin-ui/.env.development`
- `admin-ui/.env.production`

默认内容：

```env
VITE_APP_TITLE=实验室设备管理系统
VITE_API_BASE=/api
VITE_ENABLE_MOCK=false
```

如果你想临时切回 mock，可把：

```env
VITE_ENABLE_MOCK=true
```

## 初始化账号

系统内置 4 个默认账号，默认密码统一为 `000000`：

| 身份 | 角色代码 | 登录标识 | 账号 |
| --- | --- | --- | --- |
| 超级管理员 | `SUPER_ADMIN` | `SA001` | `superadmin` |
| 管理员 | `ADMIN` | `A001` | `admin01` |
| 教师 | `TEACHER` | `T2026001` | `teacher01` |
| 学生 | `STUDENT` | `20230001` | `student01` |

说明：

- 超级管理员、管理员可使用“登录标识”或“账号”登录
- 教师使用工号登录
- 学生使用学号登录
- 新建用户默认密码为 `000000`
- 新建用户或重置密码后的用户，首次登录后需要修改密码

## 已实现模块

### 前端

- 登录
- 权限路由
- 用户管理
- 角色权限
- 个人中心
- 设备管理
- 设备导入
- 预约管理
- 借用归还
- 消息中心
- 维修管理
- 统计分析
- 菜单配置

### 后端

- 认证与登录
- 用户管理
- 角色与权限
- 设备管理
- 设备导入
- 预约与审批
- 借用与归还
- 维修管理
- 个人中心
- 消息与通知
- 统计分析
- 菜单与图标配置
- 定时任务执行日志
- MySQL 持久化
- Redis 基础设施接入

## 测试与验证

### 前端测试

```bash
cd admin-ui
npm run test
npm run test:coverage
```

当前已经补齐：

- 页面级测试
- 布局层测试
- 路由守卫测试
- 认证状态测试
- mock 聚合与集成链路测试

### 前端构建

```bash
cd admin-ui
npm run build
```

### 后端测试

```bash
cd admin-server
mvn clean test
```

### 后端覆盖率

JaCoCo 报告输出：

- `admin-server/target/site/jacoco/index.html`

## 联调状态

当前状态：

- 默认数据库 `lab_booking` 已修复到当前代码所需结构
- 后端可直接基于默认库启动
- 前端默认走真实后端
- 真实服务验收已完成

已验证通过的关键项：

- `GET /api/health` 返回 `UP`
- `SA001 / 000000` 可真实登录
- 学生访问受限接口会返回 `403`
- 核心业务链路已在真实服务上跑通：
  - 学生预约
  - 管理员审批
  - 生成借用记录
  - 学生取用
  - 学生归还

## 数据库修复说明

仓库曾检测到本地默认库 `lab_booking` 仍是旧项目表结构。当前已完成：

- 旧库备份
- 默认库重建
- 新 schema 导入
- 新 seed 数据导入
- 默认库真实启动验证

备份文件：

- `backups/lab_booking-20260331-211940.sql`

## 文档

项目文档位于 `md/` 目录：

- PRD：
  - `md/equipment-borrowing-admin-prd.md`
- API 文档：
  - `md/equipment-borrowing-api-doc.md`
- 真实服务验收报告：
  - `md/real-service-acceptance-2026-03-31.md`
- 默认环境人工验收清单：
  - `md/default-env-manual-acceptance-checklist-2026-03-31.md`

## 常用命令

### 前端

```bash
cd admin-ui
npm install
npm run dev
npm run build
npm run test
npm run test:coverage
```

### 后端

```bash
cd admin-server
mvn spring-boot:run
mvn clean test
```

## 当前建议

如果你是第一次接手这个仓库，推荐按下面顺序使用：

1. 启动后端并确认 `/api/health` 正常
2. 启动前端并使用默认账号登录
3. 参考 `md/default-env-manual-acceptance-checklist-2026-03-31.md` 做人工走查
4. 如需了解设计背景，先读 PRD 和 API 文档

## 说明

- 前端当前默认不走 mock
- Vite 构建仍有主包体积较大的告警，但不影响功能使用
- Redis 默认关闭，不影响当前主链路运行
