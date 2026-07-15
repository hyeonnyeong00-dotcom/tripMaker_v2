package com.tripplanner.admin;

import com.tripplanner.admin.dto.AdminUserDto;
import com.tripplanner.admin.dto.UpdateUserRoleRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin - Users", description = "사용자 목록/역할 변경/비밀번호 초기화/삭제 (admin 전용)")
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public List<AdminUserDto> list(@RequestParam(required = false) String role) {
        return adminUserService.listUsers(role);
    }

    @PatchMapping("/{userId}/role")
    public AdminUserDto updateRole(
            Authentication authentication,
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserRoleRequest request) {
        UUID requesterId = UUID.fromString(authentication.getName());
        return adminUserService.updateRole(requesterId, userId, request.role());
    }

    @PostMapping("/{userId}/reset-password")
    public ResponseEntity<Void> resetPassword(@PathVariable UUID userId) {
        adminUserService.resetPassword(userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> delete(Authentication authentication, @PathVariable UUID userId) {
        UUID requesterId = UUID.fromString(authentication.getName());
        adminUserService.deleteUser(requesterId, userId);
        return ResponseEntity.noContent().build();
    }
}
