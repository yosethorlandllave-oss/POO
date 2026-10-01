package pe.edu.upeu.sysventas.exception;

/**
 * Operación rechazada por una regla del negocio (duplicados, registros en uso...).
 * Opcionalmente indica el campo del formulario al que se refiere el problema.
 */
public class ReglaNegocioException extends RuntimeException {
    private final String campo;

    public ReglaNegocioException(String message) {
        this(message, null);
    }

    public ReglaNegocioException(String message, String campo) {
        super(message);
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }
}
