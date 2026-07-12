/* ---------- 11. DELIVERY ---------- */
let delTab = 'Todos';
let delPrevPendientes = null;

function beep() {
    try {
        const Ctx = window.AudioContext || window.webkitAudioContext;
        const ac = new Ctx();
        const o = ac.createOscillator(), g = ac.createGain();
        o.connect(g); g.connect(ac.destination);
        o.frequency.value = 880; g.gain.value = 0.05; o.start();
        setTimeout(() => { o.stop(); ac.close(); }, 250);
    } catch (_) { /* sin audio disponible */ }
}

async function cargarDelivery() {
    let base = [];
    try { base = await api('/api/pedidos?tipo=Delivery'); } catch (e) { notificarError(e); return; }
    // Trae el detalle de cada pedido (productos + repartidor). Usamos allSettled:
    // si un pedido falla, los demas conservan su detalle (antes "Productos --").
    const resultados = await Promise.allSettled(base.map((p) => api('/api/pedidos/' + p.idPedido)));
    const pedidos = resultados.map((r, i) => (r.status === 'fulfilled' ? r.value : base[i]));

    const activos = pedidos.filter((p) => !['Entregado', 'Cancelado'].includes(p.estado));
    const entregadosHoy = pedidos.filter((p) => p.estado === 'Entregado' && esHoy(p.fechaPedido));
    const ingresos = entregadosHoy.reduce((s, p) => s + Number(p.costoEnvio || 0), 0);
    document.getElementById('del-activos').textContent = activos.length;
    document.getElementById('del-entregados').textContent = entregadosHoy.length;
    document.getElementById('del-ingresos').textContent = soles(ingresos);

    // Alerta de nuevo pendiente
    const pendientes = pedidos.filter((p) => p.estado === 'Pendiente').length;
    const alerta = document.getElementById('del-alerta');
    if (delPrevPendientes !== null && pendientes > delPrevPendientes) {
        alerta.innerHTML = `${svg('camion', 18)}<span>¡Nuevo pedido pendiente! Hay ${pendientes} esperando preparación.</span>`;
        alerta.hidden = false;
        beep();
        setTimeout(() => { alerta.hidden = true; }, 6000);
    }
    delPrevPendientes = pendientes;

    // Pestañas con conteo
    const grupos = [
        ['Todos', pedidos.length],
        ['Pendiente', pedidos.filter((p) => p.estado === 'Pendiente').length],
        ['Preparando', pedidos.filter((p) => p.estado === 'Preparando').length],
        ['En Tránsito', pedidos.filter((p) => p.estado === 'En Tránsito').length],
        ['Entregado', pedidos.filter((p) => p.estado === 'Entregado').length],
    ];
    const etiquetas = { Todos: 'Todos', Pendiente: 'Pendientes', Preparando: 'Preparando', 'En Tránsito': 'En Tránsito', Entregado: 'Entregados' };
    document.getElementById('del-tabs').innerHTML = grupos.map(([id, n]) =>
        `<button type="button" class="pill ${id === delTab ? 'pill--activo' : ''}" data-tab="${id}">${etiquetas[id]} (${n})</button>`).join('');

    // Repartidores disponibles (para asignar)
    let disponibles = [];
    try { disponibles = await api('/api/repartidores/disponibles'); } catch (_) {}

    const lista = delTab === 'Todos' ? pedidos : pedidos.filter((p) => p.estado === delTab);
    const cont = document.getElementById('del-lista');
    if (!lista.length) { cont.innerHTML = `<div class="vacio">${svg('camion', 36)}<p>No hay pedidos en esta categoría.</p></div>`; return; }

    cont.innerHTML = lista.map((p) => tarjetaPedido(p, disponibles)).join('');
}

