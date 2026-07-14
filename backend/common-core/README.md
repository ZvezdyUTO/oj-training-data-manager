# common-core

## 模块职责

提供与具体 OJ 无关的 SQL 任务 DAG 执行器。它在每次运行时读取 YAML manifest、校验依赖图，并按拓扑顺序执行 SQL；每个节点使用独立事务。

## 目录结构

```text
src/main/java/com/ojtraining/manager/common/sqltask/  SQL task 实现
src/test/java/com/ojtraining/manager/common/sqltask/  单元测试
src/test/resources/sqltask/                           测试 manifest 与 SQL
```

## 依赖与分层

- 只依赖 Spring Resource、JDBC/Transaction、SnakeYAML 和 SLF4J。
- 不包含训练成员、OJ、HTTP 或鉴权概念。
- 具体数仓模块拥有 manifest、SQL 脚本和业务参数。

## 文件级职责

| 文件 | 职责 |
| --- | --- |
| `SqlTaskRunner.java` | 加载、规划并执行 SQL DAG，汇总节点状态 |
| `YamlSqlTaskManifestLoader.java` | 读取和校验 YAML manifest |
| `SqlTaskGraph.java` | 构建依赖图、检测环和生成执行计划 |
| `SqlScriptSplitter.java` | 在保留引号与注释语义的前提下拆分 SQL |
| `SqlTaskExecutionRequest.java` / `SqlTaskExecutionResult.java` | 执行输入与运行级结果 |
| `SqlTaskNodeResult.java` | 节点级成功、失败和跳过结果 |
| `SqlTaskException.java` / `SqlTaskErrorCode.java` | 带稳定错误码的执行异常 |
