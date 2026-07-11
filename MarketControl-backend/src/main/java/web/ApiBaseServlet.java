package web;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.JsonObject;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import util.JsonUtil;

/**
 * Servlet base: centraliza JSON, lectura del cuerpo y controles de sesion/rol.
 * Atributos de sesion usados: {@code idUsuario}, {@code rol}, {@code nombre}.
 */
public abstract class ApiBaseServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    // -------------------- Respuestas JSON --------------------

    protected void escribir(HttpServletResponse resp, int estado, Object datos) throws IOException {
        resp.setStatus(estado);
        resp.setContentType("application/json; charset=UTF-8");
        resp.getWriter().write(JsonUtil.toJson(datos));
    }

    protected void ok(HttpServletResponse resp, Object datos) throws IOException {
        escribir(resp, HttpServletResponse.SC_OK, datos);
    }

    protected void error(HttpServletResponse resp, int estado, String mensaje) throws IOException {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("error", mensaje);
        escribir(resp, estado, cuerpo);
    }

    protected Map<String, Object> mensaje(String clave, Object valor) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put(clave, valor);
        return m;
    }

    // -------------------- Lectura del cuerpo --------------------

    protected String leerCuerpo(HttpServletRequest req) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader lector = req.getReader()) {
            String linea;
            while ((linea = lector.readLine()) != null) {
                sb.append(linea);
            }
        }
        return sb.toString();
    }

    protected JsonObject leerJson(HttpServletRequest req) throws IOException {
        return JsonUtil.parse(leerCuerpo(req));
    }

    /** Devuelve el path extra ("/5", "/disponibles") o "" si no hay. */
    protected String ruta(HttpServletRequest req) {
        String p = req.getPathInfo();
        return (p == null) ? "" : p;
    }

    /** Extrae un id numerico desde la ruta (p. ej. "/12" -> 12). -1 si no aplica. */
    protected int idDeRuta(HttpServletRequest req) {
        String p = ruta(req);
        if (p.length() <= 1) {
            return -1;
        }
        String[] partes = p.substring(1).split("/");
        try {
            return Integer.parseInt(partes[0]);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    // -------------------- Sesion y roles --------------------

    protected HttpSession sesionActiva(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        return (s != null && s.getAttribute("idUsuario") != null) ? s : null;
    }

    /** Exige sesion. Devuelve false (y responde 401) si no hay. */
    protected boolean requiereLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (sesionActiva(req) == null) {
            error(resp, HttpServletResponse.SC_UNAUTHORIZED, "No autenticado. Inicia sesion.");
            return false;
        }
        return true;
    }

    /** Exige rol Administrador. Devuelve false (y responde 401/403) si no cumple. */
    protected boolean requiereAdmin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession s = sesionActiva(req);
        if (s == null) {
            error(resp, HttpServletResponse.SC_UNAUTHORIZED, "No autenticado.");
            return false;
        }
        if (!"Administrador".equalsIgnoreCase(String.valueOf(s.getAttribute("rol")))) {
            error(resp, HttpServletResponse.SC_FORBIDDEN, "Acceso restringido a Administradores.");
            return false;
        }
        return true;
    }

    /** Maneja excepciones de forma uniforme: 400 para validacion, 500 para el resto. */
    protected void manejarError(HttpServletResponse resp, Exception e) throws IOException {
        if (e instanceof IllegalArgumentException || e instanceof NullPointerException) {
            error(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } else {
            error(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Error interno del servidor: " + e.getMessage());
        }
    }
}
