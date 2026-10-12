package com.example.contracts.security;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final AppUserRepository users;

    public UserController(AppUserRepository users) {
        this.users = users;
    }

    public record UserView(Long id, String username, String displayName, String role) {}

    @GetMapping
    public List<UserView> list() {
        return users.findAll().stream()
                .filter(AppUser::isEnabled)
                .map(u -> new UserView(u.getId(), u.getUsername(), u.getDisplayName(), u.getRole()))
                .toList();
    }
}
