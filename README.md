# OJ Training Data Manager

OJ Training Data Manager 是一个面向个人部署和小团队的独立 OJ 训练数据平台。它把 Codeforces、AtCoder 的提交数据采集、分层加工、公开查询和成员管理放进一个可独立运行的项目中。

项目只认 `username`，从空库开始，不依赖、不连接也不迁移其他系统的数据、数据库或 Docker volume。

## 它解决什么问题

OJ API 适合查询单个用户，却不适合长期维护团队训练数据：重复抓取成本高、历史数据容易丢失、不同 OJ 的字段不一致，也很难直接回答“某人最近做了什么题”“某题有哪些人首次通过”等问题。

本项目把这件事拆成稳定的数据链路：

```text
Codeforces / AtCoder
        │
        ▼
ODS 原始提交层
        │
        ▼
DWD 统一明细层
        │
        ├──────────────► DWM 成员-题目首次 AC
        │
        └──────────────► DWS 成员每日难度统计
                              │
                              ▼
                     公开查询 API 与前端
```

- **ODS** 保存接近来源结构的原始提交，负责去重和追溯。
- **DWD** 把不同 OJ 的字段整理为统一提交模型。
- **DWM** 保存成员与题目的首次 AC 关系，支持首次通过查询。
- **DWS** 保存按成员、日期、难度聚合的统计，避免每次查询重新扫描全部提交。
- 数仓刷新使用可重复执行的 upsert，同一批数据重复加工不会无限制造重复记录。

## 核心设计

### 独立部署

运行时只有三个容器：

```text
浏览器 ──► frontend (Nginx + Vue 3) ──► training-api (Spring Boot)
                                                   │
                                                   ▼
                                            training-db (MySQL)
```

Nginx 在一个入口同时提供前端和 `/api/**` 代理；API 是唯一 HTTP 业务入口；MySQL volume 只属于本项目。

### 简化鉴权

- `GET`、`HEAD`、`OPTIONS` 完全公开，任何人都能查询成员、游标、统计和任务状态。
- `POST`、`PUT`、`PATCH`、`DELETE` 必须携带 `X-Operation-Password`。
- 不存在登录、JWT、session、角色、nickname 或个人密码。
- 操作密码只来自部署端的 `TRAINING_OPERATION_PASSWORD`，前端仅在单次确认框和请求调用栈中临时持有。

明文密码指请求凭据格式简单，不代表可以使用明文网络传输。公网部署必须在服务前配置 HTTPS。

### 全量与增量采集

每个 `username + OJ` 独立保存 `lastCollectedAt` 游标：

- 没有成功游标时是首次采集，有效倒退小时强制为 `0`，底层从历史起点全量抓取。
- 已有游标时从 `lastCollectedAt - lookback` 开始，用重叠窗口吸收来源 API 的延迟数据。
- 混合批次按成员分别计算窗口，首次成员使用 `0`，已有游标成员使用填写值。
- 只有成功采集才推进游标；失败不会让下一次采集跳过数据。

### 数据一致性

成员 handle 变更、远端抓取、ODS 写入和数仓刷新不会被当成一个超长数据库事务。系统通过 OJ fence、generation 和二次复核保证抓取期间 handle 被修改或删除时，旧数据不会重新写回。

修改或删除 handle 会同步清理关联的 ODS、DWD、DWM、DWS 数据并重置采集状态。手动任务进度保存在 API 进程内存中，重启后任务历史消失，但持久化数据和游标不会丢失。

## 一键部署

需要 macOS 或 Linux、Docker Engine/Docker Desktop，以及 Docker Compose v2。

```bash
git clone https://github.com/ZvezdyUTO/oj-training-data-manager.git
cd oj-training-data-manager
./scripts/quick-start.sh
```

脚本只在 `deploy/.env` 不存在时生成数据库密码、root 密码和全局操作密码；已有配置不会被覆盖。随后它会校验 Compose、构建并等待三个服务健康，再执行冒烟检查。

启动后打开 [http://localhost:3100](http://localhost:3100)。需要修改数据时，从本机 `deploy/.env` 获取 `TRAINING_OPERATION_PASSWORD`；该文件已被 Git 和 Docker 构建上下文排除，请勿上传或分享。

## 手动部署与配置

需要自定义端口、volume 名称或自动采集计划时，先创建配置：

```bash
cp deploy/.env.example deploy/.env
chmod 600 deploy/.env
# 编辑 deploy/.env，至少替换三个 change-me 密码
./scripts/compose-config.sh
docker compose --env-file deploy/.env -f deploy/docker-compose.yml up -d --build --wait
./scripts/smoke-test.sh
```

默认只监听本机：前端为 `127.0.0.1:3100`，API 为 `127.0.0.1:8190`。所有自动采集和 AtCoder 题目元数据 bootstrap 默认关闭，只有显式修改 `.env` 才会启用。

查看状态、日志和停止服务：

```bash
docker compose --env-file deploy/.env -f deploy/docker-compose.yml ps
docker compose --env-file deploy/.env -f deploy/docker-compose.yml logs -f training-api
docker compose --env-file deploy/.env -f deploy/docker-compose.yml down
```

`down` 不删除数据库；`down -v` 会永久删除本项目数据库与日志 volume，只应在明确需要清空数据时使用。

## 使用流程

1. 打开“成员管理”，输入 `username`、Codeforces/AtCoder handle，并决定是否参与自动采集。
2. 修改操作需要输入全局操作密码；查询不需要密码。
3. 打开“数据采集”，选择 OJ 并执行单人或全部采集。首次成员会显示不可编辑的倒退小时 `0`。
4. 在多人、单人或题目页面查询统计、提交明细和首次 AC。

浏览器和 API 共用同一入口。例如公开健康检查：

```bash
curl http://localhost:3100/api/health
```

写请求必须显式携带当次操作密码：

```bash
curl -X POST http://localhost:3100/api/members/batch \
  -H 'Content-Type: application/json' \
  -H 'X-Operation-Password: <TRAINING_OPERATION_PASSWORD>' \
  --data '{"members":[{"username":"alice","needCollect":true,"handles":{"CODEFORCES":"alice"}}]}'
```

## 项目结构

```text
backend/
  common-core/              数仓 SQL DAG 与执行内核
  training-data-common/     跨 OJ 领域、采集游标、查询和持久化
  training-data-codeforces/ Codeforces 来源与 ODS 适配
  training-data-atcoder/    AtCoder 来源、题目元数据与 ODS 适配
  training-api/             唯一可运行 Spring Boot 服务
frontend/                   Vue 3 前端与 Nginx 网关
deploy/                     三服务 Docker Compose 部署
scripts/                    一键部署、配置检查和冒烟检查
```

更完整的部署参数见 [`deploy/README.md`](deploy/README.md)，各模块边界见对应目录的 `README.md`。

## 开发验证

```bash
mvn clean test
pnpm --dir frontend install --frozen-lockfile
pnpm --dir frontend lint
pnpm --dir frontend test
pnpm --dir frontend build
./scripts/compose-config.sh
```
