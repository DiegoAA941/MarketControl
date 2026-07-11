package web;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import dao.DAOFactory;
import dao.interfaces.IProductoDAO;
import dao.interfaces.IVentaDAO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import modelo.DetalleVenta;
import modelo.Producto;
import modelo.Venta;

/**
 * Recurso Ventas (modulo POS y fuente de Reportes).
 *
 *   POST /api/ventas                       -> registra una venta (cajero en sesion)
 *       body: { "metodoPago": "Efectivo",
 *               "detalles": [ { "idProducto":1, "cantidad":2, "precioUnitario":4.50 } ] }
 *   GET  /api/ventas?desde=YYYY-MM-DD&hasta=YYYY-MM-DD  -> ventas del rango
 */
@WebServlet("/api/ventas/*")
public class VentaServlet extends ApiBaseServlet {

    private static final long serialVersionUID = 1L;
    private final transient IVentaDAO ventaDAO = DAOFactory.ventaDAO();
    private final transient IProductoDAO productoDAO = DAOFactory.productoDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!requiereLogin(req, resp)) {
            return;
        }
        try {
            HttpSession s = sesionActiva(req);
            JsonObject o = leerJson(req);

            Venta venta = new Venta();
            venta.setIdUsuario((Integer) s.getAttribute("idUsuario"));
            venta.setMetodoPago(o.has("metodoPago") && !o.get("metodoPago").isJsonNull()
                    ? o.get("metodoPago").getAsString() : "Efectivo");
            venta.setEstado("Completada");
            if (o.has("idPedido") && !o.get("idPedido").isJsonNull()) {
                venta.setIdPedidoOrigen(o.get("idPedido").getAsInt());
            }

            if (!o.has("detalles") || !o.get("detalles").isJsonArray()
                    || o.getAsJsonArray("detalles").isEmpty()) {
                error(resp, HttpServletResponse.SC_BAD_REQUEST, "La venta debe incluir al menos un producto.");
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

                DetalleVenta dv = new DetalleVenta();
                dv.setIdProducto(idProducto);
                dv.setCantidad(cantidad);
                dv.setPrecioUnitario(precio);
                dv.setSubtotal(subtotal);
                venta.getDetalles().add(dv);

                total = total.add(subtotal);
            }
            venta.setTotal(total);

            ventaDAO.crear(venta);
            escribir(resp, HttpServletResponse.SC_CREATED, venta);
        } catch (Exception e) {
            manejarError(resp, e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!requiereLogin(req, resp)) {
            return;
        }
        try {
            int id = idDeRuta(req);
            if (id > 0) {
                Venta v = ventaDAO.buscarPorId(id);
                if (v == null) {
                    error(resp, HttpServletResponse.SC_NOT_FOUND, "Venta no encontrada.");
                } else {
                    ok(resp, v);
                }
                return;
            }

            String desde = req.getParameter("desde");
            String hasta = req.getParameter("hasta");
            if (desde != null && hasta != null) {
                LocalDateTime ini = LocalDate.parse(desde).atStartOfDay();
                LocalDateTime fin = LocalDate.parse(hasta).atTime(LocalTime.MAX);
                ok(resp, ventaDAO.ventasPorRango(ini, fin));
            } else {
                ok(resp, ventaDAO.listarTodos());
            }
        } catch (Exception e) {
            manejarError(resp, e);
        }
    }
}
