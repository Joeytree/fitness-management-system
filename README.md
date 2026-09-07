# 多功能健身管理系统

毕业设计项目：**多功能健身管理系统的设计与实现**

> 课程学习 + 饮食管理 + 健康监测 + 社交互动 + 智能推荐 一体化平台

## 技术栈

| 层 | 技术 |
|----|------|
| 后端 | Spring Boot 3.4 + MyBatis-Plus + MySQL 8 + JWT |
| 管理端 | Vue 3 + Vite + Element Plus + ECharts |
| 移动端 | Vue 3 + Vite + Vant + ECharts |

## 项目结构

```
D:\Workb_pj\
├── fitness-backend/    # Spring Boot 后端（端口 8080）
├── fitness-admin/      # PC 管理端（Vite 端口 5173）
├── fitness-h5/         # 移动端 H5（Vite 端口 5174）
├── sql/                # 建表 SQL + 补充脚本
└── README.md
```

## 环境要求

- JDK 17+（本机 24 可用）
- Maven 3.9+
- MySQL 8.0（本机已装，root/root）
- Node.js 18+

## 快速启动

### 1. 初始化数据库（首次）

```bash
mysql -uroot -proot < sql/建表SQL.sql
```

> 建表 SQL 位于 `C:/Users/赖正俊/Desktop/毕业论文/多功能智慧健身/数据库设计-建表SQL.sql`
> 本项目额外补充了 5 张表（收藏/点赞/课程动作关联/驳回原因字段），见 `sql/补充SQL.sql`

### 2. 启动后端（8080）

```bash
cd fitness-backend
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8080"
```

或打包后运行：

```bash
mvn package -DskipTests
java -jar target/fitness-backend-1.0.0.jar --server.port=8080
```

> 注意：`--server.port=8080` 必须显式指定（本机环境存在 `SERVER__PORT=0` 环境变量会覆盖默认端口）。

### 3. 启动管理端（5173）

```bash
cd fitness-admin
npm install
npm run dev
```

### 4. 启动移动端（5174）

```bash
cd fitness-h5
npm install
npm run dev
```

## 账号信息

| 端 | 账号 | 密码 | 地址 |
|----|------|------|------|
| 管理端 | admin | admin123 | http://localhost:5173 |
| 移动端 | 13800000001 | 123456 | http://localhost:5174 |
| 移动端 | 13800000002 | 123456 | http://localhost:5174 |

## 功能模块

| 模块 | 说明 |
|------|------|
| 用户 | 注册登录（JWT + BCrypt）、个人资料、数据隔离 |
| 课程 | 动作讲解、部位分类、训练计划、收藏点赞评论、UGC |
| 饮食 | 食物库、饮食记录、营养汇总、目标对比、食谱 |
| 健康 | 身体数据/BMI、趋势图、健康评估、打卡 |
| 社交 | 关注/粉丝、动态发布、点赞评论、审核 |
| 推荐 | 规则引擎（目标+3/水平+2/部位+1）标签匹配 |
| 管理端 | 仪表盘、用户/内容管理、审核、RBAC |

## 数据库说明

原设计 21 张表，实际开发中补充了 5 处（见 `sql/补充SQL.sql`）：

1. `course_favorite` 课程收藏关系表
2. `course_like` 课程点赞关系表
3. `course_action` 课程-动作关联表
4. `course.like_count` 点赞数字段
5. `user.target_weight / target_body_fat` 健康目标字段
6. `reject_reason` 驳回原因字段（moment/course_comment/moment_comment/action）

## 说明

- 推荐引擎采用「规则引擎 + 标签匹配」实现，可解释性强，便于答辩讲解
- 前端通过 Vite proxy 转发 `/api` 到后端 8080，支持跨域
