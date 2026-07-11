package web;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import dao.DAOFactory;
import dao.interfaces.ICategoriaDAO;
import dao.interfaces.IClienteDAO;
import dao.interfaces.IConfiguracionDAO;
import dao.interfaces.IPedidoDAO;
import dao.interfaces.IProductoDAO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import modelo.Cliente;
import modelo.DetallePedido;
import modelo.Pedido;
import modelo.Producto;
import servicio.CalculadoraFinanciera;
import util.LimitePedidosPublicosUtil;

/**
 * API PÚBLICA de la tienda virtual (sin autenticación).
 *
 *   GET  /api/public/productos      -> catálogo (solo activos), solo lectura
 *   GET  /api/public/categorias     -> categorías activas
 *   GET  /api/public/configuracion  -> datos públicos (teléfono WhatsApp, nombre)
 *   POST /api/public/pedidos        -> crea cliente + pedido de DELIVERY (estado Pendiente)
 *
 * Seguridad: el alta de pedido es pública, así que los PRECIOS se calculan en el
 * servidor a partir de la BD (no se confía en el precio que envía el navegador),
 * y las cantidades se acotan. El flujo de "recojo en tienda" NO usa este endpoint:
 * se cobra luego en el POS como venta de mostrador.
 */
@WebServlet("/api/public/*")
public class PublicServlet extends ApiBaseServlet {

