<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { mediaApi } from '@/utils/media'
import { audioApi, videoApi } from '@/utils/request'
import { assetApi } from '@/utils/asset'
import { message as AMessage } from 'ant-design-vue'
import {
  AudioOutlined,
  VideoCameraOutlined,
  MergeCellsOutlined,
  FolderOpenOutlined,
  PlayCircleOutlined,
  DeleteOutlined,
  UploadOutlined,
  CloudUploadOutlined,
  SoundOutlined,
  RobotOutlined,
  SendOutlined,
  EditOutlined,
  FileImageOutlined,
  ReloadOutlined,
} from '@ant-design/icons-vue'

const route = useRoute()
const props = defineProps<{ dramaId: string }>()

const activeTab = ref('tts')
const generating = ref(false)

// ====== TTS 配音 ======
const ttsText = ref('')
const selectedVoice = ref('female-tianmei')
const audios = ref<any[]>([])
const audiosLoading = ref(false)

const voices = [
  { value: 'female-tianmei', label: '甜美女生' },
  { value: 'female-shaonv', label: '少女音' },
  { value: 'female-yujie', label: '御姐女声' },
  { value: 'male-chengshu', label: '成熟男声' },
  { value: 'male-qingse', label: '青涩男声' },
  { value: 'male-badao', label: '霸道男声' },
]

const generateTTS = async () => {
  if (!ttsText.value.trim()) { AMessage.warning('请输入配音文本'); return }
  generating.value = true
  try {
    const res = await mediaApi.generateTTS({
      dramaId: props.dramaId,
      episodeNumber: 1,
      storyboardId: '',
      characterId: '',
      text: ttsText.value,
      voiceId: selectedVoice.value,
    })
    if (res.code === 200 && res.data) {
      audios.value.unshift(res.data)
      if (res.data.audioUrl) {
        const audio = new Audio(res.data.audioUrl)
        audio.play().catch((err) => {
          console.error('Audio play failed:', err)
          AMessage.warning('音频播放失败，可能是文件访问权限问题')
        })
      }
      AMessage.success('配音生成成功')
      ttsText.value = ''
    } else {
      AMessage.error(res.message || 'TTS生成失败')
    }
  } catch (e: any) {
    AMessage.error(e?.message || 'TTS生成失败，请检查AI配置')
  } finally {
    generating.value = false
  }
}

// ====== 视频 ======
const videoImageUrl = ref('')
const generatingVideo = ref(false)
const videos = ref<any[]>([])
const videosLoading = ref(false)
const selectedVideoModel = ref('video-01')

