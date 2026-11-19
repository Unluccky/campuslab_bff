package com.ms.campuslab_bff.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequestDto(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 8, max = 256) String password) {}
