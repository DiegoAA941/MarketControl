package web;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.JsonObject;

import dao.DAOFactory;
import dao.interfaces.IUsuarioDAO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import modelo.Usuario;
import util.IntentosLoginUtil;
import util.PasswordUtil;

/**
 * Autenticacion del sistema POS cerrado.
 *
 *   POST /api/auth/login   body: { "usuario": "...", "clave": "..." }
 *   POST /api/auth/logout
 *   GET  /api/auth/sesion  -> usuario en sesion o 401
 *
 * Al iniciar sesion se guardan en {@link HttpSession} los atributos
 * {@code idUsuario}, {@code rol} y {@code nombre}; con ellos el resto de
 * servlets aplican los controles de acceso.
 */
@WebServlet("/api/auth/*")
public class AuthServlet extends ApiBaseServlet {

    private static final long serialVersionUID = 1L;
    private final transient IUsuarioDAO usuarioDAO = DAOFactory.usuarioDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String ruta = ruta(req);
        try {
            if ("/login".equals(ruta)) {
                login(req, resp);
            } else if ("/logout".equals(ruta)) {
                logout(req, resp);
            } else {
                error(resp, HttpServletResponse.SC_NOT_FOUND, "Ruta de autenticacion no encontrada.");
            }
        } catch (Exception e) {
            manejarError(resp, e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if ("/sesion".equals(ruta(req))) {
            HttpSession s = sesionActiva(req);
            if (s == null) {
                error(resp, HttpServletResponse.SC_UNAUTHORIZED, "No hay sesion activa.");
                return;
            }
            ok(resp, datosSesion(s));
        } else {
            error(resp, HttpServletResponse.SC_NOT_FOUND, "Ruta no encontrada.");
        }
    }

    private void login(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        JsonObject body = leerJson(req);
        String username = obtener(body, "usuario");
        String clave = obtener(body, "clave");

        if (username.isBlank() || clave.isBlank()) {
            error(resp, HttpServletResponse.SC_BAD_REQUEST, "Completa usuario y contrasena.");
            return;
        }

        long bloqueoSeg = IntentosLoginUtil.segundosBloqueoRestante(username);
        if (bloqueoSeg > 0) {
            long minutos = (bloqueoSeg + 59) / 60;
            error(resp, 429, "Demasiados intentos fallidos. Intenta de nuevo en "
                    + minutos + " minuto(s).");
            return;
        }

        Usuario u = usuarioDAO.buscarPorUsername(username);
        if (u == null || !u.isEstado() || !PasswordUtil.verificar(clave, u.getContrasenaHash())) {
            IntentosLoginUtil.registrarFallo(username);
            int restantes = IntentosLoginUtil.intentosRestantes(username);
            String msg = restantes > 0
                    ? "Usuario o contrasena incorrectos. Intentos disponibles: " + restantes + "."
                    : "Demasiados intentos fallidos. Intenta de nuevo en "
                        + IntentosLoginUtil.BLOQUEO_MINUTOS + " minuto(s).";
            error(resp, HttpServletResponse.SC_UNAUTHORIZED, msg);
            return;
        }

        IntentosLoginUtil.registrarExito(username);

        // Sesion nueva y limpia para evitar fijacion de sesion
        HttpSession previa = req.getSession(false);
        if (previa != null) {
            previa.invalidate();
        }
        HttpSession s = req.getSession(true);
        s.setAttribute("idUsuario", u.getIdUsuario());
        s.setAttribute("rol", u.getRol());
        s.setAttribute("nombre", u.getNombres() + " " + u.getApellidos());

        ok(resp, datosSesion(s));
    }

    private void logout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession s = req.getSession(false);
        if (s != null) {
            s.invalidate();
        }
        ok(resp, mensaje("ok", true));
    }

    private Map<String, Object> datosSesion(HttpSession s) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("idUsuario", s.getAttribute("idUsuario"));
        datos.put("nombre", s.getAttribute("nombre"));
        datos.put("rol", s.getAttribute("rol"));
        return datos;
    }

    private String obtener(JsonObject o, String campo) {
        return (o.has(campo) && !o.get(campo).isJsonNull()) ? o.get(campo).getAsString().trim() : "";
    }
}