    private static final long serialVersionUID = 1L;
    private final transient IProductoDAO productoDAO = DAOFactory.productoDAO();
    private final transient ICategoriaDAO categoriaDAO = DAOFactory.categoriaDAO();
    private final transient IClienteDAO clienteDAO = DAOFactory.clienteDAO();
    private final transient IPedidoDAO pedidoDAO = DAOFactory.pedidoDAO();
    private final transient IConfiguracionDAO configDAO = DAOFactory.configuracionDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            switch (ruta(req)) {
                case "/productos" -> {
                    List<Producto> activos = new ArrayList<>();
                    for (Producto p : productoDAO.listarTodos()) {
                        if (p.isEstado()) {
                            activos.add(p);
                        }
                    }
                    ok(resp, activos);
                }
                case "/categorias" -> ok(resp, categoriaDAO.listarTodos());
                case "/configuracion" -> {
                    Map<String, String> cfg = configDAO.obtenerTodo();
                    Map<String, Object> publico = new LinkedHashMap<>();
                    publico.put("telefono_whatsapp", cfg.getOrDefault("telefono_whatsapp", ""));
                    publico.put("nombre_negocio", cfg.getOrDefault("nombre_negocio", "MarketControl"));
                    publico.put("direccion", cfg.getOrDefault("direccion", ""));
                    publico.put("costo_envio", cfg.getOrDefault("costo_envio", "0"));
                    publico.put("envio_gratis_desde", cfg.getOrDefault("envio_gratis_desde", "0"));
                    ok(resp, publico);
                }
                default -> error(resp, HttpServletResponse.SC_NOT_FOUND, "Recurso público no encontrado.");
            }
        } catch (Exception e) {
            manejarError(resp, e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!"/pedidos".equals(ruta(req))) {
            error(resp, HttpServletResponse.SC_NOT_FOUND, "Recurso público no encontrado.");
            return;
        }
        if (!LimitePedidosPublicosUtil.permitirNuevoPedido(req.getRemoteAddr())) {
            error(resp, 429, "Demasiados pedidos desde esta conexión. Intenta de nuevo en unos minutos.");
            return;
        }
        try {
            JsonObject o = leerJson(req);

            // Tipo de entrega: 'delivery' (por defecto) o 'recojo'
            String tipoRaw = texto(o, "tipo");
            boolean esRecojo = "recojo".equalsIgnoreCase(tipoRaw);
            String tipoEntrega = esRecojo ? "Recojo" : "Delivery";

            // --- Datos del cliente ---
            JsonObject c = o.getAsJsonObject("cliente");
            if (c == null || !c.has("nombres") || c.get("nombres").getAsString().isBlank()) {
                error(resp, HttpServletResponse.SC_BAD_REQUEST, "Falta el nombre del cliente.");
                return;
            }
            String nombres = c.get("nombres").getAsString().trim();
            String telefono = texto(c, "telefono");
            String direccion = texto(c, "direccion");
            String referencia = texto(c, "referencia");
            if (!esRecojo && direccion.isBlank()) {
                error(resp, HttpServletResponse.SC_BAD_REQUEST, "El delivery requiere una dirección de entrega.");
                return;
            }
            if (esRecojo && telefono.isBlank()) {
                error(resp, HttpServletResponse.SC_BAD_REQUEST, "El recojo requiere un teléfono de contacto.");
                return;
            }

            // --- Items (se acota cantidad y se ignora cualquier precio enviado) ---
            JsonArray items = o.getAsJsonArray("items");
            if (items == null || items.isEmpty()) {
                error(resp, HttpServletResponse.SC_BAD_REQUEST, "El pedido no tiene productos.");
                return;
            }

            List<DetallePedido> detalles = new ArrayList<>();
            BigDecimal total = BigDecimal.ZERO;
            for (int i = 0; i < items.size(); i++) {
                JsonObject it = items.get(i).getAsJsonObject();
                int idProducto = it.get("idProducto").getAsInt();
                int cantidad = it.get("cantidad").getAsInt();
                if (cantidad < 1 || cantidad > 20) {
                    error(resp, HttpServletResponse.SC_BAD_REQUEST,
                            "Cantidad inválida para un producto (máximo 20 por producto).");
                    return;
                }
                Producto p = productoDAO.buscarPorId(idProducto);
                if (p == null || !p.isEstado()) {
                    error(resp, HttpServletResponse.SC_BAD_REQUEST,
                            "Un producto del pedido ya no está disponible.");
                    return;
                }
                BigDecimal precio = precioConDescuento(p);
                BigDecimal subtotal = precio.multiply(BigDecimal.valueOf(cantidad));

                DetallePedido d = new DetallePedido();
                d.setIdProducto(idProducto);
                d.setCantidad(cantidad);
                d.setPrecioUnitario(precio);
                d.setSubtotal(subtotal);
                detalles.add(d);
                total = total.add(subtotal);
            }

            // --- Cliente: reutiliza por teléfono o crea uno nuevo ---
            Cliente cliente = null;
            if (!telefono.isBlank()) {
                cliente = clienteDAO.buscarPorTelefono(telefono);
            }
            if (cliente == null) {
                cliente = new Cliente();
                cliente.setNombres(nombres);
                cliente.setTelefono(telefono.isBlank() ? null : telefono);
                cliente.setDireccion(direccion.isBlank() ? null : direccion);
                cliente.setReferencia(referencia.isBlank() ? null : referencia);
                clienteDAO.crear(cliente);
            } else {
                // Actualiza nombre siempre; direccion/referencia solo si vienen (no las borra en recojo).
                cliente.setNombres(nombres);
                if (!direccion.isBlank()) {
                    cliente.setDireccion(direccion);
                }
                if (!referencia.isBlank()) {
                    cliente.setReferencia(referencia);
                }
                clienteDAO.actualizar(cliente);
            }

            // --- Costo de envío: solo Delivery; gratis si supera el umbral configurado ---
            BigDecimal envio = BigDecimal.ZERO;
            if (!esRecojo) {
                BigDecimal tarifa = parseMonto(configDAO.obtener("costo_envio"));
                BigDecimal umbral = parseMonto(configDAO.obtener("envio_gratis_desde"));
                boolean gratis = umbral.signum() > 0 && total.compareTo(umbral) >= 0;
                envio = gratis ? BigDecimal.ZERO : tarifa;
            }

            // --- Pedido (Pendiente). PedidoDAO.crear reserva el stock en su transacción. ---
            Pedido pedido = new Pedido();
            pedido.setIdCliente(cliente.getIdCliente());
            pedido.setEstado("Pendiente");
            pedido.setTipoEntrega(tipoEntrega);
            pedido.setCostoEnvio(envio);
            pedido.setObservaciones(texto(o, "observaciones"));
            pedido.setTotal(total.add(envio));   // productos + envío = monto a pagar
            pedido.setDetalles(detalles);

            try {
                pedidoDAO.crear(pedido);
            } catch (Exception ePedido) {
                // Nota: si la reserva de stock falla, un cliente recien creado queda huerfano (no se revierte).
                throw ePedido;
            }

            Pedido creado = pedidoDAO.buscarPorId(pedido.getIdPedido());
            escribir(resp, HttpServletResponse.SC_CREATED, creado);

        } catch (Exception e) {
            manejarError(resp, e);
        }
    }

    private BigDecimal precioConDescuento(Producto p) {
        return CalculadoraFinanciera.precioConDescuento(p.getPrecioVenta(), p.getPorcentajeDescuento());
    }

    private String texto(JsonObject o, String campo) {
        return (o.has(campo) && !o.get(campo).isJsonNull()) ? o.get(campo).getAsString().trim() : "";
    }

    /** Convierte un valor de configuración a BigDecimal; 0 si está vacío o es inválido. */
    private BigDecimal parseMonto(String valor) {
        if (valor == null || valor.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(valor.trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
