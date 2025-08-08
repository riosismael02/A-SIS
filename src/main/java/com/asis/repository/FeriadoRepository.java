package com.asis.repository;

import com.asis.model.Feriado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeriadoRepository extends JpaRepository<Feriado, Long> {

    List<Feriado> findAll();

    List<Feriado> findAllByOrderByMesAscDiaAsc();


}

