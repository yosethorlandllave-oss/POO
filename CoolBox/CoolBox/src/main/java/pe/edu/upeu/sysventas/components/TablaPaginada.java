package pe.edu.upeu.sysventas.components;

import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Listado reutilizable con el diseño del panel: barra de herramientas (Agregar,
 * cantidad de registros, filtros y buscador), tabla con acciones por fila
 * (ver, editar, eliminar), contador de resultados y paginación.
 *
 * La búsqueda, el orden por columna y la paginación trabajan sobre la lista
 * completa, no solo sobre la página visible.
 */
public class TablaPaginada<T> extends VBox {

    public static final List<Integer> CANTIDADES = List.of(5, 10, 25, 50, 100);
    private static final double ANCHO_APILADO = 720;
    private static final double ALTO_FILA = 36;
    /** Encabezado de 34 px más sus bordes, con un pequeño margen. */
    private static final double ALTO_ENCABEZADO = 38;
    /** Reserva para la barra de desplazamiento horizontal cuando las columnas no caben. */
    private static final double ALTO_BARRA_H = 20;

    private final TableView<T> tabla = new TableView<>();
    private final Button btnAgregar = new Button();
    private final ComboBox<Integer> cbxCantidad = new ComboBox<>();
    private final TextField txtBuscar = new TextField();
    private final List<Node> filtros = new ArrayList<>();
    private final FlowPane filtrosExtra = new FlowPane(8, 8);
    private final HBox buscador;
    private final Label lblContador = new Label();
    private final HBox paginacion = new HBox(2);
    private final Label placeholder = new Label();

    private final HBox barraIzquierda;
    private final HBox barraDerecha;
    private final Pane barra = new StackPane();
    private final Pane pie = new StackPane();

    private List<T> todos = List.of();
    private List<T> visibles = List.of();
    private int pagina = 0;
    private boolean actualizando = false;
    private boolean columnasCaben = true;
    private int filasVisibles = 3;

    private BiPredicate<T, String> filtroTexto = (t, q) -> true;
    private Predicate<T> filtroAdicional = t -> true;
    private String nombrePlural = "registros";

    private TableColumn<T, ?> columnaFlexible;
    private final Map<TableColumn<T, ?>, Double> anchosBase = new LinkedHashMap<>();
    private TableColumn<T, Void> columnaAcciones;

