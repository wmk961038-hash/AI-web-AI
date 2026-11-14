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
import App from './App.vue'
import { router } from './router'
import './style.css'
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
app.mount('#app')
