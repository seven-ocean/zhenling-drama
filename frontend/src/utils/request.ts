import { ofetch } from 'ofetch'

const baseURL = '/api/v1'

// 创建带 baseURL 的 ofetch 实例（关键！所有 API 请求自动加 /api/v1 前缀）
const api = ofetch.create({
  baseURL,
  onRequest({ request, options }) {
    const token = localStorage.getItem('token')
    if (token) {
      options.headers.set('Authorization', `Bearer ${token}`)
    }
  },
  onResponseError({ response }) {
    if (response.status === 401) {
      localStorage.removeItem('token')
      // window.location.href = '/login'
    }
  }
})

export default api

// ====== 剧集 API ======
export const dramaApi = {
  list: (params: any) => api('/dramas', { params }),
  create: (data: any) => api('/dramas', { method: 'POST', body: data }),
  get: (id: string) => api(`/dramas/${id}`),
  update: (id: string, data: any) => api(`/dramas/${id}`, { method: 'PUT', body: data }),
  delete: (id: string) => api(`/dramas/${id}`, { method: 'DELETE' }),
  updateStatus: (id: string, status: string) =>
    api(`/dramas/${id}/status?status=${status}`, { method: 'PATCH' }),
}

// ====== 角色 API ======
export const characterApi = {
  list: (dramaId: string) => api(`/characters/drama/${dramaId}`),
  create: (data: any) => api('/characters', { method: 'POST', body: data }),
  get: (id: string) => api(`/characters/${id}`),
  update: (id: string, data: any) => api(`/characters/${id}`, { method: 'PUT', body: data }),
  delete: (id: string) => api(`/characters/${id}`, { method: 'DELETE' }),
}

// ====== 场景 API ======
export const sceneApi = {
  list: (dramaId: string) => api(`/scenes/drama/${dramaId}`),
  create: (data: any) => api('/scenes', { method: 'POST', body: data }),
  get: (id: string) => api(`/scenes/${id}`),
  update: (id: string, data: any) => api(`/scenes/${id}`, { method: 'PUT', body: data }),
  delete: (id: string) => api(`/scenes/${id}`, { method: 'DELETE' }),
}

// ====== 分镜 API ======
export const storyboardApi = {
  list: (dramaId: string, episode: number) =>
    api(`/storyboards/drama/${dramaId}/episode/${episode}`),
  create: (data: any) => api('/storyboards', { method: 'POST', body: data }),
  get: (id: string) => api(`/storyboards/${id}`),
  update: (id: string, data: any) => api(`/storyboards/${id}`, { method: 'PUT', body: data }),
  delete: (id: string) => api(`/storyboards/${id}`, { method: 'DELETE' }),
}

// ====== 音频 API ======
export const audioApi = {
  list: (dramaId: string, episodeNumber: number) =>
    api(`/audios/drama/${dramaId}/episode/${episodeNumber}`),
}

// ====== 视频 API ======
export const videoApi = {
  list: (dramaId: string) => api(`/videos/drama/${dramaId}`),
  generate: (data: any) => api('/videos/generate', { method: 'POST', body: data }),
}
