package com.ms.campuslab_bff.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import java.util.*;

@Configuration
public class TokenAuthorities {
    @Bean
    JwtAuthenticationConverter campuslabAuthenticationConverter(
        @Value("${campuslab.azure.tenant-id:c98282a6-2da3-4791-98c0-dcf952c6454b}") String tenant,
        @Value("${campuslab.azure.admin-object-ids:}") List<String> admins,
        @Value("${campuslab.azure.operator-object-ids:}") List<String> operators) {
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Set<String> roles = new HashSet<>();
            if (jwt.getIssuer() != null && jwt.getIssuer().toString().equals("https://login.microsoftonline.com/" + tenant + "/v2.0")) {
                var appRoles = jwt.getClaimAsStringList("roles");
                if (appRoles != null) for (String role : appRoles) {
                    switch (role) {
                        case "Admin" -> roles.add("ROLE_ADMIN");
                        case "Operador", "Tecnico" -> roles.add("ROLE_OPERADOR");
                        case "Estudiante", "Cliente" -> roles.add("ROLE_ESTUDIANTE");
                        case "Auditor" -> roles.add("ROLE_AUDITOR");
                    }
                }
                String oid = jwt.getClaimAsString("oid");
                if (oid != null && admins.contains(oid)) roles.add("ROLE_ADMIN");
                if (oid != null && operators.contains(oid)) roles.add("ROLE_OPERADOR");
                // Academic tenant members can request resources, never administer by default.
                roles.add("ROLE_ESTUDIANTE");
            } else {
                var groups = jwt.getClaimAsStringList("cognito:groups");
                if (groups != null) roles.addAll(groups);
            }
            return roles.stream().map(SimpleGrantedAuthority::new)
                .map(a -> (org.springframework.security.core.GrantedAuthority) a).toList();
        });
        return converter;
    }
}
