package com.example.demo.service;

import com.example.demo.dto.UserRegistrationDto;
import com.example.demo.model.Usuario;
import com.example.demo.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private UserRegistrationDto registrationDto;
    private Usuario usuarioActivo;
    private Usuario usuarioInactivo;

    @BeforeEach
    void setUp() {
        registrationDto = new UserRegistrationDto(
                "Juan", "García", "juangarcia",
                "juan@example.com", "password123", "password123");

        usuarioActivo = new Usuario();
        usuarioActivo.setId(1L);
        usuarioActivo.setUsername("juangarcia");
        usuarioActivo.setPassword("encodedPassword");
        usuarioActivo.setActivo(true);

        usuarioInactivo = new Usuario();
        usuarioInactivo.setId(2L);
        usuarioInactivo.setUsername("inactivo");
        usuarioInactivo.setPassword("encodedPassword");
        usuarioInactivo.setActivo(false);
    }

    @Test
    void save_creaUsuarioConDatosDelDto() {
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioActivo);

        Usuario resultado = userService.save(registrationDto);

        assertThat(resultado).isNotNull();
        verify(usuarioRepository, times(1)).save(any(Usuario.class));
        verify(passwordEncoder, times(1)).encode("password123");
    }

    @Test
    void save_codificaLaPasswordAntesDeGuardar() {
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            assertThat(u.getPassword()).isEqualTo("encodedPassword");
            assertThat(u.getPassword()).isNotEqualTo("password123");
            return u;
        });

        userService.save(registrationDto);

        verify(passwordEncoder).encode("password123");
    }

    @Test
    void save_estableceActivoEnTrue() {
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            assertThat(u.getActivo()).isTrue();
            return u;
        });

        userService.save(registrationDto);
    }

    @Test
    void save_mapeaTodosLosCamposDelDto() {
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            assertThat(u.getNombre()).isEqualTo("Juan");
            assertThat(u.getApellido()).isEqualTo("García");
            assertThat(u.getUsername()).isEqualTo("juangarcia");
            assertThat(u.getEmail()).isEqualTo("juan@example.com");
            return u;
        });

        userService.save(registrationDto);
    }

    @Test
    void loadUserByUsername_cuandoUsuarioExisteYActivo_devuelveUserDetails() {
        when(usuarioRepository.findByUsername("juangarcia")).thenReturn(usuarioActivo);

        UserDetails resultado = userService.loadUserByUsername("juangarcia");

        assertThat(resultado).isNotNull();
        assertThat(resultado.getUsername()).isEqualTo("juangarcia");
        assertThat(resultado.getPassword()).isEqualTo("encodedPassword");
    }

    @Test
    void loadUserByUsername_cuandoUsuarioNoExiste_lanzaUsernameNotFoundException() {
        when(usuarioRepository.findByUsername("noexiste")).thenReturn(null);

        assertThatThrownBy(() -> userService.loadUserByUsername("noexiste"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("inválidos");
    }

    @Test
    void loadUserByUsername_cuandoUsuarioInactivo_lanzaDisabledException() {
        when(usuarioRepository.findByUsername("inactivo")).thenReturn(usuarioInactivo);

        assertThatThrownBy(() -> userService.loadUserByUsername("inactivo"))
                .isInstanceOf(DisabledException.class)
                .hasMessageContaining("inactivo");
    }

    @Test
    void loadUserByUsername_asignaRoleUser() {
        when(usuarioRepository.findByUsername("juangarcia")).thenReturn(usuarioActivo);

        UserDetails resultado = userService.loadUserByUsername("juangarcia");

        assertThat(resultado.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");
    }
}
