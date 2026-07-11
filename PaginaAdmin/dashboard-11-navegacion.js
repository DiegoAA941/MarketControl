/* ---------- 12. NAVEGACIÓN / MENÚ ---------- */
function activarMenuMovil() {
    const lateral = document.getElementById('lateral');
    const velo = document.getElementById('velo');
    const boton = document.getElementById('menu-boton');
    boton.addEventListener('click', () => { lateral.classList.add('abierta'); velo.hidden = false; boton.setAttribute('aria-expanded', 'true'); });
    velo.addEventListener('click', () => { lateral.classList.remove('abierta'); velo.hidden = true; boton.setAttribute('aria-expanded', 'false'); });
    document.addEventListener('keydown', (e) => { if (e.key === 'Escape') { lateral.classList.remove('abierta'); velo.hidden = true; } });
}

const VISTAS = ['dashboard', 'inventario', 'pos', 'delivery', 'reportes', 'config'];
async function mostrarVista(id) {
    const activa = VISTAS.includes(id) ? id : 'dashboard';
    document.getElementById('vista-dashboard').hidden = activa !== 'dashboard';
    document.getElementById('vista-inventario').hidden = activa !== 'inventario';
    document.getElementById('vista-pos').hidden = activa !== 'pos';
    document.getElementById('vista-delivery').hidden = activa !== 'delivery';
    document.getElementById('vista-reportes').hidden = activa !== 'reportes';
    document.getElementById('vista-config').hidden = activa !== 'config';

    try {
        if (activa === 'dashboard') await cargarDashboard();
        else if (activa === 'inventario') { await cargarCatalogo(); pintarInventario(); }
        else if (activa === 'pos') { await cargarCatalogo(); pintarPosProductos(); pintarPosCarrito(); }
        else if (activa === 'delivery') await cargarDelivery();
        else if (activa === 'reportes') await cargarReportes();
        else if (activa === 'config') await cargarConfig();
    } catch (e) { notificarError(e); }

    window.scrollTo({ top: 0, behavior: 'auto' });
}

function activarNavegacion() {
    document.getElementById('nav').addEventListener('click', (e) => {
        const btn = e.target.closest('.nav__enlace');
        if (!btn) return;
        document.querySelectorAll('.nav__enlace').forEach((b) => b.removeAttribute('aria-current'));
        btn.setAttribute('aria-current', 'page');
        mostrarVista(btn.dataset.id);
        document.getElementById('lateral').classList.remove('abierta');
        document.getElementById('velo').hidden = true;
    });
    document.getElementById('cerrar-sesion').addEventListener('click', async () => {
        try { await api('/api/auth/logout', { method: 'POST' }); } catch (_) {}
        window.location.href = 'index.html';
    });
}
