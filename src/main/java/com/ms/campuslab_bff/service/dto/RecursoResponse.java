package com.ms.campuslab_bff.service.dto;

import java.time.LocalDateTime;

public record RecursoResponse(
        Long id,
        String nombre,
        String tipo,
        String descripcion,
        String laboratorio,
        Integer stockTotal,
        Integer stockDisponible,
        Boolean activo,
        LocalDateTime creadoEn
) {
}
