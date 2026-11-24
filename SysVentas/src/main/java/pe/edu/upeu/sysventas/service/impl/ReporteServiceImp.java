package pe.edu.upeu.sysventas.service.impl;

import pe.edu.upeu.sysventas.components.reporte.ColumnaReporte;
import pe.edu.upeu.sysventas.components.reporte.ParametroReporte;
import pe.edu.upeu.sysventas.components.reporte.Reporte;
import pe.edu.upeu.sysventas.dto.SessionManager;
import pe.edu.upeu.sysventas.repository.ReporteRepository;
import pe.edu.upeu.sysventas.service.IReporteService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reportes PDF de Coolbox. Usa el componente de reportes del docente (Sesión 16):
 * cada reporte se DECLARA (título, filtros, columnas, datos) y el ReportePanel
 * arma los filtros, genera el PDF y lo muestra.
 */
public class ReporteServiceImp implements IReporteService {

    private static final String[] EMPRESA = {
            "Coolbox Juliaca",
            "RUC 20123456789 · Jr. San Roman 123 - Juliaca",
            "Tienda de tecnología · Tel. 951782520"};
    private static final String LOGO = "/img/logo-coolbox.png";
    private static final String NEGRO = "#1A1A1A", ROJO = "#E30613";
    /** Mismo límite que pinta de rojo el stock en la tabla de productos. */
    private static final double STOCK_BAJO = 10;

    private final ReporteRepository reporteRepository;

    public ReporteServiceImp(ReporteRepository reporteRepository) {
        this.reporteRepository = reporteRepository;
    }

    @Override
    public List<Reporte> reportesDisponibles() {
        return List.of(reporteVentas(), reporteMasVendidos(), reporteInventario(), reporteCompras());
    }

    @Override
    public Reporte reporteVentas() {
        LocalDate hoy = LocalDate.now();
        List<String> cajeros = new ArrayList<>();
        cajeros.add("Todos");
        try { cajeros.addAll(reporteRepository.usuarios()); } catch (Exception ignored) { }
        return base("Reporte de Ventas")
                .subtitulo("Boletas emitidas en el periodo")
                .rangoFechas(hoy.withDayOfMonth(1), hoy)
                .parametro(ParametroReporte.texto("cliente", "Cliente (nombre o DNI)"))
                .parametro(ParametroReporte.lista("cajero", "Cajero", cajeros))
                .orientacion(Reporte.Orientacion.HORIZONTAL)
                .columna(ColumnaReporte.fecha("Fecha", 1.2f))
                .columna(ColumnaReporte.texto("Boleta", 1.7f))
                .columna(ColumnaReporte.texto("Cliente", 4f))
                .columna(ColumnaReporte.texto("Cajero", 1.3f))
                .columna(ColumnaReporte.moneda("Ahorro promo", 1.5f).sumar())
                .columna(ColumnaReporte.moneda("Base", 1.4f).sumar())
                .columna(ColumnaReporte.moneda("IGV", 1.2f).sumar())
                .columna(ColumnaReporte.moneda("Total", 1.5f).sumar())
                .resumen(filas -> {
                    double total = suma(filas, 7), ahorro = suma(filas, 4);
                    Map<String, String> r = new LinkedHashMap<>();
                    r.put("Boletas emitidas", String.valueOf(filas.size()));
                    r.put("Venta total", soles(total));
                    r.put("Ticket promedio", soles(filas.isEmpty() ? 0 : total / filas.size()));
                    r.put("Ahorro dado con promociones", soles(ahorro));
                    return r;
                })
                .notaPie("Montos en soles (PEN), incluyen IGV 18%. El ahorro es (precio antes - precio de venta) x cantidad.")
                .datos(p -> {
                    String cajero = (String) p.get("cajero");
                    return reporteRepository.ventasCoolbox(
                            (LocalDate) p.get("desde"), (LocalDate) p.get("hasta"),
                            (String) p.get("cliente"),
                            cajero == null || "Todos".equals(cajero) ? null : cajero);
                })
                .build();
    }

    @Override
    public Reporte reporteMasVendidos() {
        LocalDate hoy = LocalDate.now();
        return base("Productos más vendidos")
                .subtitulo("Ranking por unidades vendidas")
                .rangoFechas(hoy.withDayOfMonth(1), hoy)
                .parametro(ParametroReporte.texto("categoria", "Categoría contiene"))
                .parametro(ParametroReporte.numero("top", "Cuántos mostrar").inicial(10))
                .columna(ColumnaReporte.texto("Producto", 4f))
                .columna(ColumnaReporte.texto("Categoría", 1.8f))
                .columna(ColumnaReporte.texto("Marca", 1.5f))
                .columna(ColumnaReporte.entero("Unidades", 1.2f).sumar())
                .columna(ColumnaReporte.moneda("Vendido", 1.6f).sumar())
                .columna(ColumnaReporte.moneda("Ahorro promo", 1.6f).sumar())
                .resumen(filas -> {
                    Map<String, String> r = new LinkedHashMap<>();
                    r.put("Producto estrella", filas.isEmpty() ? "-" : String.valueOf(filas.get(0)[0]));
                    r.put("Unidades vendidas", String.valueOf((long) suma(filas, 3)));
                    r.put("Total vendido", soles(suma(filas, 4)));
                    return r;
                })
                .notaPie("Solo cuenta productos vendidos en boletas registradas.")
                .datos(p -> {
                    Double top = (Double) p.get("top");
                    int n = (top == null || top < 1) ? 10 : (int) Math.min(top, 100);
                    return reporteRepository.masVendidos(
                            (LocalDate) p.get("desde"), (LocalDate) p.get("hasta"),
                            (String) p.get("categoria"), n);
                })
                .build();
    }

