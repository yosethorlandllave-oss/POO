package pe.edu.upeu.sysventas.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import pe.edu.upeu.sysventas.components.Aviso;
import pe.edu.upeu.sysventas.components.Dialogos;
import pe.edu.upeu.sysventas.components.Iconos;
import pe.edu.upeu.sysventas.config.AppContext;
import pe.edu.upeu.sysventas.service.ICategoriaService;
import pe.edu.upeu.sysventas.service.IMarcaService;
import pe.edu.upeu.sysventas.service.IProductoService;
import pe.edu.upeu.sysventas.service.IUnidadMedidaService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntSupplier;

/**
 * Ventana principal: barra superior, menú lateral agrupado y área de contenido.
 * El menú se adapta al ancho: completo en escritorio (contraíble a iconos),
 * solo iconos en tablet y oculto en pantallas angostas, donde se abre encima
 * del contenido.
 */
@RequiredArgsConstructor
public class MainGuiController {

    public static final double ANCHO_TABLET = 992;
    public static final double ANCHO_MOVIL = 600;

    private final IProductoService ps;
    private final IMarcaService ms;
    private final ICategoriaService cs;
    private final IUnidadMedidaService us;

    @FXML BorderPane bp;
    @FXML StackPane capaModal;
    @FXML HBox cuerpo;
    @FXML ScrollPane panelMenu;
    @FXML VBox menuLateral;
    @FXML StackPane contenido;
    @FXML Button btnMenu;
    @FXML Label lblUsuario;

    /** Una opción del menú. controlador es null cuando la vista declara fx:controller. */
    public record Opcion(String id, String grupo, String texto, String icono, String fxml,
                         Class<?> controlador) {
    }

    private final List<Opcion> opciones = List.of(
            new Opcion("inicio", "PRINCIPAL", "Inicio", Iconos.INICIO, null, null),
            new Opcion("productos", "CATÁLOGO", "Productos", Iconos.PRODUCTO, "/view/main_producto.fxml", null),
            new Opcion("marcas", "CATÁLOGO", "Marcas", Iconos.MARCA, "/view/main_catalogo.fxml", MarcaController.class),
            new Opcion("categorias", "CATÁLOGO", "Categorías", Iconos.CATEGORIA, "/view/main_catalogo.fxml",
                    CategoriaController.class),
            new Opcion("unidades", "CATÁLOGO", "Unidades de medida", Iconos.UNIDAD, "/view/main_catalogo.fxml",
                    UnidadMedidaController.class),
            new Opcion("salir", "SISTEMA", "Salir", Iconos.SALIR, null, null)
    );

    private final Map<String, Button> botones = new LinkedHashMap<>();
    private final List<Label> encabezados = new ArrayList<>();
    private String activo;
    private Object controladorActivo;

    private enum Modo { COMPLETO, MINI, OCULTO }

    private Modo modo = Modo.COMPLETO;
    private boolean contraidoEnEscritorio = false;
    private boolean expandidoEnTablet = false;
    private boolean abiertoEnMovil = false;
    private StackPane capaMenuMovil;

    @FXML
    public void initialize() {
        String grupoActual = null;
        for (Opcion op : opciones) {
            if (!op.grupo().equals(grupoActual)) {
                grupoActual = op.grupo();
                Label encabezado = new Label(grupoActual);
                encabezado.getStyleClass().add("menu-grupo");
                encabezados.add(encabezado);
                menuLateral.getChildren().add(encabezado);
            }
            Button b = new Button(op.texto(), Iconos.crear(op.icono(), 16));
            b.getStyleClass().add("item-menu");
            b.setId("menu-" + op.id());
            b.setMaxWidth(Double.MAX_VALUE);
            b.setAlignment(Pos.CENTER_LEFT);
            b.setAccessibleText(op.texto());
            b.setTooltip(new Tooltip(op.texto()));
            b.setOnAction(e -> navegar(op.id()));
            botones.put(op.id(), b);
            menuLateral.getChildren().add(b);
        }

        btnMenu.setGraphic(Iconos.crear(Iconos.MENU, 18));
        btnMenu.setOnAction(e -> alternarMenu());

        String usuario = System.getProperty("user.name", "usuario");
        lblUsuario.setText(usuario);
        lblUsuario.setGraphic(Iconos.crear(Iconos.USUARIO, 16));
        lblUsuario.setTooltip(new Tooltip("Sesión de Windows: " + usuario
                + ". CoolBox todavía no tiene inicio de sesión propio."));
        lblUsuario.setAccessibleText("Usuario conectado: " + usuario);

        bp.sceneProperty().addListener((obs, anterior, escena) -> {
            if (escena != null) {
                escena.widthProperty().addListener((o, a, ancho) -> aplicarModo());
                aplicarModo();
            }
        });

        navegar("inicio");
    }

