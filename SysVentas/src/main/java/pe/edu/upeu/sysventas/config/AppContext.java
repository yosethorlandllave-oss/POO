package pe.edu.upeu.sysventas.config;

import pe.edu.upeu.sysventas.controller.*;
import pe.edu.upeu.sysventas.repository.*;
import pe.edu.upeu.sysventas.service.*;
import pe.edu.upeu.sysventas.service.impl.*;
import pe.edu.upeu.sysventas.utils.ConsultaDNI;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

public class AppContext {

    // Singleton: una sola instancia en toda la app
    private static AppContext instance;

    public static synchronized AppContext getInstance() {
        if (instance == null) instance = new AppContext();
        return instance;
    }

    // El "directorio": Clase → Objeto
    private final Map<Class<?>, Object> contenedor = new HashMap<>();

    // Constructor privado: aquí se arma toda la aplicación
    private AppContext() {
        registrarRepositorios();
        registrarServicios();
        registrarControladores();
    }

    // CAPA 1 — REPOSITORIOS
    // Cada repositorio sabe hablar con una tabla de la base de datos.
    // No reciben dependencias: solo necesitan la conexión (DatabaseConfig).
    private void registrarRepositorios() {
        //registrar(CategoriaRepository.class, new CategoriaRepository());


        registrar(CategoriaRepository.class, new CategoriaRepository());
        registrar(MarcaRepository.class, new MarcaRepository());
        registrar(UnidadMedidaRepository.class, new UnidadMedidaRepository());
        registrar(ProductoRepository.class, new ProductoRepository());
        registrar(ClienteRepository.class, new ClienteRepository());
        registrar(ProveedorRepository.class, new ProveedorRepository());

        registrar(UsuarioRepository.class, new UsuarioRepository());

        registrar(VentCarritoRepository.class, new VentCarritoRepository());
        registrar(VentaDetalleRepository.class, new VentaDetalleRepository());
        registrar(VentaRepository.class, new VentaRepository());

        // Compras (Yoseth)
        registrar(CompCarritoRepository.class, new CompCarritoRepository());
        registrar(CompraRepository.class, new CompraRepository());
        registrar(CompraDetalleRepository.class, new CompraDetalleRepository());

        // Promociones (banner de la pantalla de inicio)
        registrar(PromocionRepository.class, new PromocionRepository());
        registrar(ReporteRepository.class, new ReporteRepository());

    }

    // CAPA 2 — SERVICIOS
    // Cada servicio recibe su repositorio por constructor.
    // Usamos getBean() para buscarlo en el directorio: no creamos nada nuevo.
    private void registrarServicios() {

        registrar(ICategoriasService.class, new CategoriaServiceImp(getBean(CategoriaRepository.class)));
        registrar(IProveedorService.class, new ProveedorServiceImp(getBean(ProveedorRepository.class)));
        registrar(IMarcaService.class, new MarcaServiceImp(getBean(MarcaRepository.class)));
        registrar(IProductoService.class, new ProductoServiceImp(getBean(ProductoRepository.class)));
        registrar(IUnidadMedidaService.class, new UnidadMedidaServiceImp(getBean(UnidadMedidaRepository.class)));
        registrar(IClienteService.class, new ClienteServiceImp(getBean(ClienteRepository.class)));

        registrar(IUsuarioService.class, new UsuarioServiceImp(getBean(UsuarioRepository.class)));
        registrar(IMenuMenuItemDao.class, new MenuMenuItemDaoImp());

        registrar(ConsultaDNI.class, new ConsultaDNI());
        DataSource ds = DatabaseConfig.getDataSource();
        registrar(IVentaCarritoService.class, new VentaCarritoServiceImp(getBean(VentCarritoRepository.class)));
        registrar(IVentaDetalleService.class, new VentaDetalleServiceImp(getBean(VentaDetalleRepository.class)));
        registrar(IVentaService.class, new VentaServiceImp(getBean(VentaRepository.class), ds));

        registrar(ICompraCarritoService.class, new CompraCarritoServiceImp(getBean(CompCarritoRepository.class)));
        registrar(ICompraService.class, new CompraServiceImp(getBean(CompraRepository.class)));
        registrar(ICompraDetalleService.class, new CompraDetalleServiceImp(getBean(CompraDetalleRepository.class)));

        registrar(IPromocionService.class, new PromocionServiceImp(getBean(PromocionRepository.class)));
        registrar(IReporteService.class, new ReporteServiceImp(getBean(ReporteRepository.class)));

    }

    // CAPA 3 — CONTROLADORES JavaFX
    // Cada controlador recibe los servicios que necesita por constructor.
    // El FXMLLoader los busca aquí a través de setControllerFactory().
    private void registrarControladores() {
        //registrar(LoginController.class, new LoginController(getBean(IUsuarioService.class)));
        registrar(LoginController.class, new LoginController(getBean(IUsuarioService.class)));
        registrar(MainGuiController.class, new MainGuiController(
                getBean(IMenuMenuItemDao.class),
                getBean(IProductoService.class),
                getBean(IPromocionService.class),
                getBean(IVentaDetalleService.class)));

        registrar(ProductoController.class,
                new ProductoController(
                        getBean(IMarcaService.class),
                        getBean(ICategoriasService.class),
                        getBean(IProductoService.class),
                        getBean(IUnidadMedidaService.class)));

        registrar(ClienteController.class,
                new ClienteController(getBean(IClienteService.class)));
        registrar(ProveedorController.class,
                new ProveedorController(getBean(IProveedorService.class)));

        registrar(VentaController.class,
                new VentaController(
                        getBean(IProductoService.class),
                        getBean(IClienteService.class),
                        getBean(IVentaCarritoService.class),
                        getBean(IUsuarioService.class),
                        getBean(IVentaService.class),
                        getBean(IVentaDetalleService.class),
                        getBean(ConsultaDNI.class)));

        registrar(CompraController.class,
                new CompraController(
                        getBean(IProveedorService.class),
                        getBean(IProductoService.class),
                        getBean(ICompraCarritoService.class),
                        getBean(IUsuarioService.class),
                        getBean(ICompraService.class),
                        getBean(ICompraDetalleService.class)));
        registrar(ReporteController.class, new ReporteController(getBean(IReporteService.class)));

    }

    // API del contenedor — estos dos métodos son todo lo que hace la DI
    /** Guarda un objeto en el directorio, indexado por su tipo o interfaz. */
    private void registrar(Class<?> tipo, Object bean) {
        contenedor.put(tipo, bean);
    }

    /**
     * Busca y devuelve un objeto por su tipo o interfaz.
     * Equivale a lo que hace Spring/Micronaut con @Inject automáticamente.
     */
    @SuppressWarnings("unchecked")
    public <T> T getBean(Class<T> tipo) {
        Object bean = contenedor.get(tipo);
        if (bean == null) {
            // Búsqueda por compatibilidad: sirve cuando se pide una interfaz
            // y el objeto guardado es su implementación concreta.
            bean = contenedor.values().stream()
                    .filter(b -> tipo.isAssignableFrom(b.getClass()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException(
                            "Bean no encontrado: " + tipo.getName() +
                                    "\n→ ¿Lo registraste en AppContext?"));
        }
        return (T) bean;
    }
}