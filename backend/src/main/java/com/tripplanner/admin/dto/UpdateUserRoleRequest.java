package com.tripplanner.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateUserRoleRequest(
        @NotBlank @Pattern(regexp = "user|admin", message = "role은 user 또는 admin이어야 합니다") String role) {
}
