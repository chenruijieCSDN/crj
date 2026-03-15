# Yu AI Agent

基于 **Spring AI** 与 **阿里云百炼（DashScope）** 的 AI 智能体应用：提供「恋爱大师」RAG 对话与「超级智能体」ReAct 工具调用（网页搜索、地图、PDF 生成、人机交互等），并配有 Vue3 前端。

## 功能概览

| 模块 | 说明 |
|------|------|
| **恋爱大师** | 基于 RAG 的恋爱/情感问答，支持同步、SSE 流式、多轮对话（可选 PostgreSQL + pgvector 或内存向量库） |
| **超级智能体（YuManus）** | ReAct 多步推理 + 工具调用：搜索、地图、PDF、文件、网页抓取、终端、向用户提问、结束任务等，支持 SSE 按步推送 |
| **前端** | Vue3 + Vite，恋爱大师聊天页 + 超级智能体聊天页，支持流式展示、地图图片、PDF 下载链接 |

## 技术栈

- **后端**：Java 21、Spring Boot 3.5、Spring AI 1.1、Spring AI Alibaba（DashScope）
- **前端**：Vue 3、Vue Router、Vite 5、Axios
- **可选**：PostgreSQL + pgvector（RAG 向量库）、SearchAPI.io（网页搜索）、高德 Web 服务（地图与静态图）

## 项目结构

```
yu-ai-agent/
├── src/main/java/com/yupi/yuaiagent/
│   ├── YuAiAgentApplication.java          # 启动类
│   ├── agent/                              # 智能体
│   │   ├── BaseAgent.java                  # 抽象基类（步骤循环、流式、超时）
│   │   ├── ReActAgent.java                 # ReAct think/act 抽象
│   │   ├── ToolCallAgent.java              # 工具调用实现（格式化前端输出）
│   │   ├── YuManus.java                    # 超级智能体（系统提示、maxSteps=20）
│   │   └── UserInputQueue.java             # 人机交互输入队列
│   ├── app/
│   │   └── LoveApp.java                    # 恋爱大师应用（RAG + ChatClient）
│   ├── controller/
│   │   ├── AiController.java               # /ai/love_app/*、/ai/manus/chat
│   │   ├── ManusController.java            # /user/input、/manus/run
│   │   ├── FileController.java            # /files/pdf 下载
│   │   ├── DocumentProcessingController.java
│   │   └── HealthController.java
│   ├── tools/                              # 智能体工具
│   │   ├── ToolRegistration.java           # 注册所有 ToolCallback
│   │   ├── WebSearchTool.java              # searchWeb（SearchAPI.io，带超时）
│   │   ├── AmapTool.java                   # getMapAddressAndImage
│   │   ├── PDFGenerationTool.java          # generatePDF
│   │   ├── QueryUserTool.java              # askUserAndWait
│   │   ├── TerminateTool.java              # doTerminate
│   │   ├── FileOperationTool.java          # readFile / writeFile
│   │   ├── WebScrapingTool.java            # scrapeWebPage
│   │   ├── ResourceDownloadTool.java       # downloadResource
│   │   └── TerminalOperationTool.java      # executeTerminalCommand
│   └── rag/                                 # RAG 配置（向量检索、过滤）
├── src/main/resources/
│   ├── application.yml                     # 主配置（端口 8123、context-path /api）
│   ├── application-local.example.yml       # 本地配置示例
│   └── application-no-db.yml              # 无数据库 profile
├── yu-ai-agent-frontend/                    # Vue3 前端
│   ├── src/
│   │   ├── App.vue
│   │   ├── views/
│   │   │   ├── LoveAppChat.vue             # 恋爱大师聊天
│   │   │   └── ManusChat.vue               # 超级智能体聊天（SSE、PDF 下载）
│   │   └── api/index.js                    # API baseURL、getApiBase
│   ├── vite.config.js                      # 开发代理 /api -> localhost:8123
│   └── package.json
└── pom.xml
```

## 环境要求

- **JDK 21**
- **Maven 3.6+**
- **Node.js 18+**（仅前端开发时需要）
- 可选：**PostgreSQL**（使用 pgvector 做 RAG 时）

## 配置

