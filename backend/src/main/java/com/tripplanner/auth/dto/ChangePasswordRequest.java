package com.tripplanner.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @JsonProperty("current_password") @NotBlank String currentPassword,
        @JsonProperty("new_password") @NotBlank @Size(min = 8, max = 100) String newPassword) {
}
