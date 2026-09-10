package com.petramas.repository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.petramas.model.Operario;
import com.petramas.model.RegistroDiario;

public interface RegistroDiarioRepository extends JpaRepository<RegistroDiario, Integer> {
    
    // Busca todos los reportes de un soldador en un día exacto
    List<RegistroDiario> findByOperarioAndFecha(Operario operario, LocalDate fecha);
    
 // Busca todo el historial de un operario, del más nuevo al más viejo
    List<RegistroDiario> findByOperarioOrderByFechaDesc(Operario operario);
    
 // Trae los registros de un soldador en un rango de fechas (ej. del día 1 al 29)
    List<RegistroDiario> findByOperarioAndFechaBetween(Operario operario, LocalDate inicio, LocalDate fin);

 // Trae todos los registros de todos los operarios en un rango de fechas
    List<RegistroDiario> findByFechaBetween(java.time.LocalDate inicio, java.time.LocalDate fin);

    // Agrega esta línea para que el repositorio reconozca el método anti-duplicados
    boolean existsByOperarioAndFechaAndHoraInicioAndHoraFin(
        Operario operario, LocalDate fecha, LocalTime horaInicio, LocalTime horaFin
    );
} 
