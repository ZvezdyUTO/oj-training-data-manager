# OJ Training Data Manager

OJ Training Data Manager 是一个面向个人部署和小团队使用的独立 OJ 训练数据管理项目。它负责 Codeforces、AtCoder 数据采集，ODS/DWD/DWM/DWS 数仓加工，训练查询，成员与 OJ handle 管理，以及手动和定时采集。

本项目从空库开始，不依赖、不连接也不迁移任何其他项目的数据、数据库或 Docker volume。

## 功能与边界

- 成员身份只保留 `username`，不包含 nickname、登录账号、角色或个人密码。
- 所有 `GET` 查询完全公开，包括 handle、采集游标和任务详情。
- 只有 `GET`、`HEAD`、`OPTIONS` 公开；其他请求全部必须携带 `X-Operation-Password`。
- 不存在登录、JWT、session 或权限角色；全局操作密码由部署配置生效，修改后重启 API。
- 支持 Codeforces 和 AtCoder 提交采集、数仓刷新与统计查询。
- 前端沿用原 Training 工作台的页面结构和样式，五个入口直接平铺在 52px 黑色顶栏。
- 自动采集和 AtCoder 题目元数据 bootstrap 默认全部关闭，必须由部署者显式启用。

## 目录

```text
backend/                 Java 21 / Spring Boot 多模块后端
  common-core/           数仓 SQL 任务编排与执行内核
  training-data-common/  公共领域、数仓 SQL、采集、存储和查询能力
  training-data-codeforces/ Codeforces 数据源适配
  training-data-atcoder/ AtCoder 数据源适配
  training-api/          唯一可运行 HTTP 服务
frontend/                Vue 3 / Vite 前端和 Nginx 网关
deploy/                  独立的三服务 Compose 部署
scripts/                 部署配置检查和运行后冒烟检查
```

运行时只有三个服务：MySQL 8.4 `training-db`、内部端口为 8190 的 `training-api`，以及默认仅绑定宿主机回环地址 3100 端口的 `frontend`。浏览器请求 `/api/**` 时由 Nginx 转发到 API。

## 快速启动

需要 Docker Engine 或 Docker Desktop，并支持 Docker Compose v2。

当前目录包含一个仅供本机验收且不纳入 Git 的 `deploy/.env`，密码使用本次部署生成的随机值。直接启动：

```bash
docker compose --env-file deploy/.env -f deploy/docker-compose.yml up -d --build
docker compose --env-file deploy/.env -f deploy/docker-compose.yml ps
./scripts/smoke-test.sh
```

浏览器打开 [http://localhost:3100](http://localhost:3100)。API 也会仅绑定到宿主机回环地址 [http://127.0.0.1:8190/health](http://127.0.0.1:8190/health)，不会直接暴露给局域网。

查看日志或停止服务：

```bash
docker compose --env-file deploy/.env -f deploy/docker-compose.yml logs -f training-api
docker compose --env-file deploy/.env -f deploy/docker-compose.yml down
```

`down` 不会删除数据库。只有明确需要清空本项目数据时才执行 `down -v`；该命令会删除独立的数据库与日志 volume，其中数据库无法恢复。

## 正式部署配置

先从模板创建本地配置，并至少更换数据库密码、root 密码和全局操作密码：

```bash
cp deploy/.env.example deploy/.env
chmod 600 deploy/.env
./scripts/compose-config.sh
```

`deploy/.env` 已被 `.gitignore` 排除。不要把真实密码提交到 Git、写入镜像或发到前端构建变量中。`TRAINING_OPERATION_PASSWORD` 更新后需要重建或重启 `training-api`：

```bash
docker compose --env-file deploy/.env -f deploy/docker-compose.yml up -d --no-deps --force-recreate training-api
```

操作密码以明文请求头形式校验，因此公网或不可信网络部署必须在本项目 Nginx 之前配置 HTTPS 终止层。前端只在修改确认框中临时持有密码，不应把它保存到 localStorage、sessionStorage、Cookie 或 URL。

## HTTP 使用约定

查询请求无需任何凭据：

```bash
curl http://localhost:3100/api/health
```

任何修改请求都必须提供操作密码：

```bash
curl -X POST http://localhost:3100/api/example \
  -H 'Content-Type: application/json' \
  -H 'X-Operation-Password: <你的 TRAINING_OPERATION_PASSWORD>' \
  --data '{}'
```

上例 `/api/example` 只用于展示请求头合同，请以实际页面或 API 文档中的写接口替换。未提供或密码错误时，API 应返回 `401 Unauthorized`；查询接口不得要求这个请求头。

## 数据、采集与任务状态

- Flyway 在全新 MySQL 库中创建最终态训练表，不导入历史数据。
- 成员、OJ handle、提交、数仓结果和 `lastCollectedAt` 游标持久化在 MySQL volume 中。
- 某个成员/OJ 没有游标时，手动任务会把该成员的有效 lookback 强制为 `0` 并覆盖全部历史；混合批次中只有已有游标的成员使用填写的倒退小时。后续从上次成功游标减去 lookback 开始，且只有整次成功才推进游标。
- 修改或删除 handle 时，同一业务操作会清理该绑定关联的 ODS/DWD/DWM/DWS 数据并重置采集状态。
- 手动采集任务的运行状态与最近任务记录只保存在 `training-api` 进程内存；容器重启后记录消失，但已经落库的数据和游标不受影响。
- `.env` 中的所有定时采集开关默认是 `false`。启用前应确认外部 API 配额、采集频率和主机资源。

## 日志与排障

API 将日志写到项目自己的 `oj-training-data-manager_training-api-logs` Docker volume，容器内路径是 `/app/logs`：

- `combined.log`：综合运行日志。
- `error.log`：错误日志；错误事件应包含稳定的 `errorCode`。

日志不得记录 `X-Operation-Password`、操作密码、数据库密码或其他凭据。日常优先使用 `docker compose logs` 查看，常用检查：

```bash
./scripts/compose-config.sh
docker compose --env-file deploy/.env -f deploy/docker-compose.yml ps
docker compose --env-file deploy/.env -f deploy/docker-compose.yml logs --tail=200 training-db training-api frontend
./scripts/smoke-test.sh
```

更完整的部署配置说明见 [`deploy/README.md`](deploy/README.md)。

## 本地验证

```bash
mvn clean test
pnpm --dir frontend install --frozen-lockfile
pnpm --dir frontend lint
pnpm --dir frontend test
pnpm --dir frontend build
./scripts/compose-config.sh
```

不执行 Git 提交或推送，除非项目所有者明确下令；MR 标题和描述使用中文。
