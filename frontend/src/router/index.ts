import { createRouter, createWebHistory } from 'vue-router'
import DramaList from '@/views/DramaList.vue'
import DramaDetail from '@/views/DramaDetail.vue'
import Workbench from '@/views/Workbench.vue'
import MediaStudio from '@/views/MediaStudio.vue'
import AiConfig from '@/views/AiConfig.vue'

const routes = [
  { path: '/', name: 'home', redirect: '/dramas' },
  { path: '/dramas', name: 'drama-list', component: DramaList },
  { path: '/drama/:id', name: 'drama-detail', component: DramaDetail },
  { path: '/workbench/:dramaId', name: 'workbench', component: Workbench },
  { path: '/media/:dramaId', name: 'media', component: MediaStudio },
  { path: '/settings/ai', name: 'ai-config', component: AiConfig },
]

export const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router