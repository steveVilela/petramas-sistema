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

    @ManyToOne
    @JoinColumn(name = "dni_operario", nullable = false)
    private Operario operario;

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

    // Método para descontar refrigerio automáticamente si pasa de 6 horas
    public double getHorasTrabajadas() {
        if (horaInicio != null && horaFin != null) {
            long minutosTotales = Duration.between(horaInicio, horaFin).toMinutes();
            double horasBrutas = minutosTotales / 60.0;
            if (horasBrutas > 5.0) {
                return horasBrutas - 1.0;
            }
            return horasBrutas;
        }
        return 0.0;
    }
}
