package controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import main.App;
import models.EstadoJuego;
import models.Juego;
import models.Usuario;
import services.JuegoService;
import utils.GameCard;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class BibliotecaController {

    @FXML
    private FlowPane gamesGrid;
    @FXML
    private Label contadorLabel;
    @FXML
    private ComboBox<String> filtroEstado;
    @FXML
    private Label messageLabel;

    private App app;
    private Usuario usuarioActual;
    private JuegoService juegoService;
    private List<Juego> todosLosJuegos;

    public BibliotecaController() {
        juegoService = new JuegoService();
    }

    public void setApp(App app) {
        this.app = app;
    }

    public void setUsuarioActual(Usuario usuario) {
        this.usuarioActual = usuario;
        configurarFiltros();
        cargarJuegos();
    }

    private void configurarFiltros() {
        filtroEstado.getItems().clear();
        filtroEstado.getItems().add("Todos");
        for (EstadoJuego estado : EstadoJuego.values()) {
            filtroEstado.getItems().add(estado.getDisplayName());
        }
        filtroEstado.setValue("Todos");
        filtroEstado.setOnAction(e -> aplicarFiltro());
    }

    private void cargarJuegos() {
        messageLabel.setText("Cargando biblioteca...");
        messageLabel.setVisible(true);
        gamesGrid.getChildren().clear();

        new Thread(() -> {
            try {
                todosLosJuegos = juegoService.obtenerJuegosLista(usuarioActual);

                Platform.runLater(() -> {
                    messageLabel.setVisible(false);
                    if (todosLosJuegos.isEmpty()) {
                        messageLabel.setText("No hay juegos en tu biblioteca.");
                        messageLabel.setVisible(true);
                        contadorLabel.setText("0 juegos");
                    } else {
                        contadorLabel.setText(todosLosJuegos.size() + " juegos");
                        mostrarJuegos(todosLosJuegos);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    messageLabel.setText("Error al cargar: " + e.getMessage());
                });
            }
        }).start();
    }

    private void aplicarFiltro() {
        String seleccion = filtroEstado.getValue();

        if (seleccion.equals("Todos")) {
            contadorLabel.setText(todosLosJuegos.size() + " juegos");
            mostrarJuegos(todosLosJuegos);
            return;
        }

        List<Juego> filtrados = todosLosJuegos.stream()
                .filter(j -> j.getEstado().getDisplayName().equals(seleccion))
                .collect(Collectors.toList());

        contadorLabel.setText(filtrados.size() + " / " + todosLosJuegos.size() + " juegos");
        mostrarJuegos(filtrados);
    }

    private void mostrarJuegos(List<Juego> juegos) {
        gamesGrid.getChildren().clear();
        for (Juego juego : juegos) {
            GameCard card = new GameCard(
                    juego.getIgdbId(),
                    juego.getNombre(),
                    juego.getNotaPersonal(),
                    juego.getNotaMediaIGDB(),
                    juego.getEstado().toString(),
                    juego.getPortadaUrl(),
                    juego.getAnyoLanzamiento() != null ? juego.getAnyoLanzamiento().toString() : null,
                    juego.getGenero(),
                    130.0, 180.0
            );

            card.setAlClic(() -> mostrarDetalleJuego(juego));
            gamesGrid.getChildren().add(card);
        }
    }

    private void mostrarDetalleJuego(Juego juego) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Detalles: " + juego.getNombre());

        TextField notaField = new TextField(juego.getNotaPersonal() != null ? juego.getNotaPersonal().toString() : "");
        TextField comentarioField = new TextField(juego.getComentario() != null ? juego.getComentario() : "");

        ComboBox<String> favCombo = new ComboBox<>();
        favCombo.getItems().addAll("No", "Sí");
        favCombo.setValue(juego.getEsFavorito() ? "Sí" : "No");

        ComboBox<String> estadoCombo = new ComboBox<>();
        for (EstadoJuego e : EstadoJuego.values()) {
            estadoCombo.getItems().add(e.getDisplayName());
        }
        estadoCombo.setValue(juego.getEstado().getDisplayName());

        VBox content = new VBox(10,
                new Label("Nota:"), notaField,
                new Label("Comentario:"), comentarioField,
                new Label("¿Favorito?"), favCombo,
                new Label("Estado:"), estadoCombo
        );
        content.setStyle("-fx-padding: 20;");

        dialog.getDialogPane().setContent(content);
        ButtonType btnActualizar = new ButtonType("Actualizar", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnEliminar = new ButtonType("Eliminar", ButtonBar.ButtonData.OTHER);
        dialog.getDialogPane().getButtonTypes().addAll(btnActualizar, btnEliminar, ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();

        if (result.isPresent()) {
            if (result.get() == btnActualizar) {
                actualizarJuegoDesdeDialogo(juego, notaField.getText(), comentarioField.getText(), favCombo.getValue(), estadoCombo.getValue());
            } else if (result.get() == btnEliminar) {
                eliminarJuego(juego);
            }
        }
    }

    private void actualizarJuegoDesdeDialogo(Juego juego, String nota, String com, String fav, String est) {
        try {
            juego.setNotaPersonal(nota.isEmpty() ? null : Double.parseDouble(nota));
            juego.setComentario(com);
            juego.setEsFavorito(fav.equals("Sí"));

            for (EstadoJuego e : EstadoJuego.values()) {
                if (e.getDisplayName().equals(est)) {
                    juego.setEstado(e);
                    break;
                }
            }

            juegoService.actualizarJuego(usuarioActual, juego);
            cargarJuegos();
            messageLabel.setText("Juego actualizado.");
        } catch (Exception e) {
            messageLabel.setText("Error al actualizar.");
        }
    }

    private void eliminarJuego(Juego juego) {
        try {
            juegoService.eliminarJuego(usuarioActual, juego.getIgdbId());
            cargarJuegos();
            messageLabel.setText("Juego eliminado.");
        } catch (Exception e) {
            messageLabel.setText("Error al eliminar.");
        }
    }

    @FXML
    private void handleIrABuscar() {
        try {
            app.mostrarMain(usuarioActual);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout() {
        try {
            app.mostrarLogin();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}