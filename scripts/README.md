# Scripts 模块

## 职责与布局

该目录只保存可重复执行的本地部署辅助脚本，不包含业务逻辑，也不修改数据库内容。

| 文件 | 职责 |
| --- | --- |
| `compose-config.sh` | 使用 `deploy/.env` 展开并校验 Compose 配置 |
| `smoke-test.sh` | 在服务启动后验证页面、代理健康端点和 API 回环健康端点 |

脚本从自身路径解析仓库根目录，因此可以在任意工作目录执行。`smoke-test.sh` 可通过 `ENV_FILE=/absolute/path/to/.env` 使用其他本地配置文件。

