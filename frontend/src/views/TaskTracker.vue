<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { taskApi } from '@/utils/task'
import { message as AMessage } from 'ant-design-vue'
import {
  ClockCircleOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  SyncOutlined,
  DeleteOutlined,
  ReloadOutlined,
} from '@ant-design/icons-vue'

// 状态
const tasks = ref<any[]>([])
const loading = ref(false)
const filterStatus = ref('all')
const autoRefresh = ref(false)
let refreshTimer: ReturnType<typeof setInterval> | null = null

// 分页
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)

const statusOptions = [
  { value: 'all', label: '全部', color: '#808080', icon: ClockCircleOutlined },
  { value: 'pending', label: '等待中', color: '#f59e0b', icon: ClockCircleOutlined },
  { value: 'running', label: '执行中', color: '#3b82f6', icon: SyncOutlined },
  { value: 'completed', label: '已完成', color: '#22c55e', icon: CheckCircleOutlined },
  { value: 'failed', label: '失败', color: '#ef4444', icon: CloseCircleOutlined },
]

const filteredTasks = ref<any[]>([])

const loadTasks = async () => {
  loading.value = true
  try {
    const res = await taskApi.list({ pageSize: pageSize.value, pageNum: pageNum.value })
    if (res.code === 200) {
      const list = res.data?.records || res.data || []
      // 按状态排序：pending/running 在前
      tasks.value = list.sort((a: any, b: any) => {
        if ((a.status === 'pending' || a.status === 'running') && b.status !== 'pending' && b.status !== 'running') return -1
        if (b.status === 'pending' || b.status === 'running' && a.status !== 'pending' && a.status !== 'running') return 1
        return new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime()
      })
      total.value = res.data?.total || tasks.value.length
      applyFilter()
    }
  } catch (e) {
    console.error('Load tasks failed:', e)
  } finally {
    loading.value = false
  }
}

const onPageChange = (page: number, size: number) => {
  pageNum.value = page
  pageSize.value = size
  loadTasks()
}

const applyFilter = () => {
  if (filterStatus.value === 'all') {
    filteredTasks.value = tasks.value
  } else {
    filteredTasks.value = tasks.value.filter((t: any) => t.status === filterStatus.value)
  }
}

const deleteTask = async (id: string) => {
  try {
    await taskApi.delete(id)
    AMessage.success('已删除')
    await loadTasks()
  } catch (e: any) {
    AMessage.error(e?.message || '删除失败')
  }
}

// 自动刷新
const toggleAutoRefresh = () => {
  autoRefresh.value = !autoRefresh.value
  if (autoRefresh.value) {
    refreshTimer = setInterval(loadTasks, 5000) // 每5秒刷新
    AMessage.info('已开启自动刷新（5秒间隔）')
  } else {
    if (refreshTimer) clearInterval(refreshTimer)
    refreshTimer = null
    AMessage.info('已关闭自动刷新')
  }
}

onMounted(() => {
  loadTasks()
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
})

// 任务类型中文映射
const taskTypeMap: Record<string, string> = {
  video_generate: '视频生成',
  audio_generate: '配音生成',
  image_generate: '图片生成',
  storyboard_generate: '分镜拆解',
  grid_prompt: '宫格图提示词',
  tts_batch: '批量配音',
  video_batch: '批量视频生成',
  compose: '视频合成',
}
</script>

