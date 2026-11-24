package pe.edu.upeu.sysventas.components;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.Locale;
import java.util.Optional;

/**
 * Ventana para MODIFICAR una fila del carrito (Venta o Compra).
 * Muestra el producto, deja cambiar la cantidad (y el precio si se permite),
 * calcula el total al instante y valida antes de aceptar.
 * Devuelve {cantidad, precio} o vacío si se cancela.
 */
public class EditarCarritoDialog {

    private EditarCarritoDialog() { }

    /**
     * @param owner         ventana dueña (puede ser null)
     * @param titulo        ej. "Modificar producto de la venta"
     * @param producto      nombre del producto
     * @param cantidad      cantidad actual
     * @param precio        precio unitario actual
     * @param editarPrecio  true = también se puede cambiar el precio (Compra)
     * @param stockMax      stock disponible (Venta) o null si no hay límite (Compra)
     */
    public static Optional<double[]> mostrar(Window owner, String titulo, String producto,
                                             double cantidad, double precio,
                                             boolean editarPrecio, Double stockMax) {
        Dialog<double[]> dialog = new Dialog<>();
        dialog.setTitle("Coolbox");
        dialog.setHeaderText(titulo);
        if (owner != null) dialog.initOwner(owner);

        ButtonType btnGuardar = new ButtonType("Guardar cambios", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnGuardar, ButtonType.CANCEL);

        Label lblProducto = new Label(producto);
        lblProducto.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        lblProducto.setWrapText(true);

        TextField txtCantidad = new TextField(numero(cantidad));
        TextField txtPrecio = new TextField(String.format(Locale.US, "%.2f", precio));
        txtPrecio.setEditable(editarPrecio);
        if (!editarPrecio) {
            txtPrecio.setStyle("-fx-opacity: 0.75;");
            txtPrecio.setTooltip(new Tooltip("El precio de venta viene del producto y no se cambia aquí"));
        }
        Label lblTotal = new Label();
        lblTotal.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #E30613;");
        Label lblError = new Label();
        lblError.setStyle("-fx-text-fill: #C62828;");
        lblError.setWrapText(true);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.addRow(0, new Label("Cantidad:"), txtCantidad);
        grid.addRow(1, new Label(editarPrecio ? "Precio de compra (S/):" : "Precio unitario (S/):"), txtPrecio);
        if (stockMax != null) {
            grid.addRow(2, new Label("Stock disponible:"), new Label(numero(stockMax)));
        }
        grid.addRow(3, new Label("Nuevo total:"), lblTotal);

        VBox contenido = new VBox(12, lblProducto, grid, lblError);
        contenido.setPadding(new Insets(10, 20, 10, 20));
        contenido.setPrefWidth(380);
        dialog.getDialogPane().setContent(contenido);

        Node botonOk = dialog.getDialogPane().lookupButton(btnGuardar);

        Runnable validar = () -> {
            String error = null;
            double c = 0, p = 0;
            try {
                c = Double.parseDouble(txtCantidad.getText().trim().replace(",", "."));
                p = Double.parseDouble(txtPrecio.getText().trim().replace(",", "."));
                if (c <= 0) error = "La cantidad debe ser mayor que 0";
                else if (c != Math.floor(c)) error = "La cantidad debe ser un número entero";
                else if (p <= 0) error = "El precio debe ser mayor que 0";
                else if (stockMax != null && c > stockMax) error = "Stock insuficiente: solo hay " + numero(stockMax) + " unidades";
            } catch (NumberFormatException ex) {
                error = "Escribe solo números en la cantidad y el precio";
            }
            lblError.setText(error == null ? "" : error);
            lblTotal.setText(error == null ? String.format(Locale.US, "S/ %.2f", c * p) : "-");
            botonOk.setDisable(error != null);
        };
        txtCantidad.textProperty().addListener((o, a, n) -> validar.run());
        txtPrecio.textProperty().addListener((o, a, n) -> validar.run());
        validar.run();

        dialog.setResultConverter(bt -> {
            if (bt != btnGuardar) return null;
            double c = Double.parseDouble(txtCantidad.getText().trim().replace(",", "."));
            double p = Double.parseDouble(txtPrecio.getText().trim().replace(",", "."));
            return new double[]{c, Math.round(p * 100.0) / 100.0};
        });

        Platform.runLater(() -> { txtCantidad.requestFocus(); txtCantidad.selectAll(); });
        return dialog.showAndWait();
    }

    private static String numero(double v) {
        return v == Math.floor(v) ? String.valueOf((long) v) : String.valueOf(v);
    }
}
