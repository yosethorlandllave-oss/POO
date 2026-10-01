package pe.edu.upeu.sysventas.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pe.edu.upeu.sysventas.enums.TipoProducto;
import pe.edu.upeu.sysventas.exception.ModelNotFoundException;
import pe.edu.upeu.sysventas.exception.ReglaNegocioException;
import pe.edu.upeu.sysventas.exception.ValidacionException;
import pe.edu.upeu.sysventas.model.Categoria;
import pe.edu.upeu.sysventas.model.Marca;
import pe.edu.upeu.sysventas.model.Producto;
import pe.edu.upeu.sysventas.model.UnidMedida;
import pe.edu.upeu.sysventas.repository.CategoriaRepository;
import pe.edu.upeu.sysventas.repository.MarcaRepository;
import pe.edu.upeu.sysventas.repository.ProductoRepository;
import pe.edu.upeu.sysventas.repository.UnidadMedidaRepository;
import pe.edu.upeu.sysventas.service.impl.CategoriaServiceImp;
import pe.edu.upeu.sysventas.service.impl.MarcaServiceImp;
import pe.edu.upeu.sysventas.service.impl.ProductoServiceImp;
import pe.edu.upeu.sysventas.service.impl.UnidadMedidaServiceImp;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Reglas de la capa de servicio con repositorios nuevos en cada prueba
 * (sin AppContext ni datos compartidos).
 */
class ServiciosCrudTest {

    private ProductoRepository productoRepo;
    private IMarcaService marcas;
    private ICategoriaService categorias;
    private IUnidadMedidaService unidades;
    private IProductoService productos;

    private Marca samsung;
    private Categoria artefactos;
    private UnidMedida unidad;

    @BeforeEach
    void preparar() {
        MarcaRepository marcaRepo = new MarcaRepository();
        CategoriaRepository categoriaRepo = new CategoriaRepository();
        UnidadMedidaRepository unidadRepo = new UnidadMedidaRepository();
        productoRepo = new ProductoRepository();
        marcas = new MarcaServiceImp(marcaRepo, productoRepo);
        categorias = new CategoriaServiceImp(categoriaRepo, productoRepo);
        unidades = new UnidadMedidaServiceImp(unidadRepo, productoRepo);
        productos = new ProductoServiceImp(productoRepo, marcaRepo, categoriaRepo, unidadRepo);

        samsung = marcas.save(Marca.builder().nombre("Samsung").build());
        artefactos = categorias.save(Categoria.builder().nombre("Artefactos").build());
        unidad = unidades.save(UnidMedida.builder().nombreMedida("Unidad").build());
    }

    private Producto producto(String nombre, Long idMarca, double precio) {
        return Producto.builder()
                .nombre(nombre)
                .tipoProducto(TipoProducto.PRODUCTO)
                .pu(precio).puold(0.0).utilidad(10.0).stock(5.0).stockold(0.0)
                // Referencias solo con id, como las arma el formulario.
                .idMarca(Marca.builder().idMarca(idMarca).build())
                .idCategoria(Categoria.builder().idCategoria(artefactos.getIdCategoria()).build())
                .idUnidad(UnidMedida.builder().idUnidad(unidad.getIdUnidad()).build())
                .build();
    }

    // ------------------------------------------------------------ catálogos

    @Test
    void crearMarcaValidaPersisteYSeConsulta() {
        Marca lg = marcas.save(Marca.builder().nombre("  LG  ").build());

        assertNotNull(lg.getIdMarca());
        assertEquals("LG", marcas.findById(lg.getIdMarca()).getNombre(), "el nombre se guarda sin espacios");
        assertTrue(marcas.findAll().stream().anyMatch(m -> m.getNombre().equals("LG")));
        assertTrue(marcas.listarCombobox().stream().anyMatch(o -> o.getValue().equals("LG")));
    }

    @Test
    void marcaSinNombreSeRechaza() {
        ValidacionException e = assertThrows(ValidacionException.class,
                () -> marcas.save(Marca.builder().nombre("   ").build()));
        assertEquals("El nombre de la marca es obligatorio", e.getErrores().get("nombre"));
        assertEquals(1, marcas.findAll().size());
    }

