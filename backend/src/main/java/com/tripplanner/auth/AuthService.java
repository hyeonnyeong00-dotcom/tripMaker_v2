package com.tripplanner.auth;

import com.tripplanner.auth.dto.AuthResponse;
import com.tripplanner.auth.dto.ChangePasswordRequest;
import com.tripplanner.auth.dto.LoginRequest;
import com.tripplanner.auth.dto.MeResponse;
import com.tripplanner.auth.dto.SignupRequest;
import com.tripplanner.common.ApiException;
import com.tripplanner.common.ErrorCode;
import com.tripplanner.common.ErrorCodes;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final String DEFAULT_ROLE = "user";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public MeResponse signup(SignupRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, ErrorCodes.EMAIL_DUPLICATED, "이미 사용 중인 이메일입니다.");
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(DEFAULT_ROLE);
        user.setCreatedAt(OffsetDateTime.now());
        User saved = userRepository.save(user);

        return new MeResponse(saved.getId(), saved.getEmail(), saved.getRole());
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(ErrorCode.AUTH_ERROR, ErrorCodes.LOGIN_FAILED, "이메일 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(ErrorCode.AUTH_ERROR, ErrorCodes.LOGIN_FAILED, "이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        user.setLastLoginAt(OffsetDateTime.now());
        userRepository.save(user);

        String token = jwtService.issueToken(user.getId(), user.getEmail(), user.getRole());
        return new AuthResponse(token, "Bearer", jwtService.expiresInSeconds(), user.getEmail(), user.getRole());
    }

    public MeResponse me(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.AUTH_ERROR, ErrorCodes.USER_NOT_FOUND, "사용자를 찾을 수 없습니다."));
        return new MeResponse(user.getId(), user.getEmail(), user.getRole());
    }

    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.AUTH_ERROR, ErrorCodes.USER_NOT_FOUND, "사용자를 찾을 수 없습니다."));

        // 401은 프론트 인터셉터가 세션을 지우고 로그인으로 보내므로, 현재 비밀번호 불일치는 400으로 응답
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, ErrorCodes.CURRENT_PASSWORD_MISMATCH, "현재 비밀번호가 올바르지 않습니다.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }
}