    public TablaPaginada(String textoAgregar) {
        getStyleClass().add("tabla-paginada");
        // Nunca más ancho que la página: si no cabe, desplaza la tabla, no la página.
        setMinWidth(0);
        tabla.setMinWidth(0);
        setSpacing(10);

        // --- Barra de herramientas ---
        btnAgregar.setText(textoAgregar);
        btnAgregar.setGraphic(Iconos.crear(Iconos.AGREGAR, 14));
        btnAgregar.getStyleClass().addAll("btn", "btn-success");
        btnAgregar.setId("btnAgregar");
        btnAgregar.setAccessibleText(textoAgregar);

        cbxCantidad.getItems().setAll(CANTIDADES);
        cbxCantidad.setValue(10);
        cbxCantidad.getStyleClass().add("selector-cantidad");
        cbxCantidad.setAccessibleText("Cantidad de registros por página");
        cbxCantidad.setOnAction(e -> recalcular(true));
        Label lblMostrar = new Label("Mostrar");
        Label lblRegistros = new Label("registros");
        lblMostrar.setLabelFor(cbxCantidad);

        barraIzquierda = new HBox(8, btnAgregar, separador(), lblMostrar, cbxCantidad, lblRegistros);
        barraIzquierda.setAlignment(Pos.CENTER_LEFT);

        txtBuscar.setPromptText("Buscar...");
        txtBuscar.setAccessibleText("Buscar en el listado");
        txtBuscar.getStyleClass().add("campo-busqueda");
        txtBuscar.setId("txtBuscar");
        txtBuscar.textProperty().addListener((obs, anterior, nuevo) -> recalcular(true));
        txtBuscar.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                txtBuscar.clear();
            }
        });
        HBox.setHgrow(txtBuscar, Priority.ALWAYS);
        buscador = new HBox(Iconos.crear(Iconos.BUSCAR, 14), txtBuscar);
        buscador.getStyleClass().add("buscador");
        buscador.setAlignment(Pos.CENTER_LEFT);
        buscador.setPrefWidth(240);
        buscador.setMaxWidth(Double.MAX_VALUE);

        filtrosExtra.setAlignment(Pos.CENTER_LEFT);
        // Evita que el FlowPane reserve 400 px (su largo de ajuste por defecto).
        filtrosExtra.setPrefWrapLength(10);
        barraDerecha = new HBox(8);
        barraDerecha.setAlignment(Pos.CENTER_RIGHT);
        barra.getStyleClass().add("barra-herramientas");

        // --- Tabla ---
        tabla.getStyleClass().add("tabla");
        tabla.setId("tablaListado");
        placeholder.getStyleClass().add("tabla-vacia");
        tabla.setPlaceholder(placeholder);
        tabla.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        tabla.setFixedCellSize(ALTO_FILA);
        tabla.setSortPolicy(t -> {
            recalcular(false);
            return true;
        });
        tabla.widthProperty().addListener((obs, a, n) -> ajustarColumnas());

        // --- Pie: contador y paginación ---
        lblContador.getStyleClass().add("contador");
        lblContador.setId("lblContador");
        paginacion.getStyleClass().add("paginacion");
        paginacion.setAlignment(Pos.CENTER_RIGHT);
        pie.getStyleClass().add("pie-tabla");

        getChildren().addAll(barra, tabla, pie);
        widthProperty().addListener((obs, a, n) -> acomodar(n.doubleValue()));
        acomodar(1000);
        recalcular(true);
    }

    // ------------------------------------------------------------ columnas

    /** Columna cuyo valor se muestra con toString() y se ordena por ese mismo valor. */
    public TableColumn<T, Object> agregarColumna(String titulo, double ancho, Function<T, ?> valor) {
        return agregarColumna(titulo, ancho, valor, t -> {
            Object v = valor.apply(t);
            return v == null ? "" : v.toString();
        }, false);
    }

    /**
     * Columna con un valor para ordenar y un texto para mostrar (p. ej. un
     * número y su versión formateada). Las numéricas se alinean a la derecha.
     */
    public TableColumn<T, Object> agregarColumna(String titulo, double ancho, Function<T, ?> valorOrden,
                                                 Function<T, String> texto, boolean numerica) {
        TableColumn<T, Object> col = new TableColumn<>(titulo);
        col.setCellValueFactory(cd -> new SimpleObjectProperty<>(valorOrden.apply(cd.getValue())));
        col.setComparator(TablaPaginada::compararValores);
        col.setPrefWidth(ancho);
        col.setMinWidth(Math.min(ancho, 50));
        anchosBase.put(col, ancho);
        if (numerica) {
            col.getStyleClass().add("col-numero");
        }
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                T fila = empty || getTableRow() == null ? null : getTableRow().getItem();
                setText(fila == null ? null : texto.apply(fila));
            }
        });
        tabla.getColumns().add(col);
        return col;
    }

    /** La columna indicada crece para ocupar el ancho libre de la tabla. */
    public void setColumnaFlexible(TableColumn<T, ?> columna) {
        columnaFlexible = columna;
        ajustarColumnas();
    }

    /**
     * Columna de acciones. Cada acción puede ser null si no aplica.
     * descripcion da el nombre del registro para los textos accesibles.
     */
    public void setAcciones(Consumer<T> ver, Consumer<T> editar, Consumer<T> eliminar,
                            Function<T, String> descripcion) {
        TableColumn<T, Void> col = new TableColumn<>("Acciones");
        col.setSortable(false);
        col.setResizable(false);
        col.getStyleClass().add("col-acciones");
        int botones = (ver != null ? 1 : 0) + (editar != null ? 1 : 0) + (eliminar != null ? 1 : 0);
        // Botones de 28 px separados 4 px, más el relleno de la celda.
        double ancho = 16 + botones * 28 + Math.max(0, botones - 1) * 4;
        col.setPrefWidth(ancho);
        col.setMinWidth(ancho);
        anchosBase.put(col, ancho);
        col.setCellFactory(c -> new TableCell<>() {
            private final Button btnVer = botonAccion(Iconos.VER, "btn-ver");
            private final Button btnEditar = botonAccion(Iconos.EDITAR, "btn-editar");
            private final Button btnEliminar = botonAccion(Iconos.ELIMINAR, "btn-eliminar");
            private final HBox caja = new HBox(4);

            {
                caja.setAlignment(Pos.CENTER);
                if (ver != null) {
                    caja.getChildren().add(btnVer);
                    btnVer.setOnAction(e -> ejecutar(ver));
                }
                if (editar != null) {
                    caja.getChildren().add(btnEditar);
                    btnEditar.setOnAction(e -> ejecutar(editar));
                }
                if (eliminar != null) {
                    caja.getChildren().add(btnEliminar);
                    btnEliminar.setOnAction(e -> ejecutar(eliminar));
                }
            }

            private void ejecutar(Consumer<T> accion) {
                T fila = getTableRow() == null ? null : getTableRow().getItem();
                if (fila != null) {
                    accion.accept(fila);
                }
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                T fila = empty || getTableRow() == null ? null : getTableRow().getItem();
                if (fila == null) {
                    setGraphic(null);
                    return;
                }
                String nombre = descripcion.apply(fila);
                etiquetar(btnVer, "Ver " + nombre);
                etiquetar(btnEditar, "Editar " + nombre);
                etiquetar(btnEliminar, "Eliminar " + nombre);
                setGraphic(caja);
            }
        });
        tabla.getColumns().add(col);
        columnaAcciones = col;
        ajustarColumnas();
    }

    // ------------------------------------------------------------ datos

    public void setNombrePlural(String nombrePlural) {
        this.nombrePlural = nombrePlural;
        recalcular(false);
    }

    /** Criterio del buscador; recibe el texto ya en minúsculas y sin espacios en los extremos. */
    public void setFiltroTexto(BiPredicate<T, String> filtroTexto) {
        this.filtroTexto = filtroTexto;
        recalcular(true);
    }

    /** Criterio de los filtros extra (combos de la barra). */
    public void setFiltroAdicional(Predicate<T> filtroAdicional) {
        this.filtroAdicional = filtroAdicional;
        recalcular(true);
    }

    /** Controles de filtro que se muestran junto al buscador. */
    public void agregarFiltro(Node filtro) {
        filtros.add(filtro);
        acomodar(getWidth() > 0 ? getWidth() : 1000);
    }

    /** Reemplaza los datos manteniendo la página, búsqueda y orden actuales. */
    public void setDatos(List<T> datos) {
        todos = new ArrayList<>(datos);
        recalcular(false);
    }

    /** Vuelve a aplicar filtros y orden desde la primera página. */
    public void refrescarFiltros() {
        recalcular(true);
    }

    public Button getBotonAgregar() {
        return btnAgregar;
    }

    public TableView<T> getTabla() {
        return tabla;
    }

    public TextField getCampoBusqueda() {
        return txtBuscar;
    }

    public ComboBox<Integer> getSelectorCantidad() {
        return cbxCantidad;
    }

    public Label getContador() {
        return lblContador;
    }

    public int getPaginaActual() {
        return pagina + 1;
    }

    public int getTotalPaginas() {
        return Math.max(1, (int) Math.ceil(visibles.size() / (double) cbxCantidad.getValue()));
    }

    /** Registros que cumplen la búsqueda y los filtros, en el orden actual. */
    public List<T> getResultados() {
        return List.copyOf(visibles);
    }

    public void irAPagina(int numero) {
        pagina = Math.max(0, Math.min(numero - 1, getTotalPaginas() - 1));
        recalcular(false);
    }

    // ------------------------------------------------------------ interno

    private void recalcular(boolean volverAPrimera) {
        if (actualizando) {
            return;
        }
        actualizando = true;
        try {
            String q = txtBuscar.getText() == null ? "" : txtBuscar.getText().trim().toLowerCase(Locale.ROOT);
            List<T> filtrados = new ArrayList<>();
            for (T t : todos) {
                if (filtroAdicional.test(t) && (q.isEmpty() || filtroTexto.test(t, q))) {
                    filtrados.add(t);
                }
            }
            Comparator<T> orden = tabla.getComparator();
            if (orden != null) {
                filtrados.sort(orden);
            }
            visibles = filtrados;

            int tamano = cbxCantidad.getValue() == null ? 10 : cbxCantidad.getValue();
            int paginas = getTotalPaginas();
            pagina = volverAPrimera ? 0 : Math.min(pagina, paginas - 1);
            int desde = pagina * tamano;
            int hasta = Math.min(visibles.size(), desde + tamano);
            tabla.getItems().setAll(visibles.subList(desde, hasta));
            // Altura justa para las filas de la página (mínimo 3 para el mensaje de vacío),
            // como en un listado web, sin un bloque de filas vacías debajo.
            filasVisibles = Math.max(3, hasta - desde);
            actualizarAltura();

            int n = visibles.size();
            if (n == 0) {
                lblContador.setText(todos.isEmpty() ? "No hay registros" : "Mostrando 0 registros");
            } else {
                String texto = String.format("Mostrando %d a %d de %d registros", desde + 1, hasta, n);
                if (n < todos.size()) {
                    texto += String.format(" (filtrado de %d registros en total)", todos.size());
                }
                lblContador.setText(texto);
            }
            placeholder.setText(todos.isEmpty()
                    ? "Aún no hay " + nombrePlural + " registrados."
                    : "Ningún registro coincide con la búsqueda o los filtros.");
            construirPaginacion(paginas);
        } finally {
            actualizando = false;
        }
    }

    private void construirPaginacion(int paginas) {
        paginacion.getChildren().clear();
        Button anterior = new Button("Anterior");
        anterior.getStyleClass().add("boton-pagina");
        anterior.setAccessibleText("Página anterior");
        anterior.setDisable(pagina == 0);
        anterior.setOnAction(e -> irAPagina(pagina));
        paginacion.getChildren().add(anterior);

        int ultimaMostrada = 0;
        for (int i = 1; i <= paginas; i++) {
            boolean visible = i == 1 || i == paginas || Math.abs(i - (pagina + 1)) <= 1;
            if (!visible) {
                continue;
            }
            if (i - ultimaMostrada > 1) {
                Label puntos = new Label("…");
                puntos.getStyleClass().add("pagina-puntos");
                paginacion.getChildren().add(puntos);
            }
            int numero = i;
            Button b = new Button(String.valueOf(i));
            b.getStyleClass().add("boton-pagina");
            if (i == pagina + 1) {
                b.getStyleClass().add("activa");
            }
            b.setAccessibleText("Página " + i + " de " + paginas);
            b.setOnAction(e -> irAPagina(numero));
            paginacion.getChildren().add(b);
            ultimaMostrada = i;
        }

        Button siguiente = new Button("Siguiente");
        siguiente.getStyleClass().add("boton-pagina");
        siguiente.setAccessibleText("Página siguiente");
        siguiente.setDisable(pagina >= paginas - 1);
        siguiente.setOnAction(e -> irAPagina(pagina + 2));
        paginacion.getChildren().add(siguiente);
    }

    /** En anchos pequeños la barra y el pie se apilan en vertical. */
    private void acomodar(double ancho) {
        boolean apilado = ancho < ANCHO_APILADO;
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        Region espacioPie = new Region();
        HBox.setHgrow(espacioPie, Priority.ALWAYS);

        if (apilado) {
            // Filtros en un FlowPane que salta de línea si no caben; buscador a todo lo ancho.
            filtrosExtra.getChildren().setAll(filtros);
            barraIzquierda.setMinWidth(Region.USE_COMPUTED_SIZE);
            VBox v = new VBox(8, barraIzquierda);
            if (!filtros.isEmpty()) {
                v.getChildren().add(filtrosExtra);
            }
            v.getChildren().add(buscador);
            barra.getChildren().setAll(v);
            VBox vp = new VBox(6, lblContador, paginacion);
            paginacion.setAlignment(Pos.CENTER_LEFT);
            pie.getChildren().setAll(vp);
        } else {
            // En una sola fila: los filtros van directo en el HBox (un FlowPane
            // horizontal reclamaría como mínimo su prefWrapLength de ancho).
            filtrosExtra.getChildren().clear();
            barraDerecha.getChildren().setAll(filtros);
            barraDerecha.getChildren().add(buscador);
            barraIzquierda.setMinWidth(Region.USE_PREF_SIZE);
            HBox h = new HBox(8, barraIzquierda, espacio, barraDerecha);
            h.setAlignment(Pos.CENTER_LEFT);
            barra.getChildren().setAll(h);
            HBox hp = new HBox(8, lblContador, espacioPie, paginacion);
            hp.setAlignment(Pos.CENTER_LEFT);
            paginacion.setAlignment(Pos.CENTER_RIGHT);
            pie.getChildren().setAll(hp);
        }
    }

    /**
     * Si las columnas caben: política "constrained" de JavaFX (ajuste exacto,
     * nunca barra horizontal); las columnas quedan limitadas a su ancho base,
     * así que el sobrante lo toma la columna flexible, y las acciones van al
     * final como en la referencia. Si no caben (tablet o móvil): anchos base,
     * desplazamiento horizontal y acciones en la primera columna para que
     * sigan a la vista.
     */
    private void ajustarColumnas() {
        if (tabla.getWidth() <= 0) {
            return;
        }
        // 2 px de borde y 2 de margen; la altura se ajusta a las filas de la
        // página, así que la barra vertical no aparece.
        double disponible = tabla.getWidth() - 4;
        double base = 0;
        for (double ancho : anchosBase.values()) {
            base += ancho;
        }
        boolean caben = base <= disponible;
        if (caben) {
            anchosBase.forEach((c, ancho) ->
                    c.setMaxWidth(c == columnaFlexible ? Double.MAX_VALUE : ancho));
            if (tabla.getColumnResizePolicy() != TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS) {
                tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
            }
        } else if (tabla.getColumnResizePolicy() != TableView.UNCONSTRAINED_RESIZE_POLICY) {
            tabla.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
            anchosBase.forEach((c, ancho) -> {
                c.setMaxWidth(Double.MAX_VALUE);
                // Forzar que el ancho vuelva al base (el cambio de prefWidth lo aplica).
                c.setPrefWidth(ancho + 1);
                c.setPrefWidth(ancho);
            });
        }
        if (caben != columnasCaben) {
            columnasCaben = caben;
            actualizarAltura();
        }
        if (columnaAcciones != null) {
            int destino = caben ? tabla.getColumns().size() - 1 : 0;
            if (tabla.getColumns().indexOf(columnaAcciones) != destino) {
                tabla.getColumns().remove(columnaAcciones);
                tabla.getColumns().add(destino, columnaAcciones);
            }
        }
    }

    /** La barra horizontal solo ocupa espacio cuando las columnas no caben. */
    private void actualizarAltura() {
        double alto = ALTO_ENCABEZADO + filasVisibles * ALTO_FILA + 2 + (columnasCaben ? 0 : ALTO_BARRA_H);
        tabla.setPrefHeight(alto);
        tabla.setMinHeight(alto);
    }

    private static Region separador() {
        Region r = new Region();
        r.setMinWidth(4);
        return r;
    }

    private static Button botonAccion(String trazo, String estilo) {
        Button b = new Button();
        b.setGraphic(Iconos.crear(trazo, 13));
        b.getStyleClass().addAll("btn-accion", estilo);
        b.setFocusTraversable(true);
        return b;
    }

    private static void etiquetar(Button b, String texto) {
        b.setAccessibleText(texto);
        Tooltip tip = b.getTooltip();
        if (tip == null) {
            b.setTooltip(new Tooltip(texto));
        } else {
            tip.setText(texto);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static int compararValores(Object a, Object b) {
        if (a == b) {
            return 0;
        }
        if (a == null) {
            return 1;
        }
        if (b == null) {
            return -1;
        }
        if (a instanceof String sa && b instanceof String sb) {
            return sa.compareToIgnoreCase(sb);
        }
        if (a instanceof Comparable ca && a.getClass().isInstance(b)) {
            return ca.compareTo(b);
        }
        return a.toString().compareToIgnoreCase(b.toString());
    }
}
