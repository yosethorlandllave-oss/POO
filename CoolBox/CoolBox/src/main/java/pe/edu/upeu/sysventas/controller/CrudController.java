package pe.edu.upeu.sysventas.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import pe.edu.upeu.sysventas.components.Aviso;
import pe.edu.upeu.sysventas.components.Dialogos;
import pe.edu.upeu.sysventas.components.Iconos;
import pe.edu.upeu.sysventas.components.TablaPaginada;
import pe.edu.upeu.sysventas.components.ToltipCustom;
import pe.edu.upeu.sysventas.exception.ModelNotFoundException;
import pe.edu.upeu.sysventas.exception.ReglaNegocioException;
import pe.edu.upeu.sysventas.exception.ValidacionException;
import pe.edu.upeu.sysventas.service.ICrudGenericoService;
import pe.edu.upeu.sysventas.service.impl.CrudGenericoServiceImp;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Base de las pantallas CRUD: listado paginado, formulario de alta/edición,
 * vista de detalle, confirmación de eliminación y mensajes. Cada módulo solo
 * define sus columnas, sus campos y cómo leerlos.
 *
 * La vista FXML de cada módulo debe declarar los nodos @FXML de esta clase.
 */
public abstract class CrudController<T> {

    @FXML protected VBox miContenedor;
    @FXML protected HBox migas;
    @FXML protected Label lblTitulo;
    @FXML protected VBox contenedorAviso;
    @FXML protected VBox panelListado;
    @FXML protected VBox panelFormulario;
    @FXML protected Label lblTituloFormulario;
    @FXML protected VBox panelDetalle;
    @FXML protected Label lblTituloDetalle;
    @FXML protected GridPane gridDetalle;
    @FXML protected HBox accionesDetalle;
    @FXML protected Button btnGuardar;
    @FXML protected FlowPane camposFormulario;

    protected Aviso aviso;
    protected TablaPaginada<T> tabla;
    protected final ToltipCustom ttc = new ToltipCustom();
    /** 0 = registro nuevo; otro valor = id del registro en edición. */
    protected Long idEnEdicion = 0L;
    private Runnable irAInicio = () -> { };

    // ------------------------------------------------------------ contrato de cada módulo

    protected abstract ICrudGenericoService<T, Long> servicio();

    /** Título del listado, en plural: "Productos". */
    protected abstract String titulo();

    /** Grupo del menú al que pertenece: "Catálogo". */
    protected abstract String grupo();

    /** Nombre en singular y minúsculas: "producto". */
    protected abstract String singular();

    /** true si el sustantivo es femenino ("la marca"), para concordar los mensajes. */
    protected abstract boolean femenino();

    protected abstract Long idDe(T t);

    protected abstract String nombreDe(T t);

    protected abstract void configurarTabla(TablaPaginada<T> tabla);

    /** Campos del formulario por nombre de propiedad del modelo, en orden visual. */
    protected abstract Map<String, Control> camposFormulario();

    /** Se llama antes de mostrar el formulario (p. ej. para recargar combos). */
    protected void prepararFormulario() {
    }

    protected abstract void limpiarFormulario();

    protected abstract void llenarFormulario(T t);

    /**
     * Construye la entidad con lo escrito en el formulario. Los valores con
     * formato inválido (p. ej. texto en un campo numérico) se informan en
     * erroresFormato y se dejan en null en la entidad.
     */
    protected abstract T leerFormulario(Map<String, String> erroresFormato);

    /** Filas "etiqueta → valor" de la vista de detalle. */
    protected abstract LinkedHashMap<String, String> datosDetalle(T t);

    // ------------------------------------------------------------ inicio

