/* ---------- 8. PUNTO DE VENTA ---------- */
function calcularTotales() {
    // El precio de venta YA incluye IGV. El total es la suma exacta de precios.
    const total = carrito.reduce((s, it) => s + it.precio * it.cantidad, 0);
    const base = total / (1 + TASA_IGV);   // Op. gravada (base imponible)
    const igv = total - base;              // IGV contenido en el precio
    return { base, igv, total };
}
function pintarPosProductos() {
    const q = (document.getElementById('pos-buscar').value || '').trim().toLowerCase();
    // Solo productos ACTIVOS pueden venderse (los desactivados no aparecen en el POS)
    const activos = productosCache.filter((p) => p.estado !== false);
    const lista = q
        ? activos.filter((p) =>
            (p.nombreProducto || '').toLowerCase().includes(q) ||
            (p.codigoSku || '').toLowerCase().includes(q) ||
            nombreCat(p.idCategoria).toLowerCase().includes(q))
        : activos;
    const grid = document.getElementById('pos-grid');
    if (!lista.length) { grid.innerHTML = `<p class="pos-grid__vacio">No se encontraron productos.</p>`; return; }
    grid.innerHTML = lista.map((p) => {
        const agotado = Number(p.stockActual) <= 0;
        return `
        <button type="button" class="pos-card ${agotado ? 'pos-card--agotado' : ''}" data-id="${p.idProducto}" ${agotado ? 'disabled' : ''}>
            <span class="pos-card__img">
                <span class="pos-card__ph" aria-hidden="true">${svg('caja', 28)}</span>
                ${p.imagenUrl ? `<img src="${esc(p.imagenUrl)}" alt="${esc(p.nombreProducto)}" loading="lazy" onerror="this.remove()">` : ''}
            </span>
            <span class="pos-card__cuerpo">
                <span class="pos-card__nombre">${esc(p.nombreProducto)}</span>
                <span class="pos-card__sku">${esc(p.codigoSku)}</span>
                <span class="pos-card__precio">${soles(p.precioVenta)}</span>
                <span class="pos-card__stock ${agotado ? 'pos-card__stock--cero' : ''}">${agotado ? 'Sin stock' : 'Stock: ' + p.stockActual}</span>
            </span>
        </button>`;
    }).join('');
}
function pintarPosCarrito() {
    const lista = document.getElementById('pos-carrito');
    if (!carrito.length) {
        lista.innerHTML = `<li class="carrito-vacio"><span aria-hidden="true">${svg('carrito', 30)}</span><p>Selecciona productos para vender</p></li>`;
    } else {
        lista.innerHTML = carrito.map((it) => `
            <li class="carrito-item">
                <div class="carrito-item__top">
                    <span class="carrito-item__nombre">${esc(it.nombre)}</span>
                    <button type="button" class="carrito-item__quitar" data-accion="quitar" data-id="${it.id}" aria-label="Quitar ${esc(it.nombre)}">${svg('basura', 16)}</button>
                </div>
                <div class="carrito-item__bottom">
                    <span class="cantidad">
                        <button type="button" data-accion="menos" data-id="${it.id}" aria-label="Restar uno">${svg('menos', 16)}</button>
                        <span class="cantidad__num">${it.cantidad}</span>
                        <button type="button" data-accion="mas" data-id="${it.id}" aria-label="Sumar uno">${svg('mas', 16)}</button>
                    </span>
                    <span class="carrito-item__unit">× ${soles(it.precio)}</span>
                    <span class="carrito-item__total">${soles(it.precio * it.cantidad)}</span>
                </div>
            </li>`).join('');
    }
    const { base, igv, total } = calcularTotales();
    document.getElementById('pos-count').textContent = `${carrito.length} prod.`;
    document.getElementById('pos-subtotal').textContent = soles(base);
    document.getElementById('pos-igv').textContent = soles(igv);
    document.getElementById('pos-total').textContent = soles(total);
    document.getElementById('pos-btn-total').textContent = soles(total);
    document.getElementById('pos-completar').disabled = carrito.length === 0;
    document.getElementById('pos-cajero').textContent = usuarioActual ? usuarioActual.nombre : '—';
    document.getElementById('pos-fecha').textContent = fechaPeruAhora();
}
function agregarAlCarrito(id) {
    const prod = productosCache.find((p) => p.idProducto === id);
    if (!prod) return;
    const en = carrito.find((it) => it.id === id);
    if (en) en.cantidad += 1;
    else carrito.push({ id: prod.idProducto, nombre: prod.nombreProducto, precio: Number(prod.precioVenta), cantidad: 1 });
    pintarPosCarrito();
}
function cambiarCantidad(id, delta) {
    const it = carrito.find((x) => x.id === id);
    if (!it) return;
    it.cantidad += delta;
    if (it.cantidad <= 0) carrito.splice(carrito.indexOf(it), 1);
    pintarPosCarrito();
}
function quitarDelCarrito(id) {
    const i = carrito.findIndex((x) => x.id === id);
    if (i >= 0) carrito.splice(i, 1);
    pintarPosCarrito();
}
function activarPOS() {
    document.getElementById('pos-buscar').addEventListener('input', pintarPosProductos);
    const btnAct = document.getElementById('pos-actualizar');
    if (btnAct) btnAct.addEventListener('click', async () => {
        document.getElementById('pos-buscar').value = '';
        btnAct.disabled = true;
        try {
            await cargarCatalogo();
            pintarPosProductos();
            actualizarBadgeRecojos();
            mostrarToast('Catálogo actualizado');
        } catch (e) { notificarError(e); }
        finally { btnAct.disabled = false; }
    });
    document.getElementById('pos-grid').addEventListener('click', (e) => {
        const card = e.target.closest('.pos-card');
        if (card) agregarAlCarrito(Number(card.dataset.id));
    });
    document.getElementById('pos-carrito').addEventListener('click', (e) => {
        const btn = e.target.closest('[data-accion]');
        if (!btn) return;
        const id = Number(btn.dataset.id);
        if (btn.dataset.accion === 'mas') cambiarCantidad(id, 1);
        else if (btn.dataset.accion === 'menos') cambiarCantidad(id, -1);
        else if (btn.dataset.accion === 'quitar') quitarDelCarrito(id);
    });
    const metodo = document.getElementById('pos-metodo');
    const iconoMetodo = document.getElementById('pos-metodo-icono');
    const pintarIcono = () => { iconoMetodo.innerHTML = svg(metodo.value === 'Yape' ? 'telefono' : 'efectivo', 18); };
    metodo.addEventListener('change', pintarIcono);
    pintarIcono();

    document.getElementById('pos-completar').addEventListener('click', async () => {
        if (!carrito.length) return;
        const payload = {
            metodoPago: metodo.value,
            detalles: carrito.map((it) => ({ idProducto: it.id, cantidad: it.cantidad, precioUnitario: it.precio })),
        };
        if (pedidoRecojoActivo) payload.idPedido = pedidoRecojoActivo; // cierra el recojo ('Atendido')
        const boton = document.getElementById('pos-completar');
        boton.disabled = true;
        try {
            await api('/api/ventas', { method: 'POST', body: payload });
            mostrarToast(pedidoRecojoActivo ? 'Recojo cobrado. Venta exitosa' : 'Venta exitosa');
            carrito.length = 0;
            limpiarRecojoActivo();
            pintarPosCarrito();
            actualizarBadgeRecojos();
        } catch (err) { notificarError(err); boton.disabled = false; }
    });
}

