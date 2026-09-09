package com.petramas.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.petramas.model.Operario;
import com.petramas.model.OrdenTrabajo;
import com.petramas.repository.OperarioRepository;
import com.petramas.repository.OrdenTrabajoRepository;

import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.PathVariable;


@Controller
@RequestMapping("/admin") // Todas las rutas aquí empezarán con /admin
public class AdminController {
	@Autowired
    private OperarioRepository operarioRepo;

    @Autowired
    private OrdenTrabajoRepository ordenTrabajoRepo; // NUEVO

    // Método de seguridad: Verifica si el que intenta entrar es realmente un Administrador
    private boolean esAdminSeguro(HttpSession session) {
        Operario usuario = (Operario) session.getAttribute("usuarioLogueado");
        return usuario != null && "Administrador".equalsIgnoreCase(usuario.getEspecialidad());
    }
    
 // Nuevo método: Deja pasar al Administrador y al Soldador
    private boolean tieneAccesoAOrdenes(HttpSession session) {
        Operario usuario = (Operario) session.getAttribute("usuarioLogueado");
        return usuario != null && 
              ("Administrador".equalsIgnoreCase(usuario.getEspecialidad()) || 
               "Soldador".equalsIgnoreCase(usuario.getEspecialidad()));
    }

 // 1. EL DASHBOARD CENTRAL (Menú principal)
    @GetMapping
    public String mostrarDashboardCentral(HttpSession session, Model model) {
        if (!esAdminSeguro(session)) return "redirect:/";
        
        Operario admin = (Operario) session.getAttribute("usuarioLogueado");
        model.addAttribute("nombreAdmin", admin.getNombreApellido());
        
        return "admin_dashboard"; // Nueva pantalla con los 3 botones
    }

    // 2. EL MÓDULO DE PERSONAL (Lo movemos a una sub-ruta)
    @GetMapping("/personal")
    public String gestionarPersonal(HttpSession session, Model model) {
        if (!esAdminSeguro(session)) return "redirect:/";
        
        Operario admin = (Operario) session.getAttribute("usuarioLogueado");
        model.addAttribute("nombreAdmin", admin.getNombreApellido());
        
        model.addAttribute("nuevoOperario", new Operario());
        model.addAttribute("listaPersonal", operarioRepo.findAll());
        
        return "admin_personal"; // Renombraremos el archivo HTML
    }

 // === NUEVO MÉTODO: Captura el clic en "Editar" y manda los datos al formulario ===
    @GetMapping("/editar/{dni}")
    public String editarPersonal(@PathVariable("dni") String dni, HttpSession session, Model model) {
        if (!esAdminSeguro(session)) return "redirect:/";

        Operario admin = (Operario) session.getAttribute("usuarioLogueado");
        model.addAttribute("nombreAdmin", admin.getNombreApellido());

        // Busca al operario específico en MySQL y lo envía al formulario
        Operario operarioAEditar = operarioRepo.findById(dni).orElse(new Operario());
        model.addAttribute("nuevoOperario", operarioAEditar);
        
        // Enviamos una "bandera" para que el HTML sepa que estamos en modo edición
        model.addAttribute("modoEdicion", true); 
        model.addAttribute("listaPersonal", operarioRepo.findAll());
        
        // ¡CORRECCIÓN AQUÍ! Devolvemos la vista HTML directamente para que pinte los datos
        return "admin_personal"; 
    }

