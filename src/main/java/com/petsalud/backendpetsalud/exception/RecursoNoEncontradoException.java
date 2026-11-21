package com.petsalud.backendpetsalud.exception;

/**
 * Semana 5: el recurso pedido (por ejemplo, un usuario por su id) no existe.
 * GlobalExceptionHandler la traduce a 404 Not Found.
 * Extiende IllegalArgumentException para no romper el codigo ni las pruebas que ya esperaban ese tipo;
 * Spring elige el manejador mas especifico, por eso esta responde 404 y no 400.
 */
public class RecursoNoEncontradoException extends IllegalArgumentException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
