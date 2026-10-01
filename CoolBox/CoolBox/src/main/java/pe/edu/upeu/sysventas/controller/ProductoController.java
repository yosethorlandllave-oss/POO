package pe.edu.upeu.sysventas.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;
import lombok.RequiredArgsConstructor;
import pe.edu.upeu.sysventas.components.TablaPaginada;
import pe.edu.upeu.sysventas.dto.ComboBoxOption;
import pe.edu.upeu.sysventas.enums.TipoProducto;
import pe.edu.upeu.sysventas.model.Categoria;
import pe.edu.upeu.sysventas.model.Marca;
import pe.edu.upeu.sysventas.model.Producto;
import pe.edu.upeu.sysventas.model.UnidMedida;
import pe.edu.upeu.sysventas.service.ICategoriaService;
import pe.edu.upeu.sysventas.service.ICrudGenericoService;
import pe.edu.upeu.sysventas.service.IMarcaService;
import pe.edu.upeu.sysventas.service.IProductoService;
import pe.edu.upeu.sysventas.service.IUnidadMedidaService;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

@RequiredArgsConstructor
public class ProductoController extends CrudController<Producto> {
    private final IMarcaService ms;
    private final ICategoriaService cs;
    private final IProductoService ps;
    private final IUnidadMedidaService us;

    private static final String FORMATO_INVALIDO = "Ingresa un número válido, por ejemplo 12.50";

    @FXML
    TextField txtNombreProducto, txtPUnit,
            txtPUnitOld, txtUtilidad, txtStock, txtStockOld;
    @FXML
    ComboBox<ComboBoxOption> cbxTipoProducto;
    @FXML ComboBox<ComboBoxOption> cbxMarca;
    @FXML ComboBox<ComboBoxOption> cbxCategoria;
    @FXML ComboBox<ComboBoxOption> cbxUnidMedida;

    private final ComboBox<ComboBoxOption> cbxFiltroTipo = new ComboBox<>();
    private final ComboBox<ComboBoxOption> cbxFiltroCategoria = new ComboBox<>();

    @Override
    protected ICrudGenericoService<Producto, Long> servicio() {
        return ps;
    }

    @Override
    protected String titulo() {
        return "Productos";
    }

    @Override
    protected String grupo() {
        return "Catálogo";
    }

    @Override
    protected String singular() {
        return "producto";
    }

    @Override
    protected boolean femenino() {
        return false;
    }

    @Override
    protected Long idDe(Producto p) {
        return p.getIdProducto();
    }

    @Override
    protected String nombreDe(Producto p) {
        return p.getNombre();
    }

    // ------------------------------------------------------------ listado

    @Override
    protected void configurarTabla(TablaPaginada<Producto> t) {
        // Anchos pensados para que todo quepa sin desplazamiento a partir de ~1280 px.
        t.agregarColumna("ID", 50, Producto::getIdProducto, p -> String.valueOf(p.getIdProducto()), true);
        TableColumn<Producto, Object> nombre = t.agregarColumna("Nombre", 160, Producto::getNombre);
        t.agregarColumna("Tipo", 90, p -> texto(p.getTipoProducto(), TipoProducto::getDescripcion));
        t.agregarColumna("Marca", 110, p -> texto(p.getIdMarca(), Marca::getNombre));
        t.agregarColumna("Categoría", 110, p -> texto(p.getIdCategoria(), Categoria::getNombre));
        t.agregarColumna("Unidad", 80, p -> texto(p.getIdUnidad(), UnidMedida::getNombreMedida));
        t.agregarColumna("P. unitario", 100, Producto::getPu, p -> soles(p.getPu()), true);
        t.agregarColumna("Utilidad", 90, Producto::getUtilidad, p -> numero(p.getUtilidad()), true);
        t.agregarColumna("Stock", 70, Producto::getStock, p -> numero(p.getStock()), true);
        t.setColumnaFlexible(nombre);

        // Búsqueda por los mismos datos que describe el manual, más tipo y unidad.
        t.setFiltroTexto((p, q) -> contiene(p.getNombre(), q)
                || contiene(texto(p.getTipoProducto(), TipoProducto::getDescripcion), q)
                || contiene(texto(p.getIdMarca(), Marca::getNombre), q)
                || contiene(texto(p.getIdCategoria(), Categoria::getNombre), q)
                || contiene(texto(p.getIdUnidad(), UnidMedida::getNombreMedida), q)
                || contiene(String.valueOf(p.getPu()), q)
                || contiene(String.valueOf(p.getUtilidad()), q));

        cbxFiltroTipo.setAccessibleText("Filtrar por tipo de producto");
        cbxFiltroTipo.setId("cbxFiltroTipo");
        cbxFiltroTipo.getStyleClass().add("filtro");
        cbxFiltroCategoria.setAccessibleText("Filtrar por categoría");
        cbxFiltroCategoria.setId("cbxFiltroCategoria");
        cbxFiltroCategoria.getStyleClass().add("filtro");
        cbxFiltroTipo.setOnAction(e -> t.refrescarFiltros());
        cbxFiltroCategoria.setOnAction(e -> t.refrescarFiltros());
        t.agregarFiltro(cbxFiltroTipo);
        t.agregarFiltro(cbxFiltroCategoria);
        t.setFiltroAdicional(p -> coincide(cbxFiltroTipo, p.getTipoProducto() == null ? null : p.getTipoProducto().name())
                && coincide(cbxFiltroCategoria, p.getIdCategoria() == null ? null
                : String.valueOf(p.getIdCategoria().getIdCategoria())));
    }

