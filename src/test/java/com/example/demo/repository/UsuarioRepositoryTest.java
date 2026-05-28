package com.example.demo.repository;

import com.example.demo.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class UsuarioRepositoryTest {

    @Autowired
    private UsuarioRepository usuarioRepository;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuarioRepository.deleteAll();

        usuario = new Usuario();
        usuario.setUsername("testuser");
        usuario.setEmail("test@example.com");
        usuario.setPassword("encodedPassword");
        usuario.setNombre("Juan");
        usuario.setApellido("García");
        usuario.setActivo(true);
        usuarioRepository.save(usuario);
    }

    @Test
    void findByUsername_cuandoExiste_devuelveUsuario() {
        Usuario resultado = usuarioRepository.findByUsername("testuser");

        assertThat(resultado).isNotNull();
        assertThat(resultado.getUsername()).isEqualTo("testuser");
        assertThat(resultado.getEmail()).isEqualTo("test@example.com");
        assertThat(resultado.getNombre()).isEqualTo("Juan");
    }

    @Test
    void findByUsername_cuandoNoExiste_devuelveNull() {
        Usuario resultado = usuarioRepository.findByUsername("noexiste");

        assertThat(resultado).isNull();
    }

    @Test
    void save_guardaUsuarioCorrectamente() {
        Usuario nuevo = new Usuario();
        nuevo.setUsername("nuevo_usuario");
        nuevo.setEmail("nuevo@example.com");
        nuevo.setPassword("password123");
        nuevo.setActivo(true);

        Usuario guardado = usuarioRepository.save(nuevo);

        assertThat(guardado.getId()).isNotNull();
        assertThat(guardado.getUsername()).isEqualTo("nuevo_usuario");
    }

    @Test
    void findById_cuandoExiste_devuelveUsuario() {
        Optional<Usuario> resultado = usuarioRepository.findById(usuario.getId());

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getUsername()).isEqualTo("testuser");
    }

    @Test
    void delete_eliminaUsuarioCorrectamente() {
        usuarioRepository.delete(usuario);

        Optional<Usuario> resultado = usuarioRepository.findById(usuario.getId());
        assertThat(resultado).isEmpty();
    }

    @Test
    void findAll_devuelveListaDeUsuarios() {
        assertThat(usuarioRepository.findAll()).hasSize(1);
    }
}
