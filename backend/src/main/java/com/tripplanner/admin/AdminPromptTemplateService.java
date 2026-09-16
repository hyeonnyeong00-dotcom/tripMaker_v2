package com.tripplanner.admin;

import com.tripplanner.admin.dto.PromptTemplateDto;
import com.tripplanner.admin.dto.PromptTemplateRevisionDto;
import com.tripplanner.ai.PromptTemplate;
import com.tripplanner.ai.PromptTemplateRepository;
import com.tripplanner.common.ApiException;
import com.tripplanner.common.ErrorCode;
import com.tripplanner.common.ErrorCodes;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminPromptTemplateService {

    private final PromptTemplateRepository promptTemplateRepository;
    private final PromptTemplateRevisionRepository revisionRepository;

    public AdminPromptTemplateService(
            PromptTemplateRepository promptTemplateRepository,
            PromptTemplateRevisionRepository revisionRepository) {
        this.promptTemplateRepository = promptTemplateRepository;
        this.revisionRepository = revisionRepository;
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
        // §5.6-a: 덮어쓰기 "전에" 기존 내용을 스냅샷으로 남긴다(같은 트랜잭션 — 스냅샷 실패 시 저장도 롤백).
        snapshot(template);
        template.setContent(content);
        template.setVersion(template.getVersion() + 1);
        template.setUpdatedAt(OffsetDateTime.now());
        return toDto(promptTemplateRepository.save(template));
    }

    @Transactional(readOnly = true)
    public List<PromptTemplateRevisionDto> listRevisions(UUID templateId) {
        findOrThrow(templateId);
        // 목록은 번호·수정일만 쓰므로 본문은 싣지 않는다(응답 크기 절감).
        return revisionRepository.findByTemplateIdOrderByVersionDesc(templateId).stream()
                .map(revision -> toRevisionDto(revision, false))
                .toList();
    }

    @Transactional(readOnly = true)
    public PromptTemplateRevisionDto getRevision(UUID templateId, int version) {
        findOrThrow(templateId);
        return toRevisionDto(findRevisionOrThrow(templateId, version), true);
    }

    /**
     * §5.6-a 롤백: 과거 버전 내용으로 <b>새 버전</b>을 만든다(버전 번호는 항상 증가, 과거 번호로 되돌리지 않음).
     * 롤백 직전의 현재 내용도 스냅샷으로 남겨 이력이 끊기지 않게 한다.
     */
    @Transactional
    public PromptTemplateDto rollback(UUID templateId, int version) {
        PromptTemplate template = findOrThrow(templateId);
        PromptTemplateRevision target = findRevisionOrThrow(templateId, version);
        snapshot(template);
        template.setContent(target.getContent());
        template.setVersion(template.getVersion() + 1);
        template.setUpdatedAt(OffsetDateTime.now());
        return toDto(promptTemplateRepository.save(template));
    }

    /** 현재 템플릿의 내용을 그 시점 버전 번호로 적재한다. */
    private void snapshot(PromptTemplate template) {
        PromptTemplateRevision revision = new PromptTemplateRevision();
        revision.setTemplateId(template.getId());
        revision.setVersion(template.getVersion());
        revision.setContent(template.getContent());
        revision.setCreatedAt(OffsetDateTime.now());
        revisionRepository.save(revision);
    }

    private PromptTemplate findOrThrow(UUID id) {
        return promptTemplateRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.VALIDATION_ERROR, ErrorCodes.ADMIN_TEMPLATE_NOT_FOUND, "존재하지 않는 템플릿입니다."));
    }

    private PromptTemplateRevision findRevisionOrThrow(UUID templateId, int version) {
        return revisionRepository.findByTemplateIdAndVersion(templateId, version)
                .orElseThrow(() -> new ApiException(ErrorCode.VALIDATION_ERROR, ErrorCodes.ADMIN_TEMPLATE_VERSION_NOT_FOUND,
                        "존재하지 않는 템플릿 버전입니다: v" + version));
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

    private PromptTemplateRevisionDto toRevisionDto(PromptTemplateRevision revision, boolean includeContent) {
        return new PromptTemplateRevisionDto(
                revision.getId(),
                revision.getTemplateId(),
                revision.getVersion(),
                includeContent ? revision.getContent() : null,
                revision.getCreatedAt());
    }
}
