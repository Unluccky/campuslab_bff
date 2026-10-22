package com.ms.campuslab_bff.config;

import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;
import java.util.*;

/** Only configured issuers are trusted. Parsing the issuer never authenticates a token. */
@Configuration
public class JwtConfig {
    @Bean
    JwtDecoder jwtDecoder(
        @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String cognitoIssuer,
        @Value("${campuslab.jwt.client-ids:6jkhuc0ea0kmj1sbv38103tt6h}") List<String> clients,
        @Value("${campuslab.azure.enabled:false}") boolean azureEnabled,
        @Value("${campuslab.azure.tenant-id:c98282a6-2da3-4791-98c0-dcf952c6454b}") String tenant,
        @Value("${campuslab.azure.audience:feba3b3c-7f62-4750-b179-8b2eb402f6ab}") String audience,
        @Value("${campuslab.azure.client-id:1eb98820-862c-45ec-9de2-c37ba699e362}") String client) {
        var decoders = new HashMap<String, JwtDecoder>();
        var cognito = NimbusJwtDecoder.withJwkSetUri(cognitoIssuer + "/.well-known/jwks.json").build();
        cognito.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
            JwtValidators.createDefaultWithIssuer(cognitoIssuer), jwt ->
                "access".equals(jwt.getClaimAsString("token_use")) && clients.contains(jwt.getClaimAsString("client_id"))
                    ? OAuth2TokenValidatorResult.success() : invalid()));
        decoders.put(cognitoIssuer, cognito);
        if (azureEnabled) {
            if (!tenant.matches("[a-fA-F0-9-]{36}")) throw new IllegalArgumentException("Invalid Azure tenant");
            String issuer = "https://login.microsoftonline.com/" + tenant + "/v2.0";
            var azure = NimbusJwtDecoder.withJwkSetUri("https://login.microsoftonline.com/" + tenant + "/discovery/v2.0/keys").build();
            azure.setJwtValidator(azureValidator(issuer, tenant, audience, client));
            decoders.put(issuer, azure);
        }
        return token -> {
            try {
                var issuer = SignedJWT.parse(token).getJWTClaimsSet().getIssuer();
                var decoder = decoders.get(issuer);
                if (decoder == null) throw new BadJwtException("Untrusted token issuer");
                return decoder.decode(token);
            } catch (java.text.ParseException | IllegalArgumentException e) {
                throw new BadJwtException("Invalid token", e);
            }
        };
    }

    static OAuth2TokenValidator<Jwt> azureValidator(String issuer, String tenant, String audience, String client) {
        return new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer), jwt -> {
            String scope = jwt.getClaimAsString("scp");
            return jwt.getAudience().contains(audience)
                && tenant.equals(jwt.getClaimAsString("tid"))
                && client.equals(jwt.getClaimAsString("azp"))
                && scope != null && Arrays.asList(scope.split(" ")).contains("access-all")
                && jwt.getExpiresAt() != null
                ? OAuth2TokenValidatorResult.success() : invalid();
        });
    }

    private static OAuth2TokenValidatorResult invalid() {
        return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid API audience, client or scope", null));
    }
}
