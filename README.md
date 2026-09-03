# 云阅 CloudRead 电子书平台

基于《电子书平台技术文档.md》从零实现的完整全栈项目：**Spring Boot 3 + Vue 3 + SQLite** 的轻量级、可溯源、高互动 B/S 架构电子书共享与管理平台。

## 功能总览

### 客户端（普通用户）
- **左侧可收起侧边栏**：分类 / 讨论 / 上传图书 / 个人主页 / 我的关注，一键折叠
- **分类页面**：点击侧边栏“分类”进入分类页，上方横排选项卡按一级/二级分类浏览书籍
- **登录即自动注册**：用户名全局唯一，密码 BCrypt 加密落库，JWT 无状态鉴权 + 无感续签
- **忘记密码**：登录页提交“邮箱 + 用户名 + 详情（≤500字）”请求，由管理员在后台重置密码
- **电子书管理**：支持 PDF / EPUB / TXT / MOBI 上传；SHA-256 数字指纹去重；重复文件可选“作为新版本上传”
- **封面处理**：封面展示统一为 16:10；可选手动上传封面（JPG/PNG）；未上传时自动解析 PDF/EPUB 内置封面；解析失败则按书名+作者生成默认文字封面
- **分类与检索**：多级分类树，按书名/作者/ISBN 模糊搜索，按最新/热门/书名排序
- **在线阅读与下载**：PDF 翻页阅读器（pdf.js）、TXT 翻页阅读器；自动记录阅读进度与下载次数
- **互动社区**：评论 + 最多两级嵌套回复；点赞（联合唯一索引防重复）；收藏管理
- **讨论区**：发布文字/带图帖子（最多 9 张，JPG/PNG/GIF/WEBP，文件头校验）；帖子评论/回复/点赞；同一用户 5 秒内只能发一个帖子（防刷屏）
- **头像系统**：个人主页可上传/预览/更换头像（JPG/PNG/WEBP，≤5MB），全局头像展示
- **个人主页**：我的收藏、我的上传、别人对我的帖子/书籍的评论聚合查看；可设置头像
- **关注体系**：关注/取消关注用户；侧边栏“我的关注”下拉查看关注者及其全部投稿（图书+帖子）；图书卡片“发布者：头像+名字”点击直达对方主页
- **书籍溯源**：上传者脱敏信息、上传时间、文件哈希、版本历史；一键实时重算哈希校验文件是否被篡改

### 管理端（管理员）
- **全栈可观测性中心**：JVM 内存/CPU/线程/GC 指标、HTTP 请求质量、SQLite 文件与 WAL 体积、连接池状态、慢查询记录
- **实时日志流**：WebSocket 实时推送 ERROR/WARN 日志，支持级别与关键字过滤（管理端鉴权）
- **前端监控**：接收 FCP/LCP、JS 异常、API 成功率埋点上报
- **内容审核**：书籍通过/驳回/删除，分类树管理，违规评论处理
- **用户请求**：待处理的忘记密码请求列表，管理员可重置该用户密码，处理完成后“对号”归档
- **用户请求日志**：所有已归档的用户请求（含处理人、处理时间）集中展示
- **用户管理**：用户列表、行为日志、封禁/解封
- **系统配置**：动态调整文件大小限制、格式白名单、审核/注册开关、分片阈值

## 技术栈

| 领域 | 选型 |
| --- | --- |
| 前端 | Vue 3 + Vite + Element Plus + ECharts + pdf.js + Pinia + Vue Router |
| 后端 | Spring Boot 3.3 + Spring Security + JWT + MyBatis-Plus |
| 数据库 | SQLite 3（WAL 模式）+ HikariCP |
| 接口文档 | Knife4j（Swagger 3） |
| 可观测性 | Spring Boot Actuator + Micrometer + Logback WebSocket Appender + TraceID 全链路 |

## 目录结构

```text
电子书平台/
├── 电子书平台技术文档.md   # 原始技术设计文档
├── backend/               # Spring Boot 后端
│   └── src/main/java/com/cloudread/
│       ├── controller/    # REST 接口
│       ├── service/       # 业务逻辑
│       ├── entity/        # 数据模型
│       ├── mapper/        # MyBatis-Plus Mapper
│       ├── security/      # JWT 鉴权
│       ├── storage/       # 文件存储 / 封面解析 / 封面生成 / SHA-256
│       ├── monitor/       # 慢查询拦截 / 日志流 WebSocket
│       └── init/          # 首次启动种子数据
├── frontend/              # Vue 3 前端
│   └── src/
│       ├── api/           # 接口封装
│       ├── views/         # 客户端与管理端页面
│       ├── components/    # 阅读器 / 图表 / 书籍卡片
│       └── utils/         # 前端埋点 / 分片上传工具
├── deploy/nginx.conf      # 生产 Nginx 反向代理配置示例
└── scripts/               # 一键构建 / 启动脚本
```

