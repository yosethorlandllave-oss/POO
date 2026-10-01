package pe.edu.upeu.sysventas.controller;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import pe.edu.upeu.sysventas.model.*;
import pe.edu.upeu.sysventas.service.ICompraService;
import pe.edu.upeu.sysventas.service.IProductoService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CompraController {
 private final ICompraService compraService;
 private final IProductoService productoService;
 @FXML private TextField txtProveedor,txtDocumentoProveedor,txtCantidad,txtPrecioUnitario,txtSubtotal,txtBuscar;
 @FXML private DatePicker dpFechaCompra;
 @FXML private ComboBox<Producto> cbxProducto;
 @FXML private TableView<CompraDetalle> tablaDetalles;
 @FXML private TableColumn<CompraDetalle,String> colProductoDetalle;
 @FXML private TableColumn<CompraDetalle,Double> colCantidadDetalle,colPrecioDetalle,colSubtotalDetalle;
 @FXML private TableView<Compra> tablaCompras;
 @FXML private TableColumn<Compra,Long> colIdCompra;
 @FXML private TableColumn<Compra,String> colProveedorCompra,colFechaCompra;
 @FXML private TableColumn<Compra,Double> colTotalCompra;
 @FXML private Label lblTotalCompra,lblMensaje;
 private final List<CompraDetalle> detalles=new ArrayList<>();
 private Long idCompraEditando;

 public CompraController(ICompraService compraService,IProductoService productoService){this.compraService=compraService;this.productoService=productoService;}

 @FXML public void initialize(){
  cbxProducto.setItems(FXCollections.observableArrayList(productoService.findAll()));
  cbxProducto.setCellFactory(v->new ListCell<>(){
   @Override protected void updateItem(Producto p,boolean empty){super.updateItem(p,empty);setText(empty||p==null?null:p.getNombre());}
  });
  cbxProducto.setButtonCell(new ListCell<>(){
   @Override protected void updateItem(Producto p,boolean empty){super.updateItem(p,empty);setText(empty||p==null?null:p.getNombre());}
  });
  cbxProducto.getSelectionModel().selectedItemProperty().addListener((o,a,p)->{if(p!=null&&p.getPu()!=null){txtPrecioUnitario.setText(p.getPu().toString());recalcularSubtotal();}});
  txtCantidad.textProperty().addListener((o,a,b)->recalcularSubtotal());
  colProductoDetalle.setCellValueFactory(c->new SimpleStringProperty(c.getValue().getProducto()==null?"":c.getValue().getProducto().getNombre()));
  colCantidadDetalle.setCellValueFactory(c->new SimpleObjectProperty<>(c.getValue().getCantidad()));
  colPrecioDetalle.setCellValueFactory(c->new SimpleObjectProperty<>(c.getValue().getPrecioUnitario()));
  colSubtotalDetalle.setCellValueFactory(c->new SimpleObjectProperty<>(c.getValue().getSubtotal()));
  colIdCompra.setCellValueFactory(c->new SimpleObjectProperty<>(c.getValue().getIdCompra()));
  colProveedorCompra.setCellValueFactory(c->new SimpleStringProperty(c.getValue().getProveedor()==null?"":c.getValue().getProveedor().getNombresRaso()));
  colFechaCompra.setCellValueFactory(c->new SimpleStringProperty(c.getValue().getFechaCompra()==null?"":c.getValue().getFechaCompra().toString()));
  colTotalCompra.setCellValueFactory(c->new SimpleObjectProperty<>(c.getValue().getTotalCompra()));
  tablaCompras.getSelectionModel().selectedItemProperty().addListener((o,a,c)->{if(c!=null)cargar(c);});
  dpFechaCompra.setValue(LocalDate.now());listarCompras();
 }
 @FXML private void agregarDetalle(){
  Producto p=cbxProducto.getValue();Double q=parsear(txtCantidad.getText()),pu=parsear(txtPrecioUnitario.getText());
  if(p==null||q==null||q<=0||pu==null||pu<0){mensaje("Seleccione producto e ingrese cantidad y precio válidos.",true);return;}
  CompraDetalle d=new CompraDetalle(null,p,q,pu,0.0);d.calcularSubtotal();detalles.add(d);actualizarDetalles();
  cbxProducto.getSelectionModel().clearSelection();txtCantidad.clear();txtPrecioUnitario.clear();txtSubtotal.clear();mensaje("Producto agregado.",false);
 }
 @FXML private void quitarDetalle(){CompraDetalle d=tablaDetalles.getSelectionModel().getSelectedItem();if(d!=null){detalles.remove(d);actualizarDetalles();}}
 @FXML private void guardarCompra(){
  try{Compra c=construir();if(idCompraEditando==null)compraService.guardarCompra(c);else compraService.actualizarCompra(idCompraEditando,c);limpiarFormulario();mensaje("Compra guardada.",false);}
  catch(RuntimeException e){mensaje(e.getMessage(),true);}
 }
 @FXML private void editarCompra(){Compra c=tablaCompras.getSelectionModel().getSelectedItem();if(c==null){mensaje("Seleccione una compra para editar.",true);return;}cargar(c);}
 @FXML private void eliminarCompra(){
  Compra c=tablaCompras.getSelectionModel().getSelectedItem();if(c==null){mensaje("Seleccione una compra.",true);return;}
  try{compraService.eliminarCompra(c.getIdCompra());limpiarFormulario();mensaje("Compra eliminada.",false);}catch(RuntimeException e){mensaje(e.getMessage(),true);}
 }
 @FXML private void nuevaCompra(){limpiarFormulario();}
 @FXML private void buscarCompras(){tablaCompras.setItems(FXCollections.observableArrayList(compraService.buscarCompras(txtBuscar.getText())));}
 @FXML private void listarCompras(){tablaCompras.setItems(FXCollections.observableArrayList(compraService.listarCompras()));}
 private Compra construir(){
  String n=txtProveedor.getText()==null?"":txtProveedor.getText().trim();if(n.isEmpty())throw new IllegalArgumentException("Ingrese el proveedor.");
  String doc=txtDocumentoProveedor.getText()==null?"":txtDocumentoProveedor.getText().trim();
  Compra c=new Compra();c.setIdCompra(idCompraEditando);c.setProveedor(new Proveedor(null,doc,n,"RUC",null,null,null));
  c.setFechaCompra(dpFechaCompra.getValue());c.setDetalles(new ArrayList<>(detalles));c.calcularTotal();return c;
 }
 private void cargar(Compra c){
  idCompraEditando=c.getIdCompra();txtProveedor.setText(c.getProveedor()==null?"":c.getProveedor().getNombresRaso());
  txtDocumentoProveedor.setText(c.getProveedor()==null||c.getProveedor().getDniruc()==null?"":c.getProveedor().getDniruc());
  dpFechaCompra.setValue(c.getFechaCompra());detalles.clear();if(c.getDetalles()!=null)detalles.addAll(c.getDetalles());actualizarDetalles();mensaje("Editando compra "+c.getIdCompra(),false);
 }
 private void actualizarDetalles(){tablaDetalles.setItems(FXCollections.observableArrayList(detalles));lblTotalCompra.setText(String.format("%.2f",detalles.stream().mapToDouble(CompraDetalle::calcularSubtotal).sum()));}
 private void recalcularSubtotal(){Double q=parsear(txtCantidad.getText()),p=parsear(txtPrecioUnitario.getText());txtSubtotal.setText(q==null||p==null?"":String.format("%.2f",q*p));}
 private Double parsear(String s){if(s==null||s.isBlank())return null;try{return Double.parseDouble(s.trim());}catch(NumberFormatException e){return null;}}
 private void limpiarFormulario(){
  idCompraEditando=null;txtProveedor.clear();txtDocumentoProveedor.clear();txtCantidad.clear();txtPrecioUnitario.clear();txtSubtotal.clear();txtBuscar.clear();
  dpFechaCompra.setValue(LocalDate.now());cbxProducto.getSelectionModel().clearSelection();detalles.clear();actualizarDetalles();tablaCompras.getSelectionModel().clearSelection();listarCompras();
 }
 private void mensaje(String s,boolean error){lblMensaje.setText(s==null?"Error inesperado":s);lblMensaje.setStyle(error?"-fx-text-fill: red;":"-fx-text-fill: green;");}
}