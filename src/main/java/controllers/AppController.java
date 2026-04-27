package controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import main.App;
import models.EstadoJuego;
import models.Juego;
import models.Usuario;
import org.json.JSONObject;
import services.IGDBService;
import services.JuegoService;

import java.util.List;
import java.util.Optional;

public class AppController {

    @FXML
    private TextField searchField;
    @FXML
    private ListView<String> resultadosList;
    @FXML
    private Label messageLabel;

    private App app;
    private IGDBService igdbService;
    private JuegoService juegoService;
    private Usuario usuarioActual;
    private List<JSONObject> ultimosResultados;

    public AppController() {
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
            messageLabel.setText("Por favor, introduce un nombre.");
            return;
        }

        messageLabel.setText("Buscando juegos...");
        resultadosList.getItems().clear();

        new Thread(() -> {
            try {
                List<JSONObject> resultados = igdbService.buscarJuegos(busqueda);
                ultimosResultados = resultados;

                Platform.runLater(() -> {
                    if (resultados.isEmpty()) {
                        messageLabel.setText("No se han encontrado resultados.");
                    } else {
                        messageLabel.setText("Resultados encontrados: " + resultados.size());
                        for (JSONObject juego : resultados) {
                            String nombre = juego.getString("name");
                            String item = nombre;
                            if (juego.has("total_rating")) {
                                item += String.format(" [%.1f/100]", juego.getDouble("total_rating"));
                            }
                            resultadosList.getItems().add(item);
                        }
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> messageLabel.setText("Error en la búsqueda: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleMisJuegos() {
        if (usuarioActual == null) return;

        messageLabel.setText("Cargando biblioteca...");
        resultadosList.getItems().clear();

        new Thread(() -> {
            try {
                List<Juego> juegos = juegoService.obtenerJuegosLista(usuarioActual);

                Platform.runLater(() -> {
                    if (juegos.isEmpty()) {
                        messageLabel.setText("Tu biblioteca está vacía.");
                    } else {
                        messageLabel.setText("Tus juegos guardados:");
                        for (Juego j : juegos) {
                            String texto = j.getNombre() + " [" + j.getEstado().getDisplayName() + "]";
                            if (j.getNotaPersonal() != null) {
                                texto += " - Nota: " + (j.getNotaPersonal() / 10);
                            }
                            resultadosList.getItems().add(texto);
                        }
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> messageLabel.setText("Error al cargar datos: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleResultadoClick() {
        int index = resultadosList.getSelectionModel().getSelectedIndex();
        if (index >= 0 && ultimosResultados != null && index < ultimosResultados.size()) {
            mostrarDialogoAñadirJuego(ultimosResultados.get(index));
        }
    }

    private void mostrarDialogoAñadirJuego(JSONObject juegoData) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Gestionar Juego");
        dialog.setHeaderText("Añadir a mi lista: " + juegoData.getString("name"));

        TextField notaField = new TextField();
        notaField.setPromptText("Nota (0-100)");

        TextField comentarioField = new TextField();
        comentarioField.setPromptText("Escribe un comentario...");

        ComboBox<String> favCombo = new ComboBox<>();
        favCombo.getItems().addAll("No", "Sí");
        favCombo.setValue("No");

        ComboBox<String> estadoCombo = new ComboBox<>();
        for (EstadoJuego estado : EstadoJuego.values()) {
            estadoCombo.getItems().add(estado.getDisplayName());
        }
        estadoCombo.setValue(EstadoJuego.NO_JUGADO.getDisplayName());

        VBox layout = new VBox(10, new Label("Nota:"), notaField, new Label("Comentario:"),
                comentarioField, new Label("¿Favorito?"), favCombo,
                new Label("Estado:"), estadoCombo);

        dialog.getDialogPane().setContent(layout);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            guardarJuego(juegoData, notaField.getText(), comentarioField.getText(),
                    favCombo.getValue(), estadoCombo.getValue());
        }
    }

    private void guardarJuego(JSONObject data, String notaStr, String com, String fav, String est) {
        try {
            Juego juego = new Juego(data.getString("name"), String.valueOf(data.getInt("id")));

            if (!notaStr.isEmpty()) {
                juego.setNotaPersonal(Double.parseDouble(notaStr));
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
            messageLabel.setText("Juego guardado correctamente.");

        } catch (Exception e) {
            messageLabel.setText("Error al guardar: " + e.getMessage());
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