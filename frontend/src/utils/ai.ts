export const aiApi = {
  // 分镜
  generateStoryboards: (dramaId: string, script: string, episodeNumber = 1) => 
    ofetch('/storyboards/generate', { method: 'POST', params: { dramaId, script, episodeNumber } }),
  generateGridPrompt: (params: any) => 
    ofetch('/storyboards/grid-prompt', { method: 'POST', body: params }),
  getStoryboards: (dramaId: string, episodeNumber: number) => 
    ofetch(`/storyboards/drama/${dramaId}/episode/${episodeNumber}`),

  // 图片
  generateCharacterImage: (params: any) => 
    ofetch('/images/character', { method: 'POST', params }),
  generateSceneImage: (params: any) => 
    ofetch('/images/scene', { method: 'POST', params }),
  generateGridImage: (params: any) => 
    ofetch('/images/grid', { method: 'POST', params }),
}