    // ------------------------------------------------------------ navegación

    public void navegar(String id) {
        Opcion op = opciones.stream().filter(o -> o.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Opción de menú desconocida: " + id));
        if (modo == Modo.OCULTO && abiertoEnMovil) {
            abiertoEnMovil = false;
            aplicarModo();
        }
        switch (op.id()) {
            case "salir" -> Dialogos.confirmar(contenido, "Salir de CoolBox",
                    "¿Deseas cerrar CoolBox? Los datos se guardan solo en memoria mientras la aplicación "
                            + "está abierta: al salir se perderán los registros creados o modificados.",
                    "Salir", true, Platform::exit);
            case "inicio" -> {
                mostrarInicio();
                marcarActivo(op.id());
            }
            default -> {
                abrirModulo(op);
                marcarActivo(op.id());
            }
        }
    }

    private void abrirModulo(Opcion op) {
        try {
            AppContext context = AppContext.getInstance();
            FXMLLoader loader = new FXMLLoader(getClass().getResource(op.fxml()));
            if (op.controlador() != null) {
                loader.setController(context.getBean(op.controlador()));
            } else {
                loader.setControllerFactory(context::getBean);
            }
            Parent vista = loader.load();
            controladorActivo = loader.getController();
            if (controladorActivo instanceof CrudController<?> crud) {
                crud.setIrAInicio(() -> navegar("inicio"));
            }
            mostrarEnContenido(vista);
        } catch (IOException | RuntimeException ex) {
            ex.printStackTrace();
            Aviso error = new Aviso();
            error.error("No se pudo abrir el módulo «" + op.texto() + "»: " + ex.getMessage());
            VBox pagina = new VBox(error);
            pagina.getStyleClass().add("pagina");
            pagina.setMinWidth(0);
            mostrarEnContenido(pagina);
        }
    }

    private void mostrarEnContenido(Parent vista) {
        ScrollPane scroll = new ScrollPane(vista);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.getStyleClass().add("scroll-contenido");
        contenido.getChildren().setAll(scroll);
    }

    private void marcarActivo(String id) {
        activo = id;
        botones.forEach((clave, b) -> {
            b.getStyleClass().remove("activo");
            if (clave.equals(id)) {
                b.getStyleClass().add("activo");
            }
        });
    }

    public String getOpcionActiva() {
        return activo;
    }

    public Object getControladorActivo() {
        return controladorActivo;
    }

    // ------------------------------------------------------------ inicio

    private void mostrarInicio() {
        controladorActivo = null;
        Label miga = new Label("Inicio");
        miga.setGraphic(Iconos.crear(Iconos.INICIO, 12));
        miga.getStyleClass().add("miga-actual");
        HBox migas = new HBox(miga);
        migas.getStyleClass().add("migas");

        Label titulo = new Label("Panel de inicio");
        titulo.getStyleClass().add("titulo-pagina");

        Aviso info = new Aviso();
        info.info("Los datos se guardan en memoria mientras CoolBox está abierto: al cerrar la aplicación "
                + "se pierden los cambios y se vuelven a cargar los datos de ejemplo.");

        FlowPane tarjetas = new FlowPane(16, 16);
        tarjetas.getStyleClass().add("tarjetas");
        tarjetas.getChildren().addAll(
                tarjeta("productos", "Productos", Iconos.PRODUCTO, () -> ps.findAll().size()),
                tarjeta("marcas", "Marcas", Iconos.MARCA, () -> ms.findAll().size()),
                tarjeta("categorias", "Categorías", Iconos.CATEGORIA, () -> cs.findAll().size()),
                tarjeta("unidades", "Unidades de medida", Iconos.UNIDAD, () -> us.findAll().size()));

        VBox pagina = new VBox(12, migas, titulo, info, tarjetas);
        pagina.getStyleClass().add("pagina");
        pagina.setMinWidth(0);
        mostrarEnContenido(pagina);
    }

    private VBox tarjeta(String id, String texto, String icono, IntSupplier total) {
        int cantidad = total.getAsInt();
        Label numero = new Label(String.valueOf(cantidad));
        numero.getStyleClass().add("tarjeta-numero");
        Label nombre = new Label(texto);
        nombre.getStyleClass().add("tarjeta-texto");
        VBox datos = new VBox(2, numero, nombre);
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        Region ico = Iconos.crear(icono, 34);
        ico.getStyleClass().add("tarjeta-icono");
        HBox cabecera = new HBox(datos, espacio, ico);
        cabecera.setAlignment(Pos.CENTER_LEFT);
        cabecera.getStyleClass().add("tarjeta-cabecera");

        Button ir = new Button("Ver listado");
        ir.getStyleClass().add("tarjeta-enlace");
        ir.setMaxWidth(Double.MAX_VALUE);
        ir.setAccessibleText("Ver listado de " + texto.toLowerCase() + " (" + cantidad + ")");
        ir.setOnAction(e -> navegar(id));

        VBox tarjeta = new VBox(cabecera, ir);
        tarjeta.getStyleClass().add("tarjeta");
        tarjeta.setId("tarjeta-" + id);
        tarjeta.setPrefWidth(220);
        return tarjeta;
    }

    // ------------------------------------------------------------ menú adaptable

    public void alternarMenu() {
        switch (modo) {
            case COMPLETO, MINI -> {
                if (anchoEscena() >= ANCHO_TABLET) {
                    contraidoEnEscritorio = !contraidoEnEscritorio;
                } else {
                    expandidoEnTablet = !expandidoEnTablet;
                }
            }
            case OCULTO -> abiertoEnMovil = !abiertoEnMovil;
        }
        aplicarModo();
    }

    private double anchoEscena() {
        return bp.getScene() == null ? 1280 : bp.getScene().getWidth();
    }

    private void aplicarModo() {
        double ancho = anchoEscena();
        if (ancho < ANCHO_MOVIL) {
            modo = Modo.OCULTO;
        } else if (ancho < ANCHO_TABLET) {
            modo = expandidoEnTablet ? Modo.COMPLETO : Modo.MINI;
            abiertoEnMovil = false;
        } else {
            modo = contraidoEnEscritorio ? Modo.MINI : Modo.COMPLETO;
            abiertoEnMovil = false;
        }

        // Devolver el menú a su lugar si estaba superpuesto.
        if (capaMenuMovil != null) {
            capaModal.getChildren().remove(capaMenuMovil);
            capaMenuMovil.getChildren().remove(panelMenu);
            capaMenuMovil = null;
        }
        if (!cuerpo.getChildren().contains(panelMenu)) {
            cuerpo.getChildren().add(0, panelMenu);
        }

        boolean mini = modo == Modo.MINI;
        menuLateral.getStyleClass().remove("mini");
        if (mini) {
            menuLateral.getStyleClass().add("mini");
        }
        encabezados.forEach(h -> {
            h.setVisible(!mini);
            h.setManaged(!mini);
        });
        botones.values().forEach(b -> b.setContentDisplay(mini ? ContentDisplay.GRAPHIC_ONLY : ContentDisplay.LEFT));
        double anchoMenu = mini ? 56 : 230;
        panelMenu.setMinWidth(anchoMenu);
        panelMenu.setPrefWidth(anchoMenu);
        panelMenu.setMaxWidth(anchoMenu);

        if (modo == Modo.OCULTO) {
            cuerpo.getChildren().remove(panelMenu);
            if (abiertoEnMovil) {
                Region fondo = new Region();
                fondo.getStyleClass().add("modal-fondo");
                fondo.setOnMouseClicked(e -> {
                    abiertoEnMovil = false;
                    aplicarModo();
                });
                capaMenuMovil = new StackPane(fondo, panelMenu);
                capaMenuMovil.setId("menuMovil");
                StackPane.setAlignment(panelMenu, Pos.TOP_LEFT);
                capaModal.getChildren().add(capaMenuMovil);
            }
        }

        String texto = switch (modo) {
            case COMPLETO -> "Contraer menú";
            case MINI -> "Expandir menú";
            case OCULTO -> abiertoEnMovil ? "Cerrar menú" : "Abrir menú";
        };
        btnMenu.setAccessibleText(texto);
        btnMenu.setTooltip(new Tooltip(texto));
    }

    /** Estado visible del menú (COMPLETO, MINI, OCULTO u OCULTO_ABIERTO), para pruebas. */
    public String getModoMenu() {
        return modo.name() + (modo == Modo.OCULTO && abiertoEnMovil ? "_ABIERTO" : "");
    }
}
