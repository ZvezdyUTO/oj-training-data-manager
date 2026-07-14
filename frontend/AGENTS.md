# Frontend Agent Notes

这是 OJ Training Data Manager 的独立 Vue 3 前端，不属于 Blog，也不共享路由、会话或身份。

## 规则

- 应用挂载于根路径，浏览器 API 使用统一 API 前缀。
- 多人、单人、题目、成员和采集任务 GET 全部公开。
- 创建、更新、删除成员和启动采集必须要求全局操作密码，并仅在当前请求的 X-Operation-Password 中发送。
- 禁止把操作密码写入 localStorage、sessionStorage、URL、日志或长期组件状态。
- 不引入 JWT、登录页、账号角色、nickname、email、avatar、文章、分类或首页图片。
- 继续使用 username 作为唯一业务身份。
- 支持 1280–2560 px 桌面，主要检查 1440×900 和 1920×1080。

## 验证

    pnpm lint
    pnpm test
    pnpm typecheck
    pnpm build
