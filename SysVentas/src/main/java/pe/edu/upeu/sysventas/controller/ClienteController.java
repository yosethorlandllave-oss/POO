package pe.edu.upeu.sysventas.controller;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import pe.edu.upeu.sysventas.components.ColumnInfo;
import pe.edu.upeu.sysventas.components.TableViewHelper;
import pe.edu.upeu.sysventas.components.Toast;
import pe.edu.upeu.sysventas.components.ToltipCustom;
import pe.edu.upeu.sysventas.dto.ComboBoxOption;
import pe.edu.upeu.sysventas.enums.TipoDocumento;
import pe.edu.upeu.sysventas.model.Cliente;
import pe.edu.upeu.sysventas.service.IClienteService;

import java.util.LinkedHashMap;
import java.util.Set;
import java.util.function.Consumer;

@RequiredArgsConstructor
public class ClienteController {
    private final IClienteService cs;

    @FXML
    TextField txtDniRuc, txtNombres, txtRepLegal, txtDireccion, txtFiltroDato;
    @FXML
    ComboBox<ComboBoxOption> cbxTipoDocumento;
    @FXML
    private TableView<Cliente> tableView;
    @FXML
    Label lbnMsg;
    @FXML
    private AnchorPane miContenedor;

    private Validator validator;
    ObservableList<Cliente> listarCliente;
    // Guarda el DNI/RUC del cliente que se está editando (null = cliente nuevo)
    String dniRucEditando = null;

    private final ToltipCustom ttc = new ToltipCustom();

    @FXML
    public void initialize() {
        cbxTipoDocumento.getItems().addAll(cs.listarTipoDocumento());
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();

        TableViewHelper<Cliente> tableViewHelper = new TableViewHelper<>();
        LinkedHashMap<String, ColumnInfo> columns = new LinkedHashMap<>();
        columns.put("DNI/RUC", new ColumnInfo("dniruc", 110.0));
        columns.put("Tipo Doc.", new ColumnInfo("tipoDocumento", 90.0));
        columns.put("Nombres / Razón Social", new ColumnInfo("nombres", 220.0));
        columns.put("Rep. Legal", new ColumnInfo("repLegal", 150.0));
        columns.put("Dirección", new ColumnInfo("direccion", 220.0));

        Consumer<Cliente> updateAction = this::editForm;
        Consumer<Cliente> deleteAction = cliente -> {
            cs.delete(cliente.getDniruc());
            mostrarToast("Se eliminó correctamente!!");
            limpiarFormulario();
            listar();
        };
        tableViewHelper.addColumnsInOrderWithSize(tableView, columns, updateAction, deleteAction);
        tableView.setTableMenuButtonVisible(true);

        // Filtra la tabla mientras se escribe
        txtFiltroDato.textProperty().addListener((obs, o, n) -> filtrarClientes(n));
        listar();
    }

    public void listar() {
        try {
            tableView.getItems().clear();
            listarCliente = FXCollections.observableArrayList(cs.finAll());
            tableView.getItems().addAll(listarCliente);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void filtrarClientes(String texto) {
        if (texto == null || texto.isBlank()) {
            tableView.setItems(listarCliente);
            return;
        }
        String t = texto.toLowerCase();
        tableView.setItems(listarCliente.filtered(c ->
                c.getDniruc().toLowerCase().contains(t) ||
                c.getNombres().toLowerCase().contains(t)));
    }

    // Pasa los datos de la fila seleccionada al formulario
    private void editForm(Cliente cliente) {
        txtDniRuc.setText(cliente.getDniruc());
        txtDniRuc.setDisable(true); // el DNI/RUC es el ID: no se puede cambiar al editar
        txtNombres.setText(cliente.getNombres());
        txtRepLegal.setText(cliente.getRepLegal());
        txtDireccion.setText(cliente.getDireccion());
        cbxTipoDocumento.getSelectionModel().select(
                cbxTipoDocumento.getItems().stream()
                        .filter(o -> o.getKey().equals(cliente.getTipoDocumento().name()))
                        .findFirst().orElse(null));
        dniRucEditando = cliente.getDniruc();
        lbnMsg.setText("Editando cliente " + cliente.getDniruc());
    }

    @FXML
    public void guardar() {
        limpiarErrores();
        ComboBoxOption tipo = cbxTipoDocumento.getSelectionModel().getSelectedItem();
        Cliente formulario = Cliente.builder()
                .dniruc(txtDniRuc.getText().trim())
                .nombres(txtNombres.getText().trim())
                .tipoDocumento(tipo == null ? null : TipoDocumento.valueOf(tipo.getKey()))
                .repLegal(txtRepLegal.getText())
                .direccion(txtDireccion.getText())
                .build();

        // 1) Validar con las anotaciones del modelo (@NotBlank, @NotNull, @Pattern)
        Set<ConstraintViolation<Cliente>> errores = validator.validate(formulario);
        if (!errores.isEmpty()) {
            for (ConstraintViolation<Cliente> v : errores) {
                Control campo = switch (v.getPropertyPath().toString()) {
                    case "dniruc" -> txtDniRuc;
                    case "nombres" -> txtNombres;
                    case "tipoDocumento" -> cbxTipoDocumento;
                    default -> null;
                };
                if (campo != null) ttc.marcarError(campo, v.getMessage());
            }
            lbnMsg.setText("Revisa los campos marcados en rojo");
            return;
        }

        // 2) Guardar o actualizar
        if (dniRucEditando == null) {
            try {
                cs.findById(formulario.getDniruc());
                ttc.marcarError(txtDniRuc, "Ya existe un cliente con ese DNI/RUC");
                lbnMsg.setText("El DNI/RUC ya está registrado");
                return;
            } catch (Exception noExiste) {
                cs.save(formulario);
                mostrarToast("Cliente registrado correctamente!!");
            }
        } else {
            cs.update(dniRucEditando, formulario);
            mostrarToast("Cliente actualizado correctamente!!");
        }
        limpiarFormulario();
        listar();
    }

    @FXML
    public void cancelar() {
        limpiarFormulario();
    }

    private void limpiarFormulario() {
        txtDniRuc.clear();
        txtDniRuc.setDisable(false);
        txtNombres.clear();
        txtRepLegal.clear();
        txtDireccion.clear();
        cbxTipoDocumento.getSelectionModel().clearSelection();
        dniRucEditando = null;
        lbnMsg.setText("");
        limpiarErrores();
    }

    private void limpiarErrores() {
        ttc.limpiarCampo(txtDniRuc);
        ttc.limpiarCampo(txtNombres);
        ttc.limpiarCampo(cbxTipoDocumento);
    }

    private void mostrarToast(String mensaje) {
        Stage stage = (Stage) miContenedor.getScene().getWindow();
        double w = stage.getWidth() / 1.5, h = stage.getHeight() / 2;
        Toast.showToast(stage, mensaje, 2000, w, h);
    }
}