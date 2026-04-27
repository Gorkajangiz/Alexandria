package controllers;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Shape;
import javafx.util.Duration;
import main.App;
import models.EstadoJuego;
import models.Juego;
import models.Usuario;
import services.IGDBService;
import services.JuegoService;
import utils.GameCard;

public class MainAppController {

    // ── Root / Nav ──────────────────────────────────────────────
    @FXML private StackPane rootPane;

    // ── Search pane ──────────────────────────────────────────────
    @FXML private VBox      searchPane;
    @FXML private ScrollPane searchScrollPane;
    @FXML private TextField searchField;
    @FXML private FlowPane  searchResultsGrid;
    @FXML private Label     searchMessage;
    @FXML private CheckBox searchMoreOptionsToggle;
    @FXML private ToggleButton detailFavToggle;

    // ── Library pane ─────────────────────────────────────────────
    @FXML private VBox      libraryPane;
    @FXML private FlowPane  libraryGrid;
    @FXML private ScrollPane libraryScrollPane;
    @FXML private Label     libraryCounter;
    @FXML private Label     libraryMessage;
    @FXML private ComboBox<String> libraryFilter;

    // ── Wishlist pane ─────────────────────────────────────────────
    @FXML private VBox      wishlistPane;
    @FXML private FlowPane  wishlistGrid;
    @FXML private ScrollPane wishlistScrollPane;
    @FXML private Label     wishlistCounter;
    @FXML private Label     wishlistMessage;

    // ── Stats pane ─────────────────────────────────────────────
    @FXML private VBox      statsPane;
    @FXML private Label     statsTotalGames, statsCompletionLabel, statsAvgGrade, statsFavCount, statsNewGames, statsOldGames;
    @FXML private Label     statsMasterpieces, statsTotalComments;
    @FXML private ProgressBar statsCompletionBar, statsPlayingBar, statsWishlistBar;
    @FXML private FlowPane  statsTopGamesGrid;

    // ── Profile / Side Menu ──────────────────────────────────────
    @FXML private StackPane sideMenuOverlay;
    @FXML private VBox      sideMenu;
    @FXML private Label     profileName;
    @FXML private Label     profileEmail;
    @FXML private Label     profileStats;

    // ── Add Game overlay ─────────────────────────────────────────
    @FXML private StackPane addGameOverlay;
    @FXML private VBox      addGamePanel;
    @FXML private ImageView addFormCover;
    @FXML private Label     addFormMeta;
    @FXML private Label     addFormRating;
    @FXML private Label     addFormSummary;

    // Add-form fields (shown after picking a game)
    @FXML private VBox         addFormPanel;
    @FXML private Label        addFormTitle;
    @FXML private TextField    addNoteField;
    @FXML private TextField    detailNoteField;
    @FXML private TextArea addComment;
    @FXML private TextArea detailComment;
    @FXML private ToggleButton addFavToggle;
    @FXML private Button       addConfirmBtn;
    @FXML private Button       btnSearch;
    @FXML private Button       btnLibrary;
    @FXML private Button       btnWishlist;
    @FXML private Button       btnStats;
    @FXML private ComboBox<String> addStateFilter;
    @FXML private ComboBox<String> detailStateFilter;

    @FXML private StackPane gameDetailOverlay;
    @FXML private Label     detailTitle, detailMeta, detailRating, detailSummary;
    @FXML private ImageView detailCover;
    @FXML private FlowPane  detailGallery;

    // ── Library Detail overlay ────────────────────────────────
    @FXML private StackPane libraryDetailOverlay;
    @FXML private Label     libDetailTitle, libDetailMeta, libDetailRating, libDetailSummary;
    @FXML private ImageView libDetailCover;
    @FXML private FlowPane  libDetailGallery;
    @FXML private TextField        libDetailNoteField;
    @FXML private ComboBox<String> libDetailStateFilter;
    @FXML private TextArea         libDetailComment;
    @FXML private ToggleButton     libDetailFavToggle;

    // ── Settings (New) ───────────────────────────────────────────
    @FXML private StackPane settingsOverlay;
    @FXML private HBox settingsPanel;
    @FXML private Label     settingsTitle;
    @FXML private Button    btnSettingsGeneral;
    @FXML private CheckBox  checkShortcutWheel;

    // ── State ────────────────────────────────────────────────────
    private App app;
    private Usuario usuario;
    private IGDBService igdbService;
    private JuegoService juegoService;
    private List<JSONObject> lastSearchResults;
    private List<Juego>      allLibraryGames;
    private JSONObject       selectedGameJson;
    private Juego            editingJuego;
    private JSONObject       selectedGameDetails; // For detail view
    private Timer            searchDebounceTimer;
    private String           currentTab = "search";
    private int              currentAmountToShow = 54;
    private java.util.List<org.json.JSONObject> preloadedSearchGames = null;

    // ── Radial Wheel Interaction State ──────────────────────────
    private javafx.animation.Timeline wheelHoldTimer;
    private Runnable                  wheelSelectedAction;
    private javafx.scene.layout.Pane  activeWheelOverlay;
    private boolean                   isWheelActive = false;
    private javafx.scene.input.MouseEvent lastMousePressEvent;

    private static final double DEFAULT_CARD_W = 175.0;
    private static final double DEFAULT_CARD_H = 240.0;

    public MainAppController() {
        igdbService  = new IGDBService();
        juegoService = new JuegoService();
    }

    public void setPreloadedSearchGames(java.util.List<org.json.JSONObject> games) {
        this.preloadedSearchGames = games;
    }

    // ─────────────────────────────────────────────────────────────
    // Init
    // ─────────────────────────────────────────────────────────────
    public void setApp(App app)               { this.app = app; }
    public void setUsuarioActual(Usuario u)   {
        this.usuario = u;
    }

    public void setPreloadedLibrary(List<Juego> games) {
        this.allLibraryGames = games;
    }

    public void navegarA(String tab) {
        currentTab = tab;
        switch (tab) {
            case "library"  -> showLibrary();
            case "wishlist" -> showWishlist();
            case "stats"    -> showStats();
            default         -> showSearch();
        }
    }

