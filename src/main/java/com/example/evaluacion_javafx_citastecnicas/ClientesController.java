package com.example.evaluacion_javafx_citastecnicas;

import com.example.evaluacion_javafx_citastecnicas.dao.ClienteDAO;
import com.example.evaluacion_javafx_citastecnicas.model.Cliente;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import java.io.IOException;

public class ClientesController extends MenuController {
    private final ClienteDAO dao = new ClienteDAO();
    @FXML private ClienteFormController clienteFormController;
    @FXML private TextField busqueda;
    @FXML private TableView<Cliente> tabla;
    @FXML private TableColumn<Cliente, Integer> idCol;
    @FXML private TableColumn<Cliente, String> nombreCol, telefonoCol, correoCol, activoCol;
    @FXML private Button verDetalles, editar;
    @FXML private Label resumen;
    private boolean primeraCarga = true;

    @FXML private void initialize() {
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        nombreCol.setCellValueFactory(new PropertyValueFactory<>("nombreCompleto"));
        telefonoCol.setCellValueFactory(new PropertyValueFactory<>("telefono"));
        correoCol.setCellValueFactory(new PropertyValueFactory<>("correo"));
        activoCol.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().isActivo() ? "Activo" : "Inactivo"));
        Ui.configurarTabla(tabla, verDetalles, editar, this::detalles, this::editar, this::eliminar);
        clienteFormController.configurar(null, this::guardar);
    }

    @Override public void cargar() { buscar(); clienteFormController.enfocar(); }

    @FXML private void buscar() {
        String criterio = busqueda.getText();
        Ui.ejecutar(root, "Consultando clientes...", () -> dao.buscar(criterio), clientes -> {
            tabla.getItems().setAll(clientes);
            if (primeraCarga) { primeraCarga = false; clienteFormController.enfocar(); }
            resumen.setText(clientes.size() + " cliente(s) encontrados");
        });
    }

    @FXML private void limpiarBusqueda() { busqueda.clear(); buscar(); }

    private void guardar(Cliente cliente) {
        if (!Ui.confirmar(root, "¿Guardar a " + cliente.getNombreCompleto() + " como nuevo cliente?")) return;
        Ui.ejecutar(root, "Guardando cliente...", () -> dao.insertar(cliente), guardado -> {
            if (!guardado) { Ui.error(root, "No se guardó el cliente. Intente de nuevo."); return; }
            Ui.informar(root, "Cliente guardado correctamente.");
            clienteFormController.limpiar();
            primeraCarga = true;
            limpiarBusqueda();
        });
    }

    private void detalles(Cliente c) {
        Ui.detalles(root, "Detalles del cliente #" + c.getId(),
                "ID: " + c.getId() + "\nNombres: " + c.getNombres() + "\nApellidos: " + c.getApellidos()
                        + "\nNombre completo: " + c.getNombreCompleto() + "\nTeléfono: " + c.getTelefono()
                        + "\nCorreo: " + c.getCorreo() + "\nEstado: " + (c.isActivo() ? "Activo" : "Inactivo"));
    }

    private void editar(Cliente cliente) {
        try {
            EditorWindow<ClienteFormController> ventana = new EditorWindow<>(Ui.ventana(root),
                    "cliente-form.fxml", "Actualizar cliente");
            ventana.controller.configurar(cliente, actualizado -> {
                if (!Ui.confirmar(ventana.root, "¿Actualizar los datos de este cliente?")) return;
                Ui.ejecutar(ventana.root, "Actualizando cliente...", () -> dao.actualizar(actualizado), guardado -> {
                    if (!guardado) { Ui.error(ventana.root, "El cliente ya no existe. Actualice la lista."); return; }
                    Ui.informar(ventana.root, "Cliente actualizado correctamente.");
                    ventana.cerrar();
                    buscar();
                });
            });
            ventana.mostrar();
            ventana.controller.enfocar();
        } catch (IOException e) { Ui.error(root, "No se pudo abrir el formulario de edición."); }
    }

    private void eliminar(Cliente cliente) {
        if (!Ui.confirmar(root, "¿Eliminar definitivamente al cliente " + cliente.getNombreCompleto() + "?")) return;
        Ui.ejecutar(root, "Eliminando cliente...", () -> dao.eliminar(cliente.getId()), eliminado -> {
            if (eliminado) Ui.informar(root, "Cliente eliminado correctamente.");
            else Ui.error(root, "El cliente ya no existe. Se actualizará la lista.");
            buscar();
        });
    }
}