<template>
  <div class="space-y-4 sm:space-y-6 w-full max-w-5xl mx-auto">
    <!-- 标题栏 -->
    <div class="flex items-center justify-between">
      <h2 class="text-xl font-semibold text-[#f5f5f5] flex items-center gap-2">
        <ClockCircleOutlined />
        任务进度追踪
      </h2>
      <div class="flex items-center gap-2">
        <a-button :type="autoRefresh ? 'primary' : 'default'" size="small" @click="toggleAutoRefresh" class="!rounded-xl">
          <template #icon><SyncOutlined :class="{ 'animate-spin': autoRefresh }" /></template>
          {{ autoRefresh ? '自动刷新中' : '自动刷新' }}
        </a-button>
        <a-button size="small" @click="loadTasks" :loading="loading" class="!rounded-xl">
          <template #icon><ReloadOutlined /></template> 刷新
        </a-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="grid grid-cols-2 sm:grid-cols-4 gap-3">
      <div v-for="s in statusOptions" :key="s.value"
           class="bg-[#1a1a1a] rounded-xl border border-[#2a2a2a] p-3 sm:p-4 cursor-pointer transition-all hover:border-[#333]"
           @click="filterStatus = s.value; applyFilter()"
           :class="{ 'ring-2 ring-[#6366f1]/30': filterStatus === s.value }">
        <div class="flex items-center justify-between mb-2">
          <component :is="s.icon" :style="{ color: s.color, fontSize: '18px' }" />
          <span class="text-lg font-bold" :style="{ color: s.color }">
            {{ s.value === 'all' ? tasks.length : tasks.filter((t: any) => t.status === s.value).length }}
          </span>
        </div>
        <p class="text-xs text-[#606060]">{{ s.label }}</p>
      </div>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="flex justify-center py-12"><a-spin tip="加载中..." /></div>

    <!-- Empty -->
    <div v-else-if="filteredTasks.length === 0" class="text-center py-12">
      <a-empty description="暂无任务记录" />
    </div>

    <!-- Task List -->
    <div v-else class="space-y-2">
      <div v-for="task in filteredTasks" :key="task.id"
        class="bg-[#1a1a1a] rounded-xl border border-[#2a2a2a] p-4 sm:p-5 hover:border-[#333] transition-all group">

        <!-- 头部信息行 -->
        <div class="flex flex-col sm:flex-row sm:items-start justify-between gap-3 mb-3">
          <div class="min-w-0 flex-1">
            <div class="flex items-center gap-2 mb-1.5">
              <!-- 状态图标 -->
              <span v-if="task.status === 'completed'" class="w-2 h-2 rounded-full bg-green-400 animate-pulse"></span>
              <span v-else-if="task.status === 'running'" class="w-2 h-2 rounded-full bg-blue-400 animate-pulse"></span>
              <span v-else-if="task.status === 'failed'" class="w-2 h-2 rounded-full bg-red-400"></span>
              <span v-else class="w-2 h-2 rounded-full bg-yellow-400"></span>

              <span class="text-sm font-medium text-[#f5f5f5] truncate">
                {{ taskTypeMap[task.taskType] || task.taskType }}
              </span>

              <a-tag :color="getStatusColor(task.status)" size="small" class="!rounded-lg !font-medium shrink-0 ml-1">
                {{ getStatusLabel(task.status) }}
              </a-tag>
            </div>
            <p v-if="task.message" class="text-xs text-[#808080] truncate">{{ task.message }}</p>
          </div>

          <!-- 操作按钮 -->
          <a-popconfirm title="确定删除该任务记录？" ok-text="确定" cancel-text="取消" @confirm="deleteTask(task.id)">
            <a-button type="text" danger size="small" class="opacity-0 group-hover:opacity-100 !p-1.5 shrink-0">
              <template #icon><DeleteOutlined /></template>
            </a-button>
          </a-popconfirm>
        </div>

        <!-- 进度条 -->
        <div v-if="task.progress != null" class="space-y-1">
          <div class="flex items-center justify-between text-xs">
            <span class="text-[#606060]">进度</span>
            <span :style="{ color: getProgressColor(task.status) }">{{ task.progress }}%</span>
          </div>
          <div class="w-full h-1.5 rounded-full bg-[#242424] overflow-hidden">
            <div
              class="h-full rounded-full transition-all duration-700 ease-out"
              :class="getProgressClass(task.status)"
              :style="{ width: `${Math.min(task.progress, 100)}%` }"
            />
          </div>
        </div>

        <!-- 结果（完成时显示） -->
        <div v-if="task.result && task.status === 'completed'" class="mt-2 p-2 bg-green-500/5 rounded-lg border border-green-500/10">
          <p class="text-xs text-green-400 truncate" :title="task.result">结果: {{ task.result }}</p>
        </div>

        <!-- 错误信息（失败时显示） -->
        <div v-if="task.status === 'failed' && task.message" class="mt-2 p-2 bg-red-500/5 rounded-lg border border-red-500/10">
          <p class="text-xs text-red-400 truncate">{{ task.message }}</p>
        </div>

        <!-- 时间信息 -->
        <div class="flex items-center justify-between mt-2 pt-2 border-t border-[#242424]">
          <span class="text-[10px] text-[#505050]">ID: {{ task.id?.substring(0, 8) }}...</span>
          <span class="text-[10px] text-[#505050]">{{ formatTime(task.updatedAt) }}</span>
        </div>
      </div>
    </div>

    <!-- Pagination -->
    <div v-if="total > pageSize" class="flex justify-center pt-4 pb-8">
      <a-pagination
        :current="pageNum"
        :pageSize="pageSize"
        :total="total"
        :showSizeChanger="true"
        size="small"
        @change="onPageChange"
        :page-size-options="['20', '30', '50']"
        class="!text-[#a0a0a0]"
      />
    </div>
  </div>
</template>

<script lang="ts">
function getStatusColor(status: string): string {
  switch (status) {
    case 'completed': return '#16a34a15'
    case 'running': return '#1d4ed815'
    case 'failed': return '#dc262615'
    case 'pending': return '#ca8a0415'
    default: return '#52525b'
  }
}

function getStatusLabel(status: string): string {
  switch (status) {
    case 'pending': return '等待中'
    case 'running': return '执行中'
    case 'completed': return '已完成'
    case 'failed': return '失败'
    default: return status
  }
}

function getProgressColor(status: string): string {
  if (status === 'completed') return '#22c55e'
  if (status === 'running') return '#3b82f6'
  if (status === 'failed') return '#ef4444'
  return '#f59e0b'
}

function getProgressClass(status: string): string {
  if (status === 'completed') return '!bg-green-400'
  if (status === 'running') return '!bg-blue-400 animate-pulse-slow'
  if (status === 'failed') return '!bg-red-400'
  return '!bg-yellow-400'
}

function formatTime(time?: string | null): string {
  if (!time) return '-'
  try {
    const d = new Date(time)
    return `${d.getMonth()+1}/${d.getDate()} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
  } catch { return '-' }
}
</script>

<style scoped>
@keyframes pulse-slow {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.7; }
}
.animate-pulse-slow { animation: pulse-slow 2s cubic-bezier(0.4, 0, 0.6, 1) infinite; }

/* 分页组件暗色适配 */
:deep(.ant-pagination) { gap: 4px; }
:deep(.ant-pagination-item) {
  background: #1a1a1a !important;
  border-color: #2a2a2a !important;
  border-radius: 8px !important;
}
:deep(.ant-pagination-item a) { color: #a0a0a0 !important; }
:deep(.ant-pagination-item-active),
:deep(.ant-pagination-item-active a) {
  background: #6366f1 !important;
  border-color: #6366f1 !important;
  color: #fff !important;
}
:deep(.ant-pagination-prev .ant-pagination-item-link),
:deep(.ant-pagination-next .ant-pagination-item-link) {
  background: #1a1a1a !important;
  border-color: #2a2a2a !important;
  color: #a0a0a0 !important;
  border-radius: 8px !important;
}
</style>
