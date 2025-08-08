package com.asis.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResumenAsistenciasDTO {
    private String dni;
    private String nombre;
    private String apellido;
    private double totalHoras;
    private double totalNormales;
    private double totalExtras;
    private double totalFinde;
    private long totalAusencias;
    private long totalLlegadasTarde;
}

