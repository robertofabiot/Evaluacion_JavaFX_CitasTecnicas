package com.example.evaluacion_javafx_citastecnicas;

import javafx.beans.binding.Bindings;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.StringConverter;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/** Utilidades compartidas: teclado, diálogos y JDBC fuera del hilo de JavaFX. */
public final class Ui {
    private Ui() { }
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);

    public static Window ventana(Node node) {
        return node.getScene() == null ? null : node.getScene().getWindow();
    }

    private static Alert alerta(Node node, Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alert = new Alert(tipo);
        if (ventana(node) != null) alert.initOwner(ventana(node));
        alert.setTitle(titulo);
        alert.setHeaderText(titulo);
        alert.setContentText(mensaje);
        alert.getDialogPane().setMinWidth(460);
        return alert;
    }

    public static void error(Node node, String mensaje) {
        alerta(node, Alert.AlertType.ERROR, "No se pudo completar la operación", mensaje).showAndWait();
    }

    public static void informar(Node node, String mensaje) {
        alerta(node, Alert.AlertType.INFORMATION, "Operación completada", mensaje).showAndWait();
    }

    public static boolean confirmar(Node node, String mensaje) {
        Alert alert = alerta(node, Alert.AlertType.CONFIRMATION, "Confirmar operación", mensaje);
        ButtonType aceptar = new ButtonType("Confirmar", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(aceptar, cancelar);
        ((Button) alert.getDialogPane().lookupButton(aceptar)).setDefaultButton(false);
        ((Button) alert.getDialogPane().lookupButton(cancelar)).setDefaultButton(true);
        return alert.showAndWait().orElse(cancelar) == aceptar;
    }

    public static void detalles(Node node, String titulo, String contenido) {
        Alert alert = alerta(node, Alert.AlertType.INFORMATION, titulo, "");
        TextArea texto = new TextArea(contenido);
        texto.setEditable(false);
        texto.setWrapText(true);
        texto.setPrefColumnCount(48);
        texto.setPrefRowCount(14);
        alert.getDialogPane().setContent(texto);
        alert.setResizable(true);
        alert.showAndWait();
    }

    public static <T> void ejecutar(Node root, Callable<T> trabajo, Consumer<T> terminado) {
        ejecutar(root, "Procesando en segundo plano...", trabajo, terminado);
    }

    public static <T> void ejecutar(Node root, String mensaje, Callable<T> trabajo, Consumer<T> terminado) {
        root.setDisable(true);
        Stage popup = crearPopupCarga(root, mensaje);
        if (popup != null) {
            popup.show();
        }

        Task<T> tarea = new Task<>() {
            @Override
            protected T call() throws Exception {
                long inicio = System.currentTimeMillis();
                T resultado = trabajo.call();
                // Breve pausa para asegurar que la animación del hilo sea fluida y visible
                long duracion = System.currentTimeMillis() - inicio;
                if (duracion < 450) {
                    try {
                        Thread.sleep(450 - duracion);
                    } catch (InterruptedException ignored) { }
                }
                return resultado;
            }
        };

        tarea.setOnSucceeded(e -> {
            if (popup != null) popup.close();
            root.setDisable(false);
            terminado.accept(tarea.getValue());
        });

        tarea.setOnFailed(e -> {
            if (popup != null) popup.close();
            root.setDisable(false);
            Throwable error = tarea.getException();
            if (error instanceof IllegalArgumentException) {
                error(root, error.getMessage());
            } else if (error instanceof SQLException sql) {
                String estado = sql.getSQLState();
                String msg = estado != null && estado.startsWith("08")
                        ? "No hay conexión con PostgreSQL. Revise su conexión de red y database.properties."
                        : "PostgreSQL no pudo completar la operación. Revise la conexión y las tablas de la base de datos.";
                error(root, msg + (estado == null ? "" : " Código SQL: " + estado));
            } else {
                error(root, "Ocurrió un error inesperado al procesar los datos.");
            }
        });

        Thread hilo = new Thread(tarea, "hilo-jdbc-" + (mensaje != null ? mensaje.replace(" ", "-") : "consulta"));
        hilo.setDaemon(true);
        hilo.start();
    }

    private static Stage crearPopupCarga(Node root, String mensaje) {
        try {
            Window ventanaPadre = ventana(root);
            Stage stage = new Stage();
            try {
                stage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
            } catch (Exception e) {
                try {
                    stage.initStyle(javafx.stage.StageStyle.UNDECORATED);
                } catch (Exception ignored) { }
            }

            if (ventanaPadre != null) {
                stage.initOwner(ventanaPadre);
                stage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            } else {
                stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            }

            ProgressIndicator spinner = new ProgressIndicator();
            spinner.setPrefSize(40, 40);

            Label lblTitulo = new Label(mensaje != null && !mensaje.isBlank() ? mensaje : "Procesando...");
            lblTitulo.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

            Label lblSub = new Label("Ejecutando operación JDBC en hilo secundario...");
            lblSub.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b; -fx-font-style: italic;");

            javafx.scene.layout.VBox layout = new javafx.scene.layout.VBox(10, spinner, lblTitulo, lblSub);
            layout.setAlignment(javafx.geometry.Pos.CENTER);
            layout.setPadding(new javafx.geometry.Insets(18, 28, 18, 28));
            layout.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 12; " +
                    "-fx-border-color: #3b82f6; -fx-border-width: 2; -fx-border-radius: 12; " +
                    "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.22), 14, 0, 0, 4);");

            javafx.scene.Scene scene = new javafx.scene.Scene(layout);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            stage.setScene(scene);
            stage.setResizable(false);

            if (ventanaPadre != null) {
                stage.setOnShown(ev -> {
                    stage.setX(ventanaPadre.getX() + (ventanaPadre.getWidth() - stage.getWidth()) / 2.0);
                    stage.setY(ventanaPadre.getY() + (ventanaPadre.getHeight() - stage.getHeight()) / 2.0);
                });
            }
            return stage;
        } catch (Exception e) {
            return null; // Si no hay entorno grafico en tests headless, continuar sin popup
        }
    }

    public static void configurarFecha(DatePicker picker) {
        picker.setPromptText("dd/mm/aaaa");
        picker.setConverter(new StringConverter<>() {
            @Override public String toString(LocalDate fecha) { return fecha == null ? "" : FECHA.format(fecha); }
            @Override public LocalDate fromString(String valor) {
                if (valor == null || valor.isBlank()) return null;
                try { return LocalDate.parse(valor.trim(), FECHA); }
                catch (RuntimeException e) { throw new IllegalArgumentException("Ingrese una fecha válida con formato dd/mm/aaaa."); }
            }
        });
    }

    public static LocalDate leerFecha(DatePicker picker) {
        LocalDate fecha = picker.getConverter().fromString(picker.getEditor().getText());
        picker.setValue(fecha);
        return fecha;
    }

    /** Enter avanza; Espacio cambia el CheckBox; Shift+Enter conserva saltos en TextArea. */
    public static void flujo(Node... controles) {
        for (int i = 0; i < controles.length - 1; i++) {
            Node actual = controles[i];
            Node siguiente = controles[i + 1];
            actual.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                if (event.getCode() != KeyCode.ENTER || event.isShiftDown()) return;
                // El desplegable debe poder confirmar primero su opción con el comportamiento nativo.
                if (actual instanceof ComboBoxBase<?> combo && combo.isShowing()) return;
                event.consume();
            });
            actual.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
                if (event.getCode() != KeyCode.ENTER || event.isShiftDown()) return;
                if (actual instanceof ComboBoxBase<?> combo && combo.isShowing()) return;
                // Avanzar al soltar evita que el siguiente control procese el mismo Enter.
                event.consume();
                avanzar(actual, siguiente);
            });
        }
    }

    private static void avanzar(Node actual, Node siguiente) {
        if (actual instanceof DatePicker picker) {
            try { leerFecha(picker); }
            catch (IllegalArgumentException e) { error(actual, e.getMessage()); picker.requestFocus(); return; }
        }
        siguiente.requestFocus();
    }

    public static <T> void configurarTabla(TableView<T> tabla, Button detalles, Button editar,
                                           Consumer<T> ver, Consumer<T> cambiar, Consumer<T> borrar) {
        detalles.disableProperty().bind(tabla.getSelectionModel().selectedItemProperty().isNull());
        editar.disableProperty().bind(tabla.getSelectionModel().selectedItemProperty().isNull());
        detalles.setOnAction(e -> ver.accept(tabla.getSelectionModel().getSelectedItem()));
        editar.setOnAction(e -> cambiar.accept(tabla.getSelectionModel().getSelectedItem()));
        tabla.setRowFactory(t -> {
            TableRow<T> fila = new TableRow<>();
            MenuItem verItem = new MenuItem("Ver detalles");
            MenuItem editarItem = new MenuItem("Editar");
            MenuItem borrarItem = new MenuItem("Eliminar");
            verItem.setOnAction(e -> ver.accept(fila.getItem()));
            editarItem.setOnAction(e -> cambiar.accept(fila.getItem()));
            borrarItem.setOnAction(e -> borrar.accept(fila.getItem()));
            ContextMenu menu = new ContextMenu(verItem, editarItem, new SeparatorMenuItem(), borrarItem);
            fila.contextMenuProperty().bind(Bindings.when(fila.emptyProperty())
                    .then((ContextMenu) null).otherwise(menu));
            fila.setOnContextMenuRequested(e -> {
                if (!fila.isEmpty()) tabla.getSelectionModel().select(fila.getItem());
            });
            fila.setOnMouseClicked(e -> {
                if (!fila.isEmpty() && e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2)
                    ver.accept(fila.getItem());
            });
            return fila;
        });
        tabla.setOnKeyPressed(e -> {
            T item = tabla.getSelectionModel().getSelectedItem();
            if (item != null && e.getCode() == KeyCode.ENTER) { ver.accept(item); e.consume(); }
            else if (item != null && e.getCode() == KeyCode.F2) { cambiar.accept(item); e.consume(); }
        });
    }
}