package com.tripplanner.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateAppSettingRequest(@NotBlank @Size(min = 8, max = 100) String value) {
}
