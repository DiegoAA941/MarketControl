/* ---------- 13. ARRANQUE ---------- */
async function iniciar() {
    try { usuarioActual = await api('/api/auth/sesion'); }
    catch (e) { window.location.href = 'index.html'; return; }

    document.body.classList.toggle('rol-empleado', usuarioActual.rol === 'Empleado');

    pintarCabecera();
    pintarNav();
    activarMenuMovil();
    activarNavegacion();
    activarInventario();
    activarPOS();
    activarRecojos();
    activarConfig();
    activarReportes();
    activarDelivery();
    activarRepartidores();
    activarDeteccionInactividad();

    try { await cargarCatalogo(); } catch (e) { notificarError(e); }
    await mostrarVista('dashboard');

    actualizarBadgeRecojos();
    setInterval(actualizarBadgeRecojos, 20000);
}

/* ---------- 14. CIERRE POR INACTIVIDAD REAL DEL USUARIO ----------
    Este temporizador cuenta solo interaccion real (clic, teclado, mouse, touch) 
    y cierra sesion por su cuenta si no hay ninguna durante INACTIVIDAD_LIMITE_MS. */
const INACTIVIDAD_LIMITE_MS = 15 * 60 * 1000;
let temporizadorInactividad;

function reiniciarTemporizadorInactividad() {
    clearTimeout(temporizadorInactividad);
    temporizadorInactividad = setTimeout(cerrarPorInactividad, INACTIVIDAD_LIMITE_MS);
}

async function cerrarPorInactividad() {
    try { await api('/api/auth/logout', { method: 'POST' }); } catch (_) { /* da igual, igual redirige */ }
    window.location.href = 'index.html';
}

function activarDeteccionInactividad() {
    ['click', 'keydown', 'mousemove', 'scroll', 'touchstart'].forEach((evento) => {
        document.addEventListener(evento, reiniciarTemporizadorInactividad, { passive: true });
    });
    reiniciarTemporizadorInactividad();
}

document.addEventListener('DOMContentLoaded', iniciar);
