# 人工智能社官网 · NEXUS

> 这是一个**独立新建的视觉增强版前端**，位于 `ai-club-nova/`。它不修改仓库中现有 `frontend/` 的任何文件，并继续对接同一套 Spring Boot 后端与 `/api` 接口。路由、页面、管理流程、权限与校验行为均与现有前端保持一致。

现有前端是偏编辑设计的绿色 / 米白风格；NEXUS 版本把它重做为深空星云 + 极光青紫的科技视觉，但功能不缩水：

- 同样的 12 条路由（含 404）、同样的数据接口、同样的管理员权限判断。
- 同样的列表筛选、分页、详情、图片放大、预览、发布、下架、回收站、账号管理、操作记录。
- 保留 Element Plus 后台组件与 wangEditor 正文编辑器。

> 下方截图使用测试期间写入 H2 内存库的**临时示例数据**，仅用于展示界面状态，不代表社团真实新闻、成员、项目或成果。正式内容以后台发布为准。

## 预览

| 首页 Hero | 首页长图 |
| --- | --- |
| ![首页 Hero](screenshots/hero.jpg) | ![首页](screenshots/home.jpg) |

| 新闻与活动 | 内容详情 |
| --- | --- |
| ![新闻与活动](screenshots/news.jpg) | ![内容详情](screenshots/detail.jpg) |

| 管理后台 | 编辑器 |
| --- | --- |
| ![管理后台](screenshots/admin.jpg) | ![编辑器](screenshots/editor.jpg) |

| 管理员登录 | 移动端菜单 |
| --- | --- |
| ![管理员登录](screenshots/login.jpg) | ![移动端菜单](screenshots/mobile-menu.jpg) |

更多截图见 [`screenshots/`](screenshots)。

## 技术栈

与现有前端保持一致，不引入额外运行时框架：

| 类别 | 选型 |
| --- | --- |
| 框架 | Vue 3.5 + TypeScript |
| 构建 | Vite 7 |
| 路由 | Vue Router 4 |
| 状态 | Pinia 3 |
| 请求 | Axios 1 |
| 后台组件 | Element Plus 2（深色主题变量覆盖） |
| 富文本 | wangEditor 5 |
| 样式 | 原生 CSS + 设计令牌 + 关键帧 / IntersectionObserver |

## 快速开始

### 1. 启动现有后端（二选一）

**本地界面预览（H2 内存库，不写生产数据）**

```powershell
cd backend
mvn spring-boot:test-run -Dspring-boot.run.main-class=cn.aiclub.PreviewApplication
```

临时账号：`test-super` / `a-test-password-12345`。该账号与 H2 仅用于测试，后端退出后数据清空。

**正式运行**

按现有后端文档配置 MySQL、`BOOTSTRAP_USER`、`BOOTSTRAP_PASSWORD` 等环境变量后启动，监听 `8080`。

### 2. 启动本项目

```powershell
cd ai-club-nova
npm install
npm run dev
```

打开 <http://localhost:5174>。开发服务器把 `/api` 代理到 `http://localhost:8080`；如果后端地址不同，修改 [`vite.config.ts`](vite.config.ts) 中的 `backend` 常量即可。

### 3. 生产构建

```powershell
npm run build
npm run preview   # 本地检查 dist 与 /api 代理
```

`dist/` 可由 Nginx 托管，保持与现有部署相同的两条规则即可：

1. `/api` 反向代理到 Spring Boot；
2. 其余路径回退到 `index.html`，保证 `/contents/:id` 等地址刷新可直接访问。

## 功能对齐

### 公开官网

| 路由 | 功能 |
| --- | --- |
| `/` | 首页 Hero、精选图片 / 轨道视觉、最新动态、重点项目、荣誉成果、加入入口；接口失败可重试 |
| `/about` | 固定页面 `about`：标题、摘要、封面、富文本正文 |
| `/join` | 固定页面 `join`：正文、封面、`joinUrl` 外部报名按钮 |
| `/news` | 新闻 / 活动列表：类型切换、分类筛选、计数、12 条 / 页分页 |
| `/projects` | 项目与成果列表：分类筛选、计数、分页 |
| `/people` | 师生风采列表：届别筛选、计数、分页 |
| `/contents/:id` | 详情：活动时间 / 地点 / 状态、成员信息、封面、正文、相册、图片来源、仓库 / 演示 / 原文链接、图片放大 |
| `/login` | 管理员登录、错误提示、登录后跳转；含“浏览官网”入口 |
| 其他 | 404 页面 |

### 管理后台

| 路由 | 功能 |
| --- | --- |
| `/admin` | 内容列表、类型 / 状态筛选、分页、新建、编辑、预览、发布、下架、移入回收站、恢复；修改密码；退出登录 |
| `/admin/edit/:id` | 新建 / 编辑：标题、摘要、wangEditor 正文、分类、排序、首页推荐、置顶、按届别归档、封面、相册、活动 / 项目 / 成员扩展字段、保存、保存并预览、保存并发布、离开未保存提醒 |
| `/admin/preview/:id` | 管理员草稿 / 已发布内容预览，显示预览提示 |
| `/admin`（操作记录） | 操作人、操作、对象、时间分页表格 |
| `/admin`（账号管理，超级管理员） | 账号列表、创建账号、启用 / 停用、调整角色、重置密码 |

### 接口契约

继续使用现有后端的全部接口，未新增或修改后端：

