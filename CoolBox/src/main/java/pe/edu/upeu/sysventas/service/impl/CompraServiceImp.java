package pe.edu.upeu.sysventas.service.impl;
import lombok.RequiredArgsConstructor;
import pe.edu.upeu.sysventas.exception.ModelNotFoundException;
import pe.edu.upeu.sysventas.model.Compra;
import pe.edu.upeu.sysventas.repository.CompraRepository;
import pe.edu.upeu.sysventas.service.ICompraService;
import java.util.List;
@RequiredArgsConstructor
public class CompraServiceImp implements ICompraService {
 private final CompraRepository compraRepository;
 @Override public Compra guardarCompra(Compra c){validar(c);return compraRepository.save(c);}
 @Override public List<Compra> listarCompras(){return compraRepository.findAll();}
 @Override public Compra buscarCompra(Long id){return compraRepository.findById(id).orElseThrow(()->new ModelNotFoundException("Compra no encontrada: "+id));}
 @Override public List<Compra> buscarCompras(String q){return compraRepository.buscarPorProveedor(q);}
 @Override public Compra actualizarCompra(Long id,Compra c){
  if(!compraRepository.existsById(id))throw new ModelNotFoundException("Compra no encontrada: "+id);
  validar(c); c.setIdCompra(id); return compraRepository.update(c);
 }
 @Override public void eliminarCompra(Long id){
  if(!compraRepository.existsById(id))throw new ModelNotFoundException("Compra no encontrada: "+id);
  compraRepository.deleteById(id);
 }
 private void validar(Compra c){
  if(c==null||c.getProveedor()==null)throw new IllegalArgumentException("Ingrese los datos del proveedor");
  if(c.getFechaCompra()==null)throw new IllegalArgumentException("Seleccione la fecha");
  if(c.getDetalles()==null||c.getDetalles().isEmpty())throw new IllegalArgumentException("Agregue al menos un producto");
  c.getDetalles().forEach(d->{
   if(d==null||d.getProducto()==null)throw new IllegalArgumentException("Cada detalle requiere producto");
   if(d.getCantidad()==null||d.getCantidad()<=0)throw new IllegalArgumentException("Cantidad debe ser mayor que cero");
   if(d.getPrecioUnitario()==null||d.getPrecioUnitario()<0)throw new IllegalArgumentException("Precio no válido");
   d.calcularSubtotal();
  }); c.calcularTotal();
 }
}