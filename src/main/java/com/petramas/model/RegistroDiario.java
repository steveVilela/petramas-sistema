package com.petramas.model;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalTime;


import lombok.Data;

@Data
@Entity
@Table(name = "tb_registro_diario")
public class RegistroDiario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_registro")
    private Integer idRegistro;

    // Relación Muchos a Uno con Operario (Varios reportes pertenecen a un operario)
    @ManyToOne
    @JoinColumn(name = "dni_operario", nullable = false)
    private Operario operario;

    // Relación Muchos a Uno con OrdenTrabajo (Varios reportes pueden ser de la misma orden)
    @ManyToOne
    @JoinColumn(name = "id_orden", nullable = false)
    private OrdenTrabajo ordenTrabajo;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @Column(name = "actividad", nullable = false, columnDefinition = "TEXT")
    private String actividad;

}