function mostrarToast(mensaje) {
    const toast = document.getElementById('toast');
    toast.innerHTML = `${svg('check', 20)}<span>${esc(mensaje)}</span>`;
    toast.classList.add('toast--visible');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => toast.classList.remove('toast--visible'), 3000);
}

/* ---------- 8b. POS: importar pedidos de Recojo ---------- */
function activarRecojos() {
    const modal = document.getElementById('modal-recojo');
    const cerrar = () => { modal.hidden = true; };
    const abrir = () => { modal.hidden = false; document.getElementById('recojo-telefono').value = ''; cargarRecojos(''); };
    document.getElementById('pos-recojos').addEventListener('click', abrir);
    document.getElementById('recojo-cerrar').addEventListener('click', cerrar);
    modal.addEventListener('click', (e) => { if (e.target === modal) cerrar(); });
    document.getElementById('recojo-buscar-btn').addEventListener('click', () => cargarRecojos(document.getElementById('recojo-telefono').value.trim()));
    document.getElementById('recojo-todos-btn').addEventListener('click', () => { document.getElementById('recojo-telefono').value = ''; cargarRecojos(''); });
    document.getElementById('recojo-telefono').addEventListener('keydown', (e) => { if (e.key === 'Enter') { e.preventDefault(); cargarRecojos(e.target.value.trim()); } });
    document.getElementById('recojo-listado').addEventListener('click', async (e) => {
        const btn = e.target.closest('[data-id]');
        if (!btn) return;
        await importarRecojo(Number(btn.dataset.id));
        cerrar();
    });
}

