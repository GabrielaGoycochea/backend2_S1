package com.petsalud.backendpetsalud.config;

import com.petsalud.backendpetsalud.entity.Rol;
import com.petsalud.backendpetsalud.entity.Usuario;
import com.petsalud.backendpetsalud.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * SOLO PARA PRUEBAS (perfil "prueba"): crea un usuario por rol para probar en Postman.
 * Ejecutar con: --spring.profiles.active=prueba
 */
@Component
@Profile("prueba")
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository repo;
    private final PasswordEncoder encoder;

    public DataInitializer(UsuarioRepository repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        crear("admin@petsalud.cl", "Admin", Rol.ADMIN);
        crear("vet@petsalud.cl", "Veterinario", Rol.VETERINARIO);
        crear("cliente@petsalud.cl", "Cliente", Rol.CLIENTE);
    }

    private void crear(String email, String nombre, Rol rol) {
        if (repo.findByEmail(email).isEmpty()) {
            repo.save(Usuario.builder()
                    .email(email).nombre(nombre).apellido("Prueba")
                    .password(encoder.encode("Petsalud2026!")).rol(rol).build());
        }
    }
}
