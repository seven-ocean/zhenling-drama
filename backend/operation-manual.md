# 操作手册

## 项目概述

火宝短剧 (Huobao Drama) 是一个基于 AI 的短剧自动化生产平台，实现从剧本生成、角色设计、分镜制作到视频合成的全流程自动化。

## 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | Vue3 + Vite + ofetch + Tailwind CSS |
| 后端 | Java 17 + SpringBoot 3.x + Maven |
| 数据库 | MySQL 8.0 |
| 缓存 | Redis |

## 环境要求

- JDK 17+
- Node.js 18+
- MySQL 8.0
- Redis
- FFmpeg (视频处理)

## 快速开始

### 1. 数据库初始化

```bash
# 登录 MySQL
mysql -u root -p

# 创建数据库
CREATE DATABASE huobao_drama DEFAULT CHARACTER SET utf8mb4;

# 执行建表 SQL
source sql/init.sql
```

### 2. 后端配置

```bash
# 复制配置
cp src/main/resources/application.yml src/main/resources/application-dev.yml

# 编辑数据库和 Redis 配置
vim src/main/resources/application-dev.yml
```

### 3. 启动后端

```bash
cd drama-project

# Maven 启动
mvn spring-boot:run

# 或打包后启动
mvn package
java -jar target/huobao-drama-1.0.0.jar
```

### 4. 启动前端

```bash
cd frontend

# 安装依赖
npm install

# 开发模式
npm run dev

# 生产构建
npm run build
```

## 环境配置

### 开发环境 (application-dev.yml)

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/huobao_drama
    username: root
    password: root
  data:
    redis:
      host: localhost
      port: 6379
```

### 生产环境 (application-prod.yml)

```yaml
spring:
  datasource:
    url: jdbc:mysql://your-host:3306/huobao_drama
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  data:
    redis:
      host: ${REDIS_HOST}
      port: ${REDIS_PORT}
      password: ${REDIS_PASSWORD}
```

## API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/v1/dramas | 创建剧集 |
| GET | /api/v1/dramas | 剧集列表 |
| GET | /api/v1/dramas/{id} | 剧集详情 |
| PUT | /api/v1/dramas/{id} | 更新剧集 |
| DELETE | /api/v1/dramas/{id} | 删除剧集 |
| POST | /api/v1/characters | 创建角色 |
| GET | /api/v1/characters/drama/{dramaId} | 角色列表 |
| POST | /api/v1/scenes | 创建场景 |
| GET | /api/v1/scenes/drama/{dramaId} | 场景列表 |

## 常见问题

### Q: 启动报错端口被占用？

A: 修改 `application.yml` 中的 `server.port`

### Q: 数据库连接失败？

A: 检查 MySQL 配置和用户权限

### Q: Redis 连接失败？

A: 检查 Redis 服务和配置

## 版本记录

- v1.0.0 - 初始版本，基础 CRUD 功能