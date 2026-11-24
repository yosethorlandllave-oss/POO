package pe.edu.upeu.sysventas.service.impl;

import lombok.RequiredArgsConstructor;
import pe.edu.upeu.sysventas.dto.ComboBoxOption;
import pe.edu.upeu.sysventas.dto.ModeloDataAutocomplet;
import pe.edu.upeu.sysventas.enums.TipoDocumento;
import pe.edu.upeu.sysventas.model.Cliente;
import pe.edu.upeu.sysventas.repository.ClienteRepository;
import pe.edu.upeu.sysventas.repository.ICrudGenericoRepository;
import pe.edu.upeu.sysventas.service.IClienteService;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class ClienteServiceImp extends CrudGenericoServiceImp<Cliente, String>
        implements IClienteService {

    private final ClienteRepository clienteRepository;

    @Override
    protected ICrudGenericoRepository<Cliente, String> getRepo() {
        return clienteRepository;
    }

    // Llena el ComboBox con los valores del enum TipoDocumento (DNI, CE, RUC, PASAPORTE)
    @Override
    public List<ComboBoxOption> listarTipoDocumento() {
        List<ComboBoxOption> listar = new ArrayList<>();
        for (TipoDocumento td : TipoDocumento.values()) {
            ComboBoxOption cb = new ComboBoxOption();
            cb.setKey(td.name());
            cb.setValue(td.name());
            listar.add(cb);
        }
        return listar;
    }

    @Override
    public List<Cliente> finAll() {

        return super.finAll();
    }

    // Lista de clientes para el autocompletado de la ventana de ventas
    @Override
    public List<ModeloDataAutocomplet> listAutoCompletCliente() {
        List<ModeloDataAutocomplet> listarclientes = new ArrayList<>();
        try {
            for (Cliente cliente : clienteRepository.findAll()) {
                ModeloDataAutocomplet data = new ModeloDataAutocomplet();
                data.setIdx(cliente.getDniruc());
                data.setNameDysplay(cliente.getNombres());
                data.setOtherData(cliente.getTipoDocumento().name());
                listarclientes.add(data);
            }
        } catch (Exception e) {
            System.out.println("Error al listar clientes: " + e.getMessage());
        }
        return listarclientes;
    }
}
