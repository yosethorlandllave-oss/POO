package pe.edu.upeu.sysventas.controller;


import com.github.anastaciocintra.escpos.EscPos;
import com.github.anastaciocintra.escpos.EscPosConst;
import com.github.anastaciocintra.escpos.Style;
import com.github.anastaciocintra.escpos.barcode.QRCode;
import com.github.anastaciocintra.output.PrinterOutputStream;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import pe.edu.upeu.sysventas.components.*;
import pe.edu.upeu.sysventas.dto.ModeloDataAutocomplet;
import pe.edu.upeu.sysventas.dto.PersonaDto;
import pe.edu.upeu.sysventas.dto.SessionManager;
import pe.edu.upeu.sysventas.dto.comprobante.Comprobante;
import pe.edu.upeu.sysventas.enums.TipoDocumento;
import pe.edu.upeu.sysventas.enums.TipoProducto;
import pe.edu.upeu.sysventas.exception.ModelNotFoundException;
import pe.edu.upeu.sysventas.model.Cliente;
import pe.edu.upeu.sysventas.model.Producto;
import pe.edu.upeu.sysventas.model.VentCarrito;
import pe.edu.upeu.sysventas.model.Venta;
import pe.edu.upeu.sysventas.model.VentaDetalle;
import pe.edu.upeu.sysventas.service.*;
import pe.edu.upeu.sysventas.utils.ConsultaDNI;
import pe.edu.upeu.sysventas.utils.PrinterManager;

import javax.print.PrintService;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;


public class VentaController {
    @FXML
    TextField autocompProducto;
    @FXML
    TextField nombreProducto, codigoPro, stockPro, cantidadPro, punitPro, preTPro, txtBaseImp, txtIgv, txtDescuento, txtImporteT;
    @FXML
    Button btnRegVenta, btnRegCarrito, btnFormCliente, btnRegCliente;
    @FXML
    TextField autocompCliente, txtDireccion;
    @FXML
    TextField razonSocial;
    @FXML
    TextField dniRuc;
    @FXML
    TableView<VentCarrito> tableView;
    AutoCompleteTextField actf;
    ModeloDataAutocomplet lastProducto;
    AutoCompleteTextField actfC;
    ModeloDataAutocomplet lastCliente;

    IProductoService ps;
    IClienteService cs;
    IVentaCarritoService daoC;
    IUsuarioService daoU;
    IVentaService daoV;
    IVentaDetalleService daoVD;
    Stage stage;
    @FXML
    private AnchorPane miContenedor;
    private final SortedSet<ModeloDataAutocomplet> entries = new TreeSet<>((ModeloDataAutocomplet o1, ModeloDataAutocomplet o2) -> o1.toString().compareTo(o2.toString()));
    private final SortedSet<ModeloDataAutocomplet> entriesC = new TreeSet<>((ModeloDataAutocomplet o1, ModeloDataAutocomplet o2) -> o1.toString().compareTo(o2.toString()));

    ConsultaDNI cDni;
    TipoProducto tipoProducto;


    public VentaController(IProductoService ps, IClienteService cs, IVentaCarritoService daoC, IUsuarioService daoU, IVentaService daoV, IVentaDetalleService daoVD, ConsultaDNI cDni) {
        this.ps = ps;
        this.cs = cs;
        this.daoC = daoC;
        this.daoU = daoU;
        this.daoV = daoV;
        this.daoVD = daoVD;
        this.cDni = cDni;
    }

