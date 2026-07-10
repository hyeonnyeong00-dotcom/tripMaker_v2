package com.tripplanner.ai;

import org.springframework.stereotype.Service;

@Service
public class PromptTemplateService {

    private final PromptTemplateRepository promptTemplateRepository;

    public PromptTemplateService(PromptTemplateRepository promptTemplateRepository) {
        this.promptTemplateRepository = promptTemplateRepository;
    }

    /** 이름으로 활성 템플릿을 조회한다. 없으면 배포 오류이므로 즉시 실패시킨다. */
    public PromptTemplate loadActive(String name) {
        return promptTemplateRepository.findByNameAndIsActiveTrue(name)
                .orElseThrow(() -> new IllegalStateException("활성 prompt_template이 없습니다: " + name));
    }
}
