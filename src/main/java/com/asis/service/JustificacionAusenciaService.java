package com.asis.service;

import com.asis.model.Empleado;
import com.asis.model.JustificacionAusencia;
import com.asis.model.RegistroAsistencia;
import com.asis.repository.EmpleadoRepository;
import com.asis.repository.JustificacionAusenciaRepository;
import com.asis.repository.RegistroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JustificacionAusenciaService {

    private final EmpleadoRepository empleadoRepo;
    private final RegistroRepository registroRepo;
    private final JustificacionAusenciaRepository justificacionRepo;
    private final FeriadoService feriadoService;

    @Transactional
    public void justificarAusencia(String dni, LocalDate desde, LocalDate hasta, String descripcion) {
        Empleado empleado = empleadoRepo.findByDni(dni)
                .orElseThrow(() -> new IllegalArgumentException("Empleado no encontrado con DNI: " + dni));

        List<RegistroAsistencia> registros = new ArrayList<>();
        LocalDate actual = desde;

        while (!actual.isAfter(hasta)) {
            boolean esFinde = actual.getDayOfWeek() == DayOfWeek.SATURDAY || actual.getDayOfWeek() == DayOfWeek.SUNDAY;
            boolean esFeriado = feriadoService.esFeriado(actual);
            boolean esLaboral = !esFinde && !esFeriado;

            boolean tieneMarcas = registroRepo.existsByEmpleadoAndFecha(empleado, actual);

            if (esLaboral && !tieneMarcas) {
                RegistroAsistencia entrada = new RegistroAsistencia();
                entrada.setEmpleado(empleado);
                entrada.setFecha(actual);
                entrada.setHora(empleado.getHoraEntrada());
                entrada.setOrdenDia(1);
                entrada.setTipoHora(RegistroAsistencia.TipoHora.NORMAL);

                RegistroAsistencia salida = new RegistroAsistencia();
                salida.setEmpleado(empleado);
                salida.setFecha(actual);
                salida.setHora(empleado.getHoraSalida());
                salida.setOrdenDia(2);
                salida.setTipoHora(RegistroAsistencia.TipoHora.NORMAL);

                registros.add(entrada);
                registros.add(salida);
            }

            actual = actual.plusDays(1);
        }

        JustificacionAusencia justificacion = JustificacionAusencia.builder()
                .empleado(empleado)
                .desde(desde)
                .hasta(hasta)
                .descripcion(descripcion)
                .registrosGenerados(registros)
                .build();

        // Asignar la justificación a cada registro generado
        registros.forEach(r -> r.setJustificacion(justificacion));

        justificacionRepo.save(justificacion);
    }

    public boolean existeJustificacionPara(Empleado empleado, LocalDate fecha) {
        return justificacionRepo.existsByEmpleadoAndDesdeLessThanEqualAndHastaGreaterThanEqual(empleado, fecha, fecha);
    }


}