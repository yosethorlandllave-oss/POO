package pe.edu.upeu.sysventas.components.reporte;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Describe una columna de la tabla del cuerpo del reporte:
 * título, ancho relativo, formato, alineación y si se totaliza.
 */
public final class ColumnaReporte {

    public enum Formato { TEXTO, ENTERO, DECIMAL, MONEDA, PORCENTAJE, FECHA, FECHA_HORA }
    public enum Alineacion { IZQUIERDA, CENTRO, DERECHA, AUTO }

    private static final DateTimeFormatter F_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter F_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DecimalFormatSymbols SIMBOLOS = new DecimalFormatSymbols(Locale.US);

    private final String titulo;
    private float ancho = 1f;
    private Formato formato = Formato.TEXTO;
    private Alineacion alineacion = Alineacion.AUTO;
    private boolean sumar;

    private ColumnaReporte(String titulo) { this.titulo = titulo; }

    public static ColumnaReporte de(String titulo) { return new ColumnaReporte(titulo); }
    public static ColumnaReporte texto(String titulo, float ancho) { return de(titulo).ancho(ancho); }
    public static ColumnaReporte entero(String titulo, float ancho) { return de(titulo).ancho(ancho).formato(Formato.ENTERO); }
    public static ColumnaReporte decimal(String titulo, float ancho) { return de(titulo).ancho(ancho).formato(Formato.DECIMAL); }
    public static ColumnaReporte moneda(String titulo, float ancho) { return de(titulo).ancho(ancho).formato(Formato.MONEDA); }
    public static ColumnaReporte fecha(String titulo, float ancho) { return de(titulo).ancho(ancho).formato(Formato.FECHA); }

    public ColumnaReporte ancho(float ancho) { this.ancho = ancho; return this; }
    public ColumnaReporte formato(Formato f) { this.formato = f; return this; }
    public ColumnaReporte alineacion(Alineacion a) { this.alineacion = a; return this; }
    /** Marca la columna para que aparezca en la fila de totales (y subtotales si hay agrupación). */
    public ColumnaReporte sumar() { this.sumar = true; return this; }

    public String titulo() { return titulo; }
    public float ancho() { return ancho; }
    public Formato formato() { return formato; }
    public boolean esSumable() { return sumar; }

    public Alineacion alineacionEfectiva() {
        if (alineacion != Alineacion.AUTO) return alineacion;
        return switch (formato) {
            case ENTERO, DECIMAL, MONEDA, PORCENTAJE -> Alineacion.DERECHA;
            case FECHA, FECHA_HORA -> Alineacion.CENTRO;
            default -> Alineacion.IZQUIERDA;
        };
    }

    /** Convierte el valor de una celda a texto según el formato de la columna. */
    public String formatear(Object v) {
        if (v == null) return "";
        try {
            return switch (formato) {
                case ENTERO -> new DecimalFormat("#,##0", SIMBOLOS).format(aNumero(v));
                case DECIMAL -> new DecimalFormat("#,##0.00", SIMBOLOS).format(aNumero(v));
                case MONEDA -> "S/ " + new DecimalFormat("#,##0.00", SIMBOLOS).format(aNumero(v));
                case PORCENTAJE -> new DecimalFormat("#,##0.00", SIMBOLOS).format(aNumero(v)) + " %";
                case FECHA -> v instanceof LocalDate d ? d.format(F_FECHA)
                        : v instanceof LocalDateTime dt ? dt.format(F_FECHA)
                        : v instanceof java.sql.Date sd ? sd.toLocalDate().format(F_FECHA) : v.toString();
                case FECHA_HORA -> v instanceof LocalDateTime dt ? dt.format(F_FECHA_HORA)
                        : v instanceof java.sql.Timestamp ts ? ts.toLocalDateTime().format(F_FECHA_HORA)
                        : v instanceof LocalDate d ? d.format(F_FECHA) : v.toString();
                default -> v.toString();
            };
        } catch (RuntimeException e) {
            return v.toString();
        }
    }

    public static double aNumero(Object v) {
        if (v == null) return 0;
        if (v instanceof Number n) return n.doubleValue();
        if (v instanceof BigDecimal b) return b.doubleValue();
        return Double.parseDouble(v.toString().trim());
    }
}
