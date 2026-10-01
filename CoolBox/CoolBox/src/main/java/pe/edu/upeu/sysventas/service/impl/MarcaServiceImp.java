package pe.edu.upeu.sysventas.service.impl;

import lombok.RequiredArgsConstructor;
import pe.edu.upeu.sysventas.dto.ComboBoxOption;
import pe.edu.upeu.sysventas.exception.ReglaNegocioException;
import pe.edu.upeu.sysventas.model.Marca;
import pe.edu.upeu.sysventas.model.Producto;
import pe.edu.upeu.sysventas.repository.ICrudGenericoRepository;
import pe.edu.upeu.sysventas.repository.MarcaRepository;
import pe.edu.upeu.sysventas.repository.ProductoRepository;
import pe.edu.upeu.sysventas.service.IMarcaService;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class MarcaServiceImp extends CrudGenericoServiceImp<Marca, Long>
        implements IMarcaService {

    private final MarcaRepository marcaRepository;
    private final ProductoRepository productoRepository;

    @Override
    protected ICrudGenericoRepository<Marca, Long> getRepo() {
        return marcaRepository;
    }

    @Override
    protected void fijarId(Marca marca, Long id) {
        marca.setIdMarca(id);
    }

    @Override
    protected void antesDeGuardar(Marca marca, Long idEnEdicion) {
        marca.setNombre(limpiar(marca.getNombre()));
        boolean duplicada = marcaRepository.findAll().stream()
                .anyMatch(m -> mismoNombre(m.getNombre(), marca.getNombre())
                        && !m.getIdMarca().equals(idEnEdicion));
        if (duplicada) {
            throw new ReglaNegocioException("Ya existe una marca con el nombre «" + marca.getNombre() + "».", "nombre");
        }
    }

    @Override
    public Marca update(Long id, Marca marca) {
        Marca actualizada = super.update(id, marca);
        // Los productos guardan la marca por referencia: que vean el nuevo nombre.
        for (Producto p : productoRepository.findAll()) {
            if (p.getIdMarca() != null && id.equals(p.getIdMarca().getIdMarca())) {
                p.setIdMarca(actualizada);
            }
        }
        return actualizada;
    }

    @Override
    protected void antesDeEliminar(Long id) {
        long enUso = contarProductos(id);
        if (enUso > 0) {
            throw new ReglaNegocioException("No se puede eliminar la marca «" + findById(id).getNombre()
                    + "» porque la usa" + (enUso == 1 ? " 1 producto." : "n " + enUso + " productos.")
                    + " Cambia o elimina primero esos productos.");
        }
    }

    @Override
    public long contarProductos(Long idMarca) {
        return productoRepository.findAll().stream()
                .filter(p -> p.getIdMarca() != null && idMarca.equals(p.getIdMarca().getIdMarca()))
                .count();
    }

    @Override
    public List<ComboBoxOption> listarCombobox() {
        List<ComboBoxOption> listar = new ArrayList<>();
        for (Marca m : marcaRepository.findAll()) {
            ComboBoxOption cb = new ComboBoxOption();
            cb.setKey(String.valueOf(m.getIdMarca()));
            cb.setValue(m.getNombre());
            listar.add(cb);
        }
        return listar;
    }

}
