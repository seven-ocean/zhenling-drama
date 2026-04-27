import { ref } from 'vue'

const toastMessage = ref('')
const toastType = ref<'success' | 'error' | 'info'>('info')
const showToast = ref(false)
let toastTimer: ReturnType<typeof setTimeout> | null = null

export function useToast() {
  const toast = (message: string, type: 'success' | 'error' | 'info' = 'info') => {
    if (toastTimer) clearTimeout(toastTimer)
    toastMessage.value = message
    toastType.value = type
    showToast.value = true
    toastTimer = setTimeout(() => { showToast.value = false }, 3000)
  }

  return { toast, toastMessage, toastType, showToast }
}
