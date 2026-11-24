package pe.edu.upeu.sysventas.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Screen;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import pe.edu.upeu.sysventas.SysVentas;
import pe.edu.upeu.sysventas.components.StageManager;
import pe.edu.upeu.sysventas.components.Toast;
import pe.edu.upeu.sysventas.config.AppContext;
import pe.edu.upeu.sysventas.config.DatabaseConfig;
import pe.edu.upeu.sysventas.dto.SessionManager;
import pe.edu.upeu.sysventas.model.Usuario;
import pe.edu.upeu.sysventas.service.IUsuarioService;

@RequiredArgsConstructor
public class LoginController {
    final private IUsuarioService us;

    @FXML
    TextField txtUsuario;

    @FXML
    PasswordField txtClave;

    @FXML
    public void login(ActionEvent e){
        String usuario = txtUsuario.getText() == null ? "" : txtUsuario.getText().trim();
        String clave = txtClave.getText() == null ? "" : txtClave.getText();

        // Validación: no consultar la base de datos si los campos están vacíos
        if (usuario.isEmpty() || clave.isEmpty()) {
            mensaje(e, "Ingrese su usuario y su clave");
            (usuario.isEmpty() ? txtUsuario : txtClave).requestFocus();
            return;
        }

        us.loginUsuario(usuario, clave).ifPresentOrElse(
                u -> abrirPantalla(e, u),
                () -> {
                    mensaje(e, "Usuario o clave incorrectos, o usuario inactivo");
                    txtClave.clear();
                    txtClave.requestFocus();
                }
        );
    }
    public void mensaje(ActionEvent e, String msg){
        Stage stage = (Stage) ((Node)e.getSource()).getScene().getWindow();
        double w = stage.getWidth() / 1.5, h = stage.getHeight() / 2;
        Toast.showToast(stage, msg, 2000, w, h);
    }

    /** Enlace "¿Olvidaste tu contraseña?": la clave está encriptada y no se puede leer. */
    @FXML
    public void olvideClave(ActionEvent e){
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Coolbox");
        alert.setHeaderText("¿Olvidaste tu contraseña?");
        alert.setContentText("Por seguridad las claves se guardan encriptadas con SHA-256 y no se pueden ver ni recuperar.\n"
                + "Pide al administrador de la tienda que te asigne una clave nueva.");
        alert.initOwner(((Node) e.getSource()).getScene().getWindow());
        alert.showAndWait();
    }

    @FXML
    public void logout(){
        // Primero se cierra la base de datos y luego el programa
        DatabaseConfig.shutdown();
        Platform.exit();
        System.exit(0);
    }

    public void abrirPantalla(ActionEvent e, Usuario usuario){

        SessionManager.getInstance().setUserId(usuario.getIdUsuario());
        SessionManager.getInstance().setUserName(usuario.getUsuario());
        SessionManager.getInstance().setUserPerfil(usuario.getIdPerfil().getNombre());

        try {
            Screen screen=Screen.getPrimary();
            Rectangle2D rectangle2D=screen.getVisualBounds();
            AppContext appContext=AppContext.getInstance();
            FXMLLoader fxmlLoader = new FXMLLoader(SysVentas.class.getResource("/view/maingui.fxml"));
            fxmlLoader.setControllerFactory(appContext::getBean);
            Scene scene = new Scene(fxmlLoader.load(),rectangle2D.getWidth(),rectangle2D.getHeight());
            scene.getStylesheets().add(SysVentas.class.getResource("/css/style.css").toExternalForm());
            scene.getStylesheets().add(SysVentas.class.getResource("/css/estilo-tienda.css").toExternalForm());
            Stage stage = (Stage) ((Node)e.getSource()).getScene().getWindow();
            StageManager.setPrimaryStage(stage);
            stage.setTitle("Coolbox");
            stage.setScene(scene);
            stage.setResizable(true);
            stage.show();
        }catch (Exception ex) { ex.printStackTrace(); }

    }

}