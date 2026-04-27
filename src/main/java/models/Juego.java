package models;

public class Juego {
    private String id;
    private String igdbId;
    private String nombre;
    private String descripcion;
    private String portadaUrl;
    private Integer anyoLanzamiento;
    private String estudio;
    private Double notaMediaIGDB;
    private EstadoJuego estado;
    private Double notaPersonal;
    private Boolean esFavorito;
    private String comentario;
    private String genero;

    public Juego() {
        this.estado = EstadoJuego.NO_JUGADO;
        this.esFavorito = false;
    }

    public Juego(String nombre, String igdbId) {
        this();
        this.nombre = nombre;
        this.igdbId = igdbId;
    }

    // Getters y Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIgdbId() {
        return igdbId;
    }

    public void setIgdbId(String igdbId) {
        this.igdbId = igdbId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getPortadaUrl() {
        return portadaUrl;
    }

    public void setPortadaUrl(String portadaUrl) {
        this.portadaUrl = portadaUrl;
    }

    public Integer getAnyoLanzamiento() {
        return anyoLanzamiento;
    }

    public void setAnyoLanzamiento(Integer anyoLanzamiento) {
        this.anyoLanzamiento = anyoLanzamiento;
    }

    public String getEstudio() {
        return estudio;
    }

    public void setEstudio(String estudio) {
        this.estudio = estudio;
    }

    public Double getNotaMediaIGDB() {
        return notaMediaIGDB;
    }

    public void setNotaMediaIGDB(Double notaMediaIGDB) {
        this.notaMediaIGDB = notaMediaIGDB;
    }

    public EstadoJuego getEstado() {
        return estado;
    }

    public void setEstado(EstadoJuego estado) {
        this.estado = estado;
    }

    public Double getNotaPersonal() {
        return notaPersonal;
    }

    public void setNotaPersonal(Double notaPersonal) {
        this.notaPersonal = notaPersonal;
    }

    public Boolean getEsFavorito() {
        return esFavorito;
    }

    public void setEsFavorito(Boolean esFavorito) {
        this.esFavorito = esFavorito;
    }

    public String getComentario() {
        return comentario;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
    }

    public String getGenero() { return genero; }
    public void setGenero(String genero) { this.genero = genero; }

    /**
     * Devuelve la nota personal en formato 0-10
     */
    public Double getNotaPersonalNormalizada() {
        return (notaPersonal != null) ? notaPersonal / 10.0 : null;
    }

    @Override
    public String toString() {
        String estadoStr = (estado != null) ? estado.getDisplayName() : "Sin estado";
        double nota = (notaPersonal != null) ? notaPersonal / 10.0 : 0.0;
        return String.format("%s [%s] - Nota: %.1f/10", nombre, estadoStr, nota);
    }
}