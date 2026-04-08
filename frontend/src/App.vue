<script setup lang="ts">
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()

const navItems = [
  { path: '/dramas', label: '剧集', icon: 'film' },
  { path: '/settings/ai', label: 'AI配置', icon: 'cpu' },
]

const isActive = (path: string) => route.path.startsWith(path)
</script>

<template>
  <div class="min-h-screen bg-[#0f0f0f]">
    <!-- Header -->
    <header class="fixed top-0 left-0 right-0 h-16 bg-[#1a1a1a] border-b border-[#2a2a2a] z-50">
      <div class="h-full max-w-7xl mx-auto px-6 flex items-center justify-between">
        <div class="flex items-center gap-6">
          <h1 class="text-xl font-semibold text-[#f5f5f5] cursor-pointer" @click="router.push('/')">火宝短剧</h1>
          <nav class="flex items-center gap-1">
            <button 
              v-for="item in navItems" 
              :key="item.path"
              @click="router.push(item.path)"
              :class="['px-3 py-1.5 rounded-lg text-sm transition-colors', isActive(item.path) ? 'bg-[#6366f1] text-white' : 'text-[#a0a0a0] hover:text-[#f5f5f5]']"
            >
              {{ item.label }}
            </button>
          </nav>
        </div>
      </div>
    </header>

    <!-- Main -->
    <main class="pt-24 px-6 pb-12 max-w-7xl mx-auto">
      <router-view />
    </main>
  </div>
</template>