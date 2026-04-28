<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { storyboardApi, characterApi, sceneApi, videoApi } from '@/utils/request'
import { aiApi } from '@/utils/ai'
import { episodeExportApi } from '@/utils/episodeExport'
import { message as AMessage } from 'ant-design-vue'
import {
  ArrowLeftOutlined,
  DeleteOutlined,
  UserOutlined,
  PictureOutlined,
  RobotOutlined,
  SendOutlined,
  VideoCameraOutlined,
  AppstoreOutlined,
  ExportOutlined,
  DownloadOutlined,
  HistoryOutlined,
  PlayCircleOutlined,
  ReloadOutlined,
} from '@ant-design/icons-vue'

const route = useRoute()
const router = useRouter()

const dramaId = route.params.dramaId as string
const activeTab = ref('storyboard')

// ====== 分镜数据 ======
const storyboards = ref<any[]>([])
const loadingSb = ref(false)

// ====== AI生成 ======
const scriptText = ref('')
const generatingSb = ref(false)

// ====== 整集导出 ======
const exporting = ref(false)
const exportHistory = ref<any[]>([])
const loadingHistory = ref(false)
const showExportPanel = ref(false)
const episodeNumber = ref(1)

// 加载导出历史
const loadExportHistory = async () => {
  loadingHistory.value = true
  try {
    const res = await episodeExportApi.list(dramaId, episodeNumber.value, 1, 5)
    if (res.code === 200) {
      exportHistory.value = res.data?.records || []
    }
  } catch (e) {
    console.error(e)
  } finally {
    loadingHistory.value = false
  }
}

// 导出整集
const exportEpisode = async () => {
  if (storyboards.value.length === 0) {
    AMessage.warning('没有分镜，无法导出')
    return
  }
  
  exporting.value = true
  try {
    const res = await episodeExportApi.exportEpisode(dramaId, episodeNumber.value)
    if (res.code === 200 && res.data) {
      AMessage.success('整集导出成功')
      // 刷新历史记录
      await loadExportHistory()
      // 如果成功且有URL，提供下载
      if (res.data.exportUrl) {
        window.open(res.data.exportUrl, '_blank')
      }
    } else {
      AMessage.error(res.message || '导出失败')
    }
  } catch (e: any) {
    AMessage.error(e?.message || '导出失败，请检查FFmpeg是否安装')
  } finally {
    exporting.value = false
  }
}

// 格式化时长
const formatDuration = (seconds?: number) => {
  if (!seconds) return '--:--'
  const mins = Math.floor(seconds / 60)
  const secs = Math.floor(seconds % 60)
  return `${mins}:${secs.toString().padStart(2, '0')}`
}

// 格式化时间
const formatTime = (time?: string) => {
  if (!time) return ''
  const date = new Date(time)
  return `${date.getMonth() + 1}/${date.getDate()} ${date.getHours()}:${date.getMinutes().toString().padStart(2, '0')}`
}

// 打开URL
const openUrl = (url: string) => {
  window.open(url, '_blank')
}

const generateFromScript = async () => {
  if (!scriptText.value.trim()) { AMessage.warning('请输入剧本内容'); return }
  generatingSb.value = true
  try {
    const res = await aiApi.generateStoryboards(dramaId, scriptText.value, 1)
    if (res.code === 200 && res.data) {
      storyboards.value = res.data
      AMessage.success(`成功拆解 ${res.data.length} 个分镜头`)
    } else {
      AMessage.error(res.message || 'AI拆解失败')
    }
  } catch (e: any) {
    AMessage.error(e?.message || 'AI拆解失败，请检查配置')
  } finally {
    generatingSb.value = false
  }
}

// 加载已有分镜
const loadStoryboards = async () => {
  loadingSb.value = true
  try {
    const res = await storyboardApi.list(dramaId, 1)
    if (res.code === 200) storyboards.value = res.data || []
  } catch (e: any) {
    console.error(e)
  } finally {
    loadingSb.value = false
  }
}

// 分镜编辑弹窗
const sbEditorOpen = ref(false)
const editingSb = ref<any>(null)
const sbForm = ref<any>({})

const shotTypes = [
  { value: 'wide', label: '全景' },
  { value: 'medium', label: '中景' },
  { value: 'close-up', label: '近景' },
  { value: 'extreme-close-up', label: '特写' },
]

// 获取分镜关联的角色和场景图片
const relatedCharImage = ref('')
const relatedSceneImage = ref('')
const relatedCharName = ref('')
const relatedSceneName = ref('')

const openSbEditor = (shot?: any) => {
  editingSb.value = shot || null
  sbForm.value = shot ? { ...shot } : {}
  
  // 加载关联的角色和场景图片
  if (shot?.characterId) {
    const char = characters.value.find(c => c.id === shot.characterId)
    relatedCharImage.value = char?.imageUrl || ''
    relatedCharName.value = char?.name || shot.characterName || '未知角色'
  } else {
    relatedCharImage.value = ''
    relatedCharName.value = shot?.characterName || ''
  }
  
  if (shot?.sceneId) {
    const scene = scenes.value.find(s => s.id === shot.sceneId)
    relatedSceneImage.value = scene?.imageUrl || ''
    relatedSceneName.value = scene?.name || '未知场景'
  } else {
    relatedSceneImage.value = ''
    relatedSceneName.value = ''
  }
  
  sbEditorOpen.value = true
}

