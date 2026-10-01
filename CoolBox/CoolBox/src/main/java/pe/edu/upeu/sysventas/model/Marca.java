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
public class Marca {
    private Long idMarca;

    @NotBlank(message = "El nombre de la marca es obligatorio")
    private String nombre;
}
