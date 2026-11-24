package pe.edu.upeu.sysventas.components.reporte;

import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToolBar;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Visor de PDF embebible en cualquier layout JavaFX. Recibe los bytes del PDF,
 * renderiza cada página como imagen (PDFBox) y las muestra en un scroll continuo
 * con zoom. No toca el disco.
 */
public class PdfVisorPane extends BorderPane {

    private static final double DPI_BASE = 96;
    private static final double[] ZOOMS = {0.5, 0.75, 1.0, 1.25, 1.5, 2.0};

    private final VBox paginas = new VBox(12);
    private final ScrollPane scroll = new ScrollPane();
    private final Label lblZoom = new Label("100%");
    private final Label lblPaginas = new Label("");
    private final Label lblMensaje = new Label("Seleccione los filtros y presione «Generar».");

    private byte[] pdf;
    private int zoomIdx = 2;
    private long version;   // evita que un render lento pise a uno más reciente

    public PdfVisorPane() {
        paginas.setAlignment(Pos.TOP_CENTER);
        paginas.setPadding(new Insets(16));
        paginas.setStyle("-fx-background-color: #6b6f73;");
        lblMensaje.setStyle("-fx-text-fill: white; -fx-font-size: 14;");
        paginas.getChildren().add(lblMensaje);

        scroll.setContent(paginas);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #6b6f73;");
        setCenter(scroll);

        Button menos = new Button("−"), mas = new Button("+"), ajustar = new Button("100%");
        menos.setOnAction(e -> cambiarZoom(-1));
        mas.setOnAction(e -> cambiarZoom(+1));
        ajustar.setOnAction(e -> { zoomIdx = 2; renderizar(); });
        ToolBar barra = new ToolBar(menos, lblZoom, mas, ajustar, lblPaginas);
        setTop(barra);
    }

    /** Muestra el PDF (null limpia el visor). */
    public void mostrar(byte[] pdfBytes) {
        this.pdf = pdfBytes;
        renderizar();
    }

    public void mensaje(String texto) {
        this.pdf = null;
        version++;
        lblMensaje.setText(texto);
        paginas.getChildren().setAll(lblMensaje);
        lblPaginas.setText("");
    }

    public byte[] getPdf() { return pdf; }

    private void cambiarZoom(int delta) {
        int n = Math.max(0, Math.min(ZOOMS.length - 1, zoomIdx + delta));
        if (n == zoomIdx) return;
        zoomIdx = n;
        renderizar();
    }

    private void renderizar() {
        lblZoom.setText((int) (ZOOMS[zoomIdx] * 100) + "%");
        if (pdf == null) return;
        final byte[] datos = pdf;
        final float dpi = (float) (DPI_BASE * ZOOMS[zoomIdx] * 1.5); // 1.5 = nitidez en pantallas HiDPI
        final long mia = ++version;

        Task<List<WritableImage>> tarea = new Task<>() {
            @Override protected List<WritableImage> call() throws Exception {
                List<WritableImage> imgs = new ArrayList<>();
                try (PDDocument doc = Loader.loadPDF(datos)) {
                    PDFRenderer r = new PDFRenderer(doc);
                    for (int i = 0; i < doc.getNumberOfPages(); i++)
                        imgs.add(aFx(r.renderImageWithDPI(i, dpi)));
                }
                return imgs;
            }
        };
        tarea.setOnSucceeded(e -> {
            if (mia != version) return;
            List<WritableImage> imgs = tarea.getValue();
            paginas.getChildren().clear();
            for (WritableImage img : imgs) {
                ImageView iv = new ImageView(img);
                iv.setPreserveRatio(true);
                iv.setFitWidth(img.getWidth() / 1.5);
                iv.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,.5), 8, 0, 0, 2);");
                paginas.getChildren().add(iv);
            }
            lblPaginas.setText("   " + imgs.size() + (imgs.size() == 1 ? " página" : " páginas"));
        });
        tarea.setOnFailed(e -> {
            if (mia != version) return;
            Throwable ex = tarea.getException();
            mensaje("No se pudo mostrar el PDF: " + (ex == null ? "" : ex.getMessage()));
        });
        Thread t = new Thread(tarea, "pdf-render");
        t.setDaemon(true);
        t.start();
    }

    /** BufferedImage → WritableImage sin depender de javafx.swing. */
    private static WritableImage aFx(BufferedImage bi) {
        int w = bi.getWidth(), h = bi.getHeight();
        int[] px = bi.getRGB(0, 0, w, h, null, 0, w);
        WritableImage wi = new WritableImage(w, h);
        wi.getPixelWriter().setPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), px, 0, w);
        return wi;
    }
}