    @FXML
    public void initialize(){

        Platform.runLater(() -> {
            stage = (Stage) miContenedor.getScene().getWindow();
            System.out.println("El título del stage es: " + stage.getTitle());
        });

        listarCliente();
        autoCompletarCliente();

        listarProducto();
        actf=new AutoCompleteTextField<>(entries, autocompProducto);
        autocompProducto.setOnKeyReleased(e->{
            lastProducto=(ModeloDataAutocomplet) actf.getLastSelectedObject();
            if(lastProducto!=null){
                System.out.println(lastProducto.getNameDysplay());
                nombreProducto.setText(lastProducto.getNameDysplay());
                codigoPro.setText(lastProducto.getIdx());
                String[] dato=lastProducto.getOtherData().split(":");
                punitPro.setText(dato[0]);
                stockPro.setText(dato[1]);
                tipoProducto=TipoProducto.valueOf(dato[2]);
                // Cantidad 1 por defecto: así el botón Add del carrito se activa al instante
                if (cantidadPro.getText() == null || cantidadPro.getText().isBlank()) {
                    cantidadPro.setText("1");
                }
                calcularPT();
                try {
                    Producto prodSel = ps.findById(Long.parseLong(codigoPro.getText()));
                    double puActual = Double.parseDouble(punitPro.getText().trim());
                    double descUnit = (prodSel != null && prodSel.getPuold() != null && prodSel.getPuold() > puActual)
                            ? (prodSel.getPuold() - puActual)
                            : 0.0;
                    double cantActual = 1.0;
                    try {
                        cantActual = Double.parseDouble(cantidadPro.getText().trim());
                    } catch (Exception ignored) {}
                    if (txtDescuento != null) {
                        txtDescuento.setText(String.format(Locale.US, "%.2f", descUnit * cantActual));
                    }
                } catch (Exception ignored) {
                    if (txtDescuento != null) {
                        txtDescuento.setText("0.00");
                    }
                }
            }
        });


        personalizarTabla();
        btnRegCarrito.setDisable(true);
        cantidadPro.textProperty().addListener((obs, antes, ahora) -> calcularPT());
        if (txtDescuento != null) {
            txtDescuento.setText("0.00");
            txtDescuento.textProperty().addListener((obs, antes, ahora) -> recalcularConDescuento());
        }
    }

    public void listarProducto(){
        entries.addAll(ps.listAutoCompletProducto());
    }
    public void listarCliente(){
        entriesC.clear();
        entriesC.addAll(cs.listAutoCompletCliente());
    }

    public void autoCompletarCliente(){
        actfC=new AutoCompleteTextField<>(entriesC, autocompCliente);
        autocompCliente.setOnKeyReleased(e->{
            lastCliente=(ModeloDataAutocomplet) actfC.getLastSelectedObject();
            if(lastCliente!=null){
                System.out.println(lastCliente.getNameDysplay());
                razonSocial.setText(lastCliente.getNameDysplay());
                dniRuc.setText(lastCliente.getIdx());
                try { txtDireccion.setText(cs.findById(lastCliente.getIdx()).getDireccion()); } catch (Exception ignored) {}
                listar();
            }else {
                limpiarFormCliente();
            }
        });
    }




    @FXML
    public void buscarClienteCdni(){
        limpiarFormCliente();
        Stage stage = StageManager.getPrimaryStage();
        double with=stage.getX() + stage.getWidth()/2 - 150;
        if(autocompCliente.getText().length()==8 || autocompCliente.getText().length()==11){
            try {
                Cliente encontrado = cs.findById(autocompCliente.getText());
                if(encontrado!=null){
                    dniRuc.setText(encontrado.getDniruc());
                    razonSocial.setText(encontrado.getNombres());
                    txtDireccion.setText(encontrado.getDireccion());
                    listar();
                    Toast.showToast(stage, "Cliente encontrado: " + encontrado.getNombres(), 2000, with, 50);
                    return;}
                consultarDNIReniec(with);
            } catch (ModelNotFoundException e) {
                // No está registrado: se prepara para registrarlo con Add
                consultarDNIReniec(with);
            }
        }else{
            Toast.showToast(stage, "El valor buscado debe tener 8 o 11 digitos", 2000, with, 50);
        }
    }



