# training-data-common

## 模块职责

提供跨 OJ 的训练成员/handle 契约、采集游标、查询 facade、采集任务、调度、数仓刷新与清理能力。成员身份只有 `username`，持久化状态只有 `needCollect`、OJ handles 和各 OJ 的 `lastCollectedAt`。

## 目录结构

```text
src/main/java/com/ojtraining/manager/trainingdata/common/
  app/        成员、查询、刷新和清理编排
  collector/  采集窗口、dispatcher、成员绑定一致性锁与进程内任务
  domain/     跨 OJ 模型、criteria 与 repository port
  infra/      JDBC repository
  scheduler/  可配置自动采集
  web/        与框架无关的采集请求/响应 DTO
src/test/     聚焦单元与 JDBC 测试
```

## 依赖与分层

- `domain` 不依赖 HTTP 或具体 OJ payload。
- `app` 通过 repository port 编排；`infra` 负责 JDBC。
- Codeforces 和 AtCoder 模块实现各自 ODS/source port。
- 手动采集支持按 username 传入 lookback；任务执行时会再次读取成员/OJ 游标，无成功游标的成员无条件使用 `PT0S`，由底层采集器执行全历史抓取。
- 远端抓取不占用数据库事务；抓取前在短事务中锁定 OJ fence 并同时快照 generation、handle 与 cursor，写 ODS/推进游标时再次锁 fence 并核对 generation。
- 成员变更、采集落库和数仓刷新按固定 OJ 顺序共享 `oj_data_consistency_fence`，避免删除后同 handle 重绑（ABA）或在途刷新留下脏数据；全失败采集不推进 generation。
- 本模块不包含登录、JWT、角色、Redis 或可运行入口。

## 文件级职责

| 文件/路径 | 职责 |
| --- | --- |
| `app/account/OjHandleAccountService.java` | 成员与 handle 校验、完整 handle 集替换、采集游标推进 |
| `domain/oj/model/OjHandleAccount.java` | username、needCollect、handles、collectionStates 模型 |
| `infra/oj/repo/account/JdbcOjHandleAccountRepository.java` | `training_member` 与 `oj_handle_binding` JDBC 持久化 |
| `app/query/OjWarehouseQueryFacade.java` | 单人/多人汇总、提交和首 AC 查询入口 |
| `app/purge/OjStudentDataPurgeService.java` | 同事务清理指定成员/OJ 的 ODS、DWD、DWM、DWS |
| `collector/job/OjSubmissionCollectionJobService.java` | 按成员解析有效 lookback、首次采集强制归零、单线程批量采集与最多 50 条内存任务记录 |
| `collector/lock/OjCollectionConsistencyGuard.java` | generation + handle/cursor 原子快照与采集 finalize 门禁契约 |
| `collector/lock/JdbcOjCollectionConsistencyGuard.java` | 事务内锁 OJ fence、复核 generation 并原子完成 ODS/cursor/generation 更新 |
| `collector/lock/OjDataConsistencyFence.java` | 成员绑定变更的固定 OJ 锁序契约 |
| `collector/lock/JdbcOjDataConsistencyFence.java` | 依次锁 ATCODER、CODEFORCES fence 并在成员事务成功后推进 generation |
| `scheduler/OjCollectorSchedulingConfig.java` | 根据配置注册自动采集任务 |
| `infra/oj/repo/query/` | 跨 OJ 数仓查询 JDBC 实现 |
| `infra/oj/repo/warehouse/` | 数仓数据清理 JDBC 实现 |
| `web/collector/request/OjSubmissionCollectionJobStartRequest.java` | 校验全局及按 username 的非负 lookback 请求参数 |
