# 多功能健身管理系统

毕业设计项目：**多功能健身管理系统的设计与实现**

> 课程学习 + 饮食管理 + 健康监测 + 社交互动 + 智能推荐 一体化平台

## 技术栈

| 层 | 技术 |
|----|------|
| 后端 | Spring Boot 3.4 + MyBatis-Plus + MySQL 8 + JWT（Java 17） |
| 管理端 | Vue 3 + Vite + Element Plus + ECharts |
| 移动端 | uni-app Vue 3 + Vite（微信小程序 mp-weixin） |

## 项目结构

```
D:\Workb_pj\
├── fitness-backend/    # Spring Boot 后端（端口 8081）
├── fitness-admin/      # PC 管理端（Vite 端口 5173）
├── fitness-miniapp/    # 微信小程序（uni-app，需在微信开发者工具中运行）
├── .gitignore
└── README.md
```

> 数据库 SQL、数据处理脚本、原型文件、预览页等仅保留在本地仓库，不在此开源仓库中。

## 环境要求

- JDK 17+（本机 24 可用）
- Maven 3.9+（本机无全局 mvn，可用 IDEA 内置 Maven 或自行安装）
- MySQL 8.0
- Node.js 18+
- 微信开发者工具（用于运行小程序端）

## 快速启动

### 1. 启动后端（端口 8081）

后端必须以 `fitness-backend` 为工作目录启动，否则 `/upload/**` 静态资源会 404。

```bash
cd fitness-backend
# 编译
mvn -DskipTests compile
# 打包
mvn -DskipTests package
# 启动（强制 8081 端口）
java -jar target/fitness-backend-1.0.0.jar --server.port=8081
```

> 8080 端口本机已被 Jenkins 等占用，统一使用 8081。后端配置文件 `src/main/resources/application.yml` 已使用环境变量占位管理数据库密码 / JWT 密钥 / 微信 AppSecret，本地私密配置放在 `config/application-local.yml`（参见同目录 `.example` 模板）。

### 2. 启动管理端（端口 5173）

```bash
cd fitness-admin
npm install
npm run dev
```

### 3. 启动微信小程序端

```bash
cd fitness-miniapp
npm install
# 编译产物
npm run dev:mp-weixin    # 监听模式，产物 dist/dev/mp-weixin
# 或
npm run build:mp-weixin  # 构建模式，产物 dist/build/mp-weixin
```

> 在微信开发者工具中「导入项目」，选择 `dist/dev/mp-weixin`（监听模式）或 `dist/build/mp-weixin`（构建模式），AppID 见 `src/manifest.json` 的 `mp-weixin.appid` 字段。

## 默认账号

| 端 | 账号 | 密码 | 访问地址 |
|----|------|------|----------|
| 管理端 | admin | admin123 | http://localhost:5173 |
| 小程序端 | 13800000001 | 123456 | 微信开发者工具 |
| 小程序端 | 13800000002 | 123456 | 微信开发者工具 |

## 功能模块

| 模块 | 说明 |
|------|------|
| 用户 | 注册登录（JWT + BCrypt）、个人资料、数据隔离 |
| 动作库 | 1324 条动作，含 GIF 动图、中文名称、分步说明、要点/呼吸/错误 |
| 课程 | 课程分类、课程详情、训练执行、收藏点赞评论、UGC |
| 训练 | 训练记录与组数明细、自动打卡 |
| 搜索 | 动作 / 课程 / 计划 / 食物聚合搜索 |
| 饮食 | 食物库、饮食记录、营养汇总、目标对比、食谱 |
| 健康 | 身体数据/BMI、趋势图、健康评估、打卡 |
| 社交 | 关注/粉丝、动态发布、点赞评论、审核 |
| 推荐 | 规则引擎（目标+3/水平+2/部位+1）标签匹配 |
| 管理端 | 仪表盘、用户/内容管理、审核、RBAC |

## 接口约定

- 接口基础地址：`http://localhost:8081/api`
- 鉴权：用户接口 `@RequireLogin`、管理接口所在 Controller 继承类级 `@RequireAdmin`
- 响应统一 `Result<T>` 包装
- 静态资源：`/upload/**` 由后端映射到工作目录下的 `upload/` 文件夹

## 说明

- 微信小程序端使用 `src/utils/config.js` 中的 `fullUrl()` 工具补全 `/upload/` 相对路径，调试时需启用「不校验合法域名」
- 推荐引擎采用「规则引擎 + 标签匹配」实现，可解释性强，便于答辩讲解
- 动作库数据来源于 [Gym visual Exercises Dataset](https://github.com/hasaneyldrm/exercises-dataset)，GIF 图片版权归 Gym visual 所有
