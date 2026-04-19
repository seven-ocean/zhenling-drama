import api from './request'

export const mediaApi = {
  // 配音
  generateTTS: (params: any) => api('/audios/generate', { method: 'POST', params }),
  batchGenerateTTS: (dramaId: string, episodeNumber: number, dialogues: any[]) =>
    api('/audios/batch', { method: 'POST', params: { dramaId, episodeNumber }, body: dialogues }),
  listAudios: (dramaId: string, episodeNumber: number) =>
    api(`/audios/drama/${dramaId}/episode/${episodeNumber}`),

  // 视频生成
  generateVideo: (params: any) => api('/videos/generate', { method: 'POST', params }),
  batchGenerateVideo: (dramaId: string, episodeNumber: number) =>
    api('/videos/batch', { method: 'POST', params: { dramaId, episodeNumber } }),
}
