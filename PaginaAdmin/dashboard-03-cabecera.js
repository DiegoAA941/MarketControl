/* ---------- 4. CABECERA / NAV ---------- */
function pintarCabecera() {
    const fecha = new Date().toLocaleDateString('es-PE', { weekday: 'long', day: 'numeric', month: 'long' });
    document.getElementById('fecha').textContent = fecha.charAt(0).toUpperCase() + fecha.slice(1);

    document.getElementById('b-nombre').textContent = usuarioActual.nombre;
    document.getElementById('b-rol').textContent = usuarioActual.rol;

    const inicial = (usuarioActual.nombre || 'U').trim().charAt(0).toUpperCase();
    document.getElementById('usuario-mini').innerHTML = `
        <span class="usuario-mini__avatar" aria-hidden="true">${esc(inicial)}</span>
        <span class="usuario-mini__datos">
            <strong>${esc(usuarioActual.nombre)}</strong>
            <small>${esc(usuarioActual.rol)}</small>
        </span>
        <span class="usuario-mini__icono" aria-hidden="true">${svg('persona', 16)}</span>`;
}

function pintarNav() {
    const ocultas = ['reportes', 'config'];
    const visibles = usuarioActual.rol === 'Empleado' ? NAV.filter((i) => !ocultas.includes(i.id)) : NAV;
    document.getElementById('nav').innerHTML = visibles.map((item, idx) => `
        <li>
            <button type="button" class="nav__enlace" data-id="${item.id}" ${idx === 0 ? 'aria-current="page"' : ''}>
                ${svg(item.icono)}
                <span class="nav__etiqueta">${esc(item.etiqueta)}</span>
                <span class="nav__chevron" aria-hidden="true">${svg('chevron', 18)}</span>
            </button>
        </li>`).join('');
}
