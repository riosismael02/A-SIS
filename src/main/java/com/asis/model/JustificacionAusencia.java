package com.asis.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JustificacionAusencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Empleado empleado;

    private LocalDate desde;

    private LocalDate hasta;

    @Column(length = 500)
    private String descripcion;

    @OneToMany(mappedBy = "justificacion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RegistroAsistencia> registrosGenerados;
}
