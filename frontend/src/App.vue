<script setup lang="ts">
import { useRouter, useRoute } from 'vue-router'
import { theme } from 'ant-design-vue'
import {
  VideoCameraOutlined,
  SettingOutlined,
  CloudUploadOutlined,
  ClockCircleOutlined,
} from '@ant-design/icons-vue'

const router = useRouter()
const route = useRoute()

const navItems = [
  { path: '/dramas', label: '剧集', icon: VideoCameraOutlined },
  { path: '/settings/ai', label: 'AI配置', icon: SettingOutlined },
  { path: '/settings/storage', label: '存储', icon: CloudUploadOutlined },
  { path: '/settings/tasks', label: '任务追踪', icon: ClockCircleOutlined },
]

const isActive = (path: string) => route.path.startsWith(path)
</script>

<template>
  <a-config-provider
    :theme="{
      algorithm: theme.darkAlgorithm,
      token: {
        colorPrimary: '#6366f1',
        colorBgContainer: '#1a1a1a',
        colorBgElevated: '#242424',
        colorBgLayout: '#0f0f0f',
        colorBorder: '#2a2a2a',
        colorBorderSecondary: '#333333',
        borderRadius: 12,
        fontFamily: 'inherit',
      },
    }"
  >
    <a-app>
      <!-- 全局 Header -->
      <header class="global-header">
        <div class="header-inner">
          <div class="header-left">
            <h1 class="logo" @click="router.push('/')">
              <img class="logo-icon" src="/logo.png" alt="臻灵短剧" />
              <span class="logo-text hidden sm:inline">臻灵短剧</span>
            </h1>
            <nav class="nav-list">
              <button
                v-for="item in navItems"
                :key="item.path"
                @click="router.push(item.path)"
                :class="['nav-item', { active: isActive(item.path) }]"
              >
                <component :is="item.icon" class="nav-icon" />
                {{ item.label }}
              </button>
            </nav>
          </div>
        </div>
      </header>

      <!-- Main Content -->
      <main class="main-content">
        <router-view />
      </main>

      <!-- 全局 Message / Modal 容器 -->
    </a-app>
  </a-config-provider>
</template>

<style scoped>
.global-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 56px;
  background: rgba(26, 26, 26, 0.95);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid #2a2a2a;
  z-index: 1000;
}

.header-inner {
  height: 100%;
  margin: 0 auto;
  padding: 0 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  max-width: 1440px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: 0;
}

.logo {
  font-size: 18px;
  font-weight: 700;
  color: #f5f5f5;
  cursor: pointer;
  white-space: nowrap;
  transition: color 0.2s;
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}
.logo:hover { color: #fff; }

.logo-icon { width: 24px; height: 24px; object-fit: contain; }
.logo-text { font-size: 17px; }

.nav-list {
  display: flex;
  align-items: center;
  gap: 4px;
  overflow-x: auto;
  -ms-overflow-style: none;
  scrollbar-width: none;
}
.nav-list::-webkit-scrollbar { display: none; }

.nav-item {
  padding: 6px 14px;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 500;
  transition: all 0.2s;
  white-space: nowrap;
  flex-shrink: 0;
  cursor: pointer;
  border: none;
  background: transparent;
  color: #808080;
  display: inline-flex;
  align-items: center;
  gap: 5px;
}
.nav-icon { font-size: 15px; }
.nav-item:hover {
  color: #d0d0d0;
  background: #242424;
}
.nav-item.active {
  background: #6366f1;
  color: #fff;
  box-shadow: 0 4px 15px rgba(99, 102, 241, 0.25);
}

.main-content {
  padding-top: 72px;
  padding-bottom: 32px;
  min-height: 100vh;
  width: 100%;
}
.main-content > div {
  width: 100%;
  padding: 0 16px;
  margin: 0 auto;
  max-width: 1440px;
}
</style>

<!-- 全局样式：Ant Design Vue 暗色主题精细化优化 -->
<style>
/* ====== 1. 按钮系统（核心：icon 与文字垂直居中对齐）====== */
.ant-btn {
  display: inline-flex !important;
  align-items: center !important;
  justify-content: center !important;
  gap: 6px !important;
  line-height: 1.5 !important;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1) !important;
}
.ant-btn .anticon {
  display: inline-flex !important;
  align-items: center !important;
  justify-content: center !important;
}
/* link 类型按钮保持左对齐 */
.ant-btn-type-link,
a-button[type="link"] .ant-btn {
  justify-content: flex-start !important;
}

/* ====== 2. Tag 标签优化 ====== */
.ant-tag {
  border-radius: 8px !important;
  font-size: 12px !important;
  padding: 2px 8px !important;
  transition: all 0.2s !important;
}

/* ====== 3. Select 下拉框暗色适配 ====== */
.ant-select-dropdown {
  background: #1e1e1e !important;
  border: 1px solid #333 !important;
  box-shadow: 0 6px 16px rgba(0,0,0,0.45) !important;
}
.ant-select-item {
  color: #d0d0d0 !important;
}
.ant-select-item-option-active {
  background: #6366f115 !important;
}
.ant-select-item-option-selected {
  background: #6366f120 !important;
  color: #a78bfa !important;
}

/* ====== 4. Modal 弹窗暗色增强 ====== */
.ant-modal-header {
  background: transparent !important;
  border-bottom-color: #2a2a2a !important;
}
.ant-modal-close {
  color: #808080 !important;
}
.ant-modal-close:hover {
  color: #f5f5f5 !important;
}

/* ====== 5. Form 表单暗色微调 ====== */
.ant-input,
.ant-input-password .ant-input,
.ant-input-affix-wrapper,
.ant-select-selector,
.ant-picker {
  transition: border-color 0.2s, box-shadow 0.2s !important;
}
.ant-input:focus,
.ant-input-focused,
.ant-input-affixwrapper-focused,
.ant-select-focused .ant-select-selector,
.ant-picker-focused {
  border-color: #6366f1 !important;
  box-shadow: 0 0 0 2px rgba(99, 102, 241, 0.15) !important;
}

/* ====== 6. Tooltip 暗色背景 ====== */
.ant-tooltip-inner {
  background: #2a2a2a !important;
  color: #e0e0e0 !important;
  font-size: 12px !important;
}
.ant-tooltip-arrow::before {
  background: #2a2a2a !important;
}

/* ====== 7. Popconfirm 暗色适配 ====== */
.ant-popconfirm .ant-popover-inner {
  background: #242424 !important;
  border: 1px solid #333 !important;
}
.ant-popconfirm .ant-popover-message-title {
  color: #e0e0e0 !important;
}

/* ====== 8. Empty 空状态文字颜色 ====== */
.ant-empty-description {
  color: #606060 !important;
}

/* ====== 9. Spin 加载指示器颜色 ====== */
.ant-spin-dot-item {
  background: #6366f1 !important;
}
.ant-spin-text {
  color: #a0a0a0 !important;
}

/* ====== 10. Scrollbar 细腻滚动条 ====== */
::-webkit-scrollbar { width: 6px; height: 6px; }
::-webkit-scrollbar-track { background: transparent; }
::-webkit-scrollbar-thumb { background: #333; border-radius: 3px; }
::-webkit-scrollbar-thumb:hover { background: #555; }

/* ====== 11. Alert 组件暗色适配 ====== */
.ant-alert-info {
  background: #1a2340 !important;
  border-color: #2a3560 !important;
}
</style>
