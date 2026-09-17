DECISIONS.md (프로젝트 루트, 마일스톤 끝날 때마다 1~3줄 추가)

## M4 — AI 생성 + 캐시
- 캐시 키에 duration_days 대신 처음엔 start_date/end_date를 그대로 썼다가,
  같은 기간인데 날짜만 다른 요청이 캐시 미스 나는 걸 발견해서 정규화 방식으로 변경
- AI가 종종 잘못된 JSON을 반환해서 재시도 로직 추가

## M6 — 부분 재생성 검증
- 처음 프롬프트만으로는 AI가 가끔 다른 날짜도 손댔음 → 응답 검증 로직 별도로 만들게 된 계기
## 운영 고도화 (ai_call_log / error_log / 템플릿 버전 이력 + 관리자 화면)
- AI 호출 기록은 이미 모든 호출·캐시 히트가 거쳐가던 AiUsageLogger 한 곳에서만 DB에 적재.
  생성/재조정 서비스는 손대지 않아 기록 누락·중복 가능성을 없앰
- error_log는 GlobalExceptionHandler의 응답 생성 지점을 한 곳으로 모은 뒤 거기서만 insert.
  DB 장애(STORAGE_ERROR) 때는 이 insert도 실패하므로 REQUIRES_NEW + 실패 무시로 처리해 503 응답 계약을 지킴
  (처음엔 @Transactional로 썼다가 같은 빈 자기 호출이라 전파 속성이 무시되는 걸 발견해 TransactionTemplate으로 교체)
- 템플릿 롤백은 과거 버전 번호로 되돌리지 않고 항상 새 버전으로 증가시켜 이력이 끊기지 않게 함
- **기존 빌드 버그 발견·수정**: tsconfig.app.json의 types 설정이 vite/client만 포함해 google.maps
  전역 타입이 빠져 있었고, TripMap.tsx가 존재하지 않는 typeof google.maps.CoreLibrary를 참조하고 있어
  npm run build(tsc -b)가 원래부터 실패하던 상태였음. 관리자 화면 검증 중 발견해
  types에 google.maps 추가 + 실제 사용하는 Size/Point 생성자 구조 타입으로 교체해 빌드 정상화. 이 수정은 되돌리지 않음
