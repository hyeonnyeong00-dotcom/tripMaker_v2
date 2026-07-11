package com.tripplanner.admin;

import com.tripplanner.admin.dto.PromptTemplateDto;
import com.tripplanner.ai.PromptTemplate;
import com.tripplanner.ai.PromptTemplateRepository;
import com.tripplanner.common.ApiException;
import com.tripplanner.common.ErrorCode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminPromptTemplateService {

    private final PromptTemplateRepository promptTemplateRepository;

    public AdminPromptTemplateService(PromptTemplateRepository promptTemplateRepository) {
        this.promptTemplateRepository = promptTemplateRepository;
    }

    @Transactional(readOnly = true)
    public List<PromptTemplateDto> listAll() {
        return promptTemplateRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public PromptTemplateDto getById(UUID id) {
        return toDto(findOrThrow(id));
    }

    @Transactional
    public PromptTemplateDto update(UUID id, String content) {
        PromptTemplate template = findOrThrow(id);
        template.setContent(content);
        template.setVersion(template.getVersion() + 1);
        template.setUpdatedAt(OffsetDateTime.now());
        return toDto(promptTemplateRepository.save(template));
    }

    private PromptTemplate findOrThrow(UUID id) {
        return promptTemplateRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.VALIDATION_ERROR, "존재하지 않는 템플릿입니다."));
    }

    private PromptTemplateDto toDto(PromptTemplate template) {
        return new PromptTemplateDto(
                template.getId(),
                template.getName(),
                template.getContent(),
                template.getVersion(),
                template.isActive(),
                template.getUpdatedAt());
    }
}
