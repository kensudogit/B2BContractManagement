package com.example.contracts.security;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.contracts.common.ApiException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AppUserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthController(AppUserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
    public record LoginResponse(String accessToken, String username, String displayName, String role) {}
    public record MeResponse(Long id, String username, String displayName, String role) {}

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest body) {
        AppUser user = users.findByUsername(body.username())
                .filter(AppUser::isEnabled)
                .orElseThrow(() -> ApiException.badRequest("ユーザー名またはパスワードが違います"));
        if (!encoder.matches(body.password(), user.getPasswordHash())) {
            throw ApiException.badRequest("ユーザー名またはパスワードが違います");
        }
        return new LoginResponse(jwt.issue(user), user.getUsername(), user.getDisplayName(), user.getRole());
    }

    @GetMapping("/me")
    public MeResponse me() {
        AppUser user = CurrentUser.require();
        return new MeResponse(user.getId(), user.getUsername(), user.getDisplayName(), user.getRole());
    }
}
