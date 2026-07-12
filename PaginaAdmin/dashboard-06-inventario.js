/* ---------- 7. INVENTARIO ---------- */
function pintarInventario() {
    document.getElementById('inv-total').textContent = productosCache.length;
    const valor = productosCache.reduce((s, p) => s + Number(p.precioCompra || 0) * Number(p.stockActual || 0), 0);
    document.getElementById('inv-valor').textContent = soles(valor);
    document.getElementById('inv-bajo').textContent =
        productosCache.filter((p) => p.stockActual <= p.stockMinimo).length;

    const q = (document.getElementById('inv-buscar').value || '').trim().toLowerCase();
    const filtrados = q
        ? productosCache.filter((p) =>
            (p.nombreProducto || '').toLowerCase().includes(q) ||
            (p.codigoSku || '').toLowerCase().includes(q) ||
            nombreCat(p.idCategoria).toLowerCase().includes(q))
        : productosCache;

    const tbody = document.getElementById('inv-tbody');
    if (!filtrados.length) {
        tbody.innerHTML = `<tr><td colspan="7" class="tabla-inv__vacio">No se encontraron productos.</td></tr>`;
        return;
    }
    tbody.innerHTML = filtrados.map((p) => {
        const bajo = p.stockActual <= p.stockMinimo;
        return `<tr>
            <td class="td-nombre">${esc(p.nombreProducto)}</td>
            <td class="td-sku">${esc(p.codigoSku)}</td>
            <td><span class="cat-pill">${esc(nombreCat(p.idCategoria))}</span></td>
            <td>${soles(p.precioVenta)}</td>
            <td>${soles(p.precioCompra)}</td>
            <td><span class="stock-pill ${bajo ? 'stock-pill--bajo' : ''}">${esc(p.stockActual)} uds</span></td>
            <td class="ta-der">
                <button type="button" class="icono-accion icono-accion--stock" data-accion="stock"
                    data-id="${p.idProducto}" aria-label="Agregar stock a ${esc(p.nombreProducto)}" title="Agregar stock">${svg('mascirculo', 18)}</button>
                <button type="button" class="icono-accion icono-accion--editar" data-accion="editar"
                    data-id="${p.idProducto}" aria-label="Editar ${esc(p.nombreProducto)}">${svg('lapiz', 18)}</button>
                <button type="button" class="icono-accion icono-accion--eliminar" data-accion="eliminar"
                    data-id="${p.idProducto}" aria-label="Desactivar ${esc(p.nombreProducto)}">${svg('basura', 18)}</button>
            </td>
        </tr>`;
    }).join('');
}

function abrirModalProducto(producto) {
    const overlay = document.getElementById('modal-producto');
    const form = document.getElementById('form-producto');
    form.reset();
    editandoId = null;
    document.getElementById('modal-titulo').textContent = producto ? 'Editar Producto' : 'Nuevo Producto';
    document.getElementById('modal-sub').textContent = producto
        ? 'Modifica los datos del producto seleccionado.'
        : 'Completa los campos para agregar un nuevo producto al inventario.';
    if (producto) {
        editandoId = producto.idProducto;
        form.nombre.value = producto.nombreProducto || '';
        form.imagen.value = producto.imagenUrl || '';
        form.sku.value = producto.codigoSku || '';
        form.categoria.value = String(producto.idCategoria || '');
        form.marca.value = producto.marca || '';
        form.estado.value = producto.estado === false ? 'Inactivo' : 'Activo';
        form.descripcion.value = producto.descripcion || '';
        form.costo.value = producto.precioCompra ?? 0;
        form.precio.value = producto.precioVenta ?? 0;
        form.descuento.value = producto.porcentajeDescuento ?? 0;
        form.stock.value = producto.stockActual ?? 0;
        form.minimo.value = producto.stockMinimo ?? 5;
    }
    overlay.hidden = false;
    document.getElementById('p-nombre').focus();
    actualizarAlertaDescuento();
}

/* Avisa si el descuento hace que el producto se venda por debajo del costo,
   una vez restado el IGV (PrecioVenta ya lo incluye). */
