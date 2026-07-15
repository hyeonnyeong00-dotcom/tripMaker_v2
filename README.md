# AI 여행 일정 플래너 (tripMaker)

목적지·기간·예산·취향을 입력하면 AI가 일자별 동선을 생성하고, 지도+타임라인으로 확인하며,
드래그로 순서를 바꾸면 **변경한 날짜만 부분 재생성**하는 여행 일정 플래너.

- 프론트: React 18 + TypeScript + Vite → **Vercel**
- 백엔드: Java 17 + Spring Boot 3 (Spring MVC / JPA) → **Render**(Docker)
- DB: **Supabase**(PostgreSQL) 단일 저장소
- AI: Anthropic Claude(저비용 모델)
- 상세 규칙은 [`CLAUDE.md`](./CLAUDE.md), API 계약은 Swagger, 에러 코드는 [`docs/error-codes.md`](./docs/error-codes.md).

---

## 1. 로컬 개발

### 백엔드
```bash
cd backend
# 방법 A) 로컬 프로파일(application-local.yml에 실제 키 입력, git 미추적)
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
# 방법 B) 환경변수로 주입 (backend/.env.example 참고)
export SUPABASE_DB_URL=... JWT_SECRET=... AI_API_KEY=...
./gradlew bootRun
```
- Swagger UI: http://localhost:8080/swagger-ui.html
- 헬스체크: http://localhost:8080/api/health → `{"status":"UP"}`
- Flyway가 기동 시 `db/migration`의 V1~ 마이그레이션을 자동 적용.

### 프론트
```bash
cd frontend
cp .env.example .env   # 값 채우기 (아래 표)
npm install
npm run dev            # http://localhost:5173
```

---

## 2. 환경변수

### 백엔드 (Render)
| 키 | 필수 | 설명 |
|---|---|---|
| `SUPABASE_DB_URL` | ✅ | Supabase Postgres JDBC 문자열. `jdbc:postgresql://<host>:5432/postgres?user=<u>&password=<p>&sslmode=require` |
| `JWT_SECRET` | ✅ | HS256 서명 키. **32바이트(256bit) 이상** 임의 문자열 (예: `openssl rand -base64 32`) |
| `AI_API_KEY` | ✅ | Anthropic API 키 (`sk-ant-...`) |
| `AI_MODEL` | – | 기본 `claude-haiku-4-5-20251001` |
| `AI_BASE_URL` | – | 기본 `https://api.anthropic.com/v1/messages` |
| `APP_CORS_ALLOWED_ORIGINS` | ✅(배포) | 허용 오리진(콤마 구분). **Vercel 도메인**으로 설정. 미설정 시 `*`(로컬 전용) |
| `PORT` | – | Render가 자동 주입. 로컬 기본 8080 |

### 프론트 (Vercel)
| 키 | 필수 | 설명 |
|---|---|---|
| `VITE_API_BASE_URL` | ✅ | 백엔드 베이스 URL. 예: `https://tripmaker-backend.onrender.com` |
| `VITE_GOOGLE_MAPS_API_KEY` | ✅ | Google Maps JavaScript API 키 |

---

## 3. 배포 가이드 (콘솔 조작은 직접)

### 3-1. Supabase (DB)
1. supabase.com → New project 생성. Database 비밀번호 기록.
2. **Settings → Database → Connection string → JDBC** 복사 → `SUPABASE_DB_URL` 값으로 사용(`?sslmode=require` 유지).
3. 스키마/시드는 백엔드 최초 기동 시 Flyway가 자동 적용하므로 수동 실행 불필요.
   - 시드 admin 계정: `admin@tripplanner.local` / `Admin1234!` (배포 후 비밀번호 변경 권장)

### 3-2. 백엔드 → Render (Docker)
1. Render 대시보드 → **New → Blueprint** → 이 저장소 선택. 루트 [`render.yaml`](./render.yaml)이 자동 인식됨.
   - (또는 New → Web Service → Docker, `dockerContext=backend`, `dockerfilePath=backend/Dockerfile`, `healthCheckPath=/api/health` 수동 설정)
2. 배포 중 `sync:false` 시크릿 입력: `SUPABASE_DB_URL`, `JWT_SECRET`, `AI_API_KEY`, `APP_CORS_ALLOWED_ORIGINS`(일단 임시로 `*` 또는 프론트 예정 URL).
3. 배포 완료 후 백엔드 URL 확인(예: `https://tripmaker-backend.onrender.com`). `GET /api/health`로 200 확인.
   - 무료 플랜은 유휴 시 슬립 → 첫 요청이 느릴 수 있음(콜드 스타트).

### 3-3. 프론트 → Vercel
1. Vercel → New Project → 이 저장소 선택.
2. **Root Directory = `frontend`** 로 지정(중요). 프레임워크는 Vite 자동 인식([`frontend/vercel.json`](./frontend/vercel.json)이 SPA rewrite 포함).
3. 환경변수 입력: `VITE_API_BASE_URL`(=3-2의 백엔드 URL), `VITE_GOOGLE_MAPS_API_KEY`.
4. 배포 완료 후 프론트 URL 확인(예: `https://tripmaker.vercel.app`).

### 3-4. CORS 연결 (배포 후 필수)
1. Render → 백엔드 서비스 → Environment → `APP_CORS_ALLOWED_ORIGINS` 를 **실제 Vercel URL**로 교체
   (예: `https://tripmaker.vercel.app`. 여러 개면 콤마로 구분). 저장 시 자동 재배포.
2. 프론트에서 로그인/일정 생성이 CORS 에러 없이 동작하는지 확인.

### 3-5. 배포 검증 체크
- [ ] `GET https://<backend>/api/health` → `{"status":"UP"}`
- [ ] `https://<backend>/swagger-ui.html` 접속 → 엔드포인트/에러 스키마/Authorize 노출
- [ ] 프론트에서 회원가입 → 로그인 → 일정 생성 → 재조정까지 정상
- [ ] 관리자 계정 로그인 → 관리자 화면 3종 접근, 일반 사용자는 403

---

## 4. API 문서 (Swagger)
- UI: `/swagger-ui.html` · 스펙: `/v3/api-docs`
- 우측 상단 **Authorize**에 로그인 응답의 `access_token`만 입력(‘Bearer’ 접두어 불필요).
- 모든 응답은 공통 에러 계약 `{ error, code, message }`을 따르며, 각 오퍼레이션에 400/401/403/500 예시가 부착됨.
  전체 에러 코드 표는 [`docs/error-codes.md`](./docs/error-codes.md).

## 5. 로그
- `backend/logs/error.log` — WARN 이상, 각 줄 `[ERR_xxx]` 포함
- `backend/logs/ai-usage.log` — AI 호출/캐시 히트마다 한 줄(토큰·소요시간·성공여부)
- 둘 다 일 롤링·14일 보관. `logs/`는 git 미추적.
