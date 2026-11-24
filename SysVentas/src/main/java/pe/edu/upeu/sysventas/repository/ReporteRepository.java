package pe.edu.upeu.sysventas.repository;

import pe.edu.upeu.sysventas.repository.helper.SqlHelper;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


public class ReporteRepository extends SqlHelper<Object[]> {

    @Override
    protected Object[] mapRow(ResultSet rs) throws SQLException {
        ResultSetMetaData md = rs.getMetaData();
        Object[] fila = new Object[md.getColumnCount()];
        for (int i = 0; i < fila.length; i++) fila[i] = rs.getObject(i + 1);
        return fila;
    }

    /**
     * Columnas: fecha, documento (serie-número), tipo, cliente, base, igv, total.
     * Los filtros tipoDoc y cliente son opcionales (null = sin filtrar).
     */
    public List<Object[]> ventasPorFecha(LocalDate desde, LocalDate hasta, String tipoDoc, String cliente) {
        StringBuilder sql = new StringBuilder(
                "SELECT v.fecha_gener, v.serie || '-' || v.num_doc, v.tipo_doc, c.nombres, " +
                "v.precio_base, v.igv, v.precio_total " +
                "FROM venta v JOIN cliente c ON c.dniruc = v.dniruc " +
                "WHERE v.fecha_gener BETWEEN ? AND ? ");
        List<Object> args = new ArrayList<>(List.of(java.sql.Date.valueOf(desde), java.sql.Date.valueOf(hasta)));
        if (tipoDoc != null) { sql.append("AND UPPER(v.tipo_doc) = UPPER(?) "); args.add(tipoDoc); }
        if (cliente != null) { sql.append("AND UPPER(c.nombres) LIKE UPPER(?) "); args.add("%" + cliente + "%"); }
        sql.append("ORDER BY v.fecha_gener, v.id_venta");
        return executeQuery(sql.toString(), args.toArray());
    }

    /**
     * Columnas: categoría, producto, stock, precio unitario, valor (stock * pu).
     * Todos los filtros son opcionales (null / false = sin filtrar).
     */
    public List<Object[]> inventario(String producto, Double stockMax, boolean soloStockBajo, double umbralBajo) {
        StringBuilder sql = new StringBuilder(
                "SELECT ca.nombre, pr.nombre, pr.stock, pr.pu, pr.stock * pr.pu " +
                "FROM producto pr JOIN categoria ca ON ca.id_categoria = pr.id_categoria WHERE 1=1 ");
        List<Object> args = new ArrayList<>();
        if (producto != null) { sql.append("AND UPPER(pr.nombre) LIKE UPPER(?) "); args.add("%" + producto + "%"); }
        if (stockMax != null) { sql.append("AND pr.stock <= ? "); args.add(stockMax); }
        if (soloStockBajo) { sql.append("AND pr.stock <= ? "); args.add(umbralBajo); }
        sql.append("ORDER BY ca.nombre, pr.nombre");
        return executeQuery(sql.toString(), args.toArray());
    }

    // ======================= Reportes Coolbox (Grupo G2) =======================

    /**
     * Ventas de Coolbox. El documento se muestra igual que en la boleta: B001-00000007.
     * Columnas: fecha, documento, cliente, cajero, ahorro por promociones, base, igv, total.
     */
    public List<Object[]> ventasCoolbox(LocalDate desde, LocalDate hasta, String cliente, String cajero) {
        StringBuilder sql = new StringBuilder(
                "SELECT v.fecha_gener, 'B001-' || LPAD(CAST(v.id_venta AS VARCHAR), 8, '0'), " +
                "c.dniruc || ' - ' || c.nombres, u.usuario, " +
                "COALESCE((SELECT SUM(d.descuento) FROM venta_detalle d WHERE d.id_venta = v.id_venta), 0), " +
                "v.precio_base, v.igv, v.precio_total " +
                "FROM venta v JOIN cliente c ON c.dniruc = v.dniruc JOIN usuario u ON u.id_usuario = v.id_usuario " +
                "WHERE v.fecha_gener BETWEEN ? AND ? ");
        List<Object> args = new ArrayList<>(List.of(java.sql.Date.valueOf(desde), java.sql.Date.valueOf(hasta)));
        if (cliente != null) {
            sql.append("AND (UPPER(c.nombres) LIKE UPPER(?) OR c.dniruc LIKE ?) ");
            args.add("%" + cliente + "%");
            args.add("%" + cliente + "%");
        }
        if (cajero != null) { sql.append("AND UPPER(u.usuario) = UPPER(?) "); args.add(cajero); }
        sql.append("ORDER BY v.fecha_gener, v.id_venta");
        return executeQuery(sql.toString(), args.toArray());
    }

