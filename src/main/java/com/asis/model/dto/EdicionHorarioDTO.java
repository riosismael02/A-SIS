package com.asis.model.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class EdicionHorarioDTO {
    private String dni;
    private LocalDate fecha;

    // Primer horario
    private LocalTime horaEntrada1;
    private LocalTime horaSalida1;

    // Segundo horario
    private LocalTime horaEntrada2;
    private LocalTime horaSalida2;

    private String motivoCambio;

    // Getters y setters
}