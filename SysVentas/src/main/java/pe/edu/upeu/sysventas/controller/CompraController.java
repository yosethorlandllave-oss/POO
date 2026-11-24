package pe.edu.upeu.sysventas.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import pe.edu.upeu.sysventas.components.AutoCompleteTextField;
import pe.edu.upeu.sysventas.components.ColumnInfo;
import pe.edu.upeu.sysventas.components.EditarCarritoDialog;
import pe.edu.upeu.sysventas.components.TableViewHelper;
import pe.edu.upeu.sysventas.components.Toast;
import pe.edu.upeu.sysventas.dto.ModeloDataAutocomplet;
import pe.edu.upeu.sysventas.dto.SessionManager;
import pe.edu.upeu.sysventas.model.CompCarrito;
import pe.edu.upeu.sysventas.model.Compra;
import pe.edu.upeu.sysventas.model.CompraDetalle;
import pe.edu.upeu.sysventas.service.*;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Consumer;

/**
 * Registro de compras a proveedores (Yoseth).
 * Flujo: elegir proveedor -> agregar productos al carrito -> registrar la compra.
 * Al registrar se guarda la cabecera (compra), el detalle (compra_detalle)
 * y el stock de cada producto SUBE con la cantidad comprada.
 */
public class CompraController {
    @FXML private AnchorPane miContenedor;
    @FXML TextField autocompProveedor, txtRucProveedor, txtRazonSocial;
    @FXML TextField autocompProducto, nombreProducto, codigoPro, stockPro, cantidadPro, pcompraPro, preTPro;
    @FXML TextField txtSerie, txtNumDoc, txtBaseImp, txtIgv, txtImporteT;
    @FXML ComboBox<String> cbxTipoDoc;
    @FXML DatePicker dpFechaComp;
    @FXML Button btnRegCarrito, btnRegCompra;
    @FXML Label lbnMsg;
    @FXML TableView<CompCarrito> tableView;

    private final IProveedorService provS;
    private final IProductoService ps;
    private final ICompraCarritoService carS;
    private final IUsuarioService us;
    private final ICompraService compS;
    private final ICompraDetalleService detS;

    private final SortedSet<ModeloDataAutocomplet> entriesProv = new TreeSet<>((ModeloDataAutocomplet o1, ModeloDataAutocomplet o2) -> o1.toString().compareTo(o2.toString()));
    private final SortedSet<ModeloDataAutocomplet> entriesProd = new TreeSet<>((ModeloDataAutocomplet o1, ModeloDataAutocomplet o2) -> o1.toString().compareTo(o2.toString()));
    AutoCompleteTextField actfProv;
    AutoCompleteTextField actfProd;
    private Long idProveedorSel = null;

    public CompraController(IProveedorService provS, IProductoService ps, ICompraCarritoService carS,
                            IUsuarioService us, ICompraService compS, ICompraDetalleService detS) {
        this.provS = provS;
        this.ps = ps;
        this.carS = carS;
        this.us = us;
        this.compS = compS;
        this.detS = detS;
    }

    @FXML
    public void initialize() {
        cbxTipoDoc.getItems().addAll("Factura", "Boleta", "Guia");
        cbxTipoDoc.getSelectionModel().select("Boleta");   // por defecto: Boleta
        dpFechaComp.setValue(LocalDate.now());

        // Proveedor: autocompletado con los proveedores del CRUD de Yerson
        entriesProv.addAll(provS.listAutoCompletProveedor());
        actfProv = new AutoCompleteTextField<>(entriesProv, autocompProveedor);
        autocompProveedor.setOnKeyReleased(e -> {
            ModeloDataAutocomplet prov = (ModeloDataAutocomplet) actfProv.getLastSelectedObject();
            if (prov != null) {
                idProveedorSel = Long.parseLong(prov.getIdx());
                txtRazonSocial.setText(prov.getNameDysplay());
                txtRucProveedor.setText(prov.getOtherData());
                listar();
            }
        });

        // Producto: autocompletado con los productos del CRUD de Eydam
        listarProductos();
        actfProd = new AutoCompleteTextField<>(entriesProd, autocompProducto);
        autocompProducto.setOnKeyReleased(e -> {
            ModeloDataAutocomplet prod = (ModeloDataAutocomplet) actfProd.getLastSelectedObject();
            if (prod != null) {
                nombreProducto.setText(prod.getNameDysplay());
                codigoPro.setText(prod.getIdx());
                String[] dato = prod.getOtherData().split(":");
                pcompraPro.setText(dato[0]); // se propone el precio actual; se puede cambiar
                stockPro.setText(dato[1]);
                calcularPT();
            }
        });

        personalizarTabla();
        btnRegCarrito.setDisable(true);
        lbnMsg.setText("");
    }