function tarjetaPedido(p, disponibles) {
    const clase = ESTADO_CLASE[p.estado] || 'estado--pendiente';
    const productos = (p.detalles || []).map((d) =>
        `${d.cantidad}× ${esc(nombrePorProducto[d.idProducto] || ('Producto #' + d.idProducto))}`).join('<br>') || '<span class="del-bloque__valor">—</span>';
    const repartidor = p.repartidor ? `<div class="del-repartidor">Repartidor: <strong>${esc(p.repartidor.nombres)}</strong></div>` : '';
    const nota = p.observaciones ? `<div class="del-nota">${esc(p.observaciones)}</div>` : '';

    let acciones = '';
    if (p.estado === 'Pendiente') {
        acciones = `
            <button type="button" class="del-btn del-btn--negro" data-accion="estado" data-id="${p.idPedido}" data-estado="Preparando">Iniciar Preparación</button>
            <button type="button" class="del-btn del-btn--rojo" data-accion="estado" data-id="${p.idPedido}" data-estado="Cancelado">Cancelar</button>`;
    } else if (p.estado === 'Preparando') {
        if (disponibles.length) {
            const opts = disponibles.map((r) => `<option value="${r.idRepartidor}">${esc(r.nombres)}</option>`).join('');
            acciones = `<select class="del-select" id="del-rep-${p.idPedido}"><option value="">Repartidor disponible…</option>${opts}</select>
               <button type="button" class="del-btn del-btn--azul" data-accion="enviar" data-id="${p.idPedido}">Enviar (En Tránsito)</button>
               <button type="button" class="del-btn del-btn--rojo" data-accion="estado" data-id="${p.idPedido}" data-estado="Cancelado">Cancelar</button>`;
        } else {
            acciones = `<div class="del-sinrep">${svg('moto', 16)}<span>No hay repartidores disponibles.</span>
                <button type="button" class="del-btn del-btn--negro" data-accion="abrir-repartidores">Registrar repartidor</button></div>
               <button type="button" class="del-btn del-btn--rojo" data-accion="estado" data-id="${p.idPedido}" data-estado="Cancelado">Cancelar</button>`;
        }
    } else if (p.estado === 'En Tránsito') {
        acciones = `<button type="button" class="del-btn del-btn--negro" data-accion="estado" data-id="${p.idPedido}" data-estado="Entregado">Marcar Entregado</button>`;
    }

    const cli = p.cliente;
    const clienteHtml = cli
        ? `<div class="del-bloque__valor"><strong>${esc(cli.nombres)}</strong></div>
           ${cli.telefono ? `<div class="del-bloque__sub">${svg('telefono', 12)} ${esc(cli.telefono)}</div>` : ''}
           ${cli.direccion ? `<div class="del-bloque__sub">${svg('pin', 12)} ${esc(cli.direccion)}${cli.referencia ? ' · ' + esc(cli.referencia) : ''}</div>` : ''}`
        : `<div class="del-bloque__valor">Cliente #${esc(p.idCliente)}</div>`;

    return `<article class="del-card">
        <div class="del-card__cab">
            <div>
                <div class="del-card__id">PD-${zfill(p.idPedido)} <span class="estado ${clase}">${esc(p.estado)}</span></div>
                <div class="del-card__fecha">${esc(fmtFecha(p.fechaPedido))}</div>
            </div>
            <div>
                <div class="del-card__total">${soles(p.total)}</div>
                ${Number(p.costoEnvio) > 0 ? `<div class="del-card__envio">incl. envío ${soles(p.costoEnvio)}</div>` : ''}
                <div class="del-card__eta">${svg('reloj', 13)} ETA: ${esc(etaDe(p.fechaPedido))}</div>
            </div>
        </div>
        <div class="del-card__cuerpo">
            <div>
                <div class="del-bloque__label">${svg('persona', 14)} Cliente</div>
                ${clienteHtml}
            </div>
            <div>
                <div class="del-bloque__label">${svg('paquete', 14)} Productos</div>
                <div class="del-prod">${productos}</div>
            </div>
        </div>
        ${repartidor}
        ${nota}
        ${acciones ? `<div class="del-card__acciones">${acciones}</div>` : ''}
    </article>`;
}

