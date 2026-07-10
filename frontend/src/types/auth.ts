export interface SignupRequest {
  email: string
  password: string
}

export interface LoginRequest {
  email: string
  password: string
}

export interface MeResponse {
  user_id: string
  email: string
  role: 'user' | 'admin'
}

export interface AuthResponse {
  access_token: string
  token_type: string
  expires_in: number
  email: string
  role: 'user' | 'admin'
}

export interface ApiErrorResponse {
  error: 'VALIDATION_ERROR' | 'GENERATION_FAILED' | 'AUTH_ERROR' | 'FORBIDDEN' | 'STORAGE_ERROR'
  message: string
}
