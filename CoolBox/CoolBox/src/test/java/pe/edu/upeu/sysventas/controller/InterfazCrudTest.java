package pe.edu.upeu.sysventas.controller;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import pe.edu.upeu.sysventas.components.TablaPaginada;
import pe.edu.upeu.sysventas.components.ToltipCustom;
import pe.edu.upeu.sysventas.config.AppContext;
import pe.edu.upeu.sysventas.dto.ComboBoxOption;
import pe.edu.upeu.sysventas.enums.TipoProducto;
import pe.edu.upeu.sysventas.model.Categoria;
import pe.edu.upeu.sysventas.model.Marca;
import pe.edu.upeu.sysventas.model.Producto;
import pe.edu.upeu.sysventas.model.UnidMedida;
import pe.edu.upeu.sysventas.service.ICategoriaService;
import pe.edu.upeu.sysventas.service.IMarcaService;
import pe.edu.upeu.sysventas.service.IProductoService;
import pe.edu.upeu.sysventas.service.IUnidadMedidaService;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Recorrido real de la interfaz: abre la ventana principal con su CSS, pulsa
 * los botones (fire), escribe en los campos y comprueba pantalla y datos.
 * Guarda capturas en target/capturas. Los datos viven solo en esta JVM.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class InterfazCrudTest {

    private static final File CAPTURAS = new File("target/capturas");

    private Stage stage;
    private Scene scene;
    private static Scene escena;
    private MainGuiController main;
    private IProductoService ps;
    private IMarcaService ms;
    private ICategoriaService cs;
    private IUnidadMedidaService us;

    @BeforeAll
    void abrirVentana() throws Exception {
        CompletableFuture<Void> iniciado = new CompletableFuture<>();
        Platform.startup(() -> iniciado.complete(null));
        iniciado.get(20, TimeUnit.SECONDS);
        Platform.setImplicitExit(false);
        CAPTURAS.mkdirs();

        AppContext ctx = AppContext.getInstance();
        ps = ctx.getBean(IProductoService.class);
        ms = ctx.getBean(IMarcaService.class);
        cs = ctx.getBean(ICategoriaService.class);
        us = ctx.getBean(IUnidadMedidaService.class);

        fx(() -> {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/maingui.fxml"));
            loader.setControllerFactory(ctx::getBean);
            Parent root = loader.load();
            main = loader.getController();
            scene = new Scene(root, 1280, 800);
            escena = scene;
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            stage = new Stage();
            stage.setTitle("CoolBox (prueba)");
            stage.setScene(scene);
            stage.show();
            return null;
        });
        esperarAncho(1280);
    }

    @AfterAll
    void cerrar() throws Exception {
        fx(() -> {
            stage.close();
            return null;
        });
    }

    // =========================================================== pruebas

    @Test
    @Order(1)
    void inicioMuestraLosRecuentosDeLosDatosDeEjemplo() throws Exception {
        assertEquals("inicio", main.getOpcionActiva());
        assertEquals(List.of("1", "5", "2", "2"), List.of(
                numeroTarjeta("productos"), numeroTarjeta("marcas"),
                numeroTarjeta("categorias"), numeroTarjeta("unidades")));
        assertEquals("COMPLETO", main.getModoMenu());
        captura("01-inicio-escritorio");
    }

    @Test
    @Order(2)
    void listadoDeProductosMuestraElEjemploConSusRelaciones() throws Exception {
        clic("#menu-productos");
        ProductoController pc = productos();
        assertEquals("productos", main.getOpcionActiva());
        TableView<Producto> t = tabla(pc);
        assertEquals(1, fx(() -> t.getItems().size()));
        // Antes el ejemplo apuntaba a una marca "fantasma" y se veía N/A.
        assertEquals("Samsung", fx(() -> textoCelda(t, "Marca", 0)));
        assertEquals("Artefactos", fx(() -> textoCelda(t, "Categoría", 0)));
        assertEquals("Mostrando 1 a 1 de 1 registros", fx(() -> pc.tabla.getContador().getText()));
        // A 1280 px todas las columnas caben: acciones al final y sin desplazamiento horizontal.
        assertEquals("Acciones", fx(() -> t.getColumns().get(t.getColumns().size() - 1).getText()));
        assertFalse(fx(() -> barraHorizontalVisible(t)), "no debe haber desplazamiento horizontal a 1280 px");
        assertEquals("Agregar", fx(() -> {
            Button agregar = pc.tabla.getBotonAgregar();
            assertTrue(agregar.getWidth() >= agregar.prefWidth(-1) - 1, "el botón Agregar no debe recortarse");
            return agregar.getText();
        }));
        captura("02-productos-listado");
    }

    @Test
    @Order(3)
    void crearProductoValidoLoGuardaYLoMuestra() throws Exception {
        ProductoController pc = productos();
        clic("#btnAgregar");
        assertTrue(fx(() -> pc.panelFormulario.isVisible()));
        assertEquals("Nuevo producto", fx(() -> pc.lblTituloFormulario.getText()));

        fx(() -> {
            elegir(pc.cbxTipoProducto, "Producto");
            pc.txtNombreProducto.setText("Lavadora 18 kg");
            pc.txtPUnit.setText("1899.90");
            pc.txtUtilidad.setText("250");
            pc.txtStock.setText("7");
            elegir(pc.cbxCategoria, "Artefactos");
            elegir(pc.cbxMarca, "LG");
            elegir(pc.cbxUnidMedida, "Unidad");
            return null;
        });
        captura("03-formulario-nuevo");
        clicBoton(pc.btnGuardar);

        assertTrue(fx(() -> pc.panelListado.isVisible()), "vuelve al listado");
        assertTrue(fx(() -> pc.aviso.getTexto()).contains("«Lavadora 18 kg» fue registrado correctamente"));
        Producto guardado = buscarProducto("Lavadora 18 kg");
        assertEquals(1899.90, guardado.getPu());
        assertEquals(0.0, guardado.getPuold(), "precio anterior vacío = 0, como en la versión original");
        assertEquals("LG", guardado.getIdMarca().getNombre());
        assertNotNull(fx(() -> fila(tabla(pc), p -> p.getNombre().equals("Lavadora 18 kg"))));
        captura("04-producto-creado");
    }

    @Test
    @Order(4)
    void datosInvalidosSeMarcanYNoSeGuardan() throws Exception {
        ProductoController pc = productos();
        int antes = ps.findAll().size();
        clic("#btnAgregar");
        fx(() -> {
            pc.txtPUnit.setText("abc");
            pc.txtUtilidad.setText("0");
            pc.txtStock.setText("-3");
            return null;
        });
        clicBoton(pc.btnGuardar);

        assertTrue(fx(() -> pc.panelFormulario.isVisible()), "se queda en el formulario");
        assertEquals("Ingresa un número válido, por ejemplo 12.50", fx(() -> ToltipCustom.errorDe(pc.txtPUnit)));
        assertEquals("El nombre del producto es obligatorio", fx(() -> ToltipCustom.errorDe(pc.txtNombreProducto)));
        assertEquals("La utilidad debe ser mayor que cero", fx(() -> ToltipCustom.errorDe(pc.txtUtilidad)));
        assertEquals("El stock no puede ser negativo", fx(() -> ToltipCustom.errorDe(pc.txtStock)));
        assertEquals("La marca del producto es obligatoria", fx(() -> ToltipCustom.errorDe(pc.cbxMarca)));
        assertTrue(fx(() -> pc.txtPUnit.getStyleClass().contains("campo-error")));
        assertTrue(fx(() -> pc.aviso.getTexto()).contains("campos marcados"));
        assertEquals(antes, ps.findAll().size());
        captura("05-formulario-validacion");

        // Cancelar vuelve al listado sin guardar y limpia los errores.
        fx(() -> {
            pc.cancelarFormulario();
            return null;
        });
        assertTrue(fx(() -> pc.panelListado.isVisible()));
        assertNull(fx(() -> ToltipCustom.errorDe(pc.txtPUnit)));
        assertEquals(antes, ps.findAll().size());
    }

    @Test
    @Order(5)
    void productoDuplicadoSeRechazaEnElFormulario() throws Exception {
        ProductoController pc = productos();
        int antes = ps.findAll().size();
        clic("#btnAgregar");
        fx(() -> {
            elegir(pc.cbxTipoProducto, "Producto");
            pc.txtNombreProducto.setText("  lavadora 18 KG ");
            pc.txtPUnit.setText("100");
            pc.txtUtilidad.setText("10");
            pc.txtStock.setText("1");
            elegir(pc.cbxCategoria, "Artefactos");
            elegir(pc.cbxMarca, "LG");
            elegir(pc.cbxUnidMedida, "Unidad");
            return null;
        });
        clicBoton(pc.btnGuardar);
        assertTrue(fx(() -> ToltipCustom.errorDe(pc.txtNombreProducto)).startsWith("Ya existe el producto"));
        assertEquals(antes, ps.findAll().size());
        fx(() -> {
            pc.cancelarFormulario();
            return null;
        });
    }

    @Test
    @Order(6)
    void buscarYFiltrarSobreElListado() throws Exception {
        ProductoController pc = productos();
        TableView<Producto> t = tabla(pc);

        escribirBusqueda(pc, "lavad");
        assertEquals(1, fx(() -> t.getItems().size()));
        assertEquals("Mostrando 1 a 1 de 1 registros (filtrado de 2 registros en total)",
                fx(() -> pc.tabla.getContador().getText()));
        captura("06-busqueda");

        escribirBusqueda(pc, "samsung");
        assertEquals("Televisor", fx(() -> t.getItems().get(0).getNombre()), "busca también por marca");

        escribirBusqueda(pc, "no-existe");
        assertEquals(0, fx(() -> t.getItems().size()));
        assertEquals("Mostrando 0 registros", fx(() -> pc.tabla.getContador().getText()));

        escribirBusqueda(pc, "");
        ComboBox<ComboBoxOption> filtroTipo = (ComboBox<ComboBoxOption>) buscar("#cbxFiltroTipo");
        fx(() -> {
            elegir(filtroTipo, "Servicio");
            return null;
        });
        assertEquals(0, fx(() -> t.getItems().size()));
        fx(() -> {
            elegir(filtroTipo, "Todos los tipos");
            return null;
        });
        assertEquals(2, fx(() -> t.getItems().size()));
    }

    @Test
    @Order(7)
    void editarCargaElFormularioYGuardaLosCambios() throws Exception {
        ProductoController pc = productos();
        Producto lavadora = buscarProducto("Lavadora 18 kg");
        clicAccion(tabla(pc), p -> p.getIdProducto().equals(lavadora.getIdProducto()), ".btn-editar");

        assertEquals("Editar producto #" + lavadora.getIdProducto(), fx(() -> pc.lblTituloFormulario.getText()));
        assertEquals("Lavadora 18 kg", fx(() -> pc.txtNombreProducto.getText()));
        assertEquals("1899.9", fx(() -> pc.txtPUnit.getText()));
        assertEquals("LG", fx(() -> pc.cbxMarca.getValue().getValue()));
        assertEquals("Producto", fx(() -> pc.cbxTipoProducto.getValue().getValue()));
        captura("07-formulario-editar");

        fx(() -> {
            pc.txtPUnit.setText("1750,5"); // también acepta coma decimal
            elegir(pc.cbxTipoProducto, "Preparado");
            return null;
        });
        clicBoton(pc.btnGuardar);

        Producto leido = ps.findById(lavadora.getIdProducto());
        assertEquals(1750.5, leido.getPu());
        assertEquals(TipoProducto.PREPARADO, leido.getTipoProducto());
        assertEquals(2, ps.findAll().size(), "editar no crea registros");
        assertTrue(fx(() -> pc.aviso.getTexto()).contains("fue actualizado correctamente"));
        int fila = fx(() -> indiceFila(tabla(pc), p -> p.getIdProducto().equals(lavadora.getIdProducto())));
        assertEquals("S/ 1,750.50", fx(() -> textoCelda(tabla(pc), "P. unitario", fila)));
    }

    @Test
    @Order(8)
    void verMuestraElDetalleCompleto() throws Exception {
        ProductoController pc = productos();
        Producto lavadora = buscarProducto("Lavadora 18 kg");
        clicAccion(tabla(pc), p -> p.getIdProducto().equals(lavadora.getIdProducto()), ".btn-ver");

        assertTrue(fx(() -> pc.panelDetalle.isVisible()));
        List<String> textos = fx(() -> pc.gridDetalle.getChildren().stream()
                .map(n -> ((Label) n).getText()).toList());
        assertTrue(textos.containsAll(List.of("Lavadora 18 kg", "LG", "Artefactos", "Unidad", "S/ 1,750.50",
                "Preparado", "250.00", "7.00")), textos.toString());
        captura("08-detalle");

        Button volver = fx(() -> (Button) pc.accionesDetalle.getChildren().get(2));
        assertEquals("Volver al listado", fx(volver::getText));
        clicBoton(volver);
        assertTrue(fx(() -> pc.panelListado.isVisible()));
    }

    @Test
    @Order(9)
    void eliminarPideConfirmacionYRespetaLaCancelacion() throws Exception {
        ProductoController pc = productos();
        Producto lavadora = buscarProducto("Lavadora 18 kg");
        Predicate<Producto> esLavadora = p -> p.getIdProducto().equals(lavadora.getIdProducto());

        clicAccion(tabla(pc), esLavadora, ".btn-eliminar");
        assertNotNull(buscar("#modalConfirmacion"), "debe aparecer la confirmación");
        captura("09-confirmar-eliminar");
        Button cancelar = fx(() -> (Button) scene.lookup("#modalConfirmacion").lookupAll(".btn-default")
                .iterator().next());
        clicBoton(cancelar);
        assertNull(buscar("#modalConfirmacion"));
        assertEquals(2, ps.findAll().size(), "cancelar no elimina");

        clicAccion(tabla(pc), esLavadora, ".btn-eliminar");
        clic("#btnConfirmarModal");
        assertEquals(1, ps.findAll().size());
        assertTrue(ps.findAll().stream().noneMatch(esLavadora));
        assertTrue(fx(() -> pc.aviso.getTexto()).contains("fue eliminado correctamente"));
        assertNull(fx(() -> fila(tabla(pc), esLavadora)));
    }

    @Test
    @Order(10)
    void paginacionYOrdenTrabajanSobreTodosLosRegistros() throws Exception {
        Marca samsung = ms.findById(1L);
        for (int i = 1; i <= 14; i++) {
            ps.save(Producto.builder().nombre(String.format("Artículo %02d", i)).tipoProducto(TipoProducto.PRODUCTO)
                    .pu(i * 10.0).puold(0.0).utilidad(1.0).stock(1.0).stockold(0.0)
                    .idMarca(Marca.builder().idMarca(samsung.getIdMarca()).build())
                    .idCategoria(Categoria.builder().idCategoria(1L).build())
                    .idUnidad(UnidMedida.builder().idUnidad(1L).build()).build());
        }
        ProductoController pc = productos();
        fx(() -> {
            pc.recargarListado();
            return null;
        });
        TablaPaginada<Producto> tp = pc.tabla;
        assertEquals("Mostrando 1 a 10 de 15 registros", fx(() -> tp.getContador().getText()));
        assertEquals(2, fx(tp::getTotalPaginas));

        Button siguiente = fx(() -> (Button) tp.lookupAll(".boton-pagina").stream()
                .filter(n -> "Siguiente".equals(((Button) n).getText())).findFirst().orElseThrow());
        clicBoton(siguiente);
        assertEquals("Mostrando 11 a 15 de 15 registros", fx(() -> tp.getContador().getText()));
        assertEquals(2, fx(tp::getPaginaActual));
        captura("10-paginacion");

        fx(() -> {
            tp.getSelectorCantidad().setValue(5);
            TableColumn<Producto, ?> precio = tp.getTabla().getColumns().stream()
                    .filter(c -> c.getText().equals("P. unitario")).findFirst().orElseThrow();
            precio.setSortType(TableColumn.SortType.DESCENDING);
            tp.getTabla().getSortOrder().setAll(precio);
            return null;
        });
        assertEquals("Mostrando 1 a 5 de 15 registros", fx(() -> tp.getContador().getText()));
        // El mayor precio (140) está en la última "página" original: ordenar debe traerlo.
        assertEquals(140.0, fx(() -> tp.getTabla().getItems().get(0).getPu()));
        assertEquals(3, fx(tp::getTotalPaginas));

        fx(() -> {
            tp.getSelectorCantidad().setValue(25);
            return null;
        });
        assertEquals("Mostrando 1 a 15 de 15 registros", fx(() -> tp.getContador().getText()));
    }

    @Test
    @Order(11)
    void marcasCrudCompletoEIntegridadConProductos() throws Exception {
        clic("#menu-marcas");
        MarcaController mc = (MarcaController) main.getControladorActivo();
        assertEquals("Marcas", fx(() -> mc.lblTitulo.getText()));
        assertEquals(5, fx(() -> mc.tabla.getTabla().getItems().size()));
        assertTrue(fx(() -> {
            escena.getRoot().applyCss();
            escena.getRoot().layout();
            Button agregar = mc.tabla.getBotonAgregar();
            return agregar.getWidth() >= agregar.prefWidth(-1) - 1;
        }), "el botón Agregar no debe recortarse");

        // Crear
        clic("#btnAgregar");
        fx(() -> {
            mc.txtNombre.setText("Panasonic");
            return null;
        });
        clicBoton(mc.btnGuardar);
        assertTrue(ms.findAll().stream().anyMatch(m -> m.getNombre().equals("Panasonic")));
        assertTrue(fx(() -> mc.aviso.getTexto()).contains("La marca «Panasonic» fue registrada correctamente"));

        // Duplicado y vacío
        clic("#btnAgregar");
        fx(() -> {
            mc.txtNombre.setText(" samsung ");
            return null;
        });
        clicBoton(mc.btnGuardar);
        assertEquals("Ya existe una marca con el nombre «samsung».", fx(() -> ToltipCustom.errorDe(mc.txtNombre)));
        fx(() -> {
            mc.txtNombre.setText("");
            return null;
        });
        clicBoton(mc.btnGuardar);
        assertEquals("El nombre de la marca es obligatorio", fx(() -> ToltipCustom.errorDe(mc.txtNombre)));
        captura("11-marca-validacion");
        fx(() -> {
            mc.cancelarFormulario();
            return null;
        });
        assertEquals(6, ms.findAll().size());

        // Editar Samsung y comprobar el efecto en productos
        clicAccion(mc.tabla.getTabla(), m -> m.getIdMarca() == 1L, ".btn-editar");
        assertEquals("Samsung", fx(() -> mc.txtNombre.getText()));
        fx(() -> {
            mc.txtNombre.setText("Samsung Electronics");
            return null;
        });
        clicBoton(mc.btnGuardar);
        assertEquals("Samsung Electronics", ms.findById(1L).getNombre());
        captura("12-marcas-listado");

        // Eliminar una marca en uso: se conserva y se explica el motivo
        clicAccion(mc.tabla.getTabla(), m -> m.getIdMarca() == 1L, ".btn-eliminar");
        clic("#btnConfirmarModal");
        String error = fx(() -> mc.aviso.getTexto());
        assertTrue(error.startsWith("No se puede eliminar la marca «Samsung Electronics» porque la usan 15 productos"),
                error);
        assertDoesNotThrow(() -> ms.findById(1L));
        captura("13-marca-en-uso");

        // Eliminar una marca libre
        Long idPanasonic = ms.findAll().stream().filter(m -> m.getNombre().equals("Panasonic"))
                .findFirst().orElseThrow().getIdMarca();
        clicAccion(mc.tabla.getTabla(), m -> m.getIdMarca().equals(idPanasonic), ".btn-eliminar");
        clic("#btnConfirmarModal");
        assertTrue(ms.findAll().stream().noneMatch(m -> m.getNombre().equals("Panasonic")));

        // El producto muestra el nombre nuevo de su marca
        clic("#menu-productos");
        ProductoController pc = productos();
        escribirBusqueda(pc, "televisor");
        assertEquals("Samsung Electronics", fx(() -> textoCelda(tabla(pc), "Marca", 0)));
        escribirBusqueda(pc, "");
    }

    @Test
    @Order(12)
    void categoriasYUnidadesCrudCompleto() throws Exception {
        clic("#menu-categorias");
        CategoriaController cc = (CategoriaController) main.getControladorActivo();
        clic("#btnAgregar");
        fx(() -> {
            cc.txtNombre.setText("Ropa deportiva");
            return null;
        });
        clicBoton(cc.btnGuardar);
        Categoria ropa = cs.findAll().stream().filter(c -> c.getNombre().equals("Ropa deportiva")).findFirst()
                .orElseThrow();
        clicAccion(cc.tabla.getTabla(), c -> c.getIdCategoria().equals(ropa.getIdCategoria()), ".btn-editar");
        fx(() -> {
            cc.txtNombre.setText("Ropa y calzado deportivo");
            return null;
        });
        clicBoton(cc.btnGuardar);
        assertEquals("Ropa y calzado deportivo", cs.findById(ropa.getIdCategoria()).getNombre());

        clicAccion(cc.tabla.getTabla(), c -> c.getIdCategoria() == 1L, ".btn-eliminar");
        clic("#btnConfirmarModal");
        assertTrue(fx(() -> cc.aviso.getTexto()).contains("No se puede eliminar la categoría «Artefactos»"));

        clicAccion(cc.tabla.getTabla(), c -> c.getIdCategoria().equals(ropa.getIdCategoria()), ".btn-eliminar");
        clic("#btnConfirmarModal");
        assertThrows(RuntimeException.class, () -> cs.findById(ropa.getIdCategoria()));
        captura("14-categorias");

        clic("#menu-unidades");
        UnidadMedidaController uc = (UnidadMedidaController) main.getControladorActivo();
        assertEquals("Unidades de medida", fx(() -> uc.lblTitulo.getText()));
        clic("#btnAgregar");
        fx(() -> {
            uc.txtNombre.setText("Caja");
            return null;
        });
        clicBoton(uc.btnGuardar);
        UnidMedida caja = us.findAll().stream().filter(u -> u.getNombreMedida().equals("Caja")).findFirst()
                .orElseThrow();
        clicAccion(uc.tabla.getTabla(), u -> u.getIdUnidad().equals(caja.getIdUnidad()), ".btn-ver");
        List<String> textos = fx(() -> uc.gridDetalle.getChildren().stream().map(n -> ((Label) n).getText()).toList());
        assertTrue(textos.containsAll(List.of("Caja", "Productos que la usan", "0")), textos.toString());
        captura("15-unidad-detalle");
        clicBoton(fx(() -> (Button) uc.accionesDetalle.getChildren().get(1))); // Eliminar desde el detalle
        clic("#btnConfirmarModal");
        assertTrue(us.findAll().stream().noneMatch(u -> u.getNombreMedida().equals("Caja")));
    }

    @Test
    @Order(13)
    void registroInexistenteSeInformaYRecargaElListado() throws Exception {
        clic("#menu-marcas");
        MarcaController mc = (MarcaController) main.getControladorActivo();
        Marca sony = ms.findAll().stream().filter(m -> m.getNombre().equals("Sony")).findFirst().orElseThrow();
        clicAccion(mc.tabla.getTabla(), m -> m.getIdMarca().equals(sony.getIdMarca()), ".btn-editar");
        ms.delete(sony.getIdMarca()); // otro camino lo elimina mientras se edita
        fx(() -> {
            mc.txtNombre.setText("Sony Corp");
            return null;
        });
        clicBoton(mc.btnGuardar);
        assertTrue(fx(() -> mc.panelListado.isVisible()));
        assertEquals("La marca que estabas editando ya no existe. Se recargó el listado.",
                fx(() -> mc.aviso.getTexto()));
        assertTrue(ms.findAll().stream().noneMatch(m -> m.getNombre().startsWith("Sony")));
    }

    @Test
    @Order(14)
    void disenoAdaptableEnTabletYMovil() throws Exception {
        clic("#menu-productos");
        try {
            redimensionar(820);
            assertEquals("MINI", main.getModoMenu(), "en tablet el menú queda solo con iconos");
            assertTrue(paginaSinDesbordar(), "la página no debe desbordarse en tablet");
            captura("16-tablet-productos");

            redimensionar(420);
            assertEquals("OCULTO", main.getModoMenu());
            captura("17-movil-listado");
            TableView<Producto> t = tabla(productos());
            assertTrue(fx(() -> barraHorizontalVisible(t)), "la tabla debe permitir desplazamiento horizontal");
            assertEquals("Acciones", fx(() -> t.getColumns().get(0).getText()),
                    "si las columnas no caben, las acciones pasan al inicio para seguir a la vista");
            assertTrue(paginaSinDesbordar(), "la página no debe desbordarse horizontalmente en móvil");

            clic("#btnMenu");
            assertEquals("OCULTO_ABIERTO", main.getModoMenu());
            assertNotNull(buscar("#menuMovil"));
            captura("18-movil-menu-abierto");
            clic("#menu-marcas");
            assertEquals("OCULTO", main.getModoMenu(), "al elegir una opción el menú se cierra");
            assertNull(buscar("#menuMovil"));
            assertTrue(paginaSinDesbordar(), "el catálogo tampoco debe desbordarse en móvil");

            clic("#btnMenu"); // en móvil las opciones solo existen con el menú abierto
            clic("#menu-productos");
            clic("#btnAgregar");
            assertTrue(paginaSinDesbordar(), "el formulario debe ocupar una sola columna en móvil");
            captura("19-movil-formulario");
            fx(() -> {
                productos().cancelarFormulario();
                return null;
            });
        } finally {
            redimensionar(1280);
        }
        assertEquals("COMPLETO", main.getModoMenu());
        clic("#btnMenu");
        assertEquals("MINI", main.getModoMenu(), "en escritorio el botón contrae el menú");
        TableView<Producto> tEscritorio = tabla(productos());
        assertFalse(fx(() -> barraHorizontalVisible(tEscritorio)),
                "al volver a escritorio las columnas caben de nuevo sin desplazamiento");
        assertEquals("Acciones", fx(() -> tEscritorio.getColumns().get(tEscritorio.getColumns().size() - 1).getText()));
        captura("20-escritorio-menu-contraido");
        clic("#btnMenu");
        assertEquals("COMPLETO", main.getModoMenu());
    }

    @Test
    @Order(15)
    void botonesConIconoTienenTextoAccesible() throws Exception {
        clic("#menu-productos");
        List<Node> acciones = fx(() -> List.copyOf(scene.getRoot().lookupAll(".btn-accion")));
        assertFalse(acciones.isEmpty());
        for (Node n : acciones) {
            if (fx(n::isVisible) && ((Button) n).getParent().getParent() != null) {
                String texto = fx(n::getAccessibleText);
                assertNotNull(texto);
                assertTrue(texto.matches("(Ver|Editar|Eliminar) .+"), texto);
            }
        }
        assertEquals("Contraer menú", fx(() -> buscar("#btnMenu").getAccessibleText()));
        assertEquals("Buscar en el listado", fx(() -> buscar("#txtBuscar").getAccessibleText()));
    }

    @Test
    @Order(16)
    void salirPideConfirmacion() throws Exception {
        clic("#menu-salir");
        assertNotNull(buscar("#modalConfirmacion"));
        captura("21-confirmar-salida");
        Button cancelar = fx(() -> (Button) scene.lookup("#modalConfirmacion").lookupAll(".btn-default")
                .iterator().next());
        clicBoton(cancelar);
        assertNull(buscar("#modalConfirmacion"));
        assertTrue(fx(() -> stage.isShowing()), "cancelar no cierra la aplicación");
    }

    // =========================================================== utilidades

    private ProductoController productos() {
        return (ProductoController) main.getControladorActivo();
    }

    @SuppressWarnings("unchecked")
    private static <T> TableView<T> tabla(CrudController<T> c) {
        return c.tabla.getTabla();
    }

    private Producto buscarProducto(String nombre) {
        return ps.findAll().stream().filter(p -> p.getNombre().equals(nombre)).findFirst().orElseThrow();
    }

    private String numeroTarjeta(String id) throws Exception {
        return fx(() -> ((Label) scene.lookup("#tarjeta-" + id).lookup(".tarjeta-numero")).getText());
    }

    private static <T> String textoCelda(TableView<T> t, String columna, int fila) {
        escena.getRoot().applyCss();
        escena.getRoot().layout();
        TableRow<?> row = (TableRow<?>) t.lookupAll(".table-row-cell").stream()
                .filter(n -> ((TableRow<?>) n).getIndex() == fila && !((TableRow<?>) n).isEmpty())
                .findFirst().orElseThrow();
        int col = t.getColumns().indexOf(t.getColumns().stream().filter(c -> c.getText().equals(columna))
                .findFirst().orElseThrow());
        return row.lookupAll(".table-cell").stream()
                .map(n -> (javafx.scene.control.TableCell<?, ?>) n)
                .filter(c -> c.getTableColumn() == t.getColumns().get(col))
                .findFirst().orElseThrow().getText();
    }

    /** El contenido de la página cabe en el ancho visible (sin desplazamiento horizontal de la página). */
    private boolean paginaSinDesbordar() throws Exception {
        return fx(() -> {
            scene.getRoot().applyCss();
            scene.getRoot().layout();
            return scene.getRoot().lookupAll(".scroll-contenido").stream()
                    .map(n -> (javafx.scene.control.ScrollPane) n)
                    .allMatch(sp -> sp.getContent().getLayoutBounds().getWidth()
                            <= sp.getViewportBounds().getWidth() + 1);
        });
    }

    private static boolean barraHorizontalVisible(TableView<?> t) {
        escena.getRoot().applyCss();
        escena.getRoot().layout();
        return t.lookupAll(".scroll-bar").stream()
                .anyMatch(n -> ((ScrollBar) n).getOrientation() == Orientation.HORIZONTAL && n.isVisible());
    }

    private static <T> int indiceFila(TableView<T> t, Predicate<T> criterio) {
        for (int i = 0; i < t.getItems().size(); i++) {
            if (criterio.test(t.getItems().get(i))) {
                return i;
            }
        }
        return -1;
    }

    @SuppressWarnings("unchecked")
    private static <T> TableRow<T> fila(TableView<T> t, Predicate<T> criterio) {
        escena.getRoot().applyCss();
        escena.getRoot().layout();
        return t.lookupAll(".table-row-cell").stream()
                .map(n -> (TableRow<T>) n)
                .filter(r -> r.getItem() != null && !r.isEmpty() && criterio.test(r.getItem()))
                .findFirst().orElse(null);
    }

    /** Pulsa el botón de acción (ver/editar/eliminar) de la fila indicada. */
    private <T> void clicAccion(TableView<T> t, Predicate<T> criterio, String claseBoton) throws Exception {
        Button b = fx(() -> {
            TableRow<T> r = fila(t, criterio);
            assertNotNull(r, "fila no encontrada en la página visible");
            return (Button) r.lookup(claseBoton);
        });
        clicBoton(b);
    }

    private void escribirBusqueda(CrudController<?> c, String texto) throws Exception {
        fx(() -> {
            c.tabla.getCampoBusqueda().setText(texto);
            return null;
        });
    }

    private static void elegir(ComboBox<ComboBoxOption> combo, String texto) {
        combo.setValue(combo.getItems().stream().filter(o -> Objects.equals(o.getValue(), texto))
                .findFirst().orElseThrow(() -> new AssertionError("Opción no encontrada: " + texto)));
    }

    private Node buscar(String selector) throws Exception {
        return fx(() -> {
            scene.getRoot().applyCss();
            scene.getRoot().layout();
            return scene.lookup(selector);
        });
    }

    private void clic(String selector) throws Exception {
        Node n = buscar(selector);
        assertNotNull(n, "no existe " + selector);
        clicBoton((Button) n);
    }

    private void clicBoton(Button b) throws Exception {
        fx(() -> {
            assertTrue(b.isVisible() && !b.isDisabled(), "botón no utilizable: " + b.getText());
            b.fire();
            return null;
        });
        fx(() -> null); // deja correr lo que el clic haya programado con runLater
    }

    private void redimensionar(double ancho) throws Exception {
        fx(() -> {
            stage.setWidth(ancho);
            return null;
        });
        esperarAncho(ancho);
    }

    private void esperarAncho(double ancho) throws Exception {
        long limite = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < limite) {
            double actual = fx(() -> scene.getWidth());
            if (Math.abs(actual - ancho) < 40) {
                fx(() -> null);
                return;
            }
            Thread.sleep(50);
        }
        fail("La ventana no alcanzó el ancho " + ancho + " (actual " + fx(() -> scene.getWidth()) + ")");
    }

    private void captura(String nombre) throws Exception {
        WritableImage img = fx(() -> {
            scene.getRoot().applyCss();
            scene.getRoot().layout();
            return scene.snapshot(null);
        });
        int w = (int) img.getWidth();
        int h = (int) img.getHeight();
        BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        PixelReader pr = img.getPixelReader();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                bi.setRGB(x, y, pr.getArgb(x, y));
            }
        }
        ImageIO.write(bi, "png", new File(CAPTURAS, nombre + ".png"));
    }

    private static <T> T fx(Callable<T> accion) throws Exception {
        if (Platform.isFxApplicationThread()) {
            return accion.call();
        }
        CompletableFuture<T> futuro = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                futuro.complete(accion.call());
            } catch (Throwable e) {
                futuro.completeExceptionally(e);
            }
        });
        try {
            return futuro.get(20, TimeUnit.SECONDS);
        } catch (java.util.concurrent.ExecutionException e) {
            if (e.getCause() instanceof Error err) {
                throw err;
            }
            throw (Exception) e.getCause();
        }
    }
}
