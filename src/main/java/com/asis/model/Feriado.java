package com.asis.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Entity
@RequiredArgsConstructor
public class Feriado {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private int dia;       // Ej: 25
    private int mes;       // Ej: 5
    private String descripcion;

    public Feriado(Long id, int dia, int mes, String descripcion) {
        this.id = id;
        this.dia = dia;
        this.mes = mes;
        this.descripcion = descripcion;
    }


}


