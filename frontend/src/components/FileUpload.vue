<script setup lang="ts">
import { ref } from 'vue'
import { assetApi } from '@/utils/asset'

const props = defineProps<{
  dramaId?: string
  type?: string
  accept?: string
}>()

const emit = defineEmits<{
  success: [file: any]
  error: [error: any]
}>()

const uploading = ref(false)
const fileInput = ref<HTMLInputElement>()

const handleClick = () => {
  fileInput.value?.click()
}

const handleChange = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  uploading.value = true
  try {
    const res = await assetApi.upload(file, props.dramaId, props.type)
    if (res.code === 200) {
      emit('success', res.data)
    } else {
      emit('error', res.message)
    }
  } catch (e: any) {
    emit('error', e.message)
  } finally {
    uploading.value = false
    target.value = ''
  }
}
</script>

<template>
  <div>
    <input
      ref="fileInput"
      type="file"
      :accept="accept"
      class="hidden"
      @change="handleChange"
    />
    <button
      :disabled="uploading"
      @click="handleClick"
      class="px-4 py-2 bg-[#6366f1] hover:bg-[#5558e3] disabled:opacity-50 rounded-xl text-white transition-colors"
    >
      <span v-if="uploading">上传中...</span>
      <slot v-else>选择文件</slot>
    </button>
  </div>
</template>