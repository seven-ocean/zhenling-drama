<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { dramaApi } from '@/utils/request'
import { useRouter } from 'vue-router'

const router = useRouter()

const dramas = ref<any[]>([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(10)

const loadDramas = async () => {
  loading.value = true
  try {
    const res = await dramaApi.list({ pageNum: pageNum.value, pageSize: pageSize.value })
    if (res.code === 200) {
      dramas.value = res.data.records || []
    }
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const goToDetail = (id: string) => {
  router.push(`/drama/${id}`)
}

onMounted(() => {
  loadDramas()
})
</script>

<template>
  <div class="min-h-screen bg-[#0f0f0f]">
    <!-- Header -->
    <header class="fixed top-0 left-0 right-0 h-16 bg-[#1a1a1a] border-b border-[#2a2a2a] z-50">
      <div class="h-full max-w-7xl mx-auto px-6 flex items-center justify-between">
        <h1 class="text-xl font-semibold text-[#f5f5f5]">火宝短剧</h1>
      </div>
    </header>

    <!-- Main Content -->
    <main class="pt-24 px-6 pb-12 max-w-7xl mx-auto">
      <!-- Actions -->
      <div class="flex justify-between items-center mb-8">
        <h2 class="text-2xl font-bold text-[#f5f5f5]">剧集列表</h2>
      </div>

      <!-- Loading -->
      <div v-if="loading" class="flex items-center justify-center py-20">
        <div class="w-8 h-8 border-2 border-[#6366f1] border-t-transparent rounded-full animate-spin"></div>
      </div>

      <!-- Empty -->
      <div v-else-if="dramas.length === 0" class="flex flex-col items-center justify-center py-20 text-[#808080]">
        <svg class="w-16 h-16 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M7 4v16M17 4v16M3 8h4m10 0h4M3 12h18M3 16h4m10 0h4M4 20h16a1 1 0 001-1V5a1 1 0 00-1-1H4a1 1 0 00-1 1v14a1 1 0 001 1z"/>
        </svg>
        <p>暂无剧集，点击新建开始创作</p>
      </div>

      <!-- List -->
      <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        <div 
          v-for="drama in dramas" 
          :key="drama.id"
          @click="goToDetail(drama.id)"
          class="group bg-[#1a1a1a] rounded-2xl overflow-hidden border border-[#2a2a2a] hover:border-[#6366f1]/50 hover:shadow-card transition-all duration-300 cursor-pointer"
        >
          <!-- Cover -->
          <div class="aspect-video bg-[#242424] relative overflow-hidden">
            <img 
              v-if="drama.coverImage" 
              :src="drama.coverImage" 
              class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
            />
            <div v-else class="w-full h-full flex items-center justify-center">
              <svg class="w-12 h-12 text-[#404040]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M7 4v16M17 4v16M3 8h4m10 0h4M3 12h18M3 16h4m10 0h4M4 20h16a1 1 0 001-1V5a1 1 0 00-1-1H4a1 1 0 00-1 1v14a1 1 0 001 1z"/>
              </svg>
            </div>
            <!-- Status Badge -->
            <div class="absolute top-3 right-3 px-3 py-1 bg-[#0f0f0f]/80 rounded-full text-xs text-[#a0a0a0]">
              {{ drama.status === 'draft' ? '草稿' : drama.status }}
            </div>
          </div>
          
          <!-- Info -->
          <div class="p-5">
            <h3 class="text-lg font-medium text-[#f5f5f5] mb-2 line-clamp-1">{{ drama.title }}</h3>
            <p class="text-sm text-[#808080] line-clamp-2 mb-4">{{ drama.description || '暂无描述' }}</p>
            <div class="flex items-center justify-between text-sm text-[#606060]">
              <span>{{ drama.totalEpisodes || 0 }}集</span>
              <span>{{ drama.createdEpisodes || 0 }}已生成</span>
            </div>
          </div>
        </div>
      </div>
    </main>

    <!-- FAB -->
    <button 
      @click="goToDetail('new')"
      class="fixed bottom-8 right-8 w-14 h-14 bg-[#6366f1] rounded-full flex items-center justify-center shadow-hover hover:scale-110 transition-all duration-300"
    >
      <svg class="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
      </svg>
    </button>
  </div>
</template>