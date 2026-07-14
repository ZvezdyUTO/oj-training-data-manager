# training-data-atcoder

## 模块职责

实现 Kenkoooo AtCoder submissions、problem list 与 problem model 的采集，负责 AtCoder ODS 写入、DWD/DWM/DWS 刷新、查询数据准备和按 handle 清理。

## 目录结构

```text
src/main/java/com/ojtraining/manager/trainingdata/atcoder/
  app/      submission/problem metadata 采集编排
  config/   source、writer、bootstrap 与 schedule 配置
  domain/   AtCoder payload/ODS port 与模型
  infra/    REST、Jackson、JDBC adapter
src/main/resources/sql/  ODS upsert 与 DWD/DWM/DWS manifest
src/test/   collector、parser、writer、purge 和数仓 SQL 测试
```

## 依赖与分层

- 依赖 `common-core` 和 `training-data-common`。
- Kenkoooo API、AtCoder payload 与 ODS 结构不进入 common。
- 提交自动采集、题目元数据 bootstrap 和 schedule 均默认关闭。

## 文件级职责

| 文件/路径 | 职责 |
| --- | --- |
| `app/AtcoderSubmissionCollectionService.java` | 按成员游标采集 AtCoder 提交 |
| `app/AtcoderProblemListCollectionService.java` | 采集题目与难度模型元数据 |
| `config/AtcoderProblemListBootstrapRunner.java` | 可选的启动时题目元数据初始化 |
| `config/AtcoderProblemListSchedulingConfig.java` | 可选的题目元数据定时采集 |
| `infra/RestClientAtcoderSourceClient.java` | 访问 Kenkoooo API |
| `infra/JacksonAtcoderPayloadParser.java` | 解析 submission/problem/model payload |
| `infra/JdbcAtcoderOds*Writer.java` | 幂等写入三类 AtCoder ODS 表 |
| `resources/sql/tasks/atcoder-warehouse-refresh.yml` | AtCoder 数仓刷新 DAG |
| `resources/sql/{dwd,dwm,dws}/` | 幂等数仓构建 SQL |
