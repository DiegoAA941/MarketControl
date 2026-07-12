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

    try { await cargarCatalogo(); } catch (e) { notificarError(e); }
    await mostrarVista('dashboard');

    actualizarBadgeRecojos();
    setInterval(actualizarBadgeRecojos, 20000);
}

document.addEventListener('DOMContentLoaded', iniciar);
