package web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonObject;

import dao.DAOFactory;
import dao.interfaces.IUsuarioDAO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import modelo.Usuario;

/**
 * Recurso Usuarios (modulo Configuracion). Todas las operaciones exigen rol
 * Administrador. El hash de la contrasena NUNCA se devuelve al frontend.
 *
 *   GET  /api/usuarios                 -> lista (sin hash)
 *   POST /api/usuarios                 -> crear empleado
 *   PUT  /api/usuarios/{id}/password   -> restablecer contrasena  body: { "clave": "..." }
 *   PUT  /api/usuarios/{id}/desactivar -> desactivar (Estado = 0)
 */
@WebServlet("/api/usuarios/*")
public class UsuarioServlet extends ApiBaseServlet {

    private static final long serialVersionUID = 1L;
    private final transient IUsuarioDAO usuarioDAO = DAOFactory.usuarioDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!requiereAdmin(req, resp)) {
            return;
        }
        try {
            List<Map<String, Object>> salida = new ArrayList<>();
            for (Usuario u : usuarioDAO.listarTodos()) {
                salida.add(aMapaPublico(u));
            }
            ok(resp, salida);
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
            Usuario u = new Usuario();
            u.setNombres(texto(o, "nombres"));
            u.setApellidos(texto(o, "apellidos"));
            u.setDni(texto(o, "dni"));
            u.setUsername(texto(o, "username"));
            u.setEmail(o.has("email") && !o.get("email").isJsonNull() ? o.get("email").getAsString() : null);
            u.setRol(texto(o, "rol"));
            u.setEstado(true);

            String clave = texto(o, "clave");
            usuarioDAO.registrarUsuario(u, clave); // hashea la clave
            escribir(resp, HttpServletResponse.SC_CREATED, aMapaPublico(u));
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
            String[] partes = ruta(req).replaceFirst("^/", "").split("/");
            if (partes.length < 2) {
                error(resp, HttpServletResponse.SC_BAD_REQUEST, "Ruta invalida. Usa /{id}/password o /{id}/desactivar.");
                return;
            }
            int id = Integer.parseInt(partes[0]);
            String accion = partes[1];

            if ("password".equals(accion)) {
                String clave = texto(leerJson(req), "clave");
                if (clave.length() < 6) {
                    error(resp, HttpServletResponse.SC_BAD_REQUEST, "La contrasena debe tener al menos 6 caracteres.");
                    return;
                }
                boolean ok = usuarioDAO.actualizarContrasena(id, clave);
                ok(resp, mensaje("restablecida", ok));
            } else if ("desactivar".equals(accion)) {
                boolean ok = usuarioDAO.eliminar(id); // logico (Estado = 0)
                ok(resp, mensaje("desactivado", ok));
            } else if ("activar".equals(accion)) {
                boolean ok = usuarioDAO.reactivar(id); // Estado = 1
                ok(resp, mensaje("activado", ok));
            } else {
                error(resp, HttpServletResponse.SC_BAD_REQUEST, "Accion no soportada: " + accion);
            }
        } catch (NumberFormatException e) {
            error(resp, HttpServletResponse.SC_BAD_REQUEST, "Id de usuario invalido.");
        } catch (Exception e) {
            manejarError(resp, e);
        }
    }

    private Map<String, Object> aMapaPublico(Usuario u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("idUsuario", u.getIdUsuario());
        m.put("nombres", u.getNombres());
        m.put("apellidos", u.getApellidos());
        m.put("dni", u.getDni());
        m.put("username", u.getUsername());
        m.put("email", u.getEmail());
        m.put("rol", u.getRol());
        m.put("estado", u.isEstado());
        return m; // sin ContrasenaHash
    }

    private String texto(JsonObject o, String campo) {
        return (o.has(campo) && !o.get(campo).isJsonNull()) ? o.get(campo).getAsString().trim() : "";
    }
}
