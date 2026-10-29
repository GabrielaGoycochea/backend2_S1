package com.petsalud.backendpetsalud.dto;

/** Credenciales del login: el usuario se identifica con su email. */
public record AuthRequest(String email, String password) {
}