# yu-ai-agent

基于 **Spring Boot 3** + **Java 21** + **Spring AI** 的 AI 应用与 ReAct 智能体项目。包含 AI 恋爱大师（多轮对话、RAG、工具调用、MCP）、自主规划智能体 **YuManus**（ReAct 工具调用 + 人机交互），以及高德地图、网页搜索、PDF 生成等工具。

## 技术栈

- **Java 21** · **Spring Boot 3.5**
- **Spring AI** · 阿里云百炼 DashScope（通义千问）
- **PgVector** 向量库 · RAG 检索增强
- **Tool Calling** · 工具调用（搜索、地图、文件、PDF、终端等）
- **MCP** · 模型上下文协议（如高德地图 MCP 服务）

## 功能概览

| 模块 | 说明 |
|------|------|
| **LoveApp** | AI 恋爱大师：多轮对话、自定义 Advisor、RAG 知识库、工具与 MCP 调用 |
| **YuManus** | ReAct 智能体：按需选择工具逐步完成任务，支持向用户提问（人机交互）与终止 |
| **工具** | 网页搜索(searchWeb)、高德地理编码与静态图、文件操作、网页抓取、资源下载、终端、PDF 生成、终止(doTerminate)、向用户提问(askUserAndWait) |
| **RAG** | PgVector 向量存储、文档加载与检索、可配合 Spring AI Advisor 做检索增强 |

## 快速开始

### 环境要求

- JDK 21
- Maven 3.6+
- PostgreSQL（含 PgVector 扩展，用于 RAG 向量库）

### 1. 克隆项目

```bash
git clone https://github.com/chenruijieCSDN/crj.git
cd crj
```

### 2. 配置本地密钥（必做）

敏感配置不提交到仓库，需在本地单独配置：

- 将 `src/main/resources/application-local.example.yml` **复制为** `application-local.yml`（与 `application.yml` 同目录）。
- 在 `application-local.yml` 中填入你的密钥与数据库信息：

```yaml
spring:
  ai:
    dashscope:
      api-key: 你的百炼/通义 API Key
  datasource:
    url: jdbc:postgresql://你的主机:5432/你的数据库
    username: 你的用户名
    password: 你的密码

search-api:
  api-key: 你的 SearchAPI.io Key   # 用于 searchWeb 百度搜索

amap:
  web-api-key: 你的高德 Web 服务 Key   # 用于地理编码与静态图
```

也可通过环境变量配置（与 `application.yml` 中的占位一致）：  
`DASHSCOPE_API_KEY`、`DATASOURCE_URL`、`DATASOURCE_USERNAME`、`DATASOURCE_PASSWORD`、`SEARCH_API_KEY`、`AMAP_WEB_API_KEY`。

### 3. 启动应用

默认激活 `local` profile，会加载 `application-local.yml`：

```bash
mvn spring-boot:run
```

或 IDE 中运行 `YuAiAgentApplication`，并保证使用 `local` profile。

- 服务端口：**8123**
- 上下文路径：**/api**
- 接口文档（Knife4j）：`http://localhost:8123/api/doc.html`

## 主要接口

| 接口 | 说明 |
|------|------|
| `GET /api/manus/run?query=任务描述` | 使用 YuManus 执行一次 ReAct 任务（多步工具调用） |
| `GET /api/user/input?input=用户回复` | 当 YuManus 通过 askUserAndWait 向用户提问时，前端调用此接口传入用户输入 |
| 其他 | 见 Knife4j `/api/doc.html`（恋爱大师对话、RAG、文档处理等） |

## 运行测试

- 单元测试需激活 profile **test**，并保证有可用配置（如数据库、DashScope Key）。
- YuManus 测试会使用不包含「向用户提问」工具的工具集，避免阻塞。  
  IDE 运行测试时，若需指定 profile，可在运行配置的 VM 选项中添加：  
  `-Dspring.profiles.active=local,test`

```bash
mvn test -Dtest=YuManusTest#run
```

## 项目结构（简要）

```
src/main/java/com/yupi/yuaiagent/
├── YuAiAgentApplication.java      # 启动类
├── agent/                          # ReAct 智能体
│   ├── BaseAgent.java
│   ├── ReActAgent.java
│   ├── ToolCallAgent.java
│   ├── YuManus.java
│   ├── UserInputQueue.java
│   └── ...
├── app/                            # LoveApp 恋爱大师
├── controller/                     # Manus、健康检查、文档处理等
├── tools/                          # 各类工具（搜索、地图、PDF、用户提问等）
├── rag/                            # RAG 与向量库
├── advisor/                        # 自定义 Advisor
└── demo/                           # 大模型调用示例
```

## 参考

- [Spring AI](https://docs.spring.io/spring-ai/reference/)
- [阿里云百炼 / 灵积 DashScope](https://help.aliyun.com/zh/dashscope/)
- [编程导航 · 智能体 Manus 与人机交互](https://www.codefather.cn/post/1928476155422310402)

## License

本项目仅供学习与参考使用。
