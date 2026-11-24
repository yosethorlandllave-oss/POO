package pe.edu.upeu.sysventas.controller;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import pe.edu.upeu.sysventas.config.AppContext;
import pe.edu.upeu.sysventas.dto.MenuMenuItenTO;
import pe.edu.upeu.sysventas.dto.SessionManager;
import pe.edu.upeu.sysventas.model.Producto;
import pe.edu.upeu.sysventas.model.Promocion;
import pe.edu.upeu.sysventas.model.VentaDetalle;
import pe.edu.upeu.sysventas.service.IMenuMenuItemDao;
import pe.edu.upeu.sysventas.service.IProductoService;
import pe.edu.upeu.sysventas.service.IPromocionService;
import pe.edu.upeu.sysventas.service.IVentaDetalleService;
import pe.edu.upeu.sysventas.utils.UtilsX;

import java.io.IOException;
import java.net.URL;
import java.text.Normalizer;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ventana principal con estilo de tienda en línea (Coolbox):
 * cabecera con logo, botón Categorías, buscador y usuario; barra negra con el menú del perfil;
 * y la pestaña Inicio con el banner de promociones, tarjetas destacadas y los productos.
 */
@RequiredArgsConstructor
public class MainGuiController {

    // ---------- Servicios (los entrega AppContext) ----------
    private final IMenuMenuItemDao mmiDao;
    private final IProductoService ps;
    private final IPromocionService promoS;
    private final IVentaDetalleService vdS;

    // ---------- Controles del FXML ----------
    @FXML BorderPane bp;
    @FXML TabPane tabPane;
    @FXML Tab tabPrincipal;
    @FXML Label lblUsuario, lblPerfil, lblResultados;
    @FXML FlowPane panelAccesos, gridProductos;
    @FXML HBox barraCategorias, filaDestacados;
    @FXML TextField txtBuscar;
    @FXML Button btnCategorias, btnCarrito;
    @FXML MenuButton mbOfertas;
    @FXML StackPane hero;
    @FXML ScrollPane scrollInicio;
    @FXML VBox contenidoInicio, seccionProductos;

    MenuBar menuBar;
    private Parent parent;
    Stage stage;
    Preferences userPrefs = Preferences.userRoot();
    UtilsX util = new UtilsX();
    Properties myresources = new Properties();
    List<MenuMenuItenTO> lista = new ArrayList<>();

    // Datos de la pantalla de inicio
    private List<Producto> productos = new ArrayList<>();
    private List<Promocion> promos = new ArrayList<>();
    private boolean soloOfertas = false;
    private int slideActual = 0;
    private Timeline carrusel;
    private final Map<String, Image> imagenes = new HashMap<>();
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Pattern PRECIO = Pattern.compile("S/\\s*([0-9]+(?:[.,][0-9]+)?)");

    // Fondos del banner (uno por promoción, se repiten): degradado y si el texto va blanco u oscuro
    private static final String[][] FONDOS = {
            {"linear-gradient(to right, #5B2A86, #C2477D)", "hero-claro"},
            {"linear-gradient(to right, #EFE7DC, #D7C5AF)", "hero-oscuro"},
            {"linear-gradient(to right, #F4F5F7, #D9DDE3)", "hero-oscuro"},
            {"linear-gradient(to right, #1A1A1A, #3D3D3D)", "hero-claro"},
            {"linear-gradient(to right, #E30613, #8E0410)", "hero-claro"}
    };

    @FXML private Menu menuEstilo = new Menu("Cambiar Estilo");
    ComboBox<String> comboBoxEstilo = new ComboBox<>(
            FXCollections.observableArrayList(
                    "Estilo Tienda Coolbox", "Estilo Noche Estrellada", "Estilo Navideño", "Estilo por Defecto",
                    "Estilo Oscuro", "Estilo Azul", "Estilo Verde", "Estilo Rosado"));
    CustomMenuItem customItem = new CustomMenuItem(comboBoxEstilo);
    @FXML private Menu menuIdioma = new Menu("Idioma");
    ComboBox<String> comboBoxIdioma = new ComboBox<>(
            FXCollections.observableArrayList("Español", "Ingles"));
    CustomMenuItem customItemIdioma = new CustomMenuItem(comboBoxIdioma);

