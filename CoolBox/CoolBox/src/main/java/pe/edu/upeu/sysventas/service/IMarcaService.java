package pe.edu.upeu.sysventas.service;

import pe.edu.upeu.sysventas.dto.ComboBoxOption;
import pe.edu.upeu.sysventas.model.Marca;

import java.util.List;

public interface IMarcaService extends ICrudGenericoService<Marca, Long> {
    List<ComboBoxOption> listarCombobox();

    /** Cantidad de productos que usan este registro (impide eliminarlo si es mayor que cero). */
    long contarProductos(Long id);
}