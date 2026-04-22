<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { dramaApi, characterApi, sceneApi, storyboardApi } from '@/utils/request'
import { useDramaStore } from '@/stores/drama'
import { message as AMessage } from 'ant-design-vue'
import {
  ArrowLeftOutlined,
  SaveOutlined,
  PlusOutlined,
  DeleteOutlined,
  EditOutlined,
  RobotOutlined,
  SendOutlined,
  PictureOutlined,
  UserOutlined,
  AppstoreOutlined,
  SettingOutlined,
  UploadOutlined,
  FolderOpenOutlined,
  CloseCircleOutlined,
  VideoCameraOutlined,
  SoundOutlined,
  PlayCircleOutlined,
} from '@ant-design/icons-vue'
import { storageApi } from '@/utils/storage'
import { assetApi } from '@/utils/asset'
import { aiConfigApi, ttsPreviewApi } from '@/utils/aiConfig'

const route = useRoute()
const router = useRouter()
const store = useDramaStore()

const loading = ref(false)
const saving = ref(false)
const loadError = ref('')

const isNew = computed(() => route.params.id === 'new')

// Tab state
const activeTab = ref('characters')
const tabs = [
  { key: 'characters', label: '角色', icon: UserOutlined },
  { key: 'scenes', label: '场景', icon: AppstoreOutlined },
  { key: 'storyboards', label: '分镜', icon: PictureOutlined },
  { key: 'settings', label: '设置', icon: SettingOutlined },
]

// ====== 数据加载 ======
const loadData = async () => {
  if (isNew.value) return
  loading.value = true
  loadError.value = ''
  try {
    const [dramaRes, charRes, sceneRes, sbRes] = await Promise.all([
      dramaApi.get(route.params.id as string),
      characterApi.list(route.params.id as string),
      sceneApi.list(route.params.id as string),
      storyboardApi.list(route.params.id as string, 1), // 默认加载第1集的分镜
    ])
    if (dramaRes.code === 200) {
      store.setDrama(dramaRes.data)
    } else {
      throw new Error(dramaRes.message || '加载剧集失败')
    }
    if (charRes.code === 200) store.setCharacters(charRes.data || [])
    if (sceneRes.code === 200) store.setScenes(sceneRes.data || [])
    if (sbRes.code === 200) sbList.value = sbRes.data || []
  } catch (e: any) {
    const msg = e?.message || '加载数据失败，请检查后端服务是否启动（端口8080）'
    loadError.value = msg
    console.error('加载剧集详情失败:', e)
    AMessage.error(msg)
  } finally {
    loading.value = false
  }
}

