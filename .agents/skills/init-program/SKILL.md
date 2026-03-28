---
name: init-program
description: 初始化项目（admin-ui + admin-server）。
---

# 目标

创建一个标准 monorepo 项目结构，包含前端后台管理系统与后端服务，具备完整开发基础设施。

---

# 工作流

## 1. 初始化目录结构

创建基础目录：

- /admin-ui # 前端项目
- /admin-server # 后端项目

---

## 2. 初始化 Git

- 执行 git init
- 创建 .gitignore（包含 node_modules、target、.env 等）
- 可选：初始化 commit 规范（husky + commitlint）

---

## 3. 初始化后端（admin-server）

技术栈：

- JDK 17
- Spring Boot 3.2.5
- Spring Security 6.2.4
- MyBatis Plus 3.5.10.1
- Redis（缓存）
- MySQL 8.0.32
- Maven

初始化内容：

- 创建 Spring Boot 项目
- 配置基础依赖（web、security、mybatis-plus、redis 等）
- 配置 application.yml（数据库、Redis、端口）
- 初始化基础包结构（controller/service/mapper/entity/config）
- 建立统一返回结构（Result）

---

## 4. 初始化前端（admin-ui）

技术栈：

- Vue 3 + TypeScript
- Vite
- Element Plus

初始化内容：

- 配置路由（权限路由结构预留）
- 配置请求工具（axios 封装）
- 配置环境变量（.env）
- 配置基础布局（layout）

---

# 规则

- 前端代码必须位于 /admin-ui
- 后端代码必须位于 /admin-server
- 前后端独立运行，通过 API 交互
- 所有接口统一走 /api 前缀

---

# 输出

- 完整 monorepo 目录结构
- 可运行的前后端基础项目

---

# 自检

- [ ] 项目目录结构正确
- [ ] 前后端均可独立启动
- [ ] 后端接口可正常访问
- [ ] 前端能成功请求后端接口
- [ ] Git 初始化成功
