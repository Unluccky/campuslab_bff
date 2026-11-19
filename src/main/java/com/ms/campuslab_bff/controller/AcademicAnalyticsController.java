package com.ms.campuslab_bff.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/** Authenticated BFF proxy from API Gateway to the private reporting service. */
@RestController
public class AcademicAnalyticsController {
    private static final String REPORTS_PATH = "/api/bff/academic/reportes";
    private static final String TOPICS_PATH = "/api/bff/academic/kafka/topics";

    private final RestClient reports;

    public AcademicAnalyticsController(
            @Value("${campuslab.reportes.base-url:http://localhost:8085}") String baseUrl) {
        var factory = new org.springframework.http.client.JdkClientHttpRequestFactory(
                java.net.http.HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(3)).build());
        factory.setReadTimeout(java.time.Duration.ofSeconds(12));
        this.reports = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    @RequestMapping(value = {REPORTS_PATH + "/kpi", REPORTS_PATH + "/mis-reservas"}, method = RequestMethod.GET)
    public ResponseEntity<byte[]> report(
            HttpServletRequest request,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        String endpoint = request.getRequestURI().substring(REPORTS_PATH.length());
        return forward(HttpMethod.GET, "/api/reportes" + endpoint, request.getQueryString(), authorization, null);
    }

    @RequestMapping(value = {TOPICS_PATH, TOPICS_PATH + "/{name}", TOPICS_PATH + "/{name}/retention"},
            method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PATCH, RequestMethod.DELETE})
    public ResponseEntity<byte[]> topics(
            HttpServletRequest request,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestBody(required = false) byte[] body) {
        String suffix = request.getRequestURI().substring(TOPICS_PATH.length());
        return forward(HttpMethod.valueOf(request.getMethod()), "/api/admin/kafka/topics" + suffix,
                request.getQueryString(), authorization, body);
    }

    private ResponseEntity<byte[]> forward(HttpMethod method, String path, String query, String authorization, byte[] body) {
        URI target = URI.create(path + (query == null || query.isBlank() ? "" : "?" + query));
        try {
            var call = reports.method(method).uri(target).header(HttpHeaders.AUTHORIZATION, authorization);
            if (body != null && body.length > 0) call.contentType(MediaType.APPLICATION_JSON).body(body);
            var response = call.retrieve().toEntity(byte[].class);
            HttpHeaders headers = new HttpHeaders();
            MediaType contentType = response.getHeaders().getContentType();
            if (contentType != null) headers.setContentType(contentType);
            return new ResponseEntity<>(response.getBody(), headers, response.getStatusCode());
        } catch (RestClientResponseException error) {
            byte[] payload = error.getResponseBodyAsByteArray();
            return ResponseEntity.status(error.getStatusCode()).contentType(MediaType.APPLICATION_JSON).body(
                    payload.length == 0 ? error.getStatusText().getBytes(StandardCharsets.UTF_8) : payload);
        } catch (RuntimeException error) {
            return ResponseEntity.status(503).contentType(MediaType.APPLICATION_JSON)
                    .body("{\"mensaje\":\"Servicio de reportería no disponible\"}".getBytes(StandardCharsets.UTF_8));
        }
    }
}
