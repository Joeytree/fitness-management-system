# 智慧健身系统 · Fitness Management System

> 毕业设计项目 —— 基于 Spring Boot 的智慧健身系统设计与实现
> 课程学习 + 训练计划 + 饮食管理 + 健康监测 + 社交互动 + AI 私教，一体化健身平台

一套面向普通健身人群的三端系统：**微信小程序（用户端）+ PC 管理后台 + Spring Boot 服务端**。
在传统健身管理功能之上，集成了基于大模型的 **AI 私教**模块，用「规则引擎兜底 + LLM 增强」的方式生成可执行的训练课程与周期化计划。

---

## 功能一览

### 用户端（微信小程序）

| 模块 | 能力 |
|------|------|
| 账号 | 注册 / 登录（JWT）、微信登录、资料与头像、训练偏好与伤病禁忌设置 |
| 动作库 | 1324 条动作，GIF 动图演示，中文名称 + 分步说明 + 要点/呼吸/常见错误，按部位/器械/难度筛选 |
| 课程 | 课程分类浏览、详情、跟练执行（计时/计次）、收藏点赞评论、用户自建课程（UGC） |
| 训练计划 | 计划模板、自建计划、周期化安排、日历视图、计划打卡与完成度统计 |
| 训练记录 | 训练会话与组数明细、历史记录、训练统计（时长/消耗/部位分布） |
| 饮食 | 食物库检索、饮食记录、营养素汇总与目标对比、推荐食谱 |
| 健康 | 身体数据录入、BMI 计算、趋势曲线、健康评估、每日打卡 |
| 社交 | 关注/粉丝、动态发布、点赞评论、内容审核 |
| 推荐 | 推荐课程 / 推荐计划 / 推荐食谱 / 推荐画像（规则引擎 + 标签匹配） |
| AI 私教 | 自然语言对话、意图识别、生成训练课程与多周计划、健身问答、流式输出（WebSocket） |
| 其他 | 聚合搜索、消息通知、我的收藏 |

### 管理后台（PC）

仪表盘数据看板、用户管理、动作库管理、课程管理、食物库管理、食谱管理、文章/知识管理、UGC 内容审核、系统与角色权限管理。

### 服务端能力

JWT 鉴权（`@RequireLogin` / `@RequireAdmin`）、统一 `Result<T>` 响应、全局异常处理、文件上传、
参数校验、Druid 连接池与 SQL 监控、Redis 会话缓存、WebSocket 长连接、大模型调用与安全拦截。

---

## 技术栈

| 层 | 技术选型 |
|----|----------|
| 服务端 | Java 17、Spring Boot 3.4.5、MyBatis-Plus、MySQL 8、Redis、Druid、WebSocket |
| 鉴权 | 自研 JWT 拦截器（`JwtInterceptor` + 注解），密码 BCrypt 加密（`spring-security-crypto`） |
| AI | DeepSeek Chat（OpenAI 兼容协议，`RestClient` 直连，JSON Mode）+ 规则引擎兜底 |
| 管理端 | Vue 3 + Vite + Element Plus + ECharts + Axios |
| 用户端 | uni-app（Vue 3 + Vite）编译至微信小程序 `mp-weixin` |

> 本项目**未使用 Spring Security 完整 starter**，仅取 `spring-security-crypto` 做密码哈希，鉴权由自定义拦截器实现。

---

## 系统架构

```
微信小程序 (uni-app)        管理后台 (Vue 3 + Element Plus)
        │                            │
        └──────────┬─────────────────┘
                   │  HTTP /api/**  ·  WebSocket /ws/ai/chat
        ┌──────────▼──────────────────────────────┐
        │           Spring Boot 3.4.5             │
        │  Controller → Service → Mapper          │
        │  ├─ JwtInterceptor（鉴权）               │
        │  ├─ GlobalExceptionHandler（统一异常）   │
        │  └─ agent（AI 私教：编排/规则/安全/会话） │
        └───────┬─────────────┬───────────────────┘
                │             │
          MySQL 8.0        Redis（会话缓存，可降级）
                │
     DeepSeek API（可选，未配置则走规则兜底）
```

---

## 项目结构

```
fitness-backend/                 Spring Boot 服务端
├── src/main/java/com/fitness/
│   ├── controller/              业务接口（action/course/plan/training/diet/...）
│   ├── service/  mapper/  entity/  dto/  config/  security/
│   └── agent/                   AI 私教模块
│       ├── orchestrate/         编排入口（意图识别 → 规则/LLM → 结构化结果）
│       ├── rule/                规则引擎（CourseAssembler / PlanAssembler）
│       ├── safety/              安全拦截（伤病禁忌双重校验）
│       ├── llm/                 DeepSeek 客户端
│       ├── session/             会话管理（Redis + MySQL）
│       ├── knowledge/           FAQ 知识库
│       ├── ws/                  WebSocket 流式推送
│       └── controller/          /api/ai/**
├── config/                      本地私密配置（application-local.yml，未入库）
└── upload/                      静态资源目录（动作 GIF 等，未入库）

fitness-admin/                   PC 管理后台（Vite，端口 5173）
├── src/views/                   页面（dashboard / user / action / course / ...）
└── src/components/ImageUpload.vue  统一上传组件

fitness-miniapp/                 微信小程序（uni-app）
├── src/pages/                   页面（home / course / plan / diet / health / ai / mine ...）
├── src/custom-tab-bar/          原生自定义 tabBar
└── src/utils/                   request / upload / config
```

