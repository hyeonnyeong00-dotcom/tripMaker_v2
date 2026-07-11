// frontend/src/types/trip.ts
// data-spec 8.1/8.3과 snake_case 그대로 1:1 (변환 레이어 없음)

export interface TripCreateRequest {
  destination: string
  start_date: string
  end_date: string
  budget_min: number
  budget_max: number | null
  companion: string
  preferences: string[]
  include_nearby: boolean
  active_start_time: string
  active_end_time: string
}

export interface TripSummary {
  trip_id: string
  destination: string
  summary: string | null
  start_date: string
  end_date: string
  duration_days: number
  preferences: string[]
}

export interface ReorderRequest {
  day: number
  new_activity_order: string[]
}

export interface RouteWarning {
  flagged: boolean
  reason: string | null
}

export interface Activity {
  id: string
  time: string | null
  title: string
  description: string | null
  category: string
  duration_minutes: number | null
  location: string | null
  estimated_cost: number | null
  tips: string | null
  lat: number
  lng: number
}

export interface ItineraryDay {
  day: number
  theme: string | null
  activities: Activity[]
  last_modified: boolean
  route_warning: RouteWarning
}

export interface TripMeta {
  generated_at: string
  revision: number
}

export interface TripResponse {
  trip_id: string
  destination: string
  duration_days: number
  summary: string | null
  days: ItineraryDay[]
  meta: TripMeta
}
