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
import org.json.JSONObject;
import services.IGDBService;
import services.JuegoService;
import utils.GameCard;

import java.util.List;
import java.util.Optional;

public class MainController {

    @FXML
    private TextField searchField;
    @FXML
    private FlowPane resultsGrid;
    @FXML
    private Label messageLabel;

    private App app;
    private Usuario usuarioActual;
    private IGDBService igdbService;
    private JuegoService juegoService;

    public MainController() {
        this.igdbService = new IGDBService();
        this.juegoService = new JuegoService();
    }

    public void setApp(App app) {
        this.app = app;
    }

    public void setUsuarioActual(Usuario usuario) {
        this.usuarioActual = usuario;
    }

    @FXML
    private void handleSearch() {
        String busqueda = searchField.getText().trim();
        if (busqueda.isEmpty()) {
            messageLabel.setText("Introduce un nombre para buscar.");
            messageLabel.setVisible(true);
            return;
        }

        messageLabel.setText("Buscando...");
        messageLabel.setVisible(true);
        resultsGrid.getChildren().clear();

        new Thread(() -> {
            try {
                List<JSONObject> resultados = igdbService.buscarJuegos(busqueda);

                Platform.runLater(() -> {
                    messageLabel.setVisible(false);
                    if (resultados.isEmpty()) {
                        messageLabel.setText("No se encontraron resultados.");
                        messageLabel.setVisible(true);
                    } else {
                        procesarResultados(resultados);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    messageLabel.setText("Error en la conexión con el servicio.");
                    messageLabel.setVisible(true);
                });
            }
        }).start();
    }

    private void procesarResultados(List<JSONObject> resultados) {
        for (JSONObject juegoJson : resultados) {
            String id = String.valueOf(juegoJson.getInt("id"));
            String nombre = juegoJson.getString("name");
            Double rating = juegoJson.has("total_rating") ? juegoJson.getDouble("total_rating") : null;
            String portada = null;

            if (juegoJson.has("cover") && juegoJson.getJSONObject("cover").has("url")) {
                portada = juegoJson.getJSONObject("cover").getString("url").replace("//", "https://");
            }

            String releaseDateStr = null;
            if (juegoJson.has("first_release_date")) {
                long ts = juegoJson.getLong("first_release_date") * 1000L;
                releaseDateStr = new java.text.SimpleDateFormat("yyyy").format(new java.util.Date(ts));
            }
            String genreStr = "Unknown";
            if (juegoJson.has("genres")) {
                org.json.JSONArray gs = juegoJson.getJSONArray("genres");
                if (gs.length() > 0) genreStr = gs.getJSONObject(0).getString("name");
            }

            GameCard card = new GameCard(id, nombre, null, rating, "NO_JUGADO", portada, releaseDateStr, genreStr, 130.0, 180.0);
            card.setAlClic(() -> mostrarDialogoAñadirJuego(juegoJson));
            resultsGrid.getChildren().add(card);
        }
    }

    private void mostrarDialogoAñadirJuego(JSONObject juegoData) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Añadir Juego");
        dialog.setHeaderText("Añadir a mi lista: " + juegoData.getString("name"));

        TextField notaField = new TextField();
        notaField.setPromptText("Nota (0-100)");

        TextField comentarioField = new TextField();
        comentarioField.setPromptText("Comentario");

        ComboBox<String> favCombo = new ComboBox<>();
        favCombo.getItems().addAll("No", "Sí");
        favCombo.setValue("No");

        ComboBox<String> estadoCombo = new ComboBox<>();
        for (EstadoJuego estado : EstadoJuego.values()) {
            estadoCombo.getItems().add(estado.getDisplayName());
        }
        estadoCombo.setValue(EstadoJuego.NO_JUGADO.getDisplayName());

        VBox content = new VBox(10,
                new Label("Nota:"), notaField,
                new Label("Comentario:"), comentarioField,
                new Label("Favorito:"), favCombo,
                new Label("Estado:"), estadoCombo
        );
        content.setStyle("-fx-padding: 20;");

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            guardarNuevoJuego(juegoData, notaField.getText(), comentarioField.getText(),
                    favCombo.getValue(), estadoCombo.getValue());
        }
    }

    private void guardarNuevoJuego(JSONObject data, String nota, String com, String fav, String est) {
        try {
            Juego juego = new Juego(data.getString("name"), String.valueOf(data.getInt("id")));

            if (!nota.isEmpty()) {
                juego.setNotaPersonal(Double.parseDouble(nota));
            }
            juego.setComentario(com);
            juego.setEsFavorito(fav.equals("Sí"));

            for (EstadoJuego e : EstadoJuego.values()) {
                if (e.getDisplayName().equals(est)) {
                    juego.setEstado(e);
                    break;
                }
            }

            if (data.has("cover")) {
                String url = data.getJSONObject("cover").getString("url").replace("//", "https://");
                juego.setPortadaUrl(url);
            }

            juegoService.añadirJuego(usuarioActual, juego);
            messageLabel.setText("Juego añadido correctamente.");
            messageLabel.setVisible(true);

        } catch (Exception e) {
            messageLabel.setText("Error al guardar el juego.");
            messageLabel.setVisible(true);
        }
    }

    @FXML
    private void handleIrABiblioteca() {
        try {
            app.mostrarBiblioteca(usuarioActual);
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