package pe.edu.upeu.sysventas.service.impl;

import lombok.RequiredArgsConstructor;
import pe.edu.upeu.sysventas.dto.ComboBoxOption;
import pe.edu.upeu.sysventas.enums.TipoProducto;
import pe.edu.upeu.sysventas.exception.ReglaNegocioException;
import pe.edu.upeu.sysventas.model.Producto;
import pe.edu.upeu.sysventas.repository.CategoriaRepository;
import pe.edu.upeu.sysventas.repository.ICrudGenericoRepository;
import pe.edu.upeu.sysventas.repository.MarcaRepository;
import pe.edu.upeu.sysventas.repository.ProductoRepository;
import pe.edu.upeu.sysventas.repository.UnidadMedidaRepository;
import pe.edu.upeu.sysventas.service.IProductoService;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class ProductoServiceImp extends CrudGenericoServiceImp<Producto, Long> implements IProductoService {

    private final ProductoRepository productoRepository;
    private final MarcaRepository marcaRepository;
    private final CategoriaRepository categoriaRepository;
    private final UnidadMedidaRepository unidadMedidaRepository;

    @Override
    protected ICrudGenericoRepository<Producto, Long> getRepo() {
        return productoRepository;
    }

    @Override
    protected void fijarId(Producto producto, Long id) {
        producto.setIdProducto(id);
    }

    @Override
    protected void antesDeGuardar(Producto producto, Long idEnEdicion) {
        producto.setNombre(limpiar(producto.getNombre()));

        // Integridad referencial: el producto apunta a los registros vigentes,
        // no a copias que podrían haber sido eliminadas o renombradas.
        producto.setIdMarca(marcaRepository.findById(producto.getIdMarca().getIdMarca())
                .orElseThrow(() -> new ReglaNegocioException("La marca seleccionada ya no existe.", "idMarca")));
        producto.setIdCategoria(categoriaRepository.findById(producto.getIdCategoria().getIdCategoria())
                .orElseThrow(() -> new ReglaNegocioException("La categoría seleccionada ya no existe.", "idCategoria")));
        producto.setIdUnidad(unidadMedidaRepository.findById(producto.getIdUnidad().getIdUnidad())
                .orElseThrow(() -> new ReglaNegocioException("La unidad de medida seleccionada ya no existe.", "idUnidad")));

        boolean duplicado = productoRepository.findAll().stream()
                .anyMatch(p -> mismoNombre(p.getNombre(), producto.getNombre())
                        && p.getIdMarca() != null
                        && p.getIdMarca().getIdMarca().equals(producto.getIdMarca().getIdMarca())
                        && !p.getIdProducto().equals(idEnEdicion));
        if (duplicado) {
            throw new ReglaNegocioException("Ya existe el producto «" + producto.getNombre() + "» de la marca "
                    + producto.getIdMarca().getNombre() + ".", "nombre");
        }
    }

    @Override
    public List<ComboBoxOption> listarTipoProducto() {
        List<ComboBoxOption> listar = new ArrayList<>();
        for (TipoProducto tp : TipoProducto.values()) {
            ComboBoxOption cb = new ComboBoxOption();
            cb.setKey(String.valueOf(tp.name()));
            cb.setValue(tp.getDescripcion());
            listar.add(cb);
        }
        return listar;
    }

}
