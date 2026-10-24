package com.petsalud.backendpetsalud.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.stereotype.Component;

/** Registra los intentos de login fallidos para detectar actividad sospechosa. */
@Slf4j
@Component
public class SecurityEventsListener {

    @EventListener
    public void onFailure(AbstractAuthenticationFailureEvent event) {
        log.warn("LOGIN FALLIDO usuario={} motivo={}",
                event.getAuthentication().getName(),
                event.getException().getClass().getSimpleName());
    }
}
