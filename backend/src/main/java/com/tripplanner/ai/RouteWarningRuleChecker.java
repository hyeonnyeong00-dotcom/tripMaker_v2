package com.tripplanner.ai;

import com.tripplanner.ai.dto.AiActivityPayload;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * CLAUDE.md 5.2: 좌표 기반 이동 거리/순서 룰 체크. Directions API를 쓸 수 없으므로(2절) 활동 간
 * 직선거리(haversine)를 이동 거리의 근사치로 사용한다. AI의 자연어 사유는 참고하되, flagged는
 * 이 룰이 최종 결정한다("사용자가 정한 순서는 절대 임의 변경 금지" — 순서 자체는 그대로, 거리만 평가).
 */
@Component
public class RouteWarningRuleChecker {

    private static final double EARTH_RADIUS_KM = 6371.0;
    // 연속한 두 활동의 직선거리 임계값. 도심 관광 동선에서 한 구간 8km(대략 택시 15~25분)를 넘으면
    // "동선이 비효율적일 수 있다"는 참고 배지를 띄우는 경험적 기준(Directions API 미사용, 직선거리 근사).
    private static final double MAX_LEG_DISTANCE_KM = 8.0;

    public static final String DEFAULT_REASON = "이동 거리가 길어 동선이 비효율적일 수 있어요";

    /** 활동 순서(activitiesInOrder) 상 연속된 두 지점 중 하나라도 임계 거리를 넘으면 flagged=true. */
    public boolean isInefficient(List<AiActivityPayload> activitiesInOrder) {
        for (int i = 0; i < activitiesInOrder.size() - 1; i++) {
            AiActivityPayload from = activitiesInOrder.get(i);
            AiActivityPayload to = activitiesInOrder.get(i + 1);
            if (haversineDistanceKm(from.lat(), from.lng(), to.lat(), to.lng()) > MAX_LEG_DISTANCE_KM) {
                return true;
            }
        }
        return false;
    }

    private double haversineDistanceKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.asin(Math.sqrt(a));
        return EARTH_RADIUS_KM * c;
    }
}
