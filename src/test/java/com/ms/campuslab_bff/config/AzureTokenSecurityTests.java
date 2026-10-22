package com.ms.campuslab_bff.config;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.*;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class AzureTokenSecurityTests {
    static final String TENANT = "c98282a6-2da3-4791-98c0-dcf952c6454b";
    static final String ISSUER = "https://login.microsoftonline.com/" + TENANT + "/v2.0";
    static final String API = "feba3b3c-7f62-4750-b179-8b2eb402f6ab";
    static final String CLIENT = "1eb98820-862c-45ec-9de2-c37ba699e362";

    @Test void verifiesSignatureAndRejectsWrongAudienceScopeTenantClientAndExpiry() throws Exception {
        var key = new RSAKeyGenerator(2048).generate();
        var decoder = NimbusJwtDecoder.withPublicKey(key.toRSAPublicKey()).build();
        decoder.setJwtValidator(JwtConfig.azureValidator(ISSUER, TENANT, API, CLIENT));
        for (String fault : List.of("valid", "aud", "scp", "tid", "azp", "expired", "iss", "signature")) {
            var claims = new JWTClaimsSet.Builder().issuer(fault.equals("iss") ? "https://untrusted.example" : ISSUER)
                .subject("student").audience(fault.equals("aud") ? CLIENT : API)
                .issueTime(Date.from(Instant.now().minusSeconds(600)))
                .expirationTime(Date.from(Instant.now().plusSeconds(fault.equals("expired") ? -300 : 300)))
                .claim("tid", fault.equals("tid") ? "another-tenant" : TENANT)
                .claim("azp", fault.equals("azp") ? "another-client" : CLIENT)
                .claim("scp", fault.equals("scp") ? "openid profile" : "access-all").build();
            var token = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
            token.sign(new RSASSASigner(fault.equals("signature") ? new RSAKeyGenerator(2048).generate() : key));
            if (fault.equals("valid")) assertEquals("student", decoder.decode(token.serialize()).getSubject());
            else assertThrows(JwtException.class, () -> decoder.decode(token.serialize()), fault);
        }
    }

    @Test void azureCannotUseCognitoGroupsToBecomeAdmin() {
        var converter = new TokenAuthorities().campuslabAuthenticationConverter(TENANT, List.of(), List.of());
        var jwt = Jwt.withTokenValue("test").header("alg", "RS256").issuer(ISSUER)
            .subject("student").claim("cognito:groups", List.of("ROLE_ADMIN")).build();
        var roles = converter.convert(jwt).getAuthorities().stream().map(Object::toString).toList();
        assertTrue(roles.contains("ROLE_ESTUDIANTE"));
        assertFalse(roles.contains("ROLE_ADMIN"));
    }

    @Test void onlyExplicitAzureRoleOrConfiguredObjectIdGrantsAdmin() {
        var converter = new TokenAuthorities().campuslabAuthenticationConverter(TENANT, List.of("approved-oid"), List.of());
        for (boolean assignedRole : List.of(true, false)) {
            var jwt = Jwt.withTokenValue("test").header("alg", "RS256").issuer(ISSUER).subject("admin")
                .claim("oid", assignedRole ? "other-oid" : "approved-oid")
                .claim("roles", assignedRole ? List.of("Admin") : List.of()).build();
            assertTrue(converter.convert(jwt).getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        }
    }
}