    @FXML
    public void initialize() {
        aviso = new Aviso();
        aviso.setId("aviso");
        contenedorAviso.getChildren().setAll(aviso);

        tabla = new TablaPaginada<>("Agregar");
        tabla.getBotonAgregar().setAccessibleText((femenino() ? "Agregar nueva " : "Agregar nuevo ") + singular());
        tabla.setNombrePlural(titulo().toLowerCase());
        tabla.getBotonAgregar().setOnAction(e -> nuevo());
        configurarTabla(tabla);
        tabla.setAcciones(this::ver, this::editar, this::confirmarEliminar, this::nombreDe);
        VBox.setVgrow(tabla, Priority.ALWAYS);
        panelListado.getChildren().setAll(tabla);

        ColumnConstraints etiquetas = new ColumnConstraints();
        etiquetas.setMinWidth(140);
        etiquetas.setHalignment(HPos.LEFT);
        ColumnConstraints valores = new ColumnConstraints();
        valores.setHgrow(Priority.ALWAYS);
        gridDetalle.getColumnConstraints().setAll(etiquetas, valores);

        btnGuardar.setGraphic(Iconos.crear(Iconos.GUARDAR, 13));
        // Formulario adaptable: 3, 2 o 1 columnas según el ancho disponible.
        camposFormulario.widthProperty().addListener((obs, a, ancho) -> ajustarColumnasFormulario(ancho.doubleValue()));

        idEnEdicion = 0L;
        mostrarListado();
    }

    private void ajustarColumnasFormulario(double ancho) {
        if (ancho <= 0) {
            return;
        }
        int columnas = ancho >= 900 ? 3 : ancho >= 560 ? 2 : 1;
        // Un formulario de un solo campo no necesita ocupar toda la fila.
        columnas = Math.min(columnas, Math.max(1, camposFormulario.getChildren().size()));
        double anchoCampo = Math.floor((ancho - camposFormulario.getHgap() * (columnas - 1)) / columnas) - 1;
        if (camposFormulario.getChildren().size() == 1) {
            anchoCampo = Math.min(anchoCampo, 420);
        }
        for (Node campo : camposFormulario.getChildren()) {
            if (campo instanceof Region r) {
                r.setPrefWidth(anchoCampo);
            }
        }
    }

    /** Acción para el enlace "Inicio" de la ruta de navegación. */
    public void setIrAInicio(Runnable irAInicio) {
        this.irAInicio = irAInicio;
        construirMigas();
    }

    // ------------------------------------------------------------ navegación entre paneles

    public void mostrarListado() {
        mostrarPanel(panelListado);
        construirMigas();
        recargarListado();
    }

    public void recargarListado() {
        tabla.setDatos(servicio().findAll());
    }

    public void nuevo() {
        aviso.ocultar();
        prepararFormulario();
        limpiarFormulario();
        limpiarErrores();
        idEnEdicion = 0L;
        lblTituloFormulario.setText((femenino() ? "Nueva " : "Nuevo ") + singular());
        mostrarPanel(panelFormulario);
        construirMigas(femenino() ? "Nueva" : "Nuevo");
        enfocarPrimerCampo();
    }

    public void editar(T fila) {
        aviso.ocultar();
        T actual = buscarVigente(fila);
        if (actual == null) {
            return;
        }
        prepararFormulario();
        limpiarFormulario();
        limpiarErrores();
        llenarFormulario(actual);
        idEnEdicion = idDe(actual);
        lblTituloFormulario.setText("Editar " + singular() + " #" + idEnEdicion);
        mostrarPanel(panelFormulario);
        construirMigas("Editar");
        enfocarPrimerCampo();
    }

