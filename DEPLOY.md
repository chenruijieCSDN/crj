# 服务器部署说明

本文档说明如何将 yu-ai-agent 前后端部署到 Linux 服务器上（**腾讯云 CVM / 轻量应用服务器** 等均适用），通过浏览器访问。

## 一、部署方式概览

| 方式 | 说明 |
|------|------|
| **宝塔 + Docker（推荐，有图形界面）** | 用宝塔装 Docker、传文件、配 Nginx 反代和 SSL，运维更省心，适合腾讯云等 |
| Docker 纯命令行 | 一条命令构建并运行，无需在服务器装 JDK/Node |
| jar + Nginx | 本地打包带前端的 jar，上传后用 systemd 守护，Nginx 反代并配 HTTPS |
| 简化版 | 仅运行 jar，直接访问 `http://服务器IP:8123/api/` |

---

## 二、宝塔面板 + Docker 部署（推荐，有图形界面）

用**宝塔**可以在一台腾讯云服务器上完成：安装 Docker、上传/构建项目、Nginx 反代、一键 SSL，全部在网页里操作，比纯命令行更省心。

### 2.1 安装宝塔（若尚未安装）

腾讯云轻量 / CVM 选 Ubuntu 或 CentOS，SSH 登录后执行（以 Ubuntu 为例）：

```bash
wget -O install.sh https://download.bt.cn/install/install-ubuntu_6.0.sh && sudo bash install.sh
```

按提示记下**面板地址、用户名、密码**，并在腾讯云安全组放行 **8888**（宝塔面板端口）。装好后用浏览器打开面板并安装 **Nginx**、**Docker 管理器**（在「软件商店」里）。

### 2.2 用宝塔安装并启用 Docker

- 软件商店 → 搜索 **Docker** → 安装「Docker 管理器」或系统自带的 Docker。
- 若使用「Docker 管理器」插件，安装后可在宝塔里看到容器/镜像；也可用 SSH 终端执行 `docker` 命令。

### 2.3 上传项目并构建镜像

- **文件**：在宝塔「文件」里进入合适目录（如 `/www/wwwroot/yu-ai-agent`），上传项目压缩包并解压（或本机 `git clone` 后打包上传），确保含有 `Dockerfile`、`docker-compose.yml`、`yu-ai-agent-frontend/`、`pom.xml`、`src/`。
- **终端**：在宝塔「终端」中执行：

```bash
cd /www/wwwroot/yu-ai-agent
echo "DASHSCOPE_API_KEY=你的阿里云百炼密钥" > .env
docker compose build
docker compose up -d
```

或先在本机/CI 构建好镜像并导出为 `yu-ai-agent.tar`，上传到服务器后：`docker load -i yu-ai-agent.tar`，再在宝塔 Docker 管理器中「从镜像创建容器」：映射端口 8123，环境变量填 `DASHSCOPE_API_KEY=xxx`，重启策略选「始终重启」。

### 2.4 在宝塔里添加 Nginx 站点（可选，用于域名 + HTTPS）

若希望用**域名**访问并开启 **HTTPS**：

1. 网站 → 添加站点：域名填你的域名（如 `ai.example.com`），根目录可随意（反代时不使用）。
2. 设置 → 反向代理 → 添加反向代理：
   - 代理名称：`yu-ai-agent`
   - 目标 URL：`http://127.0.0.1:8123`
   - 发送域名：`$host`
   - 高级里可增加：`proxy_read_timeout 300s;`（避免 SSE 断连）
3. 保存后，在「SSL」里选 Let’s Encrypt 申请证书并强制 HTTPS。

访问：**https://你的域名/api/**（若反代配置的是根路径，且后端 context-path 为 `/api`，则必须带 `/api/`）。

若不用域名，只通过 IP 访问：在腾讯云安全组放行 **8123**，浏览器打开 **http://服务器IP:8123/api/** 即可。

### 2.5 小结（宝塔流程）

