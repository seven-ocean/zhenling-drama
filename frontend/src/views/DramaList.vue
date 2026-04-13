<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { dramaApi } from '@/utils/request'
import { useRouter } from 'vue-router'
import {
  PlusOutlined,
  VideoCameraOutlined,
  FileImageOutlined,
} from '@ant-design/icons-vue'

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
  <div class="w-full">
    <!-- 页面标题 + 新增按钮 -->
    <div class="flex justify-between items-center mb-8">
      <h2 class="text-2xl font-bold text-[#f5f5f5]">剧集列表</h2>
      <a-button type="primary" size="large" @click="goToDetail('new')">
        <template #icon><PlusOutlined /></template>
        新增剧集
      </a-button>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="flex items-center justify-center py-20">
      <a-spin size="large" tip="加载中..." />
    </div>

    <!-- Empty -->
    <div v-else-if="dramas.length === 0" class="flex flex-col items-center justify-center py-20 text-[#808080]">
      <a-empty description="暂无剧集，点击上方按钮新建" :image-style="{ opacity: 0.4 }">
        <template #extra>
          <a-button type="primary" @click="goToDetail('new')">
            <template #icon><PlusOutlined /></template>
            创建剧集
          </a-button>
        </template>
      </a-empty>
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
            <VideoCameraOutlined style="font-size: 48px; color: #404040;" />
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
  </div>
</template>

<style scoped>
.line-clamp-1 { overflow: hidden; display: -webkit-box; -webkit-box-orient: vertical; -webkit-line-clamp: 1; }
.line-clamp-2 { overflow: hidden; display: -webkit-box; -webkit-box-orient: vertical; -webkit-line-clamp: 2; }
.hover\:shadow-card:hover {
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3), 0 0 15px rgba(99, 102, 241, 0.08);
}
</style>
