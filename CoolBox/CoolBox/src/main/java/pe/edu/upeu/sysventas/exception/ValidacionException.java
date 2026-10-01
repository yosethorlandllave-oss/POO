package pe.edu.upeu.sysventas.exception;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * El objeto no cumple las anotaciones de Bean Validation del modelo.
 * Los errores se agrupan por nombre de propiedad, en orden alfabético.
 */
public class ValidacionException extends RuntimeException {
    private final Map<String, String> errores;

    public ValidacionException(Map<String, String> errores) {
        super(errores.values().iterator().next());
        this.errores = Collections.unmodifiableMap(new LinkedHashMap<>(errores));
    }

    public Map<String, String> getErrores() {
        return errores;
    }
}