    @Override
    public void recargarListado() {
        cargarFiltros();
        super.recargarListado();
    }

    /** Recarga las opciones de los filtros conservando la selección si sigue existiendo. */
    private void cargarFiltros() {
        recargarCombo(cbxFiltroTipo, new ComboBoxOption("", "Todos los tipos"), ps.listarTipoProducto());
        recargarCombo(cbxFiltroCategoria, new ComboBoxOption("", "Todas las categorías"), cs.listarCombobox());
    }

    private static void recargarCombo(ComboBox<ComboBoxOption> combo, ComboBoxOption todos, List<ComboBoxOption> opciones) {
        String elegido = combo.getValue() == null ? "" : combo.getValue().getKey();
        combo.getItems().setAll(todos);
        combo.getItems().addAll(opciones);
        combo.setValue(combo.getItems().stream()
                .filter(o -> o.getKey().equals(elegido)).findFirst().orElse(todos));
    }

    private static boolean coincide(ComboBox<ComboBoxOption> filtro, String clave) {
        ComboBoxOption elegido = filtro.getValue();
        return elegido == null || elegido.getKey().isEmpty() || elegido.getKey().equals(clave);
    }

    // ------------------------------------------------------------ formulario

    @Override
    protected Map<String, Control> camposFormulario() {
        Map<String, Control> campos = new LinkedHashMap<>();
        campos.put("tipoProducto", cbxTipoProducto);
        campos.put("nombre", txtNombreProducto);
        campos.put("pu", txtPUnit);
        campos.put("puold", txtPUnitOld);
        campos.put("utilidad", txtUtilidad);
        campos.put("stock", txtStock);
        campos.put("stockold", txtStockOld);
        campos.put("idCategoria", cbxCategoria);
        campos.put("idMarca", cbxMarca);
        campos.put("idUnidad", cbxUnidMedida);
        return campos;
    }

    @Override
    protected void prepararFormulario() {
        // Se recargan cada vez: las marcas, categorías y unidades pueden haber
        // cambiado en sus propios módulos.
        cbxTipoProducto.getItems().setAll(ps.listarTipoProducto());
        cbxMarca.getItems().setAll(ms.listarCombobox());
        cbxCategoria.getItems().setAll(cs.listarCombobox());
        cbxUnidMedida.getItems().setAll(us.listarCombobox());
    }

    @Override
    protected void limpiarFormulario() {
        txtNombreProducto.clear();
        cbxTipoProducto.getSelectionModel().clearSelection();
        txtPUnit.clear(); txtPUnitOld.clear();
        txtUtilidad.clear(); txtStock.clear(); txtStockOld.clear();
        cbxMarca.getSelectionModel().clearSelection();
        cbxCategoria.getSelectionModel().clearSelection();
        cbxUnidMedida.getSelectionModel().clearSelection();
    }

    /** Conserva el nombre del método original del proyecto. */
    public void clearForm() {
        limpiarFormulario();
        limpiarErrores();
        idEnEdicion = 0L;
    }

    @Override
    protected void llenarFormulario(Producto producto) {
        txtNombreProducto.setText(producto.getNombre());
        txtPUnit.setText(editable(producto.getPu()));
        txtPUnitOld.setText(editable(producto.getPuold()));
        txtUtilidad.setText(editable(producto.getUtilidad()));
        txtStock.setText(editable(producto.getStock()));
        txtStockOld.setText(editable(producto.getStockold()));

        // Comparación con equals() y sin suponer relaciones no nulas
        // (antes: == entre String y NullPointerException con nulos).
        seleccionar(cbxTipoProducto, producto.getTipoProducto() == null ? null : producto.getTipoProducto().name());
        seleccionar(cbxMarca, producto.getIdMarca() == null ? null : String.valueOf(producto.getIdMarca().getIdMarca()));
        seleccionar(cbxCategoria, producto.getIdCategoria() == null ? null
                : String.valueOf(producto.getIdCategoria().getIdCategoria()));
        seleccionar(cbxUnidMedida, producto.getIdUnidad() == null ? null
                : String.valueOf(producto.getIdUnidad().getIdUnidad()));
    }

