package pe.edu.upeu.sysventas.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import pe.edu.upeu.sysventas.components.reporte.ReportePanel;
import pe.edu.upeu.sysventas.service.IReporteService;

/**
 * Pantalla de reportes PDF (Sesión 16). El ReportePanel del docente muestra
 * el selector de reportes, los filtros, el botón Generar y el visor del PDF.
 */
public class ReporteController {

    @FXML private StackPane contenedor;

    private final IReporteService reporteService;

    public ReporteController(IReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @FXML
    public void initialize() {
        try {
            contenedor.getChildren().add(new ReportePanel(reporteService.reportesDisponibles()));
        } catch (Exception e) {
            e.printStackTrace();
            Label error = new Label("No se pudieron cargar los reportes: " + e.getMessage());
            error.setStyle("-fx-text-fill: #C62828; -fx-font-size: 14px;");
            contenedor.getChildren().add(error);
        }
    }
}
