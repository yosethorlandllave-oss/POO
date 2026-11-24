package pe.edu.upeu.sysventas;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Abre la consola web de H2 ya conectada a la base de datos del proyecto.
 * Uso: clic derecho en esta clase -> Run 'AbrirConsolaH2.main()'.
 * Gracias a AUTO_SERVER=TRUE se puede tener abierta la consola y el programa a la vez.
 */
public class AbrirConsolaH2 {
    public static void main(String[] args) throws Exception {
        Path ruta = Paths.get("data", "sysventas").toAbsolutePath();
        String url = "jdbc:h2:file:" + ruta.toString().replace('\\', '/') + ";AUTO_SERVER=TRUE";

        System.out.println("=== Consola H2 de Coolbox ===");
        System.out.println("JDBC URL : " + url);
        System.out.println("Usuario  : sa");
        System.out.println("Clave    : (vacia)");
        System.out.println("Si el navegador no se abre solo, entra a: http://localhost:8082");
        System.out.println("Para cerrar la consola, presiona el cuadrado rojo en IntelliJ.");

        try {
            org.h2.tools.Server.createWebServer("-webPort", "8082").start();
        } catch (Exception e) {
            // El puerto ya está ocupado: la consola ya está abierta (por ejemplo, desde el programa)
            System.out.println("La consola H2 ya estaba abierta. Solo se abre el navegador.");
        }
        try {
            org.h2.tools.Server.openBrowser("http://localhost:8082");
        } catch (Exception e) {
            System.out.println("Abre el navegador en: http://localhost:8082");
        }
    }
}
