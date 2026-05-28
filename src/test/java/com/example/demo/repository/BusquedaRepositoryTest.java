package com.example.demo.repository;

import com.example.demo.model.Busqueda;
import com.example.demo.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class BusquedaRepositoryTest {

    @Autowired
    private BusquedaRepository busquedaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private Usuario usuario;
    private Usuario otroUsuario;

    @BeforeEach
    void setUp() {
        busquedaRepository.deleteAll();
        usuarioRepository.deleteAll();

        usuario = new Usuario();
        usuario.setUsername("usuario1");
        usuario.setEmail("usuario1@example.com");
        usuario.setPassword("password");
        usuario.setActivo(true);
        usuarioRepository.save(usuario);

        otroUsuario = new Usuario();
        otroUsuario.setUsername("usuario2");
        otroUsuario.setEmail("usuario2@example.com");
        otroUsuario.setPassword("password");
        otroUsuario.setActivo(true);
        usuarioRepository.save(otroUsuario);

        // Crear búsquedas para usuario1
        Busqueda b1 = crearBusqueda(usuario, "Cervantes", "wikidata", "SELECT ?item ...", 2);
        Busqueda b2 = crearBusqueda(usuario, "Lope de Vega", "bnf", "SELECT ?author ...", 5);
        busquedaRepository.save(b1);
        busquedaRepository.save(b2);

        // Crear búsqueda para usuario2
        Busqueda b3 = crearBusqueda(otroUsuario, "Quevedo", "wikidata", "SELECT ?item ...", 1);
        busquedaRepository.save(b3);
    }

    private Busqueda crearBusqueda(Usuario u, String termino, String endpoint, String query, int resultados) {
        Busqueda b = new Busqueda();
        b.setUsuario(u);
        b.setTerminoBusqueda(termino);
        b.setEndpointsConsultados(endpoint);
        b.setQuerySparql(query);
        b.setTotalResultados(resultados);
        b.setTiempoEjecucionMs(100L);
        return b;
    }

    @Test
    void findByUsuario_devuelveSoloLasBusquedasDelUsuario() {
        List<Busqueda> resultado = busquedaRepository.findByUsuario(usuario);

        assertThat(resultado).hasSize(2);
        assertThat(resultado).allMatch(b -> b.getUsuario().getUsername().equals("usuario1"));
    }

    @Test
    void findByUsuario_cuandoUsuarioSinBusquedas_devuelveListaVacia() {
        Usuario sinBusquedas = new Usuario();
        sinBusquedas.setUsername("sinbusquedas");
        sinBusquedas.setEmail("sinbusquedas@example.com");
        sinBusquedas.setPassword("password");
        sinBusquedas.setActivo(true);
        usuarioRepository.save(sinBusquedas);

        List<Busqueda> resultado = busquedaRepository.findByUsuario(sinBusquedas);

        assertThat(resultado).isEmpty();
    }

    @Test
    void findByUsuarioId_devuelveLasBusquedasPorId() {
        List<Busqueda> resultado = busquedaRepository.findByUsuarioId(usuario.getId());

        assertThat(resultado).hasSize(2);
    }

    @Test
    void findByUsuarioOrderByFechaBusquedaDesc_devuelveOrdenCorrecto() {
        List<Busqueda> resultado = busquedaRepository.findByUsuarioOrderByFechaBusquedaDesc(usuario);

        assertThat(resultado).hasSize(2);
        // Todas las búsquedas pertenecen al mismo usuario
        assertThat(resultado).allMatch(b -> b.getUsuario().getId().equals(usuario.getId()));
    }

    @Test
    void save_guardaBusquedaConTodosLosCampos() {
        Busqueda nueva = crearBusqueda(usuario, "Tirso de Molina", "cervantesvirtual", "SELECT ?work ...", 7);
        Busqueda guardada = busquedaRepository.save(nueva);

        assertThat(guardada.getId()).isNotNull();
        assertThat(guardada.getTerminoBusqueda()).isEqualTo("Tirso de Molina");
        assertThat(guardada.getEndpointsConsultados()).isEqualTo("cervantesvirtual");
        assertThat(guardada.getTotalResultados()).isEqualTo(7);
    }

    @Test
    void findAll_devuelveTodasLasBusquedas() {
        List<Busqueda> todas = busquedaRepository.findAll();

        assertThat(todas).hasSize(3);
    }
}
