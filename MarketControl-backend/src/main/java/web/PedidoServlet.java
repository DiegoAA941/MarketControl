package web;

import java.io.IOException;
import java.math.BigDecimal;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import dao.DAOFactory;
import dao.interfaces.IPedidoDAO;
import dao.interfaces.IProductoDAO;
import dao.interfaces.IRepartidorDAO;
import dao.interfaces.IVentaDAO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import modelo.DetallePedido;
import modelo.DetalleVenta;
import modelo.Pedido;
import modelo.Producto;
import modelo.Venta;

/**
 * Recurso Pedidos (modulo Delivery).
 *
 *   GET  /api/pedidos                  -> lista (con repartidor por composicion)
 *   GET  /api/pedidos/{id}             -> uno (con detalles)
 *   POST /api/pedidos                  -> crea pedido en estado 'Pendiente'
 *   PUT  /api/pedidos/{id}/estado      -> body: { "estado": "Preparando" }
 *   PUT  /api/pedidos/{id}/repartidor  -> body: { "idRepartidor": 3 } (repartidor pasa a 'En ruta')
 *
 * La reserva de stock al entrar en 'Pendiente' descuenta StockActual
 * y registra MovimientosInventario dentro de una transaccion.
 */
@WebServlet("/api/pedidos/*")
public class PedidoServlet extends ApiBaseServlet {

