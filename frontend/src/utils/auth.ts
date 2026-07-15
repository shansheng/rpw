/**
 * Token 管理工具
 * 使用 localStorage 存储 Token
 */

const TOKEN_KEY = 'rpw_token'

/**
 * 获取 Token
 * @returns Token字符串，不存在则返回null
 */
export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

/**
 * 保存 Token
 * @param token Token字符串
 */
export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
}

/**
 * 移除 Token
 */
export function removeToken(): void {
  localStorage.removeItem(TOKEN_KEY)
}

/**
 * 判断用户是否已登录
 * @returns 是否已登录
 */
export function isLoggedIn(): boolean {
  return !!getToken()
}
