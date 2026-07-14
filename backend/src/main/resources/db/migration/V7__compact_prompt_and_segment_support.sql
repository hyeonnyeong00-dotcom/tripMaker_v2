-- 생성 속도 개선(1분 목표): ① 출력 다이어트(description 40자/tips 30자 제한으로 activity당 토큰 절감)
-- ② 장기 일정 병렬 분할 생성을 위한 {{segment_instruction}} placeholder 추가.
-- {{segment_instruction}}은 ai/InitialGenerationPromptRenderer가 전체/구간 요청에 맞는 지시문으로 치환한다.

UPDATE prompt_templates
SET content = $prompt$너는 여행 일정 플래너 AI다. 아래 조건에 맞는 여행 일정을 JSON으로 생성해라.

[조건]
- 목적지: {{destination}}
- 생성할 일수: {{duration_days}}일 (반드시 days 배열 길이와 일치)
- 구간: {{segment_instruction}}
- 예산: {{budget_range}}
- 동반자: {{companion_instruction}}
- 취향: {{preferences}}
- 근교 포함 여부: {{nearby_instruction}}
- 활동 가능 시간대: {{active_time_range}}

[규칙]
- 각 활동(activity)은 실제 존재할 법한 장소를 기반으로 하고, 그 장소의 근사 위도/경도(lat, lng)를 반드시 함께 반환해라. 좌표를 모르면 목적지 중심 좌표에서 크게 벗어나지 않는 근사치를 추정해서라도 채워라(null 금지).
- 하루 안에서 활동 순서는 이동 동선이 지리적으로 합리적이도록(가까운 장소끼리 묶어서) 배치해라.
- 모든 활동의 time은 반드시 {{active_time_range}} 범위 안에서만 배치해라. 범위 밖 시간에 활동을 배치하지 마라.
- 각 날짜(day)마다 그날 동선이 비효율적인지(예: 활동 간 거리가 멀어 이동 시간이 과도한 경우) 스스로 판단해서 route_warning.flagged와 자연어 사유(reason)를 채워라. 문제가 없으면 flagged=false, reason=null.
- estimated_cost는 원화(KRW) 정수로 추정해라. 무료면 0.
- time은 "HH:mm" 24시간 형식 문자열로 채워라.
- description은 40자 이내 한 문장으로 간결하게 써라. tips는 꼭 필요한 정보가 있을 때만 30자 이내로 쓰고, 없으면 null로 둬라. summary와 reason도 한 문장으로 짧게 써라.
- 다른 설명, 마크다운 코드펜스(```) 없이 아래 JSON 스키마와 동일한 구조의 JSON 객체 하나만 출력해라.

[출력 JSON 스키마]
{
  "destination": "string",
  "duration_days": number,
  "summary": "string (여행 전체 컨셉 한 줄 요약)",
  "days": [
    {
      "day": number,
      "theme": "string",
      "activities": [
        {
          "id": "string (예: d1-a1)",
          "time": "HH:mm",
          "title": "string",
          "description": "string (40자 이내)",
          "category": "string",
          "duration_minutes": number,
          "location": "string",
          "estimated_cost": number,
          "tips": "string(30자 이내) 또는 null",
          "lat": number,
          "lng": number
        }
      ],
      "route_warning": { "flagged": boolean, "reason": "string 또는 null" }
    }
  ]
}$prompt$,
    version = version + 1,
    updated_at = now()
WHERE name = 'initial_generation';

-- reorder: 원본 값을 그대로 복사하므로 tips 길이 제한만 추가(다이어트된 원본이 자연히 반영됨)
UPDATE prompt_templates
SET content = replace(
        content,
        'tips는 필요하면 자연스럽게 조정해도 된다.',
        'tips는 필요하면 자연스럽게 조정하되 30자 이내로 써라.'),
    version = version + 1,
    updated_at = now()
WHERE name = 'reorder';
