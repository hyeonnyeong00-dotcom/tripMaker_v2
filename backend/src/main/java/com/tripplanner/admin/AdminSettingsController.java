package com.tripplanner.admin;

import com.tripplanner.admin.dto.AppSettingDto;
import com.tripplanner.admin.dto.UpdateAppSettingRequest;
import com.tripplanner.common.ApiException;
import com.tripplanner.common.ErrorCode;
import com.tripplanner.common.ErrorCodes;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin - Settings", description = "비밀번호 초기화 기본값 조회/수정 (admin 전용)")
@RestController
@RequestMapping("/api/admin/settings")
public class AdminSettingsController {

    private final AppSettingRepository appSettingRepository;

    public AdminSettingsController(AppSettingRepository appSettingRepository) {
        this.appSettingRepository = appSettingRepository;
    }

    @GetMapping("/reset-password")
    public AppSettingDto getResetPassword() {
        AppSetting setting = findOrThrow();
        return new AppSettingDto(setting.getValue(), setting.getUpdatedAt());
    }

    @PutMapping("/reset-password")
    public AppSettingDto updateResetPassword(@Valid @RequestBody UpdateAppSettingRequest request) {
        AppSetting setting = findOrThrow();
        setting.setValue(request.value());
        setting.setUpdatedAt(OffsetDateTime.now());
        AppSetting saved = appSettingRepository.save(setting);
        return new AppSettingDto(saved.getValue(), saved.getUpdatedAt());
    }

    private AppSetting findOrThrow() {
        return appSettingRepository.findById(AdminUserService.RESET_PASSWORD_SETTING_KEY)
                .orElseThrow(() -> new ApiException(ErrorCode.STORAGE_ERROR, ErrorCodes.APP_SETTING_MISSING, "비밀번호 초기화 설정이 없습니다."));
    }
}
