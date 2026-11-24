package pe.edu.upeu.sysventas.components.comprob;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import pe.edu.upeu.sysventas.dto.comprobante.Comprobante;
import pe.edu.upeu.sysventas.dto.comprobante.ItemComprobante;

import java.math.BigDecimal;
import java.net.URL;

/**
 * Boleta en pantalla con el estilo de la tienda Coolbox (blanco, rojo y negro),
 * ordenada como una boleta de venta electrónica y con el logo en la esquina.
 */
public class ComprobanteView {

    private static final Color ROJO   = Color.web("#E30613");
    private static final Color NEGRO  = Color.web("#1A1A1A");
    private static final Color GRIS   = Color.web("#6B6B6B");
    private static final Color CLARO  = Color.web("#F7F7F7");
    private static final Color LINEA  = Color.web("#E3E3E3");

    private static final double ANCHO = 694;   // 750 - 2 * 28 de margen

    private static final Font F_NORMAL  = Font.font("Segoe UI", 11);
    private static final Font F_BOLD    = Font.font("Segoe UI", FontWeight.BOLD, 11);
    private static final Font F_EMPRESA = Font.font("Segoe UI", FontWeight.BOLD, 22);
    private static final Font F_DOC     = Font.font("Segoe UI", FontWeight.BOLD, 13);
    private static final Font F_SERIE   = Font.font("Segoe UI", FontWeight.BOLD, 18);
    private static final Font F_SECCION = Font.font("Segoe UI", FontWeight.BOLD, 10);
    private static final Font F_TOTAL   = Font.font("Segoe UI", FontWeight.BOLD, 14);
    private static final Font F_GRACIAS = Font.font("Segoe UI", FontPosture.ITALIC, 13);
    private static final Font F_PEQUE   = Font.font("Segoe UI", 9);

    private final Comprobante boleta;

    public ComprobanteView(Comprobante boleta) {
        this.boleta = boleta;
    }

    public VBox construir() {
        VBox pagina = new VBox(14);
        pagina.setStyle("-fx-background-color: white; -fx-border-color: #E3E3E3; -fx-border-width: 1;");
        pagina.setPadding(new Insets(0, 28, 22, 28));
        pagina.setMaxWidth(750);
        pagina.setMinWidth(750);

        Rectangle franjaRoja = new Rectangle(ANCHO, 6, ROJO);
        pagina.getChildren().addAll(
            franjaRoja,
            seccionCabecera(),
            lineaPunteada(),
            seccionCliente(),
            seccionItems(),
            seccionTotales(),
            seccionPie()
        );
        return pagina;
    }

    // Logo en la esquina + datos de la tienda + recuadro de la boleta
    private HBox seccionCabecera() {
        StackPane logo = new StackPane();
        logo.setMinSize(68, 68);
        logo.setPrefSize(68, 68);
        URL url = getClass().getResource("/img/logo-coolbox.png");
        if (url != null) {
            ImageView iv = new ImageView(new Image(url.toExternalForm()));
            iv.setFitWidth(68);
            iv.setFitHeight(68);
            iv.setPreserveRatio(true);
            logo.getChildren().add(iv);
        }

        VBox emisor = new VBox(2,
            lbl(boleta.getRazonSocial(), F_EMPRESA, NEGRO),
            lbl("RUC: " + boleta.getRucEmisor(), F_BOLD, NEGRO),
            lbl(boleta.getDireccion(), F_NORMAL, GRIS),
            lbl(boleta.getUbigeo(), F_NORMAL, GRIS),
            lbl("Tel: " + boleta.getTelefono(), F_NORMAL, GRIS)
        );
        emisor.setAlignment(Pos.CENTER_LEFT);

        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);

        Label tipo = lbl(boleta.getTipoDocumento(), F_DOC, Color.WHITE);
        HBox bandaTipo = new HBox(tipo);
        bandaTipo.setAlignment(Pos.CENTER);
        bandaTipo.setPadding(new Insets(7));
        bandaTipo.setBackground(fondo(ROJO, 6, 6, 0, 0));

