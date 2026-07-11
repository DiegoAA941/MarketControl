package util;

import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Limite de pedidos publicos por IP: sin login, alguien podria mandar pedidos
 * falsos en bucle y dejar el inventario reservado sin pagar. Mismo patron en
 * memoria que {@link IntentosLoginUtil}.
 */
public final class LimitePedidosPublicosUtil {

    public static final int MAX_PEDIDOS = 5;
    public static final long VENTANA_MINUTOS = 10;

    private static final ConcurrentHashMap<String, Deque<Long>> registro = new ConcurrentHashMap<>();

    private LimitePedidosPublicosUtil() {
    }

    /** Si lo permite, registra el intento contra el limite de la ventana. */
    public static boolean permitirNuevoPedido(String ip) {
        String clave = ip == null ? "desconocida" : ip;
        long ahora = System.currentTimeMillis();
        long ventanaMs = VENTANA_MINUTOS * 60_000;
        Deque<Long> marcas = registro.computeIfAbsent(clave, k -> new ConcurrentLinkedDeque<>());
        synchronized (marcas) {
            while (!marcas.isEmpty() && ahora - marcas.peekFirst() > ventanaMs) {
                marcas.pollFirst();
            }
            if (marcas.size() >= MAX_PEDIDOS) {
                return false;
            }
            marcas.addLast(ahora);
            return true;
        }
    }
}
