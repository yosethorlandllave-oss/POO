package pe.edu.upeu.sysventas.repository;

import pe.edu.upeu.sysventas.model.Promocion;

import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PromocionRepository extends AbstractJpaRepository<Promocion, Long> {
    @Override
    protected String getTableName() {
        return "promocion";
    }

    @Override
    protected String getPkColumn() {
        return "id_promocion";
    }

    @Override
    protected Promocion insert(Connection connection, Promocion e) throws SQLException {
        long id = executeInsertGetKey(connection,
                "INSERT INTO promocion(nombre, descripcion, porcentaje, fecha_inicio, fecha_fin, estado) VALUES(?,?,?,?,?,?)",
                e.getNombre(), e.getDescripcion(), e.getPorcentaje(),
                Date.valueOf(e.getFechaInicio()), Date.valueOf(e.getFechaFin()), e.getEstado());
        e.setIdPromocion(id);
        return e;
    }

    @Override
    protected Promocion updateRow(Connection connection, Promocion e) throws SQLException {
        executeUpdate(connection,
                "UPDATE promocion SET nombre=?, descripcion=?, porcentaje=?, fecha_inicio=?, fecha_fin=?, estado=? WHERE id_promocion=?",
                e.getNombre(), e.getDescripcion(), e.getPorcentaje(),
                Date.valueOf(e.getFechaInicio()), Date.valueOf(e.getFechaFin()), e.getEstado(),
                e.getIdPromocion());
        return e;
    }

    @Override
    protected Promocion mapRow(ResultSet rs) throws SQLException {
        Date inicio = rs.getDate("fecha_inicio");
        Date fin = rs.getDate("fecha_fin");
        return Promocion.builder()
                .idPromocion(rs.getLong("id_promocion"))
                .nombre(rs.getString("nombre"))
                .descripcion(rs.getString("descripcion"))
                .porcentaje(rs.getDouble("porcentaje"))
                .fechaInicio(inicio != null ? inicio.toLocalDate() : null)
                .fechaFin(fin != null ? fin.toLocalDate() : null)
                .estado(rs.getString("estado"))
                .build();
    }
}
