package com.petramas.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

import lombok.Data;

@Data
@Entity
@Table(name = "tb_orden_trabajo")
public class OrdenTrabajo {

    @Id
    @Column(name = "id_orden", length = 20)
    private String idOrden;

    @Column(name = "equipo", nullable = false, length = 100)
    private String equipo;

    @Column(name = "descripcion", nullable = false, length = 255)
    private String descripcion;

    
    @Column(name = "fecha_creacion")
    private LocalDate fechaCreacion;
    
    @Column(name = "estado", length = 20)
    private String estado;

    // --- NUEVO CAMPO ---
    @Column(name = "fecha_finalizacion")
    private LocalDate fechaFinalizacion;

}
