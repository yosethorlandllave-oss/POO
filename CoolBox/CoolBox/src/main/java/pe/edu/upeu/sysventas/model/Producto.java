package pe.edu.upeu.sysventas.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.edu.upeu.sysventas.enums.TipoProducto;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Producto {

    private Long idProducto;
    @NotBlank(message = "El nombre del producto es obligatorio")
    private String nombre;
    @NotNull(message = "El tipo de producto es obligatorio")
    private TipoProducto tipoProducto;
    @NotNull(message = "El precio unitario es obligatorio")
    @Positive(message = "El precio unitario debe ser mayor que cero")
    private Double pu;
    @NotNull(message = "El precio anterior es obligatorio")
    @PositiveOrZero(message = "El precio anterior no puede ser negativo")
    private Double puold;
    @NotNull(message = "La utilidad es obligatoria")
    @Positive(message = "La utilidad debe ser mayor que cero")
    private Double utilidad;
    @NotNull(message = "El stock es obligatorio")
    @PositiveOrZero(message = "El stock no puede ser negativo")
    private Double stock;
    @NotNull(message = "El stock anterior es obligatorio")
    @PositiveOrZero(message = "El stock anterior no puede ser negativo")
    private Double stockold;
    @NotNull(message = "La categoría del producto es obligatoria")
    private Categoria idCategoria;
    @NotNull(message = "La marca del producto es obligatoria")
    private Marca idMarca;
    @NotNull(message = "La unidad de medida del producto es obligatoria")
    private UnidMedida idUnidad;
}
