package web;

import java.io.IOException;

import com.google.gson.JsonObject;

import dao.DAOFactory;
import dao.interfaces.IRepartidorDAO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import modelo.Repartidor;

/**
 * Recurso Repartidores (modulo Delivery).
 *
 *   GET  /api/repartidores              -> todos
 *   GET  /api/repartidores/disponibles  -> solo los 'Disponible' (para el select de asignacion)
 *   POST /api/repartidores              -> crear (Admin)
 */
@WebServlet("/api/repartidores/*")
public class RepartidorServlet extends ApiBaseServlet {

    private static final long serialVersionUID = 1L;
    private final transient IRepartidorDAO repartidorDAO = DAOFactory.repartidorDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!requiereLogin(req, resp)) {
            return;
        }
        try {
            if ("/disponibles".equals(ruta(req))) {
                ok(resp, repartidorDAO.listarDisponibles());
            } else {
                ok(resp, repartidorDAO.listarTodos());
            }
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
            JsonObject o = leerJson(req);
            Repartidor r = new Repartidor();
            r.setNombres(o.get("nombres").getAsString());
            r.setTelefono(o.has("telefono") && !o.get("telefono").isJsonNull()
                    ? o.get("telefono").getAsString() : null);
            r.setEstado("Disponible");
            repartidorDAO.crear(r);
            escribir(resp, HttpServletResponse.SC_CREATED, r);
        } catch (Exception e) {
            manejarError(resp, e);
        }
    }

    /** DELETE /api/repartidores/{id} -> baja LOGICA (Estado = 'Inactivo'). */
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!requiereAdmin(req, resp)) {
            return;
        }
        try {
            int id = idDeRuta(req);
            if (id <= 0) {
                error(resp, HttpServletResponse.SC_BAD_REQUEST, "Falta el id del repartidor en la ruta.");
                return;
            }
            boolean ok = repartidorDAO.eliminar(id); // -> 'Inactivo'
            ok(resp, mensaje("inactivado", ok));
        } catch (Exception e) {
            manejarError(resp, e);
        }
    }
}
