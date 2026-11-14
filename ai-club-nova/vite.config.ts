import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// Backend address for the dev/preview proxy. Change it if the API runs elsewhere.
const backend = 'http://localhost:8080'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5174,
    watch: {
      ignored: [
        '**/.chrome-profile/**',
        '**/.preview-*/**',
        '**/.tmp/**',
        '**/.npm-cache/**',
        '**/dist/**',
        '**/*.tmpdir/**',
        '**/*.tmpdir',
        '**/*.tmp',
      ],
    },
    proxy: {
      '/api': { target: backend, changeOrigin: false },
    },
  },
  preview: {
    port: 4174,
    proxy: {
      '/api': { target: backend, changeOrigin: false },
    },
  },
})
