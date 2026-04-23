<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { mediaApi } from '@/utils/media'
import { audioApi, videoApi } from '@/utils/request'
import { assetApi } from '@/utils/asset'
import { composeApi } from '@/utils/compose'
import { aiConfigApi, ttsPreviewApi } from '@/utils/aiConfig'
import { storyboardApi } from '@/utils/request'
import { message as AMessage } from 'ant-design-vue'
import {
  ArrowLeftOutlined,
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
const router = useRouter()
// dramaId 从路由参数获取（路由: /media/:dramaId）
const dramaId = computed(() => (route.params.dramaId as string) || '')

const activeTab = ref('tts')
const generating = ref(false)

// ====== TTS 配音（关联分镜+角色+AI配置） ======
const ttsText = ref('')
const selectedVoice = ref('female-tianmei')
const audios = ref<any[]>([])
const audiosLoading = ref(false)
const ttsStoryboardId = ref('')
const ttsCharacterId = ref('')
const ttsStoryboards = ref<any[]>([])
const ttsCharacters = ref<any[]>([])
const ttsVoices = ref<{ value: string; label: string }[]>([
  // ====== MiniMax 官方完整系统音色列表 ======
  // 参考: https://platform.minimaxi.com/docs/faq/system-voice-id
  // ====== 中文(普通话) - 标准音色 ======
  { value: 'male-qn-qingse', label: '🎙️ 青涩青年音色' },
  { value: 'male-qn-jingying', label: '🎙️ 精英青年音色' },
  { value: 'male-qn-badao', label: '🎙️ 霸道青年音色' },
  { value: 'male-qn-daxuesheng', label: '🎙️ 青年大学生音色' },
  { value: 'female-shaonv', label: '🎙️ 少女音色' },
  { value: 'female-yujie', label: '🎙️ 御姐音色' },
  { value: 'female-chengshu', label: '🎙️ 成熟女性音色' },
  { value: 'female-tianmei', label: '🎙️ 甜美女性音色' },
  // ====== 中文(普通话) - Beta精品音色 ======
  { value: 'male-qn-qingse-jingpin', label: '⭐ 青涩青年音色-beta' },
  { value: 'male-qn-jingying-jingpin', label: '⭐ 精英青年音色-beta' },
  { value: 'male-qn-badao-jingpin', label: '⭐ 霸道青年音色-beta' },
  { value: 'male-qn-daxuesheng-jingpin', label: '⭐ 青年大学生音色-beta' },
  { value: 'female-shaonv-jingpin', label: '⭐ 少女音色-beta' },
  { value: 'female-yujie-jingpin', label: '⭐ 御姐音色-beta' },
  { value: 'female-chengshu-jingpin', label: '⭐ 成熟女性音色-beta' },
  { value: 'female-tianmei-jingpin', label: '⭐ 甜美女性音色-beta' },
  // ====== 中文(普通话) - 特色角色音色 ======
  { value: 'clever_boy', label: '🧒 聪明男童' },
  { value: 'cute_boy', label: '🧒 可爱男童' },
  { value: 'lovely_girl', label: '👧 萌萌女童' },
  { value: 'cartoon_pig', label: '🐷 卡通猪小琪' },
  { value: 'bingjiao_didi', label: '😈 病娇弟弟' },
  { value: 'junlang_nanyou', label: '👦 俊朗男友' },
  { value: 'chunzhen_xuedi', label: '🎒 纯真学弟' },
  { value: 'lengdan_xiongzhang', label: '🧊 冷淡学长' },
  { value: 'badao_shaoye', label: '🎩 霸道少爷' },
  { value: 'tianxin_xiaoling', label: '🍬 甜心小玲' },
  { value: 'qiaopi_mengmei', label: '😜 俏皮萌妹' },
  { value: 'wumei_yujie', label: '💋 妩媚御姐' },
  { value: 'diadia_xuemei', label: '🎀 嗲嗲学妹' },
  { value: 'danya_xuejie', label: '📚 淡雅学姐' },
  // ====== 中文(普通话) - 专业/特色音色 ======
  { value: 'Chinese (Mandarin)_Reliable_Executive', label: '💼 沉稳高管' },
  { value: 'Chinese (Mandarin)_News_Anchor', label: '📺 新闻女声' },
  { value: 'Chinese (Mandarin)_Mature_Woman', label: '💅 傲娇御姐' },
  { value: 'Chinese (Mandarin)_Unrestrained_Young_Man', label: '🏍️ 不羁青年' },
  { value: 'Arrogant_Miss', label: '😤 嚣张小姐' },
  { value: 'Robot_Armor', label: '🤖 机械战甲' },
  { value: 'Chinese (Mandarin)_Kind-hearted_Antie', label: '👵 热心大婶' },
  { value: 'Chinese (Mandarin)_HK_Flight_Attendant', label: '✈️ 港普空姐' },
  { value: 'Chinese (Mandarin)_Humorous_Elder', label: '😂 搞笑大爷' },
  { value: 'Chinese (Mandarin)_Gentleman', label: '🎩 温润男声' },
  { value: 'Chinese (Mandarin)_Warm_Bestie', label: '👭 温暖闺蜜' },
  { value: 'Chinese (Mandarin)_Male_Announcer', label: '🎤 播报男声' },
  { value: 'Chinese (Mandarin)_Sweet_Lady', label: '🌸 甜美女声' },
  { value: 'Chinese (Mandarin)_Southern_Young_Man', label: '🌾 南方小哥' },
  { value: 'Chinese (Mandarin)_Wise_Women', label: '📖 阅历姐姐' },
  { value: 'Chinese (Mandarin)_Gentle_Youth', label: '🍃 温润青年' },
  { value: 'Chinese (Mandarin)_Warm_Girl', label: '☀️ 温暖少女' },
  { value: 'Chinese (Mandarin)_Kind-hearted_Elder', label: '👵 花甲奶奶' },
  { value: 'Chinese (Mandarin)_Cute_Spirit', label: '🦄 憨憨萌兽' },
  { value: 'Chinese (Mandarin)_Radio_Host', label: '📻 电台男主播' },
  { value: 'Chinese (Mandarin)_Lyrical_Voice', label: '🎵 抒情男声' },
  { value: 'Chinese (Mandarin)_Straightforward_Boy', label: '🗣️ 率真弟弟' },
  { value: 'Chinese (Mandarin)_Sincere_Adult', label: '🙏 真诚青年' },
  { value: 'Chinese (Mandarin)_Gentle_Senior', label: '🌙 温柔学姐' },
  { value: 'Chinese (Mandarin)_Stubborn_Friend', label: '😤 嘴硬竹马' },
  { value: 'Chinese (Mandarin)_Crisp_Girl', label: '✨ 清脆少女' },
  { value: 'Chinese (Mandarin)_Pure-hearted_Boy', label: '💙 清澈邻家弟弟' },
  { value: 'Chinese (Mandarin)_Soft_Girl', label: '🌸 柔和少女' },
  // ====== 中文(粤语) ======
  { value: 'Cantonese_ProfessionalHost（F)', label: '🇭🇰 粤语-专业女主持' },
  { value: 'Cantonese_GentleLady', label: '🇭🇰 粤语-温柔女声' },
  { value: 'Cantonese_ProfessionalHost（M)', label: '🇭🇰 粤语-专业男主持' },
  { value: 'Cantonese_PlayfulMan', label: '🇭🇰 粤语-活泼男声' },
  { value: 'Cantonese_CuteGirl', label: '🇭🇰 粤语-可爱女孩' },
  { value: 'Cantonese_KindWoman', label: '🇭🇰 粤语-善良女声' },
])
const previewTTS = ref(false)
const previewAudioUrl = ref('')

// 加载分镜列表（用于选择要配音/生成视频的分镜）
const loadStoryboardsForTTS = async () => {
  try {
    const res = await storyboardApi.list(dramaId.value, 1)
    if (res.code === 200) {
      // 加载所有分镜，不限制必须有台词
      ttsStoryboards.value = res.data || []
    }
  } catch (e) { console.error(e) }
}

// 当选择分镜时，自动填入台词文本 + 关联角色 + 带出音色
const onSelectStoryboard = () => {
  const sb = ttsStoryboards.value.find((s: any) => s.id === ttsStoryboardId.value)
  if (sb) {
    if (sb.dialogue) ttsText.value = sb.dialogue
    if (sb.characterId) {
      ttsCharacterId.value = sb.characterId
      // 自动带出该角色的专属音色
      resolveVoiceFromCharacter(sb.characterId)
    } else {
      ttsCharacterId.value = ''
    }
  }
}

// 根据角色ID自动解析并设置音色
const resolveVoiceFromCharacter = (characterId: string) => {
  const ch = ttsCharacters.value.find((c: any) => c.id === characterId)
  if (ch && ch.voiceId && ch.voiceId.trim()) {
    const voiceExists = ttsVoices.value.some((v: any) => v.value === ch.voiceId)
    if (voiceExists) {
      selectedVoice.value = ch.voiceId
      // 开发环境调试：自动解析角色音色
      if (import.meta.env.DEV) console.log(`[TTS] Auto-resolved voice "${ch.voiceId}" from character "${ch.name || characterId}"`)
    }
  }
}

// 切换分镜时自动带出角色
watch(ttsStoryboardId, (newVal) => {
  if (newVal) onSelectStoryboard()
})

// 切换角色时自动带出该角色的专属音色
watch(ttsCharacterId, (newVal) => {
  if (newVal) resolveVoiceFromCharacter(newVal)
})

// 监听 Tab 切换：切换到素材管理时，如果没有数据则重新加载
watch(activeTab, (newTab) => {
  if (newTab === 'assets' && assets.value.length === 0 && !assetsLoading.value) {
    loadAssets(false)
  }
})

// 监听 dramaId 变化：当剧集ID变化时重新加载所有数据
watch(dramaId, (newId, oldId) => {
  if (newId && newId !== oldId) {
    // 重置素材列表并重新加载
    assets.value = []
    assetsPage.value = 1
    assetsHasMore.value = true
    loadAssets(false)
  }
})

const generateTTS = async () => {
  if (!ttsText.value.trim()) { AMessage.warning('请输入配音文本'); return }
  generating.value = true
  try {
    const res = await mediaApi.generateTTS({
      dramaId: dramaId.value,
      episodeNumber: 1,
      storyboardId: ttsStoryboardId.value,
      characterId: ttsCharacterId.value,
      text: ttsText.value,
      voiceId: selectedVoice.value,
    })
    if (res.code === 200 && res.data) {
      audios.value.unshift(res.data)
      if (res.data.audioUrl) {
        const audio = new Audio(res.data.audioUrl)
        audio.play().catch(() => AMessage.warning('音频播放失败'))
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

// 试听当前选择的音色
const handlePreviewVoice = async () => {
  if (!selectedVoice.value) { AMessage.warning('请先选择音色'); return }
  previewTTS.value = true
  previewAudioUrl.value = ''
  try {
    const res = await ttsPreviewApi.preview({
      text: '你好，这是一段音色效果试听。请确认该音色是否符合您的预期。',
      voiceId: selectedVoice.value,
      characterId: ttsCharacterId.value,
    })
    if (res.code === 200 && res.data?.audioData) {
      previewAudioUrl.value = res.data.audioData
      AMessage.success('试听就绪，请点击播放')
    }
  } catch (e: any) {
    AMessage.error(e?.message || '试听失败')
  } finally {
    previewTTS.value = false
  }
}

// ====== 视频（关联分镜+AI配置模型） ======
const videoImageUrl = ref('')
const generatingVideo = ref(false)
const videos = ref<any[]>([])
const videosLoading = ref(false)
const selectedVideoModel = ref('MiniMax-Hailuo-2.3')
// 视频预览
const videoPreviewOpen = ref(false)
const currentVideo = ref<any>(null)
const videoStoryboardId = ref('')
const videoModelsFromConfig = ref<{ value: string; label: string }[]>([])

// 视频生成模式
const videoGenerationMode = ref('IMAGE_TO_VIDEO')
const videoModes = [
  { value: 'TEXT_TO_VIDEO', label: '📝 文生视频', desc: '根据文本描述直接生成视频' },
  { value: 'IMAGE_TO_VIDEO', label: '🖼️ 图生视频', desc: '基于图片+文本描述生成视频' },
  { value: 'FIRST_LAST_FRAME', label: '🎬 首尾帧生成', desc: '提供开始和结束图片生成视频' },
  { value: 'SUBJECT_REFERENCE', label: '👤 主体参考', desc: '基于人脸照片保持人物特征生成视频' },
]

// 首尾帧和主体参考模式的额外图片URL（保留用于API调用）
const firstFrameUrl = ref('')
const lastFrameUrl = ref('')
const subjectImageUrl = ref('')
const videoPrompt = ref('')

// ====== 图片选择器（替代URL输入） ======
type ImagePickerTarget = 'videoImage' | 'firstFrame' | 'lastFrame' | 'subjectImage'
const imagePickerOpen = ref(false)
const imagePickerTarget = ref<ImagePickerTarget>('videoImage')
const imagePickerAssets = ref<any[]>([])
const imagePickerLoading = ref(false)
const imagePickerPage = ref(1)
const imagePickerHasMore = ref(true)
const imagePickerLoadingMore = ref(false)

// 已选图片的展示信息（用于缩略图预览）
const pickedVideoImage = ref<{ url: string; filename: string } | null>(null)
const pickedFirstFrame = ref<{ url: string; filename: string } | null>(null)
const pickedLastFrame = ref<{ url: string; filename: string } | null>(null)
const pickedSubjectImage = ref<{ url: string; filename: string } | null>(null)

/** 打开图片选择器 */
const openImagePicker = async (target: ImagePickerTarget) => {
  imagePickerTarget.value = target
  imagePickerOpen.value = true
  imagePickerPage.value = 1
  imagePickerHasMore.value = true
  imagePickerAssets.value = [] // 清空已有数据
  await loadPickerAssets(false)
}

/** 加载素材（支持分页+加载更多） */
const loadPickerAssets = async (isLoadMore = false) => {
  // 防止重复加载
  if (isLoadMore && imagePickerLoadingMore.value) return
  if (!isLoadMore && imagePickerLoading.value) return
  
  if (isLoadMore) {
    imagePickerLoadingMore.value = true
  } else {
    imagePickerLoading.value = true
  }
  try {
    // 加载全部图片（不传 dramaId），确保能看到所有素材
    const res = await assetApi.page({ 
      pageNum: imagePickerPage.value, 
      pageSize: 24, 
      type: 'image' 
    })
    if (res.code === 200) {
      const list = res.data?.records || []
      const total = res.data?.total || 0
      
      if (isLoadMore) {
        imagePickerAssets.value.push(...list)
      } else {
        imagePickerAssets.value = list
      }
      
      // 判断是否还有更多：已加载数量 < 总数 且 本次返回数量等于pageSize
      const hasMore = imagePickerAssets.value.length < total && list.length === 24
      imagePickerHasMore.value = hasMore
      
      console.log(`[图片选择器] 第${imagePickerPage.value}页加载完成，本页${list.length}条，总计${imagePickerAssets.value.length}/${total}条，${hasMore ? '还有更多' : '已加载全部'}`)
    }
  } catch (e) { 
    console.error('[图片选择器] 加载失败:', e)
    AMessage.error('加载图片失败，请重试')
  }
  finally {
    imagePickerLoading.value = false
    imagePickerLoadingMore.value = false
  }
}

/** 滚动加载更多 */
const onPickerScroll = (e: Event) => {
  const target = e.target as HTMLElement
  const scrollBottom = target.scrollHeight - target.scrollTop - target.clientHeight
  // 当距离底部小于80px时触发加载
  if (scrollBottom < 80 && imagePickerHasMore.value && !imagePickerLoadingMore.value && !imagePickerLoading.value) {
    imagePickerPage.value++
    loadPickerAssets(true)
  }
}

/** 选择一张图片 */
const selectImage = (asset: any) => {
  const url = asset.fileUrl || ''
  const filename = asset.filename || 'image'
  switch (imagePickerTarget.value) {
    case 'videoImage':
      videoImageUrl.value = url
      pickedVideoImage.value = { url, filename }
      break
    case 'firstFrame':
      firstFrameUrl.value = url
      pickedFirstFrame.value = { url, filename }
      break
    case 'lastFrame':
      lastFrameUrl.value = url
      pickedLastFrame.value = { url, filename }
      break
    case 'subjectImage':
      subjectImageUrl.value = url
      pickedSubjectImage.value = { url, filename }
      break
  }
  imagePickerOpen.value = false
}

/** 清除已选图片 */
const clearPickedImage = (target: ImagePickerTarget) => {
  switch (target) {
    case 'videoImage':
      videoImageUrl.value = ''; pickedVideoImage.value = null; break
    case 'firstFrame':
      firstFrameUrl.value = ''; pickedFirstFrame.value = null; break
    case 'lastFrame':
      lastFrameUrl.value = ''; pickedLastFrame.value = null; break
    case 'subjectImage':
      subjectImageUrl.value = ''; pickedSubjectImage.value = null; break
  }
}

/** 在图片选择器内上传图片 */
const handlePickerUpload = async (file: any) => {
  try {
    const res = await assetApi.upload(file, dramaId.value)
    if (res.code === 200 && res.data) {
      AMessage.success('图片上传成功')
      // 直接选中新上传的图片
      selectImage(res.data)
    } else {
      AMessage.error(res.message || '上传失败')
    }
  } catch (e: any) {
    AMessage.error(e?.message || '上传失败')
  }
}

/** 获取当前目标已选信息 */
const currentPickedImage = computed(() => {
  switch (imagePickerTarget.value) {
    case 'videoImage': return pickedVideoImage.value
    case 'firstFrame': return pickedFirstFrame.value
    case 'lastFrame': return pickedLastFrame.value
    case 'subjectImage': return pickedSubjectImage.value
    default: return null
  }
})

// 视频模型选项（默认兜底，优先从AI配置加载）
const videoModels = [
  { value: 'MiniMax-Hailuo-2.3', label: 'MiniMax 海螺 2.3' },
  { value: 'video-01', label: 'MiniMax Video-01' },
]

// 加载视频类型的 AI 配置（获取可用模型）
const loadVideoConfigs = async () => {
  try {
    const res = await aiConfigApi.getEnabledByType('video')
    if (res.code === 200 && res.data) {
      const configs = Array.isArray(res.data) ? res.data : []
      if (configs.length > 0) {
        // 用配置中的模型作为选项，保留默认值作为兜底
        const configModels = configs
          .filter((c: any) => c.model)
          .map((c: any) => ({ value: c.model, label: `${c.provider || '视频'} · ${c.model}` }))
        if (configModels.length > 0) {
          videoModelsFromConfig.value = configModels
          // 如果当前选中的模型不在列表中，切换到第一个
          const hasCurrent = configModels.some(m => m.value === selectedVideoModel.value)
          if (!hasCurrent) selectedVideoModel.value = configModels[0].value
        }
      }
    }
  } catch (e) {
    console.error('Failed to load video AI configs:', e)
  }
}

// 当选择分镜时，自动填入图片URL和提示词（优先宫格图→角色图→场景图）
watch(videoStoryboardId, (newVal) => {
  if (newVal) {
    const sb = ttsStoryboards.value.find((s: any) => s.id === newVal) // 复用已加载的分镜列表
    if (sb) {
      // ====== 智能提示词构建：组合分镜所有字段生成丰富描述 ======
      const parts: string[] = []
      if (sb.characterName) parts.push(`角色:${sb.characterName}`)
      if (sb.sceneName) parts.push(`场景:${sb.sceneName}`)
      // 优先级: action(动作描述) > description(镜头描述) > dialogue(台词)
      if (sb.action) parts.push(sb.action)
      else if (sb.description) parts.push(sb.description)
      else if (sb.dialogue) parts.push(`${sb.characterName || '角色'}说："${sb.dialogue}"`)
      if (sb.mood) parts.push(`情绪:${sb.mood}`)

      videoPrompt.value = parts.join('，')

      const pickImage = (url: string) => url ? { url, filename: '分镜图片' } : null

      // 根据模式填入不同的图片 + 更新预览状态
      if (videoGenerationMode.value === 'FIRST_LAST_FRAME') {
        firstFrameUrl.value = sb.sceneImageUrl || sb.gridImageUrl || ''
        lastFrameUrl.value = sb.gridImageUrl || sb.characterImageUrl || ''
        pickedFirstFrame.value = pickImage(firstFrameUrl.value)
        pickedLastFrame.value = pickImage(lastFrameUrl.value)
      } else if (videoGenerationMode.value === 'SUBJECT_REFERENCE') {
        subjectImageUrl.value = sb.characterImageUrl || sb.gridImageUrl || ''
        pickedSubjectImage.value = pickImage(subjectImageUrl.value)
      } else {
        if (sb.gridImageUrl) { videoImageUrl.value = sb.gridImageUrl }
        else if (sb.characterImageUrl) { videoImageUrl.value = sb.characterImageUrl }
        else if (sb.sceneImageUrl) { videoImageUrl.value = sb.sceneImageUrl }
        pickedVideoImage.value = pickImage(videoImageUrl.value)
      }
    }
  }
})

const generateVideo = async () => {
  // 根据模式验证必填项
  if (videoGenerationMode.value === 'TEXT_TO_VIDEO') {
    if (!videoPrompt.value.trim()) { AMessage.warning('请输入视频描述文本'); return }
  } else {
    // 图生视频/首尾帧/主体参考模式：prompt 不是必填但强烈建议有描述性文字，
    // 否则 MiniMax 会用默认描述生成，画面可能不够精准
      if (!videoPrompt.value.trim()) {
        // 仅警告，不阻止（用户可能确实只想用图片驱动）
        if (import.meta.env.DEV) console.warn('[视频生成] 未填写提示词，AI将使用默认描述生成，画面可能与预期不符')
      }
    if (!videoImageUrl.value && videoGenerationMode.value === 'IMAGE_TO_VIDEO') { 
      AMessage.warning('请输入参考图片URL或先选择有图的分镜'); return 
    }
    if ((!firstFrameUrl.value || !lastFrameUrl.value) && videoGenerationMode.value === 'FIRST_LAST_FRAME') { 
      AMessage.warning('请输入首帧和尾帧图片URL'); return 
    }
    if (!subjectImageUrl.value && videoGenerationMode.value === 'SUBJECT_REFERENCE') { 
      AMessage.warning('请输入主体参考图片URL（人脸照片）'); return 
    }
  }

  generatingVideo.value = true
  try {
    const res = await mediaApi.generateVideo({
      dramaId: dramaId.value,
      episodeNumber: 1,
      storyboardId: videoStoryboardId.value,
      mode: videoGenerationMode.value,
      prompt: videoPrompt.value,
      imageUrl: videoImageUrl.value,
      firstFrameUrl: firstFrameUrl.value,
      lastFrameUrl: lastFrameUrl.value,
      subjectImageUrl: subjectImageUrl.value,
      model: selectedVideoModel.value,
    })
    if (res.code === 200 && res.data) {
      videos.value.unshift(res.data)
      AMessage.success('视频任务已提交')
      if (res.data.status === 'processing') {
        AMessage.info('视频正在异步生成中，请稍后查看')
      }
      // 清空表单
      videoImageUrl.value = ''
      firstFrameUrl.value = ''
      lastFrameUrl.value = ''
      subjectImageUrl.value = ''
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

// 打开视频预览
const openVideoPreview = (video: any) => {
  if (!video.videoUrl) {
    AMessage.warning('视频尚未生成完成，请稍后再试')
    return
  }
  currentVideo.value = video
  videoPreviewOpen.value = true
}

// 关闭视频预览
const closeVideoPreview = () => {
  videoPreviewOpen.value = false
  currentVideo.value = null
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
const composeEpisodeNumber = ref(1)
const ffmpegAvailable = ref(true)
const composeRecords = ref<any[]>([])
const composeLoading = ref(false)
// 新增：上次合成的镜头对齐详情
const lastComposeDetails = ref<any[]>([])
const lastComposeSummary = ref<{ shotCount: number; totalDuration: number } | null>(null)

// 检查 FFmpeg 状态
const checkFfmpeg = async () => {
  try {
    const res = await composeApi.checkStatus()
    if (res.code === 200 && res.data) {
      ffmpegAvailable.value = res.data.available
      if (!ffmpegAvailable.value) {
        AMessage.warning('FFmpeg 未安装，合成功能暂不可用')
      }
    }
  } catch (e) { console.error(e) }
}

// 整集拼接合成
const composeEpisode = async () => {
  if (!ffmpegAvailable.value) { AMessage.warning('FFmpeg 未安装，无法进行视频合成'); return }

  composing.value = true
  lastComposeDetails.value = []
  lastComposeSummary.value = null

  try {
    const res = await composeApi.composeEpisode(dramaId.value, composeEpisodeNumber.value)
    if (res.code === 200 && res.data) {
      const data = res.data as any
      // 解析镜头详情（extraData 中的 shotDetails 数组）
      let details: any[] = []
      try {
        const extraData = typeof data.extraData === 'string' ? JSON.parse(data.extraData) : (data.extraData || {})
        details = extraData.shotDetails || []
      } catch (e) { /* extraData 解析失败则忽略 */ }

      // 统计策略分布
      const strategyCounts: Record<string, number> = {}
      let totalDur = 0
      for (const d of details) {
        const s = d.strategy || 'unknown'
        strategyCounts[s] = (strategyCounts[s] || 0) + 1
        totalDur += d.outputDuration || 0
      }
      
      lastComposeDetails.value = details
      lastComposeSummary.value = { shotCount: data.shotCount || details.length, totalDuration: Math.round(totalDur * 10) / 10 }

      AMessage.success(`第${composeEpisodeNumber.value}集合成成功！共 ${data.shotCount || 0} 个镜头，总时长 ${Math.round(totalDuration * 10) / 10}s`)

      // 刷新记录
      await loadComposeRecords()
      // 提供下载
      if (data.exportUrl) {
        window.open(data.exportUrl, '_blank')
      }
    } else {
      AMessage.error(res.message || '合成失败')
    }
  } catch (e: any) {
    AMessage.error(e?.message || '合成失败，请检查FFmpeg是否安装')
  } finally {
    composing.value = false
  }
}

/** 策略标签颜色映射 */
const strategyColor = (strategy: string): string => {
  switch (strategy) {
    case 'DIRECT_BLEND': return 'green'
    case 'EXTEND_VIDEO_FREEZE': return 'blue'
    case 'PAD_AUDIO_SILENCE': return 'orange'
    case 'VIDEO_ONLY': return 'default'
    case 'FAILED_FALLBACK': return 'red'
    case 'FALLBACK_SIMPLE': return 'gold'
    default: return 'default'
  }
}

/** 策略显示名称映射 */
const strategyLabel = (strategy: string): string => {
  switch (strategy) {
    case 'DIRECT_BLEND': return '✅ 直接混流'
    case 'EXTEND_VIDEO_FREEZE': return '🔵 冻结延展'
    case 'PAD_AUDIO_SILENCE': return '🟢 补静音'
    case 'VIDEO_ONLY': return '⚪ 纯画面'
    case 'FAILED_FALLBACK': return '🔴 降级(原始)'
    case 'FALLBACK_SIMPLE': return '🟡 降级(简单)'
    default: return strategy
  }
}

/** 策略分布统计（用于汇总行展示） */
const strategyDistribution = computed(() => {
  const dist: Record<string, number> = {}
  for (const d of lastComposeDetails.value) {
    const s = d.strategy || 'unknown'
    dist[s] = (dist[s] || 0) + 1
  }
  return dist
})

// 加载合成记录
const loadComposeRecords = async () => {
  composeLoading.value = true
  try {
    const res = await composeApi.listRecords({ dramaId: dramaId.value, pageNum: 1, pageSize: 10 })
    if (res.code === 200) {
      composeRecords.value = res.data?.records || []
    }
  } catch (e) { console.error(e) }
  finally { composeLoading.value = false }
}

// ====== 素材管理 ======
const assets = ref<any[]>([])
const assetsLoading = ref(false)
const assetsLoadingMore = ref(false)
const assetsPage = ref(1)
const assetsHasMore = ref(true)
const assetsTotal = ref(0)
const uploadLoading = ref(false)
const uploadProgress = ref(0)

// 文件上传
const handleUpload = async ({ file }: any) => {
  uploadLoading.value = true
  try {
    const res = await assetApi.upload(file.originFileObj || file, dramaId.value)
    if (res.code === 200 && res.data) {
      // 将新上传的素材插入到列表开头，并更新总数
      assets.value.unshift(res.data)
      assetsTotal.value++
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
    assetsTotal.value = Math.max(0, assetsTotal.value - 1)
    AMessage.success('已删除')
  } catch (e: any) {
    AMessage.error(e?.message || '删除失败')
  }
}

// ====== 数据加载 ======
const loadAudios = async () => {
  audiosLoading.value = true
  try {
    const res = await audioApi.listByDrama(dramaId.value)
    if (res.code === 200) audios.value = res.data || []
  } catch (e) { console.error(e) }
  finally { audiosLoading.value = false }
}

// 删除配音记录
const deleteAudio = async (id: string) => {
  try {
    const res = await audioApi.delete(id)
    if (res.code === 200) {
      audios.value = audios.value.filter(a => a.id !== id)
      AMessage.success('已删除')
    } else {
      AMessage.error(res.message || '删除失败')
    }
  } catch (e: any) {
    AMessage.error(e?.message || '删除失败')
  }
}

const loadVideos = async () => {
  videosLoading.value = true
  try {
    const res = await videoApi.listByDrama(dramaId.value)
    if (res.code === 200) videos.value = res.data || []
  } catch (e) { console.error(e) }
  finally { videosLoading.value = false }
}

/** 加载素材列表（支持分页） */
const loadAssets = async (isLoadMore = false) => {
  // dramaId 为空时不加载
  if (!dramaId.value) {
    console.log('[素材列表] dramaId 为空，跳过加载')
    return
  }
  
  // 防止重复加载
  if (isLoadMore && assetsLoadingMore.value) return
  if (!isLoadMore && assetsLoading.value) return
  
  if (isLoadMore) {
    assetsLoadingMore.value = true
  } else {
    assetsLoading.value = true
    assetsPage.value = 1
    assetsHasMore.value = true
  }
  
  try {
    console.log(`[素材列表] 开始加载第${assetsPage.value}页`)
    // 加载全部素材（不传 dramaId），与存储设置页面保持一致
    const res = await assetApi.page({ 
      pageNum: assetsPage.value, 
      pageSize: 24
    })
    if (res.code === 200) {
      const list = res.data?.records || []
      const total = res.data?.total || 0
      assetsTotal.value = total
      
      if (isLoadMore) {
        assets.value.push(...list)
      } else {
        assets.value = list
      }
      
      // 判断是否还有更多
      assetsHasMore.value = assets.value.length < total && list.length === 24
      console.log(`[素材列表] 第${assetsPage.value}页加载完成，本页${list.length}条，总计${assets.value.length}/${total}条`)
    }
  } catch (e) { 
    console.error('[素材列表] 加载失败:', e)
    AMessage.error('加载素材失败，请重试')
  }
  finally { 
    assetsLoading.value = false 
    assetsLoadingMore.value = false
  }
}

/** 素材列表滚动加载更多 */
const onAssetsScroll = (e: Event) => {
  const target = e.target as HTMLElement
  const scrollBottom = target.scrollHeight - target.scrollTop - target.clientHeight
  if (scrollBottom < 80 && assetsHasMore.value && !assetsLoadingMore.value && !assetsLoading.value) {
    assetsPage.value++
    loadAssets(true)
  }
}

// 定时轮询（5秒）
let pollTimer: ReturnType<typeof setInterval> | null = null

onMounted(async () => {
  // 安全防护：防止组件重复挂载导致多个并行定时器
  if (pollTimer) { clearInterval(pollTimer); pollTimer = null }

  // 只在 dramaId 有效时加载数据
  if (dramaId.value) {
    await Promise.all([loadAudios(), loadVideos(), loadAssets(), loadStoryboardsForTTS()])
    // 加载角色列表（用于TTS关联）
    try {
      const charRes = await (await import('@/utils/request')).characterApi.list(dramaId.value)
      if (charRes.code === 200) ttsCharacters.value = charRes.data || []
    } catch (e) { console.error(e) }
    // 加载视频AI配置（获取可用模型）
    await loadVideoConfigs()
    await loadComposeRecords()
  }
  
  await checkFfmpeg()
  pollTimer = setInterval(() => { pollVideos() }, 5000)

  // 检查查询参数（从工作台跳转过来）
  checkQueryParams()
})

// 检查URL查询参数，自动填入分镜信息
const checkQueryParams = () => {
  const query = route.query
  if (query.tab === 'video') {
    // 切换到视频生成Tab
    activeTab.value = 'video'

    // 自动填入分镜信息（注意：prompt 不在此处直接赋值，
    // 统一由下方「从分镜列表智能构建」或保留已有值，避免单字段 action 覆盖丰富描述）
    if (query.storyboardId) {
      videoStoryboardId.value = query.storyboardId as string
    }
    // query.action 仅在无分镜ID且当前prompt为空时作为降级值
    if (!query.storyboardId && query.action && !videoPrompt.value) {
      videoPrompt.value = query.action as string
    }

    // 自动填入图片（优先级：宫格图 > 角色图 > 场景图）
    if (query.gridImageUrl) {
      videoImageUrl.value = query.gridImageUrl as string
    } else if (query.characterImageUrl) {
      videoImageUrl.value = query.characterImageUrl as string
    } else if (query.sceneImageUrl) {
      videoImageUrl.value = query.sceneImageUrl as string
    }

    // 如果有分镜ID但没有图片，尝试从已加载的分镜列表中查找
    if (videoStoryboardId.value && !videoImageUrl.value) {
      const sb = ttsStoryboards.value.find((s: any) => s.id === videoStoryboardId.value)
      if (sb) {
        if (sb.gridImageUrl) videoImageUrl.value = sb.gridImageUrl
        else if (sb.characterImageUrl) videoImageUrl.value = sb.characterImageUrl
        else if (sb.sceneImageUrl) videoImageUrl.value = sb.sceneImageUrl
        // 智能提示词构建（与 watch 中的逻辑保持一致）
        if (!videoPrompt.value && (sb.action || sb.description || sb.dialogue)) {
          const parts: string[] = []
          if (sb.characterName) parts.push(`角色:${sb.characterName}`)
          if (sb.sceneName) parts.push(`场景:${sb.sceneName}`)
          if (sb.action) parts.push(sb.action)
          else if (sb.description) parts.push(sb.description)
          else if (sb.dialogue) parts.push(`${sb.characterName || '角色'}说："${sb.dialogue}"`)
          if (sb.mood) parts.push(`情绪:${sb.mood}`)
          videoPrompt.value = parts.join('，')
        }
      }
    }

    AMessage.success('已自动填入分镜信息，请选择生成模式并点击生成视频')
  }
}

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
  <div class="media-studio-container space-y-4 sm:space-y-6">
    <!-- 页面标题 -->
    <div class="flex items-center gap-3">
      <a-button type="text" @click="router.back()" class="!p-2 !text-[#a0a0a0] hover:!bg-[#242424] rounded-xl shrink-0">
        <ArrowLeftOutlined />
      </a-button>
      <h1 class="text-lg sm:text-xl font-semibold text-[#f5f5f5] flex items-center gap-2">
        <AudioOutlined />
        媒体工作室
      </h1>
    </div>

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

    <!-- ========== TTS Tab（关联分镜+角色+AI配置） ========== -->
    <div v-show="activeTab === 'tts'" class="space-y-4 w-full">
      <!-- TTS 生成区 -->
      <div class="bg-gradient-to-br from-[#1a1520] to-[#16101e] rounded-xl p-4 sm:p-5 border border-[#2a2238]">
        <h4 class="text-sm font-medium text-[#f5f5f5] mb-3 sm:mb-4 flex items-center gap-2">
          <AudioOutlined />
          文本转语音
          <span v-if="ttsStoryboards.length" class="ml-auto px-1.5 py-0.5 bg-white/10 rounded-full text-[9px] text-[#888]">{{ ttsStoryboards.length }}个有台词的分镜</span>
        </h4>

        <!-- 关联选择区：分镜 + 角色 -->
        <div class="grid grid-cols-1 sm:grid-cols-2 gap-3 mb-4">
          <div>
            <label class="block text-xs sm:text-sm text-[#a0a0a0] mb-1.5">
              关联分镜（自动填入台词）
              <span v-if="ttsStoryboards.length" class="ml-1 px-1.5 py-0.5 bg-green-500/10 rounded text-[9px] text-green-400">{{ ttsStoryboards.length }}个有台词</span>
            </label>
            <a-select
              v-model:value="ttsStoryboardId"
              placeholder="选择有台词的分镜..."
              size="large"
              allowClear
              showSearch
              class="w-full"
            >
              <a-select-option
                v-for="sb in ttsStoryboards"
                :key="sb.id"
                :value="sb.id"
              >#{{ sb.shotNumber }} {{ sb.characterName || '?' }} · {{ (sb.dialogue || '').substring(0, 28) }}{{ (sb.dialogue || '').length > 28 ? '...' : '' }}</a-select-option>
            </a-select>
            <p v-if="!ttsStoryboards.length && !audiosLoading" class="text-[10px] text-[#555] mt-1.5 flex items-center gap-1">
              <RobotOutlined /> 暂无分镜数据，请先在「剧集详情 → 分镜Tab」中 AI 拆解剧本
            </p>
          </div>
          <div>
            <label class="block text-xs sm:text-sm text-[#a0a0a0] mb-1.5">关联角色（可选，自动带出音色）</label>
            <a-select
              v-model:value="ttsCharacterId"
              placeholder="选择该分镜的角色..."
              size="large"
              allowClear
              showSearch
              class="w-full"
            >
              <a-select-option
                v-for="ch in ttsCharacters"
                :key="ch.id"
                :value="ch.id"
              >{{ ch.name }} {{ ch.voiceProvider ? `· ${ch.voiceProvider}` : '' }}</a-select-option>
            </a-select>
          </div>
        </div>

        <div class="grid grid-cols-1 sm:grid-cols-2 gap-3 sm:gap-4 mb-4">
          <div>
            <label class="block text-xs sm:text-sm text-[#a0a0a0] mb-1.5">选择音色
              <a-button type="link" size="small" class="!p-0 !ml-1" @click="handlePreviewVoice" :loading="previewTTS">
                试听 →
              </a-button>
            </label>
            <a-select v-model:value="selectedVoice" size="large" class="w-full">
              <a-select-option v-for="v in ttsVoices" :key="v.value" :value="v.value">{{ v.label }}</a-select-option>
            </a-select>
          </div>
          <div>
            <label class="block text-xs sm:text-sm text-[#a0a0a0] mb-1.5">预览效果</label>
            <div class="flex items-center gap-2 h-[38px] sm:h-[42px] px-3 sm:px-4 bg-[#0f0d14] border border-[#2a2238] rounded-xl text-[#606060] text-xs sm:text-sm">
              <span v-if="previewAudioUrl" class="flex items-center gap-2 flex-1">
                <audio :src="previewAudioUrl" controls class="h-6 w-full !bg-transparent" />
              </span>
              <span v-else-if="selectedVoice">{{ ttsVoices.find(v => v.value === selectedVoice)?.label || selectedVoice }}</span>
              <span v-else>请先选择音色</span>
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
                <p class="text-xs sm:text-sm text-[#e0e0e0] truncate">
                  {{ audio.text?.substring(0, 50) }}{{ (audio.text || '').length > 50 ? '...' : '' }}<span v-if="audio.storyboardId" class="text-[#6366f1] ml-1">· #{{ (ttsStoryboards.find((s:any) => s.id === audio.storyboardId)?.shotNumber || audio.storyboardId?.substring(0,8)) }}</span>
                </p>
                <p class="text-[10px] xs:text-xs text-[#606060] mt-0.5">{{ audio.provider }} · {{ audio.voiceId }}{{ audio.duration ? ` · ${audio.duration.toFixed(1)}s` : '' }}</p>
              </div>
              <a-tag
                :color="statusColorMap[audio.status] || '#333'"
                :class="[statusTextColorMap[audio.status] || '!text-[#808080]', 'shrink-0']"
              >
                {{ statusLabelMap[audio.status] || audio.status }}
              </a-tag>
              <!-- 删除按钮 -->
              <a-popconfirm
                title="确定删除该配音？"
                ok-text="确定"
                cancel-text="取消"
                @confirm="deleteAudio(audio.id)"
              >
                <a-button
                  type="text"
                  danger
                  size="small"
                  class="opacity-0 group-hover:opacity-100 !p-1 shrink-0"
                >
                  <template #icon><DeleteOutlined /></template>
                </a-button>
              </a-popconfirm>
            </div>
          </div>
          <div v-else class="text-center py-12">
            <a-empty description="暂无配音记录，在上方输入文本后点击生成" />
          </div>
        </a-spin>
      </div>
    </div>

    <!-- ========== Video Tab（关联分镜+AI配置模型） ========== -->
    <div v-show="activeTab === 'video'" class="space-y-4 w-full">
      <!-- 视频生成区 -->
      <div class="bg-gradient-to-br from-[#151a25] to-[#101520] rounded-xl p-4 sm:p-5 border border-[#222a38]">
        <h4 class="text-sm font-medium text-[#f5f5f5] mb-3 sm:mb-4 flex items-center gap-2">
          <VideoCameraOutlined />
          AI 视频生成
        </h4>

        <!-- 视频生成模式选择 -->
        <div class="mb-4">
          <label class="block text-xs sm:text-sm text-[#a0a0a0] mb-1.5">生成模式</label>
          <a-radio-group v-model:value="videoGenerationMode" size="small" class="w-full">
            <div class="grid grid-cols-2 gap-2">
              <a-radio-button v-for="mode in videoModes" :key="mode.value" :value="mode.value" class="!h-auto !py-2">
                <div class="text-left">
                  <div class="text-xs font-medium">{{ mode.label }}</div>
                  <div class="text-[9px] text-[#888] mt-0.5">{{ mode.desc }}</div>
                </div>
              </a-radio-button>
            </div>
          </a-radio-group>
        </div>

        <!-- 关联分镜选择（自动填入图片URL和提示词） -->
        <div class="mb-3">
          <label class="block text-xs sm:text-sm text-[#a0a0a0] mb-1.5">
            关联分镜（自动填入数据）
            <span v-if="ttsStoryboards.filter((s:any) => s.gridImageUrl || s.characterImageUrl || s.sceneImageUrl).length" class="ml-1 px-1.5 py-0.5 bg-green-500/10 rounded text-[9px] text-green-400">{{ ttsStoryboards.filter((s:any) => s.gridImageUrl || s.characterImageUrl || s.sceneImageUrl).length }}个有图</span>
          </label>
          <a-select
            v-model:value="videoStoryboardId"
            placeholder="选择分镜自动填入图片和描述..."
            size="large"
            allowClear
            showSearch
            class="w-full"
          >
            <a-select-option
              v-for="sb in ttsStoryboards"
              :key="'v-' + sb.id"
              :value="sb.id"
              :disabled="videoGenerationMode.value !== 'TEXT_TO_VIDEO' && !sb.gridImageUrl && !sb.characterImageUrl && !sb.sceneImageUrl"
            >#{{ sb.shotNumber }} {{ sb.characterName || '' }} <span v-if="sb.action" class="text-[#666]">· {{ sb.action.slice(0, 20) }}...</span> <span v-if="(sb.gridImageUrl || sb.characterImageUrl || sb.sceneImageUrl)" title="有图">📸</span><span v-else class="text-[#555]">(无图)</span></a-select-option>
          </a-select>
          <p v-if="!ttsStoryboards.length && !videosLoading" class="text-[10px] text-[#555] mt-1.5 flex items-center gap-1">
            <RobotOutlined /> 暂无分镜数据，请先在「剧集详情 → 分镜Tab」中 AI 拆解剧本并生成宫格图
          </p>
        </div>

        <!-- 视频描述/提示词（所有模式都需要） -->
        <div class="mb-3">
          <label class="block text-xs sm:text-sm text-[#a0a0a0] mb-1.5">视频描述 <span class="text-[#666]">（选填，建议描述动作和画面）</span></label>
          <a-textarea v-model:value="videoPrompt" :rows="2" placeholder="描述你想要的视频内容，例如：角色缓缓转身，背景是夕阳下的海滩..." size="large" />
        </div>

        <!-- 图生视频：参考图片（从存储选择） -->
        <div v-if="videoGenerationMode === 'IMAGE_TO_VIDEO'" class="mb-4">
          <label class="block text-xs sm:text-sm text-[#a0a0a0] mb-1.5">参考图片</label>
          <div v-if="pickedVideoImage" class="flex items-center gap-3 p-2 bg-[#0f0d14] border border-[#2a2238] rounded-xl">
            <img :src="pickedVideoImage.url" class="w-16 h-16 object-cover rounded-lg shrink-0 border border-[#333]" />
            <div class="flex-1 min-w-0">
              <p class="text-xs text-[#e0e0e0] truncate">{{ pickedVideoImage.filename }}</p>
              <p class="text-[10px] text-[#555] mt-0.5 truncate">{{ pickedVideoImage.url }}</p>
            </div>
            <a-button type="text" danger size="small" @click="clearPickedImage('videoImage')">
              <template #icon><DeleteOutlined /></template>
            </a-button>
          </div>
          <a-button v-else block size="large" class="!border-dashed !h-14" @click="openImagePicker('videoImage')">
            <template #icon><FileImageOutlined /></template> 从素材库选择参考图片
          </a-button>
        </div>

        <!-- 首尾帧模式：首帧和尾帧（从存储选择） -->
        <div v-if="videoGenerationMode === 'FIRST_LAST_FRAME'" class="mb-4 space-y-3">
          <div>
            <label class="block text-xs sm:text-sm text-[#a0a0a0] mb-1.5">首帧图片 <span class="text-[#666]">（视频开始画面）</span></label>
            <div v-if="pickedFirstFrame" class="flex items-center gap-3 p-2 bg-[#0f0d14] border border-[#2a2238] rounded-xl">
              <img :src="pickedFirstFrame.url" class="w-16 h-16 object-cover rounded-lg shrink-0 border border-[#333]" />
              <div class="flex-1 min-w-0">
                <p class="text-xs text-[#e0e0e0] truncate">{{ pickedFirstFrame.filename }}</p>
              </div>
              <a-button type="text" danger size="small" @click="clearPickedImage('firstFrame')">
                <template #icon><DeleteOutlined /></template>
              </a-button>
            </div>
            <a-button v-else block size="large" class="!border-dashed !h-12" @click="openImagePicker('firstFrame')">
              <template #icon><FileImageOutlined /></template> 选择首帧图片
            </a-button>
          </div>
          <div>
            <label class="block text-xs sm:text-sm text-[#a0a0a0] mb-1.5">尾帧图片 <span class="text-[#666]">（视频结束画面）</span></label>
            <div v-if="pickedLastFrame" class="flex items-center gap-3 p-2 bg-[#0f0d14] border border-[#2a2238] rounded-xl">
              <img :src="pickedLastFrame.url" class="w-16 h-16 object-cover rounded-lg shrink-0 border border-[#333]" />
              <div class="flex-1 min-w-0">
                <p class="text-xs text-[#e0e0e0] truncate">{{ pickedLastFrame.filename }}</p>
              </div>
              <a-button type="text" danger size="small" @click="clearPickedImage('lastFrame')">
                <template #icon><DeleteOutlined /></template>
              </a-button>
            </div>
            <a-button v-else block size="large" class="!border-dashed !h-12" @click="openImagePicker('lastFrame')">
              <template #icon><FileImageOutlined /></template> 选择尾帧图片
            </a-button>
          </div>
        </div>

        <!-- 主体参考模式：主体参考图片（从存储选择） -->
        <div v-if="videoGenerationMode === 'SUBJECT_REFERENCE'" class="mb-4">
          <label class="block text-xs sm:text-sm text-[#a0a0a0] mb-1.5">主体参考图片 <span class="text-[#666]">（人脸照片，用于保持人物特征）</span></label>
          <div v-if="pickedSubjectImage" class="flex items-center gap-3 p-2 bg-[#0f0d14] border border-[#2a2238] rounded-xl">
            <img :src="pickedSubjectImage.url" class="w-16 h-16 object-cover rounded-lg shrink-0 border border-[#333]" />
            <div class="flex-1 min-w-0">
              <p class="text-xs text-[#e0e0e0] truncate">{{ pickedSubjectImage.filename }}</p>
            </div>
            <a-button type="text" danger size="small" @click="clearPickedImage('subjectImage')">
              <template #icon><DeleteOutlined /></template>
            </a-button>
          </div>
          <a-button v-else block size="large" class="!border-dashed !h-12" @click="openImagePicker('subjectImage')">
            <template #icon><FileImageOutlined /></template> 选择主体参考图片（人脸照片）
          </a-button>
        </div>

        <!-- 视频模型：优先显示AI配置中的模型 -->
        <div class="mb-4">
          <label class="block text-xs sm:text-sm text-[#a0a0a0] mb-1.5">
            视频模型
            <span v-if="videoModelsFromConfig.length" class="ml-1 px-1.5 py-0.5 bg-green-500/10 rounded text-[9px] text-green-400">来自AI配置</span>
          </label>
          <a-select v-model:value="selectedVideoModel" size="large" class="w-full">
            <!-- AI配置中的模型优先展示 -->
            <a-select-option v-for="m in videoModelsFromConfig" :key="m.value" :value="m.value">{{ m.label }}</a-select-option>
            <a-select-option value="MiniMax-Hailuo-2.3">MiniMax 海螺 2.3（默认）</a-select-option>
            <a-select-option value="video-01">MiniMax Video-01</a-select-option>
          </a-select>
          <p v-if="videoModelsFromConfig.length === 0" class="text-[9px] text-[#555] mt-1">💡 提示：可在 AI 配置页面添加 video 类型的配置，模型会自动出现在这里</p>
        </div>

        <div class="flex justify-between items-center">
          <span></span>
          <a-button
            type="primary"
            :loading="generatingVideo"
            :disabled="!videoPrompt.trim()"
            @click="generateVideo"
          >
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
                <p class="text-xs sm:text-sm text-[#e0e0e0] truncate">
                  {{ v.model }}<span v-if="v.storyboardId"> · 分镜 #{{ (ttsStoryboards.find((s:any) => s.id === v.storyboardId)?.shotNumber || v.storyboardId?.substring(0,8)) }}</span>
                </p>
                <p class="text-[10px] xs:text-xs text-[#606060] mt-0.5">{{ v.provider }}{{ v.taskId ? ` · ${v.taskId.substring(0, 12)}...` : '' }}{{ v.duration ? ` · ${v.duration.toFixed(1)}s` : '' }}</p>
              </div>
              <a-tag
                :color="statusColorMap[v.status] || '#333'"
                :class="[statusTextColorMap[v.status] || '!text-[#808080]', 'shrink-0']"
              >
                {{ statusLabelMap[v.status] || v.status }}
              </a-tag>
              <!-- 播放按钮 -->
              <a-button
                v-if="v.videoUrl"
                type="text"
                size="small"
                class="opacity-0 group-hover:opacity-100 !p-1"
                @click="openVideoPreview(v)"
              >
                <template #icon><PlayCircleOutlined /></template>
              </a-button>
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
          <h4 class="text-xs sm:text-sm font-medium text-[#808080]">已上传素材 ({{ assets.length }}{{ assetsTotal > 0 ? `/${assetsTotal}` : '' }})</h4>
          <a-button size="small" type="text" :loading="assetsLoading" @click="() => loadAssets(false)">
            <template #icon><ReloadOutlined /></template> 刷新
          </a-button>
        </div>
        <a-spin :spinning="assetsLoading">
          <!-- 滚动容器 -->
          <div 
            class="max-h-[60vh] overflow-y-auto pr-1" 
            @scroll="onAssetsScroll"
          >
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
            <div v-else-if="!assetsLoading" class="text-center py-12">
              <a-empty description="暂无素材，拖拽文件到上方区域上传" />
            </div>
            
            <!-- 加载更多指示 -->
            <div v-if="assetsLoadingMore" class="flex justify-center py-4">
              <a-spin size="small" tip="加载更多..." />
            </div>
            <div v-else-if="!assetsHasMore && assets.length > 0" class="text-center py-4 text-[#666] text-xs">
              已加载全部 {{ assets.length }} 个素材
            </div>
          </div>
        </a-spin>
      </div>
    </div>

    <!-- ========== Compose Tab（合成输出） ========== -->
    <div v-show="activeTab === 'compose'" class="space-y-4 w-full">
      <!-- FFmpeg 状态提示 -->
      <div v-if="!ffmpegAvailable" class="bg-red-500/10 border border-red-500/20 rounded-xl p-3 flex items-center gap-2 text-red-400">
        <EditOutlined />
        <span class="text-xs">FFmpeg 未安装或不可用，视频合成功能暂时禁用。请在服务器安装 FFmpeg 后重试。</span>
      </div>

      <!-- 合成操作区 -->
      <div class="rounded-xl p-5 sm:p-6 border border-[#2a2a2a] bg-[#1a1a1a]">
        <h4 class="text-sm font-medium text-[#f5f5f5] mb-4 flex items-center gap-2">
          <MergeCellsOutlined style="color: #6366f1;" />
          整集视频合成
        </h4>

        <p class="text-xs sm:text-sm text-[#888] mb-4 max-w-lg">
          将当前剧集所有已完成的分镜视频按镜头顺序拼接为完整剧集。
        </p>

        <div class="flex items-end gap-3 mb-2">
          <div class="flex-1 min-w-0">
            <label class="block text-xs text-[#a0a0a0] mb-1.5">集数</label>
            <a-input-number v-model:value="composeEpisodeNumber" :min="1" :max="100" size="large" class="w-full" />
          </div>
          <a-button
            type="primary"
            :loading="composing"
            :disabled="composing || !ffmpegAvailable"
            @click="composeEpisode"
            size="large"
            class="!rounded-xl shrink-0 px-6"
          >
            <template #icon><MergeCellsOutlined /></template>
            {{ composing ? '合成中...' : `开始合成第${composeEpisodeNumber}集` }}
          </a-button>
        </div>

        <p v-if="ffmpegAvailable" class="text-[10px] text-[#555] mt-2">💡 前提：工作台中该集至少有 1 个状态为"已完成"的视频</p>
      </div>

      <!-- 镜头对齐详情（合成后展示） -->
      <div v-if="lastComposeDetails.length > 0" class="rounded-xl p-5 sm:p-6 border border-[#2a2a2a] bg-[#1a1a1a]">
        <h4 class="text-sm font-medium text-[#f5f5f5] mb-3 flex items-center gap-2">
          🔗 音画对齐详情
        </h4>
        
        <!-- 汇总行 -->
        <div v-if="lastComposeSummary" class="flex flex-wrap items-center gap-4 mb-3 pb-3 border-b border-[#2a2a2a]">
          <span class="text-xs text-[#aaa]">
            共 <span class="text-white font-medium">{{ lastComposeSummary.shotCount }}</span> 个镜头
            · 总时长 <span class="text-green-400 font-medium">{{ lastComposeSummary.totalDuration }}s</span>
          </span>
          <!-- 策略分布 -->
          <template v-for="(count, strategy) in strategyDistribution" :key="strategy">
            <a-tag :color="strategyColor(strategy)" class="!text-xs !py-0 !px-1.5 !rounded-md">
              {{ strategyLabel(strategy) }} × {{ count }}
            </a-tag>
          </template>
        </div>

        <!-- 每个镜头详情表 -->
        <div class="overflow-x-auto">
          <table class="w-full text-xs">
            <thead>
              <tr class="text-[#666] text-left border-b border-[#2a2a2a]">
                <th class="pb-2 pr-3 font-medium">#</th>
                <th class="pb-2 pr-3 font-medium">策略</th>
                <th class="pb-2 pr-3 font-medium">视频</th>
                <th class="pb-2 pr-3 font-medium">音频</th>
                <th class="pb-2 pr-3 font-medium">输出</th>
                <th class="pb-2 pr-3 font-medium">台词</th>
                <th class="pb-2 font-medium">字幕</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="detail in lastComposeDetails" :key="detail.shotNumber"
                class="border-b border-[#1f1f1f] hover:bg-[#222] transition-colors">
                <td class="py-2 pr-3 text-[#888]">#{{ detail.shotNumber }}</td>
                <td class="py-2 pr-3">
                  <a-tag :color="strategyColor(detail.strategy)" class="!text-[10px] !py-0 !px-1 !rounded">
                    {{ strategyLabel(detail.strategy) }}
                  </a-tag>
                </td>
                <td class="py-2 pr-3 text-[#ccc]">{{ (detail.videoDuration || 0).toFixed(1) }}s</td>
                <td class="py-2 pr-3" :class="detail.audioDuration > detail.videoDuration ? 'text-orange-400' : detail.videoDuration > 0 && detail.audioDuration > 0 ? 'text-blue-400' : 'text-[#555]'">
                  {{ detail.audioDuration > 0 ? detail.audioDuration.toFixed(1) + 's' : '-' }}
                </td>
                <td class="py-2 pr-3 text-green-400 font-medium">{{ (detail.outputDuration || 0).toFixed(1) }}s</td>
                <td class="py-2 pr-3 text-[#999] max-w-[150px] truncate" :title="detail.dialogue">
                  {{ detail.dialogue || '-' }}
                </td>
                <td class="py-2">
                  <span v-if="detail.hasSubtitle" class="text-purple-400">✓ ASS</span>
                  <span v-else class="text-[#444]">-</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- 策略说明 -->
        <div class="mt-3 pt-3 border-t border-[#2a2a2a] flex flex-wrap gap-x-4 gap-y-1 text-[10px] text-[#555]">
          <span>✅ 直接混流 = 音画时长相近</span>
          <span>🔵 冻结延展 = 配音较长，画面末帧延展</span>
          <span>🟢 补静音 = 配音较短，音频后补空白</span>
          <span>⚪ 纯画面 = 无配音</span>
        </div>
      </div>

      <!-- 合成历史 -->
      <div>
        <div class="flex items-center justify-between mb-3">
          <h4 class="text-xs sm:text-sm font-medium text-[#808080]">合成记录 ({{ composeRecords.length }})</h4>
          <a-button size="small" type="text" :loading="composeLoading" @click="loadComposeRecords">
            <template #icon><ReloadOutlined /></template> 刷新
          </a-button>
        </div>

        <a-spin :spinning="composeLoading">
          <div v-if="composeRecords.length > 0" class="space-y-2">
            <div v-for="record in composeRecords" :key="record.id"
              class="flex items-center gap-2 sm:gap-3 p-3 bg-[#1a1a1a] rounded-xl border border-[#2a2a2a] group hover:border-[#6366f1]/30 transition-all">
              <div class="w-8 h-8 sm:w-9 sm:h-9 flex items-center justify-center rounded-lg bg-green-500/10 shrink-0">
                <MergeCellsOutlined style="color: #34d399; font-size: 16px;" />
              </div>
              <div class="flex-1 min-w-0">
                <p class="text-xs sm:text-sm text-[#e0e0e0] font-medium">第{{ record.episodeNumber }}集 · {{ record.shotCount || 0 }}个镜头</p>
                <p class="text-[10px] xs:text-xs text-[#606060] mt-0.5">
                  {{ formatTime(record.createdAt) }} · 时长 {{ formatDuration(record.duration) }}
                </p>
              </div>
              <a-tag v-if="record.status === 'completed'" color="#16a34a15" class="!text-green-400 !rounded-lg shrink-0">成功</a-tag>
              <a-button
                v-if="record.exportUrl"
                type="link" size="small"
                @click="openUrl(record.exportUrl)"
                class="shrink-0"
              >
                <template #icon><DownloadOutlined /></template>下载
              </a-button>
            </div>
          </div>
          <div v-else class="text-center py-8">
            <a-empty description="暂无合成记录，生成足够多的分镜视频后点击上方按钮开始合成" :image-style="{ opacity: 0.3 }" />
          </div>
        </a-spin>
      </div>

      <!-- 功能卡片说明 -->
      <div class="grid grid-cols-1 sm:grid-cols-3 gap-3 sm:gap-4">
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

  <!-- 视频预览模态框 -->
  <a-modal
    v-model:open="videoPreviewOpen"
    :title="currentVideo ? `视频预览 - ${currentVideo.model || 'Generated Video'}` : '视频预览'"
    :footer="null"
    width="800px"
    :centered="true"
    @cancel="closeVideoPreview"
  >
    <div v-if="currentVideo" class="video-preview-container">
      <video
        :src="currentVideo.videoUrl"
        controls
        style="width: 100%; max-height: 500px; border-radius: 8px;"
        preload="metadata"
      >
        <p>您的浏览器不支持视频播放</p>
      </video>
      <div class="mt-4 space-y-2">
        <p class="text-sm text-[#a0a0a0]">
          <span class="text-[#808080]">模型:</span> {{ currentVideo.model }}
        </p>
        <p class="text-sm text-[#a0a0a0]">
          <span class="text-[#808080]">提供商:</span> {{ currentVideo.provider }}
        </p>
        <p v-if="currentVideo.duration" class="text-sm text-[#a0a0a0]">
          <span class="text-[#808080]">时长:</span> {{ currentVideo.duration.toFixed(1) }}s
        </p>
        <p class="text-sm text-[#a0a0a0] truncate">
          <span class="text-[#808080]">视频URL:</span> {{ currentVideo.videoUrl }}
        </p>
      </div>
    </div>
  </a-modal>

  <!-- 图片选择器弹窗 -->
  <a-modal
    v-model:open="imagePickerOpen"
    title="选择图片"
    :footer="null"
    width="700px"
    :centered="true"
  >
    <div class="space-y-3">
      <p class="text-xs text-[#888]">选择一张图片作为{{ {videoImage:'参考图片',firstFrame:'首帧图片',lastFrame:'尾帧图片',subjectImage:'主体参考图片'}[imagePickerTarget] }}</p>
      
      <!-- 上传区域 -->
      <div class="border border-dashed border-[#3a3a4a] rounded-lg p-3 hover:border-[#6366f1]/40 transition-colors">
        <a-upload
          name="file"
          :show-upload-list="false"
          :before-upload="(file: any) => { handlePickerUpload(file); return false; }"
          accept="image/*"
        >
          <div class="flex items-center justify-center gap-2 cursor-pointer text-[#808080] hover:text-[#6366f1] transition-colors">
            <CloudUploadOutlined />
            <span class="text-xs">点击上传图片到素材库</span>
          </div>
        </a-upload>
      </div>

      <!-- 图片网格滚动容器 -->
      <div class="max-h-[400px] overflow-y-auto pr-1" @scroll="onPickerScroll">
        <a-spin :spinning="imagePickerLoading">
          <!-- 图片网格 -->
          <div v-if="imagePickerAssets.length > 0" class="grid grid-cols-3 sm:grid-cols-4 gap-2">
            <div
              v-for="asset in imagePickerAssets"
              :key="asset.id"
              class="relative aspect-square bg-[#1a1a1a] rounded-lg border border-[#2a2a2a] overflow-hidden cursor-pointer hover:border-[#6366f1]/60 transition-all group"
              @click="selectImage(asset)"
            >
              <img :src="asset.fileUrl" class="w-full h-full object-cover" />
              <div class="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
                <span class="text-white text-xs font-medium px-2 py-1 rounded bg-[#6366f1]">选择</span>
              </div>
              <div class="absolute bottom-0 left-0 right-0 p-1 bg-gradient-to-t from-black/80 to-transparent">
                <p class="text-[9px] text-white truncate">{{ asset.filename }}</p>
              </div>
            </div>
          </div>
          <!-- 空状态 -->
          <div v-else-if="!imagePickerLoading && imagePickerAssets.length === 0" class="text-center py-12">
            <a-empty description="素材库暂无图片，请先上传" />
            <a-button type="link" size="small" @click="imagePickerOpen = false; activeTab='assets'">
              去上传 →
            </a-button>
          </div>
        </a-spin>
        <!-- 加载更多指示（放在滚动容器内） -->
        <div v-if="imagePickerLoadingMore" class="flex justify-center py-3">
          <a-spin size="small" tip="加载更多..." />
        </div>
        <div v-else-if="!imagePickerHasMore && imagePickerAssets.length > 0" class="text-center py-3 text-[#666] text-xs">
          已加载全部 {{ imagePickerAssets.length }} 张图片
        </div>
      </div>
    </div>
  </a-modal>
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

// 格式化时长
function formatDuration(seconds?: number): string {
  if (!seconds) return '--:--'
  const mins = Math.floor(seconds / 60)
  const secs = Math.floor(seconds % 60)
  return `${mins}:${secs.toString().padStart(2, '0')}`
}

// 格式化时间
function formatTime(time?: string): string {
  if (!time) return '-'
  const d = new Date(time)
  return `${d.getMonth()+1}/${d.getDate()} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
}

// 打开URL
function openUrl(url: string) { window.open(url, '_blank') }

export default { data: () => ({ formatFileSize, formatDuration, formatTime }) }
</script>

<style scoped>
.media-studio-container {
  width: 100%;
  max-width: 1280px;
  margin: 0 auto;
  padding: 0 20px;
  box-sizing: border-box;
}
@media (min-width: 768px) {
  .media-studio-container { padding: 0 32px; }
}
@media (min-width: 1024px) {
  .media-studio-container { padding: 0 48px; }
}
.space-y-4 > * + * { margin-top: 1rem; }
.sm\:space-y-6 > * + * { margin-top: 1.5rem; }
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
