package com.students.crud_students.dto;

import java.util.List;

public record AuthResponseDTO(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        Long userId,
        String email,
        Long studentId,
        List<String> roles
) {
    public AuthResponseDTO(
            String accessToken,
            String refreshToken,
            long expiresIn,
            Long userId,
            String email,
            Long studentId,
            List<String> roles
    ) {
        this(accessToken, refreshToken, "Bearer", expiresIn, userId, email, studentId, roles);
    }
}