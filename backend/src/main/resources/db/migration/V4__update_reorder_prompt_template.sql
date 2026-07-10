-- M7: reorder 프롬프트 본문 채움(V2 placeholder 대체).
-- {{original_itinerary_json}}, {{day}}, {{new_activity_order}}는
-- ai/ReorderPromptRenderer가 요청값으로 치환한다.

UPDATE prompt_templates
SET content = $prompt$너는 여행 일정 플래너 AI다. 사용자가 특정 날짜의 활동 순서를 드래그로 바꿨다. 그 날짜만 다시 계산해라.

[현재 확정된 전체 일정(JSON)]
{{original_itinerary_json}}

[사용자 요청]
- 대상 날짜: {{day}}일차
- {{day}}일차의 새 활동 순서(activity id 배열, 이 순서를 절대 바꾸지 마라): {{new_activity_order}}

[규칙 — 반드시 지켜라]
1. {{day}}일차만 다시 계산해라:
   - 활동 순서는 위에서 지정한 새 순서를 정확히 그대로 유지해라. 순서 자체를 임의로 바꾸는 것은 절대 금지다.
   - 각 활동의 time만 새 순서에 맞게 자연스럽게 재배치해라.
   - title/description/category/location/estimated_cost/duration_minutes/lat/lng는 위 원본 JSON에 있는 같은 activity id의 값을 그대로 사용해라(같은 장소이므로 값이 바뀌면 안 된다). tips는 필요하면 자연스럽게 조정해도 된다.
   - 이 날짜의 동선이 비효율적인지(활동 간 이동 거리가 먼 경우) 자연어로 사유를 판단해서 route_warning.reason에 채워라(reason만 참고용이며 최종 flagged는 서버가 별도 계산한다). 문제 없으면 reason=null.
   - theme은 그대로 유지하거나, 순서 변경으로 흐름이 바뀌었다면 자연스럽게 다시 써도 된다.
2. {{day}}일차를 제외한 모든 날짜는 위 원본 JSON과 완전히 동일하게, 활동 하나하나와 필드 하나하나까지 그대로 반환해라. 절대 수정하거나 순서를 바꾸지 마라.
3. 출력은 최초 생성과 동일한 JSON 스키마(destination, duration_days, summary, days[])이며, 다른 설명 문장이나 마크다운 코드펜스(```) 없이 JSON 객체 하나만 출력해라.

[출력 JSON 스키마]
{
  "destination": "string",
  "duration_days": number,
  "summary": "string",
  "days": [
    {
      "day": number,
      "theme": "string",
      "activities": [
        {
          "id": "string",
          "time": "HH:mm",
          "title": "string",
          "description": "string",
          "category": "string",
          "duration_minutes": number,
          "location": "string",
          "estimated_cost": number,
          "tips": "string 또는 null",
          "lat": number,
          "lng": number
        }
      ],
      "route_warning": { "flagged": boolean, "reason": "string 또는 null" }
    }
  ]
}$prompt$,
    version = 2,
    updated_at = now()
WHERE name = 'reorder';
