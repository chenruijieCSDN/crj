# 部署操作指南：本机上传 vs 从 GitHub 部署

两种常用方式：**本机直接上传到服务器**、**服务器从 GitHub 拉取**。任选一种，按步骤执行即可。

## ⚠️ 白屏问题解决方案

如果遇到前端白屏问题，请按以下步骤处理：

### 快速修复方法

1. **修改前端路由模式**（推荐）：
   编辑 `yu-ai-agent-frontend/src/router/index.js`：
   ```javascript
   import { createRouter, createWebHashHistory } from 'vue-router'

   const router = createRouter({
     history: createWebHashHistory(),  // 改为 hash 模式
     routes,
   })
   ```

2. **重新构建并部署**：
   ```bash
   docker compose build --no-cache
   docker compose up -d
   ```

### 其他解决方案

详见文档末尾的【白屏问题详细解决方案】章节

---

## 一、本机上传到服务器并部署

适合：服务器访问 GitHub 不稳定，或希望用本机最新未推送代码部署。

### 1.1 本机（Windows PowerShell）

**（1）打包项目（排除 node_modules、target、.git）**

在项目根目录 `D:\code\yu-ai-agent` 执行：

```powershell
cd D:\code\yu-ai-agent
# 用 tar 或 7z 打包（PowerShell 自带的 Compress-Archive 会包含 .git，建议用下面方式）
# 若已安装 7-Zip 或 tar：
tar --exclude=node_modules --exclude=target --exclude=.git -cvf yu-ai-agent.tar .
# 若无 tar，可用：Compress-Archive -Path * -DestinationPath ..\yu-ai-agent.zip -Force
```

若本机没有 `tar`，可用宝塔/资源管理器手动打包：选中除 `node_modules`、`target`、`.git` 外的所有文件，打成 `yu-ai-agent.zip`。

**（2）上传到服务器**

```powershell
# 把 你的服务器IP 换成实际 IP，root 换成你的 SSH 用户（如 ubuntu）
scp yu-ai-agent.tar root@你的服务器IP:/www/wwwroot/
```

或使用 **宝塔「文件」**：本机把 `yu-ai-agent.zip` 上传到 `/www/wwwroot/`，在宝塔里解压到 `yu-ai-agent` 目录（覆盖或新建）。

**（3）在服务器上解压、配置、构建、启动**

SSH 登录服务器后执行（若用 zip 上传则把 `tar -xvf` 换成 `unzip -o yu-ai-agent.zip -d yu-ai-agent`）：

```bash
cd /www/wwwroot
mkdir -p yu-ai-agent
tar -xvf yu-ai-agent.tar -C yu-ai-agent
cd yu-ai-agent
# 若服务器上已有 .env 且不想覆盖，可跳过下一行；否则写入密钥
echo "DASHSCOPE_API_KEY=你的阿里云百炼密钥" > .env
docker compose build --no-cache
docker compose up -d
```

---

### 1.2 一键脚本思路（本机上传 + 服务器部署）

**本机执行（PowerShell，需先设好 `$SERVER`、`$USER`）：**

```powershell
$SERVER = "你的服务器IP"
$USER = "root"
cd D:\code\yu-ai-agent
tar --exclude=node_modules --exclude=target --exclude=.git -cvf yu-ai-agent.tar .
scp yu-ai-agent.tar ${USER}@${SERVER}:/www/wwwroot/
ssh ${USER}@${SERVER} "cd /www/wwwroot && mkdir -p yu-ai-agent && tar -xvf yu-ai-agent.tar -C yu-ai-agent && cd yu-ai-agent && echo 'DASHSCOPE_API_KEY=你的密钥' > .env && docker compose build --no-cache && docker compose up -d"
```

（若没有 `tar`，本机用压缩软件打 zip，上传后 SSH 里用 `unzip` 解压再执行 `cd yu-ai-agent && ...`。）

---

## 二、从 GitHub 到服务器并部署

适合：代码已推送到 GitHub，服务器能访问 GitHub（或可接受首次 clone 后改文件不拉取）。

### 2.1 首次部署（clone + 构建 + 启动）

SSH 登录服务器后执行：

```bash
cd /www/wwwroot
git clone https://github.com/chenruijieCSDN/crj.git yu-ai-agent
cd yu-ai-agent
echo "DASHSCOPE_API_KEY=你的阿里云百炼密钥" > .env
# 若 Dockerfile 里是 npm ci --omit=dev，需先改成本机已修复的版本（安装 dev 依赖）：
sed -i 's/npm ci --omit=dev/npm ci/' Dockerfile
docker compose build --no-cache
docker compose up -d
```

若 `git clone` 超时（国内服务器常见），见下方 **2.3 服务器访问不了 GitHub 时**。

---

### 2.2 后续更新（pull + 重新构建）

代码在 GitHub 更新后，在服务器上拉取并重新构建、启动：

```bash
cd /www/wwwroot/yu-ai-agent
git pull origin master
docker compose build --no-cache
docker compose up -d
```

若 `git pull` 失败（连接超时），用 **2.3** 或改用 **本机上传** 方式更新。

---

### 2.3 服务器访问不了 GitHub 时

- **不改代码只修构建**：不执行 `git pull`，在服务器上直接改 Dockerfile 后构建：
  ```bash
  cd /www/wwwroot/yu-ai-agent
  sed -i 's/npm ci --omit=dev/npm ci/' Dockerfile
  docker compose build --no-cache
  docker compose up -d
  ```
