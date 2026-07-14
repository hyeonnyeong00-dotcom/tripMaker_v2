import { apiClient } from './client'
import type {
  AdminUser,
  AppSettingResponse,
  DestinationStatsResponse,
  FlaggedTripsResponse,
  PromptTemplate,
} from '../types/admin'

export async function getFlaggedTrips(): Promise<FlaggedTripsResponse> {
  const { data } = await apiClient.get<FlaggedTripsResponse>('/api/admin/flagged-trips')
  return data
}

export async function getDestinationStats(): Promise<DestinationStatsResponse> {
  const { data } = await apiClient.get<DestinationStatsResponse>('/api/admin/stats/destinations')
  return data
}

export async function listPromptTemplates(): Promise<PromptTemplate[]> {
  const { data } = await apiClient.get<PromptTemplate[]>('/api/admin/prompt-templates')
  return data
}

export async function updatePromptTemplate(id: string, content: string): Promise<PromptTemplate> {
  const { data } = await apiClient.put<PromptTemplate>(`/api/admin/prompt-templates/${id}`, { content })
  return data
}

export async function listUsers(role?: 'user' | 'admin'): Promise<AdminUser[]> {
  const { data } = await apiClient.get<AdminUser[]>('/api/admin/users', { params: role ? { role } : undefined })
  return data
}

export async function updateUserRole(userId: string, role: 'user' | 'admin'): Promise<AdminUser> {
  const { data } = await apiClient.patch<AdminUser>(`/api/admin/users/${userId}/role`, { role })
  return data
}

export async function resetUserPassword(userId: string): Promise<void> {
  await apiClient.post(`/api/admin/users/${userId}/reset-password`)
}

export async function deleteUser(userId: string): Promise<void> {
  await apiClient.delete(`/api/admin/users/${userId}`)
}

export async function getResetPasswordSetting(): Promise<AppSettingResponse> {
  const { data } = await apiClient.get<AppSettingResponse>('/api/admin/settings/reset-password')
  return data
}

export async function updateResetPasswordSetting(value: string): Promise<AppSettingResponse> {
  const { data } = await apiClient.put<AppSettingResponse>('/api/admin/settings/reset-password', { value })
  return data
}
