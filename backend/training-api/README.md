# training-api

## 模块职责

OJ Training Data Manager 的唯一 Spring Boot 运行入口。提供公开查询、训练成员管理、手动采集任务、最终态 Flyway schema 与全局操作密码校验。

## 目录结构

```text
src/main/java/com/ojtraining/manager/api/
  config/    训练模块组合与操作密码配置
  security/  写请求操作密码过滤器
  member/    成员写 API 与事务编排
  web/       查询、采集任务、健康检查、统一响应/异常
src/main/resources/
  db/migration/V001__create_training_data_schema.sql
  application.yml
  logback-spring.xml
src/test/    过滤器、成员事务与 application context 测试
```

## 依赖与分层

- 依赖三个 training-data library 模块；自身拥有 HTTP、DataSource 和 Flyway runtime。
- GET、HEAD、OPTIONS 公开；POST、PUT、PATCH、DELETE 必须通过 `X-Operation-Password`。
- 密码只来自 `app.operation-password` / `TRAINING_OPERATION_PASSWORD`，不进入 URL、持久化或日志。
- 所有成员写先按固定顺序锁定 ATCODER、CODEFORCES fence，再在同一事务中完成成员锁定、旧 handle 的 ODS/DWD/DWM/DWS 清理、游标调整、绑定变更和 generation 推进。
- username 使用大小写敏感排序规则；OJ handle 使用大小写不敏感排序规则，以兼容 OJ API 对 handle 大小写的规范化。
- 不包含账号、登录、JWT、角色、Redis 或 MyBatis。

## 文件级职责

| 文件/路径 | 职责 |
| --- | --- |
| `OjTrainingDataManagerApplication.java` | Spring Boot 入口 |
| `config/TrainingDataModuleConfiguration.java` | 组合 common、Codeforces、AtCoder、调度与 bootstrap |
| `security/OperationPasswordFilter.java` | 恒定时间比对操作密码并统一返回 401 |
| `member/MemberManagementService.java` | 批量创建、安全改名、handle 清理更新和删除事务 |
| `member/MemberController.java` | `/api/members/**` 写接口 |
| `web/TrainingDataQueryController.java` | 七个公开训练查询 GET |
| `web/CollectionJobController.java` | 公开任务查询与受保护的任务创建，接收按 username 的有效 lookback |
| `web/GlobalExceptionHandler.java` | 统一 HTTP status 与 `{code,message,data}` 响应 |
| `resources/db/migration/V001__create_training_data_schema.sql` | 空库最终态 13 表 schema（含两行 OJ 一致性 fence 元数据） |
| `resources/logback-spring.xml` | console、combined.log、error.log 输出 |
