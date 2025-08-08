package com.asis.controller;

import com.asis.repository.EmpleadoRepository;
import com.asis.service.JustificacionAusenciaService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
@RequestMapping("/justificaciones")
@RequiredArgsConstructor
public class JustificacionAusenciaController {

    private final EmpleadoRepository empleadoRepo;
    private final JustificacionAusenciaService justificacionService;

    @GetMapping("/cargar")
    public String verPantallaJustificaciones(Model model) {
        model.addAttribute("empleados", empleadoRepo.findAll());
        return "asistencias/justificaciones";
    }


    @PostMapping("/guardar")
    public String justificarAusencia(@RequestParam("dni") String dni,
                                     @RequestParam("desde") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                     @RequestParam(value = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                     @RequestParam("descripcion") String descripcion) {
        if (hasta == null) hasta = desde;
        justificacionService.justificarAusencia(dni, desde, hasta, descripcion);
        return "redirect:/justificaciones/cargar";
    }
}
