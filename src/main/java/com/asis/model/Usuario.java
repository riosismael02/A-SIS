package com.asis.model;

import jakarta.persistence.*;
import lombok.Data;
import com.asis.model.Rol;


@Data
@Entity
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rol rol;

    @Column(nullable = false)
    private boolean activo;

    // Usuario.java
    @OneToOne(mappedBy = "usuario", fetch = FetchType.LAZY)
    private Empleado empleado;
    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] imagen;



    }