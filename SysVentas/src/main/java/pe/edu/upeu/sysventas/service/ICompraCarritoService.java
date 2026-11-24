package pe.edu.upeu.sysventas.service;

import pe.edu.upeu.sysventas.model.CompCarrito;

import java.util.List;

public interface ICompraCarritoService extends ICrudGenericoService<CompCarrito, Long> {
    List<CompCarrito> listaCarritoProveedor(Long idProveedor);
    void deleteCarAll(Long idProveedor);
}
