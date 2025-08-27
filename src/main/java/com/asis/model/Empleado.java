package com.asis.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Empleado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String dni;
    private String legajo;
    private String nombre;
    private String apellido;

    private LocalTime horaEntrada;   // Ej: 07:00
    private LocalTime horaSalida;    // Ej: 13:00

    // Turno secundario (opcional)
    private LocalTime horaEntrada2;
    private LocalTime horaSalida2;

    // Empleado.java
    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;
    private Integer flexMinutos;     // Ej: 5

    @OneToMany(mappedBy = "empleado", cascade = CascadeType.ALL)
    private List<RegistroAsistencia> registros;

    @ManyToOne
    private Area area;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_contrato")
    private TipoContrato tipoContrato;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol")
    private Rol rol;


    public enum TipoContrato {
        SERVICIO("Contrato de servicio"),
        HORA("Contrato prestacional"),
        OBRA("Contrato de obra"),
        PERMANENTE("Permanente");

        private final String label;

        TipoContrato(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

}
