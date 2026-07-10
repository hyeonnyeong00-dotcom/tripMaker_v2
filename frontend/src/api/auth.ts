import { apiClient } from './client'
import type { AuthResponse, LoginRequest, MeResponse, SignupRequest } from '../types/auth'

export async function signup(request: SignupRequest): Promise<MeResponse> {
  const { data } = await apiClient.post<MeResponse>('/api/auth/signup', request)
  return data
}

export async function login(request: LoginRequest): Promise<AuthResponse> {
  const { data } = await apiClient.post<AuthResponse>('/api/auth/login', request)
  return data
}

export async function logout(): Promise<void> {
  await apiClient.post('/api/auth/logout')
}

export async function me(): Promise<MeResponse> {
  const { data } = await apiClient.get<MeResponse>('/api/auth/me')
  return data
}