    private void listarProductos() {
        entriesProd.clear();
        entriesProd.addAll(ps.listAutoCompletProducto());
    }

    public void personalizarTabla() {
        TableViewHelper<CompCarrito> helper = new TableViewHelper<>();
        LinkedHashMap<String, ColumnInfo> columns = new LinkedHashMap<>();
        columns.put("ID Prod", new ColumnInfo("idProducto.idProducto", 80.0));
        columns.put("Nombre Producto", new ColumnInfo("nombreProducto", 280.0));
        columns.put("Cantidad", new ColumnInfo("cantidad", 80.0));
        columns.put("P.Compra", new ColumnInfo("punitario", 100.0));
        columns.put("P.Total", new ColumnInfo("ptotal", 100.0));
        Consumer<CompCarrito> updateAction = this::editarDelCarrito;
        Consumer<CompCarrito> deleteAction = this::eliminarDelCarrito;
        helper.addColumnsInOrderWithSize(tableView, columns, updateAction, deleteAction);
        TableViewHelper.formatoCarrito(tableView, List.of("P.Compra", "P.Total"), List.of("ID Prod", "Cantidad"));
        tableView.setPlaceholder(new Label("Elige un proveedor y agrega productos a la compra"));
        tableView.setTableMenuButtonVisible(true);
    }

    /** Muestra el carrito del proveedor elegido y calcula base, IGV y total. */
    public void listar() {
        tableView.getItems().clear();
        if (idProveedorSel == null) return;
        List<CompCarrito> lista = carS.listaCarritoProveedor(idProveedorSel);
        double total = 0;
        for (CompCarrito c : lista) {
            total += c.getPtotal();
        }
        double base = total / 1.18;
        txtImporteT.setText(String.valueOf(Math.round(total * 100.0) / 100.0));
        txtBaseImp.setText(String.valueOf(Math.round(base * 100.0) / 100.0));
        txtIgv.setText(String.valueOf(Math.round((total - base) * 100.0) / 100.0));
        tableView.getItems().addAll(lista);
    }

    @FXML
    public void calcularPT() {
        try {
            double cantidad = Double.parseDouble(cantidadPro.getText());
            double precio = Double.parseDouble(pcompraPro.getText());
            preTPro.setText(String.valueOf(Math.round(cantidad * precio * 100.0) / 100.0));
            btnRegCarrito.setDisable(!(cantidad > 0 && precio > 0));
        } catch (Exception e) {
            preTPro.clear();
            btnRegCarrito.setDisable(true);
        }
    }

    @FXML
    public void registrarPCarrito() {
        if (idProveedorSel == null) {
            mensaje("Primero elija un proveedor", true);
            return;
        }
        if (codigoPro.getText().isBlank()) {
            mensaje("Elija un producto", true);
            return;
        }
        try {
            CompCarrito item = CompCarrito.builder()
                    .idProveedor(provS.findById(idProveedorSel))
                    .idProducto(ps.findById(Long.parseLong(codigoPro.getText())))
                    .nombreProducto(nombreProducto.getText())
                    .cantidad(Double.parseDouble(cantidadPro.getText()))
                    .punitario(Double.parseDouble(pcompraPro.getText()))
                    .ptotal(Double.parseDouble(preTPro.getText()))
                    .estado(1)
                    .idUsuario(us.findById(SessionManager.getInstance().getUserId()))
                    .build();
            carS.save(item);
            limpiarProducto();
            listar();
            mensaje("", false);
        } catch (Exception e) {
            mensaje("No se pudo agregar: " + e.getMessage(), true);
        }
    }

    /** MODIFICAR: cambia la cantidad y el precio de compra de un producto del carrito. */
    public void editarDelCarrito(CompCarrito obj) {
        Optional<double[]> nuevo = EditarCarritoDialog.mostrar(
                miContenedor.getScene() != null ? miContenedor.getScene().getWindow() : null,
                "Modificar producto de la compra", obj.getNombreProducto(),
                obj.getCantidad(), obj.getPunitario(), true, null);
        if (nuevo.isEmpty()) return;
        try {
            obj.setCantidad(nuevo.get()[0]);
            obj.setPunitario(nuevo.get()[1]);
            obj.setPtotal(Math.round(nuevo.get()[0] * nuevo.get()[1] * 100.0) / 100.0);
            carS.update(obj.getIdCompcarrito(), obj);
            listar();
            mensaje("Producto modificado: " + obj.getNombreProducto(), false);
        } catch (Exception e) {
            mensaje("No se pudo modificar: " + e.getMessage(), true);
        }
    }

