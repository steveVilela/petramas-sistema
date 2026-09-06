package com.petramas.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.petramas.model.Operario;
import com.petramas.repository.RegistroDiarioRepository;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/admin/reportes") // Ruta base para todo lo de reportes
public class ReporteAdminController {

    @Autowired
    private RegistroDiarioRepository registroRepo;

    // Método de seguridad para proteger los reportes
    private boolean esAdminSeguro(HttpSession session) {
        Operario usuario = (Operario) session.getAttribute("usuarioLogueado");
        return usuario != null && "Administrador".equalsIgnoreCase(usuario.getEspecialidad());
    }

    // === PANTALLA PRINCIPAL DE REPORTES ===
 // === PANTALLA PRINCIPAL DE REPORTES ===
    @GetMapping
    public String mostrarReportes(
            @org.springframework.web.bind.annotation.RequestParam(value = "mes", required = false) String mesParam,
            HttpSession session, Model model) {
        
        if (!esAdminSeguro(session)) return "redirect:/";

        Operario admin = (Operario) session.getAttribute("usuarioLogueado");
        model.addAttribute("nombreAdmin", admin.getNombreApellido());

        // 1. Determinar el mes a consultar (Usamos YearMonth para manejar el formato "YYYY-MM")
        java.time.YearMonth mesSeleccionado;
        if (mesParam != null && !mesParam.isEmpty()) {
            mesSeleccionado = java.time.YearMonth.parse(mesParam); // Si el usuario eligió un mes
        } else {
            mesSeleccionado = java.time.YearMonth.now(); // Por defecto el mes actual
        }

        // 2. Definimos el inicio y fin del mes seleccionado
        java.time.LocalDate inicioMes = mesSeleccionado.atDay(1);
        java.time.LocalDate finMes = mesSeleccionado.atEndOfMonth();

        // 3. Traemos los reportes de ese mes exacto
        List<com.petramas.model.RegistroDiario> registrosMes = registroRepo.findByFechaBetween(inicioMes, finMes);

        // 4. Sumamos las horas (con el escudo protector para descartar errores negativos)
        java.util.Map<String, Long> minutosPorOperario = new java.util.HashMap<>();

        for (com.petramas.model.RegistroDiario reg : registrosMes) {
            if (reg.getHoraInicio() != null && reg.getHoraFin() != null && reg.getOperario() != null) {
                long minutos = java.time.Duration.between(reg.getHoraInicio(), reg.getHoraFin()).toMinutes();
                
                if (minutos > 0) { // Solo suma si los minutos son reales/positivos
                    String nombre = reg.getOperario().getNombreApellido();
                    minutosPorOperario.put(nombre, minutosPorOperario.getOrDefault(nombre, 0L) + minutos);
                }
            }
        }

        // 5. Convertimos a texto "XX h YY m"
        java.util.Map<String, String> reporteFinal = new java.util.HashMap<>();
        for (java.util.Map.Entry<String, Long> entry : minutosPorOperario.entrySet()) {
            long horas = entry.getValue() / 60;
            long minutosRestantes = entry.getValue() % 60;
            reporteFinal.put(entry.getKey(), String.format("%d h %02d m", horas, minutosRestantes));
        }

        // 6. Enviamos datos a la vista
        model.addAttribute("reporteHoras", reporteFinal);
        model.addAttribute("mesSeleccionado", mesSeleccionado.toString()); // Enviamos "2026-09" al HTML
        
     // ==========================================
        // 7. REPORTE DE ÓRDENES (Resumen y Detalle)
        // ==========================================
        java.util.Map<String, Long> ordenMinutos = new java.util.HashMap<>();
        java.util.Map<String, String> ordenEstados = new java.util.HashMap<>();
        java.util.Map<String, java.util.Set<String>> ordenOperarios = new java.util.HashMap<>();
        java.util.Map<String, java.util.Set<String>> ordenFechas = new java.util.HashMap<>();

        for (com.petramas.model.RegistroDiario reg : registrosMes) {
            
            // ⚠️ OJO: Cambia "getOrden()" por el nombre de tu atributo (ej. getOrdenTrabajo())
            if (reg.getHoraInicio() != null && reg.getHoraFin() != null && reg.getOrdenTrabajo() != null) {
                
                long min = java.time.Duration.between(reg.getHoraInicio(), reg.getHoraFin()).toMinutes();
                
                if (min > 0) {
                    // ⚠️ OJO: Cambia "getId()" por el nombre de tu ID de Orden (ej. getIdOrden() o getCodigo())
                    String codOrden = reg.getOrdenTrabajo().getIdOrden(); 
                    
                    // A. Sumar el tiempo a la orden
                    ordenMinutos.put(codOrden, ordenMinutos.getOrDefault(codOrden, 0L) + min);
                    // B. Registrar su estado actual
                    ordenEstados.put(codOrden, reg.getOrdenTrabajo().getEstado());
                    
                    // C. Guardar trabajadores sin repetir nombres (Usamos HashSet)
                    ordenOperarios.putIfAbsent(codOrden, new java.util.HashSet<>());
                    ordenOperarios.get(codOrden).add(reg.getOperario().getNombreApellido());
                    
                    // D. Guardar las fechas de intervención sin repetir
                    ordenFechas.putIfAbsent(codOrden, new java.util.HashSet<>());
                    ordenFechas.get(codOrden).add(reg.getFecha().toString());
                }
            }
        }

        // 8. Convertir los datos a una lista amigable para enviarla al HTML
        List<java.util.Map<String, Object>> reporteOrdenes = new java.util.ArrayList<>();
        for (String cod : ordenMinutos.keySet()) {
            java.util.Map<String, Object> fila = new java.util.HashMap<>();
            fila.put("codigo", cod);
            fila.put("estado", ordenEstados.get(cod));
            
            long h = ordenMinutos.get(cod) / 60;
            long m = ordenMinutos.get(cod) % 60;
            fila.put("tiempoTotal", String.format("%d h %02d m", h, m));
            
            // Unimos los nombres y las fechas separándolos por comas
            fila.put("trabajadores", String.join(" • ", ordenOperarios.get(cod)));
            fila.put("fechas", String.join(", ", ordenFechas.get(cod)));
            
            reporteOrdenes.add(fila);
        }

        // Enviamos la lista de órdenes procesadas a la vista
        model.addAttribute("reporteOrdenes", reporteOrdenes);
        
        
        return "admin_reportes"; 
    }
}