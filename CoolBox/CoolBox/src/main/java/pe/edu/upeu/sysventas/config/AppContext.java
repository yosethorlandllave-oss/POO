package pe.edu.upeu.sysventas.config;

import pe.edu.upeu.sysventas.controller.*;
import pe.edu.upeu.sysventas.repository.*;
import pe.edu.upeu.sysventas.service.*;
import pe.edu.upeu.sysventas.service.impl.*;
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


        registrar(CategoriaRepository.class,     new CategoriaRepository());
        registrar(MarcaRepository.class,         new MarcaRepository());
        registrar(UnidadMedidaRepository.class,  new UnidadMedidaRepository());
        registrar(ProductoRepository.class,      new ProductoRepository());

        cargarDatosDeEjemplo();
    }

    // Datos de ejemplo: se cargan UNA vez al iniciar. Antes se volvían a sembrar
    // cada vez que una lista quedaba vacía, así que eliminar el último registro
    // lo hacía reaparecer.
    private void cargarDatosDeEjemplo() {
        CategoriaRepository categorias = getBean(CategoriaRepository.class);
        MarcaRepository marcas = getBean(MarcaRepository.class);
        UnidadMedidaRepository unidades = getBean(UnidadMedidaRepository.class);
        categorias.seedData();
        marcas.seedData();
        unidades.seedData();
        getBean(ProductoRepository.class).seedData(
                categorias.findById(1L).orElseThrow(),
                marcas.findById(1L).orElseThrow(),
                unidades.findById(1L).orElseThrow());
    }

    // CAPA 2 — SERVICIOS
    // Cada servicio recibe su repositorio por constructor.
    // Usamos getBean() para buscarlo en el directorio: no creamos nada nuevo.
    // Los catálogos reciben también el repositorio de productos para impedir
    // eliminar registros en uso, y el de productos los de catálogo para validar
    // que sus relaciones existan.
    private void registrarServicios() {

        registrar(ICategoriaService.class,    new CategoriaServiceImp(   getBean(CategoriaRepository.class),
                getBean(ProductoRepository.class)));
        registrar(IMarcaService.class,        new MarcaServiceImp(       getBean(MarcaRepository.class),
                getBean(ProductoRepository.class)));
        registrar(IProductoService.class,     new ProductoServiceImp(    getBean(ProductoRepository.class),
                getBean(MarcaRepository.class), getBean(CategoriaRepository.class),
                getBean(UnidadMedidaRepository.class)));
        registrar(IUnidadMedidaService.class, new UnidadMedidaServiceImp(getBean(UnidadMedidaRepository.class),
                getBean(ProductoRepository.class)));


    }

    // CAPA 3 — CONTROLADORES JavaFX
    // Cada controlador recibe los servicios que necesita por constructor.
    // El FXMLLoader los busca aquí a través de setControllerFactory().
    private void registrarControladores() {
        //registrar(LoginController.class, new LoginController(getBean(IUsuarioService.class)));
        registrar(MainGuiController.class, new MainGuiController(
                getBean(IProductoService.class),
                getBean(IMarcaService.class),
                getBean(ICategoriaService.class),
                getBean(IUnidadMedidaService.class)));

        registrar(ProductoController.class,
                new ProductoController(
                        getBean(IMarcaService.class),
                        getBean(ICategoriaService.class),
                        getBean(IProductoService.class),
                        getBean(IUnidadMedidaService.class)));

        registrar(MarcaController.class,         new MarcaController(getBean(IMarcaService.class)));
        registrar(CategoriaController.class,     new CategoriaController(getBean(ICategoriaService.class)));
        registrar(UnidadMedidaController.class,  new UnidadMedidaController(getBean(IUnidadMedidaService.class)));

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