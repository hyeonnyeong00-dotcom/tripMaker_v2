package com.tripplanner.admin;

import com.tripplanner.admin.dto.AdminUserDto;
import com.tripplanner.auth.User;
import com.tripplanner.auth.UserRepository;
import com.tripplanner.common.ApiException;
import com.tripplanner.common.ErrorCode;
import java.util.List;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserService {

    static final String RESET_PASSWORD_SETTING_KEY = "default_reset_password";

    private final UserRepository userRepository;
    private final AppSettingRepository appSettingRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminUserService(
            UserRepository userRepository,
            AppSettingRepository appSettingRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.appSettingRepository = appSettingRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<AdminUserDto> listUsers(String role) {
        List<User> users = (role == null || role.isBlank())
                ? userRepository.findAllByOrderByCreatedAtAsc()
                : userRepository.findByRoleOrderByCreatedAtAsc(role);
        return users.stream()
                .map(u -> new AdminUserDto(u.getId(), u.getEmail(), u.getRole(), u.getLastLoginAt(), u.getCreatedAt()))
                .toList();
    }

    @Transactional
    public AdminUserDto updateRole(UUID requesterId, UUID targetId, String newRole) {
        if (requesterId.equals(targetId)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "본인 계정의 역할은 변경할 수 없습니다.");
        }
        User target = findOrThrow(targetId);
        if ("admin".equals(target.getRole()) && !"admin".equals(newRole) && userRepository.countByRole("admin") <= 1) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "마지막 관리자 계정의 역할은 변경할 수 없습니다.");
        }
        target.setRole(newRole);
        User saved = userRepository.save(target);
        return new AdminUserDto(saved.getId(), saved.getEmail(), saved.getRole(), saved.getLastLoginAt(), saved.getCreatedAt());
    }

    @Transactional
    public void resetPassword(UUID targetId) {
        User target = findOrThrow(targetId);
        String defaultPassword = appSettingRepository.findById(RESET_PASSWORD_SETTING_KEY)
                .orElseThrow(() -> new ApiException(ErrorCode.STORAGE_ERROR, "비밀번호 초기화 설정이 없습니다."))
                .getValue();
        target.setPasswordHash(passwordEncoder.encode(defaultPassword));
        userRepository.save(target);
    }

    @Transactional
    public void deleteUser(UUID requesterId, UUID targetId) {
        if (requesterId.equals(targetId)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "본인 계정은 삭제할 수 없습니다.");
        }
        User target = findOrThrow(targetId);
        if ("admin".equals(target.getRole()) && userRepository.countByRole("admin") <= 1) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "마지막 관리자 계정은 삭제할 수 없습니다.");
        }
        // trips/itinerary_days/... 는 FK ON DELETE CASCADE로 함께 삭제된다
        userRepository.delete(target);
    }

    private User findOrThrow(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.VALIDATION_ERROR, "존재하지 않는 사용자입니다."));
    }
}
