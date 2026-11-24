package pe.edu.upeu.sysventas.service;

import pe.edu.upeu.sysventas.dto.ComboBoxOption;
import pe.edu.upeu.sysventas.dto.ModeloDataAutocomplet;
import pe.edu.upeu.sysventas.model.Producto;

import java.util.List;

public interface IProductoService extends ICrudGenericoService<Producto,Long> {
    List<ComboBoxOption> listarTipoProducto();
        List<ModeloDataAutocomplet> listAutoCompletProducto();
    // Control de stock: positivo suma (compra), negativo resta (venta)
    void actualizarStock(Long idProducto, double cantidad);
    }