package com.petramas.controller;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
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
import org.springframework.web.bind.annotation.RequestParam;

import com.petramas.model.Operario;
import com.petramas.model.RegistroDiario;
import com.petramas.repository.RegistroDiarioRepository;

import jakarta.servlet.http.HttpSession;

@Controller
public class SoldadorController {

    @Autowired
    private RegistroDiarioRepository registroRepo;

    // Método auxiliar para calcular las horas netas de un día considerando el refrigerio fijo (12:30 a 13:30)
    private double calcularHorasNetasDia(List<RegistroDiario> registrosDelDia) {
        long minutosTotalesBrutos = 0;
        boolean cruzaRefrigerio = false;
        
        LocalTime inicioRefri = LocalTime.of(12, 30);
        LocalTime finRefri = LocalTime.of(13, 30);

        for (RegistroDiario reg : registrosDelDia) {
            if (reg.getHoraInicio() != null && reg.getHoraFin() != null) {
                minutosTotalesBrutos += Duration.between(reg.getHoraInicio(), reg.getHoraFin()).toMinutes();
                
                // Si algún bloque abarca o cruza el horario de refrigerio de 12:30 a 13:30
                if (reg.getHoraInicio().isBefore(finRefri) && reg.getHoraFin().isAfter(inicioRefri)) {
                    cruzaRefrigerio = true;
                }
            }
        }

        double horasNetas = minutosTotalesBrutos / 60.0;

        // Si la jornada acumulada supera las 5 horas o cruza el horario de almuerzo, descontamos 1 hora de refrigerio
        if (cruzaRefrigerio && horasNetas > 5.0) {
            horasNetas -= 1.0;
        }

        return Math.max(0.0, horasNetas);
    }

    @GetMapping("/dashboard")
    public String mostrarDashboard(HttpSession session, Model model) {
        
        Operario usuario = (Operario) session.getAttribute("usuarioLogueado");
        if (usuario == null || !"Soldador".equalsIgnoreCase(usuario.getEspecialidad())) {
            return "redirect:/";
        }
        
        LocalDate hoy = LocalDate.now();
        LocalDate inicioMes = hoy.withDayOfMonth(1);

        // ==========================================
        // BLOQUE 1: MIS ACTIVIDADES DE HOY (Recuadro Negro)
        // ==========================================
        List<RegistroDiario> registrosHoy = registroRepo.findByOperarioAndFecha(usuario, hoy);
        double horasNetasHoy = calcularHorasNetasDia(registrosHoy);
        
        long horasHoy = (long) horasNetasHoy;
        long minutosRestantesHoy = Math.round((horasNetasHoy - horasHoy) * 60);
        String totalHorasFormato = String.format("%d h %02d m", horasHoy, minutosRestantesHoy);
        
        // ==========================================
        // BLOQUE 2: ALERTAS DE HORAS PENDIENTES (Recuadro Amarillo)
        // ==========================================
        List<RegistroDiario> registrosMes = registroRepo.findByOperarioAndFechaBetween(usuario, inicioMes, hoy);
        
        // Agrupamos los registros por fecha
        Map<LocalDate, List<RegistroDiario>> registrosPorFecha = new HashMap<>();
        for (RegistroDiario reg : registrosMes) {
            registrosPorFecha.computeIfAbsent(reg.getFecha(), k -> new ArrayList<>()).add(reg);
        }

        List<String> alertasIncompletas = new ArrayList<>();
        List<String> diasVacios = new ArrayList<>();
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM");

        // Revisamos día por día desde inicio de mes hasta AYER
        for (LocalDate dia = inicioMes; dia.isBefore(hoy); dia = dia.plusDays(1)) {
            List<RegistroDiario> regsDelDia = registrosPorFecha.get(dia);
            
            if (regsDelDia == null || regsDelDia.isEmpty()) {
                diasVacios.add(dia.format(formato));
            } else {
                double horasTrabajadas = calcularHorasNetasDia(regsDelDia);

                if (horasTrabajadas > 0.0 && horasTrabajadas < 8.0) {
                    double faltan = 8.0 - horasTrabajadas;
                    alertasIncompletas.add("Día " + dia.format(formato) + ": Incompleto. Registraste " + String.format("%.1f", horasTrabajadas) + "h (Faltan " + String.format("%.1f", faltan) + "h)");
                }
            }
        }

        if (!diasVacios.isEmpty()) {
            String fechasVacias = String.join(", ", diasVacios);
            alertasIncompletas.add("⚠️ Días sin reportes: " + fechasVacias + ". (Si alguno fue tu día de descanso, ignóralo. Regulariza los días laborables).");
        }

        model.addAttribute("nombreUsuario", usuario.getNombreApellido());
        model.addAttribute("registrosHoy", registrosHoy);
        model.addAttribute("totalHoras", totalHorasFormato);
        model.addAttribute("alertasIncompletas", alertasIncompletas);
        
        return "soldador_dashboard";
    }
    
    @GetMapping("/historial")
    public String verHistorial(@RequestParam(name = "fechaFiltro", required = false) String fechaStr, 
                               HttpSession session, Model model) {
        Operario usuario = (Operario) session.getAttribute("usuarioLogueado");
        
        if (usuario == null || !"Soldador".equalsIgnoreCase(usuario.getEspecialidad())) {
            return "redirect:/";
        }
        
        LocalDate fechaSeleccionada;
        if (fechaStr != null && !fechaStr.isEmpty()) {
            fechaSeleccionada = LocalDate.parse(fechaStr);
        } else {
            fechaSeleccionada = LocalDate.now(); // Por defecto muestra el día de hoy
        }
        
        // Buscamos los registros del operario para esa fecha específica
        List<RegistroDiario> registrosDelDia = registroRepo.findByOperarioAndFechaOrderByHoraInicioAsc(usuario, fechaSeleccionada);
        
        // Calculamos las horas netas totales del día seleccionado usando tu lógica de refrigerio
        double horasNetasDia = calcularHorasNetasDia(registrosDelDia);
        long horas = (long) horasNetasDia;
        long minutos = Math.round((horasNetasDia - horas) * 60);
        String totalHorasDiaFormato = String.format("%d h %02d m", horas, minutos);
        
        // --- AQUÍ ESTÁ EL ÚNICO CAMBIO NUEVO: Convertimos a minutos totales enteros ---
        int minutosTotalesDia = (int) Math.round(horasNetasDia * 60);

        model.addAttribute("nombreUsuario", usuario.getNombreApellido());
        model.addAttribute("registros", registrosDelDia);
        model.addAttribute("fechaSeleccionada", fechaSeleccionada.toString());
        model.addAttribute("totalHorasDia", totalHorasDiaFormato);
        model.addAttribute("minutosTotalesDia", minutosTotalesDia); // <-- ¡Nuevo atributo para Thymeleaf!
        
        return "soldador_historial";
    }
    
    @GetMapping("/reporte/eliminar/{id}")
    public String eliminarRegistro(@PathVariable("id") Integer id, HttpSession session) {
        Operario usuario = (Operario) session.getAttribute("usuarioLogueado");
        
        if (usuario == null || !"Soldador".equalsIgnoreCase(usuario.getEspecialidad())) {
            return "redirect:/";
        }
        
        registroRepo.deleteById(id);
        
        return "redirect:/dashboard";
    }
}