---

## 快速开始

### 1. 环境要求

| 依赖 | 版本建议 | 说明 |
|------|----------|------|
| JDK | 17+ | 项目按 17 编译 |
| Maven | 3.6+ | 或用 IDE 内置 Maven |
| MySQL | 8.0 | 库名 `fitness_management`，字符集 `utf8mb4` |
| Redis | 5.0+ | 可选，未启动时 AI 会话自动降级为无缓存模式 |
| Node.js | 18+ | 管理端与小程序共用 |
| 微信开发者工具 | 最新稳定版 | 运行小程序端 |

### 2. 准备数据库

在 MySQL 中创建库并导入表结构与基础数据（`sql/` 目录未包含在本开源仓库中，请使用自己的备份导入）：

```sql
CREATE DATABASE fitness_management DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

> 执行含中文的 SQL 时请加 `--default-character-set=utf8mb4`，否则易报 `Data too long for column`。

### 3. 配置本地参数

```bash
cd fitness-backend/config
cp application-local.yml.example application-local.yml
```

按需填写数据库密码、JWT 密钥、DeepSeek API Key 等。也可全部改用环境变量（见下方「配置说明」）。

### 4. 启动后端（端口 8081）

```bash
cd fitness-backend

# 打包
mvn -DskipTests package

# 启动（必须在 fitness-backend 目录下执行，否则 /upload/** 静态资源 404）
java -jar target/fitness-backend-1.0.0.jar --server.port=8081
```

启动后：

- 接口地址 `http://localhost:8081/api`
- Druid 监控台 `http://localhost:8081/druid`（账号见配置，默认 `admin` / `admin123`）

> 若 8080 被其他服务占用（如 Jenkins），务必用 `--server.port=8081` 前置参数覆盖。

### 5. 启动管理后台（端口 5173）

```bash
cd fitness-admin
npm install
npm run dev
```

访问 `http://localhost:5173`。后端 CORS 白名单默认放行 `localhost:5173` 与 `127.0.0.1:5173`，
其他来源需通过 `FITNESS_CORS_ORIGINS` 追加。

### 6. 启动微信小程序

```bash
cd fitness-miniapp
npm install

npm run dev:mp-weixin     # 开发监听，产物 dist/dev/mp-weixin
npm run build:mp-weixin   # 生产构建，产物 dist/build/mp-weixin
```

在微信开发者工具中「导入项目」，选择对应的产物目录，AppID 见 `src/manifest.json` 的 `mp-weixin.appid`。

调试时需勾选「不校验合法域名、web-view（业务域名）、TLS 版本以及 HTTPS 证书」，
并在 `src/utils/config.js` 中把 `BASE_URL` 指向本机后端地址（真机调试请改为局域网 IP）。

---

## 配置说明

`application.yml` 中不含任何明文密钥，全部通过环境变量占位；本地可用 `config/application-local.yml` 覆盖。

| 环境变量 | 说明 | 默认 |
|----------|------|------|
| `DB_USERNAME` / `DB_PASSWORD` | 数据库账号密码 | `root` / 空 |
| `JWT_SECRET` | JWT 签名密钥 | 空（启动后需配置） |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | Redis 连接 | `localhost` / `6379` / 空 |
| `WECHAT_APPID` / `WECHAT_APPSECRET` | 小程序登录凭证 | AppID 内置 / Secret 空 |
| `DEEPSEEK_API_KEY` | DeepSeek API Key，未配置时 AI 走规则兜底 | 空 |
| `DEEPSEEK_BASE_URL` / `DEEPSEEK_MODEL` | 模型服务地址与名称 | `https://api.deepseek.com` / `deepseek-chat` |
| `FITNESS_CORS_ORIGINS` | 允许的跨域来源，逗号分隔 | `localhost:5173,127.0.0.1:5173` |
| `FITNESS_UPLOAD_DIR` | 上传文件存储目录 | `${user.dir}/upload` |
| `DRUID_STAT_USERNAME` / `DRUID_STAT_PASSWORD` | 监控台登录账号 | `admin` / `admin123` |

---

## 默认账号

| 端 | 账号 | 密码 |
|----|------|------|
| 管理后台 | `admin` | `admin123` |
| 小程序 | `13800000001` | `123456` |
| 小程序 | `13800000002` | `123456` |

