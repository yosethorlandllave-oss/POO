package pe.edu.upeu.sysventas.components;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Confirmaciones modales dibujadas dentro de la ventana principal, sobre la
 * capa con id "capaModal", para que compartan el estilo del panel. Si la vista
 * no está dentro de esa capa (p. ej. se abrió sola), usa un Alert estándar.
 */
public final class Dialogos {

    public static final String ID_CAPA = "capaModal";

    private Dialogos() {
    }

    /**
     * Pide confirmación antes de una acción. alAceptar solo se ejecuta si el
     * usuario pulsa el botón de aceptar; cancelar o Escape no hacen nada.
     */
    public static void confirmar(Node origen, String titulo, String mensaje, String textoAceptar,
                                 boolean peligrosa, Runnable alAceptar) {
        StackPane capa = origen.getScene() == null ? null
                : (StackPane) origen.getScene().lookup("#" + ID_CAPA);
        if (capa == null) {
            Alert alerta = new Alert(Alert.AlertType.CONFIRMATION, mensaje, ButtonType.OK, ButtonType.CANCEL);
            alerta.setTitle(titulo);
            alerta.setHeaderText(titulo);
            alerta.showAndWait().filter(ButtonType.OK::equals).ifPresent(b -> alAceptar.run());
            return;
        }

        Label lblTitulo = new Label(titulo);
        lblTitulo.getStyleClass().add("modal-titulo");
        Label lblMensaje = new Label(mensaje);
        lblMensaje.setWrapText(true);
        lblMensaje.getStyleClass().add("modal-mensaje");

        Button btnCancelar = new Button("Cancelar");
        btnCancelar.getStyleClass().addAll("btn", "btn-default");
        btnCancelar.setCancelButton(true);
        Button btnAceptar = new Button(textoAceptar);
        btnAceptar.getStyleClass().addAll("btn", peligrosa ? "btn-danger" : "btn-primary");
        btnAceptar.setId("btnConfirmarModal");

        HBox botones = new HBox(8, btnCancelar, btnAceptar);
        botones.setAlignment(Pos.CENTER_RIGHT);
        botones.getStyleClass().add("modal-pie");

        VBox tarjeta = new VBox(lblTitulo, lblMensaje, botones);
        tarjeta.getStyleClass().add("modal-tarjeta");
        tarjeta.setMaxSize(440, VBox.USE_PREF_SIZE);
        tarjeta.setAccessibleText(titulo + ". " + mensaje);

        StackPane fondo = new StackPane(tarjeta);
        fondo.getStyleClass().add("modal-fondo");
        fondo.setId("modalConfirmacion");
        StackPane.setAlignment(tarjeta, Pos.CENTER);

        Runnable cerrar = () -> capa.getChildren().remove(fondo);
        btnCancelar.setOnAction(e -> cerrar.run());
        btnAceptar.setOnAction(e -> {
            cerrar.run();
            alAceptar.run();
        });
        fondo.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                cerrar.run();
            }
        });
        // Clic fuera de la tarjeta = cancelar.
        fondo.setOnMouseClicked(e -> {
            if (e.getTarget() == fondo) {
                cerrar.run();
            }
        });

        capa.getChildren().add(fondo);
        // El foco inicial va a "Cancelar": un Enter accidental no borra nada.
        btnCancelar.requestFocus();
    }
}
