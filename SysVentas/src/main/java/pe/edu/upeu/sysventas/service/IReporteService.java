package pe.edu.upeu.sysventas.service;

import pe.edu.upeu.sysventas.components.reporte.Reporte;

import java.util.List;

/**
 * Reportes en PDF (componente de la Sesión 16 del docente), adaptados a Coolbox.
 */
public interface IReporteService {
    /** Ventas por rango de fechas, con el número de boleta, el cajero y el ahorro por promociones. */
    Reporte reporteVentas();

    /** Ranking de productos más vendidos en el periodo. */
    Reporte reporteMasVendidos();

    /** Inventario valorizado por categoría, con stock bajo y productos en oferta. */
    Reporte reporteInventario();

    /** Compras a proveedores por rango de fechas. */
    Reporte reporteCompras();

    /** Todos los reportes que se ofrecen en la pantalla de reportes. */
    List<Reporte> reportesDisponibles();
}
