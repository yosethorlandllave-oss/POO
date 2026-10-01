package pe.edu.upeu.sysventas.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;
import pe.edu.upeu.sysventas.components.TablaPaginada;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Pantalla CRUD de los catálogos simples (id + nombre) que usan los productos:
 * marcas, categorías y unidades de medida. Comparten la vista main_catalogo.fxml.
 */
public abstract class CatalogoController<T> extends CrudController<T> {

    @FXML protected TextField txtNombre;
    @FXML protected Label lblNombre;

    /** Nombre de la propiedad del modelo (para asociar los errores de validación). */
    protected abstract String propiedadNombre();

    protected abstract T nuevaInstancia(String nombre);

    /** Productos que usan el registro. */
    protected abstract long productosQueUsan(Long id);

    @Override
    public void initialize() {
        String etiqueta = (femenino() ? "Nombre de la " : "Nombre del ") + singular();
        lblNombre.setText(etiqueta + " *");
        txtNombre.setAccessibleText(etiqueta);
        super.initialize();
    }

    @Override
    protected void configurarTabla(TablaPaginada<T> t) {
        t.agregarColumna("ID", 70, this::idDe, x -> String.valueOf(idDe(x)), true);
        TableColumn<T, Object> nombre = t.agregarColumna("Nombre", 260, this::nombreDe);
        t.agregarColumna("Productos", 110, x -> productosQueUsan(idDe(x)),
                x -> String.valueOf(productosQueUsan(idDe(x))), true);
        t.setColumnaFlexible(nombre);
        t.setFiltroTexto((x, q) -> nombreDe(x).toLowerCase(Locale.ROOT).contains(q)
                || String.valueOf(idDe(x)).equals(q));
    }

    @Override
    protected Map<String, Control> camposFormulario() {
        Map<String, Control> campos = new LinkedHashMap<>();
        campos.put(propiedadNombre(), txtNombre);
        return campos;
    }

    @Override
    protected void limpiarFormulario() {
        txtNombre.clear();
    }

    @Override
    protected void llenarFormulario(T t) {
        txtNombre.setText(nombreDe(t));
    }

    @Override
    protected T leerFormulario(Map<String, String> erroresFormato) {
        return nuevaInstancia(txtNombre.getText());
    }

    @Override
    protected LinkedHashMap<String, String> datosDetalle(T t) {
        LinkedHashMap<String, String> datos = new LinkedHashMap<>();
        long enUso = productosQueUsan(idDe(t));
        datos.put("ID", String.valueOf(idDe(t)));
        datos.put("Nombre", nombreDe(t));
        String pronombre = femenino() ? "la" : "lo";
        datos.put("Productos que " + pronombre + " usan", String.valueOf(enUso));
        if (enUso > 0) {
            datos.put("Eliminación", "No se puede eliminar mientras haya productos que " + pronombre + " usen.");
        }
        return datos;
    }
}
