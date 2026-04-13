import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useDramaStore = defineStore('drama', () => {
  const currentDrama = ref<any>(null)
  const characters = ref<any[]>([])
  const scenes = ref<any[]>([])
  const storyboards = ref<any[]>([])

  function setDrama(drama: any) {
    currentDrama.value = drama
  }

  function setCharacters(list: any[]) {
    characters.value = list
  }

  function setScenes(list: any[]) {
    scenes.value = list
  }

  function setStoryboards(list: any[]) {
    storyboards.value = list
  }

  function addCharacter(char: any) {
    characters.value.push(char)
  }

  function updateCharacter(id: string, data: any) {
    const idx = characters.value.findIndex(c => c.id === id)
    if (idx >= 0) characters.value[idx] = { ...characters.value[idx], ...data }
  }

  function removeCharacter(id: string) {
    characters.value = characters.value.filter(c => c.id !== id)
  }

  function addScene(scene: any) {
    scenes.value.push(scene)
  }

  function updateScene(id: string, data: any) {
    const idx = scenes.value.findIndex(s => s.id === id)
    if (idx >= 0) scenes.value[idx] = { ...scenes.value[idx], ...data }
  }

  function removeScene(id: string) {
    scenes.value = scenes.value.filter(s => s.id !== id)
  }

  function reset() {
    currentDrama.value = null
    characters.value = []
    scenes.value = []
    storyboards.value = []
  }

  return {
    currentDrama, characters, scenes, storyboards,
    setDrama, setCharacters, setScenes, setStoryboards,
    addCharacter, updateCharacter, removeCharacter,
    addScene, updateScene, removeScene,
    reset,
  }
})
