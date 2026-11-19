package com.ms.campuslab_bff.controller;

import com.ms.campuslab_bff.dto.AuthResponseDto;
import com.ms.campuslab_bff.dto.LoginRequestDto;
import com.ms.campuslab_bff.dto.RegisterRequestDto;
import com.ms.campuslab_bff.dto.ConfirmRegistrationRequestDto;
import com.ms.campuslab_bff.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/bff/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register")
    public ResponseEntity<MessageResponse> register(@Valid @RequestBody RegisterRequestDto request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new MessageResponse(authService.register(request)));
    }

    @PostMapping("/confirm")
    public ResponseEntity<MessageResponse> confirm(@Valid @RequestBody ConfirmRegistrationRequestDto request) {
        authService.confirmRegistration(request);
        return ResponseEntity.ok(new MessageResponse("Correo confirmado. Ya puedes ingresar con tu cuenta."));
    }

    public record MessageResponse(String message) {}
}