 // === MÉTODO ACTUALIZADO: Más seguro para los Estados ===
    @PostMapping("/guardarPersonal")
    public String guardarPersonal(Operario operario, HttpSession session) {
        if (!esAdminSeguro(session)) return "redirect:/";
        
        // Verificamos si el usuario ya existe en la base de datos
        if (operarioRepo.existsById(operario.getDni())) {
            // Es una EDICIÓN
            Operario existente = operarioRepo.findById(operario.getDni()).get();
            operario.setClave(existente.getClave()); // Protegemos su contraseña
            
            // Si el HTML no envió un estado, mantenemos el que ya tenía en la BD
            if (operario.getEstado() == null || operario.getEstado().isEmpty()) {
                operario.setEstado(existente.getEstado());
            }
        } else {
            // Es NUEVO: Le asignamos su DNI como clave y lo activamos por defecto
            operario.setClave(operario.getDni());
            operario.setEstado("Activo");
        }
        
        operarioRepo.save(operario);
        return "redirect:/admin/personal?exito";
    }
    
 // === NUEVO MÉTODO: Borrado Lógico (Desactivar) ===
    @GetMapping("/eliminar/{dni}")
    public String eliminarPersonal(@PathVariable("dni") String dni, HttpSession session) {
        if (!esAdminSeguro(session)) return "redirect:/";
        
        // Buscamos al operario, le cambiamos el estado a Inactivo y lo guardamos
        if (operarioRepo.existsById(dni)) {
            Operario op = operarioRepo.findById(dni).get();
            op.setEstado("Inactivo");
            operarioRepo.save(op);
        }
        
        return "redirect:/admin/personal?eliminado";
    }
    
 // ==========================================
    //        MÓDULO DE ÓRDENES DE TRABAJO
    // ==========================================

    @GetMapping("/ordenes")
    public String gestionarOrdenes(HttpSession session, Model model) {
        // CAMBIO AQUÍ: Usamos la nueva regla
        if (!tieneAccesoAOrdenes(session)) return "redirect:/"; 
        
        Operario usuario = (Operario) session.getAttribute("usuarioLogueado");
        model.addAttribute("nombreAdmin", usuario.getNombreApellido());
        
        // Enviamos el rol a la pantalla para hacer el botón inteligente
        model.addAttribute("rolUsuario", usuario.getEspecialidad()); 
        
        model.addAttribute("nuevaOrden", new OrdenTrabajo());
        model.addAttribute("listaOrdenes", ordenTrabajoRepo.findAll());
        
        return "admin_ordenes"; 
    }

    @PostMapping("/guardarOrden")
public String guardarOrden(OrdenTrabajo orden, HttpSession session) {
    if (!tieneAccesoAOrdenes(session)) return "redirect:/"; 
    
    // Verificamos si la orden ya existe por su ID para evitar sobrescribir
    if (ordenTrabajoRepo.existsById(orden.getIdOrden())) {
        return "redirect:/admin/ordenes?errorDuplicado";
    }
    
    // REGLAS DE NEGOCIO: Si no se eligió fecha, por defecto ponemos la de hoy. 
    // Si el usuario eligió una fecha pasada en el formulario, se respeta esa.
    if (orden.getFechaCreacion() == null) {
        orden.setFechaCreacion(java.time.LocalDate.now());
    }
    
    if (orden.getEstado() == null || orden.getEstado().isEmpty()) {
        orden.setEstado("Pendiente"); // Estado por defecto
    }
    
    ordenTrabajoRepo.save(orden);
    return "redirect:/admin/ordenes?exito";
}

    
 // === NUEVO MÉTODO: Finalizar Orden ===
    @GetMapping("/finalizarOrden/{idOrden}")
    public String finalizarOrden(@PathVariable("idOrden") String idOrden, HttpSession session) {
    	// CAMBIO AQUÍ: Usamos la nueva regla
        if (!tieneAccesoAOrdenes(session)) return "redirect:/"; 
        
        
        if (ordenTrabajoRepo.existsById(idOrden)) {
            OrdenTrabajo orden = ordenTrabajoRepo.findById(idOrden).get();
            orden.setEstado("Finalizado");
            orden.setFechaFinalizacion(java.time.LocalDate.now()); // Sella la fecha de hoy
            ordenTrabajoRepo.save(orden);
        }
        
        return "redirect:/admin/ordenes?finalizado";
    }
    
}
