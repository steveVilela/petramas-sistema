package com.petramas.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "tb_operario")
public class Operario {

    @Id
    @Column(name = "dni", length = 8)
    private String dni;

    @Column(name = "nombre_apellido", nullable = false, length = 100)
    private String nombreApellido;

    @Column(name = "especialidad", nullable = false, length = 50)
    private String especialidad;

    @Column(name = "clave")
    private String clave;
    
 // --- NUEVO CAMPO DE ESTADO ---
    @Column(name = "estado", length = 20)
    private String estado = "Activo"; // Por defecto, todos nacen como Activos

}
