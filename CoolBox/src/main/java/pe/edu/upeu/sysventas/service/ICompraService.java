package pe.edu.upeu.sysventas.service;
import pe.edu.upeu.sysventas.model.Compra;
import java.util.List;
public interface ICompraService {
 Compra guardarCompra(Compra compra);
 List<Compra> listarCompras();
 Compra buscarCompra(Long id);
 List<Compra> buscarCompras(String criterio);
 Compra actualizarCompra(Long id,Compra compra);
 void eliminarCompra(Long id);
}