/* ---------- 9. CONFIGURACIÓN ---------- */
async function cargarConfig() {
    // Datos del negocio (compartidos con la tienda vía /api/configuracion)
    const tel = document.getElementById('cfg-telefono');
    try {
        const cfg = await api('/api/configuracion');
        if (cfg) {
            if (cfg.telefono_whatsapp) tel.value = cfg.telefono_whatsapp;
            if (cfg.direccion) document.getElementById('cfg-direccion').value = cfg.direccion;
            if (cfg.costo_envio != null) document.getElementById('cfg-costo-envio').value = cfg.costo_envio;
            if (cfg.envio_gratis_desde != null) document.getElementById('cfg-envio-gratis').value = cfg.envio_gratis_desde;
        }
    } catch (_) {}

    // Información del sistema
    const filas = [
        ['Sistema', 'MarketControl'],
        ['Versión', '2.0.0'],
        ['Moneda', 'Soles Peruanos (S/)'],
        ['IGV', '18%'],
        ['Almacenamiento', 'SQL Server (API REST)'],
        ['Usuario actual', `${usuarioActual.nombre} (${usuarioActual.rol})`],
    ];
    document.getElementById('cfg-sistema').innerHTML = filas.map((f) =>
        `<div class="cfg-info__fila"><span>${esc(f[0])}</span><span>${esc(f[1])}</span></div>`).join('');

    // Usuarios del sistema
    const cont = document.getElementById('cfg-usuarios');
    try {
        usuariosCache = await api('/api/usuarios');
        cont.innerHTML = usuariosCache.map((u) => {
            const nombre = `${u.nombres} ${u.apellidos}`.trim();
            const ini = (u.nombres || '?').charAt(0).toUpperCase() + (u.apellidos || '').charAt(0).toUpperCase();
            const esAdmin = (u.rol || '').toLowerCase() === 'administrador';
            const badge = esAdmin
                ? `<span class="badge-rol badge-rol--admin">${svg('corona', 14)}Admin</span>`
                : `<span class="badge-rol badge-rol--empleado">Empleado</span>`;
            const inactivo = u.estado === false ? `<span class="cfg-user__inactivo">(inactivo)</span>` : '';
            const accionEstado = u.estado === false
                ? `<button type="button" class="cfg-menu__item" data-accion="activar" data-id="${u.idUsuario}">Reactivar usuario</button>`
                : `<button type="button" class="cfg-menu__item cfg-menu__item--peligro" data-accion="desactivar" data-id="${u.idUsuario}">Desactivar</button>`;
            return `<div class="cfg-user" data-id="${u.idUsuario}">
                <span class="cfg-user__avatar" aria-hidden="true">${esc(ini)}</span>
                <div class="cfg-user__datos">
                    <div class="cfg-user__nombre">${esc(nombre)}${inactivo}</div>
                    <div class="cfg-user__meta">@${esc(u.username)} · DNI ${esc(u.dni)}</div>
                </div>
                ${badge}
                <div class="cfg-menu">
                    <button type="button" class="cfg-menu__boton" data-accion="menu" aria-label="Opciones de ${esc(nombre)}">${svg('puntos', 20)}</button>
                    <div class="cfg-menu__lista">
                        ${accionEstado}
                    </div>
                </div>
            </div>`;
        }).join('');
    } catch (e) { notificarError(e); }
}

