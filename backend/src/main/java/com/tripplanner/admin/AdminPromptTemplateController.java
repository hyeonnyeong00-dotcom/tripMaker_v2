package com.tripplanner.admin;

import com.tripplanner.admin.dto.PromptTemplateDto;
import com.tripplanner.admin.dto.PromptTemplateUpdateRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
}
