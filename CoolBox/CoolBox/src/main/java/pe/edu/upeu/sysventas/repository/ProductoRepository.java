package pe.edu.upeu.sysventas.repository;

import pe.edu.upeu.sysventas.enums.TipoProducto;
import pe.edu.upeu.sysventas.model.Categoria;
import pe.edu.upeu.sysventas.model.Marca;
import pe.edu.upeu.sysventas.model.Producto;
import pe.edu.upeu.sysventas.model.UnidMedida;

public class ProductoRepository extends AbstractJpaRepository<Producto, Long>{
    private long sequence=1;
    @Override
    protected Long getId(Producto entity) {
        return entity.getIdProducto();
    }

    @Override
    protected void setId(Producto entity, Long id) {
        entity.setIdProducto(id);
    }

    @Override
    protected Long generateId() {
        return sequence++;
    }

    /**
     * Producto de ejemplo. Recibe los registros reales del catálogo para que la
     * tabla muestre sus nombres y la integridad referencial se cumpla.
     */
    public void seedData(Categoria categoria, Marca marca, UnidMedida unidMedida) {
        if (findAll().isEmpty()) {
            save(new Producto(generateId(), "Televisor", TipoProducto.PRODUCTO ,
                    0.0, 0.0, 0.0, 0.0, 0.0,categoria, marca,unidMedida));
        }
    }

}