function actualizarAlertaDescuento() {
    const form = document.getElementById('form-producto');
    const alerta = document.getElementById('p-descuento-alerta');
    const costo = parseFloat(form.costo.value) || 0;
    const precio = parseFloat(form.precio.value) || 0;
    const descuento = parseFloat(form.descuento.value) || 0;
    if (precio <= 0 || costo <= 0) { alerta.hidden = true; return; }

    const FACTOR_IGV = 1.18;
    const maxDescuento = Math.max(0, (1 - (costo * FACTOR_IGV) / precio) * 100);
    const precioConDescuento = precio * (1 - descuento / 100);
    const margen = precioConDescuento / FACTOR_IGV - costo;

    alerta.hidden = false;
    if (margen < 0) {
        alerta.className = 'alerta-descuento col-6 alerta-descuento--peligro';
        alerta.textContent = `Con ${descuento}% de descuento vendes por debajo de tu costo (pierdes S/${Math.abs(margen).toFixed(2)} por unidad). Descuento máximo sin perder dinero: ${maxDescuento.toFixed(1)}%.`;
    } else {
        alerta.className = 'alerta-descuento col-6';
        alerta.textContent = `Descuento máximo sin perder dinero: ${maxDescuento.toFixed(1)}% (margen actual: S/${margen.toFixed(2)} por unidad).`;
    }
}
function cerrarModalProducto() {
    document.getElementById('modal-producto').hidden = true;
    document.getElementById('form-producto').reset();
    editandoId = null;
}

