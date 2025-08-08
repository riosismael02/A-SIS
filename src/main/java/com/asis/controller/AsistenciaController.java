package com.asis.controller;


import com.asis.model.LogCargaAsistencia;
import com.asis.model.dto.CargaAsistenciaDTO;
import com.asis.model.dto.EdicionHorarioDTO;
import com.asis.model.dto.ResumenAsistenciasDTO;
import com.asis.repository.EmpleadoRepository;
import com.asis.repository.LogCargaAsistenciaRepository;
import com.asis.repository.RegistroRepository;
import com.asis.service.ExcelService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/asistencias")
public class AsistenciaController {

    private final EmpleadoRepository empleadoRepo;
    private final RegistroRepository registroRepo;
    private final LogCargaAsistenciaRepository cargaAsistRepo;
    private final ExcelService excelService;

    @GetMapping("/cargar")
    public String mostrarFormularioCarga(Model model) {
        LocalDate hoy = LocalDate.now();

        // 21 del mes anterior
//        LocalDate desde = hoy.minusMonths(1).withDayOfMonth(21);

        // 20 del mes actual
        LocalDate desde = hoy.minusMonths(1).withDayOfMonth(22);
        LocalDate hasta = hoy.minusMonths(1).withDayOfMonth(29);

        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);

        return "asistencias/carga";
    }


    @PostMapping("/procesar")
    public String procesarArchivo(@ModelAttribute CargaAsistenciaDTO form, RedirectAttributes redirectAttrs) {
        excelService.cargarYGuardarAsistencias(form);  // Ya guarda todo, genera descripción y log

        String descripcion = excelService.generarDescripcion(form.getDesde(), form.getHasta());

        redirectAttrs.addFlashAttribute("mensaje",
                "Archivo procesado correctamente. Total: registros guardados.");

        // Redirigir al resumen con los datos de rango y descripción
        return "redirect:/asistencias/logs";
    }


    @GetMapping("/logs")
    public String listarLogs(Model model) {
        List<LogCargaAsistencia> logs = cargaAsistRepo
                .findAll(Sort.by(Sort.Direction.DESC, "desde"))
                .stream()
                .limit(12)
                .toList(); // Mostramos solo los últimos 12 logs

        model.addAttribute("logs", logs);
        return "asistencias/lista-logs";
    }

    @GetMapping("/resumen-por-log/{logId}")
    public String mostrarResumenTotalesPorLog(@PathVariable Long logId, Model model) {
        LogCargaAsistencia log = cargaAsistRepo.findById(logId).orElseThrow(() -> new RuntimeException("Log no encontrado"));

        List<ResumenAsistenciasDTO> resumen = excelService.generarResumenTotalesPorLog(logId);

        model.addAttribute("log", log);
        model.addAttribute("resumen", resumen);

        return "asistencias/resumen-log";
    }

    @PostMapping("/logs/eliminar/{logId}")
    public String eliminarLog(@PathVariable Long logId, RedirectAttributes redirectAttrs) {
        cargaAsistRepo.findById(logId).ifPresentOrElse(log -> {
            // Eliminar registros de asistencia en el rango
            registroRepo.eliminarPorRangoFechas(log.getDesde(), log.getHasta());

            // Eliminar el log
            cargaAsistRepo.delete(log);

            redirectAttrs.addFlashAttribute("mensaje", "Log y registros de asistencia eliminados correctamente.");
        }, () -> {
            redirectAttrs.addFlashAttribute("error", "No se encontró el log con ID " + logId);
        });

        return "redirect:/asistencias/logs";
    }


    @GetMapping("/detalle")
    public String detalleAsistencia(
            @RequestParam(name = "dni", required = false) String dni,
            @RequestParam(name = "desde", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(name = "hasta", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            Model model) {

        // Para cargar la lista de empleados en el select (puedes ajustar esto)
        model.addAttribute("empleados", empleadoRepo.findAll());

        if (dni != null && desde != null && hasta != null) {
            Map<String, Object> detalleModel = excelService.generarDetalleEmpleadoView(dni, desde, hasta);

            // Agregamos todo el resultado al modelo de la vista
            model.addAllAttributes(detalleModel);

            // También agregamos la variable para que el select tenga seleccionado el dni correcto
            model.addAttribute("dniSeleccionado", dni);
        }

        // Devuelve el nombre del template Thymeleaf (por ej. "detalle-asistencia.html")
        return "asistencias/detalle-asistencias";
    }

    @GetMapping("/editar-horario")
    public String mostrarFormularioEdicion(
            @RequestParam String dni,
            @RequestParam LocalDate fecha,
            Model model) {

        // Obtenemos el DTO con ambos horarios (principal y secundario)
        EdicionHorarioDTO dto = excelService.prepararEdicionHorario(dni, fecha);

        // Verificamos si el empleado tiene segundo horario configurado
        boolean tieneSegundoHorario = dto.getHoraEntrada2() != null && dto.getHoraSalida2() != null;

        // Agregamos atributos al modelo
        model.addAttribute("edicionDTO", dto);
        model.addAttribute("tieneSegundoHorario", tieneSegundoHorario);

        return "asistencias/editar-horario";
    }

    @PostMapping("/guardar-horario")
    public String guardarHorarioEditado(
            @ModelAttribute EdicionHorarioDTO edicionDTO,
            RedirectAttributes redirectAttributes) {

        try {
            excelService.editarHorariosDia(edicionDTO);
            redirectAttributes.addFlashAttribute("success", "Horarios actualizados correctamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al actualizar horarios: " + e.getMessage());
        }

        // Redirecciona de vuelta al formulario de edición con los mismos parámetros
        return "redirect:/asistencias/editar-horario?dni=" + edicionDTO.getDni() +
                "&fecha=" + edicionDTO.getFecha().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }
}





