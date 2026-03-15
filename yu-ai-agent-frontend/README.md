# yu-ai-agent 前端

Vue 3 + Vite + Axios，对接 yu-ai-agent 后端 SSE 接口。

## 功能

- **主页**：切换「AI 恋爱大师」与「AI 超级智能体」
- **AI 恋爱大师**：聊天室风格，进入页面自动生成会话 ID，通过 SSE（GET `/api/ai/love_app/chat/sse`）实时显示回复
- **AI 超级智能体**：同上，通过 SSE（POST `/api/ai/manus/chat`）实时显示回复

## 开发

```bash
cd yu-ai-agent-frontend
npm install
npm run dev
```

前端默认运行在 `http://localhost:5173`，接口通过 Vite 代理到 `http://localhost:8123`（见 `vite.config.js`）。请先启动后端。

## 环境变量（可选）

- `VITE_API_BASE`：接口根路径，默认空（使用相对路径 `/api`，走代理）。若前端直接连后端可设为 `http://localhost:8123`。

## 构建

```bash
npm run build
npm run preview
```
