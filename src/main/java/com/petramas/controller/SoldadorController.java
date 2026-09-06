package com.petramas.controller;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.petramas.model.Operario;
import com.petramas.model.RegistroDiario;
import com.petramas.repository.RegistroDiarioRepository;

import jakarta.servlet.http.HttpSession;

@Controller
public class SoldadorController {

    @Autowired
    private RegistroDiarioRepository registroRepo;

    @GetMapping("/dashboard")
    public String mostrarDashboard(HttpSession session, Model model) {
        
        // 1. Verificación de Seguridad
        Operario usuario = (Operario) session.getAttribute("usuarioLogueado");
        if (usuario == null || !"Soldador".equalsIgnoreCase(usuario.getEspecialidad())) {
            return "redirect:/";
        }
        
        LocalDate hoy = LocalDate.now();
        LocalDate inicioMes = hoy.withDayOfMonth(1); // Día 1 del mes actual

        // ==========================================
        // BLOQUE 1: MIS ACTIVIDADES DE HOY (Recuadro Negro)
        // ==========================================
        List<RegistroDiario> registrosHoy = registroRepo.findByOperarioAndFecha(usuario, hoy);
        long minutosTotalesHoy = 0;
        
        for (RegistroDiario reg : registrosHoy) {
            if (reg.getHoraInicio() != null && reg.getHoraFin() != null) {
                minutosTotalesHoy += Duration.between(reg.getHoraInicio(), reg.getHoraFin()).toMinutes();
            }
        }
        
        long horasHoy = minutosTotalesHoy / 60;
        long minutosRestantesHoy = minutosTotalesHoy % 60;
        String totalHorasFormato = String.format("%d h %02d m", horasHoy, minutosRestantesHoy);
        
        // ==========================================
        // BLOQUE 2: ALERTAS DE HORAS PENDIENTES (Recuadro Amarillo)
        // ==========================================
        List<RegistroDiario> registrosMes = registroRepo.findByOperarioAndFechaBetween(usuario, inicioMes, hoy);
        Map<LocalDate, Double> horasPorDia = new HashMap<>();

        // Sumamos las horas de cada día
        for (RegistroDiario reg : registrosMes) {
            if (reg.getHoraInicio() != null && reg.getHoraFin() != null) {
                long minutos = ChronoUnit.MINUTES.between(reg.getHoraInicio(), reg.getHoraFin());
                double horas = minutos / 60.0;
                horasPorDia.put(reg.getFecha(), horasPorDia.getOrDefault(reg.getFecha(), 0.0) + horas);
            }
        }

        List<String> alertasIncompletas = new ArrayList<>();
        List<String> diasVacios = new ArrayList<>(); // <-- Aquí guardaremos los días en blanco
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM");

        // Revisamos día por día desde inicio de mes hasta AYER
        for (LocalDate dia = inicioMes; dia.isBefore(hoy); dia = dia.plusDays(1)) {
            double horasTrabajadas = horasPorDia.getOrDefault(dia, 0.0);
            
            // Si el total de horas en el día supera las 8 (ej. 7:30 a 16:30 = 9h), descontamos 1h de refrigerio
            if (horasTrabajadas > 8.0) {
                horasTrabajadas -= 1.0;
            }

            // CASO A: Jornada incompleta (Trabajó algo, pero no llegó a las 8 horas)
            if (horasTrabajadas > 0.0 && horasTrabajadas < 8.0) {
                double faltan = 8.0 - horasTrabajadas;
                alertasIncompletas.add("Día " + dia.format(formato) + ": Incompleto. Registraste " + horasTrabajadas + "h (Faltan " + faltan + "h)");
            }
            
            // CASO B: Día totalmente sin registros (0 horas)
            else if (horasTrabajadas == 0.0) {
                diasVacios.add(dia.format(formato));
            }
        }

        // Si hay días sin registros, los juntamos todos en un solo mensaje amigable
        if (!diasVacios.isEmpty()) {
            String fechasVacias = String.join(", ", diasVacios);
            alertasIncompletas.add("⚠️ Días sin reportes: " + fechasVacias + ". (Si alguno fue tu día de descanso, ignóralo. Regulariza los días laborables).");
        }

        // ==========================================
        // BLOQUE 3: ENVIAR DATOS A LA VISTA
        // ==========================================
        model.addAttribute("nombreUsuario", usuario.getNombreApellido());
        model.addAttribute("registrosHoy", registrosHoy);
        model.addAttribute("totalHoras", totalHorasFormato);
        model.addAttribute("alertasIncompletas", alertasIncompletas);
        
        return "soldador_dashboard";
    }
    
    
    
    // === MÉTODO: Historial Completo ===
    @GetMapping("/historial")
    public String verHistorial(HttpSession session, Model model) {
        Operario usuario = (Operario) session.getAttribute("usuarioLogueado");
        
        if (usuario == null || !"Soldador".equalsIgnoreCase(usuario.getEspecialidad())) {
            return "redirect:/";
        }
        
        // Buscamos todos sus registros ordenados por fecha (Del más nuevo al más viejo)
        List<RegistroDiario> miHistorial = registroRepo.findByOperarioOrderByFechaDesc(usuario);
        
        model.addAttribute("nombreUsuario", usuario.getNombreApellido());
        model.addAttribute("registros", miHistorial);
        
        return "soldador_historial";
    }
    
 // === MÉTODO: Eliminar Registro ===
    @GetMapping("/reporte/eliminar/{id}")
    public String eliminarRegistro(@PathVariable("id") Integer id, HttpSession session) {
        Operario usuario = (Operario) session.getAttribute("usuarioLogueado");
        
        // Seguridad básica
        if (usuario == null || !"Soldador".equalsIgnoreCase(usuario.getEspecialidad())) {
            return "redirect:/";
        }
        
        // Eliminamos el registro por su ID
        registroRepo.deleteById(id);
        
        // Redirigimos de vuelta al dashboard para que vea la tabla actualizada
        return "redirect:/dashboard";
    }
    
}