    /**
     * El cliente no está registrado: se intenta completar su nombre con RENIEC (solo DNI)
     * y se activa el botón Add para registrarlo. Si no hay internet, se escribe el nombre a mano.
     */
    public void consultarDNIReniec(double with){
        Stage st = StageManager.getPrimaryStage();
        String doc = autocompCliente.getText().trim();
        PersonaDto p = null;
        if (doc.length() == 8) {
            try { p = cDni.consultarDNI(doc); } catch (Exception ignored) { }
        }
        dniRuc.setText(doc);
        if (p != null && p.getNombre() != null && !p.getNombre().isBlank()) {
            razonSocial.setText(p.getNombre() + " " + p.getApellidoPaterno() + " " + p.getApellidoMaterno());
            Toast.showToast(st, "Cliente encontrado en RENIEC: completa la dirección y presiona Nuevo para registrarlo", 3000, with, 80);
            txtDireccion.requestFocus();
        } else {
            Toast.showToast(st, "Cliente nuevo: escribe su nombre y dirección y presiona Nuevo para registrarlo", 3000, with, 80);
            razonSocial.requestFocus();
        }
    }

    public void limpiarFormCliente(){
        razonSocial.clear();
        dniRuc.clear();
        txtDireccion.clear();
    }


    @FXML
    public void guardarCliente(){
        Stage stage = StageManager.getPrimaryStage();
        double with=stage.getX() + stage.getWidth()/2 - 150;
        String doc = dniRuc.getText() == null ? "" : dniRuc.getText().trim();
        String nombre = razonSocial.getText() == null ? "" : razonSocial.getText().trim();
        if (doc.isEmpty()) {
            Toast.showToast(stage, "Escribe el DNI/RUC en el primer campo y presiona Buscar", 2500, with, 80);
            autocompCliente.requestFocus();
            return;
        }
        // Si el cliente ya existe no se vuelve a registrar: se avisa y se sigue con la venta
        try {
            Cliente existente = cs.findById(doc);
            if (existente != null) {
                Toast.showToast(stage, "Este cliente ya está registrado. Continúa eligiendo el producto.", 2500, with, 80);
                autocompProducto.requestFocus();
                return;
            }
        } catch (Exception noExiste) {
            // no está registrado: se guarda abajo
        }
        if (!doc.matches("\\d{8}") && !doc.matches("\\d{11}")) {
            Toast.showToast(stage, "El DNI debe tener 8 dígitos o el RUC 11 dígitos", 2500, with, 80);
            return;
        }
        if (nombre.isEmpty()) {
            Toast.showToast(stage, "Escribe el nombre o razón social del cliente", 2500, with, 80);
            razonSocial.requestFocus();
            return;
        }
        try {
            Cliente c= Cliente.builder().dniruc(doc)
                    .nombres(nombre)
                    .repLegal(nombre)
                    .tipoDocumento(doc.length() == 11 ? TipoDocumento.RUC : TipoDocumento.DNI)
                    .direccion(txtDireccion.getText())
                    .build();
            cs.save(c);
            Toast.showToast(stage, "El cliente se guardo satisfactoriamente!", 2000, with, 50);
            listarCliente();
            listar();
        }catch (Exception e){
            Toast.showToast(stage, "Error al guardar cliente!", 2000, with, 50);
        }
    }

