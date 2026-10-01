package pe.edu.upeu.sysventas.components;

import javafx.animation.PauseTransition;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.util.Duration;

/**
 * Mensaje en línea (éxito, error o información) que se muestra sobre el
 * contenido de una página. Los de éxito se ocultan solos; los de error quedan
 * visibles hasta que el usuario los cierra o realiza otra acción.
 */
public class Aviso extends HBox {

    private static final Duration DURACION_EXITO = Duration.seconds(6);

    private final Label icono = new Label();
    private final Label texto = new Label();
    private final PauseTransition autoOcultar = new PauseTransition(DURACION_EXITO);

    public Aviso() {
        getStyleClass().add("aviso");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(8);
        texto.setWrapText(true);
        texto.setMaxWidth(Double.MAX_VALUE);
        texto.getStyleClass().add("aviso-texto");
        HBox.setHgrow(texto, Priority.ALWAYS);

        Button cerrar = new Button();
        cerrar.setGraphic(Iconos.crear(Iconos.CERRAR, 12));
        cerrar.getStyleClass().add("aviso-cerrar");
        cerrar.setAccessibleText("Cerrar mensaje");
        cerrar.setOnAction(e -> ocultar());

        getChildren().addAll(icono, texto, cerrar);
        setAccessibleRole(AccessibleRole.TEXT);
        autoOcultar.setOnFinished(e -> ocultar());
        ocultar();
    }

    public void exito(String mensaje) {
        mostrar(mensaje, "aviso-exito", Iconos.EXITO);
        autoOcultar.playFromStart();
    }

    public void error(String mensaje) {
        mostrar(mensaje, "aviso-error", Iconos.ALERTA);
    }

    public void info(String mensaje) {
        mostrar(mensaje, "aviso-info", Iconos.INFO);
    }

    public void ocultar() {
        autoOcultar.stop();
        setVisible(false);
        setManaged(false);
    }

    public String getTexto() {
        return isVisible() ? texto.getText() : "";
    }

    private void mostrar(String mensaje, String estilo, String trazoIcono) {
        autoOcultar.stop();
        getStyleClass().removeAll("aviso-exito", "aviso-error", "aviso-info");
        getStyleClass().add(estilo);
        icono.setGraphic(Iconos.crear(trazoIcono, 16));
        texto.setText(mensaje);
        setAccessibleText(mensaje);
        setVisible(true);
        setManaged(true);
    }
}
