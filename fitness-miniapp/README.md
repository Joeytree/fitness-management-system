# 多功能健身 · 用户端微信小程序（uni-app Vue3）

> 毕业设计《多功能健身管理系统的设计与实现》—— 用户端小程序
> 技术栈：uni-app（Vue3）+ Vite，编译为微信小程序

## 项目结构

```
fitness-miniapp/
├── src/
│   ├── App.vue              # 全局设计 token（薄荷绿 #10b981）
│   ├── main.js              # createSSRApp 入口
│   ├── manifest.json        # 应用配置（微信 appid 等）
│   ├── pages.json           # 32 个页面路由 + 自定义 tabBar
│   ├── custom-tab-bar/      # 自定义底部 tabBar（4 Tab）
│   ├── utils/
│   │   ├── request.js       # uni.request 封装（token + 统一响应）
│   │   ├── config.js        # Base URL（后端地址）
│   │   └── index.js         # 字典/工具函数/登录态
│   └── pages/               # 32 个页面
│       ├── login/ register/          # 登录注册
│       ├── home/                      # 首页（Tab）
│       ├── course/                    # 课程列表（Tab）+ 详情 + 动作详情
│       ├── plan/                      # 计划列表/详情/创建/模板/日历
│       ├── diet/                      # 饮食首页/食物搜索/记录添加
│       ├── recipe/ knowledge/         # 食谱/知识库
│       ├── health/                    # 健康首页/身体/目标/打卡
│       ├── community/                 # 社区（Tab）+ 发布 + 详情
│       ├── user/                      # 他人主页/关注粉丝
│       └── mine/                      # 我的（Tab）+ 资料/收藏/通知
├── package.json
└── vite.config.js          # vite proxy 指向后端 8080
```

## 页面清单（32 个，与 PRD 1:1）

- **底部 Tab（4）**：首页 / 课程 / 社区 / 我的
- **登录注册（2）**：登录（微信占位+密码登录）/ 注册
- **推荐（1）**：为你推荐（计划/食谱切换）
- **课程（3）**：课程列表 / 课程详情（动作折叠+评论+收藏点赞）/ 动作讲解
- **计划（5）**：计划列表 / 详情 / 创建 / 模板 / 训练日历
- **饮食（3）**：饮食首页 / 食物搜索 / 记录添加
- **食谱/知识（4）**：食谱列表 / 食谱详情 / 知识库 / 文章详情
- **健康（4）**：健康首页（**含体重趋势折线图 7/30天/全部**）/ 身体数据 / 健康目标 / 打卡日历
- **社区（3）**：动态流 / 发布动态 / 动态详情（**含评论回复**）
- **用户（2）**：他人主页（**含 TA 的动态**）/ 关注粉丝
- **我的（4）**：个人中心 / 资料编辑（头像/生日/简介）/ **我的课程（学习进度）** / 收藏 / 消息通知（**公告+审核结果**）
- **动作库/UGC（4）**：**动作库（部位筛选）** / **提交自定义动作** / **提交自定义课程**

## 与 PRD 需求对应（全量补齐）

| PRD 需求 | 实现 |
|----------|------|
| REQ-USER-007/008 微信登录 | ✅ 已接入真实 code2session（后端 /auth/wxlogin + 前端 wx.login），AppID 已配置 |
| REQ-COURSE-002 动作库部位筛选 | pages/action/index |
| REQ-COURSE-007 UGC 自定义动作/课程 | pages/action/ugc + pages/course/ugc（待审核） |
| REQ-COURSE-008 学习进度 | 课程详情标记进度 + pages/mine/courses |
| REQ-HEALTH-002 体重趋势图 | 健康首页 canvas 折线图（7/30天/全部） |
| REQ-SOCIAL-006 评论回复 | 动态详情回复（reply_to） |
| REQ-SOCIAL-007 审核结果通知 | pages/mine/notifications（驳回原因） |
| REQ-REC-005 推荐行为记录 | 首页/推荐页点击上报 feedback |
| 个人主页动态 | /user/{id} 返回该用户动态 |

> 后端配套：新增 course_progress 表、action/course/user 加列（user_id/intro/openid）、/auth/wxlogin、/course/*/progress、/action/ugc、/course/ugc、/notifications、/user/{id} 补动态。

## 微信登录说明

- AppID：`wx6193281e02e83bc0`（已写入 manifest.json / project.config.json）
- AppSecret 仅存于后端 `application.yml`（fitness.wechat.secret），不进入前端代码
- 流程：前端 `uni.login` 拿 code → POST `/api/auth/wxlogin` → 后端调微信 `code2session` 换 openid → 无则自动注册（REQ-USER-007）→ 签发 JWT
- 失败处理：code 无效/过期返回 2003「微信登录失败，请重试」（REQ-USER-008）
- ⚠️ 微信登录需在**微信开发者工具或真机**中测试（H5 环境无 uni.login，按钮会提示）；后端需保持 `http://localhost:8081` 可达（开发者工具勾选「不校验合法域名」）

## 启动方式

### 1. 确保后端已启动（8081，8080 被本机 Jenkins 占用）
```bash
cd fitness-backend
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"
```
> Base URL 配置在 `src/utils/config.js`，默认 `http://localhost:8081/api`

### 2. 安装依赖
```bash
cd fitness-miniapp
npm install --registry=https://registry.npmmirror.com
```

### 3. 编译到微信小程序
```bash
npm run build:mp-weixin
# 或开发模式（热更新）
npm run dev:mp-weixin
```

### 4. 用微信开发者工具打开
1. 打开「微信开发者工具」
2. 导入项目，目录选择 `fitness-miniapp/dist/build/mp-weixin`（或 `dist/dev/mp-weixin`）
3. AppID 用测试号即可（manifest.json 里已配置 `touristappid`）
4. **重要**：详情 → 本地设置 → 勾选「不校验合法域名、web-view（业务域名）、TLS 版本以及 HTTPS 证书」（因为后端是 localhost HTTP）

### 5. 浏览器预览（H5 调试用）
```bash
npm run dev:h5
# 访问 http://localhost:5175
```

## 演示账号

| 账号 | 密码 | 说明 |
|------|------|------|
| 13800000001 | 123456 | 种子用户（增肌/新手） |
| 13800000002 | 123456 | 种子用户（增肌/进阶） |

## 设计系统（薄荷绿清新系）

- 主色 `#10b981`，渐变 `linear-gradient(135deg, #34d399, #10b981)`
- 背景 `#f6faf8`，卡片白底圆角 28rpx
- 按钮 22px 圆角 + 渐变 + 软阴影
- 设计 token 集中在 `App.vue` 的 `page` 选择器

## 说明

- 微信一键登录按钮为**占位**（演示环境未接微信开放平台），实际用手机号密码登录
- 后端 Base URL 修改见 `src/utils/config.js`
- 登录态存于 `uni.setStorageSync('token')`，请求自动携带 `Authorization: Bearer <token>`