async function cargarRecojos(telefono) {
    const cont = document.getElementById('recojo-listado');
    cont.innerHTML = `<p class="vacio">Cargando…</p>`;
    try {
        const q = telefono ? ('?telefono=' + encodeURIComponent(telefono)) : '';
        const lista = await api('/api/pedidos/recojo' + q);
        if (!lista.length) {
            cont.innerHTML = `<div class="vacio">${svg('paquete', 30)}<p>${telefono ? 'Sin recojos pendientes para ese teléfono.' : 'No hay pedidos de recojo pendientes.'}</p></div>`;
            return;
        }
        cont.innerHTML = lista.map((p) => {
            const cli = p.cliente;
            return `<div class="recojo-card">
                <div class="recojo-card__cab">
                    <span class="recojo-card__id">PD-${zfill(p.idPedido)}</span>
                    <span class="recojo-card__total">${soles(p.total)}</span>
                </div>
                <div class="recojo-card__cli">${cli ? esc(cli.nombres) : 'Cliente #' + esc(p.idCliente)}${cli && cli.telefono ? ' · ' + esc(cli.telefono) : ''}</div>
                <div class="recojo-card__meta">${esc(fmtFecha(p.fechaPedido))}${p.observaciones ? ' · ' + esc(p.observaciones) : ''}</div>
                <div class="recojo-card__pie"><button type="button" class="recojo-card__importar" data-id="${p.idPedido}">Importar al POS</button></div>
            </div>`;
        }).join('');
    } catch (e) { notificarError(e); }
}

async function importarRecojo(idPedido) {
    try {
        const p = await api('/api/pedidos/' + idPedido);
        if (!p || !p.detalles || !p.detalles.length) { alert('El pedido no tiene productos.'); return; }
        carrito.length = 0;
        p.detalles.forEach((d) => {
            carrito.push({
                id: d.idProducto,
                nombre: nombrePorProducto[d.idProducto] || ('Producto #' + d.idProducto),
                precio: Number(d.precioUnitario),
                cantidad: d.cantidad,
            });
        });
        pedidoRecojoActivo = idPedido;
        pintarPosCarrito();
        mostrarToast(`Recojo PD-${zfill(idPedido)} importado. Cobra normalmente para cerrarlo.`);
    } catch (e) { notificarError(e); }
}

function limpiarRecojoActivo() {
    pedidoRecojoActivo = null;
}

async function actualizarBadgeRecojos() {
    const badge = document.getElementById('pos-recojos-badge');
    if (!badge) return;
    try {
        const lista = await api('/api/pedidos/recojo');
        const n = lista.length;
        badge.textContent = n > 9 ? '9+' : String(n);
        badge.style.display = n > 0 ? 'inline-flex' : 'none';
    } catch (_) { /* no interrumpir la UI por un fallo de polling */ }
}