## 快速开始（从 GitHub 下载后）



---


### 源码方式运行（不使用 Docker）

**第 1 步：安装依赖环境**

- JDK 17+（下载 OpenJDK/Temurin 均可）
- Maven 3.8+
- Node.js 18+

**第 2 步：启动后端（端口 8080）**

```bash
cd backend
mvn -DskipTests package
java -jar target/cloudread-backend-1.0.0.jar
```

或开发模式：`mvn spring-boot:run`

**第 3 步：启动前端（端口 5173）**

```bash
cd frontend
npm install
npm run dev
```

浏览器访问 http://localhost:5173 ，接口由 Vite 自动代理到 http://localhost:8080 。

> Windows 用户也可直接用一键脚本：`.\scripts\build-all.ps1`（构建）→ `.\scripts\start-dev.ps1`（启动）。

---



---

### 常见问题排查（FAQ）

| 现象 | 原因与解决 |
| --- | --- |
| `docker: command not found` | Docker 未安装或未加入 PATH，请先安装并重启终端 |
| 无法连接 Docker 引擎 | Docker Desktop 未启动，先启动它 |
| 80 / 8080 端口被占用 | 修改 `docker-compose.yml` 的 `ports`（如 `"8081:8080"`），或停止占用端口的程序后重试 |
| 镜像拉取失败 / 很慢 | 确认 Docker 镜像源可用（国内建议配置 DaoCloud 等加速源），或用 `--build-arg BASE_REGISTRY=其他镜像地址` 换源 |
| 首次构建耗时很长 | 正常，正在下载基础镜像与依赖；网络越慢越久，后续启动秒级 |
| 后端启动失败 | 查看日志：`docker compose -p cloudread logs -f backend`，按报错排查（端口、磁盘空间等） |
| 想恢复初始数据 | `docker compose -p cloudread down -v` 后重新 `up -d`（会清空所有数据，谨慎） |
| 代码更新后怎么刷新 | 直接 `docker compose -p cloudread up -d --build`，自动重建有改动的服务且数据保留 |
| 忘记密码功能 | 用户在登录页提交请求后，由管理员在“管理后台 → 用户请求”中重置密码并归档 |

---

### 演示账号与接口文档

| 角色 | 用户名 | 密码 |
| --- | --- | --- |
| 管理员 | admin | admin123 |
| 普通用户 | demo | demo123 |

任意不存在的用户名 + 密码（≥6 位）即可自动注册。

接口文档（后端启动后）：http://localhost:8080/doc.html （Knife4j 在线调试）

### 上线前必改

- 修改默认管理员密码；
- 用环境变量覆盖 JWT 密钥：设置 `APP_JWT_SECRET`（对应配置 `app.jwt.secret`），避免使用仓库中公开的开发密钥；
- 生产环境建议启用 HTTPS（Nginx 配置证书），并按 [生产部署](#生产部署nginx--spring-boot) 章节落地。

## 核心设计说明

- **文件去重与版本**：上传后先计算 SHA-256，若与已入库书籍哈希一致则提示“作为新版本上传”，旧版本自动归档到 `version_history` 并写入溯源日志
- **防篡改校验**：`POST /api/v1/books/{id}/verify` 实时重算物理文件哈希并与库中记录比对，不一致时写入安全告警日志
- **分片上传**：文件超过阈值（默认 100MB）前端按 5MB 分片上传，后端合并后重新计算 SHA-256
- **实时日志流**：自定义 Logback Appender 将 ERROR/WARN 日志推送给已鉴权的管理端 WebSocket 会话；`/ws/logs` 支持首条消息携带 token 鉴权
- **全链路追踪**：每个请求生成 TraceID，写入日志 MDC、响应头与 `system_log`，前端请求自动携带
- **SQLite 优化**：启动时强制 `PRAGMA journal_mode=WAL`、`busy_timeout=5000`、`foreign_keys=ON`

## 生产部署（Nginx + Spring Boot）

参考 [deploy/nginx.conf](deploy/nginx.conf)：

- Nginx 托管 `frontend/dist` 静态资源，开启 Gzip
- `/api` 与 `/ws` 反向代理至 Spring Boot 8080
- 后端以 `nohup java -jar` 或 systemd 守护运行，SQLite 每日定时备份 `.db` 与 `-wal` 文件

## 数据与备份

- 数据库与上传文件默认位于 `backend/data/`（可在 `application.yml` 中通过 `app.data-dir` / `app.upload-dir` 调整）
- 首次启动自动建表并初始化管理员、演示用户、分类树、系统配置与示例书籍
- 删除 `backend/data` 后重启即可恢复初始状态（示例数据会重新生成）