const saveDrama = async () => {
  const d = store.currentDrama
  if (!d?.title) return
  saving.value = true
  try {
    if (isNew.value) {
      const res = await dramaApi.create(d)
      if (res.code === 200) router.replace(`/drama/${res.data.id}`)
    } else {
      await dramaApi.update(d.id, d)
      AMessage.success('保存成功')
    }
  } catch (e: any) {
    AMessage.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// 初始化（新建模式或加载已有数据）
const initPage = () => {
  loadError.value = ''
  if (isNew.value) {
    // 新建剧集：重置 store 并设置默认值
    store.reset()
    store.setDrama({ title: '', description: '', totalEpisodes: 1 })
  } else {
    // 编辑模式：加载数据
    loadData()
  }
}

onMounted(() => {
  initPage()
})

// 监听路由变化（从 /drama/xxx → /drama/new 或反向切换时重新初始化）
watch(() => route.params.id, () => {
  initPage()
})

// ====== 角色管理 ======
const charModalOpen = ref(false)
const editingChar = ref<any>(null)
const charForm = ref<any>({ name: '', description: '', imageUrl: '', appearancePrompt: '', dialogueStyle: '', voiceId: '', voiceProvider: '', previewAudioUrl: '' })
const ttsConfigList = ref<any[]>([])
const ttsVoices = ref<{ value: string; label: string }[]>([])
const previewingVoice = ref(false)
const previewAudioUrl = ref<string>('')
const previewAudioRef = ref<HTMLAudioElement | null>(null)

// MiniMax TTS 官方音色列表（用于下拉选择）- 完整版
// 参考: https://platform.minimaxi.com/docs/faq/system-voice-id
const minimaxTTSVoices = [
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
  { value: 'chunzhen_xuedi', label: '⭐ 纯真学弟' },
  { value: 'lengdan_xiongzhang', label: '🧊 冷淡学长' },
  { value: 'badao_shaoye', label: '⭐ 霸道少爷' },
  { value: 'tianxin_xiaoling', label: '⭐ 甜心小玲' },
  { value: 'qiaopi_mengmei', label: '😜 俏皮萌妹' },
  { value: 'wumei_yujie', label: '💋 妩媚御姐' },
  { value: 'diadia_xuemei', label: '⭐ 嗲嗲学妹' },
  { value: 'danya_xuejie', label: '📚 淡雅学姐' },
  // ====== 中文(普通话) - 专业/特色音色 ======
  { value: 'Chinese (Mandarin)_Reliable_Executive', label: '💼 沉稳高管' },
  { value: 'Chinese (Mandarin)_News_Anchor', label: '📺 新闻女声' },
  { value: 'Chinese (Mandarin)_Mature_Woman', label: '💅 傲娇御姐' },
  { value: 'Chinese (Mandarin)_Unrestrained_Young_Man', label: '⭐ 不羁青年' },
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
]

const openCharModal = async (item?: any) => {
  editingChar.value = item || null
  charForm.value = item ? { ...item } : { name: '', description: '', imageUrl: '', appearancePrompt: '', dialogueStyle: '', voiceId: '', voiceProvider: '', previewAudioUrl: '' }
  // 加载已保存的试听音频URL
  previewAudioUrl.value = item?.previewAudioUrl || ''
  // 加载 AI 配置中的 TTS 类型，获取可用模型和默认音色
  await loadTTSConfigs()
  charModalOpen.value = true
}

// 加载 TTS 类型的 AI 配置
const loadTTSConfigs = async () => {
  try {
    const res = await aiConfigApi.getEnabledByType('tts')
    if (res.code === 200 && res.data) {
      ttsConfigList.value = Array.isArray(res.data) ? res.data : []
      // 从配置中提取模型名作为参考信息
      if (ttsConfigList.value.length > 0 && import.meta.env.DEV) {
        console.log('Loaded {} TTS config(s): model={}', ttsConfigList.value.length, ttsConfigList.value[0].model)
      }
    }
  } catch (e) {
    console.error('Failed to load TTS configs:', e)
  }
  // 始终使用内置音色列表作为选项
  ttsVoices.value = minimaxTTSVoices
}

// 试听音色
const previewVoice = async () => {
  if (!charForm.value.voiceId) { AMessage.warning('请先选择音色'); return }
  previewingVoice.value = true
  previewAudioUrl.value = ''
  try {
    const res = await ttsPreviewApi.preview({
      text: `你好，我是${charForm.value.name || '该角色'}。这是一段试听文本，用于测试音色效果。`,
      voiceId: charForm.value.voiceId,
      characterId: editingChar.value?.id,
    })
    if (res.code === 200 && res.data?.audioData) {
      previewAudioUrl.value = res.data.audioData
      charForm.value.previewAudioUrl = res.data.audioData
      AMessage.success('试听生成成功，已归档到存储')
    } else {
      AMessage.error(res.message || '试听生成失败')
    }
  } catch (e: any) {
    AMessage.error(e?.message || '试听失败，请检查AI配置')
  } finally {
    previewingVoice.value = false
  }
}

// 播放/停止试听音频
const togglePreviewAudio = () => {
  if (previewAudioRef.value && previewAudioUrl.value) {
    if (previewAudioRef.value.paused) previewAudioRef.value.play()
    else { previewAudioRef.value.pause(); previewAudioRef.value.currentTime = 0 }
  }
}

const saveChar = async () => {
  if (!charForm.value.name.trim()) { AMessage.warning('角色名不能为空'); return }
  try {
    const dramaId = route.params.id as string
    if (editingChar.value) {
      await characterApi.update(editingChar.value.id, { ...charForm.value, dramaId })
      store.updateCharacter(editingChar.value.id, charForm.value)
    } else {
      const res = await characterApi.create({ ...charForm.value, dramaId })
      if (res.code === 200) store.addCharacter(res.data)
    }
    charModalOpen.value = false
    AMessage.success(editingChar.value ? '角色已更新' : '角色已创建')
  } catch (e: any) {
    AMessage.error(e?.message || '操作失败')
  }
}

const deleteChar = async (id: string) => {
  try {
    await characterApi.delete(id)
    store.removeCharacter(id)
    AMessage.success('已删除')
  } catch (e: any) {
    AMessage.error(e?.message || '删除失败')
  }
}

// ====== 场景管理 ======
const sceneModalOpen = ref(false)
const editingScene = ref<any>(null)
const sceneForm = ref<any>({ name: '', description: '', imageUrl: '', location: '', timeOfDay: '', prompt: '' })

const openSceneModal = (item?: any) => {
  editingScene.value = item || null
  sceneForm.value = item ? { ...item } : { name: '', description: '', imageUrl: '', location: '', timeOfDay: '', prompt: '' }
  sceneModalOpen.value = true
}

const saveScene = async () => {
  if (!sceneForm.value.name.trim()) { AMessage.warning('场景名不能为空'); return }
  try {
    const dramaId = route.params.id as string
    if (editingScene.value) {
      await sceneApi.update(editingScene.value.id, { ...sceneForm.value, dramaId })
      store.updateScene(editingScene.value.id, sceneForm.value)
    } else {
      const res = await sceneApi.create({ ...sceneForm.value, dramaId })
      if (res.code === 200) store.addScene(res.data)
    }
    sceneModalOpen.value = false
    AMessage.success(editingScene.value ? '场景已更新' : '场景已创建')
  } catch (e: any) {
    AMessage.error(e?.message || '操作失败')
  }
}

const deleteScene = async (id: string) => {
  try {
    await sceneApi.delete(id)
    store.removeScene(id)
    AMessage.success('已删除')
  } catch (e: any) {
    AMessage.error(e?.message || '删除失败')
  }
}

// ====== 分镜管理 ======
import { aiApi } from '@/utils/ai'

const scriptText = ref('')
const generatingSb = ref(false)
const sbList = ref<any[]>([])

const generateStoryboards = async () => {
  if (!scriptText.value.trim()) { AMessage.warning('请输入剧本内容'); return }
  generatingSb.value = true
  try {
    const res = await aiApi.generateStoryboards(
      route.params.id as string,
      scriptText.value,
      1
    )
    if (res.code === 200 && res.data) {
      sbList.value = res.data
      AMessage.success(`成功拆解 ${res.data.length} 个分镜`)
    } else {
      AMessage.error(res.message || 'AI拆解失败')
    }
  } catch (e: any) {
    AMessage.error(e?.message || 'AI拆解失败，请检查AI配置')
  } finally {
    generatingSb.value = false
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

const openSbEditor = (shot?: any) => {
  editingSb.value = shot || null
  sbForm.value = shot ? { ...shot } : {}
  sbEditorOpen.value = true
}

const saveShot = async () => {
  if (!editingSb.value?.id) { sbEditorOpen.value = false; return }
  try {
    const res = await storyboardApi.update(editingSb.value.id, sbForm.value)
    if (res.code === 200) {
      const idx = sbList.value.findIndex(s => s.id === editingSb.value!.id)
      if (idx >= 0) sbList.value[idx] = res.data
      AMessage.success('分镜已更新')
    }
    sbEditorOpen.value = false
  } catch (e: any) {
    AMessage.error(e?.message || '保存失败')
  }
}

const deleteShot = async (id: string) => {
  try {
    await storyboardApi.delete(id)
    sbList.value = sbList.value.filter(s => s.id !== id)
    AMessage.success('已删除')
    // 重新加载列表确保数据同步
    await loadData()
  } catch (e: any) {
    AMessage.error(e?.message || '删除失败')
  }
}

const getShotTypeLabel = (type: string) => shotTypes.find(t => t.value === type)?.label || type

const statusOptions = [
  { v: 'draft', l: '草稿' },
  { v: 'in_progress', l: '制作中' },
  { v: 'completed', l: '已完成' },
]

// ====== 图片选择器（角色/场景弹窗共用）======
const imagePickerOpen = ref(false)
const imagePickerTarget = ref<'char' | 'scene' | 'cover'>('char')
const imageAssets = ref<any[]>([])
const loadingImages = ref(false)
const imagePickerPage = ref(1)
const hasMoreImages = ref(true)
const loadingMoreImages = ref(false)
const imagePickerScrollRef = ref<HTMLElement | null>(null)

// AI 生成状态
const generatingCharImg = ref<string | null>(null)
const generatingSceneImg = ref<string | null>(null)

const openImagePicker = (target: 'char' | 'scene' | 'cover') => {
  imagePickerTarget.value = target
  imagePickerOpen.value = true
  imagePickerPage.value = 1
  hasMoreImages.value = true
  imageAssets.value = []
  loadImageAssets()
}

const loadImageAssets = async (isLoadMore = false) => {
  if (isLoadMore) {
    loadingMoreImages.value = true
  } else {
    loadingImages.value = true
  }
  try {
    const res = await assetApi.page({ pageNum: imagePickerPage.value, pageSize: 20 })
    if (res.code === 200) {
      const list = res.data?.records || res.data || []
      const imageList = list.filter((a: any) => a.type === 'image')
      if (isLoadMore) {
        imageAssets.value.push(...imageList)
      } else {
        imageAssets.value = imageList
      }
      // 判断是否还有更多
      const total = res.data?.total || 0
      hasMoreImages.value = imageAssets.value.length < total && imageList.length === 20
    }
  } catch (e) { console.error('Load images failed:', e) }
  finally {
    loadingImages.value = false
    loadingMoreImages.value = false
  }
}

// 滚动加载更多
const onImagePickerScroll = (e: Event) => {
  const target = e.target as HTMLElement
  const scrollBottom = target.scrollHeight - target.scrollTop - target.clientHeight
  if (scrollBottom < 50 && hasMoreImages.value && !loadingMoreImages.value) {
    imagePickerPage.value++
    loadImageAssets(true)
  }
}

// 从存储中选择图片
const selectImage = (asset: any) => {
  if (imagePickerTarget.value === 'char') charForm.value.imageUrl = asset.fileUrl
  else if (imagePickerTarget.value === 'scene') sceneForm.value.imageUrl = asset.fileUrl
  else store.currentDrama.coverImage = asset.fileUrl
  imagePickerOpen.value = false
}

// 上传图片并自动填入 URL
const charFileInput = ref<HTMLInputElement | null>(null)
const sceneFileInput = ref<HTMLInputElement | null>(null)
const coverFileInput = ref<HTMLInputElement | null>(null)

const uploadAndSetImage = async (event: Event, target: 'char' | 'scene' | 'cover') => {
  const input = event.target as HTMLInputElement
  if (!input.files?.length) return
  const file = input.files[0]
  if (!file.type.startsWith('image/')) { AMessage.warning('请选择图片文件'); return }
  try {
    const res = await storageApi.upload(file, undefined, 'image')
    if (res.code === 200 && res.data?.fileUrl) {
      if (target === 'char') charForm.value.imageUrl = res.data.fileUrl
      else if (target === 'scene') sceneForm.value.imageUrl = res.data.fileUrl
      else store.currentDrama.coverImage = res.data.fileUrl
      AMessage.success('图片已上传')
    } else {
      throw new Error('上传返回异常')
    }
  } catch (e: any) {
    AMessage.error(e?.message || '上传失败')
  } finally {
    input.value = ''
  }
}

// AI 生成角色形象图
const aiGenerateCharImage = async () => {
  if (!charForm.value.name.trim()) { AMessage.warning('请先填写角色名称'); return }
  const prompt = charForm.value.appearancePrompt || `${charForm.value.name}, anime style character portrait, detailed`
  generatingCharImg.value = 'modal'
  try {
    const dramaId = route.params.id as string
    const res = await aiApi.generateCharacterImage({
      dramaId,
      characterId: editingChar.value?.id || '',
      prompt,
    })
    if (res.code === 200 && res.data) {
      charForm.value.imageUrl = res.data.fileUrl || res.data.url || res.data
      AMessage.success('AI 角色图生成成功')
    } else {
      AMessage.error(res.message || 'AI 生成失败，请检查 AI 配置')
    }
  } catch (e: any) {
    AMessage.error(e?.message || 'AI 生成失败')
  } finally {
    generatingCharImg.value = null
  }
}

// AI 生成场景图
const aiGenerateSceneImage = async () => {
  if (!sceneForm.value.name.trim()) { AMessage.warning('请先填写场景名称'); return }
  const prompt = sceneForm.value.prompt || `${sceneForm.value.name}, anime style background scene, detailed cinematic`
  generatingSceneImg.value = 'modal'
  try {
    const dramaId = route.params.id as string
    const res = await aiApi.generateSceneImage({
      dramaId,
      sceneId: editingScene.value?.id || '',
      prompt,
    })
    if (res.code === 200 && res.data) {
      sceneForm.value.imageUrl = res.data.fileUrl || res.data.url || res.data
      AMessage.success('AI 场景图生成成功')
    } else {
      AMessage.error(res.message || 'AI 生成失败，请检查 AI 配置')
    }
  } catch (e: any) {
    AMessage.error(e?.message || 'AI 生成失败')
  } finally {
    generatingSceneImg.value = null
  }
}

// 清除已选图片
const clearImageUrl = (target: 'char' | 'scene') => {
  if (target === 'char') charForm.value.imageUrl = ''
  else sceneForm.value.imageUrl = ''
}
</script>

<template>
  <div class="w-full min-h-[calc(100vh-5rem)]">
    <!-- 页面标题栏 -->
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-6">
      <div class="flex items-center gap-3 min-w-0">
        <a-button type="text" @click="router.back()" class="!p-2 !text-[#a0a0a0] hover:!bg-[#242424] rounded-xl shrink-0">
          <ArrowLeftOutlined />
        </a-button>
        <h1 class="text-lg sm:text-xl font-semibold text-[#f5f5f5] truncate">
          {{ isNew ? '新建剧集' : (store.currentDrama?.title || '剧集详情') }}
        </h1>
      </div>
      <a-button
        v-if="!isNew"
        type="primary"
        @click="saveDrama"
        :disabled="saving || !store.currentDrama?.title"
        :loading="saving"
        class="shrink-0 w-full sm:w-auto"
      >
        <template #icon><SaveOutlined /></template>
        保存
      </a-button>
    </div>

    <!-- ========== 加载错误（非新建时） ========== -->
    <div v-if="!isNew && loadError" class="flex flex-col items-center justify-center py-20">
      <a-result status="warning" :title="loadError" class="!text-[#808080]">
        <template #extra>
          <a-button type="primary" @click="loadData">重试</a-button>
          <a-button @click="router.push('/dramas')">返回列表</a-button>
        </template>
      </a-result>
    </div>

    <!-- ========== 剧集不存在（加载完成但数据为空） ========== -->
    <div v-if="!isNew && !loading && !loadError && !store.currentDrama" class="flex flex-col items-center justify-center py-20">
      <a-result status="404" title="剧集不存在或已被删除">
        <template #extra>
          <a-button type="primary" @click="router.push('/dramas')">返回剧集列表</a-button>
          <a-button @click="router.push('/drama/new')">新建剧集</a-button>
        </template>
      </a-result>
    </div>

    <!-- ========== 新建剧集表单 ========== -->
    <div v-if="isNew && store.currentDrama" class="max-w-xl mx-auto">
      <div class="bg-[#1a1a1a] rounded-2xl border border-[#2a2a2a] p-6 sm:p-8 space-y-6">
        <!-- 封面上传（新建时可选） -->
        <div class="relative w-full aspect-video rounded-xl bg-[#242424] border border-dashed border-[#333] overflow-hidden cursor-pointer hover:border-[#6366f1]/40 transition-colors"
             @click="coverFileInput?.click()">
          <img
            v-if="store.currentDrama.coverImage"
            :src="store.currentDrama.coverImage"
            class="w-full h-full object-cover"
          />
          <div v-else class="w-full h-full flex flex-col items-center justify-center text-[#404040]">
            <UploadOutlined style="font-size: 28px;" class="mb-2" />
            <span class="text-xs">点击上传封面图（可选）</span>
          </div>
          <input ref="coverFileInput" type="file" accept="image/*" class="hidden" hidden
                 @change="(e) => uploadAndSetImage(e, 'cover')" />
        </div>

        <div>
          <label class="block text-sm text-[#a0a0a0] mb-2">剧集标题 <span class="text-red-400">*</span></label>
          <a-input v-model:value="store.currentDrama.title" placeholder="输入剧集标题" size="large" />
        </div>
        <div>
          <label class="block text-sm text-[#a0a0a0] mb-2">剧集描述</label>
          <a-textarea v-model:value="store.currentDrama.description" placeholder="输入剧集描述" :rows="4" />
        </div>
        <div>
          <label class="block text-sm text-[#a0a0a0] mb-2">总集数</label>
          <a-input-number v-model:value="store.currentDrama.totalEpisodes" :min="1" size="large" class="w-full" />
        </div>
        <a-button type="primary" size="large" block :loading="saving" :disabled="!store.currentDrama?.title" @click="saveDrama">
          <template #icon><SendOutlined /></template>
          {{ saving ? '创建中...' : '创建剧集' }}
        </a-button>
      </div>
    </div>

    <!-- ========== 剧集详情 Tabs ========== -->
    <div v-else-if="!isNew && !loadError && store.currentDrama" class="w-full">
      <!-- Tab Bar -->
      <div class="flex gap-2 mb-6 overflow-x-auto pb-2 no-scrollbar">
        <a-button
          v-for="tab in tabs"
          :key="tab.key"
          @click="activeTab = tab.key"
          :type="activeTab === tab.key ? 'primary' : 'default'"
          size="small"
          :class="[
            '!rounded-xl !font-medium whitespace-nowrap shrink-0',
            activeTab !== tab.key && '!bg-[#1a1a1a] !border-[#2a2a2a] !text-[#a0a0a0] hover:!text-[#f5f5f5] hover:!bg-[#242424]'
          ]"
        >
          <template #icon><component :is="tab.icon" /></template>
          {{ tab.label }}
          <span v-if="tab.key === 'characters' && store.characters.length" class="ml-1 px-1.5 py-0.5 bg-white/20 rounded-full text-[10px]">{{ store.characters.length }}</span>
          <span v-if="tab.key === 'scenes' && store.scenes.length" class="ml-1 px-1.5 py-0.5 bg-white/20 rounded-full text-[10px]">{{ store.scenes.length }}</span>
          <span v-if="tab.key === 'storyboards' && sbList.length" class="ml-1 px-1.5 py-0.5 bg-white/20 rounded-full text-[10px]">{{ sbList.length }}</span>
        </a-button>
      </div>

      <!-- ========== Characters Tab ========== -->
      <div v-show="activeTab === 'characters'" class="space-y-6 w-full">
        <div class="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-3">
          <h3 class="text-base sm:text-lg font-medium text-[#f5f5f5]">角色管理</h3>
          <a-button type="primary" @click="openCharModal()">
            <template #icon><PlusOutlined /></template>
            添加角色
          </a-button>
        </div>

        <div v-if="store.characters.length === 0" class="text-center py-16 text-[#606060]">
          <a-empty description="暂无角色，点击上方按钮添加" />
        </div>

        <div v-else class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-3 sm:gap-4">
          <div v-for="char in store.characters" :key="char.id"
            class="group bg-[#1a1a1a] rounded-xl border border-[#2a2a2a] p-3 sm:p-4 hover:border-[#6366f1]/50 transition-all cursor-pointer relative">
            <a-popconfirm title="确定删除该角色？" ok-text="确定" cancel-text="取消" @confirm="deleteChar(char.id)">
              <button @click.stop class="absolute top-1.5 right-1.5 p-1 rounded-lg opacity-0 group-hover:opacity-100 hover:bg-red-500/20 text-red-400 transition-all z-10">
                <DeleteOutlined style="font-size: 12px;" />
              </button>
            </a-popconfirm>
            <div @click="openCharModal(char)" class="flex flex-col items-center">
              <div class="aspect-square w-full max-w-[100px] sm:max-w-[120px] bg-[#242424] rounded-lg mb-2 sm:mb-3 overflow-hidden group-hover:ring-2 ring-[#6366f1]/50 transition-all">
                <img v-if="char.imageUrl" :src="char.imageUrl" class="w-full h-full object-cover" />
                <div v-else class="w-full h-full flex items-center justify-center text-[#404040] text-xl sm:text-2xl">{{ char.name?.[0] || '?' }}</div>
              </div>
              <p class="text-xs sm:text-sm font-medium text-[#f5f5f5] text-center truncate w-full px-1">{{ char.name }}</p>
              <p v-if="char.voiceProvider" class="text-[10px] xs:text-xs text-[#606060] mt-0.5">{{ char.voiceProvider }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- ========== Scenes Tab ========== -->
      <div v-show="activeTab === 'scenes'" class="space-y-6 w-full">
        <div class="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-3">
          <h3 class="text-base sm:text-lg font-medium text-[#f5f5f5]">场景管理</h3>
          <a-button type="primary" @click="openSceneModal()">
            <template #icon><PlusOutlined /></template>
            添加场景
          </a-button>
        </div>

        <div v-if="store.scenes.length === 0" class="text-center py-16 text-[#606060]">
          <a-empty description="暂无场景，点击上方按钮添加" />
        </div>

        <div v-else class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3 sm:gap-4">
          <div v-for="scene in store.scenes" :key="scene.id"
            class="group bg-[#1a1a1a] rounded-xl border border-[#2a2a2a] overflow-hidden hover:border-[#6366f1]/50 transition-all cursor-pointer">
            <!-- 场景图片区域（点击打开编辑弹窗） -->
            <div class="relative aspect-video bg-[#242424]" @click="openSceneModal(scene)">
              <img v-if="scene.imageUrl" :src="scene.imageUrl" class="w-full h-full object-cover" />
              <div v-else class="w-full h-full flex items-center justify-center text-[#404040]">
                <PictureOutlined style="font-size: 32px;" />
              </div>
              <!-- 删除按钮：独立区域，阻止事件冒泡到 openSceneModal -->
              <div class="absolute top-2 right-2 z-10">
                <a-popconfirm title="确定删除该场景？" ok-text="确定" cancel-text="取消"
                             @confirm="deleteScene(scene.id)">
                  <button @click.stop class="p-1.5 rounded-lg opacity-0 group-hover:opacity-100 hover:bg-red-500/20 text-red-400 bg-black/30 backdrop-blur-sm transition-all">
                    <DeleteOutlined style="font-size: 14px;" />
                  </button>
                </a-popconfirm>
              </div>
            </div>
            <div class="p-2.5 sm:p-3" @click="openSceneModal(scene)">
              <p class="text-xs sm:text-sm font-medium text-[#f5f5f5] truncate">{{ scene.name }}</p>
              <div class="flex items-center justify-between mt-1 gap-2">
                <p class="text-[10px] xs:text-xs text-[#606060] truncate flex-1 min-w-0">{{ scene.location || '无地点信息' }}</p>
                <a-tag v-if="scene.timeOfDay" size="small" color="#242424" class="!text-[#808080] shrink-0">{{ scene.timeOfDay }}</a-tag>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- ========== Storyboards Tab ========== -->
      <div v-show="activeTab === 'storyboards'" class="space-y-6 w-full">
        <!-- AI生成区 -->
        <div class="bg-[#1a1a1a] rounded-xl p-4 sm:p-5 border border-[#2a2a2a]">
          <h4 class="text-sm font-medium text-[#f5f5f5] mb-3 flex items-center gap-2">
            <span class="w-1.5 h-1.5 rounded-full bg-green-400 animate-pulse"></span>
            AI 自动拆解剧本
          </h4>
          <a-textarea v-model:value="scriptText" placeholder="粘贴剧本内容，AI将自动拆解为分镜..." :rows="4" class="mb-3" />
          <div class="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
            <p class="text-xs text-[#606060]">支持中英文剧本，自动识别角色、场景、镜头类型</p>
            <a-button type="primary" :loading="generatingSb" :disabled="!scriptText.trim()" @click="generateStoryboards">
              <template #icon><RobotOutlined /></template>
              {{ generatingSb ? 'AI拆解中...' : 'AI拆解分镜' }}
            </a-button>
          </div>
        </div>

        <!-- 分镜列表 -->
        <div class="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-3">
          <h3 class="text-base sm:text-lg font-medium text-[#f5f5f5]">
            分镜列表
            <span v-if="sbList.length" class="text-sm font-normal text-[#808080] ml-2">({{ sbList.length }}个镜头)</span>
          </h3>
          <div v-if="sbList.length > 0" class="flex gap-2">
            <a-button @click="router.push(`/workbench/${route.params.id}`)">进入工作台 →</a-button>
            <a-button class="!bg-[#1a1a1e] !border-[#2a2a3e] !text-[#a78bfa]" @click="router.push(`/media/${route.params.id}`)">
              <template #icon><VideoCameraOutlined /></template>
              媒体工作室
            </a-button>
          </div>
        </div>

        <div v-if="sbList.length === 0" class="text-center py-16 text-[#606060]">
          <a-empty description="暂无分镜，输入剧本后点击 AI 拆解" />
        </div>

        <div v-else class="space-y-3">
          <div v-for="(shot, idx) in sbList" :key="shot.id"
            class="group flex gap-3 sm:gap-4 p-3 sm:p-4 bg-[#1a1a1a] rounded-xl border border-[#2a2a2a] hover:border-[#6366f1]/40 transition-all cursor-pointer"
            @click="openSbEditor(shot)">
            <!-- 序号 & 类型 -->
            <div class="flex-shrink-0 w-9 h-9 sm:w-10 sm:h-10 rounded-lg bg-[#6366f1]/10 flex items-center justify-center">
              <span class="text-xs sm:text-sm font-bold text-[#6366f1]">{{ shot.shotNumber || idx + 1 }}</span>
            </div>
            <!-- 内容 -->
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-1.5 sm:gap-2 mb-1 flex-wrap">
                <a-tag color="#6366f115" class="!text-[#a78bfa] font-medium">{{ getShotTypeLabel(shot.shotType) }}</a-tag>
                <span v-if="shot.shotDirection" class="text-[10px] xs:text-xs text-[#606060]">{{ shot.shotDirection }}</span>
                <span v-if="shot.characterName" class="px-1 sm:px-1.5 py-0.5 bg-yellow-500/10 text-yellow-400/70 rounded text-[9px] sm:text-[10px]">{{ shot.characterName }}</span>
              </div>
              <p class="text-xs sm:text-sm text-[#e0e0e0] line-clamp-2">{{ shot.action || '无动作描述' }}</p>
              <p v-if="shot.dialogue" class="text-[10px] xs:text-xs text-[#888] mt-1 italic line-clamp-1">「{{ shot.dialogue }}」</p>
            </div>
            <!-- 操作 -->
            <div class="flex-shrink-0 opacity-0 group-hover:opacity-100 flex items-start gap-1 transition-opacity" @click.stop>
              <a-popconfirm title="确定删除该分镜？" ok-text="确定" cancel-text="取消" @confirm="deleteShot(shot.id)">
                <a-button type="text" danger size="small"><template #icon><DeleteOutlined /></template></a-button>
              </a-popconfirm>
            </div>
          </div>
        </div>
      </div>

      <!-- ========== Settings Tab ========== -->
      <div v-show="activeTab === 'settings'" class="max-w-full sm:max-w-2xl mx-auto space-y-6">
        <div class="bg-[#1a1a1a] rounded-2xl border border-[#2a2a2a] p-5 sm:p-6 space-y-5">
          <h3 class="text-base sm:text-lg font-medium text-[#f5f5f5]">基本信息</h3>

          <!-- 封面图上传 -->
          <div>
            <label class="block text-sm text-[#a0a0a0] mb-2">剧集封面</label>
            <div class="relative w-full aspect-video max-w-sm rounded-xl bg-[#242424] border border-[#2a2a2a] overflow-hidden group">
              <img
                v-if="store.currentDrama?.coverImage"
                :src="store.currentDrama.coverImage"
                class="w-full h-full object-cover group-hover:scale-[1.02] transition-transform duration-300"
              />
              <div v-else class="w-full h-full flex flex-col items-center justify-center text-[#404040] cursor-pointer hover:bg-[#2a2a2a] transition-colors"
                   @click="coverFileInput?.click()">
                <PictureOutlined style="font-size: 32px;" class="mb-2" />
                <span class="text-xs">点击上传封面图</span>
              </div>
            </div>
            <div class="flex gap-2 mt-2">
              <a-button size="small" @click="openImagePicker('cover')" class="!flex-1">
                <template #icon><FolderOpenOutlined /></template>从存储选择
              </a-button>
              <a-button size="small" @click="coverFileInput?.click()" class="!flex-1">
                <template #icon><UploadOutlined /></template>{{ store.currentDrama?.coverImage ? '更换' : '上传图片' }}
              </a-button>
              <a-popconfirm title="确定移除封面？" ok-text="确定" cancel-text="取消"
                            @confirm="store.currentDrama.coverImage = ''"
                            v-if="store.currentDrama?.coverImage">
                <a-button size="small" danger class="!px-3">移除</a-button>
              </a-popconfirm>
              <input ref="coverFileInput" type="file" accept="image/*" class="hidden" hidden
                     @change="(e) => uploadAndSetImage(e, 'cover')" />
            </div>
          </div>

          <div>
            <label class="block text-sm text-[#a0a0a0] mb-2">剧集标题</label>
            <a-input v-model:value="store.currentDrama.title" size="large" />
          </div>
          <div>
            <label class="block text-sm text-[#a0a0a0] mb-2">描述</label>
            <a-textarea v-model:value="store.currentDrama.description" :rows="4" />
          </div>
          <div>
            <label class="block text-sm text-[#a0a0a0] mb-2">总集数</label>
            <a-input-number v-model:value="store.currentDrama.totalEpisodes" :min="1" size="large" class="w-full" />
          </div>
          <div>
            <label class="block text-sm text-[#a0a0a0] mb-2">状态</label>
            <a-radio-group v-model:value="store.currentDrama.status" button-style="solid">
              <a-radio-button v-for="s in statusOptions" :key="s.v" :value="s.v">{{ s.l }}</a-radio-button>
            </a-radio-group>
          </div>
          <a-button type="primary" size="large" block :loading="saving" @click="saveDrama">
            <template #icon><SaveOutlined /></template>
            {{ saving ? '保存中...' : '保存修改' }}
          </a-button>
        </div>
      </div>
    </div>

    <!-- Loading 状态（非新建时） -->
    <div v-if="!isNew && loading" class="flex items-center justify-center py-20">
      <a-spin size="large" tip="加载中..." />
    </div>

    <!-- ====== 角色弹窗 ====== -->
    <a-modal v-model:open="charModalOpen" :title="editingChar ? '编辑角色' : '添加角色'"
      @ok="saveChar" :okButtonProps="{ disabled: !charForm.name?.trim() }" okText="创建" cancelText="取消" width="500px" destroyOnClose>
      <div class="space-y-4 pt-2">
        <a-form layout="vertical">
          <a-form-item label="名称" required><a-input v-model:value="charForm.name" placeholder="角色名称" /></a-form-item>
          <a-form-item label="描述"><a-textarea v-model:value="charForm.description" placeholder="角色背景描述..." :rows="2" /></a-form-item>
          <div class="grid grid-cols-2 gap-3">
            <a-form-item label="外观提示词"><a-input v-model:value="charForm.appearancePrompt" placeholder="用于AI生成角色图" /></a-form-item>
            <a-form-item label="台词风格"><a-input v-model:value="charForm.dialogueStyle" placeholder="如：温柔/霸道/幽默" /></a-form-item>
          </div>

          <!-- ====== 音色选择 + 试听（BUG00023 关联AI配置） ====== -->
          <div class="bg-[#12121a] rounded-lg p-3 sm:p-4 border border-[#2a2238] space-y-3">
            <div class="flex items-center justify-between mb-1">
              <label class="text-xs text-[#a0a0a0] font-medium flex items-center gap-1.5">
                <SoundOutlined style="color: #a78bfa;" />
                专属音色
              </label>
              <a-button
                type="primary"
                size="small"
                :loading="previewingVoice"
                :disabled="!charForm.voiceId"
                @click="previewVoice"
                class="!rounded-lg !px-3 !text-xs"
              >
                {{ previewingVoice ? '生成中...' : '试听' }}
              </a-button>
            </div>

            <!-- 音色下拉选择 -->
            <a-select
              v-model:value="charForm.voiceId"
              placeholder="选择该角色的专属TTS音色"
              size="large"
              allowClear
              showSearch
              class="w-full"
            >
              <a-select-option
                v-for="v in ttsVoices"
                :key="v.value"
                :value="v.value"
              >{{ v.label }}<span class="text-[#555] ml-1">({{ v.value }})</span></a-select-option>
            </a-select>

            <!-- 试听播放器 -->
            <div v-if="previewAudioUrl" class="flex items-center gap-2 bg-black/30 rounded-lg p-2">
              <audio ref="previewAudioRef" :src="previewAudioUrl" class="hidden" />
              <a-button type="text" size="small" @click="togglePreviewAudio" class="shrink-0 !p-1">
                <template #icon><PlayCircleOutlined style="color: #34d399;" /></template>播放
              </a-button>
              <span class="text-[10px] text-[#666] truncate">试听音频已就绪，点击播放</span>
            </div>
            <p v-if="ttsConfigList.length > 0" class="text-[9px] text-[#444]">
              💡 当前TTS模型: {{ ttsConfigList.map((c: any) => c.model).filter(Boolean).join(' / ') || 'speech-02-hd' }}
            </p>
          </div>

          <!-- ====== 角色形象图（从存储选 + 上传） ====== -->
          <a-form-item label="角色形象图">
            <!-- 已选预览 -->
            <div v-if="charForm.imageUrl" class="relative rounded-lg overflow-hidden bg-[#242424] border border-[#333]">
              <img :src="charForm.imageUrl" class="w-full h-32 object-cover" />
              <button type="button" @click="clearImageUrl('char')"
                class="absolute top-1.5 right-1.5 p-1 rounded-lg bg-black/50 text-red-400 hover:bg-red-500/80 transition-all">
                <CloseCircleOutlined style="font-size: 16px;" />
              </button>
            </div>
            <!-- 操作按钮 -->
            <div class="flex gap-2 mt-2">
              <a-button size="small" @click="openImagePicker('char')" class="!flex-1">
                <template #icon><FolderOpenOutlined /></template>从存储选择
              </a-button>
              <a-button size="small" @click="charFileInput?.click()" class="!flex-1">
                <template #icon><UploadOutlined /></template>上传图片
              </a-button>
              <a-button size="small" :loading="generatingCharImg === 'modal'" @click="aiGenerateCharImage"
                        class="!flex-1 !border-[#6366f1]/40 !text-[#a78bfa] hover:!bg-[#6366f1]/10">
                <template #icon><RobotOutlined /></template>{{ generatingCharImg === 'modal' ? '生成中...' : 'AI生成' }}
              </a-button>
              <input ref="charFileInput" type="file" accept="image/*" class="hidden" hidden
                     @change="(e) => uploadAndSetImage(e, 'char')" />
            </div>
          </a-form-item>
        </a-form>
      </div>
    </a-modal>

    <!-- ====== 场景弹窗 ====== -->
    <a-modal v-model:open="sceneModalOpen" :title="editingScene ? '编辑场景' : '添加场景'"
      @ok="saveScene" :okButtonProps="{ disabled: !sceneForm.name?.trim() }" okText="创建" cancelText="取消" width="500px" destroyOnClose>
      <div class="space-y-4 pt-2">
        <a-form layout="vertical">
          <a-form-item label="场景名称" required><a-input v-model:value="sceneForm.name" placeholder="如：咖啡厅 / 教室 / 天台" /></a-form-item>
          <div class="grid grid-cols-2 gap-3">
            <a-form-item label="地点"><a-input v-model:value="sceneForm.location" placeholder="如：市中心 / 郊外" /></a-form-item>
            <a-form-item label="时间">
              <a-select v-model:value="sceneForm.timeOfDay" allowClear placeholder="不限">
                <a-select-option value="白天">白天</a-select-option>
                <a-select-option value="夜晚">夜晚</a-select-option>
                <a-select-option value="清晨">清晨</a-select-option>
                <a-select-option value="黄昏">黄昏</a-select-option>
                <a-select-option value="黎明">黎明</a-select-option>
                <a-select-option value="午夜">午夜</a-select-option>
              </a-select>
            </a-form-item>
          </div>
          <a-form-item label="描述"><a-textarea v-model:value="sceneForm.description" placeholder="环境描述..." :rows="2" /></a-form-item>
          <a-form-item label="AI生成提示词"><a-input v-model:value="sceneForm.prompt" placeholder="用于AI生成场景图" /></a-form-item>

          <!-- ====== 场景图（从存储选 + 上传） ====== -->
          <a-form-item label="场景图片">
            <!-- 已选预览 -->
            <div v-if="sceneForm.imageUrl" class="relative rounded-lg overflow-hidden bg-[#242424] border border-[#333]">
              <img :src="sceneForm.imageUrl" class="w-full h-32 object-cover" />
              <button type="button" @click="clearImageUrl('scene')"
                class="absolute top-1.5 right-1.5 p-1 rounded-lg bg-black/50 text-red-400 hover:bg-red-500/80 transition-all">
                <CloseCircleOutlined style="font-size: 16px;" />
              </button>
            </div>
            <!-- 操作按钮 -->
            <div class="flex gap-2 mt-2">
              <a-button size="small" @click="openImagePicker('scene')" class="!flex-1">
                <template #icon><FolderOpenOutlined /></template>从存储选择
              </a-button>
              <a-button size="small" @click="sceneFileInput?.click()" class="!flex-1">
                <template #icon><UploadOutlined /></template>上传图片
              </a-button>
              <a-button size="small" :loading="generatingSceneImg === 'modal'" @click="aiGenerateSceneImage"
                        class="!flex-1 !border-[#6366f1]/40 !text-[#a78bfa] hover:!bg-[#6366f1]/10">
                <template #icon><RobotOutlined /></template>{{ generatingSceneImg === 'modal' ? '生成中...' : 'AI生成' }}
              </a-button>
              <input ref="sceneFileInput" type="file" accept="image/*" class="hidden" hidden
                     @change="(e) => uploadAndSetImage(e, 'scene')" />
            </div>
          </a-form-item>
        </a-form>
      </div>
    </a-modal>

    <!-- ====== 分镜编辑弹窗 ====== -->
    <a-modal v-model:open="sbEditorOpen" title="编辑分镜" @ok="saveShot" okText="保存" cancelText="取消" width="540px" destroyOnClose>
      <div class="space-y-4 pt-2">
        <a-form layout="vertical">
          <div class="grid grid-cols-2 gap-3">
            <a-form-item label="镜头类型">
              <a-select v-model:value="sbForm.shotType">
                <a-select-option v-for="t in shotTypes" :key="t.value" :value="t.value">{{ t.label }}</a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item label="运镜方式"><a-input v-model:value="sbForm.shotDirection" placeholder="如：推镜头/拉镜头/固定" /></a-form-item>
          </div>
          <a-form-item label="动作描述"><a-textarea v-model:value="sbForm.action" placeholder="描述这个镜头中的动作和画面..." :rows="3" /></a-form-item>
          <a-form-item label="台词"><a-textarea v-model:value="sbForm.dialogue" placeholder="角色的台词内容..." :rows="2" /></a-form-item>
        </a-form>
      </div>
    </a-modal>

    <!-- ====== 从存储选择图片弹窗 ====== -->
    <a-modal v-model:open="imagePickerOpen"
      title="选择图片（素材库）" okText="" cancelText="关闭" width="600px" destroyOnClose
      :body-style="{ padding: '16px', overflow: 'hidden' }">
      <div ref="imagePickerScrollRef"
        style="max-height: calc(60vh - 20px); overflow-y: auto; padding: 4px;"
        @scroll="onImagePickerScroll">
        <!-- Loading -->
        <div v-if="loadingImages" class="flex justify-center py-8"><a-spin tip="加载中..." /></div>

        <!-- Empty -->
        <div v-else-if="imageAssets.length === 0" class="text-center py-8">
          <p class="text-sm text-[#808080] mb-3">素材库暂无图片</p>
          <a-button size="small" @click="imagePickerOpen = false; charFileInput?.click()">去上传</a-button>
        </div>

        <!-- 图片网格 -->
        <div v-else class="grid grid-cols-3 sm:grid-cols-4 gap-2">
          <div v-for="img in imageAssets" :key="img.id"
            class="group aspect-square rounded-lg bg-[#242424] border border-[#333] overflow-hidden cursor-pointer hover:border-[#6366f1]/50 transition-all relative"
            @click="selectImage(img)">
            <img :src="img.fileUrl" class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-200" loading="lazy" />
            <!-- 选中指示器 -->
            <div class="absolute inset-0 bg-[#6366f1]/0 group-hover:bg-[#6366f1]/10 transition-all flex items-end p-1.5">
              <span class="text-[9px] text-white/70 truncate drop-shadow">{{ img.filename }}</span>
            </div>
            <!-- 已选中标记 -->
            <div v-if="(imagePickerTarget==='char' && charForm.imageUrl===img.fileUrl) || (imagePickerTarget==='scene' && sceneForm.imageUrl===img.fileUrl) || (imagePickerTarget==='cover' && store.currentDrama?.coverImage===img.fileUrl)"
              class="absolute top-1 right-1 w-5 h-5 rounded-full bg-[#6366f1] flex items-center justify-center">
              <span style="font-size: 11px;" class="text-white">✓</span>
            </div>
          </div>
        </div>

        <!-- 加载更多 -->
        <div v-if="loadingMoreImages" class="flex justify-center py-4">
          <a-spin size="small" tip="加载更多..." />
        </div>
        <div v-else-if="!hasMoreImages && imageAssets.length > 0" class="text-center py-4 text-[#666] text-xs">
          已加载全部 {{ imageAssets.length }} 张图片
        </div>
      </div>
    </a-modal>
  </div>
</template>

<style scoped>
.no-scrollbar { -ms-overflow-style: none; scrollbar-width: none; }
.no-scrollbar::-webkit-scrollbar { display: none; }
.line-clamp-1 { overflow: hidden; display: -webkit-box; -webkit-box-orient: vertical; -webkit-line-clamp: 1; }
.line-clamp-2 { overflow: hidden; display: -webkit-box; -webkit-box-orient: vertical; -webkit-line-clamp: 2; }

/* 暗色主题下覆盖 Ant Design 表单标签颜色 */
::deep(.ant-form-item-label > label) { color: #a0a0a0 !important; }
</style>