    public void ver(T fila) {
        aviso.ocultar();
        T actual = buscarVigente(fila);
        if (actual == null) {
            return;
        }
        lblTituloDetalle.setText(capitalizar(singular()) + " #" + idDe(actual) + ": " + nombreDe(actual));
        gridDetalle.getChildren().clear();
        int fila0 = 0;
        for (Map.Entry<String, String> dato : datosDetalle(actual).entrySet()) {
            Label etiqueta = new Label(dato.getKey());
            etiqueta.getStyleClass().add("detalle-etiqueta");
            Label valor = new Label(dato.getValue() == null || dato.getValue().isBlank() ? "—" : dato.getValue());
            valor.getStyleClass().add("detalle-valor");
            valor.setWrapText(true);
            etiqueta.setMaxWidth(Double.MAX_VALUE);
            valor.setMaxWidth(Double.MAX_VALUE);
            etiqueta.setLabelFor(valor);
            gridDetalle.addRow(fila0++, etiqueta, valor);
        }

        Button btnEditar = boton("Editar", Iconos.EDITAR, "btn-warning", () -> editar(actual));
        Button btnEliminar = boton("Eliminar", Iconos.ELIMINAR, "btn-danger", () -> confirmarEliminar(actual));
        Button btnVolver = boton("Volver al listado", Iconos.VOLVER, "btn-default", this::mostrarListado);
        accionesDetalle.getChildren().setAll(btnEditar, btnEliminar, btnVolver);

        mostrarPanel(panelDetalle);
        construirMigas("Detalle");
        btnVolver.requestFocus();
    }

    // ------------------------------------------------------------ acciones

    @FXML
    public void validarFormulario() {
        guardar();
    }

    /** Valida y guarda el formulario. Devuelve true si se guardó. */
    public boolean guardar() {
        limpiarErrores();
        Map<String, String> errores = new LinkedHashMap<>();
        T entidad = leerFormulario(errores);
        // Los errores de formato tienen prioridad sobre "es obligatorio" del mismo campo.
        CrudGenericoServiceImp.erroresDe(entidad).forEach(errores::putIfAbsent);
        if (!errores.isEmpty()) {
            mostrarErrores(errores);
            return false;
        }

        boolean esNuevo = idEnEdicion == null || idEnEdicion == 0L;
        try {
            T guardado = esNuevo ? servicio().save(entidad) : servicio().update(idEnEdicion, entidad);
            String articulo = femenino() ? "La " : "El ";
            String participio = esNuevo
                    ? (femenino() ? "registrada" : "registrado")
                    : (femenino() ? "actualizada" : "actualizado");
            idEnEdicion = 0L;
            mostrarListado();
            aviso.exito(articulo + singular() + " «" + nombreDe(guardado) + "» fue " + participio
                    + " correctamente.");
            return true;
        } catch (ValidacionException e) {
            mostrarErrores(e.getErrores());
        } catch (ReglaNegocioException e) {
            Control campo = e.getCampo() == null ? null : camposFormulario().get(e.getCampo());
            if (campo != null) {
                ttc.marcarError(campo, e.getMessage());
                Platform.runLater(campo::requestFocus);
            }
            aviso.error(e.getMessage());
        } catch (ModelNotFoundException e) {
            idEnEdicion = 0L;
            mostrarListado();
            aviso.error((femenino() ? "La " : "El ") + singular()
                    + " que estabas editando ya no existe. Se recargó el listado.");
        }
        return false;
    }

    @FXML
    public void cancelarFormulario() {
        limpiarErrores();
        idEnEdicion = 0L;
        aviso.ocultar();
        mostrarListado();
    }

    public void confirmarEliminar(T fila) {
        String articulo = femenino() ? "la " : "el ";
        Dialogos.confirmar(miContenedor,
                "Eliminar " + singular(),
                "¿Seguro que deseas eliminar " + articulo + singular() + " «" + nombreDe(fila)
                        + "»? Esta acción no se puede deshacer.",
                "Eliminar", true,
                () -> eliminar(fila));
    }

    /** Elimina sin pedir confirmación (la confirmación la hace confirmarEliminar). */
    public void eliminar(T fila) {
        String articulo = femenino() ? "La " : "El ";
        try {
            servicio().delete(idDe(fila));
            mostrarListado();
            aviso.exito(articulo + singular() + " «" + nombreDe(fila) + "» fue "
                    + (femenino() ? "eliminada" : "eliminado") + " correctamente.");
        } catch (ReglaNegocioException e) {
            // La restricción se conserva: se explica el motivo y no se elimina.
            mostrarListado();
            aviso.error(e.getMessage());
        } catch (ModelNotFoundException e) {
            mostrarListado();
            aviso.error(articulo + singular() + " «" + nombreDe(fila) + "» ya no existe. Se recargó el listado.");
        }
    }

