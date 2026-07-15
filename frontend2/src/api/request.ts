import axios from 'axios'
import type { AxiosResponse } from 'axios'
import type { R } from '@/types/api'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
})

/** 请求拦截器：添加 token */
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token && config.headers) {
      // Axios 1.x: 用 set() 方法（最可靠）
      ;(config.headers as any).set('Authorization', `Bearer ${token}`)
    }
    return config
  },
  (error) => Promise.reject(error)
)

/** 响应拦截器：业务状态码处理 */
request.interceptors.response.use(
  (response: AxiosResponse<R>) => {
    const { data } = response
    if (data.code === 200) {
      return data
    }
    if (data.code === 401) {
      localStorage.removeItem('token')
      window.location.href = '/login'
      return Promise.reject(new Error(data.message || '未登录'))
    }
    return Promise.reject(new Error(data.message || '请求失败'))
  },
  (error) => {
    const status = error.response?.status
    if (status === 401 || status === 403) {
      localStorage.removeItem('token')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default request
