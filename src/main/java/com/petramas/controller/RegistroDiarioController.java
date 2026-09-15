package com.petramas.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
// ¡ESTA ES LA IMPORTACIÓN QUE FALTABA!
import org.springframework.web.bind.annotation.RequestParam;

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
        if (usuario == null) return "redirect:/";

        model.addAttribute("registro", new RegistroDiario());
        model.addAttribute("ordenes", ordenRepo.findByEstado("Pendiente"));
        model.addAttribute("usuarioActual", usuario);
        model.addAttribute("rolUsuario", usuario.getEspecialidad());
        
        return "registro"; 
    }

    @GetMapping("/reporte/editar/{id}")
    public String editarReporte(@PathVariable("id") Integer id, HttpSession session, Model model) {
        Operario usuario = (Operario) session.getAttribute("usuarioLogueado");
        if (usuario == null) return "redirect:/";

        RegistroDiario registroAEditar = registroRepo.findById(id).orElse(null);
        
        if (registroAEditar == null || !registroAEditar.getOperario().getDni().equals(usuario.getDni())) {
            return "redirect:/dashboard";
        }

        model.addAttribute("registro", registroAEditar);
        model.addAttribute("ordenes", ordenRepo.findByEstado("Pendiente"));
        model.addAttribute("usuarioActual", usuario);
        model.addAttribute("rolUsuario", usuario.getEspecialidad());
        model.addAttribute("modoEdicion", true);
        
        return "registro"; 
    }

    @PostMapping("/reporte")
    public String guardarReporte(RegistroDiario registro, HttpSession session, RedirectAttributes redirectAttributes) {
        Operario usuario = (Operario) session.getAttribute("usuarioLogueado");
        if (usuario == null) return "redirect:/";
        
        if (registro.getHoraInicio() != null && registro.getHoraFin() != null) {
            if (registro.getHoraFin().isBefore(registro.getHoraInicio()) || registro.getHoraFin().equals(registro.getHoraInicio())) {
                redirectAttributes.addFlashAttribute("errorHoras", "⚠️ La hora de fin debe ser mayor a la hora de inicio (revisa si es AM o PM).");
                return "redirect:/reporte";
            }
        }
        
        registro.setOperario(usuario);

        if (registro.getIdRegistro() == null) {
            boolean yaExiste = registroRepo.existsByOperarioAndFechaAndHoraInicioAndHoraFin(
                usuario, registro.getFecha(), registro.getHoraInicio(), registro.getHoraFin()
            );

            if (yaExiste) {
                return "redirect:/dashboard";
            }
        }
        
        registroRepo.save(registro);
        
        return "redirect:/dashboard"; 
    }

    @GetMapping("/reporte/nuevo")
    public String nuevoReporteConFecha(@RequestParam(name = "fecha", required = false) String fechaStr, 
                                      HttpSession session, Model model) {
        Operario usuario = (Operario) session.getAttribute("usuarioLogueado");
        if (usuario == null) return "redirect:/";

        RegistroDiario nuevoRegistro = new RegistroDiario();
        
        // Si el enlace traía una fecha seleccionada, la seteamos de una vez
        if (fechaStr != null && !fechaStr.isEmpty()) {
            nuevoRegistro.setFecha(java.time.LocalDate.parse(fechaStr));
        } else {
            nuevoRegistro.setFecha(java.time.LocalDate.now()); // Por si acaso, la de hoy
        }

        model.addAttribute("registro", nuevoRegistro);
        model.addAttribute("ordenes", ordenRepo.findByEstado("Pendiente"));
        model.addAttribute("usuarioActual", usuario);
        model.addAttribute("rolUsuario", usuario.getEspecialidad());
        
        return "registro"; 
    }
}