```text
GET  /api/public/pages/{key}
GET  /api/public/contents
GET  /api/public/contents/{id}
GET  /api/media/{id}
GET  /api/auth/csrf
POST /api/auth/login
GET  /api/auth/me
POST /api/auth/logout
POST /api/auth/password
GET  /api/admin/contents
GET  /api/admin/contents/{id}
POST /api/admin/contents
PUT  /api/admin/contents/{id}
POST /api/admin/contents/{id}/{publish|unpublish|trash|restore}
POST /api/admin/media
GET  /api/admin/accounts
POST /api/admin/accounts
PUT  /api/admin/accounts/{id}
POST /api/admin/accounts/{id}/password
GET  /api/admin/audit
```

请求体、版本号并发校验、CSRF、Cookie 会话、上传大小与格式限制全部沿用原实现。

## 视觉与交互亮点

- **深空星云背景**：多层极光色场、网格、噪点与暗角叠加，固定视差层不干扰阅读。
- **神经粒子场**：Canvas 绘制粒子与连线，跟随鼠标产生连接反馈；低性能 / 减少动效环境自动降级。
- **轨道主视觉**：封面图存在时切换为发光画框，没有封面时显示三层轨道、节点与核心标识。
- **滚动叙事**：`IntersectionObserver` 驱动的淡入 / 位移 / 模糊消散；顶部滚动进度线与详情阅读进度线。
- **3D 卡片**：鼠标跟随的轻微 tilt、径向高光、图片缩放与渐变描边；触屏自动关闭 tilt。
- **图片查看器**：封面临场、相册缩略图、左右方向键、ESC、双击放大，正文图片点击放大。
- **管理后台**：独立玻璃质感工作台、深色 Element Plus 表格 / 下拉 / 弹窗、渐变主按钮、移动端抽屉侧栏。
- **小屏适配**：`1100 / 900 / 760 / 460` 四档断点；移动端全屏导航、单列卡片、可横向滚动的表格。
- **可访问性**：跳转到主要内容、清晰焦点环、按钮 `aria` 状态、图片 alt、键盘操作、`prefers-reduced-motion` 降级。
- **不依赖外部字体 CDN**：使用 Bahnschrift / Avenir Next / 微软雅黑 / 苹方等系统字体栈，离线与国内网络环境不会阻塞首屏。

## 目录结构

```text
ai-club-nova/
├── public/
│   └── favicon.svg
├── screenshots/              # 视觉预览截图（示例数据）
├── src/
│   ├── api.ts                # Axios 实例、CSRF、类型与展示辅助
│   ├── main.ts               # 应用入口、Element Plus 深色主题、全局指令
│   ├── router.ts             # 与现有前端一致的路由与管理员守卫
│   ├── App.vue               # 全局背景、页面过渡、公共页头尾
│   ├── components/           # 卡片、页头、页脚、灯箱、后台表格等
│   ├── directives/motion.ts  # v-reveal / v-tilt
│   ├── stores/auth.ts        # Pinia 登录状态
│   ├── styles/               # base / public / admin 三套样式
│   └── views/                # 首页、列表、详情、静态页、登录、后台、编辑器、404
├── index.html
├── package.json
├── tsconfig.json
└── vite.config.ts
```

## 设计令牌

```css
--nx-bg: #05070d;        /* 深空底色 */
--nx-text: #eef3fb;      /* 主文字 */
--nx-muted: #8d9ab2;     /* 次文字 */
--nx-cyan: #5eead4;      /* 极光青 */
--nx-violet: #a78bfa;    /* 极光紫 */
--nx-lime: #d9f99d;      /* 荧光绿 */
--nx-grad: linear-gradient(112deg, #5eead4, #a78bfa 52%, #f0abfc);
```

字体：`Bahnschrift / DIN Alternate / Avenir Next / Segoe UI` 用于标题，`Noto Sans SC / 微软雅黑 / 苹方` 用于中文正文，`Cascadia Code / Consolas` 用于编号与标签。

## 验证记录

在 Windows + Node 24 + Chrome Headless 环境下完成：

- `npm run build`：`vue-tsc -b` 类型检查通过，Vite 生产构建通过。
- 真实浏览器（Chrome DevTools Protocol）逐项验证通过：
  - 新闻 13 条数据分页：第 1 页 12 条、第 2 页 1 条；
  - “社团活动”筛选后 2 条；
  - 详情页标题、4 项活动信息、3 张相册图片渲染；
  - 封面灯箱打开、ESC 关闭；
  - 管理员登录后跳转 `/admin`；
  - 后台内容列表 20 行、操作记录 20 行、账号管理 1 行；
  - 编辑器标题回填、wangEditor 挂载、保存并显示“已保存，官网内容已更新”；
  - 390 × 844 下移动端菜单打开、后台抽屉侧栏可用。
- 截图核对桌面 `1440 × 1000` 与移动端 `390 × 844` 布局。

## 说明

- 本目录只包含前端；后端、数据库迁移、部署脚本仍使用仓库现有实现。
- 仓库原有 `frontend/`、`backend/` 等代码未做任何修改。
- 截图中的人物 / 活动 / 项目均为界面测试用示例数据，请勿当作社团真实资料引用。
- 生产环境请替换 `vite.config.ts` 中的后端地址或在 Nginx 层代理 `/api`，并继续使用现有部署的 HTTPS / Cookie / 上传安全配置。
