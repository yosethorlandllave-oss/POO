package pe.edu.upeu.sysventas.controller;

import lombok.RequiredArgsConstructor;
import pe.edu.upeu.sysventas.model.Marca;
import pe.edu.upeu.sysventas.service.ICrudGenericoService;
import pe.edu.upeu.sysventas.service.IMarcaService;

@RequiredArgsConstructor
public class MarcaController extends CatalogoController<Marca> {
    private final IMarcaService ms;

    @Override
    protected ICrudGenericoService<Marca, Long> servicio() {
        return ms;
    }

    @Override
    protected String titulo() {
        return "Marcas";
    }

    @Override
    protected String grupo() {
        return "Catálogo";
    }

    @Override
    protected String singular() {
        return "marca";
    }

    @Override
    protected boolean femenino() {
        return true;
    }

    @Override
    protected Long idDe(Marca m) {
        return m.getIdMarca();
    }

    @Override
    protected String nombreDe(Marca m) {
        return m.getNombre();
    }

    @Override
    protected String propiedadNombre() {
        return "nombre";
    }

    @Override
    protected Marca nuevaInstancia(String nombre) {
        return Marca.builder().nombre(nombre).build();
    }

    @Override
    protected long productosQueUsan(Long id) {
        return ms.contarProductos(id);
    }
}
