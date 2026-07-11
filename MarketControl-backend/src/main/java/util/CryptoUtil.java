package util;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cifrado AES-256-GCM para campos sensibles en reposo (DNI, Email, Telefono, Direccion, Referencia).
 * El IV aleatorio impide comparar por igualdad, por eso DNI/Email/Telefono usan ademas
 * un indice ciego (HMAC-SHA256) para busqueda y UNIQUE. Clave maestra en MC_CLAVE_CIFRADO
 * (Base64, 32 bytes), nunca hardcodeada ni logueada; generarla con: openssl rand -base64 32
 */
public final class CryptoUtil {

    private static final Logger logger = LoggerFactory.getLogger(CryptoUtil.class);

    private static final String VARIABLE_ENTORNO = "MC_CLAVE_CIFRADO";
    private static final int GCM_IV_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;

    private static final SecretKeySpec CLAVE_AES;
    private static final SecretKeySpec CLAVE_HMAC;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    static {
        String claveB64 = System.getenv(VARIABLE_ENTORNO);
        if (StringUtils.isBlank(claveB64)) {
            throw new IllegalStateException("Falta la variable de entorno " + VARIABLE_ENTORNO
                    + " (clave AES-256 en Base64, 32 bytes). Generarla con: openssl rand -base64 32");
        }
        byte[] claveMaestra;
        try {
            claveMaestra = Base64.getDecoder().decode(claveB64);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(VARIABLE_ENTORNO + " no es Base64 valido.", e);
        }
        if (claveMaestra.length != 32) {
            throw new IllegalStateException(VARIABLE_ENTORNO + " debe decodificar a 32 bytes (AES-256).");
        }
        try {
            // Subclaves independientes por HMAC-SHA256: no reusar la clave maestra en dos algoritmos.
            CLAVE_AES = new SecretKeySpec(derivarSubclave(claveMaestra, "AES"), "AES");
            CLAVE_HMAC = new SecretKeySpec(derivarSubclave(claveMaestra, "HMAC"), "HmacSHA256");
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudieron derivar las subclaves de cifrado.", e);
        }
        logger.info("CryptoUtil inicializado correctamente.");
    }

    private CryptoUtil() {
    }

    private static byte[] derivarSubclave(byte[] claveMaestra, String etiqueta) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(claveMaestra, "HmacSHA256"));
        return mac.doFinal(etiqueta.getBytes(StandardCharsets.UTF_8));
    }

    /** @return Base64 de (IV || ciphertext || tag), o null si la entrada es null. */
    public static String cifrar(String textoPlano) {
        if (textoPlano == null) {
            return null;
        }
        try {
            byte[] iv = new byte[GCM_IV_BYTES];
            ALEATORIO.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, CLAVE_AES, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] cifrado = cipher.doFinal(textoPlano.getBytes(StandardCharsets.UTF_8));
            byte[] resultado = new byte[iv.length + cifrado.length];
            System.arraycopy(iv, 0, resultado, 0, iv.length);
            System.arraycopy(cifrado, 0, resultado, iv.length, cifrado.length);
            return Base64.getEncoder().encodeToString(resultado);
        } catch (GeneralSecurityException e) {
            logger.error("Error al cifrar un campo sensible.", e);
            throw new RuntimeException("Error de cifrado.", e);
        }
    }

    /** Descifra un valor generado por {@link #cifrar(String)}. */
    public static String descifrar(String textoCifradoB64) {
        if (textoCifradoB64 == null) {
            return null;
        }
        try {
            byte[] datos = Base64.getDecoder().decode(textoCifradoB64);
            byte[] iv = new byte[GCM_IV_BYTES];
            System.arraycopy(datos, 0, iv, 0, GCM_IV_BYTES);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, CLAVE_AES, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] textoPlano = cipher.doFinal(datos, GCM_IV_BYTES, datos.length - GCM_IV_BYTES);
            return new String(textoPlano, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            logger.error("Error al descifrar un campo sensible.", e);
            throw new RuntimeException("Error de descifrado.", e);
        }
    }

    /** HMAC-SHA256 en hex, determinista, para busquedas exactas y UNIQUE. Normaliza trim+minusculas. */
    public static String indiceCiego(String valor) {
        if (valor == null) {
            return null;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(CLAVE_HMAC);
            byte[] hash = mac.doFinal(valor.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (GeneralSecurityException e) {
            logger.error("Error al calcular el indice ciego de un campo sensible.", e);
            throw new RuntimeException("Error de indexado.", e);
        }
    }
}