    @Test
    void marcaDuplicadaSeRechazaSinImportarMayusculas() {
        ReglaNegocioException e = assertThrows(ReglaNegocioException.class,
                () -> marcas.save(Marca.builder().nombre(" SAMSUNG ").build()));
        assertEquals("nombre", e.getCampo());
        assertEquals(1, marcas.findAll().size());
    }

    @Test
    void editarMarcaConservandoSuNombreEstaPermitidoPeroNoTomarElDeOtra() {
        Marca lg = marcas.save(Marca.builder().nombre("LG").build());

        marcas.update(lg.getIdMarca(), Marca.builder().nombre("lg").build());
        assertEquals("lg", marcas.findById(lg.getIdMarca()).getNombre());

        assertThrows(ReglaNegocioException.class,
                () -> marcas.update(lg.getIdMarca(), Marca.builder().nombre("Samsung").build()));
    }

    @Test
    void actualizarOEliminarUnIdInexistenteLanzaModelNotFound() {
        assertThrows(ModelNotFoundException.class, () -> marcas.update(999L, Marca.builder().nombre("X").build()));
        assertThrows(ModelNotFoundException.class, () -> marcas.delete(999L));
        assertThrows(ModelNotFoundException.class, () -> categorias.findById(999L));
    }

    @Test
    void noSeEliminaUnaMarcaEnUsoPeroSiCuandoQuedaLibre() {
        Producto tv = productos.save(producto("Televisor", samsung.getIdMarca(), 1500));

        ReglaNegocioException e = assertThrows(ReglaNegocioException.class,
                () -> marcas.delete(samsung.getIdMarca()));
        assertTrue(e.getMessage().contains("la usa 1 producto"), e.getMessage());
        assertEquals(1, marcas.contarProductos(samsung.getIdMarca()));
        assertDoesNotThrow(() -> marcas.findById(samsung.getIdMarca()));

        productos.delete(tv.getIdProducto());
        marcas.delete(samsung.getIdMarca());
        assertTrue(marcas.findAll().isEmpty());
    }

    @Test
    void categoriaYUnidadEnUsoTampocoSeEliminan() {
        productos.save(producto("Televisor", samsung.getIdMarca(), 1500));

        assertThrows(ReglaNegocioException.class, () -> categorias.delete(artefactos.getIdCategoria()));
        assertThrows(ReglaNegocioException.class, () -> unidades.delete(unidad.getIdUnidad()));
        assertEquals(1, categorias.findAll().size());
        assertEquals(1, unidades.findAll().size());
    }

    @Test
    void renombrarUnCatalogoSeReflejaEnLosProductos() {
        Producto tv = productos.save(producto("Televisor", samsung.getIdMarca(), 1500));

        marcas.update(samsung.getIdMarca(), Marca.builder().nombre("Samsung Electronics").build());
        categorias.update(artefactos.getIdCategoria(), Categoria.builder().nombre("Electrodomésticos").build());
        unidades.update(unidad.getIdUnidad(), UnidMedida.builder().nombreMedida("Pieza").build());

        Producto leido = productos.findById(tv.getIdProducto());
        assertEquals("Samsung Electronics", leido.getIdMarca().getNombre());
        assertEquals("Electrodomésticos", leido.getIdCategoria().getNombre());
        assertEquals("Pieza", leido.getIdUnidad().getNombreMedida());
    }

    @Test
    void listarComboboxYaNoVuelveASembrarDatosDeEjemplo() {
        marcas.delete(samsung.getIdMarca());
        assertTrue(marcas.listarCombobox().isEmpty());
        assertTrue(marcas.findAll().isEmpty());
    }

    // ------------------------------------------------------------ productos

