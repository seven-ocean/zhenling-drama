import { createApp } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import App from './App.vue'
import './style.css'

// 页面
import DramaList from './views/DramaList.vue'
import DramaDetail from './views/DramaDetail.vue'

const routes = [
  { path: '/', name: 'drama-list', component: DramaList },
  { path: '/drama/:id', name: 'drama-detail', component: DramaDetail },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

const app = createApp(App)
app.use(router)
app.mount('#app')