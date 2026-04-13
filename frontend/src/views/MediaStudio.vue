<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { mediaApi } from '@/utils/media'
import { audioApi } from '@/utils/request'
import { message as AMessage } from 'ant-design-vue'
import {
  AudioOutlined,
  VideoCameraOutlined,
  MergeCellsOutlined,
  PlayCircleOutlined,
  EditOutlined,
} from '@ant-design/icons-vue'

const route = useRoute()
const props = defineProps<{ dramaId: string }>()

const activeTab = ref('tts')
const generating = ref(false)

// ====== TTS ======
const ttsText = ref('')
const selectedVoice = ref('female-tianmei')
const audios = ref<any[]>([])

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
        new Audio(res.data.audioUrl).play().catch(() => {})
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

// ====== 合成 ======
const composing = ref(false)

const composeEpisode = async () => {
  composing.value = true
  try {
    // TODO: 调用后端合成API
    AMessage.info('功能开发中 - 需要后端合成API支持')
  } catch (e: any) {
    AMessage.error(e?.message)
  } finally {
    composing.value = false
  }
}

// 加载已有数据
onMounted(async () => {
  try {
    const audioRes = await audioApi.list(props.dramaId, 1)
    if (audioRes.code === 200) audios.value = audioRes.data || []
  } catch (e) {}
})
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
        v-for="tab in [{ key: 'tts', label: '🎙️ 配音' }, { key: 'video', label: '🎬 视频生成' }, { key: 'compose', label: '🔗 合成输出' }]"
        :key="tab.key"
        :type="activeTab === tab.key ? 'primary' : 'default'"
        size="small"
        @click="activeTab = tab.key"
        :class="[
          '!rounded-xl !font-medium whitespace-nowrap shrink-0',
          activeTab !== tab.key && '!bg-[#1a1a1a] !border-[#2a2a2a] !text-[#a0a0a0] hover:!text-[#f5f5f5] hover:!bg-[#242424]'
        ]"
      >
        {{ tab.label }}
      </a-button>
    </div>

    <!-- ========== TTS Tab ========== -->
    <div v-show="activeTab === 'tts'" class="space-y-4 w-full">
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

      <!-- 配音历史 -->
      <div v-if="audios.length > 0" class="w-full">
        <h4 class="text-xs sm:text-sm font-medium text-[#808080] mb-3">最近生成 ({{ audios.length }})</h4>
        <div class="space-y-2">
          <div v-for="audio in audios.slice(0, 10)" :key="audio.id"
            class="flex items-center gap-2 sm:gap-3 p-2.5 sm:p-3 bg-[#1a1a1a] rounded-xl border border-[#2a2a2a] group">
            <div class="w-8 h-8 sm:w-9 sm:h-9 flex items-center justify-center rounded-lg bg-purple-500/10 shrink-0">
              <PlayCircleOutlined style="color: #a78bfa; font-size: 16px;" />
            </div>
            <div class="flex-1 min-w-0">
              <p class="text-xs sm:text-sm text-[#e0e0e0] truncate">{{ audio.text?.substring(0, 60) }}{{ audio.text?.length > 60 ? '...' : '' }}</p>
              <p class="text-[10px] xs:text-xs text-[#606060] mt-0.5">{{ audio.provider }} · {{ audio.voiceId }}</p>
            </div>
            <a-tag color="#16a34a15" class="!text-green-400 shrink-0">{{ audio.status }}</a-tag>
          </div>
        </div>
      </div>
    </div>

    <!-- ========== Video Tab ========== -->
    <div v-show="activeTab === 'video'" class="space-y-4 w-full">
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

        <div class="flex justify-end">
          <a-button type="primary" :loading="generatingVideo" :disabled="!videoImageUrl.trim()" @click="generateVideo">
            <template #icon><VideoCameraOutlined /></template>
            {{ generatingVideo ? '生成中...' : '生成视频' }}
          </a-button>
        </div>
      </div>

      <div v-if="videos.length > 0" class="w-full">
        <h4 class="text-xs sm:text-sm font-medium text-[#808080] mb-3">生成记录</h4>
        <div class="space-y-2">
          <div v-for="v in videos" :key="v.id"
            class="flex items-center gap-2 sm:gap-3 p-2.5 sm:p-3 bg-[#1a1a1a] rounded-xl border border-[#2a2a2a]">
            <div class="w-8 h-8 sm:w-9 sm:h-9 flex items-center justify-center rounded-lg bg-blue-500/10 shrink-0">
              <VideoCameraOutlined style="color: #60a5fa; font-size: 16px;" />
            </div>
            <div class="flex-1 min-w-0">
              <p class="text-xs sm:text-sm text-[#e0e0e0] truncate">{{ v.model }}</p>
              <p class="text-[10px] xs:text-xs text-[#606060] mt-0.5">{{ v.provider }}</p>
            </div>
            <a-tag
              :color="v.status === 'completed' ? '#16a34a15' : v.status === 'processing' ? '#ca8a0415' : '#dc262615'"
              :class="[v.status === 'completed' ? '!text-green-400' : v.status === 'processing' ? '!text-yellow-400' : '!text-red-400', 'shrink-0']"
            >
              {{ v.status === 'completed' ? '已完成' : v.status === 'processing' ? '处理中' : '失败' }}
            </a-tag>
          </div>
        </div>
      </div>

      <div v-if="videos.length === 0 && !generatingVideo" class="text-center py-8">
        <a-empty description="在工作台生成宫格图后，复制图片URL到此处生成视频" :image-style="{ opacity: 0.3 }" />
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

<style scoped>
.no-scrollbar {
  -ms-overflow-style: none;
  scrollbar-width: none;
}
.no-scrollbar::-webkit-scrollbar {
  display: none;
}
</style>
