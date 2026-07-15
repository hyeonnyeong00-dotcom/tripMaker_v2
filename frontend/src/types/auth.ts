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

export interface ChangePasswordRequest {
  current_password: string
  new_password: string
}

export interface ApiErrorResponse {
  error: 'VALIDATION_ERROR' | 'GENERATION_FAILED' | 'AUTH_ERROR' | 'FORBIDDEN' | 'STORAGE_ERROR' | 'INTERNAL_ERROR'
  // 세분화 에러 코드("ERR_xxx"). 백엔드 계약에 추가된 필드(docs/error-codes.md). 기존 error 필드 분기는 그대로 사용.
  code?: string
  message: string
}
