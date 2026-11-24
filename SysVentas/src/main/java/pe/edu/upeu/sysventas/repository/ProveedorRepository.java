package pe.edu.upeu.sysventas.repository;

import pe.edu.upeu.sysventas.model.Proveedor;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

// CRUD de Proveedor (Yerson) adaptado a la base de datos H2
public class ProveedorRepository extends AbstractJpaRepository<Proveedor, Long> {
    @Override
    protected String getTableName() {
        return "proveedor";
    }

    @Override
    protected String getPkColumn() {
        return "id_proveedor";
    }

    @Override
    protected Proveedor insert(Connection connection, Proveedor entity) throws SQLException {
        long id = executeInsertGetKey(connection,
                "INSERT INTO proveedor(dniruc, nombres_raso, tipo_doc, celular, email, direccion) VALUES(?,?,?,?,?,?)",
                entity.getDniruc(), entity.getNombresRaso(), entity.getTipoDoc(),
                entity.getCelular(), entity.getEmail(), entity.getDireccion());
        entity.setIdProveedor(id);
        return entity;
    }

    @Override
    protected Proveedor updateRow(Connection connection, Proveedor entity) throws SQLException {
        executeUpdate(connection,
                "UPDATE proveedor SET dniruc=?, nombres_raso=?, tipo_doc=?, celular=?, email=?, direccion=? WHERE id_proveedor=?",
                entity.getDniruc(), entity.getNombresRaso(), entity.getTipoDoc(),
                entity.getCelular(), entity.getEmail(), entity.getDireccion(),
                entity.getIdProveedor());
        return entity;
    }

    @Override
    protected Proveedor mapRow(ResultSet rs) throws SQLException {
        return Proveedor.builder()
                .idProveedor(rs.getLong("id_proveedor"))
                .dniruc(rs.getString("dniruc"))
                .nombresRaso(rs.getString("nombres_raso"))
                .tipoDoc(rs.getString("tipo_doc"))
                .celular(rs.getString("celular"))
                .email(rs.getString("email"))
                .direccion(rs.getString("direccion"))
                .build();
    }
}
