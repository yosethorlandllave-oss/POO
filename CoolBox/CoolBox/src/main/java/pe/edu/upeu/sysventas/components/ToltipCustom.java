package pe.edu.upeu.sysventas.components;

import javafx.scene.Parent;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

/**
 * Marca visualmente los campos con error de validación: borde rojo (clase
 * CSS "campo-error"), mensaje visible bajo el campo y tooltip con el detalle.
 * El mensaje visible solo se agrega cuando el campo está dentro de un
 * contenedor de tipo Pane (por ejemplo, el VBox "campo" del formulario).
 */
public class ToltipCustom {
    public static final String CLASE_ERROR = "campo-error";
    private static final String CLAVE_TOOLTIP = "coolbox.tooltipError";
    private static final String CLAVE_MENSAJE = "coolbox.mensajeError";

    public void marcarError(Control campo, String mensaje) {
        limpiarCampo(campo);
        if (!campo.getStyleClass().contains(CLASE_ERROR)) {
            campo.getStyleClass().add(CLASE_ERROR);
        }

        Tooltip tooltip = new Tooltip("⚠  " + mensaje);
        tooltip.getStyleClass().add("tooltip-error");
        tooltip.setShowDelay(Duration.millis(100));
        tooltip.setHideDelay(Duration.millis(200));
        tooltip.setShowDuration(Duration.seconds(10));
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(300);
        Tooltip.install(campo, tooltip);
        campo.getProperties().put(CLAVE_TOOLTIP, tooltip);

        Parent padre = campo.getParent();
        if (padre instanceof Pane contenedor) {
            Label lbl = new Label(mensaje);
            lbl.getStyleClass().add("error-campo");
            lbl.setWrapText(true);
            int pos = contenedor.getChildren().indexOf(campo);
            contenedor.getChildren().add(pos + 1, lbl);
            campo.getProperties().put(CLAVE_MENSAJE, lbl);
        }
        campo.setAccessibleHelp(mensaje);
    }

    public void limpiarCampo(Control campo) {
        campo.getStyleClass().remove(CLASE_ERROR);
        Object tooltip = campo.getProperties().remove(CLAVE_TOOLTIP);
        if (tooltip instanceof Tooltip t) {
            Tooltip.uninstall(campo, t);
        }
        Object lbl = campo.getProperties().remove(CLAVE_MENSAJE);
        if (lbl instanceof Label l && l.getParent() instanceof Pane contenedor) {
            contenedor.getChildren().remove(l);
        }
        campo.setAccessibleHelp(null);
    }

    /** Mensaje de error que muestra actualmente el campo, o null si no tiene. */
    public static String errorDe(Control campo) {
        Object lbl = campo.getProperties().get(CLAVE_MENSAJE);
        if (lbl instanceof Label l) {
            return l.getText();
        }
        Object tooltip = campo.getProperties().get(CLAVE_TOOLTIP);
        return tooltip instanceof Tooltip t ? t.getText() : null;
    }
}
