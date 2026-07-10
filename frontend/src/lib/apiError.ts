import { isAxiosError } from 'axios'
import type { ApiErrorResponse } from '../types/auth'

export function extractErrorMessage(error: unknown, fallback: string): string {
  if (isAxiosError<ApiErrorResponse>(error) && error.response?.data?.message) {
    return error.response.data.message
  }
  return fallback
}
