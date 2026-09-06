package com.petramas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.petramas.model.Operario;

// Recibe la entidad (Operario) y el tipo de dato de su Llave Primaria (String para el DNI)
public interface OperarioRepository extends JpaRepository<Operario, String> {
	// Método mágico de Spring: Busca un operario que coincida con el DNI y la clave
    Operario findByDniAndClave(String dni, String clave);
}