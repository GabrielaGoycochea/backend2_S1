package com.petsalud.backendpetsalud.dto;

/**
 * Patrones de validacion de entrada (Semana 3), usados con @Pattern en los DTO.
 *
 * Primera barrera contra XSS y SQL Injection: el backend valida forma, largo y caracteres
 * permitidos antes de llegar al servicio, sin depender de lo que valide el front.
 * La segunda barrera contra SQL Injection es JPA/Hibernate, que siempre envia los valores
 * como parametros de la consulta (los repositorios no concatenan SQL ni usan consultas nativas).
 */
public final class Validaciones {

    /**
     * Texto libre (descripciones, diagnosticos, mensajes, motivos, direcciones): se acepta todo
     * menos los caracteres de HTML. Asi no se puede guardar un script que luego se ejecute en el
     * navegador de otra persona (XSS almacenado).
     */
    public static final String TEXTO_SEGURO = "^[^<>]*$";
    public static final String MSG_TEXTO_SEGURO = "no puede contener los caracteres < ni >";

    /**
     * Nombres, especies, razas y categorias: letras (con tildes y enie), numeros, espacios y . , ( ) -
     * Sin comillas, apostrofes, signo igual, punto y coma ni < >.
     */
    public static final String NOMBRE = "^[\\p{L}\\p{M}0-9 .,()-]*$";
    public static final String MSG_NOMBRE = "solo admite letras, numeros, espacios y . , ( ) -";

    /** Nombres y apellidos de personas: letras, espacios, punto y guion. */
    public static final String NOMBRE_PERSONA = "^[\\p{L}\\p{M} .-]*$";
    public static final String MSG_NOMBRE_PERSONA = "solo admite letras, espacios, punto y guion";

    /** Telefono: numeros, espacios y el signo +. */
    public static final String TELEFONO = "^[+0-9 ]*$";
    public static final String MSG_TELEFONO = "solo admite numeros, espacios y +";

    /** Estado de una cita (Agendada, Completada, Cancelada...): solo letras y espacios. */
    public static final String ESTADO = "^[\\p{L} ]{3,20}$";
    public static final String MSG_ESTADO = "solo admite letras (3 a 20)";

    private Validaciones() {
    }
}
