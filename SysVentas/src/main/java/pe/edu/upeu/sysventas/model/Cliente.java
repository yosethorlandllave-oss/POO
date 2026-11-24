package pe.edu.upeu.sysventas.model;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.edu.upeu.sysventas.enums.TipoDocumento;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cliente {
    @NotBlank(message = "El DNI/RUC es obligatorio")
    @Pattern(regexp = "[A-Za-z0-9]{8,12}", message = "El documento debe tener entre 8 y 12 letras o números")
    private String dniruc;
    @NotBlank(message = "El nombre del cliente es obligatorio")
    private String nombres;
    @NotNull(message = "El tipo de documento es obligatorio")
    private TipoDocumento tipoDocumento;
    private String repLegal;
    private String direccion;
}
