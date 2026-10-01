package pe.edu.upeu.sysventas.model;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data @NoArgsConstructor @AllArgsConstructor
public class CompraDetalle {
 private Long idDetalle;
 private Producto producto;
 private Double cantidad;
 private Double precioUnitario;
 private Double subtotal;
 public double calcularSubtotal() {
  subtotal=(cantidad==null?0.0:cantidad)*(precioUnitario==null?0.0:precioUnitario);
  return subtotal;
 }
}