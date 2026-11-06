package com.petsalud.backendpetsalud.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;

/**
 * Crea y valida los tokens JWT.
 * Claims: iss (emisor), aud (audiencia), sub (username), roles, iat (emision) y exp (expiracion). Firma: HS256.
 * Semana 3: se agrega la audiencia (aud). Un token emitido para otro sistema con la misma clave
 * no sirve en esta API, porque al validarlo se exige el emisor y la audiencia configurados.
 */
@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expirationMs;
    private final String issuer;
    private final String audience;

    public JwtUtil(@Value("${jwt.secret}") String secret, @Value("${jwt.expiration-ms}") long expirationMs,
                   @Value("${jwt.issuer}") String issuer, @Value("${jwt.audience:petsalud-api}") String audience) {
        byte[] bytes = Decoders.BASE64.decode(secret);
        if (bytes.length < 32) {
            throw new IllegalArgumentException("La clave JWT debe contener al menos 32 bytes");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.expirationMs = expirationMs;
        this.issuer = issuer;
        this.audience = audience;
    }

    public String generarToken(UserDetails usuario) {
        List<String> roles = usuario.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        Date ahora = new Date();
        return Jwts.builder()
                .issuer(issuer)
                .audience().add(audience).and()
                .subject(usuario.getUsername())
                .claim("roles", roles)
                .issuedAt(ahora)
                .expiration(new Date(ahora.getTime() + expirationMs))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public String extraerUsername(String token) {
        return extraerClaims(token).getSubject();
    }

    public boolean validarToken(String token, UserDetails usuario) {
        Claims claims = extraerClaims(token);
        return claims.getSubject() != null
                && !claims.getSubject().isBlank()
                && claims.getSubject().equals(usuario.getUsername())
                && claims.getExpiration() != null;
    }

    // Verifica la firma; lanza JwtException si el token fue alterado, no esta firmado, expiro
    // o trae otro emisor o audiencia
    private Claims extraerClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(issuer)
                .requireAudience(audience)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}