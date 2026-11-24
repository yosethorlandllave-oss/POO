package pe.edu.upeu.sysventas.components.reporte;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * COMPONENTE LISTO PARA USAR: formulario de filtros + visor PDF.
 *
 * <pre>
 * ReportePanel panel = new ReportePanel(reporteVentas, reporteProductos); // 1 o varios reportes
 * tab.setContent(panel);
 * </pre>
 *
 * - Con varios reportes aparece un selector.
 * - Los filtros se construyen solos a partir de {@link ParametroReporte}.
 * - «Generar» ejecuta la fuente de datos y el PDF en segundo plano y lo muestra en el visor.
 * - «Guardar PDF» y «Abrir/Imprimir» trabajan sobre el PDF ya generado.
 */
public class ReportePanel extends BorderPane {

    private final List<Reporte> reportes;
    private final ComboBox<Reporte> cmbReporte = new ComboBox<>();
    private final FlowPane zonaFiltros = new FlowPane(12, 8);
    private final Map<String, Node> controles = new LinkedHashMap<>();
    private final PdfVisorPane visor = new PdfVisorPane();
    private final Button btnGenerar = new Button("▶  Generar");
    private final Button btnGuardar = new Button("⬇  Guardar PDF");
    private final Button btnImprimir = new Button("Imprimir");
    private final Label lblEstado = new Label();
    private final ProgressIndicator progreso = new ProgressIndicator();

    private Reporte actual;
    private String nombreArchivoSugerido = "reporte.pdf";

    public ReportePanel(Reporte... reportes) {
        this(List.of(reportes));
    }