function activarConfig() {
    // Cambiar contraseña centralizada: el admin indica el username del usuario
    document.getElementById('form-clave').addEventListener('submit', async (e) => {
        e.preventDefault();
        const username = document.getElementById('cl-username').value.trim();
        const nueva = document.getElementById('cl-nueva').value;
        const conf = document.getElementById('cl-confirmar').value;
        if (!username) { alert('Indica el username del usuario.'); return; }
        if (nueva.length < 6) { alert('La nueva contraseña debe tener al menos 6 caracteres.'); return; }
        if (nueva !== conf) { alert('Las contraseñas no coinciden.'); return; }
        const destino = usuariosCache.find((u) => (u.username || '').toLowerCase() === username.toLowerCase());
        if (!destino) { alert(`No existe un usuario con username "${username}".`); return; }
        try {
            await api(`/api/usuarios/${destino.idUsuario}/password`, { method: 'PUT', body: { clave: nueva } });
            e.target.reset();
            const correo = simularCorreoSeguridad(destino);
            mostrarToast(`Contraseña de @${destino.username} actualizada · correo enviado a ${correo}`);
        } catch (err) { notificarError(err); }
    });

    // Nuevo empleado (modal)
    const modal = document.getElementById('modal-empleado');
    const abrir = () => { document.getElementById('form-empleado').reset(); modal.hidden = false; document.getElementById('e-nombres').focus(); };
    const cerrar = () => { modal.hidden = true; };
    document.getElementById('btn-nuevo-emp').addEventListener('click', abrir);
    document.getElementById('emp-cerrar').addEventListener('click', cerrar);
    document.getElementById('emp-cancelar').addEventListener('click', cerrar);
    modal.addEventListener('click', (e) => { if (e.target === e.currentTarget) cerrar(); });

    document.getElementById('form-empleado').addEventListener('submit', async (e) => {
        e.preventDefault();
        const f = e.target;
        const payload = {
            nombres: f.nombres.value.trim(),
            apellidos: f.apellidos.value.trim(),
            dni: f.dni.value.trim(),
            username: f.usuario.value.trim(),
            clave: f.clave.value,
            rol: f.rol.value,
        };
        try {
            await api('/api/usuarios', { method: 'POST', body: payload });
            cerrar();
            await cargarConfig();
            mostrarToast('Empleado registrado');
        } catch (err) { notificarError(err); }
    });

    // Acciones de usuarios (menú 3 puntos)
    document.getElementById('cfg-usuarios').addEventListener('click', async (e) => {
        const btn = e.target.closest('[data-accion]');
        if (!btn) return;
        const accion = btn.dataset.accion;
        if (accion === 'menu') {
            const menu = btn.closest('.cfg-menu');
            const abierto = menu.classList.contains('cfg-menu--abierto');
            document.querySelectorAll('.cfg-menu--abierto').forEach((m) => m.classList.remove('cfg-menu--abierto'));
            if (!abierto) menu.classList.add('cfg-menu--abierto');
            return;
        }
        const id = Number(btn.dataset.id);
        if (accion === 'desactivar') {
            if (!confirm('¿Desactivar este usuario? (Estado = 0, no se elimina)')) return;
            try { await api(`/api/usuarios/${id}/desactivar`, { method: 'PUT', body: {} }); await cargarConfig(); mostrarToast('Usuario desactivado'); }
            catch (err) { notificarError(err); }
        } else if (accion === 'activar') {
            try { await api(`/api/usuarios/${id}/activar`, { method: 'PUT', body: {} }); await cargarConfig(); mostrarToast('Usuario reactivado'); }
            catch (err) { notificarError(err); }
        }
    });

    // Guardar datos del negocio en el backend (dirección + teléfono + envío) — compartidos con la tienda
    document.getElementById('btn-guardar-tel').addEventListener('click', async () => {
        const direccion = document.getElementById('cfg-direccion').value.trim();
        const tel = document.getElementById('cfg-telefono').value.trim();
        const costoEnvio = document.getElementById('cfg-costo-envio').value.trim() || '0';
        const envioGratis = document.getElementById('cfg-envio-gratis').value.trim() || '0';
        if (!direccion) { alert('Ingresa la dirección del negocio.'); return; }
        if (!tel) { alert('Ingresa un número de teléfono.'); return; }
        try {
            await api('/api/configuracion', { method: 'PUT', body: { clave: 'direccion', valor: direccion } });
            await api('/api/configuracion', { method: 'PUT', body: { clave: 'telefono_whatsapp', valor: tel } });
            await api('/api/configuracion', { method: 'PUT', body: { clave: 'costo_envio', valor: costoEnvio } });
            await api('/api/configuracion', { method: 'PUT', body: { clave: 'envio_gratis_desde', valor: envioGratis } });
            mostrarToast('Datos del negocio guardados');
        } catch (err) { notificarError(err); }
    });

    document.addEventListener('click', (e) => {
        if (!e.target.closest('.cfg-menu')) {
            document.querySelectorAll('.cfg-menu--abierto').forEach((m) => m.classList.remove('cfg-menu--abierto'));
        }
    });
}

/* Simula el envío de un correo de notificación de seguridad al administrador. */
function simularCorreoSeguridad(usuarioDestino) {
    const admin = usuariosCache.find((u) => u.idUsuario === usuarioActual.idUsuario);
    const correoAdmin = (admin && admin.email) ? admin.email : 'el correo del administrador';
    // En producción aquí iría una llamada real a un servicio de correo.
    console.info('[CORREO SIMULADO] Para: ' + correoAdmin +
        ' | Asunto: Contraseña actualizada | Cuerpo: Se cambió la contraseña de @' +
        usuarioDestino.username + ' el ' + fechaPeruAhora() + '.');
    return correoAdmin;
}