function activarInventario() {
    document.getElementById('btn-nuevo').addEventListener('click', () => abrirModalProducto());
    document.getElementById('inv-buscar').addEventListener('input', pintarInventario);

    document.getElementById('inv-tbody').addEventListener('click', async (e) => {
        const btn = e.target.closest('[data-accion]');
        if (!btn) return;
        const id = Number(btn.dataset.id);
        const prod = productosCache.find((p) => p.idProducto === id);
        if (!prod) return;
        if (btn.dataset.accion === 'editar') {
            abrirModalProducto(prod);
        } else if (btn.dataset.accion === 'stock') {
            const txt = prompt(`Agregar stock a "${prod.nombreProducto}" (stock actual: ${prod.stockActual}).\n¿Cuántas unidades ingresan?`);
            if (txt == null) return;
            const cant = parseInt(txt, 10);
            if (!Number.isInteger(cant) || cant <= 0) { alert('Ingresa una cantidad válida (entero mayor a 0).'); return; }
            try {
                const r = await api('/api/productos/' + id + '/stock', { method: 'PUT', body: { cantidad: cant } });
                await cargarCatalogo(); pintarInventario();
                mostrarToast(`Stock actualizado: ${prod.nombreProducto} → ${r.stockActual} uds`);
            } catch (err) { notificarError(err); }
        } else if (btn.dataset.accion === 'eliminar') {
            if (!confirm(`¿Desactivar "${prod.nombreProducto}"? (Estado = 0, no se borra)`)) return;
            try { await api('/api/productos/' + id, { method: 'DELETE' }); await cargarCatalogo(); pintarInventario(); mostrarToast('Producto desactivado'); }
            catch (err) { notificarError(err); }
        }
    });

    ['p-costo', 'p-precio', 'p-descuento'].forEach((id) => {
        document.getElementById(id).addEventListener('input', actualizarAlertaDescuento);
    });

    document.getElementById('modal-cerrar').addEventListener('click', cerrarModalProducto);
    document.getElementById('modal-cancelar').addEventListener('click', cerrarModalProducto);
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape' && !document.getElementById('modal-producto').hidden) cerrarModalProducto();
    });

    document.getElementById('form-producto').addEventListener('submit', async (e) => {
        e.preventDefault();
        const form = e.target;
        const payload = {
            idCategoria: Number(form.categoria.value),
            codigoSku: form.sku.value.trim(),
            nombreProducto: form.nombre.value.trim(),
            descripcion: form.descripcion.value.trim(),
            marca: form.marca.value.trim(),
            imagenUrl: form.imagen.value.trim(),
            precioCompra: parseFloat(form.costo.value) || 0,
            precioVenta: parseFloat(form.precio.value) || 0,
            porcentajeDescuento: parseFloat(form.descuento.value) || 0,
            stockActual: parseInt(form.stock.value, 10) || 0,
            stockMinimo: parseInt(form.minimo.value, 10) || 5,
            estado: form.estado.value === 'Activo',
        };
        if (!payload.nombreProducto || !payload.codigoSku || !payload.idCategoria) return;
        try {
            if (editandoId !== null) await api('/api/productos/' + editandoId, { method: 'PUT', body: payload });
            else await api('/api/productos', { method: 'POST', body: payload });
            cerrarModalProducto();
            await cargarCatalogo();
            pintarInventario();
            mostrarToast(editandoId !== null ? 'Producto actualizado' : 'Producto creado');
        } catch (err) { notificarError(err); }
    });

    // --- Modal de categorías (crear, editar, desactivar) ---
    const modalCat = document.getElementById('modal-categoria');
    const salirModoEdicionCat = () => {
        editandoCategoriaId = null;
        document.getElementById('form-categoria').reset();
        document.getElementById('cat-estado-campo').hidden = true;
        document.getElementById('cat-guardar-btn').textContent = 'Crear Categoría';
    };
    const abrirCat = () => {
        salirModoEdicionCat();
        modalCat.hidden = false;
        document.getElementById('c-nombre').focus();
        cargarCategoriasListado();
    };
    const cerrarCat = () => { modalCat.hidden = true; };
    document.getElementById('btn-nueva-cat').addEventListener('click', abrirCat);
    document.getElementById('cat-cerrar').addEventListener('click', cerrarCat);
    document.getElementById('cat-cancelar').addEventListener('click', () => { salirModoEdicionCat(); cerrarCat(); });
    modalCat.addEventListener('click', (e) => { if (e.target === e.currentTarget) cerrarCat(); });
    document.getElementById('form-categoria').addEventListener('submit', async (e) => {
        e.preventDefault();
        const f = e.target;
        const payload = { nombreCategoria: f.nombre.value.trim(), descripcion: f.descripcion.value.trim() };
        if (!payload.nombreCategoria) return;
        try {
            if (editandoCategoriaId !== null) {
                payload.estado = f.estado.value === 'Activo';
                await api('/api/categorias/' + editandoCategoriaId, { method: 'PUT', body: payload });
                mostrarToast('Categoría actualizada');
            } else {
                const creada = await api('/api/categorias', { method: 'POST', body: payload });
                if (creada && creada.idCategoria) {
                    document.getElementById('p-categoria').value = String(creada.idCategoria);
                }
                mostrarToast('Categoría creada');
            }
            await cargarCatalogo();                 // recarga categoriasCache + select
            await cargarCategoriasListado();
            salirModoEdicionCat();
        } catch (err) { notificarError(err); }
    });

    document.getElementById('cat-listado').addEventListener('click', async (e) => {
        const btnEditar = e.target.closest('[data-accion="editar-cat"]');
        const btnBaja = e.target.closest('[data-accion="baja-cat"]');
        if (btnEditar) {
            const id = Number(btnEditar.dataset.id);
            const cat = categoriasListadoCache.find((c) => c.idCategoria === id);
            if (!cat) return;
            editandoCategoriaId = id;
            const f = document.getElementById('form-categoria');
            f.nombre.value = cat.nombreCategoria || '';
            f.descripcion.value = cat.descripcion || '';
            document.getElementById('cat-estado-campo').hidden = false;
            f.estado.value = cat.estado === false ? 'Inactivo' : 'Activo';
            document.getElementById('cat-guardar-btn').textContent = 'Guardar Cambios';
            document.getElementById('c-nombre').focus();
        } else if (btnBaja) {
            const id = Number(btnBaja.dataset.id);
            if (!confirm('¿Desactivar esta categoría? Los productos ya asignados no se ven afectados.')) return;
            try {
                await api('/api/categorias/' + id, { method: 'DELETE' });
                await cargarCatalogo();
                await cargarCategoriasListado();
                mostrarToast('Categoría desactivada');
            } catch (err) { notificarError(err); }
        }
    });
}
