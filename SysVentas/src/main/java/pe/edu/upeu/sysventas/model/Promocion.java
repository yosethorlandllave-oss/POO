package pe.edu.upeu.sysventas.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Promoción de la tienda (tabla promocion).
 * La descripción puede llevar una etiqueta antes de "|", por ejemplo:
 * "¡La espera terminó!|PS5 código GTA VI. S/ 299 Antes: S/ 449"
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Promocion {
    private Long idPromocion;
    private String nombre;
    private String descripcion;
    private Double porcentaje;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private String estado;

    /** Etiqueta amarilla del banner (lo que va antes de "|"). */
    public String getEtiqueta() {
        if (descripcion != null && descripcion.contains("|")) {
            return descripcion.substring(0, descripcion.indexOf('|')).trim();
        }
        return "Promoción Coolbox";
    }

    /** Detalle de la promoción (lo que va después de "|"). */
    public String getDetalle() {
        if (descripcion == null) return "";
        return descripcion.contains("|") ? descripcion.substring(descripcion.indexOf('|') + 1).trim() : descripcion.trim();
    }

    /** Está activa y la fecha de hoy está dentro de su vigencia. */
    public boolean isVigente() {
        LocalDate hoy = LocalDate.now();
        return "ACTIVO".equalsIgnoreCase(estado)
                && fechaInicio != null && fechaFin != null
                && !hoy.isBefore(fechaInicio) && !hoy.isAfter(fechaFin);
    }
}