        String hora = boleta.getHora() != null ? "   Hora: " + boleta.getHora() : "";
        VBox datosDoc = new VBox(3,
            lbl("R.U.C. " + boleta.getRucEmisor(), F_BOLD, NEGRO),
            lbl(boleta.getSerie() + " - " + boleta.getNumero(), F_SERIE, ROJO),
            lbl("Fecha: " + boleta.getFechaEmision() + hora, F_NORMAL, GRIS)
        );
        datosDoc.setAlignment(Pos.CENTER);
        datosDoc.setPadding(new Insets(8));

        VBox docBox = new VBox(bandaTipo, datosDoc);
        docBox.setPrefWidth(260);
        docBox.setMaxWidth(260);
        docBox.setBorder(new Border(new BorderStroke(ROJO, BorderStrokeStyle.SOLID, new CornerRadii(8), new BorderWidths(2))));
        docBox.setBackground(fondo(Color.WHITE, 8, 8, 8, 8));

        HBox cabecera = new HBox(14, logo, emisor, espacio, docBox);
        cabecera.setAlignment(Pos.CENTER_LEFT);
        return cabecera;
    }

    private VBox seccionCliente() {
        GridPane grid = new GridPane();
        grid.setBackground(fondo(CLARO, 8, 8, 8, 8));
        grid.setPadding(new Insets(6, 4, 6, 4));
        grid.setHgap(4);
        agregarDato(grid, 0, 0, boleta.getTipoDocCliente(), boleta.getNroDocCliente());
        agregarDato(grid, 0, 2, "N° artículos", String.valueOf(numeroArticulos()));
        agregarDato(grid, 1, 0, "Nombre", boleta.getNombreCliente());
        agregarDato(grid, 1, 2, "Cajero", boleta.getCajero() != null ? boleta.getCajero() : "-");
        agregarDato(grid, 2, 0, "Dirección", boleta.getDireccionCliente());
        agregarDato(grid, 2, 2, "Moneda", "PEN".equals(boleta.getMoneda()) ? "Soles (PEN)" : boleta.getMoneda());

        grid.getColumnConstraints().addAll(
            new ColumnConstraints(90), new ColumnConstraints(330),
            new ColumnConstraints(90), new ColumnConstraints(160));
        return new VBox(6, tituloSeccion("DATOS DEL CLIENTE"), grid);
    }

    private VBox seccionItems() {
        GridPane header = new GridPane();
        header.setBackground(fondo(NEGRO, 6, 6, 0, 0));
        header.setPadding(new Insets(7, 8, 7, 8));
        header.addRow(0,
            celda("COD.", Color.WHITE, F_BOLD, 50, Pos.CENTER_LEFT),
            celda("CANT.", Color.WHITE, F_BOLD, 45, Pos.CENTER),
            celda("DESCRIPCIÓN", Color.WHITE, F_BOLD, 300, Pos.CENTER_LEFT),
            celda("P. UNIT", Color.WHITE, F_BOLD, 95, Pos.CENTER_RIGHT),
            celda("DCTO.", Color.WHITE, F_BOLD, 80, Pos.CENTER_RIGHT),
            celda("SUBTOTAL", Color.WHITE, F_BOLD, 95, Pos.CENTER_RIGHT)
        );

        VBox filas = new VBox(0);
        filas.setBorder(new Border(new BorderStroke(LINEA, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(0, 1, 1, 1))));
        for (int i = 0; i < boleta.getItems().size(); i++) {
            ItemComprobante item = boleta.getItems().get(i);
            GridPane fila = new GridPane();
            fila.setPadding(new Insets(6, 8, 6, 8));
            fila.setBackground(fondo(i % 2 == 0 ? Color.WHITE : Color.web("#FAFAFA"), 0, 0, 0, 0));
            boolean enPromo = item.getDescuento() != null && item.getDescuento().signum() > 0;
            // Descripción: si está en promoción se agrega el precio de antes
            String descripcion = item.getDescripcion();
            if (enPromo && item.getPrecioAntes() != null) {
                descripcion += "\nPromoción · Antes: S/ " + item.getPrecioAntes();
            }
            fila.addRow(0,
                celda(item.getCodigo() != null ? item.getCodigo() : "-", GRIS, F_NORMAL, 50, Pos.CENTER_LEFT),
                celda(formatoCantidad(item.getCantidad()), NEGRO, F_NORMAL, 45, Pos.CENTER),
                celda(descripcion, NEGRO, F_NORMAL, 300, Pos.CENTER_LEFT),
                celda("S/ " + item.getPrecioUnitario(), NEGRO, F_NORMAL, 95, Pos.CENTER_RIGHT),
                celda(enPromo ? "-S/ " + item.getDescuento() : "-", enPromo ? ROJO : GRIS, enPromo ? F_BOLD : F_NORMAL, 80, Pos.CENTER_RIGHT),
                celda("S/ " + item.getSubtotal(), NEGRO, F_BOLD, 95, Pos.CENTER_RIGHT)
            );
            filas.getChildren().add(fila);
        }
        return new VBox(6, tituloSeccion("DETALLE DE LA COMPRA"), new VBox(header, filas));
    }

    private HBox seccionTotales() {
        Label letras = lbl(NumeroALetras.soles(boleta.getTotal()), F_BOLD, NEGRO);
        letras.setTextAlignment(TextAlignment.LEFT);
        VBox son = new VBox(4, lbl("SON:", F_SECCION, ROJO), letras);
        son.setPadding(new Insets(10));
        son.setBackground(fondo(CLARO, 8, 8, 8, 8));
        son.setBorder(new Border(new BorderStroke(ROJO, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(0, 0, 0, 3))));

        Label gracias = lbl("¡Gracias por tu compra en Coolbox!", F_GRACIAS, NEGRO);
        VBox izq = new VBox(10, son, gracias);
        BigDecimal ahorro = boleta.getTotalDescuento() != null ? boleta.getTotalDescuento() : BigDecimal.ZERO;
        if (ahorro.signum() > 0) {
            Label ahorroLbl = lbl("¡Ahorraste S/ " + ahorro + " con las promociones Coolbox!", F_BOLD, Color.WHITE);
            HBox caja = new HBox(ahorroLbl);
            caja.setPadding(new Insets(6, 10, 6, 10));
            caja.setBackground(fondo(ROJO, 6, 6, 6, 6));
            caja.setMaxWidth(Region.USE_PREF_SIZE);
            izq.getChildren().add(caja);
        }
        izq.setPrefWidth(340);

        BigDecimal cero = BigDecimal.ZERO.setScale(2);
        VBox der = new VBox(
            filaTotal("OP. GRAVADAS", boleta.getOpGravadas(), false),
            filaTotal("OP. GRATUITA", cero, false),
            filaTotal("OP. EXONERADO", cero, false),
            filaTotal("OP. INAFECTO", cero, false),
            filaTotal("TOTAL DESCUENTO", boleta.getTotalDescuento() != null ? boleta.getTotalDescuento() : cero, false),
            filaTotal("I.G.V. (18%)", boleta.getIgv(), false),
            filaTotal("TOTAL VENTA", boleta.getTotal(), true)
        );
        der.setPrefWidth(320);

        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        return new HBox(14, izq, espacio, der);
    }

    private VBox seccionPie() {
        VBox pie = new VBox(4,
            lbl("Representación impresa de la " + capitalizar(boleta.getTipoDocumento()), F_BOLD, NEGRO),
            lbl("Cambios y devoluciones: plazo de 7 días, productos nuevos y sin uso. Revisa las políticas y garantía en tienda.", F_NORMAL, GRIS),
            lbl("Sistema Coolbox · Proyecto académico de POO - UPeU Juliaca", F_PEQUE, GRIS)
        );
        pie.setAlignment(Pos.CENTER);
        pie.setPadding(new Insets(10, 0, 0, 0));
        pie.setBorder(new Border(new BorderStroke(LINEA, BorderStrokeStyle.DASHED, CornerRadii.EMPTY, new BorderWidths(1, 0, 0, 0))));
        return pie;
    }

    // -- Componentes reutilizables --

    private Region lineaPunteada() {
        Region r = new Region();
        r.setPrefHeight(1);
        r.setBorder(new Border(new BorderStroke(LINEA, BorderStrokeStyle.DASHED, CornerRadii.EMPTY, new BorderWidths(1, 0, 0, 0))));
        return r;
    }

    private HBox tituloSeccion(String texto) {
        Rectangle barra = new Rectangle(4, 14, ROJO);
        HBox t = new HBox(6, barra, lbl(texto, F_SECCION, NEGRO));
        t.setAlignment(Pos.CENTER_LEFT);
        return t;
    }

    private Label lbl(String texto, Font font, Color color) {
        Label l = new Label(texto == null ? "" : texto);
        l.setFont(font);
        l.setTextFill(color);
        l.setWrapText(true);
        return l;
    }

    private Label celda(String texto, Color color, Font font, double width, Pos pos) {
        Label l = lbl(texto, font, color);
        l.setPrefWidth(width);
        l.setMinWidth(width);
        l.setAlignment(pos);
        return l;
    }

    private void agregarDato(GridPane grid, int fila, int col, String etiqueta, String valor) {
        Label e = lbl(etiqueta, F_BOLD, NEGRO);
        e.setPadding(new Insets(3, 8, 3, 8));
        Label v = lbl(valor, F_NORMAL, NEGRO);
        v.setPadding(new Insets(3, 8, 3, 8));
        grid.add(e, col, fila);
        grid.add(v, col + 1, fila);
    }

    private HBox filaTotal(String etiqueta, BigDecimal valor, boolean esTotal) {
        Font f = esTotal ? F_TOTAL : F_BOLD;
        Label lEtiq = lbl(etiqueta, f, esTotal ? Color.WHITE : NEGRO);
        lEtiq.setPrefWidth(180);
        lEtiq.setAlignment(Pos.CENTER_LEFT);
        lEtiq.setPadding(new Insets(4, 10, 4, 10));
        Label lVal = lbl("S/ " + (valor != null ? valor.toString() : "0.00"), f, esTotal ? Color.WHITE : NEGRO);
        lVal.setPrefWidth(140);
        lVal.setAlignment(Pos.CENTER_RIGHT);
        lVal.setPadding(new Insets(4, 10, 4, 10));

        HBox fila = new HBox(lEtiq, lVal);
        if (esTotal) {
            fila.setBackground(fondo(ROJO, 0, 0, 6, 6));
            fila.setPadding(new Insets(3, 0, 3, 0));
        } else {
            fila.setBorder(new Border(new BorderStroke(LINEA, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(0, 0, 1, 0))));
        }
        return fila;
    }

    private long numeroArticulos() {
        double total = 0;
        for (ItemComprobante item : boleta.getItems()) {
            total += item.getCantidad() == null ? 0 : item.getCantidad();
        }
        return Math.round(total);
    }

    private String formatoCantidad(Double cantidad) {
        if (cantidad == null) return "";
        return cantidad == Math.floor(cantidad) ? String.valueOf(cantidad.longValue()) : String.valueOf(cantidad);
    }

    // "BOLETA DE VENTA ELECTRÓNICA" -> "Boleta de Venta Electrónica"
    private String capitalizar(String texto) {
        if (texto == null || texto.isBlank()) return "";
        StringBuilder sb = new StringBuilder();
        for (String palabra : texto.toLowerCase().split(" ")) {
            if (palabra.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            if (palabra.equals("de")) sb.append(palabra);
            else sb.append(Character.toUpperCase(palabra.charAt(0))).append(palabra.substring(1));
        }
        return sb.toString();
    }

    // Fondo con esquinas: arriba-izq, arriba-der, abajo-der, abajo-izq
    private Background fondo(Color color, double ai, double ad, double abd, double abi) {
        return new Background(new BackgroundFill(color, new CornerRadii(ai, ad, abd, abi, false), Insets.EMPTY));
    }
}