| 步骤 | 操作 |
|------|------|
| 1 | 装宝塔 → 装 Nginx、Docker（或 Docker 管理器） |
| 2 | 上传项目到服务器 → 终端里 `docker compose build && docker compose up -d`，或导入已构建镜像 |
| 3 | 可选：添加站点 → 反向代理到 `127.0.0.1:8123` → 申请 SSL |
| 4 | 访问 `http(s)://域名或IP/api/` |

---

## 三、Docker 部署（纯命令行）

服务器只需安装 **Docker** 和 **Docker Compose**，无需单独安装 JDK、Node。

### 3.1 安装 Docker（腾讯云 Ubuntu 示例）

```bash
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER
# 重新登录后生效；或当前终端：newgrp docker
sudo apt install docker-compose-plugin -y   # 或 docker-compose 独立安装
```

### 3.2 上传代码并构建镜像

将项目根目录（含 `Dockerfile`、`docker-compose.yml`、`yu-ai-agent-frontend/`、`pom.xml`、`src/`）上传到服务器，例如 `/home/ubuntu/yu-ai-agent`，然后：

```bash
cd /home/ubuntu/yu-ai-agent
docker compose build
```

或仅构建镜像（不用 compose）：

```bash
docker build -t yu-ai-agent .
```

### 3.3 运行容器

**方式 A：docker compose（推荐）**

在项目目录下创建 `.env` 文件，写入密钥（不提交到 Git）：

```bash
echo "DASHSCOPE_API_KEY=你的阿里云百炼密钥" > .env
# 可选：SEARCH_API_KEY=xxx、AMAP_WEB_API_KEY=xxx
```

启动：

```bash
docker compose up -d
```

**方式 B：docker run**

```bash
docker run -d \
  --name yu-ai-agent \
  -p 8123:8123 \
  -e DASHSCOPE_API_KEY=你的密钥 \
  -e SEARCH_API_KEY=可选 \
  -e AMAP_WEB_API_KEY=可选 \
  --restart unless-stopped \
  yu-ai-agent
```

### 3.4 访问与常用命令

- 浏览器访问：**http://服务器公网IP:8123/api/**
- 查看日志：`docker compose logs -f` 或 `docker logs -f yu-ai-agent`
- 停止：`docker compose down` 或 `docker stop yu-ai-agent`

### 3.5 腾讯云安全组

若无法访问，请在腾讯云控制台为实例**开放 8123 端口**（入站规则）：协议 TCP，端口 8123，来源 0.0.0.0/0（或按需限制）。若前面再套 Nginx 用 80/443，则只需开放 80、443，Nginx 反代到本机 8123。

---

## 四、非 Docker：服务器要求

- **系统**：Linux（如 Ubuntu 22.04、CentOS 7+）
- **JDK 21**（仅运行 jar，无需 Maven/Node）
- **内存**：建议 ≥1GB
- **可选**：Nginx（若需 HTTPS 或 80/443 端口）

---

## 五、非 Docker：在本地或 CI 打包

在**有 Node 和 Maven 的环境**（本机或 CI）打包出**带前端的 jar**：

```bash
# 1. 前端构建
cd yu-ai-agent-frontend
npm install
npm run build

# 2. 后端 + 前端一起打包
cd ..
mvn -DskipTests package -Pwith-frontend
```

得到：`target/yu-ai-agent-0.0.1-SNAPSHOT.jar`（内已含前端静态资源）。

将**该 jar** 上传到服务器，例如：`/opt/yu-ai-agent/app.jar`。

---

## 六、非 Docker：服务器上的配置与运行

### 6.1 安装 JDK 21（若未安装）

```bash
# Ubuntu / Debian 示例
sudo apt update
sudo apt install openjdk-21-jdk -y
java -version
```

### 6.2 环境变量（必填与可选）

建议用环境变量存放密钥，不写进配置文件。**必填**：

```bash
export DASHSCOPE_API_KEY=你的阿里云百炼API密钥
```

**可选**（不配置则对应功能不可用或使用默认）：

```bash
export SEARCH_API_KEY=你的SearchAPI密钥      # 超级智能体网页搜索
export AMAP_WEB_API_KEY=你的高德Web服务Key   # 地图与静态图
# 若使用 PostgreSQL 做 RAG：
# export DATASOURCE_URL=jdbc:postgresql://主机:5432/库名
# export DATASOURCE_USERNAME=用户
# export DATASOURCE_PASSWORD=密码
```

