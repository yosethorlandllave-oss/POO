package pe.edu.upeu.sysventas.repository;

import pe.edu.upeu.sysventas.enums.TipoDocumento;
import pe.edu.upeu.sysventas.model.Cliente;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

// El ID del cliente es su DNI/RUC (un String), por eso ID = String y no Long.
public class ClienteRepository extends AbstractJpaRepository<Cliente, String> {
    @Override
    protected String getTableName() {
        return "cliente";
    }

    @Override
    protected String getPkColumn() {
        return "dniruc";
    }

    @Override
    protected Cliente insert(Connection connection, Cliente entity) throws SQLException {
        // La PK (dniruc) la escribe el usuario, no la genera la BD.
        executeUpdate(connection,
                "INSERT INTO cliente(dniruc, nombres, rep_legal, direccion, tipo_documento) VALUES(?,?,?,?,?)",
                entity.getDniruc(),
                entity.getNombres(),
                valorOVacio(entity.getRepLegal()),
                valorOVacio(entity.getDireccion()),
                entity.getTipoDocumento().name());
        return entity;
    }

    @Override
    protected Cliente updateRow(Connection connection, Cliente entity) throws SQLException {
        executeUpdate(connection,
                "UPDATE cliente SET nombres=?, rep_legal=?, direccion=?, tipo_documento=? WHERE dniruc=?",
                entity.getNombres(),
                valorOVacio(entity.getRepLegal()),
                valorOVacio(entity.getDireccion()),
                entity.getTipoDocumento().name(),
                entity.getDniruc());
        return entity;
    }

    @Override
    protected Cliente mapRow(ResultSet rs) throws SQLException {
        return Cliente.builder()
                .dniruc(rs.getString("dniruc"))
                .nombres(rs.getString("nombres"))
                .repLegal(rs.getString("rep_legal"))
                .direccion(rs.getString("direccion"))
                .tipoDocumento(TipoDocumento.valueOf(rs.getString("tipo_documento")))
                .build();
    }

    // Las columnas rep_legal y direccion son NOT NULL en la BD.
    private static String valorOVacio(String valor) {
        return valor == null ? "" : valor;
    }
}
