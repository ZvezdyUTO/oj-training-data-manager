# training-data-codeforces

## 模块职责

实现 Codeforces `user.status` 数据源访问、分页/限速/重试、ODS 写入、数仓刷新、查询所需 SQL 以及按 handle 清理。

## 目录结构

```text
src/main/java/com/ojtraining/manager/trainingdata/codeforces/
  app/      采集与 ODS ingest 编排
  config/   source client、writer 和 refresh bean
  domain/   Codeforces payload/ODS port 与模型
  infra/    REST、Jackson、JDBC adapter
src/main/resources/sql/  ODS upsert 与 DWD/DWM/DWS manifest
src/main/resources/fixtures/  离线测试样本
src/test/   source、parser、writer、purge 和数仓 SQL 测试
```

## 依赖与分层

- 依赖 `common-core` 和 `training-data-common`。
- 外部 API 与 Codeforces payload 只存在于本模块。
- 测试通过 fixture、fake 或本地 HTTP server 运行，不依赖线上 API。

## 文件级职责

| 文件/路径 | 职责 |
| --- | --- |
| `app/CodeforcesSubmissionCollectionService.java` | 按成员游标采集 Codeforces 提交 |
| `infra/RestClientCodeforcesSubmissionSourceClient.java` | 调用 `user.status` |
| `infra/JacksonSubmissionPayloadParser.java` | 解析 Codeforces payload |
| `infra/JdbcCodeforcesOdsSubmissionWriter.java` | 幂等写入 Codeforces ODS |
| `infra/JdbcCodeforcesOdsDataPurgeRepository.java` | 按 handle 清理 ODS |
| `infra/JdbcCodeforcesWarehouseRefreshIntervalRepository.java` | 从 ODS batch 计算刷新区间 |
| `resources/sql/tasks/codeforces-warehouse-refresh.yml` | Codeforces 数仓刷新 DAG |
| `resources/sql/{dwd,dwm,dws}/` | 幂等数仓构建 SQL |