    @FXML
    public void initialize() {
        PauseTransition pause = new PauseTransition(Duration.millis(300));
        pause.setOnFinished(e -> {
            if (tabPane.getScene() != null && tabPane.getScene().getWindow() != null) {
                stage = (Stage) tabPane.getScene().getWindow();
            }
        });
        pause.play();

        graficarMenus();

        SessionManager ses = SessionManager.getInstance();
        lblUsuario.setText("Hola, " + ses.getUserName());
        lblPerfil.setText("Perfil: " + ses.getUserPerfil());

        // Buscador: filtra los productos de Inicio mientras se escribe
        txtBuscar.textProperty().addListener((obs, antes, ahora) -> {
            tabPane.getSelectionModel().select(tabPrincipal);
            mostrarProductos();
            if (ahora != null && !ahora.isBlank()) irA(seccionProductos);
        });
        // Al volver a Inicio se recargan los productos (por si cambió el stock con una venta o compra)
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, antes, ahora) -> {
            if (ahora == tabPrincipal) cargarProductos();
        });

        cargarInicio();
    }

    // =====================================================================
    //  PANTALLA DE INICIO
    // =====================================================================

    private void cargarInicio() {
        try {
            promos = promoS.listarVigentes();
        } catch (Exception ex) {
            System.out.println("No se pudieron cargar las promociones: " + ex.getMessage());
            promos = new ArrayList<>();
        }
        llenarMenuOfertas();
        slideActual = 0;
        construirHero();
        iniciarCarrusel();
        cargarProductos();
    }

    private void cargarProductos() {
        try {
            productos = new ArrayList<>(ps.finAll());
        } catch (Exception ex) {
            System.out.println("No se pudieron cargar los productos: " + ex.getMessage());
            productos = new ArrayList<>();
        }
        construirDestacados();
        mostrarProductos();
    }

    // ---------- Banner de promociones ----------

    private void construirHero() {
        hero.getChildren().clear();
        if (promos.isEmpty()) {
            Label vacio = new Label("Aún no hay promociones activas");
            vacio.getStyleClass().add("seccion-titulo");
            hero.getChildren().add(vacio);
            return;
        }
        Node slide = crearSlide(promos.get(slideActual), slideActual);

        Button izq = new Button();
        izq.setGraphic(icono("/img/ui-izq.png", 22));
        izq.getStyleClass().add("hero-flecha");
        izq.setOnAction(e -> { irASlide(slideActual - 1); iniciarCarrusel(); });
        StackPane.setAlignment(izq, Pos.CENTER_LEFT);
        StackPane.setMargin(izq, new Insets(0, 0, 0, 40));

        Button der = new Button();
        der.setGraphic(icono("/img/ui-der.png", 22));
        der.getStyleClass().add("hero-flecha");
        der.setOnAction(e -> { irASlide(slideActual + 1); iniciarCarrusel(); });
        StackPane.setAlignment(der, Pos.CENTER_RIGHT);
        StackPane.setMargin(der, new Insets(0, 40, 0, 0));

        HBox puntos = new HBox(6);
        puntos.getStyleClass().add("hero-puntos");
        puntos.setMaxWidth(Region.USE_PREF_SIZE);
        puntos.setMaxHeight(Region.USE_PREF_SIZE);
        puntos.setAlignment(Pos.CENTER);
        for (int i = 0; i < promos.size(); i++) {
            Region punto = new Region();
            punto.getStyleClass().add("hero-punto");
            if (i == slideActual) punto.getStyleClass().add("hero-punto-activo");
            final int indice = i;
            punto.setOnMouseClicked(e -> { irASlide(indice); iniciarCarrusel(); });
            puntos.getChildren().add(punto);
        }
        StackPane.setAlignment(puntos, Pos.BOTTOM_CENTER);
        StackPane.setMargin(puntos, new Insets(0, 0, 14, 0));

        hero.getChildren().addAll(slide, izq, der, puntos);

        FadeTransition ft = new FadeTransition(Duration.millis(400), slide);
        ft.setFromValue(0.35);
        ft.setToValue(1.0);
        ft.play();
    }

    private void irASlide(int indice) {
        if (promos.isEmpty()) return;
        slideActual = (indice % promos.size() + promos.size()) % promos.size();
        construirHero();
    }

    private void iniciarCarrusel() {
        if (carrusel != null) carrusel.stop();
        if (promos.size() < 2) return;
        carrusel = new Timeline(new KeyFrame(Duration.seconds(6), e -> irASlide(slideActual + 1)));
        carrusel.setCycleCount(Timeline.INDEFINITE);
        carrusel.play();
    }

    private Node crearSlide(Promocion p, int indice) {
        String[] fondo = FONDOS[indice % FONDOS.length];
        HBox slide = new HBox(60);
        slide.setAlignment(Pos.CENTER);
        slide.setPadding(new Insets(30, 130, 40, 130));
        slide.getStyleClass().add(fondo[1]);
        slide.setStyle("-fx-background-color: " + fondo[0] + ";");

        // Izquierda: etiqueta amarilla, título, detalle, botón y vigencia
        Label etiqueta = new Label(p.getEtiqueta());
        etiqueta.getStyleClass().add("hero-etiqueta");
        Label titulo = new Label(p.getNombre());
        titulo.getStyleClass().add("hero-titulo");
        titulo.setWrapText(true);
        titulo.setMaxWidth(600);
        Label detalle = new Label(p.getDetalle());
        detalle.getStyleClass().add("hero-detalle");
        detalle.setWrapText(true);
        detalle.setMaxWidth(600);
        Button verOfertas = new Button("¡VER OFERTAS!");
        verOfertas.getStyleClass().add("hero-btn");
        verOfertas.setGraphic(icono("/img/ui-der-blanco.png", 14));
        verOfertas.setContentDisplay(ContentDisplay.RIGHT);
        verOfertas.setOnAction(e -> verSoloOfertas());
        String vigencia = (p.getFechaInicio() != null && p.getFechaFin() != null)
                ? "*Promoción válida del " + p.getFechaInicio().format(FECHA) + " al " + p.getFechaFin().format(FECHA) + " o hasta agotar stock."
                : "*Promoción válida hasta agotar stock.";
        Label lblVigencia = new Label(vigencia);
        lblVigencia.getStyleClass().add("hero-vigencia");
        VBox izquierda = new VBox(12, etiqueta, titulo, detalle, verOfertas, lblVigencia);
        izquierda.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(izquierda, Priority.ALWAYS);

        // Derecha: descuento y precio
        VBox derecha = new VBox(6);
        derecha.setAlignment(Pos.CENTER_LEFT);
        double porcentaje = p.getPorcentaje() == null ? 0 : p.getPorcentaje();
        if (porcentaje > 0) {
            Label desc = new Label("-" + Math.round(porcentaje) + "%");
            desc.getStyleClass().add("badge-desc");
            derecha.getChildren().add(desc);
        }
        String[] precios = leerPrecios(p.getDetalle());
        if (precios[0] != null) {
            VBox caja = new VBox(2);
            caja.getStyleClass().add("precio-caja");
            caja.setAlignment(Pos.CENTER);
            if (p.getDetalle().contains("Desde")) {
                Label desde = new Label("Desde");
                desde.getStyleClass().add("precio-antes");
                caja.getChildren().add(desde);
            }
            Label precio = new Label("S/ " + precios[0]);
            precio.getStyleClass().add("precio-grande");
            caja.getChildren().add(precio);
            if (precios[1] != null) {
                Label antes = new Label("Antes: S/ " + precios[1]);
                antes.getStyleClass().add("precio-antes");
                caja.getChildren().add(antes);
            }
            derecha.getChildren().add(caja);
        } else if (porcentaje > 0) {
            VBox caja = new VBox(2);
            caja.getStyleClass().add("precio-caja");
            caja.setAlignment(Pos.CENTER);
            Label hasta = new Label("Hasta");
            hasta.getStyleClass().add("precio-antes");
            Label valor = new Label(Math.round(porcentaje) + "%");
            valor.getStyleClass().add("precio-grande");
            Label dscto = new Label("de descuento");
            dscto.getStyleClass().add("precio-antes");
            caja.getChildren().addAll(hasta, valor, dscto);
            derecha.getChildren().add(caja);
        }
        slide.getChildren().addAll(izquierda, derecha);
        return slide;
    }

    /** Lee el precio actual y el precio "Antes" del texto de la promoción. */
    private String[] leerPrecios(String texto) {
        String[] r = new String[2];
        if (texto == null) return r;
        int posAntes = texto.indexOf("Antes");
        Matcher m = PRECIO.matcher(texto);
        while (m.find()) {
            if (posAntes >= 0 && m.start() > posAntes) {
                if (r[1] == null) r[1] = m.group(1);
            } else if (posAntes >= 0) {
                r[0] = m.group(1);                // el último precio antes de "Antes"
            } else if (r[0] == null) {
                r[0] = m.group(1);                // sin "Antes": el primer precio
            }
        }
        return r;
    }

    private void llenarMenuOfertas() {
        mbOfertas.getItems().clear();
        for (int i = 0; i < promos.size(); i++) {
            final int indice = i;
            MenuItem item = new MenuItem(promos.get(i).getNombre());
            item.setOnAction(e -> {
                tabPane.getSelectionModel().select(tabPrincipal);
                scrollInicio.setVvalue(0);
                irASlide(indice);
                iniciarCarrusel();
            });
            mbOfertas.getItems().add(item);
        }
        if (promos.isEmpty()) {
            MenuItem vacio = new MenuItem("No hay promociones activas");
            vacio.setDisable(true);
            mbOfertas.getItems().add(vacio);
        }
    }

    // ---------- Lo último / Ofertas bombas / Lo más vendido ----------

    private void construirDestacados() {
        filaDestacados.getChildren().clear();
        if (productos.isEmpty()) return;

        Producto ultimo = productos.stream()
                .max(Comparator.comparingLong((Producto p) -> p.getIdProducto() == null ? 0L : p.getIdProducto()))
                .orElse(null);
        Producto oferta = productos.stream()
                .max(Comparator.comparingDouble((Producto p) -> descuento(p)))
                .orElse(null);
        Producto masVendido = buscarMasVendido();

        filaDestacados.getChildren().addAll(
                tarjetaDestacada("Lo último", ultimo, false),
                tarjetaDestacada("Ofertas bombas", oferta, true),
                tarjetaDestacada("Lo más vendido", masVendido, false));
    }

    /** Suma las cantidades vendidas de cada producto (tabla venta_detalle). */
    private Producto buscarMasVendido() {
        Map<Long, Double> vendidos = new HashMap<>();
        try {
            for (VentaDetalle vd : vdS.finAll()) {
                if (vd.getIdProducto() == null || vd.getIdProducto().getIdProducto() == null) continue;
                double cant = vd.getCantidad() == null ? 0 : vd.getCantidad();
                vendidos.merge(vd.getIdProducto().getIdProducto(), cant, Double::sum);
            }
        } catch (Exception ex) {
            System.out.println("No se pudo leer el detalle de ventas: " + ex.getMessage());
        }
        return productos.stream()
                .max(Comparator.comparingDouble((Producto p) -> vendidos.getOrDefault(p.getIdProducto(), 0.0))
                        .thenComparingDouble(p -> p.getStock() == null ? 0 : p.getStock()))
                .orElse(null);
    }

    private VBox tarjetaDestacada(String titulo, Producto p, boolean ofertas) {
        VBox card = new VBox(8);
        card.getStyleClass().add("tarjeta");
        card.setPrefWidth(300);
        card.setMinWidth(260);
        HBox.setHgrow(card, Priority.ALWAYS);

        Label lblTitulo = new Label(titulo);
        lblTitulo.getStyleClass().add("tarjeta-titulo");
        card.getChildren().add(lblTitulo);
        if (p == null) return card;

        StackPane img = new StackPane(icono(iconoCategoria(p), 90));
        img.getStyleClass().add("card-img");
        img.setPrefHeight(150);
        int desc = (int) Math.round(descuento(p));
        if (desc > 0) {
            Label badge = new Label("-" + desc + "%");
            badge.getStyleClass().add("badge-verde");
            StackPane.setAlignment(badge, Pos.TOP_RIGHT);
            StackPane.setMargin(badge, new Insets(8));
            img.getChildren().add(badge);
        }
        Label nombre = new Label(p.getNombre());
        nombre.getStyleClass().add("card-nombre");
        nombre.setStyle("-fx-text-fill: #8A8A8A;");
        HBox precios = new HBox(8);
        precios.setAlignment(Pos.BASELINE_LEFT);
        Label precio = new Label(soles(p.getPu()));
        precio.getStyleClass().add("precio");
        precios.getChildren().add(precio);
        if (desc > 0) {
            Label antes = new Label(soles(p.getPuold()));
            antes.getStyleClass().add("precio-tachado");
            precios.getChildren().add(antes);
        }
        Button verTodo = new Button("Ver todo");
        verTodo.getStyleClass().add("btn-ver-todo");
        verTodo.setMaxWidth(Double.MAX_VALUE);
        verTodo.setOnAction(e -> {
            if (ofertas) verSoloOfertas(); else verTodos();
        });
        card.getChildren().addAll(img, nombre, precios, verTodo);
        return card;
    }

    // ---------- Productos (tarjetas) ----------

    private void mostrarProductos() {
        String buscado = normalizar(txtBuscar.getText());
        List<Producto> filtrados = productos.stream()
                .filter(p -> buscado.isEmpty() || normalizar(textoBusqueda(p)).contains(buscado))
                .filter(p -> !soloOfertas || descuento(p) > 0)
                .sorted(Comparator.comparingDouble((Producto p) -> descuento(p)).reversed())
                .toList();
        List<Node> tarjetas = new ArrayList<>();
        for (Producto p : filtrados) tarjetas.add(tarjetaProducto(p));
        gridProductos.getChildren().setAll(tarjetas);
        String extra = soloOfertas ? " · solo ofertas" : "";
        if (!buscado.isEmpty()) extra += " · búsqueda: \"" + txtBuscar.getText().trim() + "\"";
        lblResultados.setText(filtrados.size() + " de " + productos.size() + " resultados" + extra);
    }

    private VBox tarjetaProducto(Producto p) {
        VBox card = new VBox(6);
        card.getStyleClass().add("card-producto");
        card.setPrefWidth(230);
        card.setMaxWidth(230);

        StackPane img = new StackPane(icono(iconoCategoria(p), 76));
        img.getStyleClass().add("card-img");
        img.setPrefHeight(130);
        int desc = (int) Math.round(descuento(p));
        if (desc > 0) {
            Label badge = new Label("-" + desc + "%");
            badge.getStyleClass().add("badge-verde");
            StackPane.setAlignment(badge, Pos.TOP_RIGHT);
            StackPane.setMargin(badge, new Insets(8));
            img.getChildren().add(badge);
        }

        String marca = (p.getIdMarca() != null && p.getIdMarca().getNombre() != null) ? p.getIdMarca().getNombre() : "";
        Label lblMarca = new Label(marca.toUpperCase());
        lblMarca.getStyleClass().add("card-marca");
        Label nombre = new Label(p.getNombre());
        nombre.getStyleClass().add("card-nombre");
        nombre.setWrapText(true);
        nombre.setMinHeight(36);

        VBox precios = new VBox(0);
        if (desc > 0) {
            Label antes = new Label(soles(p.getPuold()));
            antes.getStyleClass().add("precio-tachado");
            precios.getChildren().add(antes);
        }
        HBox fila = new HBox(8);
        fila.setAlignment(Pos.BASELINE_LEFT);
        Label precio = new Label(soles(p.getPu()));
        precio.getStyleClass().add("card-precio");
        fila.getChildren().add(precio);
        if (desc > 0) {
            Label dscto = new Label(desc + "% DSCTO");
            dscto.getStyleClass().add("card-dscto");
            fila.getChildren().add(dscto);
        }
        precios.getChildren().add(fila);

        double stock = p.getStock() == null ? 0 : p.getStock();
        Label lblStock = new Label(stock > 0 ? "Stock: " + formatoCantidad(stock) + " und." : "Sin stock");
        lblStock.getStyleClass().add("card-stock");
        if (stock < 10) lblStock.getStyleClass().add("card-stock-bajo");

        card.getChildren().addAll(img, lblMarca, nombre, precios, lblStock);
        Tooltip.install(card, new Tooltip(p.getNombre() + "\nClic para abrir Gestión de Productos"));
        card.setOnMouseClicked(e -> abrirAcceso("miproducto", "Tu perfil no tiene acceso a Productos."));
        return card;
    }

    // ---------- Accesos rápidos (según el perfil) ----------

    private void cargarAccesosRapidos() {
        if (panelAccesos == null) return;
        panelAccesos.getChildren().clear();
        for (MenuMenuItenTO m : lista) {
            if ("C".equals(m.getTipoTab())) continue;   // "Salir" no va como acceso rápido
            Button b = new Button(m.getMenuitemnombre());
            b.getStyleClass().add("acceso-rapido");
            b.setTooltip(new Tooltip("Abrir " + m.getNombreTab()));
            String nombreIcono = m.getIdNombreObj().replaceFirst("^mi", "");   // producto, cliente, venta...
            URL urlNormal = getClass().getResource("/img/ic-" + nombreIcono + "-oscuro.png");
            URL urlHover = getClass().getResource("/img/ic-" + nombreIcono + "-rojo.png");
            if (urlNormal != null && urlHover != null) {
                Image normal = new Image(urlNormal.toExternalForm());
                Image encimaImg = new Image(urlHover.toExternalForm());
                ImageView iv = new ImageView(normal);
                iv.setFitWidth(30);
                iv.setFitHeight(30);
                b.setGraphic(iv);
                b.setContentDisplay(ContentDisplay.TOP);
                b.hoverProperty().addListener((obs, antes, encima) -> iv.setImage(encima ? encimaImg : normal));
            }
            b.setOnAction(e -> abrirTab(m.getRutaFile(), m.getNombreTab()));
            panelAccesos.getChildren().add(b);
        }
    }

    // =====================================================================
    //  ACCIONES DE LA CABECERA
    // =====================================================================

    /** Botón rojo "Categorías": muestra todas las pantallas del perfil. */
    @FXML
    public void mostrarCategorias(ActionEvent e) {
        ContextMenu menu = new ContextMenu();
        for (MenuMenuItenTO m : lista) {
            if ("C".equals(m.getTipoTab())) continue;
            MenuItem item = new MenuItem(m.getNombreTab());
            String nombreIcono = m.getIdNombreObj().replaceFirst("^mi", "");
            item.setGraphic(icono("/img/ic-" + nombreIcono + "-oscuro.png", 18));
            item.setOnAction(ev -> abrirTab(m.getRutaFile(), m.getNombreTab()));
            menu.getItems().add(item);
        }
        menu.getItems().add(new SeparatorMenuItem());
        MenuItem salir = new MenuItem("Cerrar sesión");
        salir.setGraphic(icono("/img/ui-salir.png", 18));
        salir.setOnAction(ev -> cerrarSesion());
        menu.getItems().add(salir);
        menu.show(btnCategorias, Side.BOTTOM, 0, 6);
    }

    /** Icono del carrito: abre Registrar Venta. */
    @FXML
    public void irAVenta() {
        abrirAcceso("miventa", "Tu perfil no tiene acceso a Ventas.");
    }

    @FXML
    public void cerrarSesion() {
        for (MenuMenuItenTO m : lista) {
            if ("C".equals(m.getTipoTab())) {
                redireccionar(m.getRutaFile());
                return;
            }
        }
        redireccionar("/view/login.fxml");
    }

    @FXML
    public void filtrarApple() {
        soloOfertas = false;
        txtBuscar.setText("Apple");
    }

    @FXML
    public void filtrarStarlink() {
        soloOfertas = false;
        txtBuscar.setText("Starlink");
    }

    @FXML
    public void verSoloOfertas() {
        soloOfertas = true;
        tabPane.getSelectionModel().select(tabPrincipal);
        mostrarProductos();
        irA(seccionProductos);
    }

    @FXML
    public void verTodos() {
        soloOfertas = false;
        txtBuscar.clear();
        tabPane.getSelectionModel().select(tabPrincipal);
        mostrarProductos();
        irA(seccionProductos);
    }

    @FXML
    public void verTiendas() {
        aviso("Nuestra tienda",
                "Coolbox Juliaca\nJr. San Román 123 - Juliaca, Puno\n"
                        + "Lunes a sábado de 9:00 am a 8:00 pm\nTeléfono: 951 782 520");
    }

    @FXML
    public void verAyuda() {
        aviso("¿Cómo usar Coolbox?",
                "1. Usa el botón rojo Categorías o la barra negra para abrir cada pantalla.\n"
                        + "2. Escribe en el buscador para encontrar productos por nombre, marca o categoría.\n"
                        + "3. Para vender: Venta > Reg. Venta (o el ícono del carrito).\n"
                        + "4. En Ofertas verás las promociones vigentes del banner.\n"
                        + "5. Cada pantalla se abre en su pestaña y se cierra con la X.");
    }

    // =====================================================================
    //  APOYO
    // =====================================================================

    private void abrirAcceso(String idNombreObj, String mensajeSinAcceso) {
        for (MenuMenuItenTO m : lista) {
            if (idNombreObj.equals(m.getIdNombreObj())) {
                abrirTab(m.getRutaFile(), m.getNombreTab());
                return;
            }
        }
        aviso("Sin acceso", mensajeSinAcceso);
    }

    private void aviso(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Coolbox");
        alert.setHeaderText(titulo);
        alert.setContentText(mensaje);
        if (bp.getScene() != null && bp.getScene().getWindow() != null) {
            alert.initOwner(bp.getScene().getWindow());
        }
        alert.showAndWait();
    }

    /** Desplaza la pantalla de inicio hasta la sección indicada. */
    private void irA(Node seccion) {
        Platform.runLater(() -> {
            double alto = contenidoInicio.getBoundsInLocal().getHeight();
            double visible = scrollInicio.getViewportBounds().getHeight();
            if (alto > visible) {
                double y = seccion.getBoundsInParent().getMinY();
                scrollInicio.setVvalue(Math.min(1.0, Math.max(0.0, y / (alto - visible))));
            }
        });
    }

    private ImageView icono(String ruta, double tam) {
        ImageView iv = new ImageView();
        Image img = imagenes.computeIfAbsent(ruta, r -> {
            URL url = getClass().getResource(r);
            return url == null ? null : new Image(url.toExternalForm());
        });
        if (img != null) iv.setImage(img);
        iv.setFitWidth(tam);
        iv.setFitHeight(tam);
        iv.setPreserveRatio(true);
        return iv;
    }

    /** Ícono según la categoría del producto. */
    private String iconoCategoria(Producto p) {
        String cat = (p.getIdCategoria() != null && p.getIdCategoria().getNombre() != null)
                ? normalizar(p.getIdCategoria().getNombre()) : "";
        String archivo = "caja";
        if (cat.contains("tele")) archivo = "tv";
        else if (cat.contains("audio")) archivo = "audio";
        else if (cat.contains("celu")) archivo = "celular";
        else if (cat.contains("comput")) archivo = "computo";
        else if (cat.contains("consol")) archivo = "consola";
        else if (cat.contains("conect")) archivo = "conectividad";
        else if (cat.contains("smartw") || cat.contains("reloj")) archivo = "smartwatch";
        else if (cat.contains("tablet")) archivo = "tablet";
        else if (cat.contains("electro")) archivo = "electro";
        else if (cat.contains("acces")) archivo = "accesorio";
        return "/img/cat-" + archivo + ".png";
    }

    /** Porcentaje de descuento: (precio anterior - precio actual) / precio anterior. */
    private double descuento(Producto p) {
        if (p.getPu() == null || p.getPuold() == null || p.getPuold() <= 0 || p.getPuold() <= p.getPu()) return 0;
        return (p.getPuold() - p.getPu()) * 100.0 / p.getPuold();
    }

    private String textoBusqueda(Producto p) {
        StringBuilder sb = new StringBuilder(p.getNombre() == null ? "" : p.getNombre());
        if (p.getIdMarca() != null && p.getIdMarca().getNombre() != null) sb.append(' ').append(p.getIdMarca().getNombre());
        if (p.getIdCategoria() != null && p.getIdCategoria().getNombre() != null) sb.append(' ').append(p.getIdCategoria().getNombre());
        return sb.toString();
    }

    private static String normalizar(String texto) {
        if (texto == null) return "";
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase().trim();
    }

    private static String soles(Double valor) {
        return String.format(Locale.US, "S/ %,.2f", valor == null ? 0.0 : valor);
    }

    private static String formatoCantidad(double cantidad) {
        return cantidad == Math.floor(cantidad) ? String.valueOf((long) cantidad) : String.valueOf(cantidad);
    }

    // =====================================================================
    //  PESTAÑAS, MENÚ, ESTILOS E IDIOMA
    // =====================================================================

    // Abre una pantalla en su pestaña. Si ya está abierta, solo la muestra.
    private void abrirTab(String fxmlPath, String tittle) {
        for (Tab t : tabPane.getTabs()) {
            if (tittle.equals(t.getText())) {
                tabPane.getSelectionModel().select(t);
                return;
            }
        }
        try {
            AppContext context = AppContext.getInstance();
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(fxmlPath));
            fxmlLoader.setControllerFactory(context::getBean);
            Parent root = fxmlLoader.load();

            ScrollPane scrollPane = new ScrollPane(root);
            scrollPane.setFitToWidth(true);
            scrollPane.setFitToHeight(true);
            Tab newTab = new Tab(tittle, scrollPane);
            tabPane.getTabs().add(newTab);
            tabPane.getSelectionModel().select(newTab);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    // Vuelve al login (Cerrar sesión / Salir)
    private void redireccionar(String fxmlPath) {
        if (carrusel != null) carrusel.stop();
        if (stage == null && bp.getScene() != null) stage = (Stage) bp.getScene().getWindow();
        tabPane.getTabs().removeIf(t -> t != tabPrincipal);
        try {
            AppContext ctx = AppContext.getInstance();
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(fxmlPath));
            fxmlLoader.setControllerFactory(ctx::getBean);
            parent = fxmlLoader.load();
            Scene scene = new Scene(parent);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/css/estilo-tienda.css").toExternalForm());
            stage.setScene(scene);
            stage.sizeToScene();
            stage.centerOnScreen();
            stage.setTitle("Coolbox - Iniciar sesión");
            stage.setResizable(false);
            stage.show();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private List<MenuMenuItenTO> listaAccesos() {
        myresources = util.detectLanguage(userPrefs.get("IDIOMAX", "es"));
        return mmiDao.listaAccesos(SessionManager.getInstance().getUserPerfil(), myresources);
    }

    private void cambiarEstilo() {
        String estiloSeleccionado = comboBoxEstilo.getSelectionModel().getSelectedItem();
        if (estiloSeleccionado == null) return;
        Scene escena = bp.getScene();
        escena.getStylesheets().clear();
        switch (estiloSeleccionado) {
            case "Estilo Tienda Coolbox":
                agregarCss(escena, "/css/style.css");
                agregarCss(escena, "/css/estilo-tienda.css"); break;
            case "Estilo Noche Estrellada":
                agregarCss(escena, "/css/style.css");
                agregarCss(escena, "/css/estilo-coolbox.css"); break;
            case "Estilo Navideño":
                agregarCss(escena, "/css/style.css");
                agregarCss(escena, "/css/estilo-navidad.css"); break;
            case "Estilo Oscuro":
                agregarCss(escena, "/css/estilo-oscuro.css"); break;
            case "Estilo Azul":
                agregarCss(escena, "/css/estilo-azul.css"); break;
            case "Estilo Verde":
                agregarCss(escena, "/css/estilo-verde.css"); break;
            case "Estilo Rosado":
                agregarCss(escena, "/css/estilo-rosado.css"); break;
            default: break;
        }
    }

    // Agrega una hoja de estilos solo si el archivo existe (así nunca se cae el programa)
    private void agregarCss(Scene escena, String ruta) {
        URL url = getClass().getResource(ruta);
        if (url != null) escena.getStylesheets().add(url.toExternalForm());
    }

    private void cambiarIdioma() {
        String idiomaSeleccionado = comboBoxIdioma.getSelectionModel().getSelectedItem();
        if (idiomaSeleccionado == null) return;
        switch (idiomaSeleccionado) {
            case "Español": userPrefs.put("IDIOMAX", "es"); break;
            case "Ingles": userPrefs.put("IDIOMAX", "en"); break;
            default: userPrefs.put("IDIOMAX", "es"); break;
        }
        graficarMenus();
    }

    private void graficarMenus() {
        lista = listaAccesos();
        int[] mmi = mmiDao.contarMenuMunuItem(lista);
        Menu[] menu = new Menu[mmi[0]];
        MenuItem[] menuItem = new MenuItem[mmi[1]];
        menuBar = new MenuBar();
        MenuItemListener d = new MenuItemListener();
        String menuN = "";
        int menui = 0, menuitem = 0;
        char conti = 'N';
        for (MenuMenuItenTO mmix : lista) {
            if (!mmix.getMenunombre().equals(menuN)) {
                menu[menui] = new Menu(mmix.getMenunombre());
                menu[menui].setId("m" + mmix.getIdNombreObj());
                if (!mmix.getMenuitemnombre().equals("")) {
                    menuItem[menuitem] = new MenuItem(mmix.getMenuitemnombre());
                    menuItem[menuitem].setId("mi" + mmix.getIdNombreObj());
                    menuItem[menuitem].setOnAction(d::handle);
                    menu[menui].getItems().add(menuItem[menuitem]);
                    menuitem++;
                }
                menuBar.getMenus().add(menu[menui]);
                menuN = mmix.getMenunombre();
                conti = 'N';
                menui++;
            } else {
                conti = 'S';
            }
            if (!mmix.getMenuitemnombre().equals("") && mmix.getMenunombre().equals(menuN) && conti == 'S') {
                menuItem[menuitem] = new MenuItem(mmix.getMenuitemnombre());
                menuItem[menuitem].setId("mi" + mmix.getIdNombreObj());
                menuItem[menuitem].setOnAction(d::handle);
                menu[menui - 1].getItems().add(menuItem[menuitem]);
                menuitem++;
            }
        }
        comboBoxEstilo.setOnAction(e -> cambiarEstilo());
        customItem.setHideOnClick(false);
        menuEstilo.getItems().clear();
        menuEstilo.getItems().add(customItem);
        menuBar.getMenus().addAll(menuEstilo);
        comboBoxIdioma.setOnAction(e -> cambiarIdioma());
        customItemIdioma.setHideOnClick(false);
        menuIdioma.getItems().clear();
        menuIdioma.getItems().add(customItemIdioma);
        menuBar.getMenus().addAll(menuIdioma);

        // El menú va dentro de la barra negra de categorías (a la derecha de "¡Lo mejor en tecnología!")
        barraCategorias.getChildren().removeIf(n -> n instanceof MenuBar);
        HBox.setHgrow(menuBar, Priority.ALWAYS);
        barraCategorias.getChildren().add(menuBar);
        cargarAccesosRapidos();
    }

    class MenuItemListener {
        Map<String, String[]> menuConfig;

        MenuItemListener() {
            menuConfig = mmiDao.accesosAutorizados(lista);
        }

        public void handle(ActionEvent e) {
            String id = ((MenuItem) e.getSource()).getId();
            if (menuConfig.containsKey(id)) {
                String[] items = menuConfig.get(id);
                if (items[2].equals("C")) {
                    redireccionar(items[0]);
                } else {
                    abrirTab(items[0], items[1]);
                }
            }
        }
    }
}
