<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { dramaApi, characterApi, sceneApi, storyboardApi } from '@/utils/request'

const route = useRoute()
const router = useRouter()

const drama = ref<any>(null)
const characters = ref<any[]>([])
const scenes = ref<any[]>([])
const storyboards = ref<any[]>([])
const activeTab = ref('characters')
const loading = ref(false)

const isNew = computed(() => route.params.id === 'new')

const tabs = [
  { key: 'characters', label: '角色' },
  { key: 'scenes', label: '场景' },
  { key: 'storyboards', label: '分镜' },
  { key: 'settings', label: '设置' },
]

const loadData = async () => {
  loading.value = true
  try {
    if (!isNew.value) {
      const dramaRes = await dramaApi.get(route.params.id as string)
      if (dramaRes.code === 200) {
        drama.value = dramaRes.data
      }
      const charRes = await characterApi.list(route.params.id as string)
      if (charRes.code === 200) {
        characters.value = charRes.data || []
      }
      const sceneRes = await sceneApi.list(route.params.id as string)
      if (sceneRes.code === 200) {
        scenes.value = sceneRes.data || []
      }
    }
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const saveDrama = async () => {
  if (!drama.value.title) return
  loading.value = true
  try {
    if (isNew.value) {
      const res = await dramaApi.create(drama.value)
      if (res.code === 200) {
        router.replace(`/drama/${res.data.id}`)
      }
    } else {
      await dramaApi.update(drama.value.id, drama.value)
    }
    await loadData()
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  if (!isNew.value) {
    loadData()
  } else {
    drama.value = { title: '', description: '', totalEpisodes: 1 }
  }
})
</script>

<template>
  <div class="min-h-screen bg-[#0f0f0f]">
    <!-- Header -->
    <header class="fixed top-0 left-0 right-0 h-16 bg-[#1a1a1a] border-b border-[#2a2a2a] z-50">
      <div class="h-full max-w-7xl mx-auto px-6 flex items-center justify-between">
        <div class="flex items-center gap-4">
          <button @click="router.back()" class="p-2 hover:bg-[#242424] rounded-xl transition-colors">
            <svg class="w-5 h-5 text-[#a0a0a0]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/>
            </svg>
          </button>
          <h1 class="text-xl font-semibold text-[#f5f5f5]">{{ isNew ? '新建剧集' : (drama?.title || '剧集详情') }}</h1>
        </div>
        <button 
          @click="saveDrama" 
          :disabled="loading || !drama?.title"
          class="px-6 py-2 bg-[#6366f1] hover:bg-[#5558e3] disabled:opacity-50 disabled:cursor-not-allowed rounded-xl text-white font-medium transition-colors"
        >
          保存
        </button>
      </div>
    </header>

    <!-- Content -->
    <main class="pt-24 px-6 pb-12 max-w-7xl mx-auto">
      <!-- New Drama Form -->
      <div v-if="isNew || !drama" class="max-w-2xl mx-auto">
        <div class="bg-[#1a1a1a] rounded-2xl border border-[#2a2a2a] p-8">
          <div class="space-y-6">
            <div>
              <label class="block text-sm text-[#a0a0a0] mb-2">剧集标题 *</label>
              <input 
                v-model="drama.title"
                type="text" 
                placeholder="输入剧集标题"
                class="w-full px-4 py-3 bg-[#242424] border border-[#2a2a2a] rounded-xl text-[#f5f5f5] placeholder-[#606060] focus:border-[#6366f1] focus:outline-none transition-colors"
              />
            </div>
            <div>
              <label class="block text-sm text-[#a0a0a0] mb-2">剧集描述</label>
              <textarea 
                v-model="drama.description"
                rows="4"
                placeholder="输入剧集描述"
                class="w-full px-4 py-3 bg-[#242424] border border-[#2a2a2a] rounded-xl text-[#f5f5f5] placeholder-[#606060] focus:border-[#6366f1] focus:outline-none transition-colors resize-none"
              ></textarea>
            </div>
            <div>
              <label class="block text-sm text-[#a0a0a0] mb-2">总集数</label>
              <input 
                v-model="drama.totalEpisodes"
                type="number" 
                min="1"
                class="w-full px-4 py-3 bg-[#242424] border border-[#2a2a2a] rounded-xl text-[#f5f5f5] focus:border-[#6366f1] focus:outline-none transition-colors"
              />
            </div>
          </div>
        </div>
      </div>

      <!-- Drama Detail -->
      <div v-else>
        <!-- Tabs -->
        <div class="flex gap-2 mb-6 overflow-x-auto pb-2">
          <button 
            v-for="tab in tabs" 
            :key="tab.key"
            @click="activeTab = tab.key"
            :class="[
              'px-5 py-2.5 rounded-xl font-medium transition-all whitespace-nowrap',
              activeTab === tab.key 
                ? 'bg-[#6366f1] text-white' 
                : 'bg-[#1a1a1a] text-[#a0a0a0] hover:text-[#f5f5f5] hover:bg-[#242424]'
            ]"
          >
            {{ tab.label }}
          </button>
        </div>

        <!-- Characters Tab -->
        <div v-if="activeTab === 'characters'" class="space-y-6">
          <div class="flex justify-between items-center">
            <h3 class="text-lg font-medium text-[#f5f5f5]">角色列表</h3>
            <button class="px-4 py-2 bg-[#6366f1] hover:bg-[#5558e3] rounded-xl text-white text-sm transition-colors">
              + 添加角色
            </button>
          </div>
          
          <div v-if="characters.length === 0" class="text-center py-12 text-[#606060]">
            暂无角色，点击添加开始创建
          </div>
          
          <div v-else class="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-6 gap-4">
            <div 
              v-for="char in characters" 
              :key="char.id"
              class="bg-[#1a1a1a] rounded-xl border border-[#2a2a2a] p-4 hover:border-[#6366f1]/50 transition-colors cursor-pointer"
            >
              <div class="aspect-square bg-[#242424] rounded-lg mb-3 overflow-hidden">
                <img v-if="char.imageUrl" :src="char.imageUrl" class="w-full h-full object-cover" />
                <div v-else class="w-full h-full flex items-center justify-center text-[#404040]">无图</div>
              </div>
              <p class="text-sm text-[#f5f5f5] text-center truncate">{{ char.name }}</p>
            </div>
          </div>
        </div>

        <!-- Scenes Tab -->
        <div v-if="activeTab === 'scenes'" class="space-y-6">
          <div class="flex justify-between items-center">
            <h3 class="text-lg font-medium text-[#f5f5f5]">场景列表</h3>
            <button class="px-4 py-2 bg-[#6366f1] hover:bg-[#5558e3] rounded-xl text-white text-sm transition-colors">
              + 添加场景
            </button>
          </div>
          
          <div v-if="scenes.length === 0" class="text-center py-12 text-[#606060]">
            暂无场景，点击添加开始创建
          </div>
          
          <div v-else class="grid grid-cols-2 md:grid-cols-4 gap-4">
            <div 
              v-for="scene in scenes" 
              :key="scene.id"
              class="bg-[#1a1a1a] rounded-xl border border-[#2a2a2a] overflow-hidden hover:border-[#6366f1]/50 transition-colors cursor-pointer"
            >
              <div class="aspect-video bg-[#242424]">
                <img v-if="scene.imageUrl" :src="scene.imageUrl" class="w-full h-full object-cover" />
              </div>
              <div class="p-3">
                <p class="text-sm text-[#f5f5f5] truncate">{{ scene.name }}</p>
                <p class="text-xs text-[#606060] truncate">{{ scene.location }}</p>
              </div>
            </div>
          </div>
        </div>

        <!-- Storyboards Tab -->
        <div v-if="activeTab === 'storyboards'" class="space-y-6">
          <div class="flex justify-between items-center">
            <h3 class="text-lg font-medium text-[#f5f5f5]">分镜列表</h3>
            <button class="px-4 py-2 bg-[#6366f1] hover:bg-[#5558e3] rounded-xl text-white text-sm transition-colors">
              + AI 生成
            </button>
          </div>
          
          <div class="text-center py-12 text-[#606060]">
            暂无分镜
          </div>
        </div>

        <!-- Settings Tab -->
        <div v-if="activeTab === 'settings'" class="max-w-2xl">
          <div class="bg-[#1a1a1a] rounded-2xl border border-[#2a2a2a] p-6 space-y-6">
            <div>
              <label class="block text-sm text-[#a0a0a0] mb-2">剧集标题</label>
              <input 
                v-model="drama.title"
                type="text" 
                class="w-full px-4 py-3 bg-[#242424] border border-[#2a2a2a] rounded-xl text-[#f5f5f5] focus:border-[#6366f1] focus:outline-none"
              />
            </div>
            <div>
              <label class="block text-sm text-[#a0a0a0] mb-2">描述</label>
              <textarea 
                v-model="drama.description"
                rows="4"
                class="w-full px-4 py-3 bg-[#242424] border border-[#2a2a2a] rounded-xl text-[#f5f5f5] focus:border-[#6366f1] focus:outline-none resize-none"
              ></textarea>
            </div>
            <button @click="saveDrama" class="w-full py-3 bg-[#6366f1] hover:bg-[#5558e3] rounded-xl text-white font-medium">
              保存修改
            </button>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>