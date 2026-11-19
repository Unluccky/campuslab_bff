package com.ms.campuslab_bff.service;

import com.ms.campuslab_bff.dto.AuthResponseDto;
import com.ms.campuslab_bff.dto.LoginRequestDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminInitiateAuthRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminInitiateAuthResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthFlowType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.NotAuthorizedException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserNotFoundException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ConfirmSignUpRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UsernameExistsException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InvalidPasswordException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CodeMismatchException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ExpiredCodeException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AliasExistsException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import com.ms.campuslab_bff.dto.RegisterRequestDto;
import com.ms.campuslab_bff.dto.ConfirmRegistrationRequestDto;

@Service
public class AuthService {

    private final CognitoIdentityProviderClient cognitoClient;

    @Value("${aws.cognito.client-id}")
    private String clientId;

    @Value("${aws.cognito.user-pool-id}")
    private String userPoolId;

    public AuthService(CognitoIdentityProviderClient cognitoClient) {
        this.cognitoClient = cognitoClient;
    }

    public AuthResponseDto login(LoginRequestDto request) {
        Map<String, String> authParams = Map.of(
                "USERNAME", request.email(),
                "PASSWORD", request.password()
        );

        AdminInitiateAuthRequest authRequest = AdminInitiateAuthRequest.builder()
                .authFlow(AuthFlowType.ADMIN_NO_SRP_AUTH)
                .userPoolId(userPoolId)
                .clientId(clientId)
                .authParameters(authParams)
                .build();

        AdminInitiateAuthResponse response;
        try {
            response = cognitoClient.adminInitiateAuth(authRequest);
        } catch (NotAuthorizedException | UserNotFoundException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas");
        }
        AuthenticationResultType result = response.authenticationResult();
        if (result == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La cuenta requiere completar un desafío de Cognito");
        }

        return new AuthResponseDto(
                result.accessToken(),
                result.idToken(),
                result.refreshToken(),
                result.expiresIn(),
                result.tokenType()
        );
    }

    public String register(RegisterRequestDto request) {
        try {
            SignUpResponse response = cognitoClient.signUp(SignUpRequest.builder()
                    .clientId(clientId)
                    .username(request.email().trim().toLowerCase(java.util.Locale.ROOT))
                    .password(request.password())
                    .userAttributes(AttributeType.builder().name("email").value(request.email().trim()).build())
                    .build());
            if (response.codeDeliveryDetails() == null || response.codeDeliveryDetails().destination() == null) {
                return "Cuenta creada. Ya puedes iniciar sesión.";
            }
            return "Enviamos un código de verificación a " + response.codeDeliveryDetails().destination() + ".";
        } catch (UsernameExistsException | AliasExistsException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una cuenta con ese correo.");
        } catch (InvalidPasswordException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña no cumple la política de seguridad de Cognito.");
        }
    }

    public void confirmRegistration(ConfirmRegistrationRequestDto request) {
        try {
            cognitoClient.confirmSignUp(ConfirmSignUpRequest.builder()
                    .clientId(clientId)
                    .username(request.email().trim().toLowerCase(java.util.Locale.ROOT))
                    .confirmationCode(request.code().trim())
                    .build());
        } catch (CodeMismatchException | ExpiredCodeException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El código no es válido o venció. Solicita otro código.");
        } catch (NotAuthorizedException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La cuenta ya está confirmada o requiere asistencia.");
        }
    }
}
