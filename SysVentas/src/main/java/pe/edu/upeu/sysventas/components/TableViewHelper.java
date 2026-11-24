package pe.edu.upeu.sysventas.components;

import java.util.Optional;

import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.layout.Region;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.util.Callback;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

public class TableViewHelper<T> {

    public void addColumnsInOrderWithSize(TableView<T> tableView, LinkedHashMap<String, ColumnInfo> columns, Consumer<T> updateAction, Consumer<T> deleteAction) {
        for (Map.Entry<String, ColumnInfo> entry : columns.entrySet()) {
            TableColumn<T, Object> column = new TableColumn<>(entry.getKey());
            String field = entry.getValue().getField();

            // Detectar si el campo es un objeto complejo o una propiedad básica
            if (field.contains(".")) {

                column.setCellValueFactory(cellData -> {

                    T item = cellData.getValue();
                    String[] fieldPath = field.split("\\.");

                    try {
                        Object value = item.getClass().getMethod("get" + capitalize(fieldPath[0])).invoke(item);
                        if (value != null) {
                            Object nestedValue = value.getClass().getMethod("get" + capitalize(fieldPath[1])).invoke(value);
                            return new SimpleObjectProperty<>(nestedValue != null ? nestedValue : "N/A");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    return new SimpleObjectProperty("N/A");
                });
            } else {
                column.setCellValueFactory(new PropertyValueFactory<>(field));
            }

            if (entry.getValue().getWidth() != null) {
                column.setPrefWidth(entry.getValue().getWidth());
            }

            tableView.getColumns().add(column);
        }

        addActionColumn(tableView, updateAction, deleteAction);
        // Ajustar el ancho del TableView según el contenido
        tableView.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
    }

    private void addActionColumn(TableView<T> tableView, Consumer<T> updateAction, Consumer<T> deleteAction) {
        TableColumn<T, Void> actionColumn = new TableColumn<>("Acciones");

        Callback<TableColumn<T, Void>, TableCell<T, Void>> cellFactory = new Callback<>() {
            @Override
            public TableCell<T, Void> call(final TableColumn<T, Void> param) {
                final TableCell<T, Void> cell = new TableCell<>() {

                    // Botones con ícono y texto: "Editar" (blanco) y "Eliminar" (rojo)
                    private final Button btnUpdate = crearBoton("Editar", "/img/document-edit-icon.png", "btn-editar");
                    private final Button btnDelete = crearBoton("Eliminar", "/img/del-icon.png", "btn-eliminar");
                    private final HBox buttons = new HBox(8, btnUpdate, btnDelete);

                    {
                        // Estilo de botón de acción (blanco con borde) y ayuda al pasar el mouse
                        buttons.setAlignment(Pos.CENTER);
                        btnUpdate.setTooltip(new Tooltip("Editar este registro"));
                        btnDelete.setTooltip(new Tooltip("Eliminar este registro"));
                        btnUpdate.setOnAction(event -> {
                            T data = getTableView().getItems().get(getIndex());
                            updateAction.accept(data);  // Llama a la función pasada como parámetro para actualizar
                        });

                        btnDelete.setOnAction(event -> {
                            T data = getTableView().getItems().get(getIndex());
                            // Se pide confirmación antes de eliminar
                            Alert confirmar = new Alert(Alert.AlertType.CONFIRMATION);
                            confirmar.setTitle("Coolbox");
                            confirmar.setHeaderText("¿Eliminar este registro?");
                            confirmar.setContentText("Esta acción no se puede deshacer.");
                            if (getScene() != null && getScene().getWindow() != null) {
                                confirmar.initOwner(getScene().getWindow());
                            }
                            Optional<ButtonType> respuesta = confirmar.showAndWait();
                            if (respuesta.isPresent() && respuesta.get() == ButtonType.OK) {
                                deleteAction.accept(data);  // Llama a la función pasada como parámetro para eliminar
                            }
                        });
                    }

                    @Override
                    public void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(buttons);
                        }
                    }
                };
                return cell;
            }
        };

        actionColumn.setCellFactory(cellFactory);
        actionColumn.setPrefWidth(200);
        actionColumn.setMinWidth(190);
        actionColumn.setSortable(false);
        actionColumn.setStyle("-fx-alignment: CENTER;");
        tableView.getColumns().add(actionColumn);
    }

    /** Crea un botón de acción con ícono pequeño, texto y su clase de estilo. */
    private static Button crearBoton(String texto, String rutaIcono, String estilo) {
        Button b = new Button(texto);
        try {
            ImageView iv = new ImageView(new Image(TableViewHelper.class.getResource(rutaIcono).toExternalForm()));
            iv.setFitWidth(14);
            iv.setFitHeight(14);
            iv.setPreserveRatio(true);
            b.setGraphic(iv);
        } catch (Exception sinIcono) {
            // si falta la imagen, el botón queda solo con texto
        }
        b.getStyleClass().addAll("btn-accion", estilo);
        b.setMinWidth(Region.USE_PREF_SIZE);
        return b;
    }

    /**
     * Da formato de dinero (S/ 0.00, alineado a la derecha) a las columnas indicadas por su título,
     * y centra las columnas de números simples (ej. Cantidad). Se usa en los carritos de Venta y Compra.
     */
    public static <T> void formatoCarrito(TableView<T> tableView, List<String> columnasMoneda, List<String> columnasCentradas) {
        for (TableColumn<T, ?> col : tableView.getColumns()) {
            if (columnasMoneda.contains(col.getText())) {
                @SuppressWarnings("unchecked")
                TableColumn<T, Object> c = (TableColumn<T, Object>) col;
                c.setStyle("-fx-alignment: CENTER-RIGHT;");
                c.setCellFactory(tc -> new TableCell<>() {
                    @Override
                    protected void updateItem(Object valor, boolean empty) {
                        super.updateItem(valor, empty);
                        if (empty || valor == null) { setText(null); return; }
                        try {
                            setText(String.format(Locale.US, "S/ %.2f", Double.parseDouble(valor.toString())));
                        } catch (NumberFormatException e) {
                            setText(valor.toString());
                        }
                    }
                });
            } else if (columnasCentradas.contains(col.getText())) {
                @SuppressWarnings("unchecked")
                TableColumn<T, Object> c = (TableColumn<T, Object>) col;
                c.setStyle("-fx-alignment: CENTER;");
                c.setCellFactory(tc -> new TableCell<>() {
                    @Override
                    protected void updateItem(Object valor, boolean empty) {
                        super.updateItem(valor, empty);
                        if (empty || valor == null) { setText(null); return; }
                        try {
                            double d = Double.parseDouble(valor.toString());
                            setText(d == Math.floor(d) ? String.valueOf((long) d) : String.valueOf(d));
                        } catch (NumberFormatException e) {
                            setText(valor.toString());
                        }
                    }
                });
            }
        }
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

}

