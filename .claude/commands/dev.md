# /dev — 启动开发环境

检查并指导启动本项目的开发环境。

步骤：
1. 提醒用户确认 MySQL 已启动（`net start MySQL`，数据库 `lab_booking`，密码 `200453`）
2. 提示启动后端：`cd admin-server && mvn spring-boot:run`（端口 8081）
3. 提示启动前端：`cd admin-ui && pnpm dev`（端口 5173）
4. 提示验证健康检查：`GET http://localhost:8081/api/health`

如果用户遇到问题，根据错误日志（`admin-server/admin-server-run.log`）诊断。
