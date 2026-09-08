package com.petramas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.petramas.model.Operario;
import com.petramas.repository.OperarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	@Bean
	public CommandLineRunner initData(OperarioRepository repo) {
		return args -> {
			// Si la base de datos está totalmente vacía, creamos el primer admin
			if (repo.count() == 0) {
				Operario admin = new Operario();
				admin.setDni("admin"); // Usuario para entrar
				admin.setClave("12345"); // Contraseña para entrar
				admin.setNombreApellido("Administrador Maestro");
				admin.setEspecialidad("Administrador");
				admin.setEstado("Activo");
				
				repo.save(admin);
				System.out.println("¡Base de datos inicializada! Administrador creado: admin / 12345");
			}
		};
	}
}
