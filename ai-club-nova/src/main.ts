import { createApp } from 'vue'
import { createPinia } from 'pinia'
import {
  ElButton,
  ElSelect,
  ElOption,
  ElPagination,
  ElDialog,
  ElTable,
  ElTableColumn,
  ElTag,
  ElLoading,
} from 'element-plus'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import App from './App.vue'
import { router } from './router'
import { reveal, tilt } from './directives/motion'
import './styles/base.css'
import './styles/public.css'
import './styles/admin.css'

document.documentElement.classList.add('dark')

const app = createApp(App).use(createPinia()).use(router)

for (const component of [
  ElButton,
  ElSelect,
  ElOption,
  ElPagination,
  ElDialog,
  ElTable,
  ElTableColumn,
  ElTag,
]) {
  app.component(component.name!, component)
}
app.directive('loading', ElLoading.directive)
app.directive('reveal', reveal)
app.directive('tilt', tilt)
app.mount('#app')