    // ------------------------------------------------------------ apoyo

    /** Relee el registro desde el servicio; si ya no existe, avisa y recarga el listado. */
    private T buscarVigente(T fila) {
        try {
            return servicio().findById(idDe(fila));
        } catch (ModelNotFoundException e) {
            mostrarListado();
            aviso.error((femenino() ? "La " : "El ") + singular() + " «" + nombreDe(fila)
                    + "» ya no existe. Se recargó el listado.");
            return null;
        }
    }

    protected void mostrarErrores(Map<String, String> errores) {
        Control primero = null;
        int marcados = 0;
        for (Map.Entry<String, Control> campo : camposFormulario().entrySet()) {
            String mensaje = errores.get(campo.getKey());
            if (mensaje != null) {
                ttc.marcarError(campo.getValue(), mensaje);
                marcados++;
                if (primero == null) {
                    primero = campo.getValue();
                }
            }
        }
        if (marcados == 0) {
            aviso.error(errores.values().iterator().next());
        } else {
            aviso.error(marcados == 1
                    ? "Revisa el campo marcado: " + errores.get(primerCampoConError(errores)) + "."
                    : "Revisa los " + marcados + " campos marcados en rojo.");
        }
        if (primero != null) {
            Control foco = primero;
            Platform.runLater(foco::requestFocus);
        }
    }

    private String primerCampoConError(Map<String, String> errores) {
        return camposFormulario().keySet().stream().filter(errores::containsKey).findFirst().orElse("");
    }

    protected void limpiarErrores() {
        camposFormulario().values().forEach(ttc::limpiarCampo);
    }

    private void enfocarPrimerCampo() {
        camposFormulario().values().stream().findFirst().ifPresent(c -> Platform.runLater(c::requestFocus));
    }

    private void mostrarPanel(VBox visible) {
        for (VBox panel : List.of(panelListado, panelFormulario, panelDetalle)) {
            panel.setVisible(panel == visible);
            panel.setManaged(panel == visible);
        }
    }

    /** Ruta de navegación: Inicio › Grupo › Título [› paso]. */
    private void construirMigas(String... paso) {
        lblTitulo.setText(titulo());
        Hyperlink inicio = new Hyperlink("Inicio");
        inicio.setGraphic(Iconos.crear(Iconos.INICIO, 12));
        inicio.setOnAction(e -> irAInicio.run());
        Label grupo = new Label(grupo());
        Node modulo;
        if (paso.length == 0) {
            Label actual = new Label(titulo());
            actual.getStyleClass().add("miga-actual");
            modulo = actual;
        } else {
            Hyperlink enlace = new Hyperlink(titulo());
            enlace.setOnAction(e -> cancelarFormulario());
            modulo = enlace;
        }
        migas.getChildren().setAll(inicio, separadorMiga(), grupo, separadorMiga(), modulo);
        if (paso.length > 0) {
            Label actual = new Label(paso[0]);
            actual.getStyleClass().add("miga-actual");
            migas.getChildren().addAll(separadorMiga(), actual);
        }
        migas.setAccessibleText("Ruta de navegación");
    }

    private static Label separadorMiga() {
        Label s = new Label("›");
        s.getStyleClass().add("miga-separador");
        return s;
    }

    private static Button boton(String texto, String icono, String estilo, Runnable accion) {
        Button b = new Button(texto, Iconos.crear(icono, 13));
        b.getStyleClass().addAll("btn", estilo);
        b.setOnAction(e -> accion.run());
        b.setAccessibleText(texto);
        return b;
    }

    protected static String capitalizar(String texto) {
        return texto.isEmpty() ? texto : Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }
}
