package pe.edu.upeu.sysventas.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Proveedor {

    private Long idProveedor;

    @NotBlank(message = "El DNI/RUC es obligatorio")
    @Pattern(regexp = "[A-Za-z0-9]{8,12}", message = "El documento debe tener entre 8 y 12 letras o números")
    private String dniruc;

    @NotBlank(message = "Los nombres o razón social son obligatorios")
    private String nombresRaso;

    @NotBlank(message = "El tipo de documento es obligatorio")
    private String tipoDoc;

    @NotBlank(message = "El celular es obligatorio")
    @Pattern(regexp = "9\\d{8}", message = "El celular debe tener 9 dígitos y empezar con 9")
    private String celular;

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El correo electrónico no es válido")
    private String email;

    @NotBlank(message = "La dirección es obligatoria")
    private String direccion;

}