package com.example.demo.service;

import com.example.demo.model.Busqueda;
import com.example.demo.model.Usuario;
import com.example.demo.repository.BusquedaRepository;
import com.example.demo.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BusquedaServiceTest {

    @Mock
    private BusquedaRepository busquedaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private BusquedaService busquedaService;

    private Usuario usuario;
    private List<Map<String, String>> resultados;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("testuser");

        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setUsername("testuser");
        usuario.setActivo(true);

        resultados = List.of(
                Map.of("label", "Don Quijote", "description", "Novela de Cervantes"),
                Map.of("label", "Rinconete y Cortadillo", "description", "Novela ejemplar"));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void saveBusqueda_cuandoUsuarioExiste_guardaLaBusqueda() {
        when(usuarioRepository.findByUsername("testuser")).thenReturn(usuario);

        busquedaService.saveBusqueda("Cervantes", "wikidata", "SELECT ?item ...", resultados, 200L, 100L);

        verify(busquedaRepository, times(1)).save(any(Busqueda.class));
    }

    @Test
    void saveBusqueda_cuandoUsuarioNoExiste_noGuardaNada() {
        when(usuarioRepository.findByUsername("testuser")).thenReturn(null);

        busquedaService.saveBusqueda("Cervantes", "wikidata", "SELECT ?item ...", resultados, 200L, 100L);

        verify(busquedaRepository, never()).save(any(Busqueda.class));
    }

    @Test
    void saveBusqueda_calculaElTiempoDeEjecucionCorrectamente() {
        when(usuarioRepository.findByUsername("testuser")).thenReturn(usuario);

        busquedaService.saveBusqueda("Cervantes", "wikidata", "SELECT ?item ...", resultados, 500L, 100L);

        ArgumentCaptor<Busqueda> captor = ArgumentCaptor.forClass(Busqueda.class);
        verify(busquedaRepository).save(captor.capture());

        Busqueda guardada = captor.getValue();
        assertThat(guardada.getTiempoEjecucionMs()).isEqualTo(400L); // 500 - 100
    }

    @Test
    void saveBusqueda_guardaElTotalDeResultados() {
        when(usuarioRepository.findByUsername("testuser")).thenReturn(usuario);

        busquedaService.saveBusqueda("Cervantes", "wikidata", "SELECT ?item ...", resultados, 200L, 100L);

        ArgumentCaptor<Busqueda> captor = ArgumentCaptor.forClass(Busqueda.class);
        verify(busquedaRepository).save(captor.capture());

        Busqueda guardada = captor.getValue();
        assertThat(guardada.getTotalResultados()).isEqualTo(2);
    }

    @Test
    void saveBusqueda_guardaElTerminoCorrectamente() {
        when(usuarioRepository.findByUsername("testuser")).thenReturn(usuario);

        busquedaService.saveBusqueda("Lope de Vega", "bnf", "SELECT ?author ...", resultados, 200L, 100L);

        ArgumentCaptor<Busqueda> captor = ArgumentCaptor.forClass(Busqueda.class);
        verify(busquedaRepository).save(captor.capture());

        Busqueda guardada = captor.getValue();
        assertThat(guardada.getTerminoBusqueda()).isEqualTo("Lope de Vega");
        assertThat(guardada.getEndpointsConsultados()).isEqualTo("bnf");
        assertThat(guardada.getQuerySparql()).isEqualTo("SELECT ?author ...");
    }

    @Test
    void saveBusqueda_cuandoTopicEsNull_usaCustomQuery() {
        when(usuarioRepository.findByUsername("testuser")).thenReturn(usuario);

        busquedaService.saveBusqueda(null, "wikidata", "SELECT ?item ...", resultados, 200L, 100L);

        ArgumentCaptor<Busqueda> captor = ArgumentCaptor.forClass(Busqueda.class);
        verify(busquedaRepository).save(captor.capture());

        Busqueda guardada = captor.getValue();
        assertThat(guardada.getTerminoBusqueda()).isEqualTo("Custom Query");
    }

    @Test
    void saveBusqueda_asociaElUsuarioCorrecto() {
        when(usuarioRepository.findByUsername("testuser")).thenReturn(usuario);

        busquedaService.saveBusqueda("Cervantes", "wikidata", "SELECT ?item ...", resultados, 200L, 100L);

        ArgumentCaptor<Busqueda> captor = ArgumentCaptor.forClass(Busqueda.class);
        verify(busquedaRepository).save(captor.capture());

        Busqueda guardada = captor.getValue();
        assertThat(guardada.getUsuario()).isEqualTo(usuario);
        assertThat(guardada.getUsuario().getUsername()).isEqualTo("testuser");
    }
}
