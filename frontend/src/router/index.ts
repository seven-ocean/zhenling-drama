import { createRouter, createWebHistory } from 'vue-router'
import DramaList from '@/views/DramaList.vue'
import DramaDetail from '@/views/DramaDetail.vue'
import Workbench from '@/views/Workbench.vue'
import MediaStudio from '@/views/MediaStudio.vue'
import AiConfig from '@/views/AiConfig.vue'
import StorageSettings from '@/views/StorageSettings.vue'
import TaskTracker from '@/views/TaskTracker.vue'

const routes = [
  { path: '/', name: 'home', redirect: '/dramas' },
  { path: '/dramas', name: 'drama-list', component: DramaList },
  { path: '/drama/:id', name: 'drama-detail', component: DramaDetail },
  { path: '/workbench/:dramaId', name: 'workbench', component: Workbench },
  { path: '/media/:dramaId', name: 'media', component: MediaStudio },
  { path: '/settings/ai', name: 'ai-config', component: AiConfig },
  { path: '/settings/storage', name: 'storage-settings', component: StorageSettings },
  { path: '/settings/tasks', name: 'task-tracker', component: TaskTracker },
]

export const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router