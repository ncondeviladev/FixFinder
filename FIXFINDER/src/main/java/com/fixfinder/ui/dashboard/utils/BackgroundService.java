package com.fixfinder.ui.dashboard.utils;

import javafx.concurrent.Task;
import javafx.application.Platform;
import java.io.File;
import java.util.function.Consumer;

/**
 * Servicio para ejecutar tareas pesadas en hilos secundarios.
 * Utiliza JavaFX Task para asegurar una integración limpia con la UI.
 */
public class BackgroundService {

    /**
     * Sube una imagen a Firebase de forma asíncrona.
     * 
     * @param file      Archivo a subir.
     * @param path      Destino en Firebase Storage.
     * @param onSuccess Callback ejecutado en el hilo de la UI tras el éxito.
     * @param onError   Callback ejecutado en el hilo de la UI si hay error.
     */
    public void subirImagen(File file, String path, Consumer<String> onSuccess, Consumer<Throwable> onError) {
        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return FirebaseStorageUploader.subirImagen(file, path);
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> onSuccess.accept(task.getValue())));
        task.setOnFailed(e -> Platform.runLater(() -> onError.accept(task.getException())));

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }
}
