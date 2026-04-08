export const mediaApi = {
  // 配音
  generateTTS: (params: any) => ofetch('/audios/generate', { method: 'POST', params }),
  batchGenerateTTS: (dramaId: string, episodeNumber: number, dialogues: any[]) =>
    ofetch('/audios/batch', { method: 'POST', params: { dramaId, episodeNumber }, body: dialogues }),
  listAudios: (dramaId: string, episodeNumber: number) =>
    ofetch(`/audios/drama/${dramaId}/episode/${episodeNumber}`),

  // 视频生成
  generateVideo: (params: any) => ofetch('/videos/generate', { method: 'POST', params }),
  batchGenerateVideo: (dramaId: string, episodeNumber: number) =>
    ofetch('/videos/batch', { method: 'POST', params: { dramaId, episodeNumber } }),
}