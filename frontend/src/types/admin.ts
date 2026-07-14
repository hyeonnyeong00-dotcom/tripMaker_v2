// frontend/src/types/admin.ts
// data-spec과 마찬가지로 snake_case 그대로 1:1 (변환 레이어 없음)

export interface FlaggedTrip {
  trip_id: string
  destination: string
  day: number
  theme: string | null
  reason: string | null
  flagged_at: string
}

export interface FlaggedTripsResponse {
  flagged_trips: FlaggedTrip[]
  flagged_count: number
  total_days: number
  flagged_ratio: number
}

export interface DestinationStat {
  destination: string
  count: number
}

export interface DestinationStatsResponse {
  destinations: DestinationStat[]
  total_trips: number
  period_days: number
}

export interface PromptTemplate {
  id: string
  name: string
  content: string
  version: number
  is_active: boolean
  updated_at: string
}

export interface AdminUser {
  user_id: string
  email: string
  role: 'user' | 'admin'
  last_login_at: string | null
  created_at: string
}

export interface AppSettingResponse {
  value: string
  updated_at: string
}
