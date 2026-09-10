import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (res) => {
    const data = res.data
    if (data.code !== 200) {
      ElMessage.error(data.message || '请求失败')
      if (data.code === 401) {
        localStorage.removeItem('token')
        localStorage.removeItem('user')
        if (!location.hash.includes('/login')) {
          router.push('/login')
        }
      }
      return Promise.reject(new Error(data.message || '请求失败'))
    }
    return data.data
  },
  (err) => {
    const status = err.response?.status
    const msg = err.response?.data?.message
    if (status === 401) {
      ElMessage.error(msg || '未登录或登录已过期')
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      if (!location.hash.includes('/login')) {
        router.push('/login')
      }
    } else {
      ElMessage.error(msg || '网络错误，请稍后再试')
    }
    return Promise.reject(err)
  }
)

export default request
