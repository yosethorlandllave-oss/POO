package pe.edu.upeu.sysventas.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


//contrusctor vacio //
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ComboBoxOption {
    String Key;
    String Value;

    @Override
    public String toString() {
        return  Value ;

    }
}
