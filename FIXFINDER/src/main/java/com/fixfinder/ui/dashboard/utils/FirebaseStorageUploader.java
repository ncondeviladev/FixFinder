package com.fixfinder.ui.dashboard.utils;

import java.io.File;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class FirebaseStorageUploader {

    private static final String BUCKET_NAME = "fixfinder-dbb81.firebasestorage.app";
    // Using Firebase Storage REST API endpoints matching the Flutter client's
    // default Firebase backend
    private static final String FIREBASE_API_URL = "https://firebasestorage.googleapis.com/v0/b/" + BUCKET_NAME + "/o";

    /**
     * Sube un archivo a Firebase Storage usando su API REST y el acceso anónimo
     * habilitado.
     * 
     * @param archivo      Archivo físico de imagen a subir.
     * @param rutaFirebase Ruta y nombre destino, ej: "perfiles/operario_123.jpg"
     * @return La URL pública de descarga (DownloadToken incluído para verla
     *         sinAuth)
     * @throws Exception Si hay algún fallo en I/O o HTTP
     */
    public static String subirImagen(File archivo, String rutaFirebase) throws Exception {
        String urlEncodedPath = URLEncoder.encode(rutaFirebase, StandardCharsets.UTF_8).replace("+", "%20");
        String urlDestino = FIREBASE_API_URL + "?name=" + urlEncodedPath;

        byte[] fileBytes = Files.readAllBytes(archivo.toPath());

        String mimeType = Files.probeContentType(archivo.toPath());
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(urlDestino))
                .header("Content-Type", mimeType)
                .POST(HttpRequest.BodyPublishers.ofByteArray(fileBytes))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200 || response.statusCode() == 201) {
            String resBody = response.body();
            String downloadToken = extractJsonValue(resBody, "downloadTokens");

            String urlPublica = FIREBASE_API_URL + "/" + urlEncodedPath + "?alt=media&token=" + downloadToken;
            return urlPublica;
        } else {
            throw new Exception("Error al subir a Firebase REST API. Código: " + response.statusCode() + " Body: "
                    + response.body());
        }
    }

    private static String extractJsonValue(String json, String key) {
        // Busca simple de valor de llave String o Number
        String tokenMark = "\"" + key + "\":";
        int index = json.indexOf(tokenMark);
        if (index == -1)
            return "unknown";

        int start = json.indexOf("\"", index + tokenMark.length()) + 1;
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }
}