function activarDelivery() {
    const btnAct = document.getElementById('del-actualizar');
    btnAct.addEventListener('click', async () => {
        btnAct.disabled = true;
        try { await cargarDelivery(); mostrarToast('Pedidos actualizados'); }
        catch (e) { notificarError(e); }
        finally { btnAct.disabled = false; }
    });
    document.getElementById('del-tabs').addEventListener('click', (e) => {
        const btn = e.target.closest('[data-tab]');
        if (!btn) return;
        delTab = btn.dataset.tab;
        cargarDelivery();
    });
    document.getElementById('del-lista').addEventListener('click', async (e) => {
        const btn = e.target.closest('[data-accion]');
        if (!btn) return;
        if (btn.dataset.accion === 'abrir-repartidores') { abrirModalRepartidores(); return; }
        const id = Number(btn.dataset.id);
        try {
            if (btn.dataset.accion === 'estado') {
                await api(`/api/pedidos/${id}/estado`, { method: 'PUT', body: { estado: btn.dataset.estado } });
                mostrarToast(`Pedido PD-${zfill(id)}: ${btn.dataset.estado}`);
                await cargarDelivery();
            } else if (btn.dataset.accion === 'enviar') {
                const sel = document.getElementById('del-rep-' + id);
                const idRep = Number(sel && sel.value);
                if (!idRep) { alert('Selecciona un repartidor disponible.'); return; }
                await api(`/api/pedidos/${id}/repartidor`, { method: 'PUT', body: { idRepartidor: idRep } });
                await api(`/api/pedidos/${id}/estado`, { method: 'PUT', body: { estado: 'En Tránsito' } });
                mostrarToast(`Pedido PD-${zfill(id)} en tránsito`);
                await cargarDelivery();
            }
        } catch (err) { notificarError(err); }
    });
}

/* ---------- 11b. REPARTIDORES (CRUD) ---------- */
async function cargarRepartidores() {
    const cont = document.getElementById('rep-listado');
    cont.innerHTML = `<p class="vacio">Cargando…</p>`;
    try {
        const lista = await api('/api/repartidores');
        if (!lista.length) {
            cont.innerHTML = `<div class="vacio">${svg('moto', 30)}<p>Aún no hay repartidores. Agrega el primero arriba.</p></div>`;
            return;
        }
        cont.innerHTML = lista.map((r) => {
            const est = (r.estado || '').toLowerCase();
            const clase = est === 'disponible' ? 'rep-item__estado--disponible'
                : est === 'inactivo' ? 'rep-item__estado--inactivo' : 'rep-item__estado--ruta';
            const baja = est === 'inactivo' ? '' :
                `<button type="button" class="rep-item__baja" data-id="${r.idRepartidor}" title="Dar de baja" aria-label="Dar de baja a ${esc(r.nombres)}">${svg('basura', 18)}</button>`;
            return `<div class="rep-item">
                <span class="cfg-user__avatar" aria-hidden="true">${esc((r.nombres || '?').charAt(0).toUpperCase())}</span>
                <div class="rep-item__datos">
                    <div class="rep-item__nombre">${esc(r.nombres)}</div>
                    <div class="rep-item__tel">${esc(r.telefono || 'Sin teléfono')}</div>
                </div>
                <span class="rep-item__estado ${clase}">${esc(r.estado || '—')}</span>
                ${baja}
            </div>`;
        }).join('');
    } catch (e) { notificarError(e); }
}

function abrirModalRepartidores() {
    document.getElementById('form-repartidor').reset();
    document.getElementById('modal-repartidores').hidden = false;
    cargarRepartidores();
}

function activarRepartidores() {
    const modal = document.getElementById('modal-repartidores');
    const cerrar = () => { modal.hidden = true; };
    document.getElementById('del-repartidores').addEventListener('click', abrirModalRepartidores);
    document.getElementById('rep-cerrar').addEventListener('click', cerrar);
    modal.addEventListener('click', (e) => { if (e.target === e.currentTarget) cerrar(); });

    document.getElementById('form-repartidor').addEventListener('submit', async (e) => {
        e.preventDefault();
        const f = e.target;
        const payload = { nombres: f.nombres.value.trim(), telefono: f.telefono.value.trim() };
        if (!payload.nombres) return;
        try {
            await api('/api/repartidores', { method: 'POST', body: payload });
            f.reset();
            await cargarRepartidores();
            await cargarDelivery();
            mostrarToast('Repartidor agregado');
        } catch (err) { notificarError(err); }
    });

    document.getElementById('rep-listado').addEventListener('click', async (e) => {
        const btn = e.target.closest('.rep-item__baja');
        if (!btn) return;
        if (!confirm('¿Dar de baja a este repartidor? (queda Inactivo, no se elimina)')) return;
        try {
            await api('/api/repartidores/' + Number(btn.dataset.id), { method: 'DELETE' });
            await cargarRepartidores();
            await cargarDelivery();
            mostrarToast('Repartidor dado de baja');
        } catch (err) { notificarError(err); }
    });
}
