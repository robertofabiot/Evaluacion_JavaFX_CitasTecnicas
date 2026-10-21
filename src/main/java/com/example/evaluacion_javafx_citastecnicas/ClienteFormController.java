package com.example.evaluacion_javafx_citastecnicas;

import com.example.evaluacion_javafx_citastecnicas.dao.ClienteDAO;
import com.example.evaluacion_javafx_citastecnicas.model.Cliente;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import java.util.Locale;
import java.util.function.Consumer;

public class ClienteFormController {
    private final ClienteDAO dao = new ClienteDAO();

    @FXML private VBox formulario;
    @FXML private TextField nombres, apellidos, telefono, correo;
    @FXML private CheckBox activo;
    @FXML private Button guardar;
    @FXML private Label titulo;
    private int id;
    private Consumer<Cliente> alGuardar = cliente -> { };

    @FXML private void initialize() {
        Ui.flujo(nombres, apellidos, telefono, correo, activo, guardar);
    }

    public void configurar(Cliente cliente, Consumer<Cliente> accion) {
        alGuardar = accion;
        if (cliente != null) {
            id = cliente.getId();
            titulo.setText("Editar cliente #" + id);
            guardar.setText("Actualizar cliente");
            nombres.setText(cliente.getNombres());
            apellidos.setText(cliente.getApellidos());
            telefono.setText(cliente.getTelefono());
            correo.setText(cliente.getCorreo());
            activo.setSelected(cliente.isActivo());
        }
    }

    Cliente leer() {
        Cliente cliente = new Cliente(id, nombres.getText().trim(), apellidos.getText().trim(),
                telefono.getText().trim(), correo.getText().trim().toLowerCase(Locale.ROOT), activo.isSelected());
        dao.validarCliente(cliente);
        nombres.setText(cliente.getNombres());
        apellidos.setText(cliente.getApellidos());
        correo.setText(cliente.getCorreo());
        return cliente;
    }

    @FXML private void guardar() {
        try { alGuardar.accept(leer()); }
        catch (IllegalArgumentException e) { Ui.error(formulario, e.getMessage()); }
    }

    @FXML public void limpiar() {
        nombres.clear(); apellidos.clear(); telefono.clear(); correo.clear();
        activo.setSelected(true);
        enfocar();
    }

    public void enfocar() { Platform.runLater(nombres::requestFocus); }
}