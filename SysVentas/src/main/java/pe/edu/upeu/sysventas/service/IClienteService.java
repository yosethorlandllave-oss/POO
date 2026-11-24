package pe.edu.upeu.sysventas.service;

import pe.edu.upeu.sysventas.dto.ComboBoxOption;
import pe.edu.upeu.sysventas.dto.ModeloDataAutocomplet;
import pe.edu.upeu.sysventas.model.Cliente;

import java.util.List;

public interface IClienteService extends ICrudGenericoService<Cliente, String> {
    List<ComboBoxOption> listarTipoDocumento();
    List<ModeloDataAutocomplet> listAutoCompletCliente();
}
