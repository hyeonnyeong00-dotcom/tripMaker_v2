// frontend/src/types/trip.ts
// data-spec 8.1 최초 생성 입력과 snake_case 그대로 1:1 (변환 레이어 없음)

export interface TripCreateRequest {
  destination: string
  start_date: string
  end_date: string
  budget_level: string
  preferences: string[]
  include_nearby: boolean
}
