package com.example.demo.controller;

import com.example.demo.model.Busqueda;
import com.example.demo.model.Usuario;
import com.example.demo.repository.BusquedaRepository;
import com.example.demo.repository.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class BusquedaController {

    private final BusquedaRepository busquedaRepository;
    private final UsuarioRepository usuarioRepository;

    public BusquedaController(BusquedaRepository busquedaRepository, UsuarioRepository usuarioRepository) {
        this.busquedaRepository = busquedaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping("/mis-busquedas")
    public String misBusquedas(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        Usuario usuario = usuarioRepository.findByUsername(username);

        if (usuario != null) {
            List<Busqueda> busquedas = busquedaRepository.findByUsuarioOrderByFechaBusquedaDesc(usuario);
            model.addAttribute("busquedas", busquedas);
        }

        return "mis-busquedas";
    }
}
