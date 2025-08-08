package com.asis.model.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@Data
public class CargaAsistenciaDTO {
    private MultipartFile archivo;
    private LocalDate desde;
    private LocalDate hasta;
    private String descripcion; // Se autogenerará como "junio de 2025"

}

