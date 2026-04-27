package models;

public enum EstadoJuego {
    COMPLETADO("Completado"),
    JUGANDO("Jugando"),
    EN_PAUSA("En pausa"),
    NO_JUGADO("Pendiente"),
    EN_WISHLIST("En wishlist");

    private final String displayName;

    EstadoJuego(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}