package com.fixfinder.modelos.componentes;

import jakarta.persistence.*;

/**
 * Representa una foto asociada a un trabajo.
 * Se almacena la URL/Ruta local, no la imagen en sí.
 */
@Entity
@Table(name = "foto_trabajo")
public class FotoTrabajo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    
    @Column(name = "id_trabajo")
    private int idTrabajo; // Usamos ID aquí para evitar ciclos infinitos al serializar, aunque podría ser
                           // objeto Trabajo.
    @Column(name = "url_archivo")
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
