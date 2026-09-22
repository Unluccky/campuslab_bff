package com.ms.campuslab_bff.controller;

import com.ms.campuslab_bff.service.CatalogClient;
import com.ms.campuslab_bff.service.dto.RecursoResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/bff/catalogo")
public class BffCatalogController {

    private final CatalogClient catalogClient;

    public BffCatalogController(CatalogClient catalogClient) {
        this.catalogClient = catalogClient;
    }

    @GetMapping
    public ResponseEntity<List<RecursoResponse>> listar(
            @RequestHeader("Authorization") String authorizationHeader) {
        return ResponseEntity.ok(catalogClient.listarRecursos(authorizationHeader));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecursoResponse> obtener(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long id) {
        return ResponseEntity.ok(catalogClient.obtenerRecurso(authorizationHeader, id));
    }
}