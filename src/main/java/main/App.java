package main;

import config.FirebaseInit;
import controllers.LoginController;
import controllers.MainAppController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import models.Usuario;
import services.AuthRestService;
import java.io.IOException;
import java.util.prefs.Preferences;

public class App extends Application {

    private static final String CSS_PATH = "/css/main.css";
    private Stage primaryStage;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        try {
            FirebaseInit.inicializar();

            this.primaryStage = stage;
            stage.setTitle("Alexandria — Tu Biblioteca de Juegos");

            // Windowed fullscreen
            stage.setMaximized(true);
            stage.setMinWidth(1024);
            stage.setMinHeight(700);

            Preferences prefs = Preferences.userNodeForPackage(App.class);
            String savedEmail = prefs.get("alexandria_email", null);
            String savedPassword = prefs.get("alexandria_password", null);

            if (savedEmail != null && savedPassword != null) {
                // Sleek Splash Screen for professional loading
                javafx.scene.layout.StackPane splashRoot = new javafx.scene.layout.StackPane();
                splashRoot.setStyle("-fx-background-color: #0b1118;");
                
                javafx.scene.layout.VBox vbox = new javafx.scene.layout.VBox(25);
                vbox.setAlignment(javafx.geometry.Pos.CENTER);
                
                javafx.scene.control.Label logo = new javafx.scene.control.Label("ALEXANDRIA");
                logo.setStyle("-fx-text-fill: #e6e6e6; -fx-font-size: 42px; -fx-font-weight: bold; -fx-letter-spacing: 2px;");
                
                javafx.scene.control.ProgressIndicator spinner = new javafx.scene.control.ProgressIndicator();
                spinner.setMaxSize(40, 40);
                spinner.setStyle("-fx-progress-color: #4a90e2;"); // A nice blue indicator
                
                vbox.getChildren().addAll(logo, spinner);
                splashRoot.getChildren().add(vbox);
                
                Scene splashScene = new Scene(splashRoot, 1024, 700);
                stage.setScene(splashScene);
                stage.setMaximized(true);
                stage.show();

                // Background task to avoid locking UI
                new Thread(() -> {
                    try {
                        AuthRestService auth = new AuthRestService();
                        Usuario u = auth.login(savedEmail, savedPassword);
                        
                        // Preload initial games to mask the API loading time with the splash screen
                        services.IGDBService igdb = new services.IGDBService();
                        java.util.List<org.json.JSONObject> preloadGames = null;
                        java.util.List<models.Juego> preloadLibrary = null;
                        if (u != null) {
                            preloadGames = igdb.buscarJuegos("");
                            // Also pre-load the user's library so Biblioteca/Wishlist open instantly
                            try {
                                services.JuegoService js = new services.JuegoService();
                                preloadLibrary = js.obtenerJuegosLista(u);
                            } catch (Exception ignored) {}
                        }
                        
                        final java.util.List<org.json.JSONObject> fPreload = preloadGames;
                        final java.util.List<models.Juego> fLibrary = preloadLibrary;
                        
                        javafx.application.Platform.runLater(() -> {
                            try {
                                if (u != null) {
                                    mostrarMainWithPreloadedData(u, fPreload, fLibrary);
                                } else {
                                    mostrarLogin();
                                }
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        });
                    } catch (Exception ex) {
                        javafx.application.Platform.runLater(() -> {
                            try { mostrarLogin(); } catch (Exception ignore) {}
                        });
                    }
                }).start();
            } else {
                mostrarLogin();
                stage.show();
            }

        } catch (Exception e) {
            System.err.println("Error al iniciar la aplicación: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void mostrarLogin() throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
        javafx.scene.Parent root = loader.load();

        if (primaryStage.getScene() == null) {
            Scene scene = new Scene(root);
            applyCSS(scene);
            primaryStage.setScene(scene);
        } else {
            primaryStage.getScene().setRoot(root);
            applyCSS(primaryStage.getScene());
        }

        LoginController controller = loader.getController();
        controller.setApp(this);

        primaryStage.setMaximized(true);
    }

    public void mostrarMain(Usuario usuario) throws Exception {
        cargarEscenaPrincipal(usuario, "search", null, null);
    }
    
    public void mostrarMainWithPreloadedGames(Usuario usuario, java.util.List<org.json.JSONObject> preloadedGames) throws Exception {
        cargarEscenaPrincipal(usuario, "search", preloadedGames, null);
    }

    public void mostrarMainWithPreloadedData(Usuario usuario, java.util.List<org.json.JSONObject> preloadedGames, java.util.List<models.Juego> preloadedLibrary) throws Exception {
        cargarEscenaPrincipal(usuario, "search", preloadedGames, preloadedLibrary);
    }

    public void mostrarBiblioteca(Usuario usuario) throws Exception {
        cargarEscenaPrincipal(usuario, "library", null, null);
    }

    public void mostrarWishlist(Usuario usuario) throws Exception {
        cargarEscenaPrincipal(usuario, "wishlist", null, null);
    }

    public void mostrarEstadisticas(Usuario usuario) throws Exception {
        cargarEscenaPrincipal(usuario, "stats", null, null);
    }

    /**
     * All main views share a single FXML shell with a bottom navbar.
     * The tab parameter tells the controller which tab to activate.
     */
    private void cargarEscenaPrincipal(Usuario usuario, String tab, java.util.List<org.json.JSONObject> preloadedGames, java.util.List<models.Juego> preloadedLibrary) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/mainapp.fxml"));
        javafx.scene.Parent root = loader.load();

        if (primaryStage.getScene() == null) {
            Scene scene = new Scene(root);
            applyCSS(scene);
            primaryStage.setScene(scene);
        } else {
            primaryStage.getScene().setRoot(root);
            applyCSS(primaryStage.getScene());
        }

        MainAppController controller = loader.getController();
        controller.setApp(this);
        controller.setUsuarioActual(usuario);
        if (preloadedGames != null) {
            controller.setPreloadedSearchGames(preloadedGames);
        }
        if (preloadedLibrary != null) {
            controller.setPreloadedLibrary(preloadedLibrary);
        }
        controller.navegarA(tab);

        primaryStage.setMaximized(true);
    }

    private void applyCSS(Scene scene) {
        if (getClass().getResource(CSS_PATH) != null) {
            scene.getStylesheets().add(getClass().getResource(CSS_PATH).toExternalForm());
        }
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }
}