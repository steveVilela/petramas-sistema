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

    // === MÉTODO AUXILIAR: Cálculo de horas netas descontando refrigerio fijo (12:30 - 13:30) ===
    private long calcularMinutosNetos(java.time.LocalTime inicio, java.time.LocalTime fin) {
        if (inicio == null || fin == null) return 0;
        
        long minutosBrutos = java.time.Duration.between(inicio, fin).toMinutes();
        if (minutosBrutos <= 0) return 0;

        java.time.LocalTime inicioRefri = java.time.LocalTime.of(12, 30);
        java.time.LocalTime finRefri = java.time.LocalTime.of(13, 30);

        long minutosRefrigerio = 0;
        
        // Si el bloque de trabajo abarca o cruza la hora de almuerzo
        if (inicio.isBefore(finRefri) && fin.isAfter(inicioRefri)) {
            java.time.LocalTime maxInicio = inicio.isAfter(inicioRefri) ? inicio : inicioRefri;
            java.time.LocalTime minFin = fin.isBefore(finRefri) ? fin : finRefri;
            minutosRefrigerio = java.time.Duration.between(maxInicio, minFin).toMinutes();
        }

        return minutosBrutos - minutosRefrigerio;
    }

    // === PANTALLA PRINCIPAL DE REPORTES ===
    @GetMapping
    public String mostrarReportes(
            @org.springframework.web.bind.annotation.RequestParam(value = "mes", required = false) String mesParam,
            HttpSession session, Model model) {
        
        if (!esAdminSeguro(session)) return "redirect:/";

        Operario admin = (Operario) session.getAttribute("usuarioLogueado");
        model.addAttribute("nombreAdmin", admin.getNombreApellido());

        // 1. Determinar el mes a consultar
        java.time.YearMonth mesSeleccionado;
        if (mesParam != null && !mesParam.isEmpty()) {
            mesSeleccionado = java.time.YearMonth.parse(mesParam);
        } else {
            mesSeleccionado = java.time.YearMonth.now();
        }

        // 2. Definimos el inicio y fin del mes seleccionado
        java.time.LocalDate inicioMes = mesSeleccionado.atDay(1);
        java.time.LocalDate finMes = mesSeleccionado.atEndOfMonth();

        // 3. Traemos los reportes de ese mes exacto
        List<com.petramas.model.RegistroDiario> registrosMes = registroRepo.findByFechaBetween(inicioMes, finMes);

        // ==========================================
        // 4. CONSOLIDADO DE HORAS POR OPERARIO (Ya con descuento de refrigerio)
        // ==========================================
        java.util.Map<String, Long> minutosPorOperario = new java.util.HashMap<>();

        for (com.petramas.model.RegistroDiario reg : registrosMes) {
            if (reg.getOperario() != null) {
                // Usamos nuestro nuevo método de minutos netos
                long minutosNetos = calcularMinutosNetos(reg.getHoraInicio(), reg.getHoraFin());
                
                if (minutosNetos > 0) { 
                    String nombre = reg.getOperario().getNombreApellido();
                    minutosPorOperario.put(nombre, minutosPorOperario.getOrDefault(nombre, 0L) + minutosNetos);
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
        model.addAttribute("mesSeleccionado", mesSeleccionado.toString()); 
        
        // ==========================================
        // 7. REPORTE DE ÓRDENES (Resumen y Detalle con horas netas)
        // ==========================================
        java.util.Map<String, Long> ordenMinutos = new java.util.HashMap<>();
        java.util.Map<String, String> ordenEstados = new java.util.HashMap<>();
        java.util.Map<String, java.util.Set<String>> ordenOperarios = new java.util.HashMap<>();
        java.util.Map<String, java.util.Set<String>> ordenFechas = new java.util.HashMap<>();

        for (com.petramas.model.RegistroDiario reg : registrosMes) {
            
            if (reg.getOrdenTrabajo() != null) {
                
                // Usamos el mismo método de minutos netos para las órdenes
                long minNetosOrden = calcularMinutosNetos(reg.getHoraInicio(), reg.getHoraFin());
                
                if (minNetosOrden > 0) {
                    String codOrden = reg.getOrdenTrabajo().getIdOrden(); 
                    
                    // A. Sumar el tiempo NETO a la orden
                    ordenMinutos.put(codOrden, ordenMinutos.getOrDefault(codOrden, 0L) + minNetosOrden);
                    // B. Registrar su estado actual
                    ordenEstados.put(codOrden, reg.getOrdenTrabajo().getEstado());
                    
                    // C. Guardar trabajadores sin repetir nombres
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
            
            fila.put("trabajadores", String.join(" • ", ordenOperarios.get(cod)));
            fila.put("fechas", String.join(", ", ordenFechas.get(cod)));
            
            reporteOrdenes.add(fila);
        }

        model.addAttribute("reporteOrdenes", reporteOrdenes);
        
        return "admin_reportes"; 
    }
}