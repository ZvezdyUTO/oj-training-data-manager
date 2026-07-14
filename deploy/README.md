# Deploy 模块

## 职责

`deploy` 提供 OJ Training Data Manager 的完整本地/单机部署入口。它只编排本项目的 MySQL、Spring API 与 Nginx 前端，不连接其他项目的服务或 volume。

## 文件职责

| 文件 | 职责 |
| --- | --- |
| `docker-compose.yml` | 定义 `training-db`、`training-api`、`frontend` 三个服务、健康检查、依赖关系和独立数据库/日志卷 |
| `.env.example` | 可提交的配置模板，不包含真实 secret；所有自动采集开关默认关闭 |
| `.env` | 本机演示配置，不提交；默认 UI 端口 3100、API 回环端口 8190 |

根目录 `Dockerfile` 构建 `backend/training-api`，`frontend/Dockerfile` 构建 Vue 静态文件和 Nginx 网关。

## 网络与端口

| 入口 | 可见范围 | 用途 |
| --- | --- | --- |
| `frontend:80` → `${FRONTEND_BIND_ADDRESS:-127.0.0.1}:${FRONTEND_PORT:-3100}` | 默认仅宿主机回环 | 页面与浏览器 `/api/**` 请求 |
| `training-api:8190` → `127.0.0.1:${BACKEND_PORT:-8190}` | 仅宿主机回环 | 本机诊断和直接健康检查 |
| `training-db:3306` | 仅 Compose 内部网络 | API 数据库连接，不映射宿主机 |

Nginx 保留 `/api/` 前缀并代理到 `http://training-api:8190/api/`。Compose 等到数据库健康后启动 API，等到 API 的 `/health` 健康后启动前端；前端健康检查同时验证 `/` 与 `/api/health`。

## 配置

```bash
cp deploy/.env.example deploy/.env
chmod 600 deploy/.env
./scripts/compose-config.sh
docker compose --env-file deploy/.env -f deploy/docker-compose.yml up -d --build
./scripts/smoke-test.sh
```

必须修改：

- `TRAINING_DB_PASSWORD`
- `TRAINING_DB_ROOT_PASSWORD`
- `TRAINING_OPERATION_PASSWORD`

数据库使用独立命名 volume `TRAINING_DB_VOLUME_NAME`。不要把它改成其他项目正在使用的 volume 名。当前项目空库启动，Flyway 只创建本项目表结构。

## 自动采集

提交采集与 AtCoder 题目元数据任务默认全部关闭：

- `TRAINING_CODEFORCES_DAILY_COLLECTION_ENABLED`
- `TRAINING_CODEFORCES_INTRADAY_COLLECTION_ENABLED`
- `TRAINING_ATCODER_DAILY_COLLECTION_ENABLED`
- `TRAINING_ATCODER_INTRADAY_COLLECTION_ENABLED`
- `TRAINING_ATCODER_PROBLEM_LIST_SCHEDULE_ENABLED`
- `TRAINING_ATCODER_PROBLEM_LIST_BOOTSTRAP_ENABLED`

每日任务默认向前回看 `100h`，日内任务默认从成功游标直接续爬（`0h`）。同一批任务内成员之间默认间隔由 `TRAINING_JOB_ITEM_INTERVAL=4s` 控制，以避免集中请求外部 OJ API。开启配置后重建或重启 API 才会生效。

## 数据与日志

- MySQL 数据与 API 文件日志分别位于两个独立 Docker volume，普通 `docker compose down` 会保留。
- `docker compose down -v` 会永久删除本项目数据库与文件日志，只能在明确需要重新空库时使用。
- API 的 `/app/logs` 位于 `TRAINING_LOG_VOLUME_NAME` 指定的 volume，包含 `combined.log` 与 `error.log`；运行日志也可通过 `docker compose logs training-api` 查看。
- 采集 job 状态和最近任务历史只在 API 内存中，重启后清空；持久化数据与采集游标保留。
- 日志不得包含全局操作密码、`X-Operation-Password`、数据库密码或其他凭据。

## 安全边界

GET、HEAD、OPTIONS 公开；其他 HTTP 方法全部使用 `X-Operation-Password` 校验。项目没有账号、JWT、session 或角色。这个设计适用于个人部署和可信小团队，不等于可直接安全暴露到公网：若流量经过不可信网络，必须在前端入口之前启用 HTTPS，并限制 3100 端口的网络访问范围。

本机验收默认使用 `FRONTEND_BIND_ADDRESS=127.0.0.1`。需要让可信小团队访问时，可显式改为 `0.0.0.0` 或指定内网地址，但应先配置 HTTPS 终止层和网络访问控制。
