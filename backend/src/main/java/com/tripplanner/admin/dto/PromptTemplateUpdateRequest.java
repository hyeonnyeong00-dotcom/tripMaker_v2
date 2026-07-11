package com.tripplanner.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record PromptTemplateUpdateRequest(@NotBlank String content) {
}
