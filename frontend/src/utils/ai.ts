import api from './request'

export const aiApi = {
  // 分镜
  generateStoryboards: (dramaId: string, script: string, episodeNumber = 1) => 
    api('/storyboards/generate', { method: 'POST', params: { dramaId, script, episodeNumber } }),
  generateGridPrompt: (params: any) => 
    api('/storyboards/grid-prompt', { method: 'POST', body: params }),
  getStoryboards: (dramaId: string, episodeNumber: number) => 
    api(`/storyboards/drama/${dramaId}/episode/${episodeNumber}`),

  // 图片
  generateCharacterImage: (params: any) => 
    api('/images/character', { method: 'POST', params }),
  generateSceneImage: (params: any) => 
    api('/images/scene', { method: 'POST', params }),
  generateGridImage: (params: any) => 
    api('/images/grid', { method: 'POST', params }),

  // 参考图
  generateReference: (params: { storyboardId: string, forceRegenerate?: boolean }) =>
    api('/image-reference/generate', { method: 'POST', params }),
  getReference: (storyboardId: string) =>
    api(`/image-reference/${storyboardId}`),
  deleteReference: (storyboardId: string) =>
    api(`/image-reference/${storyboardId}`, { method: 'DELETE' }),
}
