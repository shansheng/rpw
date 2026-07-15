import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios'
import { getToken, removeToken } from '@/utils/auth'
import { ElMessage } from 'element-plus'
import router from '@/router'

/**
 * 创建 Axios 实例
 * 配置基础URL、超时时间等
 */
const service: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json'
  }
})

/**
 * 请求拦截器
 * - 自动携带Token
 * - 可在此处添加loading等
 */
service.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

/**
 * 响应拦截器
 * - 统一处理错误
 * - Token过期自动跳转登录
 */
service.interceptors.response.use(
  (response: AxiosResponse) => {
    const res = response.data

    // 业务状态码不是200，说明有错误
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')

      // Token过期或无效
      if (res.code === 401) {
        removeToken()
        router.push('/login')
      }

      return Promise.reject(new Error(res.message || 'Error'))
    }

    return res
  },
  (error) => {
    const message = error.response?.data?.message || error.message || '网络异常'
    ElMessage.error(message)

    // 401未授权，跳转登录页
    if (error.response?.status === 401) {
      removeToken()
      router.push('/login')
    }

    return Promise.reject(error)
  }
)

export default service