    public void eliminarDelCarrito(CompCarrito obj) {
        // La confirmación ya la pide la tabla (TableViewHelper) antes de llamar a este método
        carS.delete(obj.getIdCompcarrito());
        listar();
        mensaje("Producto quitado de la compra", false);
    }

    @FXML
    public void registrarCompra() {
        if (idProveedorSel == null) {
            mensaje("Primero elija un proveedor", true);
            return;
        }
        List<CompCarrito> carrito = carS.listaCarritoProveedor(idProveedorSel);
        if (carrito.isEmpty()) {
            mensaje("No hay productos en el carrito", true);
            return;
        }
        // Si no eligen tipo, se guarda como Boleta
        String tipo = cbxTipoDoc.getValue() == null ? "Boleta" : cbxTipoDoc.getValue();
        // Si dejan vacía la serie o el número, se generan solos (ej: B001 - 00000005)
        String serie = txtSerie.getText().trim();
        String numero = txtNumDoc.getText().trim();
        if (serie.isEmpty()) serie = tipo.substring(0, 1).toUpperCase() + "001";
        if (numero.isEmpty()) numero = String.format("%08d", compS.finAll().size() + 1);
        if (serie.length() > 10 || numero.length() > 12) {
            mensaje("La serie es de máx. 10 caracteres y el número de máx. 12", true);
            return;
        }
        try {
            Compra compra = Compra.builder()
                    .idProveedor(provS.findById(idProveedorSel))
                    .idUsuario(us.findById(SessionManager.getInstance().getUserId()))
                    .precioBase(Double.parseDouble(txtBaseImp.getText()))
                    .igv(Double.parseDouble(txtIgv.getText()))
                    .precioTotal(Double.parseDouble(txtImporteT.getText()))
                    .serie(serie)
                    .numDoc(numero)
                    .tipoDoc(tipo)
                    .fechaComp(dpFechaComp.getValue() == null ? LocalDate.now() : dpFechaComp.getValue())
                    .fechaReg(LocalDate.now())
                    .build();
            Compra guardada = compS.save(compra);

            for (CompCarrito c : carrito) {
                CompraDetalle det = CompraDetalle.builder()
                        .idCompra(guardada)
                        .idProducto(c.getIdProducto())
                        .cantidad(c.getCantidad())
                        .pu(c.getPunitario())
                        .subtotal(c.getPtotal())
                        .build();
                detS.save(det);
                // Control de stock: la mercadería que entra suma al stock
                ps.actualizarStock(c.getIdProducto().getIdProducto(), c.getCantidad());
            }
            carS.deleteCarAll(idProveedorSel);
            listar();
            limpiarFormCliente();
            limpiarProducto();

            listarProductos();
            txtSerie.clear();
            txtNumDoc.clear();
            Stage stage = (Stage) miContenedor.getScene().getWindow();
            Toast.showToast(stage, "Compra registrada. El stock se actualizó.", 2500, stage.getWidth() / 1.5, stage.getHeight() / 2);
            mensaje(tipo + " " + serie + "-" + numero + " registrada · Compra N° " + guardada.getId()
                    + " · Total S/ " + String.format("%.2f", guardada.getPrecioTotal()), false);
            cbxTipoDoc.getSelectionModel().select("Boleta");
        } catch (Exception e) {
            mensaje("No se pudo registrar la compra: " + e.getMessage(), true);
        }
    }


    public void limpiarFormCliente(){
        listarProductos();
        txtRazonSocial.clear();
        txtRazonSocial.clear();
        txtRazonSocial.clear();
    }
    private void limpiarProducto() {
        autocompProducto.clear();
        nombreProducto.clear();
        codigoPro.clear();
        stockPro.clear();
        cantidadPro.clear();
        pcompraPro.clear();
        preTPro.clear();
        btnRegCarrito.setDisable(true);
    }

    private void mensaje(String texto, boolean error) {
        lbnMsg.setText(texto);
        lbnMsg.setStyle(error ? "-fx-text-fill: red; -fx-font-size: 13px;" : "-fx-text-fill: #2e7d32; -fx-font-size: 13px;");
    }
}