// 选择角色后：自动更新关联信息 + 回填 characterImageUrl
const onSbSelectCharacter = (characterId: string) => {
  sbForm.value.characterId = characterId
  const char = characters.value.find(c => c.id === characterId)
  if (char) {
    relatedCharImage.value = char.imageUrl || ''
    relatedCharName.value = char.name || ''
    sbForm.value.characterName = char.name
    sbForm.value.characterImageUrl = char.imageUrl || ''
  }
}

// 选择场景后：自动更新关联信息 + 回填 sceneImageUrl
const onSbSelectScene = (sceneId: string) => {
  sbForm.value.sceneId = sceneId
  const scene = scenes.value.find(s => s.id === sceneId)
  if (scene) {
    relatedSceneImage.value = scene.imageUrl || ''
    relatedSceneName.value = scene.name || ''
    sbForm.value.sceneName = scene.name
    sbForm.value.sceneImageUrl = scene.imageUrl || ''
  }
}

const saveShot = async () => {
  if (!editingSb.value?.id) return
  try {
    const res = await storyboardApi.update(editingSb.value.id, sbForm.value)
    if (res.code === 200) {
      const idx = storyboards.value.findIndex(s => s.id === editingSb.value!.id)
      if (idx >= 0) storyboards.value[idx] = res.data
      AMessage.success('分镜已更新')
    }
    sbEditorOpen.value = false
  } catch (e: any) { AMessage.error(e?.message || '保存失败') }
}

const deleteShot = async (id: string) => {
  try {
    await storyboardApi.delete(id)
    storyboards.value = storyboards.value.filter(s => s.id !== id)
    AMessage.success('已删除')
    // 重新加载列表确保数据同步
    await loadStoryboards()
  } catch (e: any) { AMessage.error(e?.message || '删除失败') }
}

// 跳转到视频生成页面（媒体工作室）
const goToVideoGeneration = (shot: any) => {
  // 构建查询参数，传递分镜信息
  const queryParams = new URLSearchParams()
  queryParams.set('storyboardId', shot.id)
  queryParams.set('action', shot.action || '')
  if (shot.gridImageUrl) queryParams.set('gridImageUrl', shot.gridImageUrl)
  if (shot.characterImageUrl) queryParams.set('characterImageUrl', shot.characterImageUrl)
  if (shot.sceneImageUrl) queryParams.set('sceneImageUrl', shot.sceneImageUrl)

  // 跳转到媒体工作室页面
  router.push({
    path: `/media/${dramaId}`,
    query: { tab: 'video', ...Object.fromEntries(queryParams) }
  })
  AMessage.success('已切换到视频生成页面')
}

// ====== 角色图生成 ======
const characters = ref<any[]>([])
const generatingCharImg = ref<string | null>(null)

const loadCharacters = async () => {
  try {
    const res = await characterApi.list(dramaId)
    if (res.code === 200) characters.value = res.data || []
  } catch (e) { console.error(e) }
}

const generateCharImage = async (char: any) => {
  if (!char.appearancePrompt && !window.confirm('该角色未设置外观提示词，是否使用角色名生成？')) return
  generatingCharImg.value = char.id
  try {
    const prompt = char.appearancePrompt || `Character portrait of ${char.name}, anime style, detailed`
    const res = await aiApi.generateCharacterImage({
      dramaId, characterId: char.id, prompt,
    })
    if (res.code === 200 && res.data) {
      const idx = characters.value.findIndex(c => c.id === char.id)
      if (idx >= 0) characters.value[idx].imageUrl = res.data.fileUrl
      AMessage.success(`${char.name} 角色图已生成`)
    } else {
      AMessage.error(res.message || '生成失败')
    }
  } catch (e: any) { AMessage.error(e?.message || '图片生成失败') }
  finally { generatingCharImg.value = null }
}

// ====== 场景图生成 ======
const scenes = ref<any[]>([])
const generatingSceneImg = ref<string | null>(null)

const loadScenes = async () => {
  try {
    const res = await sceneApi.list(dramaId)
    if (res.code === 200) scenes.value = res.data || []
  } catch (e) { console.error(e) }
}

const generateSceneImg = async (scene: any) => {
  if (!scene.prompt && !window.confirm('该场景未设置提示词，是否使用场景名生成？')) return
  generatingSceneImg.value = scene.id
  try {
    const prompt = scene.prompt || `Anime background scene of ${scene.name}, detailed, cinematic`
    const res = await aiApi.generateSceneImage({
      dramaId, sceneId: scene.id, prompt,
    })
    if (res.code === 200 && res.data) {
      const idx = scenes.value.findIndex(s => s.id === scene.id)
      if (idx >= 0) scenes.value[idx].imageUrl = res.data.fileUrl
      AMessage.success(`${scene.name} 场景图已生成`)
    } else {
      AMessage.error(res.message || '生成失败')
    }
  } catch (e: any) { AMessage.error(e?.message || '图片生成失败') }
  finally { generatingSceneImg.value = null }
}

// ====== 视频生成 ======
const videoImageUrl = ref('')
const generatingVideo = ref(false)
const videos = ref<any[]>([])
const videosLoading = ref(false)
const selectedVideoModel = ref('MiniMax-Hailuo-2.3')
const playingVideoId = ref<string | null>(null)

