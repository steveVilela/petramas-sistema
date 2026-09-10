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
import java.time.Duration;

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

    // === MÉTODO PARA CALCULAR LAS HORAS NETAS (DESCONTANDO REFRIGERIO) ===
    public double getHorasTrabajadas() {
        if (horaInicio != null && horaFin != null) {
            long minutosTotales = Duration.between(horaInicio, horaFin).toMinutes();
            double horasBrutas = minutosTotales / 60.0;
            
            // Si la jornada supera las 6 horas (como de 07:30 a 16:30 que son 9h), 
            // le restamos automáticamente 1 hora de refrigerio.
            if (horasBrutas > 6.0) {
                return horasBrutas - 1.0;
            }
            return horasBrutas;
        }
        return 0.0;
    }
}

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @Column(name = "actividad", nullable = false, columnDefinition = "TEXT")
    private String actividad;

}
