import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
        rewrite: (path) => path,
        // 配置代理超时时间,文件上传可能需要更长时间
        timeout: 60000,
        // 保持原始请求体,对于文件上传很重要
        configure: (proxy, options) => {
          proxy.on('proxyReq', (proxyReq, req, res) => {
            // 确保 multipart/form-data 请求正确转发
            if (req.headers['content-type']?.includes('multipart/form-data')) {
              // 不修改 content-type,保持原样
            }
          })
        }
      }
    }
  }
})
