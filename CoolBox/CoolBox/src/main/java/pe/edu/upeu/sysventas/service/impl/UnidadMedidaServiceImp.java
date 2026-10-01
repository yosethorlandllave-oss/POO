package pe.edu.upeu.sysventas.service.impl;

import lombok.RequiredArgsConstructor;
import pe.edu.upeu.sysventas.dto.ComboBoxOption;
import pe.edu.upeu.sysventas.exception.ReglaNegocioException;
import pe.edu.upeu.sysventas.model.Producto;
import pe.edu.upeu.sysventas.model.UnidMedida;
import pe.edu.upeu.sysventas.repository.ICrudGenericoRepository;
import pe.edu.upeu.sysventas.repository.ProductoRepository;
import pe.edu.upeu.sysventas.repository.UnidadMedidaRepository;
import pe.edu.upeu.sysventas.service.IUnidadMedidaService;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class UnidadMedidaServiceImp extends CrudGenericoServiceImp<UnidMedida, Long>
        implements IUnidadMedidaService {

    private final UnidadMedidaRepository unidadMedidaRepository;
    private final ProductoRepository productoRepository;

    @Override
    protected ICrudGenericoRepository<UnidMedida, Long> getRepo() {
        return unidadMedidaRepository;
    }

    @Override
    protected void fijarId(UnidMedida unidad, Long id) {
        unidad.setIdUnidad(id);
    }

    @Override
    protected void antesDeGuardar(UnidMedida unidad, Long idEnEdicion) {
        unidad.setNombreMedida(limpiar(unidad.getNombreMedida()));
        boolean duplicada = unidadMedidaRepository.findAll().stream()
                .anyMatch(u -> mismoNombre(u.getNombreMedida(), unidad.getNombreMedida())
                        && !u.getIdUnidad().equals(idEnEdicion));
        if (duplicada) {
            throw new ReglaNegocioException("Ya existe una unidad de medida con el nombre «"
                    + unidad.getNombreMedida() + "».", "nombreMedida");
        }
    }

    @Override
    public UnidMedida update(Long id, UnidMedida unidad) {
        UnidMedida actualizada = super.update(id, unidad);
        for (Producto p : productoRepository.findAll()) {
            if (p.getIdUnidad() != null && id.equals(p.getIdUnidad().getIdUnidad())) {
                p.setIdUnidad(actualizada);
            }
        }
        return actualizada;
    }

    @Override
    protected void antesDeEliminar(Long id) {
        long enUso = contarProductos(id);
        if (enUso > 0) {
            throw new ReglaNegocioException("No se puede eliminar la unidad de medida «" + findById(id).getNombreMedida()
                    + "» porque la usa" + (enUso == 1 ? " 1 producto." : "n " + enUso + " productos.")
                    + " Cambia o elimina primero esos productos.");
        }
    }

    @Override
    public long contarProductos(Long idUnidad) {
        return productoRepository.findAll().stream()
                .filter(p -> p.getIdUnidad() != null && idUnidad.equals(p.getIdUnidad().getIdUnidad()))
                .count();
    }

    @Override
    public List<ComboBoxOption> listarCombobox() {
        List<ComboBoxOption> listar = new ArrayList<>();
        for (UnidMedida m : unidadMedidaRepository.findAll()) {
            ComboBoxOption cb = new ComboBoxOption();
            cb.setKey(String.valueOf(m.getIdUnidad()));
            cb.setValue(m.getNombreMedida());
            listar.add(cb);
        }
        return listar;
    }

}
