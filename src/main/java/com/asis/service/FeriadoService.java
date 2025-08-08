package com.asis.service;

import com.asis.model.Feriado;
import com.asis.repository.FeriadoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FeriadoService {

    private final FeriadoRepository feriadoRepo;

    public boolean esFeriado(LocalDate fecha) {
        return feriadoRepo.findAll().stream()
                .anyMatch(f -> f.getDia() == fecha.getDayOfMonth() && f.getMes() == fecha.getMonthValue());
    }

    public List<Feriado> listarFeriados() {
        return feriadoRepo.findAllByOrderByMesAscDiaAsc();
    }

    public void agregarFeriado(Feriado feriado) {
        feriadoRepo.save(feriado);
    }

    public void eliminarFeriado(Long id) {
        feriadoRepo.deleteById(id);
    }
}