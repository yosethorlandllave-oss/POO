package pe.edu.upeu.sysventas.model;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
@Data @NoArgsConstructor
public class Compra extends DocumentoComercial {
 private Long idCompra;
 private Proveedor proveedor;
 private Usuario usuario;
 private LocalDate fechaCompra;
 private List<CompraDetalle> detalles = new ArrayList<>();
 private Double totalCompra = 0.0;
 public Compra(Long idCompra, Proveedor proveedor, Usuario usuario, LocalDate fechaCompra, List<CompraDetalle> detalles, Double totalCompra) {
  setIdCompra(idCompra); this.proveedor=proveedor; this.usuario=usuario; this.fechaCompra=fechaCompra;
  this.detalles=detalles==null?new ArrayList<>():new ArrayList<>(detalles); this.totalCompra=totalCompra==null?0.0:totalCompra;
 }
 public void setIdCompra(Long id) { idCompra=id; super.setId(id); }
 @Override public double calcularTotal() {
  if(detalles==null) detalles=new ArrayList<>();
  totalCompra=detalles.stream().filter(d->d!=null).mapToDouble(CompraDetalle::calcularSubtotal).sum();
  return totalCompra;
 }
}