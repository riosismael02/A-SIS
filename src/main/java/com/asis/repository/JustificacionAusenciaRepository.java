package com.asis.repository;


import com.asis.model.Empleado;
import com.asis.model.JustificacionAusencia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface JustificacionAusenciaRepository extends JpaRepository<JustificacionAusencia, Long> {

    List<JustificacionAusencia> findByEmpleado(Empleado empleado);

    List<JustificacionAusencia> findByEmpleadoDniAndDesdeLessThanEqualAndHastaGreaterThanEqual(
            String dni, LocalDate fechaDesde, LocalDate fechaHasta);

    Optional<JustificacionAusencia> findById(Long id);

    // Este es el que se usa para verificar si una fecha puntual está dentro de una justificación
    boolean existsByEmpleadoAndDesdeLessThanEqualAndHastaGreaterThanEqual(
            Empleado empleado, LocalDate fecha, LocalDate fecha2);
}