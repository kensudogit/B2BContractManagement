package com.example.contracts.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.example.contracts.common.ApiException;

public final class CurrentUser {
    private CurrentUser() {}

    public static AppUser require() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AppUser user)) {
            throw new ApiException(org.springframework.http.HttpStatus.UNAUTHORIZED, "未ログインです");
        }
        return user;
    }
}
