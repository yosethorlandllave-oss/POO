package pe.edu.upeu.sysventas.service.impl;

import pe.edu.upeu.sysventas.model.CompCarrito;
import pe.edu.upeu.sysventas.repository.CompCarritoRepository;
import pe.edu.upeu.sysventas.repository.ICrudGenericoRepository;
import pe.edu.upeu.sysventas.service.ICompraCarritoService;

import java.util.List;

public class CompraCarritoServiceImp extends CrudGenericoServiceImp<CompCarrito, Long>
        implements ICompraCarritoService {
    private final CompCarritoRepository repository;

    public CompraCarritoServiceImp(CompCarritoRepository repository) {
        this.repository = repository;
    }

    @Override
    protected ICrudGenericoRepository<CompCarrito, Long> getRepo() {
        return repository;
    }

    @Override
    public List<CompCarrito> listaCarritoProveedor(Long idProveedor) {
        return repository.listaCarritoProveedor(idProveedor);
    }

    @Override
    public void deleteCarAll(Long idProveedor) {
        repository.deleteByProveedor(idProveedor);
    }
}
