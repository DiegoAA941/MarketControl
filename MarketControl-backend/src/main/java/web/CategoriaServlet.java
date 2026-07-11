package web;

import java.io.IOException;

import com.google.gson.JsonObject;

import dao.DAOFactory;
import dao.interfaces.ICategoriaDAO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import modelo.Categoria;

/**
 * Recurso Categorias (lo consumen el modal de Inventario y los reportes
 * por categoria).
 *
 *   GET    /api/categorias      -> todas
 *   POST   /api/categorias      -> crear (Admin)
 *   PUT    /api/categorias/{id} -> actualizar (Admin)
 *   DELETE /api/categorias/{id} -> desactivar (Admin, Estado=0)
 */
@WebServlet("/api/categorias/*")
public class CategoriaServlet extends ApiBaseServlet {

    private static final long serialVersionUID = 1L;
    private final transient ICategoriaDAO categoriaDAO = DAOFactory.categoriaDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!requiereLogin(req, resp)) {
            return;
        }
        try {
            ok(resp, categoriaDAO.listarTodos());
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
            Categoria c = new Categoria();
            c.setNombreCategoria(o.get("nombreCategoria").getAsString());
            c.setDescripcion(o.has("descripcion") && !o.get("descripcion").isJsonNull()
                    ? o.get("descripcion").getAsString() : null);
            c.setEstado(true);
            categoriaDAO.crear(c);
            escribir(resp, HttpServletResponse.SC_CREATED, c);
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
                error(resp, HttpServletResponse.SC_BAD_REQUEST, "Falta el id de la categoria en la ruta.");
                return;
            }
            Categoria existente = categoriaDAO.buscarPorId(id);
            if (existente == null) {
                error(resp, HttpServletResponse.SC_NOT_FOUND, "Categoria no encontrada.");
                return;
            }
            JsonObject o = leerJson(req);
            if (o.has("nombreCategoria") && !o.get("nombreCategoria").isJsonNull()) {
                existente.setNombreCategoria(o.get("nombreCategoria").getAsString());
            }
            if (o.has("descripcion") && !o.get("descripcion").isJsonNull()) {
                existente.setDescripcion(o.get("descripcion").getAsString());
            }
            if (o.has("estado") && !o.get("estado").isJsonNull()) {
                existente.setEstado(o.get("estado").getAsBoolean());
            }
            categoriaDAO.actualizar(existente);
            ok(resp, existente);
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
                error(resp, HttpServletResponse.SC_BAD_REQUEST, "Falta el id de la categoria en la ruta.");
                return;
            }
            boolean ok = categoriaDAO.eliminar(id); // logico (Estado = 0)
            ok(resp, mensaje("desactivada", ok));
        } catch (Exception e) {
            manejarError(resp, e);
        }
    }
}