    /**
     * Productos más vendidos en el periodo.
     * Columnas: producto, categoría, marca, unidades, total vendido, ahorro dado al cliente.
     */
    public List<Object[]> masVendidos(LocalDate desde, LocalDate hasta, String categoria, int top) {
        StringBuilder sql = new StringBuilder(
                "SELECT pr.nombre, ca.nombre, ma.nombre, SUM(d.cantidad), SUM(d.subtotal), SUM(d.descuento) " +
                "FROM venta_detalle d JOIN venta v ON v.id_venta = d.id_venta " +
                "JOIN producto pr ON pr.id_producto = d.id_producto " +
                "JOIN categoria ca ON ca.id_categoria = pr.id_categoria " +
                "JOIN marca ma ON ma.id_marca = pr.id_marca " +
                "WHERE v.fecha_gener BETWEEN ? AND ? ");
        List<Object> args = new ArrayList<>(List.of(java.sql.Date.valueOf(desde), java.sql.Date.valueOf(hasta)));
        if (categoria != null) { sql.append("AND UPPER(ca.nombre) LIKE UPPER(?) "); args.add("%" + categoria + "%"); }
        sql.append("GROUP BY pr.nombre, ca.nombre, ma.nombre ORDER BY SUM(d.cantidad) DESC, SUM(d.subtotal) DESC ");
        sql.append("FETCH FIRST ").append(Math.max(1, top)).append(" ROWS ONLY");   // top es un int: no hay riesgo de inyección
        return executeQuery(sql.toString(), args.toArray());
    }

    /**
     * Inventario Coolbox con promociones y stock bajo.
     * Columnas: categoría, producto, marca, stock, precio antes (si está en oferta), precio actual, valor.
     */
    public List<Object[]> inventarioCoolbox(String producto, boolean soloStockBajo, double umbralBajo, boolean soloOfertas) {
        StringBuilder sql = new StringBuilder(
                "SELECT ca.nombre, pr.nombre, ma.nombre, pr.stock, " +
                "CASE WHEN pr.puold > pr.pu THEN pr.puold ELSE NULL END, pr.pu, pr.stock * pr.pu " +
                "FROM producto pr JOIN categoria ca ON ca.id_categoria = pr.id_categoria " +
                "JOIN marca ma ON ma.id_marca = pr.id_marca WHERE 1=1 ");
        List<Object> args = new ArrayList<>();
        if (producto != null) {
            sql.append("AND (UPPER(pr.nombre) LIKE UPPER(?) OR UPPER(ma.nombre) LIKE UPPER(?)) ");
            args.add("%" + producto + "%");
            args.add("%" + producto + "%");
        }
        if (soloStockBajo) { sql.append("AND pr.stock < ? "); args.add(umbralBajo); }
        if (soloOfertas) sql.append("AND pr.puold > pr.pu ");
        sql.append("ORDER BY ca.nombre, pr.nombre");
        return executeQuery(sql.toString(), args.toArray());
    }

    /**
     * Compras a proveedores.
     * Columnas: fecha, documento (tipo serie-número), proveedor, usuario, base, igv, total.
     */
    public List<Object[]> compras(LocalDate desde, LocalDate hasta, String tipoDoc, String proveedor) {
        StringBuilder sql = new StringBuilder(
                "SELECT co.fecha_comp, co.tipo_doc || ' ' || co.serie || '-' || co.num_doc, " +
                "pv.dniruc || ' - ' || pv.nombres_raso, u.usuario, co.precio_base, co.igv, co.precio_total " +
                "FROM compra co JOIN proveedor pv ON pv.id_proveedor = co.id_proveedor " +
                "JOIN usuario u ON u.id_usuario = co.id_usuario " +
                "WHERE co.fecha_comp BETWEEN ? AND ? ");
        List<Object> args = new ArrayList<>(List.of(java.sql.Date.valueOf(desde), java.sql.Date.valueOf(hasta)));
        if (tipoDoc != null) { sql.append("AND UPPER(co.tipo_doc) = UPPER(?) "); args.add(tipoDoc); }
        if (proveedor != null) {
            sql.append("AND (UPPER(pv.nombres_raso) LIKE UPPER(?) OR pv.dniruc LIKE ?) ");
            args.add("%" + proveedor + "%");
            args.add("%" + proveedor + "%");
        }
        sql.append("ORDER BY co.fecha_comp, co.id_compra");
        return executeQuery(sql.toString(), args.toArray());
    }

    /** Usuarios para el filtro "Cajero" del reporte de ventas. */
    public List<String> usuarios() {
        List<String> lista = new ArrayList<>();
        for (Object[] f : executeQuery("SELECT usuario FROM usuario ORDER BY usuario")) lista.add(String.valueOf(f[0]));
        return lista;
    }
}