// ====== 参考图 ======
const refImages = ref<Record<string, any>>({}) // storyboardId → Asset
const generatingRefImg = ref<string | null>(null) // 正在生成参考图的分镜ID
const refPreviewOpen = ref(false)
const refPreviewImage = ref<any>(null)

// 视频模型选项
const videoModels = [
  { value: 'MiniMax-Hailuo-2.3', label: 'MiniMax 海螺 2.3' },
  { value: 'video-01', label: 'MiniMax Video-01' },
]

// 生成视频
const generateVideo = async () => {
  if (!videoImageUrl.value.trim()) { AMessage.warning('请输入参考图片URL'); return }
  generatingVideo.value = true
  try {
    const res = await videoApi.generate({
      dramaId,
      episodeNumber: episodeNumber.value,
      storyboardId: '',
      imageUrl: videoImageUrl.value,
      model: selectedVideoModel.value,
    })
    if (res.code === 200 && res.data) {
      videos.value.unshift(res.data)
      AMessage.success('视频任务已提交')
      if (res.data.status === 'processing') {
        AMessage.info('视频正在异步生成中，请稍后查看')
      }
      videoImageUrl.value = ''
    } else {
      AMessage.error(res.message || '视频生成失败')
    }
  } catch (e: any) {
    AMessage.error(e?.message || '视频生成失败，请检查AI配置')
  } finally {
    generatingVideo.value = false
  }
}

// 加载视频列表
const loadVideos = async () => {
  videosLoading.value = true
  try {
    const res = await videoApi.listByDrama(dramaId)
    if (res.code === 200) videos.value = res.data || []
  } catch (e) { console.error(e) }
  finally { videosLoading.value = false }
}

// 删除视频
const deleteVideo = async (id: string) => {
  try {
    await videoApi.delete(id)
    videos.value = videos.value.filter(v => v.id !== id)
    AMessage.success('已删除')
  } catch (e: any) { AMessage.error(e?.message || '删除失败') }
}

// 播放/停止视频预览
const togglePlayVideo = (id: string) => {
  playingVideoId.value = playingVideoId.value === id ? null : id
}

// ====== 参考图 ======
const loadReferenceImages = async () => {
  // 加载所有分镜已有的参考图
  for (const shot of storyboards.value) {
    if (shot.id) {
      try {
        const res = await aiApi.getReference(shot.id)
        if (res.code === 200 && res.data) {
          refImages.value[shot.id] = res.data
        }
      } catch { /* ignore */ }
    }
  }
}

const generateRefImage = async (shot: any) => {
  if (!shot.id) return
  generatingRefImg.value = shot.id
  try {
    const res = await aiApi.generateReference({ storyboardId: shot.id, forceRegenerate: false })
    if (res.code === 200 && res.data) {
      refImages.value[shot.id] = res.data
      AMessage.success('参考图已生成')
    } else {
      AMessage.error(res.message || '生成失败')
    }
  } catch (e: any) { AMessage.error(e?.message || '参考图生成失败') }
  finally { generatingRefImg.value = null }
}

const forceRegenerateRefImage = async (shot: any) => {
  if (!shot.id) return
  generatingRefImg.value = shot.id
  try {
    const res = await aiApi.generateReference({ storyboardId: shot.id, forceRegenerate: true })
    if (res.code === 200 && res.data) {
      refImages.value[shot.id] = res.data
      AMessage.success('参考图已重新生成')
    } else {
      AMessage.error(res.message || '生成失败')
    }
  } catch (e: any) { AMessage.error(e?.message || '参考图生成失败') }
  finally { generatingRefImg.value = null }
}

const previewRefImage = (shot: any) => {
  if (refImages.value[shot.id]) {
    refPreviewImage.value = { ...refImages.value[shot.id], shot }
    refPreviewOpen.value = true
  }
}

const deleteRefImage = async (shot: any) => {
  if (!shot.id) return
  try {
    await aiApi.deleteReference(shot.id)
    delete refImages.value[shot.id]
    refPreviewOpen.value = false
    AMessage.success('参考图已删除')
  } catch (e: any) { AMessage.error(e?.message || '删除失败') }
}

// 轮询处理中的视频
let pollTimer: ReturnType<typeof setInterval> | null = null
const pollVideos = () => {
  const hasProcessing = videos.value.some((v: any) => v.status === 'processing')
  if (hasProcessing) {
    videoApi.poll().then(() => loadVideos()).catch(() => {})
  }
}

// 状态标签映射
const statusColorMap: Record<string, string> = {
  completed: '#16a34a15',
  processing: '#ca8a0415',
  pending: '#3b82f615',
  failed: '#dc262615',
}
const statusTextColorMap: Record<string, string> = {
  completed: '!text-green-400',
  processing: '!text-yellow-400',
  pending: '!text-blue-400',
  failed: '!text-red-400',
}
const statusLabelMap: Record<string, string> = {
  completed: '已完成',
  processing: '处理中',
  pending: '等待中',
  failed: '失败',
}

onMounted(async () => {
  // 安全防护：防止组件重复挂载导致多个并行定时器
  if (pollTimer) { clearInterval(pollTimer); pollTimer = null }

  await Promise.all([loadStoryboards(), loadCharacters(), loadScenes(), loadExportHistory(), loadVideos()])
  // 加载已有参考图
  await loadReferenceImages()
  // 每5秒轮询一次视频状态
  pollTimer = setInterval(pollVideos, 5000)
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
})
</script>

