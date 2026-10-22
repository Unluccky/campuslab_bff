package com.ms.campuslab_bff.controller;

import java.util.Map;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;

@RestController
public class AcademicAccountController {
    @Value("${campuslab.azure.tenant-id:c98282a6-2da3-4791-98c0-dcf952c6454b}")
    private String tenant;

    @GetMapping("/api/bff/academic/me")
    public Map<String, Object> me(JwtAuthenticationToken authentication) {
        var jwt = authentication.getToken();
        if (!jwt.getIssuer().toString().equals("https://login.microsoftonline.com/" + tenant + "/v2.0")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Se requiere identidad académica");
        }
        return Map.of("subject", jwt.getSubject(), "roles", authentication.getAuthorities().stream()
            .map(a -> a.getAuthority()).sorted().toList(), "provider", "Microsoft Entra ID");
    }
}
