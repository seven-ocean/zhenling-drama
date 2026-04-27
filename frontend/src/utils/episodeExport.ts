import api from './request'

export const episodeExportApi = {
  // 导出整集视频
  exportEpisode: (dramaId: string, episodeNumber: number) =>
    api('/episode-exports/export', { method: 'POST', params: { dramaId, episodeNumber } }),
  
  // 查询导出记录列表
  list: (dramaId: string, episodeNumber: number, pageNum = 1, pageSize = 10) =>
    api('/episode-exports', { params: { dramaId, episodeNumber, pageNum, pageSize } }),
  
  // 获取单个导出记录
  getById: (id: string) =>
    api(`/episode-exports/${id}`),
  
  // 删除导出记录
  delete: (id: string) =>
    api(`/episode-exports/${id}`, { method: 'DELETE' }),
}
