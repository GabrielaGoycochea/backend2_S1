package com.petsalud.backendpetsalud.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import javax.crypto.SecretKey;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {
    private JwtUtil jwtUtil;
    private UserDetails usuario;
    private SecretKey key;

    @BeforeEach
    void preparar() {
        byte[] bytes = new byte[32];
        Arrays.fill(bytes, (byte) 7);
        key = Keys.hmacShaKeyFor(bytes);
        jwtUtil = new JwtUtil(Base64.getEncoder().encodeToString(bytes), 900000L, "PetSalud", "petsalud-api");
        usuario = User.withUsername("cliente@petsalud.cl").password("solo-prueba").authorities("CLIENTE").build();
    }

    @Test
    void tokenValidoIncluyeClaimsYFirma() {
        String token = jwtUtil.generarToken(usuario);
        var claims = Jwts.parser().verifyWith(key).requireIssuer("PetSalud").requireAudience("petsalud-api").build().parseSignedClaims(token).getPayload();
        assertEquals(usuario.getUsername(), claims.getSubject());
        assertEquals(List.of("CLIENTE"), claims.get("roles", List.class));
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
        assertTrue(jwtUtil.validarToken(token, usuario));
    }

    @Test
    void rechazaTokenDeOtroUsuario() {
        var otro = User.withUsername("otro@petsalud.cl").password("solo-prueba").authorities("CLIENTE").build();
        assertFalse(jwtUtil.validarToken(jwtUtil.generarToken(usuario), otro));
    }

    @Test
    void rechazaTokenVencido() {
        String token = Jwts.builder().issuer("PetSalud").audience().add("petsalud-api").and().subject(usuario.getUsername()).expiration(new Date(System.currentTimeMillis() - 60000)).signWith(key, Jwts.SIG.HS256).compact();
        assertThrows(ExpiredJwtException.class, () -> jwtUtil.validarToken(token, usuario));
    }

    @Test
    void rechazaFirmaDeOtraClave() {
        byte[] otraClave = new byte[32];
        Arrays.fill(otraClave, (byte) 8);
        String token = Jwts.builder().issuer("PetSalud").audience().add("petsalud-api").and().subject(usuario.getUsername()).expiration(new Date(System.currentTimeMillis() + 60000)).signWith(Keys.hmacShaKeyFor(otraClave), Jwts.SIG.HS256).compact();
        assertThrows(JwtException.class, () -> jwtUtil.validarToken(token, usuario));
    }

    @Test
    void rechazaEmisorIncorrecto() {
        String token = Jwts.builder().issuer("OtroSistema").audience().add("petsalud-api").and().subject(usuario.getUsername()).expiration(new Date(System.currentTimeMillis() + 60000)).signWith(key, Jwts.SIG.HS256).compact();
        assertThrows(JwtException.class, () -> jwtUtil.validarToken(token, usuario));
    }

    @Test
    void rechazaTokenSinUsuario() {
        String token = Jwts.builder().issuer("PetSalud").audience().add("petsalud-api").and().expiration(new Date(System.currentTimeMillis() + 60000)).signWith(key, Jwts.SIG.HS256).compact();
        assertFalse(jwtUtil.validarToken(token, usuario));
    }

    @Test
    void rechazaTokenSinExpiracion() {
        String token = Jwts.builder().issuer("PetSalud").audience().add("petsalud-api").and().subject(usuario.getUsername()).signWith(key, Jwts.SIG.HS256).compact();
        assertFalse(jwtUtil.validarToken(token, usuario));
    }
    @Test
    void tokenIncluyeAudiencia() {
        var claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(jwtUtil.generarToken(usuario)).getPayload();
        assertTrue(claims.getAudience().contains("petsalud-api"));
    }

    @Test
    void rechazaAudienciaIncorrecta() {
        String token = Jwts.builder().issuer("PetSalud").audience().add("otra-api").and().subject(usuario.getUsername()).expiration(new Date(System.currentTimeMillis() + 60000)).signWith(key, Jwts.SIG.HS256).compact();
        assertThrows(JwtException.class, () -> jwtUtil.validarToken(token, usuario));
    }

    @Test
    void rechazaClaveJwtDemasiadoCorta() {
        String claveCorta = Base64.getEncoder().encodeToString(new byte[16]);
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> new JwtUtil(claveCorta, 900000L, "PetSalud", "petsalud-api"));
        assertTrue(error.getMessage().contains("32 bytes"));
    }

    @Test
    void tokenGeneradoTieneDuracionDe15Minutos() {
        String token = jwtUtil.generarToken(usuario);
        var claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        long duracion = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();
        assertEquals(900000L, duracion);
    }
}
