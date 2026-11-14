> **2026-11-10 恢复第一版功能**：当前恢复完整内容维护与原角色权限，详见[第一版恢复说明](docs/第一版恢复说明.md)。

# 人工智能社官网

完整代码已生成在现有目录中：

```text
frontend/   Vue 3 官网与管理后台
backend/    Spring Boot 接口、权限、数据库迁移与测试
deploy/     Docker Compose、Nginx、备份恢复脚本
docs/       运行维护、接口设计与验证记录
```

快速启动（正式本地开发需要 MySQL）：见 [运行与维护](docs/运行与维护.md)。

如果只想先查看界面，可使用测试源码提供的内存预览，分别在两个终端执行：

```powershell
cd backend
mvn spring-boot:test-run -Dspring-boot.run.main-class=cn.aiclub.PreviewApplication
```

```powershell
cd frontend
npm.cmd ci
npm.cmd run dev
```

访问 http://localhost:5173 。临时预览账号：`test-super`，密码：`a-test-password-12345`。此账号和 H2 仅用于测试，未打包进生产 jar；内存内容在后端退出后清空。

正式部署请设置数据库与首次超级管理员环境变量，执行 deploy/ 下的 Docker Compose 配置，不使用测试预览账号。完整说明包含 HTTPS、数据持久化、交接、备份和恢复。

- [运行与维护](docs/运行与维护.md)
- [接口与数据设计](docs/接口与数据设计.md)
- [验证记录](docs/验证记录.md)
- [需求与验收依据](README.md)
- [原技术约定](TechStacks.md)

Spring Boot 按原约定使用 3.5.16；该系列开源支持已于 2026 年 6 月结束，生产上线需确认企业支持或评估主版本升级，详见维护文档。
