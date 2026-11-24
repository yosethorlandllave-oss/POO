package pe.edu.upeu.sysventas.components.reporte;

import java.time.LocalDate;
import java.util.List;

/**
 * Parámetro de entrada del reporte (filtro). El {@link ReportePanel} genera
 * automáticamente el control adecuado según el {@link Tipo}.
 * El valor llega a la fuente de datos en un {@code Map<clave, valor>}:
 * FECHA → LocalDate, TEXTO/LISTA → String, NUMERO → Double, BOOLEANO → Boolean.
 */
public final class ParametroReporte {

    public enum Tipo { FECHA, TEXTO, NUMERO, LISTA, BOOLEANO }

    private final String clave;
    private final String etiqueta;
    private final Tipo tipo;
    private Object valorInicial;
    private List<String> opciones = List.of();
    private boolean requerido;

    private ParametroReporte(String clave, String etiqueta, Tipo tipo) {
        this.clave = clave; this.etiqueta = etiqueta; this.tipo = tipo;
    }

    public static ParametroReporte fecha(String clave, String etiqueta, LocalDate inicial) {
        ParametroReporte p = new ParametroReporte(clave, etiqueta, Tipo.FECHA);
        p.valorInicial = inicial; p.requerido = true;
        return p;
    }
    public static ParametroReporte texto(String clave, String etiqueta) {
        return new ParametroReporte(clave, etiqueta, Tipo.TEXTO);
    }
    public static ParametroReporte numero(String clave, String etiqueta) {
        return new ParametroReporte(clave, etiqueta, Tipo.NUMERO);
    }
    public static ParametroReporte lista(String clave, String etiqueta, List<String> opciones) {
        ParametroReporte p = new ParametroReporte(clave, etiqueta, Tipo.LISTA);
        p.opciones = List.copyOf(opciones);
        if (!opciones.isEmpty()) p.valorInicial = opciones.get(0);
        return p;
    }
    public static ParametroReporte booleano(String clave, String etiqueta, boolean inicial) {
        ParametroReporte p = new ParametroReporte(clave, etiqueta, Tipo.BOOLEANO);
        p.valorInicial = inicial;
        return p;
    }

    public ParametroReporte requerido() { this.requerido = true; return this; }
    public ParametroReporte inicial(Object v) { this.valorInicial = v; return this; }

    public String clave() { return clave; }
    public String etiqueta() { return etiqueta; }
    public Tipo tipo() { return tipo; }
    public Object valorInicial() { return valorInicial; }
    public List<String> opciones() { return opciones; }
    public boolean esRequerido() { return requerido; }
}
