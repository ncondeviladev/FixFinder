package com.fixfinder.modelos.componentes;

/**
 * Representa una foto asociada a un trabajo.
 * Se almacena la URL/Ruta local, no la imagen en sí.
 */
public class FotoTrabajo {
    private int id;
    private int idTrabajo; // Usamos ID aquí para evitar ciclos infinitos al serializar, aunque podría ser
                           // objeto Trabajo.
    private String url;

    public FotoTrabajo() {
    }

    public FotoTrabajo(int id, int idTrabajo, String url) {
        this.id = id;
        this.idTrabajo = idTrabajo;
        this.url = url;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getIdTrabajo() {
        return idTrabajo;
    }

    public void setIdTrabajo(int idTrabajo) {
        this.idTrabajo = idTrabajo;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
