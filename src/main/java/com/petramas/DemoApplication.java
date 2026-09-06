package com.petramas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	@org.springframework.context.annotation.Bean
	public org.springframework.boot.CommandLineRunner initData(com.petramas.repository.OperarioRepository repo) {
		return args -> {
			// Si la base de datos está totalmente vacía, creamos el primer admin
			if (repo.count() == 0) {
				com.petramas.model.Operario admin = new com.petramas.model.Operario();
				admin.setDni("admin"); // Usuario para entrar
				admin.setClave("12345"); // Contraseña para entrar
				admin.setNombreApellido("Administrador Maestro");
				admin.setEspecialidad("Sistemas");
				admin.setEstado("Administrador"); // Asumo que así diferencias a los admins
				
				repo.save(admin);
				System.out.println("¡Base de datos inicializada! Administrador creado: admin / 12345");
			}
		};
	}
}
