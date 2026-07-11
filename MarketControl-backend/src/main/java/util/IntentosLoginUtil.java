package util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Bloqueo por fuerza bruta en memoria: {@value #MAX_INTENTOS} fallos bloquean
 * {@value #BLOQUEO_MINUTOS} minutos. Se reinicia si Tomcat reinicia; para varios
 * servidores habria que moverlo a BD o a un cache compartido.
 */
public final class IntentosLoginUtil {

    public static final int MAX_INTENTOS = 5;
    public static final long BLOQUEO_MINUTOS = 5;

    private static final ConcurrentHashMap<String, Estado> intentos = new ConcurrentHashMap<>();

    private IntentosLoginUtil() {
    }

    private static final class Estado {
        private final AtomicInteger fallos = new AtomicInteger(0);
        private volatile long bloqueadoHasta = 0;
    }

    private static String normalizar(String username) {
        return username == null ? "" : username.trim().toLowerCase();
    }

    /** Segundos restantes de bloqueo para este usuario (0 si no esta bloqueado). */
    public static long segundosBloqueoRestante(String username) {
        Estado e = intentos.get(normalizar(username));
        if (e == null) {
            return 0;
        }
        long restante = (e.bloqueadoHasta - System.currentTimeMillis()) / 1000;
        return restante > 0 ? restante : 0;
    }

    /** Intentos que le quedan antes de bloquearse (nunca negativo). */
    public static int intentosRestantes(String username) {
        Estado e = intentos.get(normalizar(username));
        if (e == null) {
            return MAX_INTENTOS;
        }
        return Math.max(0, MAX_INTENTOS - e.fallos.get());
    }

    /** Registra un intento fallido; si llega al maximo, activa el bloqueo temporal. */
    public static void registrarFallo(String username) {
        Estado e = intentos.computeIfAbsent(normalizar(username), k -> new Estado());
        int fallos = e.fallos.incrementAndGet();
        if (fallos >= MAX_INTENTOS) {
            e.bloqueadoHasta = System.currentTimeMillis() + BLOQUEO_MINUTOS * 60_000;
        }
    }

    /** Reinicia el contador de un usuario tras un login exitoso. */
    public static void registrarExito(String username) {
        intentos.remove(normalizar(username));
    }
}
