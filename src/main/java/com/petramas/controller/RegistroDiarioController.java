8package com.petramas.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.petramas.model.Operario;
import com.petramas.model.RegistroDiario;
import com.petramas.repository.OperarioRepository;
import com.petramas.repository.OrdenTrabajoRepository;
import com.petramas.repository.RegistroDiarioRepository;

import jakarta.servlet.http.HttpSession;

@Controller
public class RegistroDiarioController {

    @Autowired
    private OrdenTrabajoRepository ordenRepo;

    @Autowired
    private RegistroDiarioRepository registroRepo;

    // NUEVO: Repositorio de operarios para buscar al compañero y cargar la lista
    @Autowired
    private OperarioRepository operarioRepo;

    @GetMapping("/reporte")
    public String mostrarFormulario(HttpSession session, Model model) {
        Operario usuario = (Operario) session.getAttribute("usuarioLogueado");
        if (usuario == null) return "redirect:/";

        model.addAttribute("registro", new RegistroDiario());
        model.addAttribute("ordenes", ordenRepo.findByEstado("Pendiente"));
        model.addAttribute("usuarioActual", usuario);
        model.addAttribute("rolUsuario", usuario.getEspecialidad());
        // Pasamos la lista para llenar el <select>
        model.addAttribute("listaOperarios", operarioRepo.findAll());
        
        return "registro"; 
    }

        @PostMapping("/reporte")
    public String guardarReporte(RegistroDiario registro, 
                                 @RequestParam(name = "dniCompanero", required = false) String dniCompanero,
                                 HttpSession session, RedirectAttributes redirectAttributes) {
        Operario usuario = (Operario) session.getAttribute("usuarioLogueado");
        if (usuario == null) return "redirect:/";
        
        if (registro.getHoraInicio() != null && registro.getHoraFin() != null) {
            if (registro.getHoraFin().isBefore(registro.getHoraInicio()) || registro.getHoraFin().equals(registro.getHoraInicio())) {
                redirectAttributes.addFlashAttribute("errorHoras", "⚠️ La hora de fin debe ser mayor a la hora de inicio (revisa si es AM o PM).");
                return "redirect:/reporte";
            }
        }
        
        registro.setOperario(usuario);

        // Validar si el registro original ya existe
        if (registro.getIdRegistro() == null) {
            boolean yaExiste = registroRepo.existsByOperarioAndFechaAndHoraInicioAndHoraFin(
                usuario, registro.getFecha(), registro.getHoraInicio(), registro.getHoraFin()
            );
            if (yaExiste) {
                return "redirect:/dashboard";
            }
        }
        
        // 1. Guarda el registro para ti (el usuario logueado)
        registroRepo.save(registro);

        // 2. Bloque de seguridad para guardar al compañero SIN que se caiga la app
        try {
            if (dniCompanero != null && !dniCompanero.trim().isEmpty()) {
                System.out.println("Intentando agregar compañero con DNI: " + dniCompanero);
                
                Operario companero = operarioRepo.findById(dniCompanero.trim()).orElse(null);
                
                if (companero != null) {
                    RegistroDiario registroCompanero = new RegistroDiario();
                    
                    // Asegurarnos de que copiamos datos limpios
                    registroCompanero.setOperario(companero);
                    registroCompanero.setOrdenTrabajo(registro.getOrdenTrabajo());
                    registroCompanero.setFecha(registro.getFecha());
                    registroCompanero.setHoraInicio(registro.getHoraInicio());
                    registroCompanero.setHoraFin(registro.getHoraFin());
                    registroCompanero.setActividad(registro.getActividad());
                    
                    registroRepo.save(registroCompanero);
                    System.out.println("Compañero guardado exitosamente.");
                }
            }
        } catch (Exception e) {
            // Si algo explota aquí, lo captura, lo imprime en consola y NO tumba la página
            System.err.println("ERROR GRAVE al guardar el compañero: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorHoras", "⚠️ Tu reporte se guardó, pero hubo un error al duplicarlo para tu compañero.");
            return "redirect:/dashboard";
        }
        
        return "redirect:/dashboard"; 
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
        // Pasamos la lista para llenar el <select>
        model.addAttribute("listaOperarios", operarioRepo.findAll());
        model.addAttribute("modoEdicion", true);
        
        return "registro"; 
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
        // Pasamos la lista para llenar el <select>
        model.addAttribute("listaOperarios", operarioRepo.findAll());
        
        return "registro"; 
    }
}
