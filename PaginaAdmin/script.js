/* MarketControl — Acceso (login). Autentica vía POST /api/auth/login;
   { credentials: 'include' } guarda la cookie de sesión que el dashboard reenvía después. */

// Misma base que dashboard.js. Ajusta si despliegas en otra URL/host.
const API_BASE = "http://localhost:8080/MarketControl";

const OJO_ABIERTO = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7-10-7-10-7z"/><circle cx="12" cy="12" r="3"/></svg>';
const OJO_CERRADO = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9.9 4.24A9.1 9.1 0 0 1 12 4c6.5 0 10 7 10 7a13.2 13.2 0 0 1-2.4 3.3"/><path d="M6.6 6.6A13.2 13.2 0 0 0 2 12s3.5 7 10 7a9.1 9.1 0 0 0 3.4-.6"/><path d="M14.1 14.1A3 3 0 0 1 9.9 9.9"/><path d="M2 2l20 20"/></svg>';

document.querySelectorAll('.ojo').forEach(btn => {
    btn.innerHTML = OJO_ABIERTO;
    btn.addEventListener('click', () => {
        const input = document.getElementById(btn.dataset.alternar);
        const mostrar = input.type === 'password';
        input.type = mostrar ? 'text' : 'password';
        btn.innerHTML = mostrar ? OJO_CERRADO : OJO_ABIERTO;
        btn.setAttribute('aria-label', mostrar ? 'Ocultar contraseña' : 'Mostrar contraseña');
    });
});

function notificar(id, texto, exito) {
    const elemento = document.getElementById(id);
    elemento.textContent = texto;
    elemento.className = 'mensaje mostrar ' + (exito ? 'exito' : 'error');
}

// Inicio de sesión (único acceso del sistema POS cerrado)
document.getElementById('acceso').addEventListener('submit', async (e) => {
    e.preventDefault();
    const usuario = document.getElementById('a-usuario').value.trim();
    const clave = document.getElementById('a-clave').value;

    if (!usuario || !clave) {
        notificar('acceso-msj', 'Completa usuario y contraseña.', false);
        return;
    }

    const boton = e.target.querySelector('.boton-enviar');
    boton.disabled = true;

    try {
        const resp = await fetch(API_BASE + '/api/auth/login', {
            method: 'POST',
            credentials: 'include',                 // <- guarda la cookie de sesión
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ usuario, clave }),
        });

        const texto = await resp.text();
        let data = null;
        if (texto) { try { data = JSON.parse(texto); } catch (_) { /* respuesta no-JSON */ } }

        if (resp.ok) {
            // Sesión activa en el backend; no usamos sessionStorage.
            notificar('acceso-msj', 'Acceso correcto. Redirigiendo…', true);
            window.location.href = 'dashboard.html';
        } else {
            const msg = (data && data.error) ? data.error : 'Usuario o contraseña incorrectos.';
            notificar('acceso-msj', msg, false);
            boton.disabled = false;
        }
    } catch (err) {
        notificar('acceso-msj',
            'No se pudo conectar con el servidor. Verifica que el backend (Tomcat) esté activo.', false);
        boton.disabled = false;
    }
});