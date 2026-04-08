<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { aiConfigApi } from '@/utils/aiConfig'

const configs = ref<any[]>([])
const loading = ref(false)
const showModal = ref(false)
const editing = ref<any>(null)

const providers = [
  { value: 'openai', label: 'OpenAI' },
  { value: 'minimax', label: 'MiniMax' },
  { value: 'gemini', label: 'Google Gemini' },
  { value: 'ali', label: '阿里云' },
  { value: 'volcengine', label: '火山引擎' },
]

const apiTypes = [
  { value: 'text', label: '文本生成' },
  { value: 'image', label: '图片生成' },
  { value: 'video', label: '视频生成' },
  { value: 'tts', label: '语音合成' },
]

const loadConfigs = async () => {
  loading.value = true
  try {
    const res = await aiConfigApi.listByType('image')
    if (res.code === 200) {
      configs.value = res.data || []
    }
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const openModal = (item?: any) => {
  editing.value = item ? { ...item } : { provider: 'openai', apiType: 'image', priority: 0, enabled: true }
  showModal.value = true
}

const save = async () => {
  loading.value = true
  try {
    if (editing.value.id) {
      await aiConfigApi.update(editing.value.id, editing.value)
    } else {
      await aiConfigApi.create(editing.value)
    }
    showModal.value = false
    await loadConfigs()
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const toggle = async (item: any) => {
  try {
    await aiConfigApi.toggle(item.id, !item.enabled)
    await loadConfigs()
  } catch (e) {
    console.error(e)
  }
}

const remove = async (id: string) => {
  if (!confirm('确定删除？')) return
  try {
    await aiConfigApi.delete(id)
    await loadConfigs()
  } catch (e) {
    console.error(e)
  }
}

onMounted(() => {
  loadConfigs()
})
</script>

<template>
  <div class="space-y-6">
    <div class="flex justify-between items-center">
      <h3 class="text-lg font-medium text-[#f5f5f5]">AI 服务配置</h3>
      <button @click="openModal()" class="px-4 py-2 bg-[#6366f1] hover:bg-[#5558e3] rounded-xl text-white text-sm">
        + 添加配置
      </button>
    </div>

    <div v-if="configs.length === 0" class="text-center py-12 text-[#606060]">
      暂无 AI 配置，点击添加
    </div>

    <div v-else class="space-y-3">
      <div v-for="item in configs" :key="item.id" class="flex items-center justify-between p-4 bg-[#1a1a1a] rounded-xl border border-[#2a2a2a]">
        <div class="flex items-center gap-4">
          <span :class="['px-2 py-1 rounded text-xs font-medium', item.enabled ? 'bg-green-500/20 text-green-400' : 'bg-gray-500/20 text-gray-400']">
            {{ item.enabled ? '启用' : '禁用' }}
          </span>
          <span class="text-[#f5f5f5]">{{ providers.find(p => p.value === item.provider)?.label }}</span>
          <span class="text-[#606060]">|</span>
          <span class="text-[#a0a0a0]">{{ apiTypes.find(t => t.value === item.apiType)?.label }}</span>
          <span class="text-[#606060]">|</span>
          <span class="text-[#808080] text-sm">{{ item.model }}</span>
        </div>
        <div class="flex items-center gap-2">
          <button @click="toggle(item)" class="p-2 hover:bg-[#242424] rounded-lg">
            <svg class="w-4 h-4 text-[#a0a0a0]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" :d="item.enabled ? 'M18.364 18.364A9 9 0 005.636 5.636m12.728 12.728A9 9 0 015.636 5.636m12.728 12.728L5.636 5.636' : 'M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z'" />
            </svg>
          </button>
          <button @click="openModal(item)" class="p-2 hover:bg-[#242424] rounded-lg">
            <svg class="w-4 h-4 text-[#a0a0a0]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2l2.828-2.828z" />
            </svg>
          </button>
          <button @click="remove(item.id)" class="p-2 hover:bg-[#242424] rounded-lg">
            <svg class="w-4 h-4 text-red-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
            </svg>
          </button>
        </div>
      </div>
    </div>

    <!-- Modal -->
    <div v-if="showModal" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50" @click.self="showModal = false">
      <div class="bg-[#1a1a1a] rounded-2xl p-6 w-full max-w-md border border-[#2a2a2a]">
        <h4 class="text-lg font-medium text-[#f5f5f5] mb-4">{{ editing.value.id ? '编辑配置' : '添加配置' }}</h4>
        <div class="space-y-4">
          <div>
            <label class="block text-sm text-[#a0a0a0] mb-2">厂商</label>
            <select v-model="editing.provider" class="w-full px-4 py-3 bg-[#242424] border border-[#2a2a2a] rounded-xl text-[#f5f5f5]">
              <option v-for="p in providers" :key="p.value" :value="p.value">{{ p.label }}</option>
            </select>
          </div>
          <div>
            <label class="block text-sm text-[#a0a0a0] mb-2">API类型</label>
            <select v-model="editing.apiType" class="w-full px-4 py-3 bg-[#242424] border border-[#2a2a2a] rounded-xl text-[#f5f5f5]">
              <option v-for="t in apiTypes" :key="t.value" :value="t.value">{{ t.label }}</option>
            </select>
          </div>
          <div>
            <label class="block text-sm text-[#a0a0a0] mb-2">Base URL</label>
            <input v-model="editing.baseUrl" type="text" placeholder="https://api.openai.com/v1" class="w-full px-4 py-3 bg-[#242424] border border-[#2a2a2a] rounded-xl text-[#f5f5f5]" />
          </div>
          <div>
            <label class="block text-sm text-[#a0a0a0] mb-2">API Key</label>
            <input v-model="editing.apiKey" type="password" placeholder="sk-xxx" class="w-full px-4 py-3 bg-[#242424] border border-[#2a2a2a] rounded-xl text-[#f5f5f5]" />
          </div>
          <div>
            <label class="block text-sm text-[#a0a0a0] mb-2">模型</label>
            <input v-model="editing.model" type="text" placeholder="gpt-4o" class="w-full px-4 py-3 bg-[#242424] border border-[#2a2a2a] rounded-xl text-[#f5f5f5]" />
          </div>
          <div>
            <label class="block text-sm text-[#a0a0a0] mb-2">优先级</label>
            <input v-model.number="editing.priority" type="number" class="w-full px-4 py-3 bg-[#242424] border border-[#2a2a2a] rounded-xl text-[#f5f5f5]" />
          </div>
        </div>
        <div class="flex justify-end gap-3 mt-6">
          <button @click="showModal = false" class="px-4 py-2 text-[#a0a0a0] hover:text-[#f5f5f5]">取消</button>
          <button @click="save" :disabled="loading" class="px-6 py-2 bg-[#6366f1] hover:bg-[#5558e3] disabled:opacity-50 rounded-xl text-white">
            保存
          </button>
        </div>
      </div>
    </div>
  </div>
</template>