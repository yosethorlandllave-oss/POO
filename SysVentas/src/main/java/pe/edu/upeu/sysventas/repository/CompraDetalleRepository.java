package pe.edu.upeu.sysventas.repository;

import pe.edu.upeu.sysventas.model.Compra;
import pe.edu.upeu.sysventas.model.CompraDetalle;
import pe.edu.upeu.sysventas.model.Producto;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

// Detalle de la compra (Yoseth): una fila por cada producto comprado.
public class CompraDetalleRepository extends AbstractJpaRepository<CompraDetalle, Long> {
    @Override protected String getTableName() { return "compra_detalle"; }
    @Override protected String getPkColumn()  { return "id_compra_detalle"; }

    @Override
    protected CompraDetalle mapRow(ResultSet rs) throws SQLException {
        Compra c = new Compra();
        c.setId(rs.getLong("id_compra"));
        Producto p = new Producto();
        p.setIdProducto(rs.getLong("id_producto"));
        return CompraDetalle.builder().idCompraDetalle(rs.getLong("id_compra_detalle"))
                .pu(rs.getDouble("pu")).cantidad(rs.getDouble("cantidad")).subtotal(rs.getDouble("subtotal"))
                .idCompra(c).idProducto(p).build();
    }

    @Override
    protected CompraDetalle insert(Connection conn, CompraDetalle e) throws SQLException {
        long id = executeInsertGetKey(conn,
                "INSERT INTO compra_detalle(pu,cantidad,subtotal,id_compra,id_producto) VALUES(?,?,?,?,?)",
                e.getPu(), e.getCantidad(), e.getSubtotal(),
                e.getIdCompra().getId(), e.getIdProducto().getIdProducto());
        e.setIdCompraDetalle(id);
        return e;
    }

    @Override
    protected CompraDetalle updateRow(Connection conn, CompraDetalle e) throws SQLException {
        executeUpdate(conn,
                "UPDATE compra_detalle SET pu=?,cantidad=?,subtotal=?,id_compra=?,id_producto=? WHERE id_compra_detalle=?",
                e.getPu(), e.getCantidad(), e.getSubtotal(),
                e.getIdCompra().getId(), e.getIdProducto().getIdProducto(), e.getIdCompraDetalle());
        return e;
    }
}
