# 实验室设备管理系统

本仓库是实验室设备管理系统的 monorepo 基础工程，包含：

- `admin-ui`：后台管理前端，基于 Vue 3 + TypeScript + Vite + Element Plus
- `admin-server`：后台管理后端，基于 Spring Boot 3 + Spring Security + MySQL
- Redis 基础设施：支持 Redis 优先、内存兜底的 token 和缓存能力

## 目录结构

```text
.
├─ admin-ui
├─ admin-server
├─ md
└─ .agents
```

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

- `DB_USERNAME`
- `DB_PASSWORD`
- `DB_URL`

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
- 如果数据库里这 4 个内置账号仍保留旧默认密码 `123456`，应用启动时会自动升级为 `000000`

## 后端完成度

对照 `md/equipment-borrowing-api-doc.md` 的模块划分，后端已完成以下主要模块：

- 认证与登录
- 用户管理
- 角色与权限
- 个人中心
- 设备管理
- 预约与审核
- 借用与归还
- 维修管理
- 统计分析
- 菜单 / 图标配置
- 定时任务
- 通知中心 / 任务执行日志
- MySQL 持久化
- Redis 基础设施接入

当前仍有两类明确未完全闭环的点：

- `/api/devices/import` 仍为占位接口，返回“导入方式待确认”
- API 文档中标记为 `TODO` 的业务规则还没有全部产品化，例如多级审核、首次登录强制改密、管理员代取代还、外部通知渠道、导入文件格式等

## 当前工程内容

- 管理后台前后端基础骨架
- 统一 `/api` 接口前缀
- 认证、用户、角色权限、设备、预约、借还、维修、统计、菜单、通知、定时任务等后端模块
- MySQL 初始化脚本与测试库重置能力
