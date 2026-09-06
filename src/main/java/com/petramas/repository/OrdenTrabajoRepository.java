package com.petramas.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.petramas.model.OrdenTrabajo;

public interface OrdenTrabajoRepository extends JpaRepository<OrdenTrabajo, String> {
    
    // Método para traer SOLO las órdenes con un estado específico
    List<OrdenTrabajo> findByEstado(String estado);
}