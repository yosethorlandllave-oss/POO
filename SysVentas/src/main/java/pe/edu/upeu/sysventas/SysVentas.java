package pe.edu.upeu.sysventas;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import pe.edu.upeu.sysventas.config.AppContext;
import pe.edu.upeu.sysventas.config.DatabaseConfig;

import java.io.IOException;
import java.nio.file.Paths;

public class SysVentas extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        Screen screen= Screen.getPrimary();
        Rectangle2D rectangle2D=screen.getVisualBounds();
        AppContext appContext= AppContext.getInstance();
        FXMLLoader fxmlLoader = new FXMLLoader(SysVentas.class.getResource("/view/login.fxml"));
        fxmlLoader.setControllerFactory(appContext::getBean);
        Scene scene = new Scene(fxmlLoader.load());
        scene.getStylesheets().add(SysVentas.class.getResource("/css/style.css").toExternalForm());
        scene.getStylesheets().add(SysVentas.class.getResource("/css/estilo-tienda.css").toExternalForm());
        stage.setTitle("Coolbox - Iniciar sesión");
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();
        iniciarConsolaH2();
    }

    /**
     * Abre la consola web de H2 dentro del mismo programa (http://localhost:8082).
     * Como corre en el mismo proceso, nunca sale "base de datos en uso"
     * y se cierra sola cuando se cierra el programa.
     */
    private void iniciarConsolaH2() {
        String ruta = Paths.get("data", "sysventas").toAbsolutePath().toString().replace('\\', '/');
        try {
            org.h2.tools.Server.createWebServer("-webPort", "8082", "-webDaemon").start();
            System.out.println("=== Consola H2 lista: http://localhost:8082 ===");
        } catch (Exception e) {
            System.out.println("=== La consola H2 ya estaba abierta: http://localhost:8082 ===");
        }
        System.out.println("JDBC URL : jdbc:h2:file:" + ruta + ";AUTO_SERVER=TRUE");
        System.out.println("Usuario  : sa   |   Clave: (vacia)");
    }

    /** Al cerrar el programa se cierra la base de datos y termina todo (no queda nada bloqueado). */
    @Override
    public void stop() {
        DatabaseConfig.shutdown();
        System.exit(0);
    }
}