const generateVideo = async () => {
  if (!videoImageUrl.value.trim()) { AMessage.warning('请输入图片URL或先生成宫格图'); return }
  generatingVideo.value = true
  try {
    const res = await mediaApi.generateVideo({
      dramaId: props.dramaId,
      episodeNumber: 1,
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

// 轮询处理中的视频
const pollVideos = () => {
  const hasProcessing = videos.value.some((v: any) => v.status === 'processing')
  if (hasProcessing) {
    videoApi.poll().then((res: any) => {
      // 刷新列表
      loadVideos()
    }).catch(() => {})
  }
}

// 播放音频（带错误处理）
const playAudio = (url: string) => {
  const audio = new Audio(url)
  audio.play().catch((err) => {
    console.error('Audio play failed:', err)
    AMessage.warning('音频播放失败，可能是文件访问权限问题')
  })
}

// ====== 合成 ======
const composing = ref(false)
const composeEpisode = async () => {
  composing.value = true
  try {
    AMessage.info('功能开发中 - 需要后端合成API支持')
  } catch (e: any) {
    AMessage.error(e?.message)
  } finally {
    composing.value = false
  }
}

// ====== 素材管理 ======
const assets = ref<any[]>([])
const assetsLoading = ref(false)
const uploadLoading = ref(false)
const uploadProgress = ref(0)

// 文件上传
const handleUpload = async ({ file }: any) => {
  uploadLoading.value = true
  try {
    const res = await assetApi.upload(file.originFileObj || file, props.dramaId)
    if (res.code === 200 && res.data) {
      assets.value.unshift(res.data)
      AMessage.success(`上传成功: ${res.data.filename}`)
    } else {
      AMessage.error(res.message || '上传失败')
    }
  } catch (e: any) {
    AMessage.error(e?.message || '文件上传失败')
  } finally {
    uploadLoading.value = false
  }
  return false // 阻止默认上传行为
}

const deleteAsset = async (id: string) => {
  try {
    await assetApi.delete(id)
    assets.value = assets.value.filter((a: any) => a.id !== id)
    AMessage.success('已删除')
  } catch (e: any) {
    AMessage.error(e?.message || '删除失败')
  }
}

// ====== 数据加载 ======
const loadAudios = async () => {
  audiosLoading.value = true
  try {
    const res = await audioApi.listByDrama(props.dramaId)
    if (res.code === 200) audios.value = res.data || []
  } catch (e) { console.error(e) }
  finally { audiosLoading.value = false }
}

const loadVideos = async () => {
  videosLoading.value = true
  try {
    const res = await videoApi.listByDrama(props.dramaId)
    if (res.code === 200) videos.value = res.data || []
  } catch (e) { console.error(e) }
  finally { videosLoading.value = false }
}

const loadAssets = async () => {
  assetsLoading.value = true
  try {
    const res = await assetApi.list(props.dramaId)
    if (res.code === 200) assets.value = res.data || []
  } catch (e) { console.error(e) }
  finally { assetsLoading.value = false }
}

// 定时轮询（5秒）
let pollTimer: ReturnType<typeof setInterval> | null = null

onMounted(async () => {
  await Promise.all([loadAudios(), loadVideos(), loadAssets()])
  pollTimer = setInterval(() => {
    pollVideos()
  }, 5000)
})

// 清理定时器
import { onUnmounted } from 'vue'
onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
})

// 状态标签颜色
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
</script>

<template>
  <div class="w-full space-y-4 sm:space-y-6">
    <!-- 页面标题 -->
    <h1 class="text-lg sm:text-xl font-semibold text-[#f5f5f5] flex items-center gap-2">
      <AudioOutlined />
      媒体工作室
    </h1>

    <!-- Tab Bar -->
    <div class="flex gap-2 overflow-x-auto pb-1 no-scrollbar">
      <a-button
        v-for="tab in [
          { key: 'tts', label: '配音', icon: SoundOutlined },
          { key: 'video', label: '视频生成', icon: VideoCameraOutlined },
          { key: 'assets', label: '素材管理', icon: FolderOpenOutlined },
          { key: 'compose', label: '合成输出', icon: MergeCellsOutlined }
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
        <span v-if="tab.key === 'tts' && audios.length" class="ml-1 px-1.5 py-0.5 bg-white/20 rounded-full text-[10px]">{{ audios.length }}</span>
        <span v-if="tab.key === 'video' && videos.length" class="ml-1 px-1.5 py-0.5 bg-white/20 rounded-full text-[10px]">{{ videos.length }}</span>
        <span v-if="tab.key === 'assets' && assets.length" class="ml-1 px-1.5 py-0.5 bg-white/20 rounded-full text-[10px]">{{ assets.length }}</span>
      </a-button>
    </div>

    <!-- ========== TTS Tab ========== -->
    <div v-show="activeTab === 'tts'" class="space-y-4 w-full">
      <!-- TTS 生成区 -->
      <div class="bg-gradient-to-br from-[#1a1520] to-[#16101e] rounded-xl p-4 sm:p-5 border border-[#2a2238]">
        <h4 class="text-sm font-medium text-[#f5f5f5] mb-3 sm:mb-4 flex items-center gap-2">
          <AudioOutlined />
          文本转语音
        </h4>

        <div class="grid grid-cols-1 sm:grid-cols-2 gap-3 sm:gap-4 mb-4">
          <div>
            <label class="block text-xs sm:text-sm text-[#a0a0a0] mb-1.5">选择音色</label>
            <a-select v-model:value="selectedVoice" size="large" class="w-full">
              <a-select-option v-for="v in voices" :key="v.value" :value="v.value">{{ v.label }}</a-select-option>
            </a-select>
          </div>
          <div>
            <label class="block text-xs sm:text-sm text-[#a0a0a0] mb-1.5">预览效果</label>
            <div class="flex items-center gap-3 h-[38px] sm:h-[42px] px-3 sm:px-4 bg-[#0f0d14] border border-[#2a2238] rounded-xl text-[#606060] text-xs sm:text-sm">
              <span class="w-2 h-2 rounded-full bg-green-400 animate-pulse shrink-0"></span>
              {{ voices.find(v => v.value === selectedVoice)?.label }}
            </div>
          </div>
        </div>

        <div>
          <label class="block text-xs sm:text-sm text-[#a0a0a0] mb-1.5">配音文本</label>
          <a-textarea v-model:value="ttsText" :rows="4" placeholder="输入需要配音的台词文本..." class="!bg-[#0f0d14] !border-[#2a2238]" />
        </div>

        <div class="flex justify-end mt-4">
          <a-button type="primary" :loading="generating" :disabled="!ttsText.trim()" @click="generateTTS">
            <template #icon><AudioOutlined /></template>
            {{ generating ? '生成中...' : '生成配音' }}
          </a-button>
        </div>
      </div>

      <!-- 配音历史列表 -->
      <div>
        <div class="flex items-center justify-between mb-3">
          <h4 class="text-xs sm:text-sm font-medium text-[#808080]">配音记录 ({{ audios.length }})</h4>
          <a-button size="small" type="text" :loading="audiosLoading" @click="loadAudios">
            <template #icon><ReloadOutlined /></template> 刷新
          </a-button>
        </div>
        <a-spin :spinning="audiosLoading">
          <div v-if="audios.length > 0" class="space-y-2">
            <div v-for="audio in audios.slice(0, 20)" :key="audio.id"
              class="flex items-center gap-2 sm:gap-3 p-2.5 sm:p-3 bg-[#1a1a1a] rounded-xl border border-[#2a2a2a] group">
              <div class="w-8 h-8 sm:w-9 sm:h-9 flex items-center justify-center rounded-lg bg-purple-500/10 shrink-0 cursor-pointer"
                @click="audio.audioUrl ? playAudio(audio.audioUrl) : null">
                <PlayCircleOutlined style="color: #a78bfa; font-size: 16px;" />
              </div>
              <div class="flex-1 min-w-0">
                <p class="text-xs sm:text-sm text-[#e0e0e0] truncate">{{ audio.text?.substring(0, 60) }}{{ audio.text?.length > 60 ? '...' : '' }}</p>
                <p class="text-[10px] xs:text-xs text-[#606060] mt-0.5">{{ audio.provider }} · {{ audio.voiceId }}{{ audio.duration ? ` · ${audio.duration.toFixed(1)}s` : '' }}</p>
              </div>
              <a-tag
                :color="statusColorMap[audio.status] || '#333'"
                :class="[statusTextColorMap[audio.status] || '!text-[#808080]', 'shrink-0']"
              >
                {{ statusLabelMap[audio.status] || audio.status }}
              </a-tag>
            </div>
          </div>
          <div v-else class="text-center py-12">
            <a-empty description="暂无配音记录，在上方输入文本后点击生成" />
          </div>
        </a-spin>
      </div>
    </div>

    <!-- ========== Video Tab ========== -->
    <div v-show="activeTab === 'video'" class="space-y-4 w-full">
      <!-- 视频生成区 -->
      <div class="bg-gradient-to-br from-[#151a25] to-[#101520] rounded-xl p-4 sm:p-5 border border-[#222a38]">
        <h4 class="text-sm font-medium text-[#f5f5f5] mb-3 sm:mb-4 flex items-center gap-2">
          <VideoCameraOutlined />
          图片 → 视频
        </h4>

        <div class="mb-4">
          <label class="block text-xs sm:text-sm text-[#a0a0a0] mb-1.5">参考图片URL</label>
          <a-input v-model:value="videoImageUrl" placeholder="输入宫格图或角色图的URL..." size="large" />
        </div>

        <div class="mb-4">
          <label class="block text-xs sm:text-sm text-[#a0a0a0] mb-1.5">模型</label>
          <a-select v-model:value="selectedVideoModel" size="large" class="w-full">
            <a-select-option value="video-01">MiniMax Video-01</a-select-option>
            <a-select-option value="hailuo-h1-video">火炼 Hailuo-H1</a-select-option>
            <a-select-option value="vidu">Vidu</a-select-option>
          </a-select>
        </div>

        <div class="flex justify-between items-center">
          <span></span>
          <a-button type="primary" :loading="generatingVideo" :disabled="!videoImageUrl.trim()" @click="generateVideo">
            <template #icon><VideoCameraOutlined /></template>
            {{ generatingVideo ? '生成中...' : '生成视频' }}
          </a-button>
        </div>
      </div>

      <!-- 视频记录列表 -->
      <div>
        <div class="flex items-center justify-between mb-3">
          <h4 class="text-xs sm:text-sm font-medium text-[#808080]">生成记录 ({{ videos.length }})</h4>
          <a-button size="small" type="text" :loading="videosLoading" @click="loadVideos">
            <template #icon><ReloadOutlined /></template> 刷新
          </a-button>
        </div>
        <a-spin :spinning="videosLoading">
          <div v-if="videos.length > 0" class="space-y-2">
            <div v-for="v in videos" :key="v.id"
              class="flex items-center gap-2 sm:gap-3 p-2.5 sm:p-3 bg-[#1a1a1a] rounded-xl border border-[#2a2a2a] group">
              <div class="w-8 h-8 sm:w-9 sm:h-9 flex items-center justify-center rounded-lg bg-blue-500/10 shrink-0">
                <VideoCameraOutlined style="color: #60a5fa; font-size: 16px;" />
              </div>
              <div class="flex-1 min-w-0">
                <p class="text-xs sm:text-sm text-[#e0e0e0] truncate">{{ v.model }} · 第{{ v.episodeNumber }}集</p>
                <p class="text-[10px] xs:text-xs text-[#606060] mt-0.5">{{ v.provider }}{{ v.taskId ? ` · ${v.taskId.substring(0, 12)}...` : '' }}{{ v.duration ? ` · ${v.duration.toFixed(1)}s` : '' }}</p>
              </div>
              <a-tag
                :color="statusColorMap[v.status] || '#333'"
                :class="[statusTextColorMap[v.status] || '!text-[#808080]', 'shrink-0']"
              >
                {{ statusLabelMap[v.status] || v.status }}
              </a-tag>
              <a-popconfirm title="确定删除该视频？" ok-text="确定" cancel-text="取消" @confirm="videoApi.delete(v.id).then(()=>videos=videos.filter(x=>x.id!==v.id))">
                <a-button type="text" danger size="small" class="opacity-0 group-hover:opacity-100 !p-1">
                  <template #icon><DeleteOutlined /></template>
                </a-button>
              </a-popconfirm>
            </div>
          </div>
          <div v-else class="text-center py-8">
            <a-empty description="在工作台生成宫格图后，复制图片URL到此处生成视频" :image-style="{ opacity: 0.3 }" />
          </div>
        </a-spin>
      </div>
    </div>

    <!-- ========== Assets Tab (素材管理) ========== -->
    <div v-show="activeTab === 'assets'" class="space-y-4 w-full">
      <!-- 上传区 -->
      <div class="bg-gradient-to-br from-[#1a1a2e] to-[#16162a] rounded-xl p-4 sm:p-5 border border-[#2a2a3e]">
        <h4 class="text-sm font-medium text-[#f5f5f5] mb-3 flex items-center gap-2">
          <CloudUploadOutlined />
          上传素材文件
        </h4>
        <a-upload-dragger
          name="file"
          :multiple="true"
          :show-upload-list="false"
          :before-upload="(file:any)=>{ handleUpload({file}); return false; }"
          accept="image/*,video/*,audio/*,.pdf,.zip,.rar"
          class="!bg-[#0f0d14] !border-[#2a2a3e] !rounded-xl"
        >
          <p class="ant-upload-drag-icon !text-[#404040]"><UploadOutlined style="font-size:36px;" /></p>
          <p class="ant-upload-text !text-[#808080]">拖拽文件到此处，或 <span class="text-[#6366f1]">点击上传</span></p>
          <p class="ant-upload-hint !text-[#505050]">支持图片、视频、音频、文档等格式，单文件最大 100MB</p>
        </a-upload-dragger>
      </div>

      <!-- 素材网格 -->
      <div>
        <div class="flex items-center justify-between mb-3">
          <h4 class="text-xs sm:text-sm font-medium text-[#808080]">已上传素材 ({{ assets.length }})</h4>
          <a-button size="small" type="text" :loading="assetsLoading" @click="loadAssets">
            <template #icon><ReloadOutlined /></template> 刷新
          </a-button>
        </div>
        <a-spin :spinning="assetsLoading">
          <div v-if="assets.length > 0" class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-3">
            <div v-for="asset in assets" :key="asset.id"
              class="group relative bg-[#1a1a1a] rounded-xl border border-[#2a2a2a] overflow-hidden hover:border-[#6366f1]/50 transition-all">
              <!-- 缩略图 -->
              <div class="aspect-square bg-[#242424] relative">
                <img v-if="asset.type === 'image'" :src="asset.fileUrl" class="w-full h-full object-cover" />
                <div v-else-if="asset.type === 'video'" class="w-full h-full flex items-center justify-center text-[#404040]">
                  <VideoCameraOutlined style="font-size: 32px;" />
                </div>
                <div v-else-if="asset.type === 'audio'" class="w-full h-full flex items-center justify-center text-[#404040]">
                  <SoundOutlined style="font-size: 32px;" />
                </div>
                <div v-else class="w-full h-full flex items-center justify-center text-[#404040]">
                  <FileImageOutlined style="font-size: 28px;" />
                </div>
                <!-- 删除按钮 -->
                <div class="absolute top-1 right-1 opacity-0 group-hover:opacity-100 transition-opacity">
                  <a-popconfirm title="确定删除？" ok-text="确定" cancel-text="取消" @confirm="deleteAsset(asset.id)">
                    <a-button type="text" danger size="small" class="!p-1 !bg-black/50 !rounded-lg">
                      <DeleteOutlined style="font-size: 12px;" />
                    </a-button>
                  </a-popconfirm>
                </div>
              </div>
              <!-- 信息 -->
              <div class="p-2">
                <p class="text-[10px] xs:text-xs text-[#e0e0e0] truncate">{{ asset.filename }}</p>
                <div class="flex items-center justify-between mt-0.5">
                  <a-tag color="#333" class="!text-[#707070] !text-[9px] !py-0 !px-1">{{ asset.type }}</a-tag>
                  <span class="text-[9px] text-[#505050]">{{ formatFileSize(asset.fileSize) }}</span>
                </div>
              </div>
            </div>
          </div>
          <div v-else class="text-center py-12">
            <a-empty description="暂无素材，拖拽文件到上方区域上传" />
          </div>
        </a-spin>
      </div>
    </div>

    <!-- ========== Compose Tab ========== -->
    <div v-show="activeTab === 'compose'" class="space-y-4 w-full">
      <div class="rounded-xl p-6 sm:p-8 border border-dashed border-[#333] text-center bg-[#1a1a1a]">
        <MergeCellsOutlined style="font-size: 48px; color: #404040;" class="mb-3" />
        <h4 class="text-base sm:text-lg font-medium text-[#808080] mb-2">视频合成</h4>
        <p class="text-xs sm:text-sm text-[#505050] max-w-md mx-auto mb-4">
          将分镜生成的视频片段、配音按顺序合成为完整剧集。<br/>
          支持自动拼接、添加字幕、背景音乐。
        </p>
        <a-button :loading="composing" :disabled="composing" @click="composeEpisode">
          <template #icon><MergeCellsOutlined /></template>
          {{ composing ? '处理中...' : '开始合成第1集' }}
        </a-button>
      </div>

      <div class="grid grid-cols-1 sm:grid-cols-3 gap-3 sm:gap-4 mt-4">
        <div class="p-3 sm:p-4 bg-[#1a1a1a] rounded-xl border border-[#2a2a2a]">
          <VideoCameraOutlined style="font-size: 20px; color: #6366f1;" class="mb-2 block" />
          <p class="text-xs sm:text-sm font-medium text-[#f5f5f5]">视频拼接</p>
          <p class="text-[10px] xs:text-xs text-[#606060] mt-1">FFmpeg无缝衔接各镜头视频</p>
        </div>
        <div class="p-3 sm:p-4 bg-[#1a1a1a] rounded-xl border border-[#2a2a2a]">
          <AudioOutlined style="font-size: 20px; color: #6366f1;" class="mb-2 block" />
          <p class="text-xs sm:text-sm font-medium text-[#f5f5f5]">音频混流</p>
          <p class="text-[10px] xs:text-xs text-[#606060] mt-1">自动对齐配音与画面时间轴</p>
        </div>
        <div class="p-3 sm:p-4 bg-[#1a1a1a] rounded-xl border border-[#2a2a2a]">
          <EditOutlined style="font-size: 20px; color: #6366f1;" class="mb-2 block" />
          <p class="text-xs sm:text-sm font-medium text-[#f5f5f5]">字幕叠加</p>
          <p class="text-[10px] xs:text-xs text-[#606060] mt-1">自动提取台词并渲染字幕</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
// 格式化文件大小
function formatFileSize(bytes: number | undefined | null): string {
  if (!bytes || bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i]
}
export default { data: () => ({ formatFileSize }) }
</script>

<style scoped>
.no-scrollbar {
  -ms-overflow-style: none;
  scrollbar-width: none;
}
.no-scrollbar::-webkit-scrollbar {
  display: none;
}
/* 暗色主题下覆盖 Ant Design 表单标签颜色 */
::deep(.ant-form-item-label > label) {
  color: #a0a0a0 !important;
}
/* 上传组件暗色适配 */
:deep(.ant-upload-drag) {
  background: transparent !important;
}
:deep(.ant-upload-drag:hover) {
  border-color: #6366f1 !important;
}
</style>
