package com.tripplanner.config;

import com.tripplanner.common.ErrorResponse;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger/OpenAPI 문서 설정. CLAUDE.md 4절 계약을 문서에 반영한다:
 * <ul>
 *   <li>JWT Bearer 인증 스킴 등록 → Swagger UI "Authorize"로 보호 엔드포인트 시험 가능</li>
 *   <li>공통 에러 응답 계약({@link ErrorResponse}: error/code/message)을 모든 오퍼레이션에 자동 부착</li>
 * </ul>
 * UI 경로: {@code /swagger-ui.html}, 스펙: {@code /v3/api-docs}.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";
    private static final String ERROR_SCHEMA_REF = "#/components/schemas/ErrorResponse";

    @Bean
    public OpenAPI tripPlannerOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AI 여행 일정 플래너 API")
                        .version("v1")
                        .description("""
                                AI 기반 여행 일정 플래너 백엔드 API.

                                **인증:** 로그인으로 받은 JWT를 우측 상단 Authorize에 `Bearer` 없이 토큰만 입력.
                                access token만 사용(만료 60분), refresh 없음.

                                **에러 형식(공통):** 모든 실패 응답은 `{ "error": ..., "code": "ERR_xxx", "message": ... }` 형태다.
                                `error`는 대분류(VALIDATION_ERROR|GENERATION_FAILED|AUTH_ERROR|FORBIDDEN|STORAGE_ERROR|INTERNAL_ERROR),
                                `code`는 세분화 코드다. 전체 코드 표는 저장소 `docs/error-codes.md` 참고.
                                """))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("로그인 응답의 access_token 값")))
                // 문서상 기본 인증 요구. 공개 엔드포인트(signup/login/logout/health)도 토큰을 함께 보내도 무해하다.
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }

    /**
     * 모든 오퍼레이션에 공통 에러 응답(400/401/403/500)을 부착하고 ErrorResponse 스키마를 등록한다.
     * 이렇게 하면 엔드포인트마다 애노테이션을 달지 않아도 계약이 문서에 일관되게 노출된다.
     */
    @Bean
    public OpenApiCustomizer commonErrorResponsesCustomizer() {
        return openApi -> {
            // ErrorResponse 스키마를 components에 등록(어떤 오퍼레이션에도 명시되지 않았으므로 수동 등록)
            ModelConverters.getInstance().read(ErrorResponse.class).forEach((name, schema) -> {
                if (openApi.getComponents().getSchemas() == null
                        || !openApi.getComponents().getSchemas().containsKey(name)) {
                    openApi.getComponents().addSchemas(name, schema);
                }
            });

            openApi.getPaths().values().forEach(pathItem -> pathItem.readOperations().forEach(operation -> {
                ApiResponses responses = operation.getResponses();
                putIfAbsent(responses, "400", "VALIDATION_ERROR — 입력 검증 실패");
                putIfAbsent(responses, "401", "AUTH_ERROR — 미인증/토큰 만료·무효");
                putIfAbsent(responses, "403", "FORBIDDEN — 권한 없음/소유자 아님");
                putIfAbsent(responses, "500", "INTERNAL_ERROR — 서버 내부 오류");
            }));
        };
    }

    private void putIfAbsent(ApiResponses responses, String status, String description) {
        if (responses.containsKey(status)) {
            return;
        }
        responses.addApiResponse(status, new ApiResponse()
                .description(description)
                .content(new Content().addMediaType("application/json",
                        new MediaType().schema(new Schema<>().$ref(ERROR_SCHEMA_REF)))));
    }
}
