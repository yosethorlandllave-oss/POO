package pe.edu.upeu.sysventas.service.impl;

import pe.edu.upeu.sysventas.model.Compra;
import pe.edu.upeu.sysventas.repository.CompraRepository;
import pe.edu.upeu.sysventas.repository.ICrudGenericoRepository;
import pe.edu.upeu.sysventas.service.ICompraService;

public class CompraServiceImp extends CrudGenericoServiceImp<Compra, Long> implements ICompraService {
    private final CompraRepository repository;

    public CompraServiceImp(CompraRepository repository) {
        this.repository = repository;
    }

    @Override
    protected ICrudGenericoRepository<Compra, Long> getRepo() {
        return repository;
    }
}
