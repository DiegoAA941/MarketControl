package web;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filtro CORS para permitir que el frontend (servido en otro origen, p. ej.
 * Live Server en http://127.0.0.1:5500) consuma la API con cookies de sesion.
 *
 * Como se usan credenciales (la cookie JSESSIONID), NO se puede responder con
 * {@code Access-Control-Allow-Origin: *}: hay que reflejar el Origin concreto
 * y habilitar {@code Allow-Credentials}. El frontend debe llamar con
 * {@code fetch(url, { credentials: 'include' })}.
 */
@WebFilter("/*")
public class CorsFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String origen = req.getHeader("Origin");
        if (origen != null) {
            resp.setHeader("Access-Control-Allow-Origin", origen);
            resp.setHeader("Vary", "Origin");
            resp.setHeader("Access-Control-Allow-Credentials", "true");
            resp.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            resp.setHeader("Access-Control-Allow-Headers", "Content-Type");
            resp.setHeader("Access-Control-Max-Age", "3600");
        }

        // Respuesta a la peticion de pre-vuelo (preflight)
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
            return;
        }

        chain.doFilter(request, response);
    }
}
