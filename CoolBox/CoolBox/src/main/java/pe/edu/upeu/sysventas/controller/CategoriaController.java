package pe.edu.upeu.sysventas.controller;

import lombok.RequiredArgsConstructor;
import pe.edu.upeu.sysventas.model.Categoria;
import pe.edu.upeu.sysventas.service.ICategoriaService;
import pe.edu.upeu.sysventas.service.ICrudGenericoService;

@RequiredArgsConstructor
public class CategoriaController extends CatalogoController<Categoria> {
    private final ICategoriaService cs;

    @Override
    protected ICrudGenericoService<Categoria, Long> servicio() {
        return cs;
    }

    @Override
    protected String titulo() {
        return "Categorías";
    }

    @Override
    protected String grupo() {
        return "Catálogo";
    }

    @Override
    protected String singular() {
        return "categoría";
    }

    @Override
    protected boolean femenino() {
        return true;
    }

    @Override
    protected Long idDe(Categoria c) {
        return c.getIdCategoria();
    }

    @Override
    protected String nombreDe(Categoria c) {
        return c.getNombre();
    }

    @Override
    protected String propiedadNombre() {
        return "nombre";
    }

    @Override
    protected Categoria nuevaInstancia(String nombre) {
        return Categoria.builder().nombre(nombre).build();
    }

    @Override
    protected long productosQueUsan(Long id) {
        return cs.contarProductos(id);
    }
}
