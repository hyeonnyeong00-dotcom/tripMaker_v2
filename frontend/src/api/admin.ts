import { apiClient } from './client'
import type { DestinationStatsResponse, FlaggedTripsResponse, PromptTemplate } from '../types/admin'

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
