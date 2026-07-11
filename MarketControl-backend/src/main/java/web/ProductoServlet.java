package web;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import com.google.gson.JsonObject;

import dao.DAOFactory;
import dao.interfaces.IProductoDAO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import modelo.Producto;

/**
 * Recurso Productos (modulo Inventario / POS).
 *
 *   GET    /api/productos                -> lista (todos)
 *   GET    /api/productos?stockBajo=true -> productos en o bajo el minimo
 *   GET    /api/productos?q=texto        -> busqueda por nombre
 *   GET    /api/productos?categoria=2    -> por categoria
 *   GET    /api/productos/{id}           -> uno
 *   POST   /api/productos                -> crear (Admin)
 *   PUT    /api/productos/{id}           -> actualizar (Admin)
 *   DELETE /api/productos/{id}           -> desactivar (Admin, Estado=0)
 */
@WebServlet("/api/productos/*")
public class ProductoServlet extends ApiBaseServlet {

    private static final long serialVersionUID = 1L;
    private final transient IProductoDAO productoDAO = DAOFactory.productoDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!requiereLogin(req, resp)) {
            return;
        }
        try {
            int id = idDeRuta(req);
            if (id > 0) {
                Producto p = productoDAO.buscarPorId(id);
                if (p == null) {
                    error(resp, HttpServletResponse.SC_NOT_FOUND, "Producto no encontrado.");
                } else {
                    ok(resp, p);
                }
                return;
            }

            String q = req.getParameter("q");
            String categoria = req.getParameter("categoria");
            List<Producto> lista;
            if ("true".equalsIgnoreCase(req.getParameter("stockBajo"))) {
                lista = productoDAO.listarStockBajo();
            } else if (q != null && !q.isBlank()) {
                lista = productoDAO.buscarPorNombre(q);
            } else if (categoria != null && !categoria.isBlank()) {
                lista = productoDAO.listarPorCategoria(Integer.parseInt(categoria));
            } else {
                lista = productoDAO.listarTodos();
            }
            ok(resp, lista);
        } catch (Exception e) {
            manejarError(resp, e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!requiereAdmin(req, resp)) {
            return;
        }
        try {
            Producto p = construir(leerJson(req), new Producto());
            productoDAO.crear(p);
            escribir(resp, HttpServletResponse.SC_CREATED, p);
        } catch (Exception e) {
            manejarError(resp, e);
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!requiereAdmin(req, resp)) {
            return;
        }
        try {
            int id = idDeRuta(req);
            if (id <= 0) {
                error(resp, HttpServletResponse.SC_BAD_REQUEST, "Falta el id del producto en la ruta.");
                return;
            }

            // Subruta: PUT /api/productos/{id}/stock  -> agregar stock rapido
            String[] partes = ruta(req).replaceFirst("^/", "").split("/");
            if (partes.length >= 2 && "stock".equals(partes[1])) {
                int cantidad = leerJson(req).get("cantidad").getAsInt();
                Integer idUsuario = (Integer) req.getSession().getAttribute("idUsuario");
                int resultante = productoDAO.agregarStock(id, cantidad, idUsuario);
                ok(resp, mensaje("stockActual", resultante));
                return;
            }

            Producto existente = productoDAO.buscarPorId(id);
            if (existente == null) {
                error(resp, HttpServletResponse.SC_NOT_FOUND, "Producto no encontrado.");
                return;
            }
            Producto p = construir(leerJson(req), existente);
            p.setIdProducto(id);
            productoDAO.actualizar(p);
            ok(resp, p);
        } catch (Exception e) {
            manejarError(resp, e);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!requiereAdmin(req, resp)) {
            return;
        }
        try {
            int id = idDeRuta(req);
            if (id <= 0) {
                error(resp, HttpServletResponse.SC_BAD_REQUEST, "Falta el id del producto en la ruta.");
                return;
            }
            boolean ok = productoDAO.eliminar(id); // logico (Estado = 0)
            ok(resp, mensaje("desactivado", ok));
        } catch (Exception e) {
            manejarError(resp, e);
        }
    }

    /** Construye/actualiza un Producto desde el JSON (los ausentes conservan su valor previo). */
    private Producto construir(JsonObject o, Producto base) {
        base.setIdCategoria(entero(o, "idCategoria", base.getIdCategoria()));
        base.setCodigoSku(texto(o, "codigoSku", base.getCodigoSku() != null ? base.getCodigoSku() : ""));
        base.setNombreProducto(texto(o, "nombreProducto", base.getNombreProducto() != null ? base.getNombreProducto() : ""));
        base.setDescripcion(texto(o, "descripcion", base.getDescripcion()));
        base.setMarca(texto(o, "marca", base.getMarca()));
        base.setImagenUrl(texto(o, "imagenUrl", base.getImagenUrl()));
        base.setPrecioCompra(decimal(o, "precioCompra", base.getPrecioCompra()));
        base.setPrecioVenta(decimal(o, "precioVenta", base.getPrecioVenta()));
        base.setPorcentajeDescuento(decimal(o, "porcentajeDescuento",
                base.getPorcentajeDescuento() != null ? base.getPorcentajeDescuento() : BigDecimal.ZERO));
        base.setStockActual(entero(o, "stockActual", base.getStockActual()));
        base.setStockMinimo(entero(o, "stockMinimo", base.getStockMinimo() > 0 ? base.getStockMinimo() : 5));
        base.setEstado(booleano(o, "estado", base.isEstado()));
        return base;
    }

    private String texto(JsonObject o, String c, String def) {
        return (o.has(c) && !o.get(c).isJsonNull()) ? o.get(c).getAsString() : def;
    }

    private int entero(JsonObject o, String c, int def) {
        return (o.has(c) && !o.get(c).isJsonNull()) ? o.get(c).getAsInt() : def;
    }

    private BigDecimal decimal(JsonObject o, String c, BigDecimal def) {
        return (o.has(c) && !o.get(c).isJsonNull()) ? new BigDecimal(o.get(c).getAsString()) : def;
    }

    private boolean booleano(JsonObject o, String c, boolean def) {
        return (o.has(c) && !o.get(c).isJsonNull()) ? o.get(c).getAsBoolean() : def;
    }
}
