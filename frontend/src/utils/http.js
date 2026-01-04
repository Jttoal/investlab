import axios from 'axios'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
http.interceptors.request.use(
  config => {
    // 可以在这里添加token等
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器
http.interceptors.response.use(
  response => {
    return response.data
  },
  error => {
    // 统一错误处理
    const message = error.response?.data?.message || error.message || '请求失败'
    console.error('API Error:', message)
    
    // 可以在这里添加全局toast提示
    alert(message)
    
    return Promise.reject(error)
  }
)

export default http
