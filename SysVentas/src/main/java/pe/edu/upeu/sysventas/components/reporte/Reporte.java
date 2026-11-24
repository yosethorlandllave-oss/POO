package pe.edu.upeu.sysventas.components.reporte;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * DEFINICIÓN declarativa de un reporte. Se arma una vez y el componente se
 * encarga de pedir los parámetros, generar el PDF y mostrarlo.
 *
 * <pre>
 * Reporte r = Reporte.nuevo("Ventas por fecha")
 *     .empresa("Mi Empresa SAC", "RUC 20123456789", "Av. Principal 123")
 *     .rangoFechas(LocalDate.now().withDayOfMonth(1), LocalDate.now())
 *     .columna(ColumnaReporte.texto("Cliente", 4))
 *     .columna(ColumnaReporte.moneda("Total", 2).sumar())
 *     .datos(p -> miServicio.buscar((LocalDate) p.get("desde"), (LocalDate) p.get("hasta")))
 *     .build();
 * </pre>
 *
 * Partes del PDF: CABECERA (logo + empresa + título + filtros), CUERPO (tabla,
 * secciones extra, resumen) y PIE (notas, usuario, fecha, paginación).
 */
public final class Reporte {

    public enum Orientacion { VERTICAL, HORIZONTAL }

    /** Origen de datos: recibe los parámetros y devuelve filas (una por registro, una celda por columna). */
    @FunctionalInterface
    public interface FuenteDatos { List<Object[]> obtener(Map<String, Object> parametros) throws Exception; }

    /** Bloque libre que se puede insertar en el cuerpo (antes/después de la tabla). */
    @FunctionalInterface
    public interface Seccion { void escribir(com.lowagie.text.Document doc, ContextoReporte ctx) throws Exception; }

    /** Datos disponibles al dibujar secciones personalizadas y resumen. */
    public record ContextoReporte(Reporte reporte, Map<String, Object> parametros, List<Object[]> filas) {}

    String titulo, subtitulo = "";
    List<String> lineasEmpresa = new ArrayList<>();
    String logoRecurso;                 // ruta en classpath (p.ej. /img/store.png)
    byte[] logoBytes;
    Orientacion orientacion = Orientacion.VERTICAL;
    final List<ParametroReporte> parametros = new ArrayList<>();
    final List<ColumnaReporte> columnas = new ArrayList<>();
    FuenteDatos fuente = p -> List.of();
    int columnaAgrupar = -1;
    boolean numerarFilas = true, mostrarFiltros = true, mostrarTotales = true;
    String colorPrimarioHex = "#2c3e50", colorAcentoHex = "#c0392b";
    final List<String> notasPie = new ArrayList<>();
    String usuario = "";
    Function<List<Object[]>, Map<String, String>> resumen;
    final List<Seccion> seccionesAntes = new ArrayList<>(), seccionesDespues = new ArrayList<>();

    private Reporte() {}

    public static Builder nuevo(String titulo) {
        Reporte r = new Reporte();
        r.titulo = titulo;
        return new Builder(r);
    }

    public String titulo() { return titulo; }
    public List<ParametroReporte> parametros() { return Collections.unmodifiableList(parametros); }
    public List<ColumnaReporte> columnas() { return Collections.unmodifiableList(columnas); }
    public FuenteDatos fuente() { return fuente; }

    public static final class Builder {
        private final Reporte r;
        private Builder(Reporte r) { this.r = r; }

        // ---- CABECERA ----
        public Builder subtitulo(String s) { r.subtitulo = s; return this; }
        /** Líneas de la empresa junto al logo: razón social, RUC, dirección... */
        public Builder empresa(String... lineas) { r.lineasEmpresa = new ArrayList<>(List.of(lineas)); return this; }
        public Builder logoRecurso(String rutaClasspath) { r.logoRecurso = rutaClasspath; return this; }
        public Builder logo(byte[] bytes) { r.logoBytes = bytes; return this; }
        public Builder colores(String primarioHex, String acentoHex) { r.colorPrimarioHex = primarioHex; r.colorAcentoHex = acentoHex; return this; }
        public Builder mostrarFiltros(boolean v) { r.mostrarFiltros = v; return this; }

        // ---- PARÁMETROS ----
        public Builder parametro(ParametroReporte p) { r.parametros.add(p); return this; }
        /** Atajo: agrega los parámetros "desde" y "hasta". */
        public Builder rangoFechas(LocalDate desde, LocalDate hasta) {
            r.parametros.add(ParametroReporte.fecha("desde", "Desde", desde));
            r.parametros.add(ParametroReporte.fecha("hasta", "Hasta", hasta));
            return this;
        }

        // ---- CUERPO ----
        public Builder orientacion(Orientacion o) { r.orientacion = o; return this; }
        public Builder columna(ColumnaReporte c) { r.columnas.add(c); return this; }
        public Builder datos(FuenteDatos f) { r.fuente = f; return this; }
        /** Agrupa filas consecutivas por el valor de la columna (índice base 0), con subtotales. */
        public Builder agruparPor(int indiceColumna) { r.columnaAgrupar = indiceColumna; return this; }
        public Builder numerarFilas(boolean v) { r.numerarFilas = v; return this; }
        public Builder mostrarTotales(boolean v) { r.mostrarTotales = v; return this; }
        /** Cuadro de indicadores (etiqueta → valor) calculado a partir de las filas. */
        public Builder resumen(Function<List<Object[]>, Map<String, String>> f) { r.resumen = f; return this; }
        public Builder seccionAntesDeTabla(Seccion s) { r.seccionesAntes.add(s); return this; }
        public Builder seccionDespuesDeTabla(Seccion s) { r.seccionesDespues.add(s); return this; }

        // ---- PIE ----
        public Builder notaPie(String nota) { r.notasPie.add(nota); return this; }
        public Builder usuario(String u) { r.usuario = u; return this; }

        public Reporte build() {
            if (r.columnas.isEmpty()) throw new IllegalStateException("El reporte necesita al menos una columna");
            return r;
        }
    }
}
