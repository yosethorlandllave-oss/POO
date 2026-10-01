package pe.edu.upeu.sysventas.service.impl;

import pe.edu.upeu.sysventas.dto.ComboBoxOption;
import pe.edu.upeu.sysventas.exception.ReglaNegocioException;
import pe.edu.upeu.sysventas.model.Categoria;
import pe.edu.upeu.sysventas.model.Producto;
import pe.edu.upeu.sysventas.repository.CategoriaRepository;
import pe.edu.upeu.sysventas.repository.ICrudGenericoRepository;
import pe.edu.upeu.sysventas.repository.ProductoRepository;
import pe.edu.upeu.sysventas.service.ICategoriaService;

import java.util.ArrayList;
import java.util.List;


public class CategoriaServiceImp extends CrudGenericoServiceImp<Categoria, Long>
        implements ICategoriaService {
    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;

    public CategoriaServiceImp(CategoriaRepository categoriaRepository, ProductoRepository productoRepository) {
        this.categoriaRepository = categoriaRepository;
        this.productoRepository = productoRepository;
    }

    @Override
    protected ICrudGenericoRepository<Categoria, Long> getRepo() {
        return categoriaRepository;
    }

    @Override
    protected void fijarId(Categoria categoria, Long id) {
        categoria.setIdCategoria(id);
    }

    @Override
    protected void antesDeGuardar(Categoria categoria, Long idEnEdicion) {
        categoria.setNombre(limpiar(categoria.getNombre()));
        boolean duplicada = categoriaRepository.findAll().stream()
                .anyMatch(c -> mismoNombre(c.getNombre(), categoria.getNombre())
                        && !c.getIdCategoria().equals(idEnEdicion));
        if (duplicada) {
            throw new ReglaNegocioException("Ya existe una categoría con el nombre «" + categoria.getNombre() + "».", "nombre");
        }
    }

    @Override
    public Categoria update(Long id, Categoria categoria) {
        Categoria actualizada = super.update(id, categoria);
        for (Producto p : productoRepository.findAll()) {
            if (p.getIdCategoria() != null && id.equals(p.getIdCategoria().getIdCategoria())) {
                p.setIdCategoria(actualizada);
            }
        }
        return actualizada;
    }

    @Override
    protected void antesDeEliminar(Long id) {
        long enUso = contarProductos(id);
        if (enUso > 0) {
            throw new ReglaNegocioException("No se puede eliminar la categoría «" + findById(id).getNombre()
                    + "» porque la usa" + (enUso == 1 ? " 1 producto." : "n " + enUso + " productos.")
                    + " Cambia o elimina primero esos productos.");
        }
    }

    @Override
    public long contarProductos(Long idCategoria) {
        return productoRepository.findAll().stream()
                .filter(p -> p.getIdCategoria() != null && idCategoria.equals(p.getIdCategoria().getIdCategoria()))
                .count();
    }

    @Override
    public List<ComboBoxOption> listarCombobox() {
        List<ComboBoxOption> listar = new ArrayList<>();
        for (Categoria m : categoriaRepository.findAll()) {
            ComboBoxOption cb = new ComboBoxOption();
            cb.setKey(String.valueOf(m.getIdCategoria()));
            cb.setValue(m.getNombre());
            listar.add(cb);
        }
        return listar;
    }
}