    public ReportePanel(List<Reporte> reportes) {
        if (reportes.isEmpty()) throw new IllegalArgumentException("Se requiere al menos un reporte");
        this.reportes = reportes;

        // ---------- barra superior: selector + filtros + acciones ----------
        Label titulo = new Label("Reporte:");
        titulo.setStyle("-fx-font-weight: bold;");
        cmbReporte.getItems().addAll(reportes);
        cmbReporte.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Reporte r) { return r == null ? "" : r.titulo(); }
            @Override public Reporte fromString(String s) { return null; }
        });
        cmbReporte.valueProperty().addListener((o, a, n) -> { if (n != null) seleccionar(n); });
        HBox selector = new HBox(8, titulo, cmbReporte);
        selector.setAlignment(Pos.CENTER_LEFT);
        selector.setVisible(reportes.size() > 1);
        selector.setManaged(reportes.size() > 1);

        estilizar(btnGenerar, "#E30613");   // colores Coolbox
        estilizar(btnGuardar, "#1A1A1A");
        estilizar(btnImprimir, "#555555");
        btnGuardar.setDisable(true);
        btnImprimir.setDisable(true);
        btnGenerar.setDefaultButton(true);
        btnGenerar.setOnAction(e -> generar());
        btnGuardar.setOnAction(e -> guardar());
        btnImprimir.setOnAction(e -> imprimir());

        progreso.setPrefSize(20, 20);
        progreso.setVisible(false);
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        HBox acciones = new HBox(8, btnGenerar, btnGuardar, btnImprimir, progreso, espacio, lblEstado);
        acciones.setAlignment(Pos.CENTER_LEFT);

        zonaFiltros.setAlignment(Pos.BOTTOM_LEFT);
        VBox superior = new VBox(10, selector, zonaFiltros, acciones);
        superior.setPadding(new Insets(10, 12, 10, 12));
        superior.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #E3E3E3; -fx-border-width: 0 0 1 0;");

        setTop(superior);
        setCenter(visor);
        seleccionar(reportes.get(0));
        cmbReporte.getSelectionModel().select(reportes.get(0));
    }

    // ------------------------------------------------------------------ API pública

    /** Genera el reporte actual con los valores del formulario. */
    public void generar() {
        final Map<String, Object> valores;
        try {
            valores = leerValores();
        } catch (IllegalArgumentException ex) {
            alerta(Alert.AlertType.WARNING, "Datos de entrada", ex.getMessage());
            return;
        }
        final Reporte rep = actual;
        btnGenerar.setDisable(true);
        btnGuardar.setDisable(true);
        btnImprimir.setDisable(true);
        progreso.setVisible(true);
        lblEstado.setText("Generando…");

        Task<byte[]> tarea = new Task<>() {
            @Override protected byte[] call() throws Exception { return ReportePdf.generar(rep, valores); }
        };
        tarea.setOnSucceeded(e -> {
            visor.mostrar(tarea.getValue());
            nombreArchivoSugerido = rep.titulo().toLowerCase().replaceAll("[^a-z0-9]+", "_") + "_" + LocalDate.now() + ".pdf";
            terminar("Listo (" + (tarea.getValue().length / 1024 + 1) + " KB)", true);
        });
        tarea.setOnFailed(e -> {
            Throwable ex = tarea.getException();
            ex.printStackTrace();
            visor.mensaje("No se pudo generar el reporte.");
            terminar("Error", false);
            alerta(Alert.AlertType.ERROR, "Error al generar el reporte", ex.getMessage() == null ? ex.toString() : ex.getMessage());
        });
        Thread t = new Thread(tarea, "reporte-gen");
        t.setDaemon(true);
        t.start();
    }

    /** Asigna un valor a un parámetro desde código (p.ej. precargar fechas). */
    public void setValor(String clave, Object valor) {
        Node n = controles.get(clave);
        if (n instanceof DatePicker dp) dp.setValue((LocalDate) valor);
        else if (n instanceof TextField tf) tf.setText(valor == null ? "" : valor.toString());
        else if (n instanceof ComboBox<?> cb) { @SuppressWarnings("unchecked") ComboBox<Object> c = (ComboBox<Object>) cb; c.setValue(valor); }
        else if (n instanceof CheckBox ck) ck.setSelected(Boolean.TRUE.equals(valor));
    }

    public byte[] getPdf() { return visor.getPdf(); }
    public PdfVisorPane getVisor() { return visor; }

    // ------------------------------------------------------------------ internos

    private void seleccionar(Reporte r) {
        this.actual = r;
        controles.clear();
        zonaFiltros.getChildren().clear();
        for (ParametroReporte p : r.parametros()) {
            Node ctl = crearControl(p);
            controles.put(p.clave(), ctl);
            Label et = new Label(p.etiqueta() + (p.esRequerido() ? " *" : ""));
            et.setStyle("-fx-font-size: 11; -fx-text-fill: gray;");
            if (p.tipo() == ParametroReporte.Tipo.BOOLEANO) zonaFiltros.getChildren().add(ctl);
            else zonaFiltros.getChildren().add(new VBox(2, et, ctl));
        }
        visor.mensaje("Seleccione los filtros y presione «Generar».");
        btnGuardar.setDisable(true);
        btnImprimir.setDisable(true);
        lblEstado.setText("");
    }

    private Node crearControl(ParametroReporte p) {
        return switch (p.tipo()) {
            case FECHA -> {
                DatePicker dp = new DatePicker((LocalDate) p.valorInicial());
                dp.setPrefWidth(140);
                yield dp;
            }
            case NUMERO -> {
                TextField tf = new TextField(p.valorInicial() == null ? "" : p.valorInicial().toString());
                tf.setPrefWidth(110);
                tf.textProperty().addListener((o, a, n) -> { if (!n.matches("-?\\d*([.]\\d*)?")) tf.setText(a); });
                yield tf;
            }
            case LISTA -> {
                ComboBox<String> cb = new ComboBox<>();
                cb.getItems().addAll(p.opciones());
                cb.setValue((String) p.valorInicial());
                cb.setPrefWidth(170);
                yield cb;
            }
            case BOOLEANO -> new CheckBox(p.etiqueta()) {{ setSelected(Boolean.TRUE.equals(p.valorInicial())); }};
            default -> {
                TextField tf = new TextField(p.valorInicial() == null ? "" : p.valorInicial().toString());
                tf.setPrefWidth(180);
                tf.setPromptText(p.etiqueta());
                yield tf;
            }
        };
    }

    /** Lee y valida los controles. Lanza IllegalArgumentException con un mensaje apto para el usuario. */
    private Map<String, Object> leerValores() {
        Map<String, Object> v = new LinkedHashMap<>();
        for (ParametroReporte p : actual.parametros()) {
            Node n = controles.get(p.clave());
            Object valor = switch (p.tipo()) {
                case FECHA -> ((DatePicker) n).getValue();
                case NUMERO -> {
                    String s = ((TextField) n).getText().trim();
                    yield s.isEmpty() || s.equals("-") || s.equals(".") ? null : Double.valueOf(s);
                }
                case LISTA -> ((ComboBox<?>) n).getValue();
                case BOOLEANO -> ((CheckBox) n).isSelected();
                default -> {
                    String s = ((TextField) n).getText().trim();
                    yield s.isEmpty() ? null : s;
                }
            };
            if (p.esRequerido() && (valor == null || valor.toString().isBlank()))
                throw new IllegalArgumentException("El campo «" + p.etiqueta() + "» es obligatorio.");
            v.put(p.clave(), valor);
        }
        if (v.get("desde") instanceof LocalDate d && v.get("hasta") instanceof LocalDate h && d.isAfter(h))
            throw new IllegalArgumentException("La fecha «Desde» no puede ser posterior a «Hasta».");
        return v;
    }

    private void terminar(String estado, boolean ok) {
        progreso.setVisible(false);
        btnGenerar.setDisable(false);
        btnGuardar.setDisable(!ok);
        btnImprimir.setDisable(!ok);
        lblEstado.setText(estado);
    }

    private void guardar() {
        byte[] pdf = visor.getPdf();
        if (pdf == null) return;
        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar reporte como PDF");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivo PDF (*.pdf)", "*.pdf"));
        fc.setInitialFileName(nombreArchivoSugerido);
        File f = fc.showSaveDialog(getScene().getWindow());
        if (f == null) return;
        try {
            Files.write(f.toPath(), pdf);
            lblEstado.setText("Guardado: " + f.getName());
        } catch (Exception ex) {
            alerta(Alert.AlertType.ERROR, "No se pudo guardar", ex.getMessage());
        }
    }

    /** Escribe un PDF temporal y lo envía a imprimir con la aplicación del sistema (o lo abre si no hay opción). */
    private void imprimir() {
        byte[] pdf = visor.getPdf();
        if (pdf == null) return;
        Thread t = new Thread(() -> {
            try {
                File tmp = File.createTempFile("reporte_", ".pdf");
                tmp.deleteOnExit();
                Files.write(tmp.toPath(), pdf);
                java.awt.Desktop d = java.awt.Desktop.getDesktop();
                if (d.isSupported(java.awt.Desktop.Action.PRINT)) d.print(tmp);
                else d.open(tmp);
            } catch (Exception ex) {
                Platform.runLater(() -> alerta(Alert.AlertType.ERROR, "No se pudo imprimir",
                        "Guarde el PDF y ábralo con su visor para imprimirlo.\n" + ex.getMessage()));
            }
        }, "reporte-print");
        t.setDaemon(true);
        t.start();
    }

    private static void estilizar(Button b, String color) {
        b.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 7 16; -fx-background-radius: 6; -fx-cursor: hand;");
    }

    private void alerta(Alert.AlertType tipo, String cabecera, String detalle) {
        Alert a = new Alert(tipo);
        a.setTitle("Reportes");
        a.setHeaderText(cabecera);
        a.setContentText(detalle);
        a.showAndWait();
    }
}
