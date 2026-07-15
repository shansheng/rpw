import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, getUserInfo } from '@/api/auth'
import type { LoginParams } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  // state
  const token = ref<string>(localStorage.getItem('token') || '')
  const username = ref<string>('')
  const realName = ref<string>('')
  const avatar = ref<string>('')

  // getters
  const isLoggedIn = computed(() => !!token.value)
  const displayName = computed(() => realName.value || username.value)

  // actions
  async function login(loginParams: LoginParams): Promise<void> {
    const res = await loginApi(loginParams)
    const { token: newToken } = res.data
    token.value = newToken
    localStorage.setItem('token', newToken)
    await fetchUserInfo()
  }

  async function fetchUserInfo(): Promise<void> {
    try {
      const res = await getUserInfo()
      const userInfo = res.data
      username.value = userInfo.username
      realName.value = userInfo.realName
      avatar.value = userInfo.avatar || ''
    } catch {
      // 如果获取用户信息失败，清除 token
      logout()
    }
  }

  function logout(): void {
    token.value = ''
    username.value = ''
    realName.value = ''
    avatar.value = ''
    localStorage.removeItem('token')
  }

  return {
    token,
    username,
    realName,
    avatar,
    isLoggedIn,
    displayName,
    login,
    fetchUserInfo,
    logout
  }
})
