package com.asis.model.dto;

import com.asis.model.Empleado;
import com.asis.model.Usuario;
import lombok.Data;

@Data
public class UsuarioEmpleadoDTO {
    private Usuario usuario;
    private Empleado empleado;

    // Getters y Setters
    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public Empleado getEmpleado() {
        return empleado;
    }

    public void setEmpleado(Empleado empleado) {
        this.empleado = empleado;
    }
}
