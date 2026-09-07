<template>
  <router-view />
</template>

<script setup>
</script>

<style>
/* ============ 设计 Token ============ */
:root {
  --brand: #2f6bff;
  --sidebar-bg: #1c2333;
  --sidebar-hover: rgba(255, 255, 255, .06);
  --content-bg: #f5f6fa;
  --border: #eef0f4;
}

* { box-sizing: border-box; }
html, body, #app { height: 100%; margin: 0; padding: 0; }
body {
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", sans-serif;
  background: var(--content-bg);
  color: #333;
  font-size: 14px;
}

/* ============ 主布局 ============ */
.admin-layout { display: flex; height: 100vh; overflow: hidden; }
.sidebar {
  width: 224px; flex: none; background: var(--sidebar-bg);
  display: flex; flex-direction: column; transition: width .2s;
}
.sidebar .side-brand {
  height: 56px; display: flex; align-items: center; gap: 10px;
  padding: 0 18px; color: #fff; flex: none;
}
.sidebar .side-brand .logo-icon {
  width: 30px; height: 30px; border-radius: 8px; flex: none;
  background: linear-gradient(135deg, #4d8bff, #2f6bff);
  display: flex; align-items: center; justify-content: center; font-weight: 700; font-size: 15px;
}
.sidebar .side-brand b { font-size: 15px; letter-spacing: .5px; white-space: nowrap; }
.side-menu { flex: 1; overflow-y: auto; padding: 6px 10px 20px; }
.side-menu::-webkit-scrollbar { width: 4px; }
.side-menu::-webkit-scrollbar-thumb { background: rgba(255,255,255,.15); border-radius: 4px; }
.side-menu .menu-item {
  display: flex; align-items: center; gap: 10px;
  height: 42px; padding: 0 14px; margin-bottom: 3px;
  color: rgba(255,255,255,.72); border-radius: 8px; cursor: pointer;
  font-size: 14px; user-select: none; position: relative;
}
.side-menu .menu-item:hover { background: var(--sidebar-hover); color: #fff; }
.side-menu .menu-item.active { background: var(--brand); color: #fff; font-weight: 500; box-shadow: 0 4px 12px rgba(47,107,255,.35); }
.side-menu .menu-item .el-icon { font-size: 17px; }
.side-menu .menu-group-title {
  padding: 14px 12px 6px; font-size: 11px; color: rgba(255,255,255,.35);
  letter-spacing: 1px; user-select: none;
}
.side-menu .menu-item .badge {
  margin-left: auto; min-width: 18px; height: 18px; line-height: 18px; padding: 0 5px;
  background: #ff4d4f; color: #fff; border-radius: 9px; font-size: 11px; text-align: center;
}
.main-area { flex: 1; display: flex; flex-direction: column; min-width: 0; }
.topbar {
  height: 56px; background: #fff; flex: none;
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 20px; border-bottom: 1px solid var(--border);
}
.topbar .crumbs { display: flex; align-items: center; gap: 8px; font-size: 13px; color: #909399; }
.topbar .crumbs b { color: #333; font-weight: 600; }
.topbar .right { display: flex; align-items: center; gap: 6px; }
.topbar .right .bell { position: relative; width: 36px; height: 36px; border-radius: 8px; display: flex; align-items: center; justify-content: center; cursor: pointer; color: #606266; }
.topbar .right .bell:hover { background: #f2f3f5; }
.topbar .right .bell .dot {
  position: absolute; top: 6px; right: 6px; min-width: 15px; height: 15px; line-height: 15px; padding: 0 4px;
  background: #ff4d4f; color: #fff; border-radius: 8px; font-size: 10px; text-align: center;
}
.topbar .user { display: flex; align-items: center; gap: 8px; padding: 4px 8px; border-radius: 8px; cursor: pointer; }
.topbar .user:hover { background: #f2f3f5; }
.topbar .avatar {
  width: 30px; height: 30px; border-radius: 50%; flex: none;
  background: linear-gradient(135deg, #4d8bff, #2f6bff); color: #fff;
  display: flex; align-items: center; justify-content: center; font-size: 13px; font-weight: 600;
}
.content { flex: 1; overflow-y: auto; padding: 18px 20px 30px; }

/* ============ 通用组件 ============ */
.page-card { background: #fff; border-radius: 10px; border: 1px solid var(--border); padding: 18px 20px; }
.page-card + .page-card { margin-top: 14px; }
.toolbar { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; margin-bottom: 14px; }
.toolbar .spacer { flex: 1; }
.stat-cards { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin-bottom: 14px; }
.stat-card {
  background: #fff; border-radius: 10px; border: 1px solid var(--border);
  padding: 18px 20px; display: flex; align-items: center; gap: 16px;
}
.stat-card .ico {
  width: 48px; height: 48px; border-radius: 12px; flex: none;
  display: flex; align-items: center; justify-content: center; font-size: 22px; color: #fff;
}
.stat-card .num { font-size: 24px; font-weight: 700; line-height: 1.2; }
.stat-card .lbl { font-size: 13px; color: #909399; margin-top: 2px; }
.stat-card .trend { font-size: 12px; margin-top: 6px; }
.chart-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
.chart-box { background: #fff; border-radius: 10px; border: 1px solid var(--border); padding: 16px 18px; }
.chart-box .chart-title { font-size: 15px; font-weight: 600; margin-bottom: 12px; display: flex; align-items: center; justify-content: space-between; }
.chart { width: 100%; height: 280px; }

.cover-thumb {
  width: 56px; height: 40px; border-radius: 6px; background: linear-gradient(135deg, #e6edff, #d4e0ff);
  display: flex; align-items: center; justify-content: center; font-size: 12px; color: #2f6bff; font-weight: 600; flex: none;
}
.avatar-circle {
  width: 34px; height: 34px; border-radius: 50%; flex: none;
  display: inline-flex; align-items: center; justify-content: center;
  color: #fff; font-size: 13px; font-weight: 600;
}
.tag-status { font-weight: 500; }
.muted { color: #909399; font-size: 12px; }
.cell-line { line-height: 1.8; }
.form-tip { font-size: 12px; color: #909399; line-height: 1.7; }
.media-placeholder {
  height: 150px; border: 1px dashed #d5dbe8; border-radius: 8px; background: #f8f9fc;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  color: #a8b0c3; gap: 6px; cursor: pointer; font-size: 12px;
}
.pager-wrap { display: flex; justify-content: flex-end; margin-top: 14px; }
.review-item { display: flex; gap: 14px; padding: 14px 0; border-bottom: 1px solid #f2f3f7; }
.review-item:last-child { border-bottom: none; }
.review-item .avatar-circle { width: 40px; height: 40px; font-size: 15px; }
.review-item .body { flex: 1; min-width: 0; }
.review-item .body .name { font-weight: 600; font-size: 14px; display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.review-item .body .content { margin-top: 6px; font-size: 13px; color: #4a4f58; line-height: 1.7; }
.review-item .body .meta { margin-top: 8px; font-size: 12px; color: #a8b0c3; display: flex; gap: 14px; align-items: center; flex-wrap: wrap; }
.review-item .actions { display: flex; align-items: center; gap: 8px; }
.thumb-imgs { display: flex; gap: 6px; margin-top: 8px; }
.thumb-imgs .ti { width: 64px; height: 64px; border-radius: 6px; background: #eef1f7; display: flex; align-items: center; justify-content: center; color: #9aa4bd; font-size: 18px; }

/* 登录页 */
.login-page {
  min-height: 100vh; display: flex;
  background: linear-gradient(135deg, #1c2333 0%, #22304f 55%, #2f6bff 130%);
}
.login-brand {
  flex: 1.1; display: flex; flex-direction: column; justify-content: center;
  padding: 0 6%; color: #fff;
}
.login-brand .logo { display: flex; align-items: center; gap: 12px; margin-bottom: 28px; }
.login-brand .logo-icon {
  width: 52px; height: 52px; border-radius: 14px;
  background: linear-gradient(135deg, #4d8bff, #2f6bff);
  display: flex; align-items: center; justify-content: center;
  font-size: 26px; font-weight: 700; box-shadow: 0 8px 24px rgba(47,107,255,.4);
}
.login-brand h1 { font-size: 30px; margin: 0 0 6px; letter-spacing: 1px; }
.login-brand .sub { font-size: 15px; color: rgba(255,255,255,.72); margin-bottom: 34px; }
.login-brand .feat { display: grid; grid-template-columns: 1fr 1fr; gap: 14px 24px; }
.login-brand .feat .item { display: flex; gap: 10px; align-items: flex-start; }
.login-brand .feat .item .dot {
  width: 30px; height: 30px; border-radius: 8px; flex: none;
  background: rgba(255,255,255,.12); display: flex; align-items: center; justify-content: center;
}
.login-brand .feat .item b { display: block; font-size: 14px; font-weight: 600; }
.login-brand .feat .item span { font-size: 12px; color: rgba(255,255,255,.6); }
.login-panel { flex: 0 0 460px; display: flex; align-items: center; justify-content: center; padding: 40px 48px; }
.login-card {
  width: 100%; background: #fff; border-radius: 16px;
  padding: 40px 36px 30px; box-shadow: 0 20px 60px rgba(0,0,0,.22);
}
.login-card h2 { margin: 0 0 4px; font-size: 22px; }
.login-card .tip { color: #909399; font-size: 13px; margin-bottom: 26px; }
.login-card .hint {
  margin-top: 18px; padding: 10px 12px; background: #f4f7ff; border-radius: 8px;
  font-size: 12px; color: #5a6b9e; line-height: 1.7;
}
.login-card .hint code { background: #e4ecff; padding: 1px 6px; border-radius: 4px; }

/* Element Plus 微调 */
.el-table { --el-table-header-bg-color: #f7f8fa; }

@media (max-width: 1100px) {
  .stat-cards { grid-template-columns: repeat(2, 1fr); }
  .chart-grid { grid-template-columns: 1fr; }
  .login-brand { display: none; }
  .login-panel { flex: 1; }
}
</style>