    /** Conserva el nombre del método original del proyecto. */
    public void editForm(Producto producto) {
        editar(producto);
    }

    @Override
    protected Producto leerFormulario(Map<String, String> erroresFormato) {
        Producto formulario = new Producto();
        formulario.setNombre(txtNombreProducto.getText());
        formulario.setPu(leerNumero(txtPUnit, "pu", false, erroresFormato));
        // Como en la versión original, los valores "anteriores" vacíos valen 0.
        formulario.setPuold(leerNumero(txtPUnitOld, "puold", true, erroresFormato));
        formulario.setUtilidad(leerNumero(txtUtilidad, "utilidad", false, erroresFormato));
        formulario.setStock(leerNumero(txtStock, "stock", false, erroresFormato));
        formulario.setStockold(leerNumero(txtStockOld, "stockold", true, erroresFormato));

        ComboBoxOption tipo = cbxTipoProducto.getValue();
        formulario.setTipoProducto(tipo == null ? null : TipoProducto.valueOf(tipo.getKey()));

        // Solo se envía el id: el servicio lo resuelve contra el registro vigente
        // y rechaza la operación si fue eliminado mientras tanto.
        Long idMarca = idElegido(cbxMarca);
        formulario.setIdMarca(idMarca == null ? null : Marca.builder().idMarca(idMarca).build());
        Long idCategoria = idElegido(cbxCategoria);
        formulario.setIdCategoria(idCategoria == null ? null : Categoria.builder().idCategoria(idCategoria).build());
        Long idUnidad = idElegido(cbxUnidMedida);
        formulario.setIdUnidad(idUnidad == null ? null : UnidMedida.builder().idUnidad(idUnidad).build());
        return formulario;
    }

    // ------------------------------------------------------------ detalle

    @Override
    protected LinkedHashMap<String, String> datosDetalle(Producto p) {
        LinkedHashMap<String, String> datos = new LinkedHashMap<>();
        datos.put("ID", String.valueOf(p.getIdProducto()));
        datos.put("Nombre", p.getNombre());
        datos.put("Tipo de producto", texto(p.getTipoProducto(), TipoProducto::getDescripcion));
        datos.put("Marca", texto(p.getIdMarca(), Marca::getNombre));
        datos.put("Categoría", texto(p.getIdCategoria(), Categoria::getNombre));
        datos.put("Unidad de medida", texto(p.getIdUnidad(), UnidMedida::getNombreMedida));
        datos.put("Precio unitario", soles(p.getPu()));
        datos.put("Precio anterior", soles(p.getPuold()));
        datos.put("Utilidad", numero(p.getUtilidad()));
        datos.put("Stock", numero(p.getStock()));
        datos.put("Stock anterior", numero(p.getStockold()));
        return datos;
    }

    // ------------------------------------------------------------ utilidades

    /**
     * Lee un número del campo. Vacío: null (el modelo dirá "es obligatorio"),
     * o 0 si vacioEsCero. Acepta coma o punto decimal. Texto inválido: error de
     * formato, nunca un 0 silencioso.
     */
    private static Double leerNumero(TextField campo, String propiedad, boolean vacioEsCero,
                                     Map<String, String> errores) {
        String texto = campo.getText() == null ? "" : campo.getText().trim();
        if (texto.isEmpty()) {
            return vacioEsCero ? 0.0 : null;
        }
        try {
            double valor = Double.parseDouble(texto.replace(',', '.'));
            if (Double.isNaN(valor) || Double.isInfinite(valor)) {
                throw new NumberFormatException(texto);
            }
            return valor;
        } catch (NumberFormatException e) {
            errores.put(propiedad, FORMATO_INVALIDO);
            return null;
        }
    }

    private static Long idElegido(ComboBox<ComboBoxOption> combo) {
        ComboBoxOption opcion = combo.getValue();
        return opcion == null ? null : Long.valueOf(opcion.getKey());
    }

    private static void seleccionar(ComboBox<ComboBoxOption> combo, String clave) {
        combo.getSelectionModel().select(combo.getItems().stream()
                .filter(o -> Objects.equals(o.getKey(), clave))
                .findFirst().orElse(null));
    }

    private static <V> String texto(V valor, Function<V, String> extractor) {
        return valor == null ? "" : Objects.toString(extractor.apply(valor), "");
    }

    private static boolean contiene(String texto, String q) {
        return texto != null && texto.toLowerCase(Locale.ROOT).contains(q);
    }

    private static String editable(Double valor) {
        return valor == null ? "" : BigDecimal.valueOf(valor).stripTrailingZeros().toPlainString();
    }

    static String soles(Double valor) {
        return valor == null ? "" : String.format(Locale.US, "S/ %,.2f", valor);
    }

    static String numero(Double valor) {
        return valor == null ? "" : String.format(Locale.US, "%,.2f", valor);
    }
}
