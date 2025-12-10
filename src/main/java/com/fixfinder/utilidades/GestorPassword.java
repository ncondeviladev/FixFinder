package com.fixfinder.utilidades;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.regex.Pattern;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Clase de utilidad para la gestión de contraseñas.
 * Incluye validación de formato y hashing seguro usando PBKDF2.
 * 
 * NOTA: Se utiliza PBKDF2 en lugar de SHA-256 simple porque PBKDF2 incorpora
 * "Key Stretching" (iteraciones) y "Salting" nativo, lo que lo hace mucho más
 * resistente a ataques de fuerza bruta y Rainbow Tables que un hash rápido como
 * SHA.
 */
public class GestorPassword {

    // Configuración para PBKDF2
    private static final int ITERATIONS = 65536; // repeticiones del hasheado para mas seguridad
    private static final int KEY_LENGTH = 256;
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256"; // PBKDF2 con SHA256
    private static final int SALT_LENGTH = 16; // Longitud del salt en bytes, se añade un dato aleatorio para que dos
    // contraseñas iguales no tengan el mismo hash 
    // Regex para validación: Mínimo 8 caracteres, al menos una mayúscula y un número.
    private static final String PASSWORD_PATTERN = "^(?=.*[A-Z])(?=.*\\d).{8,}$";
    private static final Pattern pattern = Pattern.compile(PASSWORD_PATTERN); // validador directo con el regex

    /**
     * Valida si la contraseña cumple con los requisitos de seguridad.
     * Requisitos: Mínimo 8 caracteres, 1 mayúscula, 1 número.
     *
     * @param password Contraseña a validar.
     * @return true si es válida, false en caso contrario.
     */
    public static boolean esFormatoValido(String password) {
        if (password == null) {
            return false;
        }
        return pattern.matcher(password).matches();
    }

    /**
     * Genera un hash seguro de la contraseña utilizando PBKDF2 y un Salt aleatorio.
     * El formato de salida es: salt:hash (ambos en Base64).
     *
     * @param password Contraseña en texto plano.
     * @return String que contiene el salt y el hash separados por ":".
     */
    public static String hashearPassword(String password) {
        char[] chars = password.toCharArray();
        byte[] salt = getSalt(); // genera un salt aleatorio

        // Configuración para PBKDF2
        PBEKeySpec spec = new PBEKeySpec(chars, salt, ITERATIONS, KEY_LENGTH);
        try {
            // Ejecuta el algoritmo de hasheado
            SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] hash = skf.generateSecret(spec).getEncoded();
            // Base64 convierte los bytes a texto
            return Base64.getEncoder().encodeToString(salt) + ":" + Base64.getEncoder().encodeToString(hash);
            // Nos devuelve el salt y el hash en formato salt:hash porque sal original se
            // usará para verificar contraseña
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Error al hashear la contraseña", e);
        }
    }

    /**
     * Verifica si una contraseña en texto plano coincide con el hash almacenado.
     *
     * @param originalPassword Contraseña en texto plano.
     * @param storedPassword   Hash almacenado (formato salt:hash).
     * @return true si coinciden, false en caso contrario.
     */
    public static boolean verificarPassword(String originalPassword, String storedPassword) {
        String[] parts = storedPassword.split(":");
        if (parts.length != 2) {
            return false; // Formato inválido
        }

        byte[] salt = Base64.getDecoder().decode(parts[0]);
        byte[] hash = Base64.getDecoder().decode(parts[1]);

        PBEKeySpec spec = new PBEKeySpec(originalPassword.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] testHash = skf.generateSecret(spec).getEncoded();

            // Comparación segura (tiempo constante)
            int diff = hash.length ^ testHash.length;
            for (int i = 0; i < hash.length && i < testHash.length; i++) {
                diff |= hash[i] ^ testHash[i];
            }
            return diff == 0;
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Error al verificar la contraseña", e);
        }
    }

    private static byte[] getSalt() {
        SecureRandom sr;
        try {
            sr = SecureRandom.getInstanceStrong();
        } catch (NoSuchAlgorithmException e) {
            sr = new SecureRandom();
        }
        byte[] salt = new byte[SALT_LENGTH];
        sr.nextBytes(salt);
        return salt;
    }
}
