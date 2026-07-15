package com.tripplanner.common;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** 배포 헬스체크용 공개 엔드포인트(Render healthCheckPath). 인증 불필요. */
@Tag(name = "Health", description = "헬스체크")
@RestController
public class HealthController {

    @GetMapping("/api/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
