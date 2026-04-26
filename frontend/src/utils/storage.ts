import ofetch from './request'

// 注意：ofetch 已自带 baseURL = '/api/v1'，这里直接写相对路径即可
export const storageApi = {
  // 获取存储状态
  status: () => ofetch('/storage/status'),

  // 上传文件
  upload: (file: File, dramaId?: string, type?: string) => {
    const formData = new FormData()
    formData.append('file', file)
    if (dramaId) formData.append('dramaId', dramaId)
    if (type) formData.append('type', type || 'image')
    return ofetch('/storage/upload', { method: 'POST', body: formData })
  },

  // 批量上传
  batchUpload: (files: File[], dramaId: string, type?: string) => {
    const formData = new FormData()
    files.forEach(f => formData.append('files', f))
    formData.append('dramaId', dramaId)
    if (type) formData.append('type', type)
    return ofetch('/storage/batch-upload', { method: 'POST', body: formData })
  },

  // 删除文件
  delete: (id: string) => ofetch(`/storage/${id}/delete`, { method: 'POST' }),

  // 获取 OSS 配置（脱敏）
  getOssConfig: () => ofetch('/storage/oss-config'),

  // 更新 OSS 配置
  updateOssConfig: (config: Record<string, string>) =>
    ofetch('/storage/oss-config', { method: 'POST', body: config }),
}
