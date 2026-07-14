import axios from 'axios'
import { clearSession, getSession } from '../lib/session'

export const SESSION_EXPIRED_FLAG = 'tripplanner_session_expired'

/** 읽기와 제거를 분리 — StrictMode가 state 초기화 함수를 두 번 호출해도 안전하도록 제거는 useEffect에서 수행 */
export function readSessionExpiredFlag(): boolean {
  return sessionStorage.getItem(SESSION_EXPIRED_FLAG) === '1'
}

export function clearSessionExpiredFlag(): void {
  sessionStorage.removeItem(SESSION_EXPIRED_FLAG)
}

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
})

apiClient.interceptors.request.use((config) => {
  const session = getSession()
  if (session) {
    config.headers.Authorization = `Bearer ${session.access_token}`
  }
  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // 세션이 있던 상태의 401만 "만료"로 안내(로그인 실패 401은 세션이 없어 플래그 미기록)
      if (getSession()) {
        sessionStorage.setItem(SESSION_EXPIRED_FLAG, '1')
      }
      clearSession()
      const loginPath = window.location.pathname.startsWith('/admin') ? '/admin/login' : '/login'
      if (window.location.pathname !== loginPath) {
        window.location.href = loginPath
      }
    }
    return Promise.reject(error)
  },
)
