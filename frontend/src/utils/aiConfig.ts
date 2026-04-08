import ofetch from './request'

export const aiConfigApi = {
  create: (data: any) => ofetch('/ai-configs', { method: 'POST', body: data }),
  listByType: (apiType: string) => ofetch(`/ai-configs/type/${apiType}`),
  get: (id: string) => ofetch(`/ai-configs/${id}`),
  update: (id: string, data: any) => ofetch(`/ai-configs/${id}`, { method: 'PUT', body: data }),
  delete: (id: string) => ofetch(`/ai-configs/${id}`, { method: 'DELETE' }),
  toggle: (id: string, enabled: boolean) => ofetch(`/ai-configs/${id}/toggle?enabled=${enabled}`, { method: 'PATCH' }),
}