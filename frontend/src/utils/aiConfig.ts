import ofetch from './request'

export const aiConfigApi = {
  create: (data: any) => ofetch('/ai-configs', { method: 'POST', body: data }),
  list: () => ofetch('/ai-configs'),
  listByType: (apiType: string) => ofetch(`/ai-configs/type/${apiType}`),
  get: (id: string) => ofetch(`/ai-configs/${id}`),
  update: (id: string, data: any) => ofetch(`/ai-configs/${id}`, { method: 'POST', body: data }),
  delete: (id: string) => ofetch(`/ai-configs/${id}`, { method: 'POST' }),
  toggle: (id: string, enabled: boolean) => ofetch(`/ai-configs/${id}/toggle?enabled=${enabled}`, { method: 'POST' }),

  // 获取可用配置列表（用于前端下拉选择模型/音色）
  getEnabledByType: (apiType: string) => ofetch(`/ai-configs/type/${apiType}`),
}

// ====== TTS 预览 API（不保存到数据库）======
export const ttsPreviewApi = {
  preview: (params: { text: string; voiceId?: string; model?: string; characterId?: string }) =>
    ofetch('/audios/tts-preview', { method: 'POST', params }),
}