    private static final long serialVersionUID = 1L;
    private final transient IPedidoDAO pedidoDAO = DAOFactory.pedidoDAO();
    private final transient IRepartidorDAO repartidorDAO = DAOFactory.repartidorDAO();
    private final transient IVentaDAO ventaDAO = DAOFactory.ventaDAO();
    private final transient IProductoDAO productoDAO = DAOFactory.productoDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!requiereLogin(req, resp)) {
            return;
        }
        try {
            // POS: pedidos de Recojo pendientes (con filtro opcional por telefono)
            if ("/recojo".equals(ruta(req))) {
                ok(resp, pedidoDAO.listarRecojoPendientes(req.getParameter("telefono")));
                return;
            }
            int id = idDeRuta(req);
            if (id > 0) {
                Pedido p = pedidoDAO.buscarPorId(id);
                if (p == null) {
                    error(resp, HttpServletResponse.SC_NOT_FOUND, "Pedido no encontrado.");
                } else {
                    ok(resp, p);
                }
            } else {
                String tipo = req.getParameter("tipo");
                if (tipo != null && !tipo.isBlank()) {
                    ok(resp, pedidoDAO.listarPorTipo(tipo));
                } else {
                    ok(resp, pedidoDAO.listarTodos());
                }
            }
        } catch (Exception e) {
            manejarError(resp, e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!requiereLogin(req, resp)) {
            return;
        }
        try {
            JsonObject o = leerJson(req);
            Pedido pedido = new Pedido();
            pedido.setIdCliente(o.get("idCliente").getAsInt());
            pedido.setEstado("Pendiente");
            pedido.setObservaciones(o.has("observaciones") && !o.get("observaciones").isJsonNull()
                    ? o.get("observaciones").getAsString() : null);

            if (!o.has("detalles") || !o.get("detalles").isJsonArray()
                    || o.getAsJsonArray("detalles").isEmpty()) {
                error(resp, HttpServletResponse.SC_BAD_REQUEST, "El pedido debe incluir al menos un producto.");
                return;
            }

            BigDecimal total = BigDecimal.ZERO;
            JsonArray detalles = o.getAsJsonArray("detalles");
            for (int i = 0; i < detalles.size(); i++) {
                JsonObject d = detalles.get(i).getAsJsonObject();
                int idProducto = d.get("idProducto").getAsInt();
                int cantidad = d.get("cantidad").getAsInt();

                // Precio siempre de la BD, nunca del navegador: evita cobrar de menos.
                Producto p = productoDAO.buscarPorId(idProducto);
                if (p == null) {
                    error(resp, HttpServletResponse.SC_BAD_REQUEST,
                            "El producto id=" + idProducto + " no existe.");
                    return;
                }
                BigDecimal precio = p.getPrecioVenta();
                BigDecimal subtotal = precio.multiply(BigDecimal.valueOf(cantidad));

                DetallePedido dp = new DetallePedido();
                dp.setIdProducto(idProducto);
                dp.setCantidad(cantidad);
                dp.setPrecioUnitario(precio);
                dp.setSubtotal(subtotal);
                pedido.getDetalles().add(dp);
                total = total.add(subtotal);
            }
            pedido.setTotal(total);

            pedidoDAO.crear(pedido);
            escribir(resp, HttpServletResponse.SC_CREATED, pedido);
        } catch (Exception e) {
            manejarError(resp, e);
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!requiereLogin(req, resp)) {
            return;
        }
        try {
            String[] partes = ruta(req).replaceFirst("^/", "").split("/");
            if (partes.length < 2) {
                error(resp, HttpServletResponse.SC_BAD_REQUEST, "Ruta invalida. Usa /{id}/estado o /{id}/repartidor.");
                return;
            }
            int id = Integer.parseInt(partes[0]);
            String accion = partes[1];
            JsonObject o = leerJson(req);

            if ("estado".equals(accion)) {
                String estado = o.get("estado").getAsString();
                Pedido actual = pedidoDAO.buscarPorId(id);
                String estadoPrevio = (actual != null) ? actual.getEstado() : null;

                // Si se cancela (y no estaba ya cancelado/entregado), se devuelve el stock reservado.
                if ("Cancelado".equalsIgnoreCase(estado)
                        && estadoPrevio != null
                        && !"Cancelado".equalsIgnoreCase(estadoPrevio)
                        && !"Entregado".equalsIgnoreCase(estadoPrevio)) {
                    pedidoDAO.restaurarStock(id);
                }

                // Al ENTREGAR un pedido de Delivery se genera su Venta para que cuente en
                // Reportes. El stock ya bajó en la reserva, por eso ajustarStock=false.
                if ("Entregado".equalsIgnoreCase(estado)
                        && estadoPrevio != null
                        && !"Entregado".equalsIgnoreCase(estadoPrevio)
                        && !"Cancelado".equalsIgnoreCase(estadoPrevio)) {
                    generarVentaDeEntrega(req, actual);
                }

                boolean ok = pedidoDAO.actualizarEstado(id, estado);

                // Al entregar o cancelar, el repartidor (si tenia) vuelve a estar disponible.
                if (actual != null && actual.getIdRepartidor() != null
                        && ("Entregado".equalsIgnoreCase(estado) || "Cancelado".equalsIgnoreCase(estado))) {
                    repartidorDAO.actualizarEstado(actual.getIdRepartidor(), "Disponible");
                }
                ok(resp, mensaje("actualizado", ok));
            } else if ("repartidor".equals(accion)) {
                int idRepartidor = o.get("idRepartidor").getAsInt();
                boolean ok = pedidoDAO.asignarRepartidor(id, idRepartidor);
                repartidorDAO.actualizarEstado(idRepartidor, "En ruta");
                ok(resp, mensaje("asignado", ok));
            } else {
                error(resp, HttpServletResponse.SC_BAD_REQUEST, "Accion no soportada: " + accion);
            }
        } catch (NumberFormatException e) {
            error(resp, HttpServletResponse.SC_BAD_REQUEST, "Id de pedido invalido.");
        } catch (Exception e) {
            manejarError(resp, e);
        }
    }

    /** Total = Total - CostoEnvio (el envio es ingreso aparte); no vuelve a descontar stock. */
    private void generarVentaDeEntrega(HttpServletRequest req, Pedido pedido) throws Exception {
        if (pedido == null || pedido.getDetalles() == null || pedido.getDetalles().isEmpty()) {
            return;
        }
        Venta venta = new Venta();
        Integer idUsuario = (Integer) req.getSession().getAttribute("idUsuario");
        venta.setIdUsuario(idUsuario != null ? idUsuario : 0);
        venta.setMetodoPago("Delivery");
        venta.setEstado("Completada");

        BigDecimal envio = pedido.getCostoEnvio() != null ? pedido.getCostoEnvio() : BigDecimal.ZERO;
        BigDecimal totalProductos = (pedido.getTotal() != null ? pedido.getTotal() : BigDecimal.ZERO).subtract(envio);
        venta.setTotal(totalProductos);

        java.util.List<DetalleVenta> detallesVenta = new java.util.ArrayList<>();
        for (DetallePedido dp : pedido.getDetalles()) {
            DetalleVenta dv = new DetalleVenta();
            dv.setIdProducto(dp.getIdProducto());
            dv.setCantidad(dp.getCantidad());
            dv.setPrecioUnitario(dp.getPrecioUnitario());
            dv.setSubtotal(dp.getSubtotal());
            detallesVenta.add(dv);
        }
        venta.setDetalles(detallesVenta);

        ventaDAO.crear(venta, false);
    }
}