    public void personalizarTabla(){
        TableViewHelper<VentCarrito> tableViewHelper = new TableViewHelper<>();
        LinkedHashMap<String, ColumnInfo> columns = new LinkedHashMap<>();
        columns.put("ID Prod", new ColumnInfo("idProducto.idProducto", 100.0)); // Columna visible "Columna 1" mapea al campo "campo1"
        columns.put("Nombre Producto", new ColumnInfo("nombreProducto", 300.0)); // Columna visible "Columna 1" mapea al campo "campo1"
        columns.put("Cantidad", new ColumnInfo("cantidad", 60.0)); // Columna visible "Columna 2" mapea al campo "campo2"
        columns.put("P.Unitario", new ColumnInfo("punitario", 100.0)); // Columna visible "Columna 2" mapea al campo "campo2"
        columns.put("P.Total", new ColumnInfo("ptotal", 100.0)); // Columna visible "Columna 2" mapea al campo "campo2"

        Consumer<VentCarrito> updateAction = this::editVenCarrito;
        Consumer<VentCarrito> deleteAction = (VentCarrito ventCarrito) -> {deleteReg(ventCarrito); };

        tableViewHelper.addColumnsInOrderWithSize(tableView, columns,updateAction, deleteAction );
        TableViewHelper.formatoCarrito(tableView, List.of("P.Unitario", "P.Total"), List.of("ID Prod", "Cantidad"));
        tableView.setPlaceholder(new Label("Elige un cliente y agrega productos al carrito"));

        tableView.setTableMenuButtonVisible(true);
    }

    public void listar(){
        tableView.getItems().clear();
        List<VentCarrito> lista=daoC.listaCarritoCliente(dniRuc.getText());
        double impoTotal = 0.0;
        double descuentoPredeterminado = 0.0;

        for (VentCarrito dato: lista){
            impoTotal += Double.parseDouble(String.valueOf(dato.getPtotal()));
            try {
                Producto prod = ps.findById(dato.getIdProducto().getIdProducto());
                if (prod != null && prod.getPuold() != null && prod.getPuold() > dato.getPunitario()) {
                    descuentoPredeterminado += (prod.getPuold() - dato.getPunitario()) * dato.getCantidad();
                }
            } catch (Exception ignored) {}
        }

        descuentoPredeterminado = Math.round(descuentoPredeterminado * 100.0) / 100.0;
        if (txtDescuento != null) {
            txtDescuento.setText(String.format(Locale.US, "%.2f", descuentoPredeterminado));
        }

        aplicarTotales(impoTotal, descuentoPredeterminado);
        tableView.getItems().addAll(lista);
    }

