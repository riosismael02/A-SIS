package com.asis.model.dto;


import com.asis.model.Empleado;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmpleadoDTO {
    public Long id;
    public String nombre;
    public String apellido;
    public String dni;
    public String horaEntrada;
    public String horaSalida;
    public Integer flexMinutos;
    public Long areaId;
    public String tipoContrato;


    public EmpleadoDTO(Empleado e) {
        this.id = e.getId();
        this.nombre = e.getNombre();
        this.apellido = e.getApellido();
        this.dni = e.getDni();
        this.horaEntrada = String.valueOf(e.getHoraEntrada());
        this.horaSalida = String.valueOf(e.getHoraSalida());
        this.flexMinutos = e.getFlexMinutos();
        this.areaId = (e.getArea() != null) ? e.getArea().getId() : null;
        this.tipoContrato = (e.getTipoContrato() != null) ? e.getTipoContrato().name() : null;
    }
}

