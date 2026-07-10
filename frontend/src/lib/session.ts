import type { AuthResponse } from '../types/auth'

const TOKEN_KEY = 'tripplanner_session'

export interface Session {
  access_token: string
  email: string
  role: 'user' | 'admin'
}

export function saveSession(auth: AuthResponse): void {
  const session: Session = { access_token: auth.access_token, email: auth.email, role: auth.role }
  sessionStorage.setItem(TOKEN_KEY, JSON.stringify(session))
}

export function getSession(): Session | null {
  const raw = sessionStorage.getItem(TOKEN_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as Session
  } catch {
    return null
  }
}

export function clearSession(): void {
  sessionStorage.removeItem(TOKEN_KEY)
}
