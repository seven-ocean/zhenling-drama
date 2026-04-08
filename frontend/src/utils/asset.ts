import { ofetch } from 'ofetch'

const baseURL = '/api/v1'

export const assetApi = {
  upload: (file: File | Blob, dramaId?: string, type?: string) => {
    const formData = new FormData()
    formData.append('file', file)
    if (dramaId) formData.append('dramaId', dramaId)
    if (type) formData.append('type', type)
    return ofetch('/assets/upload', {
      baseURL,
      method: 'POST',
      body: formData
    })
  },
  list: (dramaId: string, type?: string) => ofetch(`/assets/drama/${dramaId}`, { params: { type }, baseURL }),
  page: (params: any) => ofetch('/assets', { params, baseURL }),
  get: (id: string) => ofetch(`/assets/${id}`, { baseURL }),
  delete: (id: string) => ofetch(`/assets/${id}`, { method: 'DELETE', baseURL }),
}