package pe.edu.upeu.sysventas.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Encripta las claves de los usuarios con SHA-256.
 * La clave nunca se guarda tal cual en la base de datos: se guarda su "huella" (hash).
 * Ejemplo: "yaz123" se guarda como "aH8y...=" (44 caracteres).
 */
public final class PasswordUtil {

    private PasswordUtil() {
    }

    /** Devuelve el hash SHA-256 de la clave, en Base64 (siempre 44 caracteres). */
    public static String encriptar(String clave) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(clave.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }

    /** Indica si un texto ya es una clave encriptada (para no encriptarla dos veces). */
    public static boolean estaEncriptada(String clave) {
        return clave != null && clave.length() == 44 && clave.endsWith("=");
    }

    /** Compara la clave que escribió el usuario con la que está guardada. */
    public static boolean coincide(String claveEscrita, String claveGuardada) {
        return encriptar(claveEscrita).equals(claveGuardada);
    }
}
