package com.tripplanner.admin;

import com.tripplanner.admin.dto.PromptTemplateDto;
import com.tripplanner.admin.dto.PromptTemplateRevisionDto;
import com.tripplanner.admin.dto.PromptTemplateUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin - Prompt Templates", description = "프롬프트 템플릿 조회/수정/버전 관리 (admin 전용)")
@RestController
@RequestMapping("/api/admin/prompt-templates")
public class AdminPromptTemplateController {

    private final AdminPromptTemplateService adminPromptTemplateService;

    public AdminPromptTemplateController(AdminPromptTemplateService adminPromptTemplateService) {
        this.adminPromptTemplateService = adminPromptTemplateService;
    }

    @GetMapping
    public List<PromptTemplateDto> list() {
        return adminPromptTemplateService.listAll();
    }

    @GetMapping("/{id}")
    public PromptTemplateDto get(@PathVariable UUID id) {
        return adminPromptTemplateService.getById(id);
    }

    @PutMapping("/{id}")
    public PromptTemplateDto update(@PathVariable UUID id, @Valid @RequestBody PromptTemplateUpdateRequest request) {
        return adminPromptTemplateService.update(id, request.content());
    }

    @Operation(summary = "버전 이력 목록", description = "저장 직전 스냅샷 목록(번호·수정일). 본문은 포함하지 않는다.")
    @GetMapping("/{id}/revisions")
    public List<PromptTemplateRevisionDto> revisions(@PathVariable UUID id) {
        return adminPromptTemplateService.listRevisions(id);
    }

    @Operation(summary = "특정 버전 본문", description = "현재 버전과 나란히 비교하기 위한 과거 버전 본문.")
    @GetMapping("/{id}/revisions/{version}")
    public PromptTemplateRevisionDto revision(@PathVariable UUID id, @PathVariable int version) {
        return adminPromptTemplateService.getRevision(id, version);
    }

    @Operation(summary = "해당 버전으로 롤백", description = "과거 버전 내용으로 새 버전을 만든다(버전 번호는 항상 증가).")
    @PostMapping("/{id}/rollback/{version}")
    public PromptTemplateDto rollback(@PathVariable UUID id, @PathVariable int version) {
        return adminPromptTemplateService.rollback(id, version);
    }
}