<template>
  <div class="w-full space-y-4 sm:space-y-6">
    <!-- 页面标题 -->
    <div class="flex items-center gap-3">
      <a-button type="text" @click="router.back()" class="!p-2 !text-[#a0a0a0] hover:!bg-[#242424] rounded-xl shrink-0">
        <ArrowLeftOutlined />
      </a-button>
      <h1 class="text-lg sm:text-xl font-semibold text-[#f5f5f5] truncate">工作台</h1>
    </div>

    <!-- Tab Bar + 整集导出 -->
    <div class="flex flex-col sm:flex-row sm:items-center gap-3">
      <div class="flex gap-2 overflow-x-auto pb-1 no-scrollbar flex-1">
        <a-button
          v-for="tab in [
            { key: 'storyboard', label: '分镜编辑器', icon: VideoCameraOutlined },
            { key: 'character', label: '角色图', icon: UserOutlined },
            { key: 'scene', label: '场景图', icon: AppstoreOutlined },
            { key: 'video', label: '视频生成', icon: PlayCircleOutlined }
          ]"
          :key="tab.key"
          :type="activeTab === tab.key ? 'primary' : 'default'"
          size="small"
          @click="activeTab = tab.key"
          :class="[
            '!rounded-xl !font-medium whitespace-nowrap shrink-0',
            activeTab !== tab.key && '!bg-[#1a1a1a] !border-[#2a2a2a] !text-[#a0a0a0] hover:!text-[#f5f5f5] hover:!bg-[#242424]'
          ]"
        >
          <template #icon><component :is="tab.icon" /></template>
          {{ tab.label }}
          <span v-if="tab.key === 'storyboard' && storyboards.length" class="ml-1 px-1.5 py-0.5 bg-white/20 rounded-full text-[10px]">{{ storyboards.length }}</span>
          <span v-if="tab.key === 'video' && videos.length" class="ml-1 px-1.5 py-0.5 bg-white/20 rounded-full text-[10px]">{{ videos.length }}</span>
        </a-button>
      </div>
      
      <!-- 整集导出按钮 -->
      <div class="flex items-center gap-2 shrink-0">
        <a-button 
          type="primary" 
          :loading="exporting"
          :disabled="storyboards.length === 0"
          @click="exportEpisode"
          class="!rounded-xl"
        >
          <template #icon><ExportOutlined /></template>
          {{ exporting ? '导出中...' : '整集导出' }}
        </a-button>
        <a-button 
          type="default" 
          @click="showExportPanel = !showExportPanel"
          class="!rounded-xl !bg-[#1a1a1a] !border-[#2a2a2a] !text-[#a0a0a0]"
        >
          <template #icon><HistoryOutlined /></template>
          历史
        </a-button>
      </div>
    </div>
    
    <!-- 导出历史面板 -->
    <div v-if="showExportPanel" class="bg-[#1a1a1a] rounded-xl p-4 border border-[#2a2a2a]">
      <div class="flex items-center justify-between mb-3">
        <h4 class="text-sm font-medium text-[#f5f5f5] flex items-center gap-2">
          <HistoryOutlined />
          导出历史
        </h4>
        <a-button type="link" size="small" @click="loadExportHistory" :loading="loadingHistory">
          刷新
        </a-button>
      </div>
      
      <div v-if="loadingHistory" class="flex justify-center py-4">
        <a-spin size="small" />
      </div>
      
      <div v-else-if="exportHistory.length === 0" class="text-center py-4 text-[#888]">
        暂无导出记录
      </div>
      
      <div v-else class="space-y-2">
        <div v-for="item in exportHistory" :key="item.id" 
          class="flex items-center justify-between p-3 bg-[#242424] rounded-lg">
          <div class="flex items-center gap-3 min-w-0">
            <a-tag v-if="item.status === 'completed'" color="success" class="!rounded-lg shrink-0">成功</a-tag>
            <a-tag v-else-if="item.status === 'failed'" color="error" class="!rounded-lg shrink-0">失败</a-tag>
            <a-tag v-else color="processing" class="!rounded-lg shrink-0">处理中</a-tag>
            <div class="min-w-0">
              <p class="text-sm text-[#f5f5f5] truncate">第{{ item.episodeNumber }}集 · {{ item.shotCount }}个分镜</p>
              <p class="text-xs text-[#888]">{{ formatTime(item.createdAt) }} · 时长 {{ formatDuration(item.duration) }}</p>
            </div>
          </div>
          <a-button 
            v-if="item.exportUrl" 
            type="link" 
            size="small"
            @click="openUrl(item.exportUrl)"
          >
            <template #icon><DownloadOutlined /></template>
            下载
          </a-button>
        </div>
      </div>
    </div>

    <!-- ========== 参考图预览 Modal ========== -->
    <a-modal
      v-model:open="refPreviewOpen"
      :title="refPreviewImage?.shot ? `分镜 #${refPreviewImage.shot.shotNumber || ''} 参考图预览` : '参考图预览'"
      width="720px"
      :footer="refPreviewImage?.shot ? null : null"
      @cancel="refPreviewOpen = false"
    >
      <div v-if="refPreviewImage?.shot" class="space-y-4">
        <div class="relative aspect-video bg-[#111] rounded-lg overflow-hidden">
          <img v-if="refPreviewImage.fileUrl" :src="refPreviewImage.fileUrl" class="w-full h-full object-contain" />
          <div v-else class="w-full h-full flex flex-col items-center justify-center text-[#444]">
            <PictureOutlined style="font-size: 48px;" />
            <span class="mt-2 text-sm">加载中...</span>
          </div>
        </div>
        <div v-if="refPreviewImage.extraData" class="p-3 bg-[#1a1a1a] rounded-lg">
          <p class="text-xs text-[#888] mb-1">提示词:</p>
          <p class="text-xs text-[#d0d0d0] whitespace-pre-wrap">{{ (JSON.parse(refPreviewImage.extraData || '{}')).prompt || '无' }}</p>
        </div>
        <div class="flex justify-between items-center">
          <a-popconfirm title="确定删除该参考图？" ok-text="确定" cancel-text="取消" @confirm="deleteRefImage(refPreviewImage.shot)">
            <a-button type="text" danger size="small">
              <template #icon><DeleteOutlined /></template>
              删除
            </a-button>
          </a-popconfirm>
          <div class="flex gap-2">
            <a-button size="small" :loading="generatingRefImg === refPreviewImage.shot?.id" @click="forceRegenerateRefImage(refPreviewImage.shot)">
              重新生成
            </a-button>
            <a-button type="primary" size="small" @click="() => { refPreviewOpen = false; goToVideoGeneration(refPreviewImage.shot) }">
              确认 → 生成视频
            </a-button>
          </div>
        </div>
      </div>
    </a-modal>

    <!-- ========== 分镜 Tab ========== -->
    <div v-show="activeTab === 'storyboard'" class="space-y-4 w-full">
      <!-- AI拆解 -->
      <div class="bg-gradient-to-br from-[#1a1a2e] to-[#16162a] rounded-xl p-4 sm:p-5 border border-[#2a2a3e]">
        <h4 class="text-sm font-medium text-[#f5f5f5] mb-3 flex items-center gap-2">
          <RobotOutlined />
          剧本 → 自动分镜
        </h4>
        <a-textarea v-model:value="scriptText" :rows="4" placeholder="粘贴剧本内容，AI自动拆解为镜头..." class="mb-3" />
        <div class="flex justify-end">
          <a-button type="primary" :loading="generatingSb" :disabled="!scriptText.trim()" @click="generateFromScript">
            <template #icon><SendOutlined /></template>
            {{ generatingSb ? 'AI拆解中...' : 'AI 拆解' }}
          </a-button>
        </div>
      </div>

      <!-- Loading -->
      <div v-if="loadingSb" class="flex justify-center py-12">
        <a-spin size="large" />
      </div>

      <!-- Empty -->
      <div v-else-if="storyboards.length === 0" class="text-center py-12">
        <a-empty description="暂无分镜，输入剧本后点击 AI 拆解" />
      </div>

      <!-- 列表 -->
      <div v-else class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-2 xl:grid-cols-3 gap-3">
        <div v-for="(shot, idx) in storyboards" :key="shot.id"
          class="group p-3 sm:p-4 bg-[#1a1a1a] rounded-xl border border-[#2a2a2a] hover:border-[#6366f1]/40 transition-all relative">
          <!-- 点击编辑区域 -->
          <div @click="openSbEditor(shot)" class="cursor-pointer">
            <div class="flex items-center justify-between mb-2">
              <span class="flex items-center gap-1.5 sm:gap-2 min-w-0">
                <span class="w-6 h-6 sm:w-7 sm:h-7 flex items-center justify-center rounded-lg bg-[#6366f1]/10 text-[10px] sm:text-sm font-bold text-[#6366f1] shrink-0">{{ shot.shotNumber || idx + 1 }}</span>
                <span class="px-1.5 sm:px-2 py-0.5 bg-[#6366f1]/15 text-[#a78bfa] rounded text-[10px] sm:text-xs shrink-0">{{ shotTypes.find(t=>t.value===shot.shotType)?.label }}</span>
              </span>
              <span @click.stop>
                <a-popconfirm title="确定删除该分镜？" ok-text="确定" cancel-text="取消" @confirm="deleteShot(shot.id)">
                  <a-button type="text" danger size="small" class="opacity-0 group-hover:opacity-100 !p-1">
                    <template #icon><DeleteOutlined /></template>
                  </a-button>
                </a-popconfirm>
              </span>
            </div>
            <p class="text-xs sm:text-sm text-[#d0d0d0] mb-1 line-clamp-2">{{ shot.action || '无动作描述' }}</p>
            <p v-if="shot.dialogue" class="text-[10px] xs:text-xs text-[#888] italic line-clamp-1">「{{ shot.dialogue }}」</p>
            <div class="mt-2 flex flex-wrap gap-1">
              <a-tag v-if="shot.characterName" color="#fffaf015" class="!text-yellow-400/70 !rounded-[10px] !text-[9px] sm:!text-[10px]">{{ shot.characterName }}</a-tag>
              <a-tag v-if="shot.shotDirection" color="#eff6ff15" class="!text-blue-400/70 !rounded-[10px] !text-[9px] sm:!text-[10px]">{{ shot.shotDirection }}</a-tag>
            </div>
            <!-- 图片标记 -->
            <div v-if="shot.gridImageUrl || shot.characterImageUrl || shot.sceneImageUrl || refImages[shot.id]" class="mt-2 flex gap-1 flex-wrap">
              <span v-if="shot.sceneImageUrl" title="有场景图" class="text-[10px] px-1.5 py-0.5 bg-green-500/10 text-green-400 rounded">场景图</span>
              <span v-if="shot.characterImageUrl" title="有角色图" class="text-[10px] px-1.5 py-0.5 bg-blue-500/10 text-blue-400 rounded">角色图</span>
              <span v-if="shot.gridImageUrl" title="有宫格图" class="text-[10px] px-1.5 py-0.5 bg-purple-500/10 text-purple-400 rounded">宫格图</span>
              <!-- 参考图缩略图预览 -->
              <span v-if="refImages[shot.id]" title="有参考图" class="text-[10px] px-1.5 py-0.5 bg-orange-500/10 text-orange-400 rounded cursor-pointer hover:bg-orange-500/20"
                @click.stop="previewRefImage(shot)">参考图🖼</span>
            </div>
          </div>
          <!-- 操作按钮行 -->
          <div class="mt-3 pt-2 border-t border-[#2a2a2a] flex gap-1.5">
            <!-- 参考图生成 -->
            <a-tooltip title="生成参考图（预览角色×场景效果）">
              <a-button
                size="small"
                :loading="generatingRefImg === shot.id"
                @click.stop="generateRefImage(shot)"
                class="!flex-1"
              >
                <template #icon><PictureOutlined /></template>
                {{ generatingRefImg === shot.id ? '生成中' : '参考图' }}
              </a-button>
            </a-tooltip>
            <!-- 生成视频 -->
            <a-button
              type="primary"
              size="small"
              block
              @click.stop="goToVideoGeneration(shot)"
              class="!flex-1"
            >
              <template #icon><VideoCameraOutlined /></template>
              生成视频
            </a-button>
          </div>
        </div>
      </div>
    </div>

    <!-- ========== 角色图 Tab ========== -->
    <div v-show="activeTab === 'character'" class="space-y-4 w-full">
      <div v-if="characters.length === 0" class="text-center py-12">
        <a-empty description="暂无角色，请先在剧集详情页添加角色">
          <template #extra>
            <a-button type="link" @click="router.push(`/drama/${dramaId}`)">前往管理 →</a-button>
          </template>
        </a-empty>
      </div>

      <div v-else class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-3 sm:gap-4">
        <div v-for="char in characters" :key="char.id"
          class="group bg-[#1a1a1a] rounded-xl border border-[#2a2a2a] p-2.5 sm:p-4 hover:border-[#6366f1]/40 transition-all">
          <div class="relative aspect-square w-full max-w-[120px] sm:max-w-[140px] mx-auto bg-[#242424] rounded-lg mb-2 sm:mb-3 overflow-hidden group-hover:ring-2 ring-[#6366f1]/30 transition-all">
            <img v-if="char.imageUrl" :src="char.imageUrl" class="w-full h-full object-cover" />
            <div v-else class="w-full h-full flex flex-col items-center justify-center text-[#404040]">
              <UserOutlined style="font-size: 32px; opacity: 0.3;" />
              <span class="text-[10px] mt-1">暂无图片</span>
            </div>
            <!-- 生成中遮罩 -->
            <div v-if="generatingCharImg === char.id" class="absolute inset-0 bg-black/60 flex items-center justify-center">
              <a-spin />
            </div>
          </div>
          <p class="text-xs sm:text-sm font-medium text-[#f5f5f5] text-center truncate px-1">{{ char.name }}</p>
          <a-button
            size="small"
            block
            :loading="generatingCharImg === char.id"
            :disabled="generatingCharImg !== null"
            @click="generateCharImage(char)"
            class="mt-1.5 sm:mt-2"
          >
            {{ generatingCharImg === char.id ? '' : 'AI生成角色图' }}
          </a-button>
        </div>
      </div>
    </div>

    <!-- ========== 场景图 Tab ========== -->
    <div v-show="activeTab === 'scene'" class="space-y-4 w-full">
      <div v-if="scenes.length === 0" class="text-center py-12">
        <a-empty description="暂无场景，请先在剧集详情页添加场景">
          <template #extra>
            <a-button type="link" @click="router.push(`/drama/${dramaId}`)">前往管理 →</a-button>
          </template>
        </a-empty>
      </div>

      <div v-else class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-3 sm:gap-4">
        <div v-for="scene in scenes" :key="scene.id"
          class="group bg-[#1a1a1a] rounded-xl border border-[#2a2a2a] overflow-hidden hover:border-[#6366f1]/40 transition-all">
          <div class="relative aspect-video bg-[#242424]">
            <img v-if="scene.imageUrl" :src="scene.imageUrl" class="w-full h-full object-cover" />
            <div v-else class="w-full h-full flex flex-col items-center justify-center text-[#404040]">
              <PictureOutlined style="font-size: 32px; opacity: 0.3;" />
              <span class="text-[10px] mt-1">暂无图片</span>
            </div>
            <div v-if="generatingSceneImg === scene.id" class="absolute inset-0 bg-black/60 flex items-center justify-center">
              <a-spin />
            </div>
          </div>
          <div class="p-2.5 sm:p-3">
            <p class="text-xs sm:text-sm font-medium text-[#f5f5f5] truncate">{{ scene.name }}</p>
            <a-button
              size="small"
              block
              :loading="generatingSceneImg === scene.id"
              :disabled="generatingSceneImg !== null"
              @click="generateSceneImg(scene)"
              class="mt-1.5 sm:mt-2"
            >
              {{ generatingSceneImg === scene.id ? '' : 'AI生成场景图' }}
            </a-button>
          </div>
        </div>
      </div>
    </div>

    <!-- ========== 视频生成 Tab ========== -->
    <div v-show="activeTab === 'video'" class="space-y-4 w-full">
      <!-- 视频生成区 -->
      <div class="bg-gradient-to-br from-[#151a25] to-[#101520] rounded-xl p-4 sm:p-5 border border-[#222a38]">
        <h4 class="text-sm font-medium text-[#f5f5f5] mb-3 sm:mb-4 flex items-center gap-2">
          <VideoCameraOutlined />
          图片 → AI 视频
        </h4>

        <div class="grid grid-cols-1 sm:grid-cols-2 gap-3 sm:gap-4 mb-4">
          <div class="sm:col-span-2">
            <label class="block text-xs sm:text-sm text-[#a0a0a0] mb-1.5">参考图片 URL</label>
            <a-input v-model:value="videoImageUrl" placeholder="输入分镜图、宫格图或角色图的 URL..." size="large" />
          </div>
          <div>
            <label class="block text-xs sm:text-sm text-[#a0a0a0] mb-1.5">视频模型</label>
            <a-select v-model:value="selectedVideoModel" size="large" class="w-full">
              <a-select-option v-for="m in videoModels" :key="m.value" :value="m.value">{{ m.label }}</a-select-option>
            </a-select>
          </div>
          <div class="flex items-end">
            <a-button
              type="primary"
              :loading="generatingVideo"
              :disabled="!videoImageUrl.trim()"
              @click="generateVideo"
              class="w-full !rounded-xl"
              size="large"
            >
              <template #icon><VideoCameraOutlined /></template>
              {{ generatingVideo ? '生成中...' : '生成视频' }}
            </a-button>
          </div>
        </div>

        <p class="text-[10px] text-[#555] mt-1">💡 提示：填写视频描述即可生成，图片仅为增强参考（可选）</p>
      </div>

      <!-- 视频记录列表 -->
      <div>
        <div class="flex items-center justify-between mb-3">
          <h4 class="text-xs sm:text-sm font-medium text-[#808080]">视频记录 ({{ videos.length }})</h4>
          <a-button size="small" type="text" :loading="videosLoading" @click="loadVideos">
            <template #icon><ReloadOutlined /></template> 刷新
          </a-button>
        </div>

        <a-spin :spinning="videosLoading">
          <div v-if="videos.length > 0" class="space-y-3">
            <div v-for="v in videos" :key="v.id"
              class="bg-[#1a1a1a] rounded-xl border border-[#2a2a2a] overflow-hidden group hover:border-[#6366f1]/40 transition-all">

              <!-- 视频预览区 / 缩略图 -->
              <div v-if="v.videoUrl" class="relative aspect-video bg-[#0f0f0f] cursor-pointer"
                   @click="togglePlayVideo(v.id)">
                <!-- 播放状态：显示播放器 -->
                <video v-show="playingVideoId === v.id"
                       :src="v.videoUrl"
                       controls
                       autoplay
                       class="w-full h-full object-contain"
                       @ended="playingVideoId = null" />
                <!-- 非播放状态：显示封面+播放按钮 -->
                <div v-show="playingVideoId !== v.id" class="absolute inset-0 flex flex-col items-center justify-center bg-black/30 group-hover:bg-black/50 transition-colors">
                  <PlayCircleOutlined style="font-size: 48px; color: #fff; opacity: 0.9;" />
                  <span class="mt-2 text-xs text-white/70">点击播放</span>
                  <span v-if="v.duration" class="absolute bottom-2 right-2 px-2 py-0.5 bg-black/60 rounded text-[10px] text-white">{{ formatDuration(v.duration) }}</span>
                </div>
              </div>

              <!-- 无视频URL时的占位 -->
              <div v-else class="aspect-video bg-[#111] flex flex-col items-center justify-center">
                <VideoCameraOutlined style="font-size: 32px; color: #333;" />
                <span class="text-xs text-[#444] mt-1">{{ v.status === 'processing' ? '等待生成中...' : '无视频文件' }}</span>
              </div>

              <!-- 信息栏 -->
              <div class="flex items-center gap-2 sm:gap-3 p-2.5 sm:p-3">
                <div class="min-w-0 flex-1">
                  <div class="flex items-center gap-2 mb-0.5">
                    <span class="text-xs sm:text-sm text-[#e0e0e0] font-medium truncate">{{ v.model || '未知模型' }}</span>
                    <a-tag
                      :color="statusColorMap[v.status] || '#333'"
                      :class="[statusTextColorMap[v.status] || '!text-[#808080]', 'shrink-0']"
                    >
                      {{ statusLabelMap[v.status] || v.status }}
                    </a-tag>
                  </div>
                  <p class="text-[10px] xs:text-xs text-[#606060] truncate">
                    第{{ v.episodeNumber }}集 · {{ v.provider }}{{ v.taskId ? ` · ${v.taskId.substring(0, 12)}...` : '' }}
                  </p>
                </div>
                <a-popconfirm title="确定删除该视频？" ok-text="确定" cancel-text="取消" @confirm="deleteVideo(v.id)">
                  <a-button type="text" danger size="small" class="opacity-0 group-hover:opacity-100 shrink-0 !p-1">
                    <template #icon><DeleteOutlined /></template>
                  </a-button>
                </a-popconfirm>
              </div>
            </div>
          </div>

          <div v-else class="text-center py-12">
            <a-empty description="尚未生成任何视频，上传图片后点击上方按钮开始生成" :image-style="{ opacity: 0.3 }" />
          </div>
        </a-spin>
      </div>
    </div>

    <!-- ====== 分镜编辑弹窗（Ant Design Modal） ====== -->
    <a-modal
      v-model:open="sbEditorOpen"
      title="编辑分镜"
      @ok="saveShot"
      okText="保存"
      cancelText="取消"
      width="640px"
      destroyOnClose
    >
      <div class="space-y-4 pt-2">
        <!-- 关联图片展示区域 -->
        <div v-if="relatedCharImage || relatedSceneImage" class="grid grid-cols-2 gap-4 p-3 bg-[#1a1a1a] rounded-lg">
          <!-- 角色图 -->
          <div v-if="relatedCharImage" class="space-y-2">
            <div class="text-xs text-gray-400 flex items-center gap-1">
              <UserOutlined />
              角色: {{ relatedCharName }}
            </div>
            <div class="relative aspect-square rounded-lg overflow-hidden bg-[#2a2a2a] border border-[#333]">
              <img :src="relatedCharImage" class="w-full h-full object-cover" />
            </div>
          </div>
          <!-- 场景图 -->
          <div v-if="relatedSceneImage" class="space-y-2">
            <div class="text-xs text-gray-400 flex items-center gap-1">
              <PictureOutlined />
              场景: {{ relatedSceneName }}
            </div>
            <div class="relative aspect-video rounded-lg overflow-hidden bg-[#2a2a2a] border border-[#333]">
              <img :src="relatedSceneImage" class="w-full h-full object-cover" />
            </div>
          </div>
        </div>
        
        <!-- 手动关联角色/场景选择器 -->
        <div class="grid grid-cols-2 gap-3 p-3 bg-[#1a1a1a] rounded-lg">
          <div>
            <label class="block text-xs text-gray-400 mb-1.5 flex items-center gap-1">
              <UserOutlined /> 关联角色
            </label>
            <a-select
              v-model:value="sbForm.characterId"
              placeholder="选择角色..."
              size="small"
              allowClear
              showSearch
              class="w-full"
              @change="onSbSelectCharacter"
            >
              <a-select-option v-for="char in characters" :key="char.id" :value="char.id">
                {{ char.name }}{{ char.imageUrl ? ' 📸' : '' }}
              </a-select-option>
            </a-select>
          </div>
          <div>
            <label class="block text-xs text-gray-400 mb-1.5 flex items-center gap-1">
              <PictureOutlined /> 关联场景
            </label>
            <a-select
              v-model:value="sbForm.sceneId"
              placeholder="选择场景..."
              size="small"
              allowClear
              showSearch
              class="w-full"
              @change="onSbSelectScene"
            >
              <a-select-option v-for="scene in scenes" :key="scene.id" :value="scene.id">
                {{ scene.name }}{{ scene.imageUrl ? ' 📸' : '' }}
              </a-select-option>
            </a-select>
          </div>
        </div>

        <a-divider class="!my-2 !border-[#333]" />
        
        <a-form layout="vertical">
          <div class="grid grid-cols-2 gap-3">
            <a-form-item label="镜头类型">
              <a-select v-model:value="sbForm.shotType">
                <a-select-option v-for="t in shotTypes" :key="t.value" :value="t.value">{{ t.label }}</a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item label="运镜方式">
              <a-input v-model:value="sbForm.shotDirection" placeholder="推/拉/摇/移/固定..." />
            </a-form-item>
          </div>
          <a-form-item label="动作描述">
            <a-textarea v-model:value="sbForm.action" :rows="3" placeholder="描述这个镜头中的动作和画面..." />
          </a-form-item>
          <a-form-item label="台词">
            <a-textarea v-model:value="sbForm.dialogue" :rows="2" placeholder="角色的台词内容..." />
          </a-form-item>
        </a-form>
      </div>
    </a-modal>
  </div>
</template>

<style scoped>
.no-scrollbar {
  -ms-overflow-style: none;
  scrollbar-width: none;
}
.no-scrollbar::-webkit-scrollbar {
  display: none;
}
.line-clamp-1 { overflow: hidden; display: -webkit-box; -webkit-box-orient: vertical; -webkit-line-clamp: 1; }
.line-clamp-2 { overflow: hidden; display: -webkit-box; -webkit-box-orient: vertical; -webkit-line-clamp: 2; }

/* 暗色主题下覆盖 Ant Design 表单标签颜色 */
::deep(.ant-form-item-label > label) {
  color: #a0a0a0 !important;
}
</style>
