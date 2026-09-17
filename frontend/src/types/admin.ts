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

// ── 운영 고도화(§5.6-a) — AI 사용량 / 에러 모니터링 ─────────────────────────
export interface AiUsageSummary {
  total_calls: number
  cache_hits: number
  cache_hit_rate: number
  success_calls: number
  total_cost_usd: number
  budget_usd: number
  budget_used_ratio: number
  avg_duration_ms: number
  total_input_tokens: number
  total_output_tokens: number
}

export interface AiUsageDailyPoint {
  date: string
  calls: number
  cache_hits: number
  input_tokens: number
  output_tokens: number
  cost_usd: number
}

export interface AiUsageDailyResponse {
  days: number
  points: AiUsageDailyPoint[]
}

export interface ErrorSummaryItem {
  error_code: string
  error_category: string
  count: number
}

export interface ErrorSummaryResponse {
  items: ErrorSummaryItem[]
  total_errors: number
}

export interface ErrorLogItem {
  id: string
  error_code: string
  error_category: string
  message: string | null
  path: string | null
  created_at: string
}

export interface RecentErrorsResponse {
  limit: number
  errors: ErrorLogItem[]
}
