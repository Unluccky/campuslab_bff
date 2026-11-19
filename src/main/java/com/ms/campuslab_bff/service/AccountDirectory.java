package com.ms.campuslab_bff.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserType;

@Service
public class AccountDirectory {
    private static final Logger LOG = LoggerFactory.getLogger(AccountDirectory.class);
    private static final String COGNITO = "Amazon Cognito";
    private static final String ENTRA = "Microsoft Entra ID";
    private static final URI GRAPH_USERS = URI.create(
        "https://graph.microsoft.com/v1.0/users?$select=id,displayName,mail,userPrincipalName,accountEnabled");

    private final CognitoIdentityProviderClient cognito;
    private final RestClient graph;
    private final String pool;
    private final String tenant;
    private final String clientId;
    private final String clientSecret;

    public AccountDirectory(CognitoIdentityProviderClient cognito,
            @Value("${aws.cognito.user-pool-id}") String pool,
            @Value("${campuslab.azure.tenant-id:c98282a6-2da3-4791-98c0-dcf952c6454b}") String tenant,
            @Value("${CAMPUSLAB_ENTRA_DIRECTORY_CLIENT_ID:}") String clientId,
            @Value("${CAMPUSLAB_ENTRA_DIRECTORY_CLIENT_SECRET:}") String clientSecret) {
        this.cognito = cognito;
        this.pool = pool;
        this.tenant = tenant;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        var factory = new JdkClientHttpRequestFactory(
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build());
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.graph = RestClient.builder().requestFactory(factory).build();
    }

    public Result list() {
        List<Account> accounts = new ArrayList<>();
        List<String> unavailable = new ArrayList<>();
        try {
            accounts.addAll(cognito.listUsersPaginator(request -> request.userPoolId(pool).limit(60))
                .users().stream().map(AccountDirectory::cognitoAccount).toList());
        } catch (SdkException e) {
            LOG.warn("No se pudo consultar el directorio Cognito ({})", e.getClass().getSimpleName());
            unavailable.add(COGNITO);
        }
        if (clientId.isBlank() || clientSecret.isBlank()) {
            unavailable.add(ENTRA);
        } else {
            try {
                accounts.addAll(entraAccounts());
            } catch (RestClientException e) {
                LOG.warn("No se pudo consultar el directorio Entra ({})", e.getClass().getSimpleName());
                unavailable.add(ENTRA);
            }
        }
        accounts.sort(Comparator.comparing(Account::nombre, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(Account::proveedor).thenComparing(Account::id));
        return new Result(accounts, unavailable);
    }

    private static Account cognitoAccount(UserType user) {
        var attributes = new java.util.HashMap<String, String>();
        user.attributes().forEach(attribute -> attributes.put(attribute.name(), attribute.value()));
        return new Account(attributes.getOrDefault("sub", user.username()),
            attributes.getOrDefault("name", user.username()), attributes.getOrDefault("email", ""),
            COGNITO, user.enabled());
    }

    private List<Account> entraAccounts() {
        var form = new LinkedMultiValueMap<String, String>();
        form.setAll(Map.of("grant_type", "client_credentials", "client_id", clientId,
            "client_secret", clientSecret, "scope", "https://graph.microsoft.com/.default"));
        var token = graph.post()
            .uri("https://login.microsoftonline.com/{tenant}/oauth2/v2.0/token", tenant)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form)
            .retrieve().body(GraphToken.class);
        if (token == null || token.accessToken() == null) {
            throw new RestClientException("Respuesta de token incompleta");
        }

        List<Account> accounts = new ArrayList<>();
        URI next = GRAPH_USERS;
        while (next != null) {
            var page = graph.get().uri(next).headers(headers -> headers.setBearerAuth(token.accessToken()))
                .retrieve().body(GraphPage.class);
            if (page == null || page.value() == null) {
                throw new RestClientException("Respuesta de directorio incompleta");
            }
            for (GraphUser user : page.value()) {
                accounts.add(new Account(user.id(), user.displayName() == null ? user.id() : user.displayName(),
                    user.mail() == null ? user.userPrincipalName() : user.mail(), ENTRA, user.accountEnabled()));
            }
            next = graphNextPage(page.nextLink());
        }
        return accounts;
    }

    private static URI graphNextPage(String link) {
        if (link == null) return null;
        try {
            URI next = URI.create(link);
            if ("https".equals(next.getScheme()) && "graph.microsoft.com".equals(next.getHost())
                    && next.getPort() == -1 && "/v1.0/users".equals(next.getPath())) {
                return next;
            }
        } catch (IllegalArgumentException e) {
            throw new RestClientException("Enlace de paginación inválido");
        }
        throw new RestClientException("Destino de paginación inválido");
    }

    public record Result(List<Account> cuentas, List<String> proveedoresNoDisponibles) {}
    public record Account(String id, String nombre, String email, String proveedor, Boolean habilitada) {}
    private record GraphToken(@JsonProperty("access_token") String accessToken) {}
    private record GraphPage(List<GraphUser> value, @JsonProperty("@odata.nextLink") String nextLink) {}
    private record GraphUser(String id, String displayName, String mail, String userPrincipalName,
                             Boolean accountEnabled) {}
}