无数据库时使用内置的 `no-db` profile，无需配置数据库。

### 6.3 直接运行 jar（简化版）

```bash
cd /opt/yu-ai-agent
export DASHSCOPE_API_KEY=sk-xxx
java -jar app.jar --spring.profiles.active=local,no-db
```

浏览器访问：`http://服务器IP:8123/api/`。

### 6.4 使用 systemd 守护（推荐）

创建服务文件：

```bash
sudo vim /etc/systemd/system/yu-ai-agent.service
```

内容示例（路径、用户按需修改）：

```ini
[Unit]
Description=Yu AI Agent
After=network.target

[Service]
Type=simple
User=www-data
WorkingDirectory=/opt/yu-ai-agent

# 环境变量：至少配置 DASHSCOPE_API_KEY
Environment="DASHSCOPE_API_KEY=你的密钥"
Environment="SEARCH_API_KEY=可选"
Environment="AMAP_WEB_API_KEY=可选"

ExecStart=/usr/bin/java -jar /opt/yu-ai-agent/app.jar --spring.profiles.active=local,no-db
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

启用并启动：

```bash
sudo systemctl daemon-reload
sudo systemctl enable yu-ai-agent
sudo systemctl start yu-ai-agent
sudo systemctl status yu-ai-agent
```

日志：`journalctl -u yu-ai-agent -f`。

---

## 六、Nginx 反向代理（HTTPS + 80/443）

若希望通过 **80/443** 访问，并配置 **HTTPS**，可在本机装 Nginx，反向代理到后端 jar。

### 7.1 安装 Nginx

```bash
sudo apt install nginx -y
```

### 7.2 配置站点（HTTP 示例）

```bash
sudo vim /etc/nginx/sites-available/yu-ai-agent
```

示例（替换 `your_domain_or_ip` 为你的域名或服务器 IP）：

```nginx
server {
    listen 80;
    server_name your_domain_or_ip;

    location /api/ {
        proxy_pass http://127.0.0.1:8123;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 300s;
        proxy_connect_timeout 75s;
    }
}
```

启用并重载：

```bash
sudo ln -s /etc/nginx/sites-available/yu-ai-agent /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

访问：`http://your_domain_or_ip/api/`。

### 7.3 配置 HTTPS（可选）

使用 Let’s Encrypt（需已解析到该服务器的域名）：

```bash
sudo apt install certbot python3-certbot-nginx -y
sudo certbot --nginx -d your_domain.com
```

按提示选择为 `your_domain.com` 申请证书并自动配置 Nginx。之后用：`https://your_domain.com/api/`。

---

## 八、端口与路径说明

| 项目 | 默认值 | 说明 |
|------|--------|------|
| 后端端口 | 8123 | 可在启动参数中覆盖：`--server.port=8080` |
| 上下文路径 | /api | 前端与接口均在 `/api` 下，如 `/api/` 首页、`/api/ai/manus/chat` |
| 前端首页 | /api/ | 由 jar 内静态资源 + SPA 回退提供 |

---

## 九、常见问题

- **Docker 构建失败**：确保在**项目根目录**执行 `docker compose build`（含 `Dockerfile`、`yu-ai-agent-frontend/`、`pom.xml`、`src/`）。构建较久属正常（需下载 Node、Maven 镜像并编译前后端）。
- **前端白屏 / 接口 404**：确认访问地址带 `/api/`（如 `http://IP:8123/api/`）。Docker 与 jar 部署均相同。
- **SSE 断连**：Nginx 需保留较长 `proxy_read_timeout`（示例中为 300s）；若仍断，可适当调大。
- **密钥不生效**：Docker 用 `-e DASHSCOPE_API_KEY=xxx` 或 `.env`；systemd 用 `Environment=`。确认变量在运行进程内生效。
- **腾讯云访问不了**：在安全组中放行 8123（或 Nginx 的 80/443）。

部署完成后，在浏览器打开 **http(s)://你的域名或IP/api/** 即可使用恋爱大师与超级智能体。
