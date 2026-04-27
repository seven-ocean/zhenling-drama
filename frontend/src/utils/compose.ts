import api from './request'

export const composeApi = {
  // 检查 FFmpeg 状态
  checkStatus: () => api('/compose/ffmpeg-status'),

  // 单镜头合成（直接传路径）
  composeShot: (params: { videoPath: string; audioPath?: string; subtitle?: string }) =>
    api('/compose/shot', { method: 'POST', params }),

  // 按分镜ID合成（自动查找视频+音频）
  composeShotByStoryboard: (dramaId: string, storyboardId: string, episodeNumber: number) =>
    api('/compose/shot/storyboard', { method: 'POST', params: { dramaId, storyboardId, episodeNumber } }),

  // 整集拼接合成
  composeEpisode: (dramaId: string, episodeNumber: number) =>
    api('/compose/episode', { method: 'POST', params: { dramaId, episodeNumber } }),

  // 查询合成记录
  listRecords: (params?: any) =>
    api('/compose/records', { params }),
}
