package pe.edu.upeu.sysventas.service;

import pe.edu.upeu.sysventas.dto.ComboBoxOption;
import pe.edu.upeu.sysventas.model.Categoria;

import java.util.List;

public interface ICategoriasService extends ICrudGenericoService<Categoria, Long> {
    List<ComboBoxOption> listarCombobox();

}
