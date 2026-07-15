package com.tripplanner.ai;

import com.tripplanner.common.ApiException;
import com.tripplanner.common.ErrorCode;
import com.tripplanner.common.ErrorCodes;
import org.springframework.stereotype.Service;

@Service
public class PromptTemplateService {

    private final PromptTemplateRepository promptTemplateRepository;

    public PromptTemplateService(PromptTemplateRepository promptTemplateRepository) {
        this.promptTemplateRepository = promptTemplateRepository;
    }

    /** 이름으로 활성 템플릿을 조회한다. 없으면 시드/배포 오류(ERR_045)이므로 즉시 실패시킨다. */
    public PromptTemplate loadActive(String name) {
        return promptTemplateRepository.findByNameAndIsActiveTrue(name)
                .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_ERROR, ErrorCodes.PROMPT_TEMPLATE_MISSING,
                        "일정 생성 구성 오류입니다. 잠시 후 다시 시도해주세요. (활성 템플릿 없음: " + name + ")"));
    }
}
