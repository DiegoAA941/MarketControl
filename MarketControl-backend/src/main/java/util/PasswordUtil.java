package util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contrasenas con PBKDF2-HMAC-SHA256 (lento a proposito), sal aleatoria por usuario.
 * Formato guardado: PBKDF2:&lt;iteraciones&gt;:&lt;salBase64&gt;:&lt;hashBase64&gt;
 */
public final class PasswordUtil {

    private static final Logger logger = LoggerFactory.getLogger(PasswordUtil.class);

    private static final String PREFIJO_PBKDF2 = "PBKDF2:";
    private static final String ALGORITMO_PBKDF2 = "PBKDF2WithHmacSHA256";
    private static final int ITERACIONES = 210_000; // lento a proposito (recomendado por OWASP para SHA-256)
    private static final int LONGITUD_CLAVE_BITS = 256;
    private static final int LONGITUD_SAL_BYTES = 16;

    private PasswordUtil() {
    }

    public static String hashContrasena(String contrasenaPlana) {
        if (StringUtils.isBlank(contrasenaPlana)) {
            throw new IllegalArgumentException("La contrasena no puede estar vacia.");
        }
        byte[] sal = new byte[LONGITUD_SAL_BYTES];
        new SecureRandom().nextBytes(sal);
        byte[] hash = derivarPBKDF2(contrasenaPlana, sal, ITERACIONES);
        String resultado = PREFIJO_PBKDF2 + ITERACIONES + ":"
                + Base64.getEncoder().encodeToString(sal) + ":"
                + Base64.getEncoder().encodeToString(hash);
        logger.debug("Hash de contrasena generado correctamente (PBKDF2).");
        return resultado;
    }

    public static boolean verificar(String contrasenaPlana, String hashAlmacenado) {
        if (StringUtils.isBlank(contrasenaPlana) || StringUtils.isBlank(hashAlmacenado)
                || !hashAlmacenado.startsWith(PREFIJO_PBKDF2)) {
            return false;
        }
        String[] partes = hashAlmacenado.substring(PREFIJO_PBKDF2.length()).split(":");
        boolean coincide = false;
        if (partes.length == 3) {
            try {
                int iteraciones = Integer.parseInt(partes[0]);
                byte[] sal = Base64.getDecoder().decode(partes[1]);
                byte[] hashEsperado = Base64.getDecoder().decode(partes[2]);
                byte[] hashCalculado = derivarPBKDF2(contrasenaPlana, sal, iteraciones);
                coincide = MessageDigest.isEqual(hashCalculado, hashEsperado);
            } catch (IllegalArgumentException e) {
                coincide = false; // hash corrupto o con formato invalido
            }
        }
        if (!coincide) {
            logger.warn("Intento de autenticacion fallido: la contrasena no coincide.");
        }
        return coincide;
    }

    private static byte[] derivarPBKDF2(String contrasenaPlana, byte[] sal, int iteraciones) {
        try {
            PBEKeySpec spec = new PBEKeySpec(contrasenaPlana.toCharArray(), sal, iteraciones, LONGITUD_CLAVE_BITS);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITMO_PBKDF2);
            return factory.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Error al derivar la contrasena con PBKDF2.", e);
        }
    }
}
