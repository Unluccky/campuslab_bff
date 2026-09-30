package com.ms.campuslab_bff.service;

import com.ms.campuslab_bff.service.dto.RecursoResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class CatalogClient {

    private final RestClient catalogRestClient;

    public CatalogClient(RestClient catalogRestClient) {
        this.catalogRestClient = catalogRestClient;
    }

    public List<RecursoResponse> listarRecursos(String bearerToken) {
        return catalogRestClient.get()
                .uri("/api/catalogo")
                .header(HttpHeaders.AUTHORIZATION, bearerToken)
                .retrieve()
                .body(new ParameterizedTypeReference<List<RecursoResponse>>() {});
    }

    public RecursoResponse obtenerRecurso(String bearerToken, Long id) {
        return catalogRestClient.get()
                .uri("/api/catalogo/{id}", id)
                .header(HttpHeaders.AUTHORIZATION, bearerToken)
                .retrieve()
                .body(RecursoResponse.class);
    }
}
