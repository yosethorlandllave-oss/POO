package pe.edu.upeu.sysventas.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnidMedida {

    private Long idUnidad;
    @NotBlank(message = "El nombre de la unidad de medida es obligatorio")
    private String nombreMedida;
}
