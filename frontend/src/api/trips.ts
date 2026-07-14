import { apiClient } from './client'
import type { ReorderRequest, TripCreateRequest, TripResponse, TripSummary } from '../types/trip'

export async function listTrips(): Promise<TripSummary[]> {
  const { data } = await apiClient.get<TripSummary[]>('/api/trips')
  return data
}

export async function createTrip(request: TripCreateRequest): Promise<TripResponse> {
  const { data } = await apiClient.post<TripResponse>('/api/trips', request)
  return data
}

export async function getTrip(tripId: string): Promise<TripResponse> {
  const { data } = await apiClient.get<TripResponse>(`/api/trips/${tripId}`)
  return data
}

export async function reorderTrip(tripId: string, request: ReorderRequest): Promise<TripResponse> {
  const { data } = await apiClient.patch<TripResponse>(`/api/trips/${tripId}/reorder`, request)
  return data
}

export async function deleteTrip(tripId: string): Promise<void> {
  await apiClient.delete(`/api/trips/${tripId}`)
}
