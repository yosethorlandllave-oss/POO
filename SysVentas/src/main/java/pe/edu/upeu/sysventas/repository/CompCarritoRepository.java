package pe.edu.upeu.sysventas.repository;

import pe.edu.upeu.sysventas.model.CompCarrito;
import pe.edu.upeu.sysventas.model.Producto;
import pe.edu.upeu.sysventas.model.Proveedor;
import pe.edu.upeu.sysventas.model.Usuario;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

// Carrito de compra (Yoseth): productos que se van agregando antes de registrar la compra.
public class CompCarritoRepository extends AbstractJpaRepository<CompCarrito, Long> {
    @Override protected String getTableName() { return "comp_carrito"; }
    @Override protected String getPkColumn()  { return "id_compcarrito"; }

    private static final String SELECT_JOIN =
            "SELECT cc.*, p.nombre AS prod_nombre " +
            "FROM comp_carrito cc " +
            "JOIN producto p ON cc.id_producto = p.id_producto ";

    @Override public List<CompCarrito> findAll() { return executeQuery(SELECT_JOIN); }

    @Override public Optional<CompCarrito> findById(Long id) {
        return executeQueryOne(SELECT_JOIN + "WHERE cc.id_compcarrito = ?", id);
    }

    @Override
    protected CompCarrito mapRow(ResultSet rs) throws SQLException {
        CompCarrito cc = new CompCarrito();
        cc.setIdCompcarrito(rs.getLong("id_compcarrito"));
        cc.setNombreProducto(rs.getString("nombre_producto"));
        cc.setCantidad(rs.getDouble("cantidad"));
        cc.setPunitario(rs.getDouble("punitario"));
        cc.setPtotal(rs.getDouble("ptotal"));
        cc.setEstado(rs.getInt("estado"));
        cc.setIdProducto(Producto.builder().idProducto(rs.getLong("id_producto"))
                .nombre(rs.getString("prod_nombre")).build());
        cc.setIdProveedor(Proveedor.builder().idProveedor(rs.getLong("id_proveedor")).build());
        Usuario u = new Usuario();
        u.setIdUsuario(rs.getLong("id_usuario"));
        cc.setIdUsuario(u);
        return cc;
    }

    @Override
    protected CompCarrito insert(Connection conn, CompCarrito e) throws SQLException {
        long id = executeInsertGetKey(conn,
                "INSERT INTO comp_carrito(id_proveedor,id_producto,nombre_producto,cantidad,punitario,ptotal,estado,id_usuario) VALUES(?,?,?,?,?,?,?,?)",
                e.getIdProveedor().getIdProveedor(), e.getIdProducto().getIdProducto(), e.getNombreProducto(),
                e.getCantidad(), e.getPunitario(), e.getPtotal(), e.getEstado(),
                e.getIdUsuario().getIdUsuario());
        e.setIdCompcarrito(id);
        return e;
    }

    @Override
    protected CompCarrito updateRow(Connection conn, CompCarrito e) throws SQLException {
        executeUpdate(conn,
                "UPDATE comp_carrito SET id_proveedor=?,id_producto=?,nombre_producto=?,cantidad=?,punitario=?,ptotal=?,estado=? WHERE id_compcarrito=?",
                e.getIdProveedor().getIdProveedor(), e.getIdProducto().getIdProducto(), e.getNombreProducto(),
                e.getCantidad(), e.getPunitario(), e.getPtotal(), e.getEstado(), e.getIdCompcarrito());
        return e;
    }

    public List<CompCarrito> listaCarritoProveedor(Long idProveedor) {
        return executeQuery(SELECT_JOIN + "WHERE cc.id_proveedor = ?", idProveedor);
    }

    public void deleteByProveedor(Long idProveedor) {
        executeUpdateStandalone("DELETE FROM comp_carrito WHERE id_proveedor = ?", idProveedor);
    }
}
