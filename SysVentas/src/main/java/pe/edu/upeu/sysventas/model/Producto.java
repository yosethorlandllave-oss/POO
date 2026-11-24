package pe.edu.upeu.sysventas.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
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
    @Size(max = 40, message = "El nombre del producto debe tener como máximo 40 caracteres")
    private String nombre;

    @NotNull(message = "Seleccione el tipo de producto")
    private TipoProducto tipoProducto;
    @NotNull(message = "El precio del producto es obligatorio")
    @Positive(message = "El precio del producto debe ser positivo")
    private Double pu;
    @NotNull(message = "El precio anterior del producto es obligatorio")
    @PositiveOrZero(message = "El precio anterior del producto debe ser positivo o cero")
    private Double puold;
    @NotNull(message = "La utilidad es obligatoria")
    @PositiveOrZero(message = "La utilidad debe ser positiva o cero")
    private Double utilidad;
    @NotNull(message = "El stock del producto es obligatorio")
    @PositiveOrZero(message = "El stock del producto debe ser positivo")
    private Double stock;
    @NotNull(message = "El stock anterior del producto es obligatorio")
    @PositiveOrZero(message = "El stock anterior del producto debe ser positivo")
    private Double stockold;
    @NotNull(message = "Seleccione la categoría")
    private Categoria idCategoria;
    @NotNull(message = "Seleccione la marca")
    private Marca idMarca;
    @NotNull(message = "Seleccione la unidad de medida")
    private UnidMedida idUnidad;
}