1. 复制本地配置模板并填入密钥：

   ```bash
   cp src/main/resources/application-local.example.yml src/main/resources/application-local.yml
   ```

2. 在 `application-local.yml` 或环境变量中配置：

   | 配置项 | 说明 |
   |--------|------|
   | `spring.ai.dashscope.api-key` / `DASHSCOPE_API_KEY` | 阿里云百炼 API Key（必填） |
   | `search-api.api-key` / `SEARCH_API_KEY` | [SearchAPI.io](https://www.searchapi.io/) 网页搜索 Key（超级智能体 searchWeb 需要） |
   | `amap.web-api-key` / `AMAP_WEB_API_KEY` | 高德 Web 服务 Key（智能体 getMapAddressAndImage 需要） |
   | `spring.datasource.*` | 使用 pgvector 做 RAG 时配置数据源 |

3. 无数据库运行：在 `application.yml` 中保持 `spring.profiles.active: local,no-db`，或启动时加上 `--spring.profiles.active=local,no-db`。此时 RAG 使用内存向量库，无需 PostgreSQL。

## 运行

### 后端

```bash
# 使用 local + no-db（不连 PostgreSQL）
mvn spring-boot:run -Dspring-boot.run.profiles=local,no-db

# 或使用 local（需配置 datasource）
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

服务启动后：**http://localhost:8123/api**（context-path 为 `/api`）。

### 前端

```bash
cd yu-ai-agent-frontend
npm install
npm run dev
```

浏览器访问 **http://localhost:5173**。前端通过 Vite 代理将 `/api` 转发到 `http://localhost:8123`，无需跨域。

### 生产构建

```bash
# 仅后端
mvn -DskipTests package

# 后端 + 前端（打包含前端的 jar，部署服务器时只需运行该 jar）
cd yu-ai-agent-frontend && npm run build && cd .. && mvn -DskipTests package -Pwith-frontend
```

**服务器部署**（前后端一体、Nginx、HTTPS、systemd）见 **[DEPLOY.md](DEPLOY.md)**。

## 主要 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET/POST | `/api/ai/manus/chat` | 超级智能体流式对话（SSE），POST 推荐，body `{"message":"任务描述"}` |
| GET | `/api/ai/manus/run?query=...` | 超级智能体同步执行，返回全文 |
| GET | `/api/user/input?input=...` | 人机交互：智能体 askUserAndWait 时前端传入用户输入 |
| GET | `/api/ai/love_app/chat/sync` | 恋爱大师同步对话 |
| GET | `/api/ai/love_app/chat/sse` | 恋爱大师 SSE 流式 |
| GET | `/api/files/pdf?name=xxx.pdf` | 下载智能体生成的 PDF |
| GET | `/api/health` | 健康检查 |

接口文档（Swagger/Knife4j）：**http://localhost:8123/api/doc.html**（需先启动后端）。

## 超级智能体工具一览

| 工具名 | 说明 |
|--------|------|
| `searchWeb` | 百度网页搜索（SearchAPI.io），结果供模型参考，前端展示简短提示 |
| `getMapAddressAndImage` | 高德地理编码 + 静态地图图片链接 |
| `generatePDF` | 根据正文生成 PDF，前端可解析并展示「下载 PDF」链接 |
| `askUserAndWait` | 向用户提问并等待输入，需配合 `/user/input` 使用 |
| `doTerminate` | 结束当前任务 |
| `readFile` / `writeFile` | 本地文件读写 |
| `scrapeWebPage` | 网页内容抓取 |
| `downloadResource` | 从 URL 下载资源 |
| `executeTerminalCommand` | 执行终端命令（慎用） |

后端对 `searchWeb`、`generatePDF` 等大段输出做了缩短处理，避免前端刷屏；单步执行带 120 秒超时，避免长时间无响应。

## 参考与致谢

- [Spring AI](https://docs.spring.io/spring-ai/reference/)
- [Spring AI Alibaba](https://github.com/alibaba/spring-ai-alibaba)
- [智能体 Manus 改进——人机交互](https://www.codefather.cn/post/1928476155422310402)

## 许可证

见项目根目录 LICENSE 文件（如有）。
