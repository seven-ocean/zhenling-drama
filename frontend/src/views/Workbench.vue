<script setup lang="ts">
import { ref, computed } from 'vue'
import { storyboardApi } from '@/utils/request'
import { aiApi } from '@/utils/ai'

const props = defineProps<{ dramaId: string }>()

const activeTab = ref('storyboard')
const storyboards = ref<any[]>([])
const script = ref('')
const generating = ref(false)

// 分镜编辑相关
const editingShot = ref<any>(null)
const showEditor = ref(false)

const shotTypes = [
  { value: 'wide', label: '全景' },
  { value: 'medium', label: '中景' },
  { value: 'close-up', label: '近景' },
  { value: 'extreme-close-up', label: '特写' },
]

const generateFromScript = async () => {
  if (!script.value.trim()) return
  generating.value = true
  try {
    const res = await aiApi.generateStoryboards(props.dramaId, script.value, 1)
    if (res.code === 200) {
      storyboards.value = res.data || []
    }
  } catch (e) {
    console.error(e)
  } finally {
    generating.value = false
  }
}

const openEditor = (shot: any) => {
  editingShot.value = { ...shot }
  showEditor.value = true
}

const saveShot = async () => {
  // 保存分镜
  showEditor.value = false
}

const getShotTypeLabel = (type: string) => {
  return shotTypes.find(t => t.value === type)?.label || type
}
</script>

<template>
  <div class="space-y-6">
    <!-- Tabs -->
    <div class="flex gap-2">
      <button 
        v-for="tab in ['storyboard', 'character', 'scene']" 
        :key="tab"
        @click="activeTab = tab"
        :class="['px-4 py-2 rounded-xl text-sm', activeTab === tab ? 'bg-[#6366f1] text-white' : 'bg-[#1a1a1a] text-[#a0a0a0]']"
      >
        {{ tab === 'storyboard' ? '分镜' : tab === 'character' ? '角色图' : '场景图' }}
      </button>
    </div>

    <!-- 分镜Tab -->
    <div v-if="activeTab === 'storyboard'" class="space-y-4">
      <!-- 剧本输入 -->
      <div class="bg-[#1a1a1a] rounded-xl p-4 border border-[#2a2a2a]">
        <h4 class="text-sm font-medium text-[#f5f5f5] mb-3">输入剧本自动拆解分镜</h4>
        <textarea 
          v-model="script" 
          rows="6"
          placeholder="粘贴剧本内容..."
          class="w-full px-4 py-3 bg-[#242424] border border-[#2a2a2a] rounded-xl text-[#f5f5f5] placeholder-[#606060] resize-none"
        ></textarea>
        <div class="flex justify-end mt-3">
          <button 
            @click="generateFromScript" 
            :disabled="generating || !script.trim()"
            class="px-4 py-2 bg-[#6366f1] hover:bg-[#5558e3] disabled:opacity-50 rounded-xl text-white text-sm"
          >
            {{ generating ? 'AI拆解中...' : 'AI拆解分镜' }}
          </button>
        </div>
      </div>

      <!-- 分镜列表 -->
      <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div 
          v-for="(shot, idx) in storyboards" 
          :key="shot.id"
          @click="openEditor(shot)"
          class="p-4 bg-[#1a1a1a] rounded-xl border border-[#2a2a2a] hover:border-[#6366f1]/50 cursor-pointer"
        >
          <div class="flex items-center gap-3 mb-2">
            <span class="text-xs text-[#606060]">{{ idx + 1 }}</span>
            <span class="px-2 py-0.5 bg-[#6366f1]/20 text-[#6366f1] rounded text-xs">
              {{ getShotTypeLabel(shot.shotType) }}
            </span>
          </div>
          <p class="text-sm text-[#f5f5f5] mb-1">{{ shot.action || '无动作描述' }}</p>
          <p class="text-xs text-[#808080]">{{ shot.dialogue || '无台词' }}</p>
        </div>
      </div>

      <div v-if="storyboards.length === 0" class="text-center py-12 text-[#606060]">
        暂无分镜，输入剧本自动生成
      </div>
    </div>

    <!-- 角色图Tab -->
    <div v-if="activeTab === 'character'" class="space-y-4">
      <div class="text-center py-12 text-[#606060]">
        在角色管理中添加AI生成
      </div>
    </div>

    <!-- 场景图Tab -->
    <div v-if="activeTab === 'scene'" class="space-y-4">
      <div class="text-center py-12 text-[#606060]">
        在场景管理中添加AI生成
      </div>
    </div>

    <!-- 分镜编辑器弹窗 -->
    <div v-if="showEditor" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50" @click.self="showEditor = false">
      <div class="bg-[#1a1a1a] rounded-2xl p-6 w-full max-w-lg border border-[#2a2a2a]">
        <h4 class="text-lg font-medium text-[#f5f5f5] mb-4">编辑分镜</h4>
        <div class="space-y-4">
          <div>
            <label class="block text-sm text-[#a0a0a0] mb-2">镜头类型</label>
            <select v-model="editingShot.shotType" class="w-full px-4 py-3 bg-[#242424] border border-[#2a2a2a] rounded-xl text-[#f5f5f5]">
              <option v-for="t in shotTypes" :key="t.value" :value="t.value">{{ t.label }}</option>
            </select>
          </div>
          <div>
            <label class="block text-sm text-[#a0a0a0] mb-2">动作描述</label>
            <textarea v-model="editingShot.action" rows="3" class="w-full px-4 py-3 bg-[#242424] border border-[#2a2a2a] rounded-xl text-[#f5f5f5] resize-none"></textarea>
          </div>
          <div>
            <label class="block text-sm text-[#a0a0a0] mb-2">台词</label>
            <textarea v-model="editingShot.dialogue" rows="3" class="w-full px-4 py-3 bg-[#242424] border border-[#2a2a2a] rounded-xl text-[#f5f5f5] resize-none"></textarea>
          </div>
        </div>
        <div class="flex justify-end gap-3 mt-6">
          <button @click="showEditor = false" class="px-4 py-2 text-[#a0a0a0]">取消</button>
          <button @click="saveShot" class="px-6 py-2 bg-[#6366f1] rounded-xl text-white">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>