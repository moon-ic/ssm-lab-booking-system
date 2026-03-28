# 实验室设备管理系统

本仓库为实验室设备管理系统的 monorepo 基础工程，包含：

- `admin-ui`：后台管理前端，基于 Vue 3 + TypeScript + Vite + Element Plus
- `admin-server`：后台管理后端，基于 Spring Boot 3 + Spring Security + MyBatis Plus

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

## 当前初始化内容

- monorepo 基础目录
- Vue 3 管理端基础骨架
- Spring Boot 后端基础骨架
- 统一 `/api` 前缀约定
- 基础路由、请求封装、统一返回结构、健康检查接口

