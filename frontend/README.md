# Frontend

独立的 Vue 3 + Vite 训练数据工作台。它直接服务于根路径，通过同一 Nginx 的 API 路径访问独立 Training API。

## 职责

- 公开展示多人统计、单人提交与首 AC、题目提交与首 AC。
- 公开展示成员、OJ handle、采集游标和采集任务。
- 创建、修改、删除成员以及启动采集时，弹出一次性操作密码确认框。
- 首次采集成员的倒退小时固定显示为不可编辑的 `0`；混合批次会按 username 分别提交 `0` 或统一/成员配置值。
- 操作密码仅存在于确认框和单次请求调用栈中，通过 X-Operation-Password 发送；不写入 Web Storage。
- 复用原 Training 工作台的页面结构和视觉样式；多人、单人、题目、成员和采集入口直接平铺在 52px 黑色顶栏，不使用下拉菜单。

## 目录

- src/api：公开查询、成员写操作和采集任务 adapter。
- src/components：顶层外壳、查询、成员、采集、分页与密码确认组件。
- src/composables：训练查询状态和请求编排。
- src/router：独立应用路由。
- src/styles：原 Training 工作台样式及独立顶栏、密码框的最小适配。
- src/views：路由级页面。
- src/test：公开请求及操作密码合同测试。
- Dockerfile、nginx.conf：静态构建、SPA fallback 和 API 反向代理。
- .dockerignore：排除本地依赖、构建产物和环境文件，保持镜像上下文精简。

## 依赖与边界

- 不依赖 Blog、JWT、账号、角色或登录状态。
- GET 查询不得附带操作密码。
- POST、PUT、DELETE 必须由调用方显式传入当次操作密码。
- 组件不得绕过 src/api 自行拼接业务请求。

## 文件职责

| 文件 | 职责 |
| --- | --- |
| src/App.vue | 独立应用组合 |
| src/theme.ts | 独立主题存储、日夜模式应用与跨标签同步 |
| src/router/index.ts | 查询、成员与采集路由 |
| src/api/client.ts | Result envelope 与统一错误 |
| src/api/training.ts | 七类公开训练查询 |
| src/api/members.ts | 公开成员查询及带密码的成员写操作 |
| src/api/collection.ts | 公开任务查询及带密码的采集启动 |
| src/utils/collectionLookback.ts | 首次采集归零、非负整数校验和混合批次 lookback 生成 |
| src/composables/useTrainingDashboard.ts | 查询状态、并发与分页 |
| src/components/OperationPasswordDialog.vue | 一次性操作密码输入和清理 |
| src/components/TrainingQueryPanel.vue | 多人、单人、题目查询工作台 |
| src/components/MembersPanel.vue | 成员创建、修改和删除 |
| src/components/CollectionPanel.vue | 手动采集和任务历史 |
| src/components/AppShell.vue | 52px 黑色顶栏及五个平铺路由入口 |
| src/styles.css | 原 Training 样式入口 |
| src/styles/foundation.css | 基础控件、尺寸和可访问性规则 |
| src/styles/theme.css | Blog/Training 共用浅色视觉变量 |
| src/styles/shell.css | 黑色顶栏、正文宽度与外壳布局 |
| src/styles/dashboard.css | 查询、成员、采集工作台布局 |
| src/styles/table.css | 统计、成员与任务表格规则 |
| src/styles/side-panel.css | 管理交互及提示组件规则 |
| src/styles/dark.css | 原 Training 深夜主题 |
| src/styles/standalone.css | 独立项目导航平铺、username-only 和操作密码最小适配 |
| src/test/restored-ui-contract.test.ts | 黑色顶栏、原查询 DOM 与样式 token 回归合同 |
| src/test/collection-lookback.test.ts | 首次、增量与混合批次 lookback 策略测试 |

## 本地验证

执行 pnpm install 后运行：

    pnpm lint
    pnpm test
    pnpm typecheck
    pnpm build

开发服务器默认把 API 路径代理到 http://localhost:8190，可通过 VITE_DEV_API_TARGET 修改。
