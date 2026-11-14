package com.petsalud.backendpetsalud;


import com.petsalud.backendpetsalud.entity.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class UsuarioTest {

    @Test
    public void testCrearUsuario() {
        Usuario usuario = Usuario.builder()
                .email("juan.perez@example.com")
                .password("password123")
                .nombre("Juan")
                .apellido("Perez")
                .telefono("1234567890")
                .direccion("Calle Falsa 123")
                .rol(Rol.CLIENTE)
                .mascotas(new ArrayList<>())
                .ventas(new ArrayList<>())
                .recompensas(new ArrayList<>())
                .notificaciones(new ArrayList<>())
                .build();

        assertNotNull(usuario);
        assertEquals("juan.perez@example.com", usuario.getEmail());
        assertEquals("password123", usuario.getPassword());
        assertEquals("Juan", usuario.getNombre());
        assertEquals("Perez", usuario.getApellido());
        assertEquals("1234567890", usuario.getTelefono());
        assertEquals("Calle Falsa 123", usuario.getDireccion());
        assertEquals(Rol.CLIENTE, usuario.getRol());
        assertTrue(usuario.getMascotas().isEmpty());
        assertTrue(usuario.getVentas().isEmpty());
        assertTrue(usuario.getRecompensas().isEmpty());
        assertTrue(usuario.getNotificaciones().isEmpty());
    }

    @Test
    public void testEquals() {
        List<Mascota> mascotas = new ArrayList<>();
        List<Venta> ventas = new ArrayList<>();
        List<Recompensa> recompensas = new ArrayList<>();
        List<Notificacion> notificaciones = new ArrayList<>();

        Usuario usuario1 = Usuario.builder()
                .id(1L)
                .email("juan.perez@example.com")
                .password("password123")
                .nombre("Juan")
                .apellido("Perez")
                .telefono("1234567890")
                .direccion("Calle Falsa 123")
                .rol(Rol.CLIENTE)
                .mascotas(mascotas)
                .ventas(ventas)
                .recompensas(recompensas)
                .notificaciones(notificaciones)
                .build();

        Usuario usuario2 = Usuario.builder()
                .id(1L)
                .email("juan.perez@example.com")
                .password("password123")
                .nombre("Juan")
                .apellido("Perez")
                .telefono("1234567890")
                .direccion("Calle Falsa 123")
                .rol(Rol.CLIENTE)
                .mascotas(mascotas)
                .ventas(ventas)
                .recompensas(recompensas)
                .notificaciones(notificaciones)
                .build();

        assertEquals(usuario1, usuario2);
    }
}