    private double leerDescuentoSeguro(double subtotal) {
        if (txtDescuento == null) return 0.0;
        String texto = txtDescuento.getText();
        if (texto == null || texto.trim().isEmpty()) return 0.0;
        try {
            double desc = Double.parseDouble(texto.trim());
            if (desc < 0) desc = 0.0;
            if (desc > subtotal) desc = subtotal;
            return desc;
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private void aplicarTotales(double subtotalCarrito, double descuento) {
        double totalFinal = Math.max(0.0, subtotalCarrito - descuento);
        totalFinal = Math.round(totalFinal * 100.0) / 100.0;
        double baseImp = Math.round((totalFinal / 1.18) * 100.0) / 100.0;
        double igv = Math.round((totalFinal - baseImp) * 100.0) / 100.0;

        txtImporteT.setText(String.format(Locale.US, "%.2f", totalFinal));
        txtBaseImp.setText(String.format(Locale.US, "%.2f", baseImp));
        txtIgv.setText(String.format(Locale.US, "%.2f", igv));
    }

    private void recalcularConDescuento() {
        double subtotalCarrito = 0.0;
        for (VentCarrito dato : tableView.getItems()) {
            subtotalCarrito += dato.getPtotal();
        }
        double desc = leerDescuentoSeguro(subtotalCarrito);
        aplicarTotales(subtotalCarrito, desc);
    }

    /** MODIFICAR: cambia la cantidad de un producto del carrito (respetando el stock). */
    public void editVenCarrito(VentCarrito obj) {
        Stage st = StageManager.getPrimaryStage();
        Double stock = null;
        try {
            stock = ps.findById(obj.getIdProducto().getIdProducto()).getStock();
        } catch (Exception ignored) { }
        Optional<double[]> nuevo = EditarCarritoDialog.mostrar(
                miContenedor.getScene() != null ? miContenedor.getScene().getWindow() : st,
                "Modificar producto del carrito", obj.getNombreProducto(),
                obj.getCantidad(), obj.getPunitario(), false, stock);
        if (nuevo.isEmpty()) return;
        try {
            obj.setCantidad(nuevo.get()[0]);
            obj.setPtotal(Math.round(nuevo.get()[0] * obj.getPunitario() * 100.0) / 100.0);
            daoC.update(obj.getIdCarrito(), obj);
            listar();
            if (st != null) {
                Toast.showToast(st, "Cantidad actualizada: " + obj.getNombreProducto(), 2000, st.getX() + st.getWidth() / 2 - 150, 80);
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Coolbox");
            alert.setHeaderText("No se pudo modificar el producto");
            alert.setContentText(e.getMessage() != null ? e.getMessage() : e.toString());
            alert.showAndWait();
        }
    }

    public void deleteReg(VentCarrito obj) {
        // La confirmación ya la pide la tabla (TableViewHelper) antes de llamar a este método
        daoC.delete(obj.getIdCarrito());
        Stage stage = StageManager.getPrimaryStage();
        if (stage != null) {
            Toast.showToast(stage, "Producto quitado del carrito", 2000, stage.getX() + stage.getWidth() / 2 - 150, 80);
        }
        listar();
    }

    @FXML
    private void calcularPT(){
        try {
            double cantidad = Double.parseDouble(cantidadPro.getText().trim());
            double precio = Double.parseDouble(punitPro.getText().trim());
            preTPro.setText(String.format(java.util.Locale.US, "%.2f", cantidad * precio));
            btnRegCarrito.setDisable(cantidad <= 0);
        } catch (Exception e) {
            // cantidad vacía, con letras, o todavía no se eligió producto
            preTPro.clear();
            btnRegCarrito.setDisable(true);
        }
    }


    @FXML
    private void registarPCarrito(){
        System.out.println("ID:"+SessionManager.getInstance().getUserId());
        Stage stMsg = StageManager.getPrimaryStage();
        if (dniRuc.getText() == null || dniRuc.getText().isBlank()) {
            Toast.showToast(stMsg, "Primero elige un cliente de la lista o búscalo por DNI/RUC", 2500, stMsg.getX() + stMsg.getWidth() / 2 - 150, 50);
            return;
        }
        if (codigoPro.getText() == null || codigoPro.getText().isBlank()) {
            Toast.showToast(stMsg, "Primero elige un producto de la lista", 2500, stMsg.getX() + stMsg.getWidth() / 2 - 150, 50);
            return;
        }
        // Control de stock: no se puede vender más de lo que hay
        try {
            double cantidad = Double.parseDouble(cantidadPro.getText());
            double stock = Double.parseDouble(stockPro.getText());
            if (cantidad > stock) {
                Stage st = StageManager.getPrimaryStage();
                Toast.showToast(st, "Stock insuficiente: solo hay " + stock + " unidades", 2500, st.getX() + st.getWidth() / 2 - 150, 50);
                return;
            }
        } catch (NumberFormatException ignored) {}
        try {
            VentCarrito ss= VentCarrito.builder()
                    .dniruc(dniRuc.getText())
                    .idProducto(ps.findById(Long.parseLong(codigoPro.getText())))
                    .nombreProducto(nombreProducto.getText())
                    .cantidad(Double.parseDouble(cantidadPro.getText()))
                    .punitario(Double.parseDouble(punitPro.getText()))
                    .ptotal(Double.parseDouble(preTPro.getText()))
                    .estado(1)
                    .idUsuario(daoU.findById(SessionManager.getInstance().getUserId()))
                    .tipoProducto(tipoProducto)
                    .build();
            daoC.save(ss);
            listar();
            Toast.showToast(stMsg, "Producto agregado al carrito", 1800, stMsg.getX() + stMsg.getWidth() / 2 - 150, 80);
        }catch (Exception e){
            // El error se muestra en una ventana para que se pueda leer con calma
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Coolbox");
            alert.setHeaderText("No se pudo agregar el producto al carrito");
            alert.setContentText(e.getMessage() != null ? e.getMessage() : e.toString());
            alert.showAndWait();
        }
    }

    @FXML
    public void registrarVenta(){
        Locale locale = new Locale("es", "es-PE");
        LocalDateTime localDate = LocalDateTime.now();
        //DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss", locale);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy", locale);
        String fechaFormateada = localDate.format(formatter);
        List<VentCarrito> dd = daoC.listaCarritoCliente(dniRuc.getText());
        if(dd.size()>0){
            Venta to=Venta.builder()
                    .dniruc(cs.findById(dniRuc.getText()))
                    .precioBase(Double.parseDouble(txtBaseImp.getText()))
                    .igv(Double.parseDouble(txtIgv.getText()))
                    .precioTotal(Double.parseDouble(txtImporteT.getText()))
                    .idUsuario(daoU.findById(SessionManager.getInstance().getUserId()))
                    .serie("V")
                    .tipoDoc("Boleta")
                    .fechaGener(LocalDate.parse(fechaFormateada, formatter))
                    .numDoc("00" )
                    .build();
            Venta idX = daoV.save(to);

            List<VentaDetalle> vdList = new ArrayList<>();
            if (idX.getId() != 0) {
                for (VentCarrito car : dd) {
                    Producto prodVenta = ps.findById(car.getIdProducto().getIdProducto());
                    // Ahorro por promoción: (precio anterior - precio de venta) * cantidad
                    double ahorro = (prodVenta.getPuold() != null && prodVenta.getPuold() > car.getPunitario())
                            ? (prodVenta.getPuold() - car.getPunitario()) * car.getCantidad() : 0.0;
                    VentaDetalle vd = VentaDetalle.builder()
                            .idVenta(idX)
                            .idProducto(prodVenta)
                            .cantidad(car.getCantidad())
                            .descuento(Math.round(ahorro * 100.0) / 100.0)
                            .pu(car.getPunitario())
                            .subtotal(car.getPtotal())
                            .build();
                    VentaDetalle vdTemp=daoVD.save(vd);
                    vdList.add(vdTemp);
                    // Control de stock: la mercadería que sale se resta del stock
                    ps.actualizarStock(car.getIdProducto().getIdProducto(), -car.getCantidad());
                }
            }
            daoC.deleteCarAll(dniRuc.getText());
            listar();
            daoC.deleteCarAll(dniRuc.getText());
            listar();
            limpiarFormCliente();
            autocompCliente.clear();
            autocompProducto.clear();
            nombreProducto.clear();
            codigoPro.clear();
            stockPro.clear();
            cantidadPro.clear();
            punitPro.clear();
            preTPro.clear();
            txtDescuento.setText("0.00");
            btnRegCarrito.setDisable(true);
            entries.clear();
            listarProducto();


            entries.clear();
            listarProducto(); // stock actualizado para la siguiente venta
            idX.setDetalleVenta(vdList);
            Comprobante comprobante = daoV.generarComprobante(idX);
            try {
                if(comprobante!=null && dd.size()>0) {
                    mostrarVisorComprobante(comprobante);
                    print(Long.parseLong(String.valueOf(idX.getId())));
                }else{
                    Stage stage = StageManager.getPrimaryStage();
                    double with=stage.getX() + stage.getWidth()/2 - 150;
                    Toast.showToast(stage, "Error al generar comprobante", 2000, with, 50);
                }
            }catch (Exception e){
                System.out.println("VER:"+e.getMessage());
            }

        }else{
            Stage stage = StageManager.getPrimaryStage();
            double with=stage.getX() + stage.getWidth()/2 - 150;
            Toast.showToast(stage, "No hay productos en el carrito", 2000, with, 50);
        }
        limpiarFormCliente();
    }

    public void mostrarVisorComprobante(Comprobante boleta) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/main_comprobante.fxml"));
            Parent visorRoot = loader.load();
            MainComprobanteController controller = loader.getController();
            controller.pasarDatos(boleta);
            Alert alert = new Alert(Alert.AlertType.NONE);
            alert.setTitle("Visualizar Comprobante");
            alert.setHeaderText(null);
            if (visorRoot instanceof javafx.scene.layout.Region) {
                ((javafx.scene.layout.Region) visorRoot).setPrefSize(800, 600);
            }
            StackPane stackPane = new StackPane(visorRoot);
            alert.getDialogPane().setContent(stackPane);
            ButtonType closeButton = new ButtonType("Cerrar");
            alert.getButtonTypes().add(closeButton);
            alert.setOnCloseRequest(event -> alert.close());
            alert.showAndWait().ifPresent(response -> {
                if (response == closeButton) {
                    alert.close();
                }
            });
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void print(Long idv){
        Venta vt=daoV.findById(idv);
        try {
            PrinterManager printerManager = PrinterManager.getInstance();
            PrintService printService = printerManager.getPrintService();
            PrinterOutputStream printerOutputStream = new PrinterOutputStream(printService);
            EscPos escpos = new EscPos(printerOutputStream);
            Style titleStyle = new Style()
                    .setJustification(EscPosConst.Justification.Center)
                    .setBold(true)
                    .setFontSize(Style.FontSize._2, Style.FontSize._2);
            Style normal = new Style()
                    .setJustification(EscPosConst.Justification.Left_Default);
            Style center = new Style()
                    .setJustification(EscPosConst.Justification.Center);
            escpos.writeLF(titleStyle, "BOLETA DE VENTA");
            escpos.writeLF(center, "Tienda Demo S.A.C.");
            escpos.writeLF(center, "RUC: 12345678901");
            escpos.writeLF(center, "Av. Principal 123 - Lima");
            escpos.writeLF(center, "--------------------------------");
//Datos del cliente
            escpos.writeLF(normal, "Cliente: "+vt.getDniruc().getNombres());
            escpos.writeLF(normal, "DNI: "+vt.getDniruc().getDniruc());
            escpos.writeLF(normal, "Fecha: "+vt.getFechaGener()+"");
            escpos.writeLF(normal, "--------------------------------");
//Detalle
            escpos.writeLF(normal, "Cant Descripción Importe");
            int x=1;
            for(VentaDetalle vd:vt.getDetalleVenta()) {
                String punit=x+" "+vd.getIdProducto().getNombre()+" S/ "+vd.getSubtotal()+"";
                escpos.writeLF(normal, punit);
            }
            escpos.writeLF(normal, "--------------------------------");
            escpos.writeLF(normal, "TOTAL: S/"+vt.getPrecioTotal()+"");
            escpos.writeLF(normal, "--------------------------------");
//Agregar QR con los datos principales
            String qrData = "Boleta N°001-000123 | Total: S/ "+vt.getPrecioTotal()+" | Fecha: "+vt.getFechaGener();
            QRCode qrCode = new QRCode()
                    .setJustification(EscPosConst.Justification.Center)
                    .setErrorCorrectionLevel(QRCode.QRErrorCorrectionLevel.QR_ECLEVEL_M_Default)
                    .setModel(QRCode.QRModel._1_Default);
            escpos.write(qrCode, qrData);
            escpos.feed(2);
            escpos.writeLF(center, "Gracias por su compra!");
            escpos.feed(6);
//Corte total
            escpos.cut(EscPos.CutMode.FULL);
            escpos.close();
            System.out.println("Boleta impresa correctamente.");
            System.out.println("Impresión completada correctamente.");
        } catch (IOException e) {
            System.err.println("Error al inicializar la impresora: " + e.getMessage());
        }
    }


}