    @Test
    void crearProductoResuelveLasRelacionesContraLosRegistrosVigentes() {
        Producto tv = productos.save(producto("  Televisor  ", samsung.getIdMarca(), 1500));

        Producto leido = productos.findById(tv.getIdProducto());
        assertEquals("Televisor", leido.getNombre());
        assertSame(samsung, leido.getIdMarca(), "debe apuntar al registro real, no a una copia con solo el id");
        assertEquals("Artefactos", leido.getIdCategoria().getNombre());
        assertEquals("Unidad", leido.getIdUnidad().getNombreMedida());
    }

    @Test
    void productoConMarcaInexistenteSeRechaza() {
        ReglaNegocioException e = assertThrows(ReglaNegocioException.class,
                () -> productos.save(producto("Radio", 404L, 100)));
        assertEquals("idMarca", e.getCampo());
        assertTrue(productos.findAll().isEmpty());
    }

    @Test
    void productoInvalidoInformaCadaCampo() {
        Producto p = producto("", samsung.getIdMarca(), 0);
        p.setUtilidad(0.0);
        p.setStock(-1.0);
        p.setTipoProducto(null);
        p.setIdCategoria(null);

        ValidacionException e = assertThrows(ValidacionException.class, () -> productos.save(p));
        assertEquals("El nombre del producto es obligatorio", e.getErrores().get("nombre"));
        assertEquals("El precio unitario debe ser mayor que cero", e.getErrores().get("pu"));
        assertEquals("La utilidad debe ser mayor que cero", e.getErrores().get("utilidad"));
        assertEquals("El stock no puede ser negativo", e.getErrores().get("stock"));
        assertEquals("El tipo de producto es obligatorio", e.getErrores().get("tipoProducto"));
        assertEquals("La categoría del producto es obligatoria", e.getErrores().get("idCategoria"));
        assertTrue(productos.findAll().isEmpty());
    }

    @Test
    void productoDuplicadoEnLaMismaMarcaSeRechazaPeroEnOtraMarcaNo() {
        productos.save(producto("Televisor", samsung.getIdMarca(), 1500));
        Marca lg = marcas.save(Marca.builder().nombre("LG").build());

        ReglaNegocioException e = assertThrows(ReglaNegocioException.class,
                () -> productos.save(producto("televisor", samsung.getIdMarca(), 1200)));
        assertEquals("nombre", e.getCampo());

        assertDoesNotThrow(() -> productos.save(producto("Televisor", lg.getIdMarca(), 1200)));
        assertEquals(2, productos.findAll().size());
    }

    @Test
    void editarProductoGuardaLosCambios() {
        Producto tv = productos.save(producto("Televisor", samsung.getIdMarca(), 1500));

        Producto cambios = producto("Televisor 50\"", samsung.getIdMarca(), 1399.9);
        productos.update(tv.getIdProducto(), cambios);

        Producto leido = productos.findById(tv.getIdProducto());
        assertEquals("Televisor 50\"", leido.getNombre());
        assertEquals(1399.9, leido.getPu());
        assertEquals(1, productos.findAll().size());
    }

    @Test
    void updateUsaElIdIndicadoYNoSobrescribeOtroRegistro() {
        Producto a = productos.save(producto("A", samsung.getIdMarca(), 10));
        Producto b = productos.save(producto("B", samsung.getIdMarca(), 20));

        Producto cambios = producto("B editado", samsung.getIdMarca(), 25);
        cambios.setIdProducto(a.getIdProducto()); // id equivocado dentro de la entidad
        productos.update(b.getIdProducto(), cambios);

        assertEquals("A", productos.findById(a.getIdProducto()).getNombre());
        assertEquals("B editado", productos.findById(b.getIdProducto()).getNombre());
    }

    @Test
    void eliminarElUltimoProductoNoLoHaceReaparecer() {
        Producto tv = productos.save(producto("Televisor", samsung.getIdMarca(), 1500));
        productos.delete(tv.getIdProducto());

        assertTrue(productos.findAll().isEmpty());
        assertTrue(productos.findAll().isEmpty(), "una segunda consulta tampoco debe re-sembrar datos");
    }

    @Test
    void tiposDeProductoDelCombo() {
        assertEquals(3, productos.listarTipoProducto().size());
        assertEquals("Preparado", productos.listarTipoProducto().get(1).getValue());
    }
}
