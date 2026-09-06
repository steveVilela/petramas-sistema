package com.petramas.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import com.petramas.model.Operario;
import com.petramas.model.RegistroDiario;
import com.petramas.repository.OrdenTrabajoRepository;
import com.petramas.repository.RegistroDiarioRepository;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpSession;

@Controller
public class RegistroDiarioController {

    @Autowired
    private OrdenTrabajoRepository ordenRepo;

    @Autowired
    private RegistroDiarioRepository registroRepo;

    @GetMapping("/reporte")
    public String mostrarFormulario(HttpSession session, Model model) {
        Operario usuario = (Operario) session.getAttribute("usuarioLogueado");
        if (usuario == null) return "redirect:/"; // Candado de seguridad

        model.addAttribute("registro", new RegistroDiario());
        
        // ¡Magia! Solo enviamos a la lista desplegable las que dicen "Pendiente"
        model.addAttribute("ordenes", ordenRepo.findByEstado("Pendiente"));
        
        // Enviamos los datos del usuario para pintar su nombre y el botón volver
        model.addAttribute("usuarioActual", usuario);
        model.addAttribute("rolUsuario", usuario.getEspecialidad());
        
        return "registro"; 
    }

    @PostMapping("/reporte")
    public String guardarReporte(RegistroDiario registro, HttpSession session, RedirectAttributes redirectAttributes) {
        Operario usuario = (Operario) session.getAttribute("usuarioLogueado");
        if (usuario == null) return "redirect:/";
        
        // ==========================================
        // VALIDACIÓN: Hora Fin debe ser mayor a Hora Inicio
        // ==========================================
        if (registro.getHoraInicio() != null && registro.getHoraFin() != null) {
            if (registro.getHoraFin().isBefore(registro.getHoraInicio()) || registro.getHoraFin().equals(registro.getHoraInicio())) {
                
                // Enviamos el mensaje de error y lo regresamos al formulario
                redirectAttributes.addFlashAttribute("errorHoras", "⚠️ La hora de fin debe ser mayor a la hora de inicio (revisa si es AM o PM).");
                return "redirect:/reporte";
            }
        }
        
        // Si todo está correcto, asignamos el usuario y guardamos
        registro.setOperario(usuario);
        registroRepo.save(registro);
        
        // Yo te sugeriría que al registrar con éxito, lo devuelva al Dashboard para que vea su tabla actualizada
        return "redirect:/dashboard"; 
    }
}