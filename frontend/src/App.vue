<script setup lang="ts">
import { useRouter, useRoute } from 'vue-router'
import { theme } from 'ant-design-vue'

const router = useRouter()
const route = useRoute()

const navItems = [
  { path: '/dramas', label: '剧集', icon: '🎬' },
  { path: '/settings/ai', label: 'AI配置', icon: '⚡' },
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
              <span class="logo-icon">🔥</span>
              <span class="logo-text hidden sm:inline">臻灵短剧</span>
            </h1>
            <nav class="nav-list">
              <button
                v-for="item in navItems"
                :key="item.path"
                @click="router.push(item.path)"
                :class="['nav-item', { active: isActive(item.path) }]"
              >
                {{ item.icon }} {{ item.label }}
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

.logo-icon { font-size: 20px; }
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
}
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
