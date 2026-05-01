package com.example.demo.repository;

import com.example.demo.model.Busqueda;
import com.example.demo.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BusquedaRepository extends JpaRepository<Busqueda, Long> {
    List<Busqueda> findByUsuario(Usuario usuario);

    List<Busqueda> findByUsuarioOrderByFechaBusquedaDesc(Usuario usuario);

    List<Busqueda> findByUsuarioId(Long usuarioId);
}
