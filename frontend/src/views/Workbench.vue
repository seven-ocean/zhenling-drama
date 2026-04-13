<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { storyboardApi, characterApi, sceneApi } from '@/utils/request'
import { aiApi } from '@/utils/ai'
import { message as AMessage } from 'ant-design-vue'
import {
  ArrowLeftOutlined,
  DeleteOutlined,
  UserOutlined,
  PictureOutlined,
  RobotOutlined,
  SendOutlined,
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

const openSbEditor = (shot?: any) => {
  editingSb.value = shot || null
  sbForm.value = shot ? { ...shot } : {}
  sbEditorOpen.value = true
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
  } catch (e: any) { AMessage.error(e?.message || '删除失败') }
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

onMounted(() => {
  loadStoryboards()
  loadCharacters()
  loadScenes()
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

    <!-- Tab Bar -->
    <div class="flex gap-2 overflow-x-auto pb-1 no-scrollbar">
      <a-button
        v-for="tab in ['storyboard', 'character', 'scene']"
        :key="tab"
        :type="activeTab === tab ? 'primary' : 'default'"
        size="small"
        @click="activeTab = tab"
        :class="[
          '!rounded-xl !font-medium whitespace-nowrap shrink-0',
          activeTab !== tab && '!bg-[#1a1a1a] !border-[#2a2a2a] !text-[#a0a0a0] hover:!text-[#f5f5f5] hover:!bg-[#242424]'
        ]"
      >
        {{ tab === 'storyboard' ? '🎬 分镜编辑器' : tab === 'character' ? '👤 角色图' : '🏞️ 场景图' }}
        <span v-if="tab === 'storyboard' && storyboards.length" class="ml-1 px-1.5 py-0.5 bg-white/20 rounded-full text-[10px]">{{ storyboards.length }}</span>
      </a-button>
    </div>

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
          @click="openSbEditor(shot)"
          class="group p-3 sm:p-4 bg-[#1a1a1a] rounded-xl border border-[#2a2a2a] hover:border-[#6366f1]/40 cursor-pointer transition-all">
          <div class="flex items-center justify-between mb-2">
            <span class="flex items-center gap-1.5 sm:gap-2 min-w-0">
              <span class="w-6 h-6 sm:w-7 sm:h-7 flex items-center justify-center rounded-lg bg-[#6366f1]/10 text-[10px] sm:text-sm font-bold text-[#6366f1] shrink-0">{{ shot.shotNumber || idx + 1 }}</span>
              <span class="px-1.5 sm:px-2 py-0.5 bg-[#6366f1]/15 text-[#a78bfa] rounded text-[10px] sm:text-xs shrink-0">{{ shotTypes.find(t=>t.value===shot.shotType)?.label }}</span>
            </span>
            <a-popconfirm title="确定删除该分镜？" ok-text="确定" cancel-text="取消" @confirm="deleteShot(shot.id)">
              <a-button type="text" danger size="small" class="opacity-0 group-hover:opacity-100 !p-1" @click.stop>
                <template #icon><DeleteOutlined /></template>
              </a-button>
            </a-popconfirm>
          </div>
          <p class="text-xs sm:text-sm text-[#d0d0d0] mb-1 line-clamp-2">{{ shot.action || '无动作描述' }}</p>
          <p v-if="shot.dialogue" class="text-[10px] xs:text-xs text-[#888] italic line-clamp-1">「{{ shot.dialogue }}」</p>
          <div class="mt-2 flex flex-wrap gap-1">
            <a-tag v-if="shot.characterName" color="#fffaf015" class="!text-yellow-400/70 !rounded-[10px] !text-[9px] sm:!text-[10px]">{{ shot.characterName }}</a-tag>
            <a-tag v-if="shot.shotDirection" color="#eff6ff15" class="!text-blue-400/70 !rounded-[10px] !text-[9px] sm:!text-[10px]">{{ shot.shotDirection }}</a-tag>
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

    <!-- ====== 分镜编辑弹窗（Ant Design Modal） ====== -->
    <a-modal
      v-model:open="sbEditorOpen"
      title="编辑分镜"
      @ok="saveShot"
      okText="保存"
      cancelText="取消"
      width="540px"
      destroyOnClose
    >
      <div class="space-y-4 pt-2">
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
