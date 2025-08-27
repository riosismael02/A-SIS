package com.asis.controller;

import com.asis.model.Empleado;
import com.asis.model.Usuario;
import com.asis.model.Rol;
import com.asis.repository.EmpleadoRepository;
import com.asis.service.EmpleadoService;
import com.asis.service.UsuarioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final EmpleadoRepository empleadoRepository;

    public UsuarioController(UsuarioService usuarioService, EmpleadoRepository empleadoRepository) {
        this.usuarioService = usuarioService;
        this.empleadoRepository = empleadoRepository;
    }


    @GetMapping("/nuevo")
    public String mostrarFormularioCreacion(Model model) {
        // Obtener lista de empleados que no tienen usuario asignado
        List<Empleado> empleadosDisponibles = empleadoRepository.findEmpleadosSinUsuario();
        model.addAttribute("roles", Rol.values());
        model.addAttribute("usuario", new Usuario());
        model.addAttribute("empleados", empleadosDisponibles);
        return "usuarios/gestion";
    }

    @PostMapping("/guardar")
    public String guardarUsuario(@ModelAttribute Usuario usuario,
                                 @RequestParam Long empleadoId,
                                 RedirectAttributes redirectAttributes) {
        if(usuario.getPassword() == null || usuario.getPassword().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "La contraseña no puede estar vacía");
            return "redirect:/usuarios/nuevo";
        }
        usuario.setActivo(true);
        Usuario usuarioGuardado = usuarioService.guardarUsuarioConEmpleado(usuario, empleadoId);
        redirectAttributes.addFlashAttribute("mensaje", "Usuario creado exitosamente");
        return "redirect:/usuarios/nuevo";
    }
}