    @FXML
    public void initialize() {
        // Live search in main search pane (debounced 400 ms)
        searchField.textProperty().addListener((obs, old, val) -> {
            if (searchDebounceTimer != null) searchDebounceTimer.cancel();

            String query = (val == null) ? "" : val.trim();

            if (query.isEmpty()) {
                doSearch("");
                return;
            }

            if (query.length() < 2) {
                searchMessage.setText("Escribe al menos 2 caracteres para buscar");
                searchMessage.setVisible(true);
                // Do not clear the grid yet, keep showing whatever was there to avoid flickering
                return;
            }

            searchMessage.setVisible(false);
            searchDebounceTimer = new Timer(true);
            searchDebounceTimer.schedule(new TimerTask() {
                @Override public void run() {
                    doSearch(query);
                }
            }, 400);
        });

        // Favorite toggle text sync
        addFavToggle.selectedProperty().addListener((o, ov, nv) ->
            addFavToggle.setText(nv ? "★  Favorito" : "☆  Marcar favorito"));

        // Initialize state filter in add form
        for (EstadoJuego e : EstadoJuego.values()) {
            addStateFilter.getItems().add(e.getDisplayName());
        }

        // Auto-set "Completado" if a grade is typed
        addNoteField.textProperty().addListener((obs, old, val) -> {
            if (val != null && !val.trim().isEmpty()) {
                addStateFilter.setValue(EstadoJuego.COMPLETADO.getDisplayName());
            }
        });

        // Auto-expanding text area
        addComment.textProperty().addListener((obs, ov, nv) -> {
            javafx.scene.text.Text text = new javafx.scene.text.Text(nv);
            text.setFont(addComment.getFont());
            text.setWrappingWidth(addComment.getWidth() > 30 ? addComment.getWidth() - 20 : 280);
            addComment.setPrefHeight(Math.max(80, text.getLayoutBounds().getHeight() + 30));
        });

        // Initialize state filter in detail form
        for (EstadoJuego e : EstadoJuego.values()) {
            libDetailStateFilter.getItems().add(e.getDisplayName());
        }

        // Auto-set "Completado" if a grade is typed
        libDetailNoteField.textProperty().addListener((obs, old, val) -> {
            if (val != null && !val.trim().isEmpty()) {
                libDetailStateFilter.setValue(EstadoJuego.COMPLETADO.getDisplayName());
            }
        });

        // Detail Favorite toggle text sync
        libDetailFavToggle.selectedProperty().addListener((o, ov, nv) ->
            libDetailFavToggle.setText(nv ? "★  Favorito" : "☆  Marcar favorito"));

        // Detail auto-expanding text area
        libDetailComment.textProperty().addListener((obs, ov, nv) -> {
            javafx.scene.text.Text text = new javafx.scene.text.Text(nv);
            text.setFont(libDetailComment.getFont());
            text.setWrappingWidth(libDetailComment.getWidth() > 30 ? libDetailComment.getWidth() - 20 : 280);
            libDetailComment.setPrefHeight(Math.max(80, text.getLayoutBounds().getHeight() + 30));
        });

        searchMoreOptionsToggle.selectedProperty().addListener((obs, old, val) -> {
            if (searchField.getText() != null && searchField.getText().trim().length() >= 2) {
                doSearch(searchField.getText().trim());
            }
        });



        addGameOverlay.setVisible(false);
        addFormPanel.setVisible(false);

        // Responsive grid resizing for default discovery feed
        if (searchScrollPane != null) {
            searchScrollPane.viewportBoundsProperty().addListener((obs, oldV, newV) -> {
                if ("search".equals(currentTab)) {
                    updateSearchGridResponsive();
                }
            });

            searchScrollPane.vvalueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.doubleValue() > 0.85 && lastSearchResults != null && currentAmountToShow < lastSearchResults.size()) {
                    currentAmountToShow += 27;
                    updateSearchGridResponsive();
                }
            });
        }
        
        // Responsive resizing for Library — mirrors updateSearchGridResponsive
        if (libraryScrollPane != null) {
            libraryScrollPane.viewportBoundsProperty().addListener((obs, oldV, newV) -> {
                if ("library".equals(currentTab) && allLibraryGames != null) {
                    renderLibraryGrid();
                }
            });
        }
        
        // Responsive resizing for Wishlist — mirrors updateSearchGridResponsive
        if (wishlistScrollPane != null) {
            wishlistScrollPane.viewportBoundsProperty().addListener((obs, oldV, newV) -> {
                if ("wishlist".equals(currentTab) && allLibraryGames != null) {
                    renderWishlistGrid();
                }
            });
        }
    }

    private void showSkeletons(javafx.scene.layout.FlowPane grid) {
        grid.getChildren().clear();

        // Calculate current responsive size for skeletons
        double width = searchScrollPane.getViewportBounds().getWidth();
        if (width <= 0) width = searchResultsGrid.getWidth();
        if (width <= 0) width = 1200; // Fallback

        double padding = 44.0;
        double hgap = 20.0;
        double usableWidth = (width - padding) - 2.0;
        double cardWidth = (usableWidth - (hgap * 8)) / 9; // 9 columns
        double cardHeight = cardWidth * 1.38;

        for (int i = 0; i < 18; i++) {
            javafx.scene.layout.VBox skeleton = new javafx.scene.layout.VBox();
            skeleton.setPrefSize(cardWidth, cardHeight + 40); // Card height + info room
            skeleton.setStyle("-fx-background-color: #1e2a3a; -fx-background-radius: 10;");
            skeleton.setOpacity(0.5);
            // ...

            javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(800), skeleton);
            ft.setFromValue(0.3);
            ft.setToValue(0.6);
            ft.setCycleCount(javafx.animation.Animation.INDEFINITE);
            ft.setAutoReverse(true);
            ft.play();

            grid.getChildren().add(skeleton);
        }
    }

    private void updateSearchGridResponsive() {
        if (lastSearchResults == null || lastSearchResults.isEmpty()) return;

        Platform.runLater(() -> {
            double width = searchScrollPane.getViewportBounds().getWidth();
            if (width <= 0) width = searchResultsGrid.getWidth();
            if (width <= 0) return;

            double padding = 44.0;
            double hgap = 20.0;
            double usableWidth = (width - padding) - 2.0;

            int gamesPerRow = 9;
            double cardWidth = (usableWidth - (hgap * (gamesPerRow - 1))) / gamesPerRow;
            double cardHeight = cardWidth * 1.38;
            int amountToShow = Math.min(currentAmountToShow, lastSearchResults.size());

            // Check for data synchronization: if the grid has cards, do they match the current results?
            boolean dataMismatch = false;
            if (!searchResultsGrid.getChildren().isEmpty()) {
                Node first = searchResultsGrid.getChildren().get(0);
                if (first instanceof GameCard) {
                    GameCard gc = (GameCard) first;
                    String firstResultId = String.valueOf(lastSearchResults.get(0).getInt("id"));
                    if (!gc.getJuegoId().equals(firstResultId)) {
                        dataMismatch = true;
                    }
                }
            }

            if (dataMismatch) {
                searchResultsGrid.getChildren().clear();
            }

            // Detect if we are currently showing skeletons (VBox instead of GameCard)
            int gridCount = searchResultsGrid.getChildren().size();
            if (gridCount > 0 && !(searchResultsGrid.getChildren().get(0) instanceof GameCard)) {
                searchResultsGrid.getChildren().clear();
                gridCount = 0;
            }

            // Update content and size of existing cards
            for (int i = 0; i < gridCount; i++) {
                Node node = searchResultsGrid.getChildren().get(i);
                if (i < amountToShow) {
                    JSONObject j = lastSearchResults.get(i);
                    if (node instanceof GameCard) {
                        GameCard gc = (GameCard) node;
                        // Update size
                        gc.setCardSize(cardWidth, cardHeight);
                        // Update content if it's a different game now
                        String jId = String.valueOf(j.getInt("id"));
                        if (!gc.getJuegoId().equals(jId)) {
                             // Re-initialize card content if ID changed
                             String releaseDateStr = null;
                             if (j.has("first_release_date")) {
                                 long ts = j.getLong("first_release_date") * 1000L;
                                 releaseDateStr = new java.text.SimpleDateFormat("yyyy").format(new java.util.Date(ts));
                             }
                             String genreStr = "Unknown";
                             if (j.has("genres")) {
                                 JSONArray gs = j.getJSONArray("genres");
                                 if (gs.length() > 0) genreStr = gs.getJSONObject(0).getString("name");
                             }
                             String portada = j.has("cover") ? j.getJSONObject("cover").getString("url") : null;
                             Double rating = j.has("total_rating") ? j.getDouble("total_rating") : null;

                             // Instead of recreating, we could add an update method to GameCard,
                             // but for now, let's just replace the node if it's different.
                             GameCard newCard = new GameCard(jId, j.getString("name"), null, rating, "NO_JUGADO", portada, releaseDateStr, genreStr, cardWidth, cardHeight);
                                                           newCard.setOnMousePressed(me -> handleCardPressed(j, newCard, me, false));
                              newCard.setOnMouseReleased(me -> handleCardReleased(j, newCard, me, false));

                             searchResultsGrid.getChildren().set(i, newCard);
                        }
                    }
                }
            }

            // Remove excess cards if results shrank
            if (gridCount > amountToShow) {
                searchResultsGrid.getChildren().remove(amountToShow, gridCount);
            }

            // Add new cards if results grew
            if (gridCount < amountToShow) {
                if (gridCount == 0) searchResultsGrid.getChildren().clear();
                for (int i = gridCount; i < amountToShow; i++) {
                    JSONObject j = lastSearchResults.get(i);
                    String releaseDateStr = null;
                    if (j.has("first_release_date")) {
                        long ts = j.getLong("first_release_date") * 1000L;
                        releaseDateStr = new java.text.SimpleDateFormat("yyyy").format(new java.util.Date(ts));
                    }
                    String genreStr = "Unknown";
                    if (j.has("genres")) {
                        JSONArray gs = j.getJSONArray("genres");
                        if (gs.length() > 0) genreStr = gs.getJSONObject(0).getString("name");
                    }
                    String portada = j.has("cover") ? j.getJSONObject("cover").getString("url") : null;
                    GameCard card = new GameCard(String.valueOf(j.getInt("id")), j.getString("name"),
                                                 null, j.has("total_rating") ? j.getDouble("total_rating") : null,
                                                 "NO_JUGADO", portada, releaseDateStr, genreStr, cardWidth, cardHeight);

                    card.setOnMousePressed(me -> handleCardPressed(j, card, me, false));
                    card.setOnMouseReleased(me -> handleCardReleased(j, card, me, false));

                    searchResultsGrid.getChildren().add(card);
                }
            }
        });
    }

    // ─────────────────────────────────────────────────────────────
    // Tab navigation
    // ─────────────────────────────────────────────────────────────
    private void hideAllPanes() {
        searchPane.setVisible(false);  searchPane.setManaged(false);
        libraryPane.setVisible(false); libraryPane.setManaged(false);
        wishlistPane.setVisible(false);wishlistPane.setManaged(false);
        statsPane.setVisible(false);    statsPane.setManaged(false);
        deactivateAllNavBtns();
    }

    private void deactivateAllNavBtns() {
        btnSearch.getStyleClass().remove("nav-active");
        btnLibrary.getStyleClass().remove("nav-active");
        btnWishlist.getStyleClass().remove("nav-active");
        btnStats.getStyleClass().remove("nav-active");
    }

    @FXML private void onNavStats()    { showStats(); }
    @FXML private void onNavSearch()   { showSearch(); }
    @FXML private void onNavLibrary()  { showLibrary(); }
    @FXML private void onNavWishlist() { showWishlist(); }
    @FXML private void handleLogout()  {
        try {
            Preferences prefs = Preferences.userNodeForPackage(main.App.class);
            prefs.remove("alexandria_email");
            prefs.remove("alexandria_password");
            app.mostrarLogin();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showSearch() {
        hideAllPanes();
        searchPane.setVisible(true); searchPane.setManaged(true);
        btnSearch.getStyleClass().add("nav-active");
        animatePane(searchPane);

        if (searchField.getText().isEmpty()) {
            searchMessage.setVisible(false);
            if (lastSearchResults != null && !lastSearchResults.isEmpty()) {
                updateSearchGridResponsive();
            } else if (preloadedSearchGames != null && !preloadedSearchGames.isEmpty()) {
                new Thread(() -> {
                    try {
                        if (allLibraryGames == null) allLibraryGames = juegoService.obtenerJuegosLista(usuario);
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (allLibraryGames == null) allLibraryGames = new java.util.ArrayList<>();
                    }
                    List<JSONObject> filtered = new java.util.ArrayList<>();
                    for (JSONObject j : preloadedSearchGames) {
                        String jId = String.valueOf(j.getInt("id"));
                        if (allLibraryGames.stream().noneMatch(g -> g.getIgdbId().equals(jId))) {
                            filtered.add(j);
                        }
                    }
                    lastSearchResults = filtered;
                    preloadedSearchGames = null;
                    Platform.runLater(() -> {
                        searchMessage.setVisible(false);
                        searchMoreOptionsToggle.setVisible(false);
                        updateSearchGridResponsive();
                    });
                }).start();
            } else {
                doSearch("");
            }
        }
    }

    private void showLibrary() {
        currentTab = "library";
        hideAllPanes();
        libraryPane.setVisible(true); libraryPane.setManaged(true);
        libraryPane.setOpacity(1.0);
        btnLibrary.getStyleClass().add("nav-active");
        animatePane(libraryPane);

        if (allLibraryGames == null) {
            cargarBibliotecaIncremental(false);
        } else {
            renderLibraryGrid(); // Sync call
            Platform.runLater(this::renderLibraryGrid); // Async fallback
        }
    }

    private void showWishlist() {
        currentTab = "wishlist";
        hideAllPanes();
        wishlistPane.setVisible(true); wishlistPane.setManaged(true);
        wishlistPane.setOpacity(1.0);
        btnWishlist.getStyleClass().add("nav-active");
        animatePane(wishlistPane);

        if (allLibraryGames == null) {
            cargarBibliotecaIncremental(true);
        } else {
            renderWishlistGrid(); // Sync call
            Platform.runLater(this::renderWishlistGrid); // Async fallback
        }
    }

    /** Mirrors updateSearchGridResponsive: reads live viewport width, bails if 0.
     *  The viewportBoundsProperty listener fires on first layout and retries. */
    private void renderLibraryGrid() {
        if (allLibraryGames == null) return;
        
        // Robust width detection: ScrollPane viewport -> Pane width -> fallback 0
        double width = (libraryScrollPane != null && libraryScrollPane.getViewportBounds() != null) 
                       ? libraryScrollPane.getViewportBounds().getWidth() : 0;
        if (width <= 0) width = libraryPane.getWidth();
        if (width <= 0) width = libraryGrid.getWidth();
        
        if (width <= 0) {
            // Still 0? The pane might not be laid out. Retry once.
            Platform.runLater(this::renderLibraryGrid);
            return;
        }

        String sel = libraryFilter != null ? libraryFilter.getValue() : null;
        List<Juego> base = allLibraryGames.stream()
            .filter(j -> j.getEstado() != EstadoJuego.EN_WISHLIST).collect(Collectors.toList());
        List<Juego> filtrados = (sel == null || "Todos".equals(sel)) ? base
            : base.stream().filter(j -> j.getEstado().getDisplayName().equals(sel)).collect(Collectors.toList());
        renderGrid(filtrados, libraryGrid, libraryCounter, libraryMessage, false, width);
        
        // Ensure the pane is actually visible. 
        // Sometimes the FadeTransition from animatePane can get stuck at 0 opacity on first load.
        libraryPane.setOpacity(1.0);
        libraryPane.setVisible(true);
    }

    private void renderWishlistGrid() {
        if (allLibraryGames == null) return;
        
        // Robust width detection
        double width = (wishlistScrollPane != null && wishlistScrollPane.getViewportBounds() != null)
                       ? wishlistScrollPane.getViewportBounds().getWidth() : 0;
        if (width <= 0) width = wishlistPane.getWidth();
        if (width <= 0) width = wishlistGrid.getWidth();
        
        if (width <= 0) {
            Platform.runLater(this::renderWishlistGrid);
            return;
        }

        List<Juego> wish = allLibraryGames.stream()
            .filter(j -> j.getEstado() == EstadoJuego.EN_WISHLIST)
            .collect(Collectors.toList());
        renderGrid(wish, wishlistGrid, wishlistCounter, wishlistMessage, true, width);
        
        // Ensure the pane is actually visible.
        wishlistPane.setOpacity(1.0);
        wishlistPane.setVisible(true);
    }

    /** Core grid renderer — called with a guaranteed non-zero width. */
    private void renderGrid(List<Juego> juegos, FlowPane grid, Label counter, Label msg, boolean isWishlist, double width) {
        grid.getChildren().clear();
        grid.setOpacity(1.0);
        grid.setVisible(true);

        if (juegos == null || juegos.isEmpty()) {
            msg.setText(isWishlist ? "Tu wishlist está vacía." : "Tu biblioteca está vacía.");
            msg.setVisible(true);
            counter.setText("0 juegos");
            grid.requestLayout();
            return;
        }
        msg.setVisible(false);
        counter.setText(juegos.size() + " juego" + (juegos.size() == 1 ? "" : "s"));

        double padding = 44.0;
        double hgap = 20.0;
        double usableWidth = (width - padding) - 2.0;
        int gamesPerRow = 9;
        double cardWidth = (usableWidth - (hgap * (gamesPerRow - 1))) / gamesPerRow;
        double cardHeight = cardWidth * 1.38;

        List<GameCard> cards = new ArrayList<>();
        for (Juego j : juegos) {
            String releaseStr = (j.getAnyoLanzamiento() != null) ? String.valueOf(j.getAnyoLanzamiento()) : null;
            GameCard card = new GameCard(
                j.getIgdbId(), j.getNombre(), j.getNotaPersonal(), j.getNotaMediaIGDB(),
                j.getEstado().toString(), j.getPortadaUrl(), releaseStr, j.getGenero(),
                cardWidth, cardHeight
            );
            card.setOnMousePressed(me -> handleCardPressed(null, card, me, true, j));
            card.setOnMouseReleased(me -> handleCardReleased(null, card, me, true, j));
            cards.add(card);
        }
        grid.getChildren().addAll(cards);
        
        // Force visual refresh by requesting layout on the entire tab pane
        grid.requestLayout();
        if (grid.getParent() != null) {
            grid.getParent().requestLayout();
            if (grid.getParent().getParent() != null) {
                grid.getParent().getParent().requestLayout();
            }
        }
    }

    private void showStats() {
        hideAllPanes();
        statsPane.setVisible(true); statsPane.setManaged(true);
        btnStats.getStyleClass().add("nav-active");
        animatePane(statsPane);
        loadStats();
    }

    private void loadStats() {
        new Thread(() -> {
            try {
                if (allLibraryGames == null) {
                    allLibraryGames = juegoService.obtenerJuegosLista(usuario);
                }

                final List<Juego> games = allLibraryGames;

                // Calculations
                long total = games.size();
                long completados = games.stream().filter(j -> j.getEstado() == EstadoJuego.COMPLETADO).count();
                long jugando = games.stream().filter(j -> j.getEstado() == EstadoJuego.JUGANDO).count();
                long wishlist = games.stream().filter(j -> j.getEstado() == EstadoJuego.EN_WISHLIST).count();
                long favoritos = games.stream().filter(j -> Boolean.TRUE.equals(j.getEsFavorito())).count();

                double avgGrade = games.stream()
                    .filter(j -> j.getNotaPersonal() != null)
                    .mapToDouble(Juego::getNotaPersonal)
                    .average().orElse(0.0);

                long newGames = games.stream()
                    .filter(j -> j.getAnyoLanzamiento() != null && j.getAnyoLanzamiento() >= 2018)
                    .count();
                long oldGames = games.stream()
                    .filter(j -> j.getAnyoLanzamiento() != null && j.getAnyoLanzamiento() < 2018)
                    .count();

                long masterpieces = games.stream()
                    .filter(j -> j.getNotaPersonal() != null && j.getNotaPersonal() >= 90)
                    .count();
                long totalComments = games.stream()
                    .filter(j -> j.getComentario() != null && !j.getComentario().trim().isEmpty())
                    .count();

                List<Juego> topGames = games.stream()
                    .filter(j -> j.getNotaPersonal() != null)
                    .sorted((j1, j2) -> Double.compare(j2.getNotaPersonal(), j1.getNotaPersonal()))
                    .limit(5)
                    .collect(Collectors.toList());

                Platform.runLater(() -> {
                    statsTotalGames.setText(String.valueOf(total));

                    double completionProgress = total > 0 ? (double) completados / total : 0;
                    statsCompletionBar.setProgress(completionProgress);
                    statsCompletionLabel.setText(String.format("%d%%", (int)(completionProgress * 100)));

                    statsAvgGrade.setText(String.format("%.1f", avgGrade));
                    statsFavCount.setText(String.valueOf(favoritos));

                    statsNewGames.setText(String.valueOf(newGames));
                    statsOldGames.setText(String.valueOf(oldGames));

                    if (statsMasterpieces != null) statsMasterpieces.setText(String.valueOf(masterpieces));
                    if (statsTotalComments != null) statsTotalComments.setText(String.valueOf(totalComments));

                    statsPlayingBar.setProgress(total > 0 ? (double) jugando / total : 0);
                    statsWishlistBar.setProgress(total > 0 ? (double) wishlist / total : 0);

                    // Update Top Games Grid
                    statsTopGamesGrid.getChildren().clear();
                    for (Juego j : topGames) {
                        GameCard card = new GameCard(j.getIgdbId(), j.getNombre(),
                            j.getNotaPersonal(), j.getNotaMediaIGDB(), j.getEstado().toString(), j.getPortadaUrl(), null, null, 110, 150);
                        card.setOnMousePressed(me -> handleCardPressed(null, card, me, true, j));
                        card.setOnMouseReleased(me -> handleCardReleased(null, card, me, true, j));
                        statsTopGamesGrid.getChildren().add(card);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    private void toggleSideMenu() {
        if (sideMenuOverlay.isVisible()) {
            // Close
            TranslateTransition tt = new TranslateTransition(Duration.millis(250), sideMenu);
            tt.setToX(-320);
            FadeTransition ft = new FadeTransition(Duration.millis(250), sideMenuOverlay);
            ft.setFromValue(1); ft.setToValue(0);
            ft.setOnFinished(e -> sideMenuOverlay.setVisible(false));
            tt.play(); ft.play();
        } else {
            // Open
            loadProfile();
            sideMenuOverlay.setVisible(true);
            sideMenuOverlay.setOpacity(0);
            TranslateTransition tt = new TranslateTransition(Duration.millis(250), sideMenu);
            tt.setFromX(-320); tt.setToX(0);
            FadeTransition ft = new FadeTransition(Duration.millis(250), sideMenuOverlay);
            ft.setFromValue(0); ft.setToValue(1);
            tt.play(); ft.play();
        }
    }

    private void animatePane(javafx.scene.Node pane) {
        if (pane == null) return;
        // Reset opacity to ensure visibility if animation fails
        pane.setOpacity(1.0); 
        
        FadeTransition ft = new FadeTransition(Duration.millis(250), pane);
        ft.setFromValue(0.1); // Start from 0.1 instead of 0 to avoid "interactive but invisible" state
        ft.setToValue(1.0);
        ft.play();
    }

    // ─────────────────────────────────────────────────────────────
    // Search
    // ─────────────────────────────────────────────────────────────
    private void doSearch(String query) {
        lastSearchResults = null; // Clear stale results immediately to prevent flickering
        Platform.runLater(() -> {
            searchResultsGrid.getChildren().clear(); // Reset grid on new search
            currentAmountToShow = 54; // Reset lazy loading
            showSkeletons(searchResultsGrid);
            searchMessage.setVisible(false);
        });
        new Thread(() -> {
            boolean soloCalificados = !searchMoreOptionsToggle.isSelected();
            List<JSONObject> results = igdbService.buscarJuegos(query, soloCalificados);
            Platform.runLater(() -> {
                if (results.isEmpty()) {
                    searchResultsGrid.getChildren().clear();
                    searchMessage.setText("No se encontraron juegos.");
                    searchMessage.setVisible(true);
                } else {
                    searchMessage.setVisible(false);
                    searchMoreOptionsToggle.setVisible(true);

                    // Filter out games already in library
                    try {
                        if (allLibraryGames == null) allLibraryGames = juegoService.obtenerJuegosLista(usuario);
                    } catch (Exception ignored) {}

                    List<JSONObject> filtered = new ArrayList<>();
                    for (JSONObject j : results) {
                        String jId = String.valueOf(j.getInt("id"));
                        if (allLibraryGames == null || allLibraryGames.stream().noneMatch(g -> g.getIgdbId().equals(jId))) {
                            filtered.add(j);
                        }
                    }

                    lastSearchResults = filtered;
                    updateSearchGridResponsive();
                }
            });
        }).start();
    }

    // ─────────────────────────────────────────────────────────────
    // Library
    // ─────────────────────────────────────────────────────────────
    private void cargarBibliotecaIncremental(boolean soloWishlist) {
        libraryMessage.setVisible(true);
        libraryMessage.setText("Sincronizando biblioteca...");

        new Thread(() -> {
            try {
                if (allLibraryGames == null) {
                    allLibraryGames = juegoService.obtenerJuegosLista(usuario);
                }

                // Render immediately after load — viewport is laid out since tab was just shown
                Platform.runLater(() -> {
                    configurarFiltros();
                    if (soloWishlist) {
                        renderWishlistGrid();
                    } else {
                        renderLibraryGrid();
                    }
                });

                // Enrich metadata in background, then re-render
                List<String> missingIds = allLibraryGames.stream()
                    .filter(j -> j.getNotaMediaIGDB() == null || j.getGenero() == null)
                    .map(Juego::getIgdbId)
                    .collect(Collectors.toList());

                if (!missingIds.isEmpty()) {
                    List<JSONObject> meta = igdbService.getGamesMetadata(missingIds);
                    for (JSONObject m : meta) {
                        String mId = String.valueOf(m.getInt("id"));
                        allLibraryGames.stream().filter(j -> j.getIgdbId().equals(mId)).findFirst().ifPresent(j -> {
                            if (m.has("total_rating")) j.setNotaMediaIGDB(m.getDouble("total_rating"));
                            if (m.has("genres")) {
                                JSONArray gs = m.getJSONArray("genres");
                                if (gs.length() > 0) j.setGenero(gs.getJSONObject(0).getString("name"));
                            }
                            if (m.has("first_release_date")) {
                                long ts = m.getLong("first_release_date") * 1000L;
                                j.setAnyoLanzamiento(Integer.parseInt(new java.text.SimpleDateFormat("yyyy").format(new java.util.Date(ts))));
                            }
                        });
                    }
                    Platform.runLater(() -> {
                        if (soloWishlist) renderWishlistGrid();
                        else renderLibraryGrid();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void cargarWishlist() {
        wishlistGrid.getChildren().clear();
        wishlistMessage.setVisible(true);
        wishlistMessage.setText("Cargando...");

        new Thread(() -> {
            try {
                List<Juego> todos = juegoService.obtenerJuegosLista(usuario);
                List<Juego> wish = todos.stream()
                    .filter(j -> j.getEstado() == EstadoJuego.EN_WISHLIST)
                    .collect(Collectors.toList());

                // Render immediately with what we have
                Platform.runLater(() ->
                    mostrarJuegosEnGrid(wish, wishlistGrid, wishlistCounter, wishlistMessage, true));

                // Fetch missing metadata
                List<String> missingIds = wish.stream()
                    .filter(j -> j.getNotaMediaIGDB() == null || j.getGenero() == null)
                    .map(Juego::getIgdbId)
                    .collect(Collectors.toList());

                if (!missingIds.isEmpty()) {
                    List<JSONObject> meta = igdbService.getGamesMetadata(missingIds);
                    for (JSONObject m : meta) {
                        String mId = String.valueOf(m.getInt("id"));
                        wish.stream().filter(j -> j.getIgdbId().equals(mId)).findFirst().ifPresent(j -> {
                            if (m.has("total_rating")) j.setNotaMediaIGDB(m.getDouble("total_rating"));
                            if (m.has("genres")) {
                                JSONArray gs = m.getJSONArray("genres");
                                if (gs.length() > 0) j.setGenero(gs.getJSONObject(0).getString("name"));
                            }
                            if (m.has("first_release_date")) {
                                long ts = m.getLong("first_release_date") * 1000L;
                                j.setAnyoLanzamiento(Integer.parseInt(new java.text.SimpleDateFormat("yyyy").format(new java.util.Date(ts))));
                            }
                        });
                    }
                    // Re-render after enrichment
                    Platform.runLater(() ->
                        mostrarJuegosEnGrid(wish, wishlistGrid, wishlistCounter, wishlistMessage, true));
                }
            } catch (Exception e) {
                Platform.runLater(() -> wishlistMessage.setText("Error al cargar."));
                e.printStackTrace();
            }
        }).start();
    }

    private void mostrarJuegosEnGrid(List<Juego> juegos, FlowPane grid,
                                     Label counter, Label msg, boolean isWishlist) {
        grid.getChildren().clear();
        if (juegos.isEmpty()) {
            msg.setText(isWishlist ? "Tu wishlist está vacía." : "Tu biblioteca está vacía.");
            msg.setVisible(true);
            counter.setText("0 juegos");
            return;
        }
        msg.setVisible(false);
        counter.setText(juegos.size() + " juego" + (juegos.size() == 1 ? "" : "s"));

        // Use exact same width logic as the search grid
        ScrollPane scrollPane = isWishlist ? wishlistScrollPane : libraryScrollPane;
        double width = scrollPane != null ? scrollPane.getViewportBounds().getWidth() : 0;
        if (width <= 0) width = grid.getWidth();
        if (width <= 0) width = 1200; // Fallback

        // Match the search grid calculation exactly: padding=44, hgap=20
        double padding = 44.0;
        double hgap = 20.0;
        double usableWidth = (width - padding) - 2.0;
        int gamesPerRow = 9;
        double cardWidth = (usableWidth - (hgap * (gamesPerRow - 1))) / gamesPerRow;
        double cardHeight = cardWidth * 1.38;

        for (Juego j : juegos) {
            String releaseStr = (j.getAnyoLanzamiento() != null) ? String.valueOf(j.getAnyoLanzamiento()) : null;
            String genreStr = j.getGenero();

            GameCard card = new GameCard(
                j.getIgdbId(),
                j.getNombre(),
                j.getNotaPersonal(),
                j.getNotaMediaIGDB(),
                j.getEstado().toString(),
                j.getPortadaUrl(),
                releaseStr,
                genreStr,
                cardWidth, cardHeight
            );
            card.setCardSize(cardWidth, cardHeight);

            card.setOnMousePressed(me -> handleCardPressed(null, card, me, true, j));
            card.setOnMouseReleased(me -> handleCardReleased(null, card, me, true, j));

            grid.getChildren().add(card);
        }
    }

    private void configurarFiltros() {
        if (libraryFilter.getItems().isEmpty()) {
            libraryFilter.getItems().add("Todos");
            for (EstadoJuego e : EstadoJuego.values()) {
                if (e != EstadoJuego.EN_WISHLIST) libraryFilter.getItems().add(e.getDisplayName());
            }
            libraryFilter.setValue("Todos");
            libraryFilter.setOnAction(e -> aplicarFiltroLibrary());
        } else {
            // Already configured, just refresh selection
            libraryFilter.setValue("Todos");
        }
    }

    @FXML private void aplicarFiltroLibrary() {
        if (allLibraryGames == null) return;
        renderLibraryGrid();
    }

    // ─────────────────────────────────────────────────────────────
    // Profile
    // ─────────────────────────────────────────────────────────────
    private void loadProfile() {
        if (usuario == null) return;
        profileName.setText(usuario.getNombre() != null ? usuario.getNombre() : "Usuario");
        profileEmail.setText(usuario.getEmail() != null ? usuario.getEmail() : "");

        new Thread(() -> {
            try {
                List<Juego> todos = juegoService.obtenerJuegosLista(usuario);
                long completados = todos.stream().filter(j -> j.getEstado() == EstadoJuego.COMPLETADO).count();
                long wishlist    = todos.stream().filter(j -> j.getEstado() == EstadoJuego.EN_WISHLIST).count();
                long favoritos   = todos.stream().filter(j -> Boolean.TRUE.equals(j.getEsFavorito())).count();
                Platform.runLater(() ->
                    profileStats.setText(String.format(
                        "%d juegos en total  |  %d completados  |  %d favoritos  |  %d en wishlist",
                        todos.size(), completados, favoritos, wishlist))
                );

            } catch (Exception ignored) {}
        }).start();
    }

    // ─────────────────────────────────────────────────────────────
    // Add Game Overlay
    // ─────────────────────────────────────────────────────────────
    @FXML private void closeAddGameOverlay() {
        FadeTransition ft = new FadeTransition(Duration.millis(150), addGameOverlay);
        ft.setFromValue(1); ft.setToValue(0);
        ft.setOnFinished(e -> addGameOverlay.setVisible(false));
        ft.play();
    }

    private void abrirAddFormConJuego(JSONObject juegoJson) {
        selectedGameJson = juegoJson;

        if (!addGameOverlay.isVisible()) {
            addGameOverlay.setVisible(true);
            FadeTransition ftO = new FadeTransition(Duration.millis(180), addGameOverlay);
            ftO.setFromValue(0); ftO.setToValue(1); ftO.play();
            ScaleTransition stO = new ScaleTransition(Duration.millis(180), addGamePanel);
            stO.setFromX(0.93); stO.setFromY(0.93);
            stO.setToX(1.0);    stO.setToY(1.0); stO.play();
        }

        // Set basic info immediately
        addFormTitle.setText(juegoJson.getString("name"));
        addFormSummary.setText("Cargando resumen...");

        String releaseYear = "N/A";
        if (juegoJson.has("first_release_date")) {
            long ts = juegoJson.getLong("first_release_date") * 1000L;
            releaseYear = new java.text.SimpleDateFormat("yyyy").format(new java.util.Date(ts));
        }
        String genre = "N/A";
        if (juegoJson.has("genres")) {
            org.json.JSONArray gs = juegoJson.getJSONArray("genres");
            if (gs.length() > 0) genre = gs.getJSONObject(0).getString("name");
        }
        addFormMeta.setText(releaseYear + " • " + genre);

        if (juegoJson.has("total_rating")) {
            addFormRating.setText(String.format("%.1f ★", juegoJson.getDouble("total_rating") / 10.0));
        } else {
            addFormRating.setText("S/N ★");
        }

        String coverUrl = getPortadaUrl(juegoJson);
        if (coverUrl != null && !coverUrl.isEmpty()) {
            addFormCover.setImage(new javafx.scene.image.Image(coverUrl, true));
        } else {
            addFormCover.setImage(null);
        }

        // Fetch detailed info (summary) in background
        new Thread(() -> {
            JSONObject details = igdbService.getGameDetails(juegoJson.getInt("id"));
            if (details != null && details.has("summary")) {
                String summary = details.getString("summary");
                Platform.runLater(() -> addFormSummary.setText(summary));
            } else if (juegoJson.has("summary")) {
                // Fallback to what we already had
                Platform.runLater(() -> addFormSummary.setText(juegoJson.getString("summary")));
            } else {
                Platform.runLater(() -> addFormSummary.setText("No hay resumen disponible para este juego."));
            }
        }).start();

        // Reset form
        addFavToggle.setSelected(false);
        addFavToggle.setText("☆  Marcar favorito");
        addConfirmBtn.setText("Añadir a mi colección");

        addFormPanel.setVisible(true);
        FadeTransition ft = new FadeTransition(Duration.millis(180), addFormPanel);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
    }
    private void abrirFormEdicion(Juego j) {
        editingJuego = j;
        selectedGameJson = null;

        if (!addGameOverlay.isVisible()) {
            addGameOverlay.setVisible(true);
            FadeTransition ftO = new FadeTransition(Duration.millis(180), addGameOverlay);
            ftO.setFromValue(0); ftO.setToValue(1); ftO.play();
            ScaleTransition stO = new ScaleTransition(Duration.millis(180), addGamePanel);
            stO.setFromX(0.93); stO.setFromY(0.93);
            stO.setToX(1.0);    stO.setToY(1.0); stO.play();
        }

        addFormTitle.setText(j.getNombre());
        addNoteField.setText(j.getNotaPersonal() != null ? String.valueOf(j.getNotaPersonal()) : "");
        addComment.setText(j.getComentario() != null ? j.getComentario() : "");
        addFavToggle.setSelected(Boolean.TRUE.equals(j.getEsFavorito()));
        addConfirmBtn.setText("Guardar cambios");
        addStateFilter.setValue(j.getEstado().getDisplayName());

        // Info Panel (Left Side)
        String year = j.getAnyoLanzamiento() != null ? String.valueOf(j.getAnyoLanzamiento()) : "N/A";
        String genre = j.getGenero() != null ? j.getGenero() : "N/A";
        addFormMeta.setText(year + "  |  " + genre);
        addFormRating.setText(j.getNotaMediaIGDB() != null ? String.format("%.1f ★", j.getNotaMediaIGDB() / 10.0) : "N/A");

        if (j.getPortadaUrl() != null) {
            addFormCover.setImage(new Image(j.getPortadaUrl(), true));
        }

        // Fetch summary from IGDB if needed
        new Thread(() -> {
            try {
                JSONObject details = igdbService.getGameDetails(Integer.parseInt(j.getIgdbId()));
                if (details != null && details.has("summary")) {
                    String summary = details.getString("summary");
                    Platform.runLater(() -> addFormSummary.setText(summary));
                }
            } catch (Exception ignored) {}
        }).start();

        addFormPanel.setVisible(true);
        FadeTransition ft = new FadeTransition(Duration.millis(180), addFormPanel);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
    }

    @FXML private void confirmarAddJuego() {
        if (selectedGameJson == null && editingJuego == null) return;
        try {
            Juego juego;
            if (editingJuego != null) {
                juego = editingJuego;
            } else {
                juego = new Juego(selectedGameJson.getString("name"),
                        String.valueOf(selectedGameJson.getInt("id")));

                if (selectedGameJson.has("cover")) {
                    String url = selectedGameJson.getJSONObject("cover").getString("url");
                    juego.setPortadaUrl(IGDBService.upgradeImageUrl(url, IGDBService.SIZE_CARD));
                }
                if (selectedGameJson.has("first_release_date")) {
                    long ts = selectedGameJson.getLong("first_release_date") * 1000L;
                    juego.setAnyoLanzamiento(Integer.parseInt(new java.text.SimpleDateFormat("yyyy").format(new java.util.Date(ts))));
                }
                if (selectedGameJson.has("total_rating")) {
                    juego.setNotaMediaIGDB(selectedGameJson.getDouble("total_rating"));
                }
                if (selectedGameJson.has("genres")) {
                    JSONArray gs = selectedGameJson.getJSONArray("genres");
                    if (gs.length() > 0) juego.setGenero(gs.getJSONObject(0).getString("name"));
                }

                // Default state for NEW additions
                String selState = addStateFilter.getValue();
                if (selState != null) {
                    for (EstadoJuego e : EstadoJuego.values()) {
                        if (e.getDisplayName().equals(selState)) {
                            juego.setEstado(e);
                            break;
                        }
                    }
                } else {
                    juego.setEstado(EstadoJuego.COMPLETADO);
                }
            }

            // Override state if graded
            String noteTxt = addNoteField.getText().trim();
            if (!noteTxt.isEmpty()) {
                juego.setEstado(EstadoJuego.COMPLETADO);
                try {
                    juego.setNotaPersonal(Double.parseDouble(noteTxt));
                } catch (NumberFormatException ignored) {}
            } else {
                juego.setNotaPersonal(null);
                // Respect manual selection if not grading
                String selState = addStateFilter.getValue();
                if (selState != null) {
                    for (EstadoJuego e : EstadoJuego.values()) {
                        if (e.getDisplayName().equals(selState)) {
                            juego.setEstado(e);
                            break;
                        }
                    }
                }
            }

            juego.setComentario(addComment.getText().trim());
            juego.setEsFavorito(addFavToggle.isSelected());

            juegoService.añadirJuego(usuario, juego);

            // Update local cache
            if (allLibraryGames != null) {
                if (editingJuego == null) {
                    allLibraryGames.add(juego);
                } else {
                    // Update stats if needed, but the object itself is updated
                }
            }

            if (lastSearchResults != null && editingJuego == null) {
                String addedId = juego.getIgdbId();
                lastSearchResults.removeIf(jo -> String.valueOf(jo.getInt("id")).equals(addedId));
            }

            Platform.runLater(() -> {
                if (editingJuego != null) {
                    // Refresh library/wishlist view
                    if ("library".equals(currentTab)) showLibrary();
                    else if ("wishlist".equals(currentTab)) showWishlist();
                } else {
                    if (searchField.getText().isEmpty()) {
                        updateSearchGridResponsive();
                    } else {
                        searchField.clear();
                        doSearch("");
                    }
                }
                closeAddGameOverlay();
                editingJuego = null;
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleCardPressed(JSONObject jo, GameCard card, javafx.scene.input.MouseEvent me, boolean isLibrary) {
        handleCardPressed(jo, card, me, isLibrary, null);
    }

    private void handleCardPressed(JSONObject jo, GameCard card, javafx.scene.input.MouseEvent me, boolean isLibrary, Juego jObj) {
        lastMousePressEvent = me;
        isWheelActive = false;
        if (wheelHoldTimer != null) wheelHoldTimer.stop();

        if (checkShortcutWheel.isSelected()) {
            wheelHoldTimer = new javafx.animation.Timeline(new javafx.animation.KeyFrame(javafx.util.Duration.millis(350), ev -> {
                isWheelActive = true;
                if (isLibrary) openCardOptionsFromJuego(jObj, card, lastMousePressEvent);
                else openCardOptions(jo, card, lastMousePressEvent);
            }));
            wheelHoldTimer.playFromStart();

            // Listen for drag on the card itself since it captures the mouse
            card.setOnMouseDragged(this::handleWheelDrag);
        }
    }

    private void handleCardReleased(JSONObject jo, GameCard card, javafx.scene.input.MouseEvent me, boolean isLibrary) {
        handleCardReleased(jo, card, me, isLibrary, null);
    }

    private void handleCardReleased(JSONObject jo, GameCard card, javafx.scene.input.MouseEvent me, boolean isLibrary, Juego jObj) {
        if (wheelHoldTimer != null) wheelHoldTimer.stop();
        card.setOnMouseDragged(null); // Stop drag tracking
        if (isWheelActive) {
            if (wheelSelectedAction != null) wheelSelectedAction.run();
            closeActiveWheel();
        } else {
            if (isLibrary) mostrarDetallesCompletosDeJuego(jObj);
            else mostrarDetallesCompletos(jo);
        }
        isWheelActive = false;
    }

    private double wheelCenterX, wheelCenterY;
    private javafx.scene.Group segG, segW, segP;
    private javafx.scene.Group currentWheelHovered = null;

    private void openCardOptions(JSONObject j, javafx.scene.Node anchor, javafx.scene.input.MouseEvent event) {
        javafx.scene.layout.Pane overlay = new javafx.scene.layout.Pane();
        overlay.getStyleClass().add("detail-overlay");
        overlay.setStyle("-fx-background-color: transparent;");
        overlay.setPrefSize(rootPane.getWidth(), rootPane.getHeight());

        activeWheelOverlay = overlay;
        wheelSelectedAction = null;
        currentWheelHovered = null;

        javafx.scene.Group wheelGroup = new javafx.scene.Group();
        double radius = 220;
        double posX = event.getSceneX();
        double posY = event.getSceneY();

        if (posX < radius) posX = radius;
        if (posX > rootPane.getWidth() - radius) posX = rootPane.getWidth() - radius;
        if (posY < radius) posY = radius;
        if (posY > rootPane.getHeight() - radius) posY = rootPane.getHeight() - radius;

        wheelCenterX = posX;
        wheelCenterY = posY;

        wheelGroup.setLayoutX(wheelCenterX);
        wheelGroup.setLayoutY(wheelCenterY);

        Label centerLabel = new Label("¿Qué quieres hacer con\n" + j.getString("name") + "?");
        centerLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-text-alignment: center; -fx-wrap-text: true;");
        centerLabel.setMaxWidth(110);
        centerLabel.layoutXProperty().bind(centerLabel.widthProperty().divide(-2));
        centerLabel.layoutYProperty().bind(centerLabel.heightProperty().divide(-2));

        segG = createWheelSegment("Evaluar", 30, () -> abrirAddFormConJuego(j));
        segW = createWheelSegment("Wishlist", 150, () -> quickAddGame(j, EstadoJuego.EN_WISHLIST));
        segP = createWheelSegment("Jugando", 270, () -> quickAddGame(j, EstadoJuego.JUGANDO));

        javafx.scene.shape.Circle boundingCircle = new javafx.scene.shape.Circle(250);
        boundingCircle.setFill(javafx.scene.paint.Color.TRANSPARENT);

        wheelGroup.getChildren().addAll(boundingCircle, segG, segW, segP, centerLabel);
        overlay.getChildren().add(wheelGroup);
        rootPane.getChildren().add(overlay);

        overlay.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(150), overlay);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
    }

    private void handleWheelDrag(javafx.scene.input.MouseEvent me) {
        if (!isWheelActive || activeWheelOverlay == null) return;

        double dx = me.getSceneX() - wheelCenterX;
        double dy = me.getSceneY() - wheelCenterY;
        double dist = Math.sqrt(dx*dx + dy*dy);
        double angle = Math.toDegrees(Math.atan2(-dy, dx));
        if (angle < 0) angle += 360;

        javafx.scene.Group target = null;
        if (dist > 60 && dist < 220) {
            if (angle >= 30 && angle < 150) target = segG;
            else if (angle >= 150 && angle < 270) target = segW;
            else if (angle >= 270 || angle < 30) target = segP;
        }

        if (target != currentWheelHovered) {
            if (currentWheelHovered != null) resetAllSegments(currentWheelHovered);
            if (target != null) simulateHover(target);
            currentWheelHovered = target;
        }
    }

    private void simulateHover(javafx.scene.Group seg) {
        if (seg.getOnMouseEntered() != null) seg.getOnMouseEntered().handle(null);
    }

    private void resetAllSegments(javafx.scene.Group... segs) {
        for (javafx.scene.Group seg : segs) {
            if (seg.getOnMouseExited() != null) seg.getOnMouseExited().handle(null);
        }
    }

    private void closeActiveWheel() {
        if (activeWheelOverlay != null) {
            final Pane toRemove = activeWheelOverlay;
            activeWheelOverlay = null;
            FadeTransition out = new FadeTransition(Duration.millis(150), toRemove);
            out.setFromValue(1); out.setToValue(0);
            out.setOnFinished(ev -> rootPane.getChildren().remove(toRemove));
            out.play();
        }
        isWheelActive = false;
        wheelSelectedAction = null;
    }

    private javafx.scene.Group createWheelSegment(String text, double startAngle, Runnable action) {
        javafx.scene.Group segmentGroup = new javafx.scene.Group();

        Arc outerArc = new Arc(0, 0, 180, 180, startAngle, 120);
        outerArc.setType(ArcType.ROUND);
        Circle innerCircle = new Circle(0, 0, 70);

        Shape donutSlice = Shape.subtract(outerArc, innerCircle);
        donutSlice.setFill(javafx.scene.paint.Color.web("#1e2837"));
        donutSlice.setStroke(javafx.scene.paint.Color.web("#2d3a50"));
        donutSlice.setStrokeWidth(3);

        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: #c7d5e0; -fx-font-size: 15px; -fx-font-weight: bold;");

        double midAngle = Math.toRadians(startAngle + 60);
        double txtX = 125 * Math.cos(midAngle);
        double txtY = -125 * Math.sin(midAngle);

        lbl.layoutXProperty().bind(lbl.widthProperty().divide(-2).add(txtX));
        lbl.layoutYProperty().bind(lbl.heightProperty().divide(-2).add(txtY));

        segmentGroup.getChildren().addAll(donutSlice, lbl);
        segmentGroup.setCursor(javafx.scene.Cursor.HAND);

        segmentGroup.setOnMouseEntered(e -> {
            wheelSelectedAction = action;
            donutSlice.setFill(javafx.scene.paint.Color.web("#243447"));
            donutSlice.setStroke(javafx.scene.paint.Color.web("#4d9ee8"));
            lbl.setStyle("-fx-text-fill: #66c0f4; -fx-font-size: 16px; -fx-font-weight: bold;");

            javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(Duration.millis(150), segmentGroup);
            tt.setToX(12 * Math.cos(midAngle));
            tt.setToY(-12 * Math.sin(midAngle));
            tt.play();

            ScaleTransition st = new ScaleTransition(Duration.millis(150), segmentGroup);
            st.setToX(1.05); st.setToY(1.05); st.play();
            segmentGroup.toFront();
        });

        segmentGroup.setOnMouseExited(e -> {
            if (wheelSelectedAction == action) wheelSelectedAction = null;
            donutSlice.setFill(javafx.scene.paint.Color.web("#1e2837"));
            donutSlice.setStroke(javafx.scene.paint.Color.web("#2d3a50"));
            lbl.setStyle("-fx-text-fill: #c7d5e0; -fx-font-size: 15px; -fx-font-weight: bold;");

            javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(Duration.millis(150), segmentGroup);
            tt.setToX(0); tt.setToY(0); tt.play();

            ScaleTransition st = new ScaleTransition(Duration.millis(150), segmentGroup);
            st.setToX(1.0); st.setToY(1.0); st.play();
        });

        segmentGroup.setPickOnBounds(false);
        donutSlice.setPickOnBounds(true);
        lbl.setMouseTransparent(true);

        return segmentGroup;
    }

    private void quickAddGame(JSONObject gameJson, EstadoJuego estado) {
        try {
            Juego juego = new Juego(gameJson.getString("name"), String.valueOf(gameJson.getInt("id")));
            juego.setEstado(estado);
            if (gameJson.has("cover")) {
                String url = gameJson.getJSONObject("cover").getString("url");
                juego.setPortadaUrl(IGDBService.upgradeImageUrl(url, IGDBService.SIZE_CARD));
            }

            // Save Metadata
            if (gameJson.has("first_release_date")) {
                long ts = gameJson.getLong("first_release_date") * 1000L;
                int year = Integer.parseInt(new java.text.SimpleDateFormat("yyyy").format(new java.util.Date(ts)));
                juego.setAnyoLanzamiento(year);
            }
            if (gameJson.has("total_rating")) {
                juego.setNotaMediaIGDB(gameJson.getDouble("total_rating"));
            }
            if (gameJson.has("genres")) {
                JSONArray gs = gameJson.getJSONArray("genres");
                if (gs.length() > 0) juego.setGenero(gs.getJSONObject(0).getString("name"));
            }

            juegoService.añadirJuego(usuario, juego);

            // Update local cache and remove from search view instantly
            if (allLibraryGames != null) {
                allLibraryGames.add(juego);
            }

            if (lastSearchResults != null) {
                String addedId = juego.getIgdbId();
                lastSearchResults.removeIf(jo -> String.valueOf(jo.getInt("id")).equals(addedId));
            }

            // Redirect to Discovery (main page) after adding
            Platform.runLater(() -> {
                if (searchField.getText().isEmpty()) {
                    updateSearchGridResponsive(); // Just remove the card
                } else {
                    searchField.clear();
                    doSearch("");
                }
                if (addGameOverlay.isVisible()) {
                    closeAddGameOverlay();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Detail dialog (inline styled)
    // ─────────────────────────────────────────────────────────────
    private void mostrarDetalleJuego(Juego juego, FlowPane grid, Label counter, Label msg, boolean isWishlist) {
        // Build a styled overlay panel instead of a default Java dialog
        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("detail-overlay");
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.75);");

        VBox panel = new VBox(18);
        panel.getStyleClass().add("detail-panel");
        panel.setMaxWidth(480);
        panel.setMaxHeight(580);
        panel.setPadding(new Insets(30));

        Label title = new Label(juego.getNombre());
        title.getStyleClass().add("detail-title");

        // Note Field
        Label lNote = new Label("Tu nota:");
        lNote.getStyleClass().add("form-label");
        TextField noteField = new TextField(juego.getNotaPersonal() != null ? String.valueOf(juego.getNotaPersonal()) : "");
        noteField.setPromptText("Nota (0-100)");
        noteField.getStyleClass().add("search-input");

        // Comment
        Label lComment = new Label("Comentario:");
        lComment.getStyleClass().add("form-label");
        TextArea commentArea = new TextArea(juego.getComentario() != null ? juego.getComentario() : "");
        commentArea.setPromptText("Escribe tu comentario...");
        commentArea.setPrefRowCount(3);
        commentArea.setWrapText(true);
        commentArea.getStyleClass().add("styled-textarea");
        commentArea.textProperty().addListener((obs, ov, nv) -> {
            javafx.scene.text.Text text = new javafx.scene.text.Text(nv);
            text.setFont(commentArea.getFont());
            text.setWrappingWidth(commentArea.getWidth() > 30 ? commentArea.getWidth() - 20 : 400);
            commentArea.setPrefHeight(Math.max(80, text.getLayoutBounds().getHeight() + 30));
        });

        // Favorite toggle
        ToggleButton favBtn = new ToggleButton(juego.getEsFavorito() ? "★  Favorito" : "☆  Marcar favorito");
        favBtn.setSelected(Boolean.TRUE.equals(juego.getEsFavorito()));
        favBtn.getStyleClass().add("fav-toggle");
        favBtn.selectedProperty().addListener((o, ov, nv) ->
            favBtn.setText(nv ? "★  Favorito" : "☆  Marcar favorito"));

        // State combo
        Label lState = new Label("Estado:");
        lState.getStyleClass().add("form-label");
        ComboBox<String> stateCombo = new ComboBox<>();
        for (EstadoJuego e : EstadoJuego.values()) stateCombo.getItems().add(e.getDisplayName());
        stateCombo.setValue(juego.getEstado().getDisplayName());
        stateCombo.getStyleClass().add("styled-combo");
        stateCombo.setMaxWidth(Double.MAX_VALUE);

        // Buttons
        Button btnSave   = new Button("Guardar cambios");
        Button btnDelete = new Button("Eliminar juego");
        Button btnCancel = new Button("Cancelar");
        btnSave.getStyleClass().addAll("btn-primary");
        btnDelete.getStyleClass().add("btn-danger");
        btnCancel.getStyleClass().add("btn-ghost");
        HBox btnRow = new HBox(10, btnSave, btnDelete, btnCancel);
        btnRow.setAlignment(Pos.CENTER_RIGHT);

        panel.getChildren().addAll(title, lNote, noteField,
                lComment, commentArea, favBtn, lState, stateCombo, btnRow);
        overlay.getChildren().add(panel);

        // Add to root
        rootPane.getChildren().add(overlay);
        overlay.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(200), overlay);
        ft.setFromValue(0); ft.setToValue(1); ft.play();

        Runnable closeOverlay = () -> {
            FadeTransition out = new FadeTransition(Duration.millis(150), overlay);
            out.setFromValue(1); out.setToValue(0);
            out.setOnFinished(ev -> rootPane.getChildren().remove(overlay));
            out.play();
        };

        btnCancel.setOnAction(e -> closeOverlay.run());
        overlay.setOnMouseClicked(e -> { if (e.getTarget() == overlay) closeOverlay.run(); });

        btnSave.setOnAction(e -> {
            try {
                try {
                    juego.setNotaPersonal(!noteField.getText().trim().isEmpty() ? Double.parseDouble(noteField.getText().trim()) : null);
                } catch (NumberFormatException ignored) {}
                juego.setComentario(commentArea.getText().trim());
                juego.setEsFavorito(favBtn.isSelected());
                for (EstadoJuego st : EstadoJuego.values()) {
                    if (st.getDisplayName().equals(stateCombo.getValue())) { juego.setEstado(st); break; }
                }
                juegoService.actualizarJuego(usuario, juego);
                closeOverlay.run();
                reloadCurrentGrids(grid, counter, msg, isWishlist);
            } catch (Exception ex) { ex.printStackTrace(); }
        });

        btnDelete.setOnAction(e -> {
            try {
                juegoService.eliminarJuego(usuario, juego.getIgdbId());
                closeOverlay.run();
                reloadCurrentGrids(grid, counter, msg, isWishlist);
            } catch (Exception ex) { ex.printStackTrace(); }
        });
    }

    private void reloadCurrentGrids(FlowPane grid, Label counter, Label msg, boolean isWishlist) {
        new Thread(() -> {
            try {
                List<Juego> todos = juegoService.obtenerJuegosLista(usuario);
                allLibraryGames = todos;
                List<Juego> juegos = isWishlist
                    ? todos.stream().filter(j -> j.getEstado() == EstadoJuego.EN_WISHLIST).collect(Collectors.toList())
                    : todos.stream().filter(j -> j.getEstado() != EstadoJuego.EN_WISHLIST).collect(Collectors.toList());
                Platform.runLater(() -> mostrarJuegosEnGrid(juegos, grid, counter, msg, isWishlist));
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    // ─────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────
    private String getPortadaUrl(JSONObject j) {
        if (j.has("cover") && j.getJSONObject("cover").has("url")) {
            return j.getJSONObject("cover").getString("url");
        }
        return null;
    }

    @FXML private void closeGameDetailOverlay() {
        FadeTransition ft = new FadeTransition(Duration.millis(200), gameDetailOverlay);
        ft.setFromValue(1); ft.setToValue(0);
        ft.setOnFinished(e -> gameDetailOverlay.setVisible(false));
        ft.play();
    }

    @FXML private void closeLibraryDetailOverlay() {
        FadeTransition ft = new FadeTransition(Duration.millis(200), libraryDetailOverlay);
        ft.setFromValue(1); ft.setToValue(0);
        ft.setOnFinished(e -> libraryDetailOverlay.setVisible(false));
        ft.play();
    }

    private void mostrarDetallesCompletos(JSONObject j) {
        selectedGameJson = j;
        editingJuego = null;
        selectedGameDetails = j;

        detailTitle.setText(j.getString("name"));
        detailSummary.setText(j.has("summary") ? j.getString("summary") : "Cargando resumen...");

        // (Search overlay has no form fields anymore)

        String year = "N/A";
        if (j.has("first_release_date")) {
            long ts = j.getLong("first_release_date") * 1000L;
            year = new java.text.SimpleDateFormat("yyyy").format(new java.util.Date(ts));
        }
        String genre = "Unknown";
        if (j.has("genres")) {
            JSONArray gs = j.getJSONArray("genres");
            if (gs.length() > 0) genre = gs.getJSONObject(0).getString("name");
        }
        detailMeta.setText(year + "  |  " + genre);
        detailRating.setText(j.has("total_rating") ? String.format("%.1f ★", j.getDouble("total_rating") / 10.0) : "N/A");

        if (j.has("cover")) {
            String url = j.getJSONObject("cover").getString("url");
            detailCover.setImage(new Image(IGDBService.upgradeImageUrl(url, IGDBService.SIZE_CARD), true));
        } else {
            detailCover.setImage(null);
        }

        detailGallery.getChildren().clear();

        gameDetailOverlay.setVisible(true);
        FadeTransition ft = new FadeTransition(Duration.millis(250), gameDetailOverlay);
        ft.setFromValue(0); ft.setToValue(1); ft.play();

        // Fetch extra info (summary and screenshots)
        new Thread(() -> {
            try {
                JSONObject details = igdbService.getGameDetails(j.getInt("id"));
                if (details != null) {
                    selectedGameDetails = details;
                    Platform.runLater(() -> {
                        if (details.has("summary")) detailSummary.setText(details.getString("summary"));

                        if (details.has("screenshots")) {
                            JSONArray scs = details.getJSONArray("screenshots");
                            for (int i = 0; i < scs.length(); i++) {
                                String sUrl = IGDBService.upgradeImageUrl(scs.getJSONObject(i).getString("url"), "t_screenshot_med");
                                ImageView iv = new ImageView(new Image(sUrl, true));
                                iv.setFitWidth(300);
                                iv.setPreserveRatio(true);
                                iv.getStyleClass().add("screenshot-img");
                                detailGallery.getChildren().add(iv);
                            }
                        }
                    });
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void mostrarDetallesCompletosDeJuego(Juego j) {
        editingJuego = j;
        selectedGameJson = null;

        libDetailTitle.setText(j.getNombre());
        libDetailSummary.setText("Cargando información...");
        libDetailMeta.setText((j.getAnyoLanzamiento() != null ? j.getAnyoLanzamiento() : "N/A") + "  |  " + (j.getGenero() != null ? j.getGenero() : "N/A"));
        libDetailRating.setText(j.getNotaMediaIGDB() != null ? String.format("%.1f ★", j.getNotaMediaIGDB() / 10.0) : "N/A");

        // Populate form with existing data
        libDetailNoteField.setText(j.getNotaPersonal() != null ? String.valueOf(j.getNotaPersonal()) : "");
        libDetailStateFilter.setValue(j.getEstado().getDisplayName());
        libDetailComment.setText(j.getComentario() != null ? j.getComentario() : "");
        libDetailFavToggle.setSelected(j.getEsFavorito() != null ? j.getEsFavorito() : false);

        if (j.getPortadaUrl() != null) {
            libDetailCover.setImage(new Image(j.getPortadaUrl(), true));
        } else {
            libDetailCover.setImage(null);
        }

        libDetailGallery.getChildren().clear();
        libraryDetailOverlay.setVisible(true);
        FadeTransition ft = new FadeTransition(Duration.millis(250), libraryDetailOverlay);
        ft.setFromValue(0); ft.setToValue(1); ft.play();

        new Thread(() -> {
            try {
                JSONObject details = igdbService.getGameDetails(Integer.parseInt(j.getIgdbId()));
                if (details != null) {
                    selectedGameDetails = details;
                    Platform.runLater(() -> {
                        if (details.has("summary")) libDetailSummary.setText(details.getString("summary"));
                        if (details.has("screenshots")) {
                            JSONArray scs = details.getJSONArray("screenshots");
                            for (int i = 0; i < scs.length(); i++) {
                                String sUrl = IGDBService.upgradeImageUrl(scs.getJSONObject(i).getString("url"), "t_screenshot_med");
                                ImageView iv = new ImageView(new Image(sUrl, true));
                                iv.setFitWidth(300); iv.setPreserveRatio(true);
                                iv.getStyleClass().add("screenshot-img");
                                libDetailGallery.getChildren().add(iv);
                            }
                        }
                    });
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void openCardOptionsFromJuego(Juego j, GameCard card, javafx.scene.input.MouseEvent e) {
        JSONObject jo = new JSONObject();
        jo.put("id", Integer.parseInt(j.getIgdbId()));
        jo.put("name", j.getNombre());
        if (j.getPortadaUrl() != null) {
            JSONObject cv = new JSONObject();
            cv.put("url", j.getPortadaUrl());
            jo.put("cover", cv);
        }
        openCardOptions(jo, card, e);
    }

    @FXML private void onDetailActionGrade() {
        closeGameDetailOverlay();
        if (editingJuego != null) abrirFormEdicion(editingJuego);
        else if (selectedGameJson != null) abrirAddFormConJuego(selectedGameJson);
    }

    @FXML private void onDetailActionWishlist() {
        closeGameDetailOverlay();
        if (editingJuego != null) {
            editingJuego.setEstado(EstadoJuego.EN_WISHLIST);
            juegoService.añadirJuego(usuario, editingJuego);
            showLibrary();
        } else if (selectedGameJson != null) {
            quickAddGame(selectedGameJson, EstadoJuego.EN_WISHLIST);
        }
    }

    @FXML private void onDetailActionPlaying() {
        closeGameDetailOverlay();
        if (editingJuego != null) {
            editingJuego.setEstado(EstadoJuego.JUGANDO);
            juegoService.añadirJuego(usuario, editingJuego);
            showLibrary();
        } else if (selectedGameJson != null) {
            quickAddGame(selectedGameJson, EstadoJuego.JUGANDO);
        }
    }

    @FXML private void onDetailActionSave() {
        if (editingJuego != null) {
            try {
                String notaStr = libDetailNoteField.getText();
                if (notaStr != null && !notaStr.trim().isEmpty()) {
                    editingJuego.setNotaPersonal(Double.parseDouble(notaStr.trim()));
                } else {
                    editingJuego.setNotaPersonal(null);
                }
            } catch (NumberFormatException e) {
                // Ignore or handle
            }
            editingJuego.setComentario(libDetailComment.getText());
            editingJuego.setEsFavorito(libDetailFavToggle.isSelected());

            String estadoStr = libDetailStateFilter.getValue();
            if (estadoStr != null) {
                for (EstadoJuego e : EstadoJuego.values()) {
                    if (e.getDisplayName().equals(estadoStr)) {
                        editingJuego.setEstado(e);
                        break;
                    }
                }
            }

            // Run the save operation asynchronously to prevent UI freeze
            final Juego toSave = editingJuego;
            new Thread(() -> {
                juegoService.añadirJuego(usuario, toSave);
            }).start();
            
            closeLibraryDetailOverlay();
            if ("library".equals(currentTab)) showLibrary();
            else if ("wishlist".equals(currentTab)) showWishlist();
        }
    }

    @FXML private void onDetailActionDelete() {
        if (editingJuego != null) {
            final Juego toDelete = editingJuego;
            
            new Thread(() -> {
                try { juegoService.eliminarJuego(usuario, toDelete.getIgdbId()); }
                catch (Exception ex) { ex.printStackTrace(); }
            }).start();

            if (allLibraryGames != null) {
                allLibraryGames.removeIf(j -> j.getIgdbId().equals(toDelete.getIgdbId()));
            }

            closeLibraryDetailOverlay();
            
            // Aggressive refresh: run immediately AND in Platform.runLater
            if ("library".equals(currentTab)) {
                renderLibraryGrid();
            } else if ("wishlist".equals(currentTab)) {
                renderWishlistGrid();
            }
            
            Platform.runLater(() -> {
                if ("library".equals(currentTab)) {
                    renderLibraryGrid();
                } else if ("wishlist".equals(currentTab)) {
                    renderWishlistGrid();
                }
            });
        }
    }

    // ── Settings Logic ───────────────────────────────────────────
    @FXML private void openSettings() {
        if (sideMenuOverlay.isVisible()) toggleSideMenu(); // Close side menu
        settingsOverlay.setVisible(true);
        settingsOverlay.setOpacity(0);

        FadeTransition ft = new FadeTransition(Duration.millis(300), settingsOverlay);
        ft.setFromValue(0); ft.setToValue(1); ft.play();

        ScaleTransition st = new ScaleTransition(Duration.millis(300), settingsPanel);
        st.setFromX(0.95); st.setFromY(0.95);
        st.setToX(1.0); st.setToY(1.0);
        st.play();
    }

    @FXML private void closeSettings() {
        FadeTransition ft = new FadeTransition(Duration.millis(250), settingsOverlay);
        ft.setFromValue(1); ft.setToValue(0);
        ft.setOnFinished(e -> settingsOverlay.setVisible(false));
        ft.play();
    }

    @FXML private void showSettingsGeneral() {
        settingsTitle.setText("General");
        btnSettingsGeneral.getStyleClass().add("nav-active-settings");
        // For now only general exists
    }
}
