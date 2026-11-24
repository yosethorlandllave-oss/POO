package pe.edu.upeu.sysventas.service.impl;

import pe.edu.upeu.sysventas.model.CompraDetalle;
import pe.edu.upeu.sysventas.repository.CompraDetalleRepository;
import pe.edu.upeu.sysventas.repository.ICrudGenericoRepository;
import pe.edu.upeu.sysventas.service.ICompraDetalleService;

public class CompraDetalleServiceImp extends CrudGenericoServiceImp<CompraDetalle, Long>
        implements ICompraDetalleService {
    private final CompraDetalleRepository repository;

    public CompraDetalleServiceImp(CompraDetalleRepository repository) {
        this.repository = repository;
    }

    @Override
    protected ICrudGenericoRepository<CompraDetalle, Long> getRepo() {
        return repository;
    }
}
