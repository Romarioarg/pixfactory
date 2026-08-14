package com.pixfactory.web;

import com.pixfactory.domain.User;
import com.pixfactory.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/auth/login")
    public Map<String, Object> login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request.email(), request.password());
    }

    @PostMapping("/auth/register")
    public Map<String, Object> register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request.name(), request.email(), request.phone(), request.password());
    }

    @GetMapping("/auth/me")
    public Map<String, Object> me(@AuthenticationPrincipal User user) {
        return authService.me(user);
    }

    @PutMapping("/auth/me")
    public Map<String, Object> updateMe(@AuthenticationPrincipal User user, @RequestBody Map<String, Object> body) {
        return authService.updateMe(user, body);
    }

    @PostMapping("/auth/change-password")
    public Map<String, String> changePassword(@AuthenticationPrincipal User user, @RequestBody PasswordChangeRequest request) {
        authService.changePassword(user, request.currentPassword(), request.nextPassword());
        return Map.of("message", "Senha alterada.");
    }

    @PostMapping("/auth/forgot-password")
    public Map<String, Object> forgot(@RequestBody EmailRequest request) {
        return authService.forgotPassword(request.email());
    }

    @PostMapping("/auth/reset-password")
    public Map<String, String> reset(@RequestBody ResetRequest request) {
        authService.resetPassword(request.email(), request.code(), request.nextPassword());
        return Map.of("message", "Senha redefinida.");
    }

    @PutMapping("/auth/plan")
    public Map<String, Object> plan(@AuthenticationPrincipal User user, @RequestBody Map<String, Object> body) {
        return authService.updatePlan(user, String.valueOf(body.getOrDefault("plan", "gratuito")));
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Map<String, Object>> users() {
        return authService.listUsers();
    }

    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> createUser(@RequestBody RegisterRequest request) {
        return authService.createUser(request.name(), request.email(), request.phone(), request.password(), request.role());
    }

    @PutMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> updateUser(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return authService.updateUser(id, body);
    }

    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, String> deleteUser(@PathVariable Long id) {
        authService.deleteUser(id);
        return Map.of("message", "Usuário removido.");
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}
    public record RegisterRequest(@NotBlank String name, @NotBlank @Email String email, String phone, @NotBlank String password, String role) {}
    public record PasswordChangeRequest(@NotBlank String currentPassword, @NotBlank String nextPassword) {}
    public record EmailRequest(@NotBlank @Email String email) {}
    public record ResetRequest(@NotBlank String email, @NotBlank String code, @NotBlank String nextPassword) {}
}
