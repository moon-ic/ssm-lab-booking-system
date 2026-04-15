# /test — 运行项目测试

根据用户指定的范围运行测试：

- **前端测试**：`cd admin-ui && npm run test`
- **前端覆盖率**：`cd admin-ui && npm run test:coverage`
- **后端测试**：`cd admin-server && mvn clean test`
- **后端覆盖率报告**：打开 `admin-server/target/site/jacoco/index.html`

如果用户没有指定范围，询问是前端、后端还是全部。

测试文件约定：
- 前端：每个视图（`src/views/*.vue`）对应同目录下的 `*.spec.ts`
- 后端：JaCoCo 覆盖率报告在 `target/site/jacoco/`
