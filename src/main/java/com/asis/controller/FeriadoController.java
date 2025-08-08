package com.asis.controller;

import com.asis.model.Feriado;
import com.asis.service.FeriadoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/feriados")
public class FeriadoController {

    private final FeriadoService feriadoService;

    // Mostrar lista de feriados
    @GetMapping
    public String listarFeriados(Model model) {
        model.addAttribute("feriados", feriadoService.listarFeriados());
        model.addAttribute("feriadoNuevo", new Feriado());
        return "feriados/lista";
    }

    // Guardar un nuevo feriado
    @PostMapping("/guardar")
    public String guardarFeriado(@ModelAttribute("feriadoNuevo") Feriado feriado) {
        feriadoService.agregarFeriado(feriado);
        return "redirect:/feriados";
    }

    // Eliminar feriado por ID
    @PostMapping("/eliminar/{id}")
    public String eliminarFeriado(@PathVariable Long id) {
        feriadoService.eliminarFeriado(id);
        return "redirect:/feriados";
    }
}