<script setup lang="ts">
import { ref } from 'vue'
import { mediaApi } from '@/utils/media'

const props = defineProps<{ dramaId: string }>()

const activeTab = ref('tts')
const generating = ref(false)

// TTS
const ttsText = ref('')
const selectedVoice = ref('female-qn-qingse')

const voices = [
  { value: 'female-qn-qingse', label: '青涩女声' },
  { value: 'female-qn-yujie', label: '御姐女声' },
  { value: 'male-qn-qingse', label: '青涩男声' },
  { value: 'male-qn-badao', label: '霸道男声' },
]

const generateTTS = async () => {
  if (!ttsText.value.trim()) return
  generating.value = true
  try {
    await mediaApi.generateTTS({
      dramaId: props.dramaId,
      episodeNumber: 1,
      storyboardId: '',
      characterId: '',
      text: ttsText.value,
      voiceId: selectedVoice.value,
    })
  } catch (e) {
    console.error(e)
  } finally {
    generating.value = false
  }
}
</script>

<template>
  <div class="space-y-6">
    <!-- Tabs -->
    <div class="flex gap-2">
      <button 
        v-for="tab in ['tts', 'video', 'compose']" 
        :key="tab"
        @click="activeTab = tab"
        :class="['px-4 py-2 rounded-xl text-sm', activeTab === tab ? 'bg-[#6366f1] text-white' : 'bg-[#1a1a1a] text-[#a0a0a0]']"
      >
        {{ tab === 'tts' ? '配音' : tab === 'video' ? '视频生成' : '合成' }}
      </button>
    </div>

    <!-- TTS Tab -->
    <div v-if="activeTab === 'tts'" class="space-y-4">
      <div class="bg-[#1a1a1a] rounded-xl p-4 border border-[#2a2a2a]">
        <h4 class="text-sm font-medium text-[#f5f5f5] mb-3">生成配音</h4>
        
        <div class="space-y-4">
          <div>
            <label class="block text-sm text-[#a0a0a0] mb-2">音色</label>
            <select v-model="selectedVoice" class="w-full px-4 py-3 bg-[#242424] border border-[#2a2a2a] rounded-xl text-[#f5f5f5]">
              <option v-for="v in voices" :key="v.value" :value="v.value">{{ v.label }}</option>
            </select>
          </div>
          
          <div>
            <label class="block text-sm text-[#a0a0a0] mb-2">配音文本</label>
            <textarea 
              v-model="ttsText" 
              rows="4"
              placeholder="输入配音文本..."
              class="w-full px-4 py-3 bg-[#242424] border border-[#2a2a2a] rounded-xl text-[#f5f5f5] placeholder-[#606060] resize-none"
            ></textarea>
          </div>
          
          <button 
            @click="generateTTS"
            :disabled="generating || !ttsText.trim()"
            class="w-full py-3 bg-[#6366f1] hover:bg-[#5558e3] disabled:opacity-50 rounded-xl text-white"
          >
            {{ generating ? '生成中...' : '生成配音' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Video Tab -->
    <div v-if="activeTab === 'video'" class="space-y-4">
      <div class="text-center py-12 text-[#606060]">
        在工作台选择图片后生成视频
      </div>
    </div>

    <!-- Compose Tab -->
    <div v-if="activeTab === 'compose'" class="space-y-4">
      <div class="text-center py-12 text-[#606060]">
        功能开发中...
      </div>
    </div>
  </div>
</template>