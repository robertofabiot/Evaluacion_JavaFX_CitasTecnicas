package com.example.evaluacion_javafx_citastecnicas;

import com.example.evaluacion_javafx_citastecnicas.dao.CitaTecnicaDAO;
import com.example.evaluacion_javafx_citastecnicas.dao.ClienteDAO;
import com.example.evaluacion_javafx_citastecnicas.model.CitaTecnica;
import com.example.evaluacion_javafx_citastecnicas.model.Cliente;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

public class CitasController extends MenuController {
    private final CitaTecnicaDAO dao = new CitaTecnicaDAO();
    private final ClienteDAO clientesDao = new ClienteDAO();
    @FXML private CitaFormController citaFormController;
    @FXML private ComboBox<String> filtroEstado;
    @FXML private DatePicker filtroFecha;
    @FXML private Button filtrar, verDetalles, editar;
    @FXML private TableView<CitaTecnica> tabla;
    @FXML private TableColumn<CitaTecnica, Integer> idCol;
    @FXML private TableColumn<CitaTecnica, String> clienteCol, fechaCol, horaCol, servicioCol, estadoCol;
    @FXML private Label resumen;
    private boolean primeraCarga = true;
    private record Datos(List<Cliente> clientes, List<CitaTecnica> citas) { }

    @FXML private void initialize() {
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        clienteCol.setCellValueFactory(new PropertyValueFactory<>("clienteNombreCompleto"));
        fechaCol.setCellValueFactory(new PropertyValueFactory<>("fechaFormateada"));
        horaCol.setCellValueFactory(new PropertyValueFactory<>("horaFormateada"));
        servicioCol.setCellValueFactory(new PropertyValueFactory<>("tipoServicio"));
        estadoCol.setCellValueFactory(new PropertyValueFactory<>("estado"));
        filtroEstado.getItems().add("Todos");
        filtroEstado.getItems().addAll(CitaTecnica.ESTADOS_VALIDOS);
        filtroEstado.setValue("Todos");
        Ui.configurarFecha(filtroFecha);
        Ui.flujo(filtroEstado, filtroFecha, filtrar);
        Ui.configurarTabla(tabla, verDetalles, editar, this::detalles, this::editar, this::eliminar);
        citaFormController.configurar(null, this::guardar);
    }

    @Override public void cargar() { filtrar(); citaFormController.enfocar(); }

    @FXML private void filtrar() {
        final LocalDate fecha;
        try { fecha = Ui.leerFecha(filtroFecha); }
        catch (IllegalArgumentException e) { Ui.error(root, e.getMessage()); return; }
        String estado = filtroEstado.getValue();
        Ui.ejecutar(root, "Consultando citas en PostgreSQL...", () -> new Datos(clientesDao.listarTodos(), dao.filtrar(estado, fecha)), datos -> {
            citaFormController.clientes(datos.clientes());
            tabla.getItems().setAll(datos.citas());
            if (primeraCarga) { primeraCarga = false; citaFormController.enfocar(); }
            resumen.setText(datos.citas().size() + " cita(s) encontradas"
                    + (datos.clientes().isEmpty() ? " · Registre un cliente para agendar una cita." : ""));
        });
    }

    @FXML private void limpiarFiltros() {
        filtroEstado.setValue("Todos"); filtroFecha.setValue(null); filtroFecha.getEditor().clear(); filtrar();
    }

    private void guardar(CitaTecnica cita) {
        if (!Ui.confirmar(root, "¿Guardar la cita de " + cita.getClienteNombreCompleto()
                + " para el " + cita.getFechaFormateada() + " a las " + cita.getHoraFormateada() + "?")) return;
        Ui.ejecutar(root, "Guardando cita técnica...", () -> dao.insertar(cita), guardado -> {
            if (!guardado) { Ui.error(root, "No se guardó la cita. Intente de nuevo."); return; }
            Ui.informar(root, "Cita guardada correctamente.");
            citaFormController.limpiar();
            primeraCarga = true;
            limpiarFiltros();
        });
    }

    private void detalles(CitaTecnica c) {
        Ui.detalles(root, "Detalles de la cita #" + c.getId(),
                "ID: " + c.getId() + "\nCliente: " + c.getClienteNombreCompleto() + "\nID del cliente: " + c.getClienteId()
                        + "\nFecha: " + c.getFechaFormateada() + "\nHora: " + c.getHoraFormateada()
                        + "\nServicio: " + c.getTipoServicio() + "\nEstado: " + c.getEstado() + "\n\nDescripción:\n" + c.getDescripcion());
    }

    private void editar(CitaTecnica cita) {
        // Recarga los clientes antes de editar para evitar opciones desactualizadas.
        Ui.ejecutar(root, "Cargando clientes...", clientesDao::listarTodos, clientes -> abrirEditor(cita, clientes));
    }

    private void abrirEditor(CitaTecnica cita, List<Cliente> clientes) {
        try {
            EditorWindow<CitaFormController> ventana = new EditorWindow<>(Ui.ventana(root),
                    "cita-form.fxml", "Actualizar cita");
            ventana.controller.clientes(clientes);
            ventana.controller.configurar(cita, actualizada -> {
                if (!Ui.confirmar(ventana.root, "¿Actualizar los datos de esta cita?")) return;
                Ui.ejecutar(ventana.root, "Actualizando cita técnica...", () -> dao.actualizar(actualizada), guardado -> {
                    if (!guardado) { Ui.error(ventana.root, "La cita ya no existe. Actualice la lista."); return; }
                    Ui.informar(ventana.root, "Cita actualizada correctamente.");
                    ventana.cerrar();
                    filtrar();
                });
            });
            ventana.mostrar();
            ventana.controller.enfocar();
        } catch (IOException e) { Ui.error(root, "No se pudo abrir el formulario de edición."); }
    }

    private void eliminar(CitaTecnica cita) {
        if (!Ui.confirmar(root, "¿Eliminar definitivamente la cita #" + cita.getId()
                + " de " + cita.getClienteNombreCompleto() + "?")) return;
        Ui.ejecutar(root, "Eliminando cita técnica...", () -> dao.eliminar(cita.getId()), eliminado -> {
            if (eliminado) Ui.informar(root, "Cita eliminada correctamente.");
            else Ui.error(root, "La cita ya no existe. Se actualizará la lista.");
            filtrar();
        });
    }
}