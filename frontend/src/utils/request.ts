import { ofetch } from 'ofetch'

const baseURL = '/api/v1'

// 统一请求拦截
ofetch.create({
  baseURL,
  onRequest({ request, options }) {
    const token = localStorage.getItem('token')
    if (token) {
      options.headers.set('Authorization', `Bearer ${token}`)
    }
  },
  onResponseError({ request, response }) {
    if (response.status === 401) {
      localStorage.removeItem('token')
      window.location.href = '/login'
    }
  }
})

export default ofetch

// API 方法封装
export const dramaApi = {
  // 剧集列表
  list: (params: any) => ofetch('/dramas', { params }),
  // 创建剧集
  create: (data: any) => ofetch('/dramas', { method: 'POST', body: data }),
  // 剧集详情
  get: (id: string) => ofetch(`/dramas/${id}`),
  // 更新剧集
  update: (id: string, data: any) => ofetch(`/dramas/${id}`, { method: 'PUT', body: data }),
  // 删除剧集
  delete: (id: string) => ofetch(`/dramas/${id}`, { method: 'DELETE' }),
  // 更新状态
  updateStatus: (id: string, status: string) => ofetch(`/dramas/${id}/status?status=${status}`, { method: 'PATCH' }),
}

export const characterApi = {
  list: (dramaId: string) => ofetch(`/characters/drama/${dramaId}`),
  create: (data: any) => ofetch('/characters', { method: 'POST', body: data }),
  get: (id: string) => ofetch(`/characters/${id}`),
  update: (id: string, data: any) => ofetch(`/characters/${id}`, { method: 'PUT', body: data }),
  delete: (id: string) => ofetch(`/characters/${id}`, { method: 'DELETE' }),
}

export const sceneApi = {
  list: (dramaId: string) => ofetch(`/scenes/drama/${dramaId}`),
  create: (data: any) => ofetch('/scenes', { method: 'POST', body: data }),
  get: (id: string) => ofetch(`/scenes/${id}`),
  update: (id: string, data: any) => ofetch(`/scenes/${id}`, { method: 'PUT', body: data }),
  delete: (id: string) => ofetch(`/scenes/${id}`, { method: 'DELETE' }),
}

export const storyboardApi = {
  list: (dramaId: string, episode: number) => ofetch(`/storyboards/drama/${dramaId}/episode/${episode}`),
  create: (data: any) => ofetch('/storyboards', { method: 'POST', body: data }),
  get: (id: string) => ofetch(`/storyboards/${id}`),
  update: (id: string, data: any) => ofetch(`/storyboards/${id}`, { method: 'PUT', body: data }),
  delete: (id: string) => ofetch(`/storyboards/${id}`, { method: 'DELETE' }),
}

export const videoApi = {
  list: (dramaId: string) => ofetch(`/videos/drama/${dramaId}`),
  generate: (data: any) => ofetch('/videos/generate', { method: 'POST', body: data }),
  get: (id: string) => ofetch(`/videos/${id}`),
}