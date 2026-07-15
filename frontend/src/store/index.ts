import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getToken, setToken, removeToken } from '@/utils/auth'
import { login as apiLogin, getUserInfo } from '@/api/auth'

/**
 * 用户状态管理 Store
 * 使用 Pinia 管理用户认证状态
 */
export const useUserStore = defineStore('user', () => {
  // 状态
  const token = ref<string>(getToken() || '')
  const username = ref<string>('')
  const realName = ref<string>('')

  /**
   * 用户登录
   * @param username 用户名
   * @param password 密码
   */
  const login = async (username: string, password: string) => {
    const res = await apiLogin({ username, password })
    // res = response.data from interceptor = {code, message, data, timestamp}
    if (res.code === 200) {
      const { token: newToken } = res.data
      token.value = newToken
      setToken(newToken)
      // 获取用户信息
      await fetchUserInfo()
    } else {
      throw new Error(res.message || '登录失败')
    }
  }

  /**
   * 获取用户信息
   */
  const fetchUserInfo = async () => {
    try {
      const res = await getUserInfo()
      if (res.code === 200) {
        username.value = res.data.username
        realName.value = res.data.realName
      }
    } catch (error) {
      console.error('获取用户信息失败', error)
    }
  }

  /**
   * 用户登出
   */
  const logout = () => {
    token.value = ''
    username.value = ''
    realName.value = ''
    removeToken()
  }

  return {
    token,
    username,
    realName,
    login,
    fetchUserInfo,
    logout
  }
})
