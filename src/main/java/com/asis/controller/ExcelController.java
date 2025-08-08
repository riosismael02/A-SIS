//package com.asis.controller;
//
//
//
//import com.asis.model.RegistroAsistencia;
//import com.asis.service.ExcelService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.format.annotation.DateTimeFormat;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.time.LocalDate;
//import java.time.format.DateTimeFormatter;
//import java.util.List;
//
//@Controller
//@RequiredArgsConstructor
//public class ExcelController {
//
//    private final ExcelService excelService;
//
//    @GetMapping("/upload")
//    public String formularioCarga() {
//        return "upload";
//    }
//    private LocalDate parseFechaDesdeString(String fechaHora) {
//        try {
//            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
//            return LocalDate.parse(fechaHora, formatter);
//        } catch (Exception e) {
//            return null;
//        }
//    }
//
//    @PostMapping("/upload")
//    public String procesarArchivo(
//            @RequestParam("archivo") MultipartFile archivo,
//            @RequestParam("desde") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
//            @RequestParam("hasta") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
//            Model model) {
//
//        List<RegistroAsistencia> registros = excelService.leerExcel(archivo);
//
//        // Filtrar por rango de fechas
//        List<RegistroAsistencia> filtrados = registros.stream()
//                .filter(r -> {
//                    LocalDate fecha = parseFechaDesdeString(r.getFechaHora());
//                    return (fecha != null && !fecha.isBefore(desde) && !fecha.isAfter(hasta));
//                })
//                .toList();
//
//        model.addAttribute("registros", filtrados);
//        return "preview";
//    }
//    }