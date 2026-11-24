package pe.edu.upeu.sysventas.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.List;

// Compra de mercadería a un proveedor (Yoseth). Hereda serie, número, precios e IGV de DocumentoComercial.
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Compra extends DocumentoComercial {
    private Proveedor idProveedor;
    private Usuario idUsuario;
    private LocalDate fechaComp;
    private LocalDate fechaReg;
    private List<CompraDetalle> detalleCompra;

    @Override
    public double calcularTotal() {
        return getPrecioBase() + getIgv();
    }
}
