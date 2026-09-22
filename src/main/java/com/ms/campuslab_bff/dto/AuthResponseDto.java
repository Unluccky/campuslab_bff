package com.ms.campuslab_bff.dto;

public record AuthResponseDto(
        String accessToken,
        String idToken,
        String refreshToken,
        Integer expiresIn,
        String tokenType
) {
}