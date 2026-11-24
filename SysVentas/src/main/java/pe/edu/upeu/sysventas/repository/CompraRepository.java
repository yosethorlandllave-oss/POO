package pe.edu.upeu.sysventas.repository;

import pe.edu.upeu.sysventas.model.Compra;
import pe.edu.upeu.sysventas.model.Proveedor;
import pe.edu.upeu.sysventas.model.Usuario;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

// Cabecera de la compra (Yoseth): una fila por factura del proveedor.
public class CompraRepository extends AbstractJpaRepository<Compra, Long> {
    @Override protected String getTableName() { return "compra"; }
    @Override protected String getPkColumn()  { return "id_compra"; }

    @Override
    protected Compra mapRow(ResultSet rs) throws SQLException {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(rs.getLong("id_usuario"));
        return Compra.builder()
                .id(rs.getLong("id_compra"))
                .precioBase(rs.getDouble("precio_base"))
                .igv(rs.getDouble("igv"))
                .precioTotal(rs.getDouble("precio_total"))
                .serie(rs.getString("serie"))
                .numDoc(rs.getString("num_doc"))
                .tipoDoc(rs.getString("tipo_doc"))
                .fechaComp(rs.getDate("fecha_comp").toLocalDate())
                .fechaReg(rs.getDate("fecha_reg").toLocalDate())
                .idProveedor(Proveedor.builder().idProveedor(rs.getLong("id_proveedor")).build())
                .idUsuario(usuario)
                .build();
    }

    @Override
    protected Compra insert(Connection conn, Compra e) throws SQLException {
        long id = executeInsertGetKey(conn,
                "INSERT INTO compra(precio_base,igv,precio_total,serie,num_doc,tipo_doc,fecha_comp,fecha_reg,id_proveedor,id_usuario) VALUES(?,?,?,?,?,?,?,?,?,?)",
                e.getPrecioBase(), e.getIgv(), e.getPrecioTotal(),
                e.getSerie(), e.getNumDoc(), e.getTipoDoc(),
                e.getFechaComp(), e.getFechaReg(),
                e.getIdProveedor().getIdProveedor(), e.getIdUsuario().getIdUsuario());
        e.setId(id);
        return e;
    }

    @Override
    protected Compra updateRow(Connection conn, Compra e) throws SQLException {
        executeUpdate(conn,
                "UPDATE compra SET precio_base=?,igv=?,precio_total=?,serie=?,num_doc=?,tipo_doc=?,fecha_comp=?,fecha_reg=?,id_proveedor=?,id_usuario=? WHERE id_compra=?",
                e.getPrecioBase(), e.getIgv(), e.getPrecioTotal(),
                e.getSerie(), e.getNumDoc(), e.getTipoDoc(),
                e.getFechaComp(), e.getFechaReg(),
                e.getIdProveedor().getIdProveedor(), e.getIdUsuario().getIdUsuario(), e.getId());
        return e;
    }
}
