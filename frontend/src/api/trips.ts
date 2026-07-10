import { apiClient } from './client'
import type { TripCreateRequest, TripResponse } from '../types/trip'

export async function createTrip(request: TripCreateRequest): Promise<TripResponse> {
  const { data } = await apiClient.post<TripResponse>('/api/trips', request)
  return data
}

export async function getTrip(tripId: string): Promise<TripResponse> {
  const { data } = await apiClient.get<TripResponse>(`/api/trips/${tripId}`)
  return data
}