    @Override
    public Reporte reporteInventario() {
        return base("Inventario de Productos")
                .subtitulo("Existencias valorizadas por categoría")
                .parametro(ParametroReporte.texto("producto", "Producto o marca contiene"))
                .parametro(ParametroReporte.booleano("soloBajo", "Solo stock bajo (menos de " + (int) STOCK_BAJO + ")", false))
                .parametro(ParametroReporte.booleano("soloOfertas", "Solo productos en oferta", false))
                .orientacion(Reporte.Orientacion.HORIZONTAL)
                .columna(ColumnaReporte.texto("Categoría", 1.8f))
                .columna(ColumnaReporte.texto("Producto", 4f))
                .columna(ColumnaReporte.texto("Marca", 1.5f))
                .columna(ColumnaReporte.entero("Stock", 1f).sumar())
                .columna(ColumnaReporte.moneda("Antes", 1.4f))
                .columna(ColumnaReporte.moneda("P. Unit.", 1.4f))
                .columna(ColumnaReporte.moneda("Valor", 1.7f).sumar())
                .agruparPor(0)
                .resumen(filas -> {
                    long bajos = filas.stream().filter(f -> ColumnaReporte.aNumero(f[3]) < STOCK_BAJO).count();
                    long ofertas = filas.stream().filter(f -> f[4] != null).count();
                    Map<String, String> r = new LinkedHashMap<>();
                    r.put("Productos", String.valueOf(filas.size()));
                    r.put("Con stock bajo", String.valueOf(bajos));
                    r.put("En oferta", String.valueOf(ofertas));
                    r.put("Valor del inventario", soles(suma(filas, 6)));
                    return r;
                })
                .notaPie("\"Antes\" aparece solo en productos en oferta (precio anterior mayor al actual).")
                .datos(p -> reporteRepository.inventarioCoolbox(
                        (String) p.get("producto"),
                        Boolean.TRUE.equals(p.get("soloBajo")), STOCK_BAJO,
                        Boolean.TRUE.equals(p.get("soloOfertas"))))
                .build();
    }

    @Override
    public Reporte reporteCompras() {
        LocalDate hoy = LocalDate.now();
        return base("Reporte de Compras")
                .subtitulo("Compras a proveedores en el periodo")
                .rangoFechas(hoy.withDayOfMonth(1), hoy)
                .parametro(ParametroReporte.lista("tipo", "Tipo de documento", List.of("Todos", "Boleta", "Factura", "Guia")))
                .parametro(ParametroReporte.texto("proveedor", "Proveedor (nombre o RUC)"))
                .orientacion(Reporte.Orientacion.HORIZONTAL)
                .columna(ColumnaReporte.fecha("Fecha", 1.2f))
                .columna(ColumnaReporte.texto("Documento", 2f))
                .columna(ColumnaReporte.texto("Proveedor", 4f))
                .columna(ColumnaReporte.texto("Usuario", 1.3f))
                .columna(ColumnaReporte.moneda("Base", 1.4f).sumar())
                .columna(ColumnaReporte.moneda("IGV", 1.2f).sumar())
                .columna(ColumnaReporte.moneda("Total", 1.5f).sumar())
                .resumen(filas -> {
                    Map<String, String> r = new LinkedHashMap<>();
                    r.put("Compras", String.valueOf(filas.size()));
                    r.put("Total comprado", soles(suma(filas, 6)));
                    return r;
                })
                .notaPie("Cada compra registrada aumentó el stock de sus productos.")
                .datos(p -> {
                    String tipo = (String) p.get("tipo");
                    return reporteRepository.compras(
                            (LocalDate) p.get("desde"), (LocalDate) p.get("hasta"),
                            tipo == null || "Todos".equals(tipo) ? null : tipo,
                            (String) p.get("proveedor"));
                })
                .build();
    }

    // ---------------------------------------------------------------- ayudas

    /** Cabecera común: logo, datos de la tienda, colores Coolbox y usuario que genera. */
    private Reporte.Builder base(String titulo) {
        return Reporte.nuevo(titulo)
                .empresa(EMPRESA)
                .logoRecurso(LOGO)
                .colores(NEGRO, ROJO)
                .usuario(usuario());
    }

    private static double suma(List<Object[]> filas, int col) {
        return filas.stream().mapToDouble(f -> ColumnaReporte.aNumero(f[col])).sum();
    }

    private static String soles(double v) {
        return String.format("S/ %,.2f", v);
    }

    private static String usuario() {
        String u = SessionManager.getInstance().getUserName();
        return u == null ? "" : u;
    }
}
