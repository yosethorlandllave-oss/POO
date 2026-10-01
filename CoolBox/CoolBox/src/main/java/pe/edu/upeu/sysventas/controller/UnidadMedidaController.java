package pe.edu.upeu.sysventas.controller;

import lombok.RequiredArgsConstructor;
import pe.edu.upeu.sysventas.model.UnidMedida;
import pe.edu.upeu.sysventas.service.ICrudGenericoService;
import pe.edu.upeu.sysventas.service.IUnidadMedidaService;

@RequiredArgsConstructor
public class UnidadMedidaController extends CatalogoController<UnidMedida> {
    private final IUnidadMedidaService us;

    @Override
    protected ICrudGenericoService<UnidMedida, Long> servicio() {
        return us;
    }

    @Override
    protected String titulo() {
        return "Unidades de medida";
    }

    @Override
    protected String grupo() {
        return "Catálogo";
    }

    @Override
    protected String singular() {
        return "unidad de medida";
    }

    @Override
    protected boolean femenino() {
        return true;
    }

    @Override
    protected Long idDe(UnidMedida u) {
        return u.getIdUnidad();
    }

    @Override
    protected String nombreDe(UnidMedida u) {
        return u.getNombreMedida();
    }

    @Override
    protected String propiedadNombre() {
        return "nombreMedida";
    }

    @Override
    protected UnidMedida nuevaInstancia(String nombre) {
        return UnidMedida.builder().nombreMedida(nombre).build();
    }

    @Override
    protected long productosQueUsan(Long id) {
        return us.contarProductos(id);
    }
}
