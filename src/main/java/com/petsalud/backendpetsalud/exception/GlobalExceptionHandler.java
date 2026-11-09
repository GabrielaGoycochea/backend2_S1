package com.petsalud.backendpetsalud.exception;

import com.petsalud.backendpetsalud.security.ActividadSospechosaService;
import com.petsalud.backendpetsalud.security.AuditoriaRequestFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

/**
 * Respuestas de error uniformes y sin detalles internos (sin stack trace ni nombres de clases).
 * Semana 3: cada entrada rechazada por validacion queda en el log de seguridad como
 * ENTRADA_RECHAZADA, solo con los nombres de los campos (nunca los valores enviados).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final ActividadSospechosaService actividad;

    public GlobalExceptionHandler(ActividadSospechosaService actividad) {
        this.actividad = actividad;
    }

    /** Reglas de negocio (email repetido, recurso inexistente, etc.): 400 en vez de 500. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> manejarArgumentoInvalido(
            IllegalArgumentException ex) {

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                Map.of(
                        "status", 400,
                        "error", "Bad Request",
                        "message", ex.getMessage() != null ? ex.getMessage() : "Solicitud invalida"
                )
        );
    }

    /** Cuerpo JSON que no cumple las reglas del DTO (@Valid): por ejemplo, un script en un nombre. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> manejarValidacion(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        List<String> campos = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getField)
                .distinct()
                .toList();
        return rechazar(request, campos, "Los datos enviados no cumplen las validaciones");
    }

    /** Parametros de la URL (@RequestParam) que no cumplen @Pattern, @Size, etc. */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<Map<String, Object>> manejarValidacionDeParametros(
            HandlerMethodValidationException ex, HttpServletRequest request) {

        List<String> campos = ex.getAllValidationResults().stream()
                .map(r -> r.getMethodParameter().getParameterName())
                .distinct()
                .toList();
        return rechazar(request, campos, "Los parametros enviados no cumplen las validaciones");
    }

    /** Tipo incorrecto, por ejemplo "/api/mascotas/1 OR 1=1" donde se espera un numero. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> manejarTipoInvalido(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

        return rechazar(request, List.of(ex.getName()), "Parametro con formato invalido");
    }

    /** Fecha u hora mal escrita en los parametros de citas. */
    @ExceptionHandler(DateTimeParseException.class)
    public ResponseEntity<Map<String, Object>> manejarFechaInvalida(
            DateTimeParseException ex, HttpServletRequest request) {

        return rechazar(request, List.of("fecha"), "Fecha con formato invalido (use AAAA-MM-DDTHH:MM:SS)");
    }

    private ResponseEntity<Map<String, Object>> rechazar(HttpServletRequest request, List<String> campos,
                                                         String mensaje) {
        actividad.entradaRechazada(request, AuditoriaRequestFilter.usuarioActual(), campos);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                Map.of(
                        "status", 400,
                        "error", "Bad Request",
                        "message", mensaje,
                        "campos", campos
                )
        );
    }
}
