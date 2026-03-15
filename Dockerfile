# 多阶段构建：前端 -> 后端 jar -> 运行
# 适用于腾讯云 CVM/轻量等，构建：docker build -t yu-ai-agent .
# 运行：docker run -d -p 8123:8123 -e DASHSCOPE_API_KEY=sk-xxx yu-ai-agent

# 阶段 1：构建前端（NODE_ENV=production 使 Vite 输出 base /api/，与后端 context-path 一致）
FROM node:20-alpine AS frontend
WORKDIR /app/frontend
COPY yu-ai-agent-frontend/package*.json ./
RUN npm ci
COPY yu-ai-agent-frontend/ ./
ENV NODE_ENV=production
RUN npm run build

# 阶段 2：构建后端（含前端静态资源）
FROM maven:3.9-eclipse-temurin-21-alpine AS backend
WORKDIR /build

COPY pom.xml ./yu-ai-agent/
COPY src ./yu-ai-agent/src
RUN mkdir -p yu-ai-agent-frontend/dist
COPY --from=frontend /app/frontend/dist ./yu-ai-agent-frontend/dist

WORKDIR /build/yu-ai-agent
RUN mvn -DskipTests package -Pwith-frontend -q

# 阶段 3：运行
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN adduser -D -u 1000 appuser
COPY --from=backend /build/yu-ai-agent/target/yu-ai-agent-*.jar ./app.jar
RUN chown -R appuser:appuser /app

USER appuser
EXPOSE 8123

ENV JAVA_OPTS="-Xms256m -Xmx512m"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --spring.profiles.active=local,no-db"]
