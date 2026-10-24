package com.petsalud.backendpetsalud.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Registra actividad sospechosa: logins fallidos, bloqueos por fuerza bruta y accesos denegados.
 * Nunca registra contrasenas.
 */
@Slf4j
@Component
public class SecurityEventsListener {

    private final LoginAttemptService loginAttemptService;

    public SecurityEventsListener(LoginAttemptService loginAttemptService) {
        this.loginAttemptService = loginAttemptService;
    }

    @EventListener
    public void onFailure(AbstractAuthenticationFailureEvent event) {
        String usuario = event.getAuthentication().getName();
        log.warn("LOGIN FALLIDO usuario={} motivo={} ip={}",
                usuario, event.getException().getClass().getSimpleName(), ip());
        if (event instanceof AuthenticationFailureBadCredentialsEvent && loginAttemptService.registrarFallo(usuario)) {
            log.warn("CUENTA BLOQUEADA usuario={} motivo=exceso_de_intentos ip={}", usuario, ip());
        }
    }

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        loginAttemptService.registrarExito(event.getAuthentication().getName());
    }

    @EventListener
    public void onAccessDenied(AuthorizationDeniedEvent<?> event) {
        Authentication auth = event.getAuthentication().get();
        String usuario = (auth == null || "anonymousUser".equals(auth.getName())) ? "anonimo" : auth.getName();
        HttpServletRequest req = request();
        log.warn("ACCESO DENEGADO usuario={} metodo={} recurso={} ip={}", usuario,
                req != null ? req.getMethod() : "-", req != null ? req.getRequestURI() : "-", ip());
    }

    private String ip() {
        HttpServletRequest req = request();
        return req != null ? req.getRemoteAddr() : "-";
    }

    private HttpServletRequest request() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest();
        }
        return null;
    }
}