> 演示数据随数据库导入脚本初始化，首次部署后请及时修改默认密码。

---

## 接口概览

统一前缀 `http://localhost:8081/api`，响应结构 `{ code, message, data }`，
业务错误一律返回 HTTP 200 + 非 0 `code`（资源不存在除外）。

| 前缀 | 说明 | 代表接口 |
|------|------|----------|
| `/api/auth` | 认证 | `POST /login`、`POST /register`、`POST /wxlogin` |
| `/api/user` | 用户资料 | `GET/PUT /profile` |
| `/api/action` | 动作库 | `GET /`（分页 + 部位/器械/难度筛选）、`GET /{id}` |
| `/api/course` | 课程 | `GET /`（分类/部位/难度/时长/关键词）、`GET /{id}` |
| `/api/training` | 训练 | `POST /start`、`POST /complete`、`GET /`、`GET /{id}` |
| `/api/plan` | 计划 | `GET /templates`、`POST /template/{id}/adopt`、`GET /calendar/detail` |
| `/api/diet`、`/api/food` | 饮食与食物库 | 记录增删改查、营养素汇总、食物检索 |
| `/api/recipe` | 食谱 | `GET /`、`GET /{id}` |
| `/api/health` | 健康 | 身体数据、趋势、评估、打卡 |
| `/api/moment` | 社交 | 动态发布、点赞、评论、关注 |
| `/api/search` | 聚合搜索 | `GET /?keyword=` |
| `/api/recommend` | 推荐 | `/course`、`/plan`、`/recipe`、`/profile`、`POST /feedback` |
| `/api/ai` | AI 私教 | `POST /chat`、`/course`、`/plan`、`POST /feedback`、`GET /qa`、`GET /session` |
| `/api/admin/**` | 管理端 | 类级 `@RequireAdmin` 统一鉴权 |
| `/ws/ai/chat?token=<JWT>` | AI 流式对话 | WebSocket，chunk 分片 + done 帧携带结构化数据 |

---

## 设计要点

**规则引擎优先，LLM 增强**
AI 私教不是把需求直接丢给大模型。意图识别、动作筛选、课程组装、周期化排程全部由确定性规则完成
（`CourseAssembler`：热身 1 + 主项 2 + 辅助 2 + 收尾 1；`PlanAssembler`：周数可配、渐进超负荷按周递增组次），
LLM 只负责意图抽取与文案润色。未配置 API Key 时系统仍可完整生成课程与计划，保证降级可用。

**安全双层拦截**
`SafetyGuard` 关键词匹配 + LLM 分类双重校验，命中伤病禁忌时直接改写或拦截方案，
并在用户画像中合并个人禁忌部位，避免"腰伤仍安排硬拉"这类问题。

**难度映射做了数据适配**
动作库难度分布极不均衡（入门级动作仅 20 余条），课程组装对新手开放 `difficulty 1~2` 并放宽热身/收尾难度，
否则新手用户几乎无可用动作。该处刻意保留的"非直觉"逻辑，改动前请先核对数据分布。

**统计口径以训练记录为准**
训练统计汇总 `training_record` 而非打卡表（`check_in` 每日仅一条，多次训练会严重漏算），并跳过时长为 0 的空会话。

**鉴权与数据隔离**
`JwtInterceptor` 仅拦截 `/api/**`，`@RequireLogin` 校验登录、`@RequireAdmin` 校验管理员，
业务层按 `UserContext.getUserId()` 过滤数据，避免越权读写。

---

## 已知限制

- **静态资源未入库**：`upload/` 下约 1300 个动作 GIF（约 120MB）未提交到仓库，克隆后动作图片需自行补齐；数据库中 `media_url` 存的是 `/upload/actions/xxx.gif` 相对路径。
- **数据库脚本未开源**：`sql/` 目录仅保留在本地，开源仓库只包含三端代码。
- **LLM 相关未做生产级加固**：限流、结果缓存、评测集与单元测试尚未接入，仅满足毕业设计演示与课程实验需要。
- **小程序需本地后端**：未部署公网服务，真机调试需将 `BASE_URL` 改为局域网地址。

---

## 数据来源与致谢

- 动作数据集来自 [Gym visual Exercises Dataset](https://github.com/hasaneyldrm/exercises-dataset)，GIF 图片版权归 Gym visual 所有，仅用于教学与毕业设计演示。
- 后端基于 [Spring Boot](https://spring.io/projects/spring-boot)、[MyBatis-Plus](https://baomidou.com/)，前端基于 [Vue 3](https://cn.vuejs.org/)、[Element Plus](https://element-plus.org/)、[uni-app](https://uniapp.dcloud.net.cn/)。
- 大模型能力由 [DeepSeek](https://www.deepseek.com/) 提供。

---

## License

本项目为个人毕业设计作品，仅用于学习与技术交流。
