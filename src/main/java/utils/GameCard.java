package utils;

import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import services.IGDBService;

public class GameCard extends VBox {

    private double cardWidth;
    private double cardHeight;

    private String juegoId;
    private String nombre;
    private Double notaPersonal;
    private Double notaIGDB;
    private String estado;
    private String portadaUrl;
    private String releaseDate;
    private String genre;

    public GameCard(String id, String nombre, Double notaPersonal, Double notaIGDB, String estado, String portadaUrl, String releaseDate, String genre, double width, double height) {
        this.juegoId = id;
        this.nombre = nombre;
        this.notaPersonal = notaPersonal;
        this.notaIGDB = notaIGDB;
        this.estado = estado;
        this.portadaUrl = portadaUrl;
        this.releaseDate = releaseDate;
        this.genre = genre;
        this.cardWidth = width;
        this.cardHeight = height;

        configurarEstilo();
        crearContenido();
        configurarAnimacion();
    }

    private ImageView portadaView;
    private Rectangle clip;

    public void setCardSize(double w, double h) {
        this.cardWidth = w;
        this.cardHeight = h;
        setPrefWidth(w);
        setMaxWidth(w);
        if (portadaView != null) {
            portadaView.setFitWidth(w);
            portadaView.setFitHeight(h);
        }
        if (clip != null) {
            clip.setWidth(w);
            clip.setHeight(h);
        }
    }

    private void configurarEstilo() {
        setPrefWidth(cardWidth);
        setMaxWidth(cardWidth);
        setAlignment(Pos.TOP_CENTER);
        setSpacing(0);
        setPadding(new Insets(0));
        setStyle("-fx-background-color: transparent;");
    }

    private void crearContenido() {
        getChildren().clear();

        // ── Cover image with clipped corners ──
        portadaView = new ImageView();
        portadaView.setFitWidth(cardWidth);
        portadaView.setFitHeight(cardHeight);
        portadaView.setPreserveRatio(false);
        portadaView.setSmooth(true);

        // Clip rounded top corners
        clip = new Rectangle(cardWidth, cardHeight);
        clip.setArcWidth(12);
        clip.setArcHeight(12);
        portadaView.setClip(clip);

        String urlToLoad = portadaUrl;
        if (urlToLoad != null && !urlToLoad.isEmpty()) {
            // Ensure high quality - upgrade to t_cover_big if not already
            urlToLoad = IGDBService.upgradeImageUrl(urlToLoad, IGDBService.SIZE_CARD);
        }

        if (urlToLoad != null && !urlToLoad.isEmpty()) {
            try {
                // Background loading - true = async, smooth = high quality
                Image img = new Image(urlToLoad, cardWidth, cardHeight, false, true, true);
                portadaView.setImage(img);
            } catch (Exception e) {
                portadaView.setStyle("-fx-background-color: #1e2a3a;");
            }
        }

        // ── Info box below cover ──
        VBox infoBox = new VBox(2);
        infoBox.setPadding(new Insets(6, 4, 6, 4));

        Label nombreLabel = new Label(nombre);
        nombreLabel.setWrapText(false); // Do not wrap, just elide
        nombreLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px;");
        // Force fixed height to keep them aligned
        nombreLabel.setMinHeight(16);
        nombreLabel.setMaxHeight(16);
        nombreLabel.setMaxWidth(cardWidth - 8);

        // Date and Tag
        String metaText = "";
        if (releaseDate != null) metaText += releaseDate;
        if (genre != null && !genre.isEmpty()) {
            if (!metaText.isEmpty()) metaText += " • ";
            metaText += genre;
        }
        Label metaLabel = new Label(metaText);
        metaLabel.setStyle("-fx-text-fill: #a0aec0; -fx-font-size: 9px;");
        metaLabel.setMinHeight(12);

        // Ratings Box
        javafx.scene.layout.HBox ratingsHBox = new javafx.scene.layout.HBox(8);
        ratingsHBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        // Personal Rating (Green)
        String pText = "N/A";
        if (notaPersonal != null && notaPersonal > 0) pText = String.format("%.1f", notaPersonal / 10.0);
        Label pLabel = new Label(pText + " ★");
        pLabel.setStyle("-fx-text-fill: #48bb78; -fx-font-size: 10px; -fx-font-weight: bold;");
        
        // IGDB Rating (Yellow)
        String iText = "N/A";
        if (notaIGDB != null && notaIGDB > 0) iText = String.format("%.1f", notaIGDB / 10.0);
        Label iLabel = new Label(iText + " ★");
        iLabel.setStyle("-fx-text-fill: #f6e05e; -fx-font-size: 10px; -fx-font-weight: bold;");

        ratingsHBox.getChildren().addAll(pLabel, iLabel);
        
        // Status Tag (Next to grade)
        if (!"NO_JUGADO".equals(estado)) {
            Label estadoLabel = new Label(getEstadoText());
            estadoLabel.setStyle("-fx-text-fill: " + getEstadoColor() + "; -fx-font-size: 10px; -fx-font-weight: bold;");
            
            // Add a separator dot between rating and status
            Label dot = new Label("•");
            dot.setStyle("-fx-text-fill: #4a5568; -fx-font-size: 10px;");
            
            ratingsHBox.getChildren().addAll(dot, estadoLabel);
        }

        infoBox.getChildren().addAll(nombreLabel, metaLabel, ratingsHBox);

        getChildren().addAll(portadaView, infoBox);
    }

    public String getJuegoId() { return juegoId; }

    private void configurarAnimacion() {
        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(150), this);
        ScaleTransition scaleOut = new ScaleTransition(Duration.millis(150), this);
        scaleIn.setToX(1.02);
        scaleIn.setToY(1.02);
        scaleOut.setToX(1.0);
        scaleOut.setToY(1.0);

        setOnMouseEntered(e -> { scaleIn.playFromStart(); setCursor(javafx.scene.Cursor.HAND); });
        setOnMouseExited(e -> { scaleOut.playFromStart(); setCursor(javafx.scene.Cursor.DEFAULT); });
    }

    private String getEstadoText() {
        switch (estado) {
            case "COMPLETADO":  return "✓ Completado";
            case "JUGANDO":     return "▶ Jugando";
            case "EN_PAUSA":    return "⏸ En pausa";
            case "EN_WISHLIST": return "♡ Wishlist";
            default:            return "• Pendiente";
        }
    }

    private String getEstadoColor() {
        switch (estado) {
            case "COMPLETADO":  return "#48bb78";
            case "JUGANDO":     return "#4d9ee8";
            case "EN_PAUSA":    return "#ed8936";
            case "EN_WISHLIST": return "#f6e05e";
            default:            return "#a0aec0";
        }
    }

    private String getEstadoStyleClass() {
        switch (estado) {
            case "COMPLETADO":  return "status-completado";
            case "JUGANDO":     return "status-jugando";
            case "EN_PAUSA":    return "status-pausa";
            case "EN_WISHLIST": return "status-wishlist";
            default:            return "status-nojugado";
        }
    }

    public void setAlClic(Runnable accion) {
        setOnMouseClicked(e -> accion.run());
    }

    public void actualizar(String nuevoEstado, Double nuevaNota) {
        this.estado = nuevoEstado;
        this.notaPersonal = nuevaNota;
        crearContenido();
    }
}