- **要同步最新代码**：在本机 `git push` 后，用 **本机上传**（一）把项目打包上传到服务器覆盖，再在服务器执行：
  ```bash
  cd /www/wwwroot/yu-ai-agent
  docker compose build --no-cache
  docker compose up -d
  ```

---

## 三、部署后检查与访问

- 查看容器：`docker ps | grep yu-ai-agent`
- 查看日志：`docker compose -f /www/wwwroot/yu-ai-agent/docker-compose.yml logs -f`
- 腾讯云安全组放行 **8123**
- 浏览器访问：**http://你的服务器IP:8123/api/**

---

## 四、对比小结

| 方式           | 优点                     | 适用场景                         |
|----------------|--------------------------|----------------------------------|
| 本机上传部署   | 不依赖 GitHub，用本机最新代码 | 服务器无法访问 GitHub、或未推送  |
| 从 GitHub 部署 | 流程简单，易自动化       | 服务器能访问 GitHub、代码已推送  |

两种方式最终都是在服务器上执行 `docker compose build` 与 `docker compose up -d`，仅代码来源不同（本机打包 vs git clone/pull）。

---

## 五、可选：服务器上一键脚本

在服务器上可把下面两段保存为脚本，按需执行。

**（1）本机已上传代码到 `/www/wwwroot/yu-ai-agent` 时执行**

保存为 `deploy-uploaded.sh`，执行前确保已上传项目并写好 `.env`：

```bash
#!/bin/bash
set -e
cd /www/wwwroot/yu-ai-agent
[ -f .env ] || { echo "请先创建 .env 并写入 DASHSCOPE_API_KEY=xxx"; exit 1; }
docker compose build --no-cache
docker compose up -d
echo "部署完成，访问 http://$(curl -s ifconfig.me 2>/dev/null || echo 你的IP):8123/api/"
```

**（2）从 GitHub 克隆并部署（含无法访问 GitHub 时的 Dockerfile 修复）**

保存为 `deploy-from-github.sh`，首次或更新时执行（需把 `你的密钥` 和仓库地址换成实际值）：

```bash
#!/bin/bash
set -e
REPO="${REPO:-https://github.com/chenruijieCSDN/crj.git}"
DIR="/www/wwwroot/yu-ai-agent"
KEY="${DASHSCOPE_API_KEY:-你的密钥}"

if [ ! -d "$DIR/.git" ]; then
  echo "首次部署：克隆仓库..."
  git clone "$REPO" "$DIR"
  cd "$DIR"
  sed -i 's/npm ci --omit=dev/npm ci/' Dockerfile
else
  echo "更新：拉取最新代码..."
  cd "$DIR"
  git pull origin master || true
  sed -i 's/npm ci --omit=dev/npm ci/' Dockerfile
fi
echo "DASHSCOPE_API_KEY=$KEY" > .env
docker compose build --no-cache
docker compose up -d
echo "部署完成。"

---

## 白屏问题详细解决方案

### 问题现象
- 访问页面显示空白
- 浏览器控制台有 404 错误
- 刷新页面后出现 404

### 根本原因
1. **Vue Router 历史模式问题**：使用 `createWebHistory` 在生产环境需要服务器配置
2. **资源路径错误**：前端构建的 base URL 与后端 context-path 冲突
3. **静态资源加载失败**：CSS/JS 文件路径不正确

### 解决方案

#### 方案1：使用 Hash 路由模式（已采用，最简单）
修改 `yu-ai-agent-frontend/src/router/index.js`：
```javascript
import { createRouter, createWebHashHistory } from 'vue-router'

const router = createRouter({
  history: createWebHashHistory(import.meta.env.BASE_URL),  // 与 Vite base /api/ 一致
  routes,
})
```
访问地址：**http://你的IP:8123/api/#/** 或 **http://你的IP:8123/api/**（会由前端处理）。

#### 方案2：修改前端构建配置
修改 `yu-ai-agent-frontend/vite.config.js`：
```javascript
export default defineConfig({
  plugins: [vue()],
  base: './',  // 使用相对路径
  build: {
    outDir: 'dist',
    assetsDir: 'assets'
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8123',
        changeOrigin: true,
      },
    },
  },
})
```

#### 方案3：Spring Boot 配置支持 SPA
在 `application.yml` 中添加：
```yaml
spring:
  web:
    resources:
      static-locations: classpath:/static/
      add-mappings: true

# 配置错误页面，将所有 404 重定向到 index.html
server:
  error:
    whitelabel:
      enabled: false
    path: /error
```

#### 方案4：Nginx 配置（如果使用 Nginx 反向代理）
```nginx
server {
    listen 80;
    server_name your-domain.com;

    location / {
        proxy_pass http://localhost:8123/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;

        # 处理 Vue Router 历史模式
        error_page 404 =200 @fallback;
    }

    location @fallback {
        rewrite ^.*$ /api/index.html break;
        proxy_pass http://localhost:8123;
    }
}
```

### 验证步骤
1. 重新构建镜像：`docker compose build --no-cache`
2. 启动容器：`docker compose up -d`
3. 检查日志：`docker compose logs -f`
4. 浏览器访问并检查控制台
5. 测试刷新页面是否正常

### 调试技巧
- 查看浏览器 Network 标签，确认资源是否成功加载
- 检查容器内的静态文件：`docker exec -it yu-ai-agent ls -la /app/static/`
- 验证 API 接口：`curl http://localhost:8123/api/actuator/health`
```

使用方式示例：

```bash
chmod +x deploy-uploaded.sh
./deploy-uploaded.sh
```

或：

```bash
chmod +x deploy-from-github.sh
DASHSCOPE_API_KEY=你的密钥 ./deploy-from-github.sh
```
