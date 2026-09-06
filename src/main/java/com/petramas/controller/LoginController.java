package com.petramas.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.petramas.model.Operario;
import com.petramas.repository.OperarioRepository;

import jakarta.servlet.http.HttpSession;

@Controller
public class LoginController {

    @Autowired
    private OperarioRepository operarioRepo;

    // 1. Muestra la pantalla de login cuando entramos a localhost:8080/
    @GetMapping("/")
    public String mostrarLogin() {
        return "login";
    }

    // 2. Procesa los datos del formulario
    @PostMapping("/login")
    public String procesarLogin(@RequestParam("dni") String dni, 
                                @RequestParam("clave") String clave, 
                                HttpSession session, 
                                Model model) {
        
        Operario usuario = operarioRepo.findByDniAndClave(dni, clave);
        
        if (usuario != null) {
            
            // --- NUEVO CANDADO DE SEGURIDAD ---
            if ("Inactivo".equalsIgnoreCase(usuario.getEstado())) {
                model.addAttribute("error", "Usuario inactivo. Comuníquese con la administración.");
                return "login"; // Lo devuelve al login con el mensaje de error
            }
            // ----------------------------------

            session.setAttribute("usuarioLogueado", usuario);
            
            if ("Administrador".equalsIgnoreCase(usuario.getEspecialidad())) {
                return "redirect:/admin";
            } else {
                return "redirect:/dashboard"; 
            }
        } else {
            model.addAttribute("error", "DNI o clave incorrectos");
            return "login";
        }
    }
    
    // 3. Método para cerrar sesión
    @GetMapping("/logout")
    public String cerrarSesion(HttpSession session) {
        session.invalidate(); // Borra la memoria
        return "redirect:/?logout";
    }
}