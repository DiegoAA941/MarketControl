package web;

import java.io.IOException;

import com.google.gson.JsonObject;

import dao.DAOFactory;
import dao.interfaces.IConfiguracionDAO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Configuración del negocio (clave/valor). Solo Administrador.
 *
 *   GET /api/configuracion           -> todas las claves
 *   PUT /api/configuracion           -> body: { "clave": "telefono_whatsapp", "valor": "51987..." }
 *
 * El valor público (teléfono) lo expone además PublicServlet para la tienda.
 */
@WebServlet("/api/configuracion/*")
public class ConfiguracionServlet extends ApiBaseServlet {

    private static final long serialVersionUID = 1L;
    private final transient IConfiguracionDAO configDAO = DAOFactory.configuracionDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!requiereAdmin(req, resp)) {
            return;
        }
        try {
            ok(resp, configDAO.obtenerTodo());
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
            JsonObject o = leerJson(req);
            String clave = o.get("clave").getAsString().trim();
            String valor = o.has("valor") && !o.get("valor").isJsonNull() ? o.get("valor").getAsString().trim() : "";
            boolean ok = configDAO.guardar(clave, valor);
            ok(resp, mensaje("guardado", ok));
        } catch (Exception e) {
            manejarError(resp, e);
        }
    }
}
