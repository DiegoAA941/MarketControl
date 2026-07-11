/* ---------- 6. DASHBOARD ---------- */
async function cargarDashboard() {
    const hoy = hoyISO();
    let ventasHoy = [], ventasTodas = [], pedidos = [], productos = productosCache;
    try { productos = await api('/api/productos'); productosCache = productos; nombrePorProducto = {}; productos.forEach((p) => { nombrePorProducto[p.idProducto] = p.nombreProducto; }); } catch (_) {}
    try { ventasHoy = await api(`/api/ventas?desde=${hoy}&hasta=${hoy}`); } catch (_) {}
    try { ventasTodas = await api('/api/ventas'); } catch (_) {}
    try { pedidos = (await api('/api/pedidos')).filter((p) => p.tipoEntrega === 'Delivery'); } catch (_) {}

    const totalHoy = ventasHoy.reduce((s, v) => s + Number(v.total || 0), 0);
    const activos = productos.filter((p) => p.estado !== false);
    const bajo = activos.filter((p) => p.stockActual <= p.stockMinimo);
    const enCurso = pedidos.filter((p) => !['Entregado', 'Cancelado'].includes(p.estado));

    const cards = [
        { titulo: 'Ventas del Día', valor: soles(totalHoy), detalle: `${ventasHoy.length} transacciones`, icono: 'dinero', color: 'verde' },
        { titulo: 'Productos en Stock', valor: String(activos.length), detalle: `${bajo.length} bajo mínimo`, icono: 'caja', color: 'azul' },
        { titulo: 'Pedidos Activos', valor: String(enCurso.length), detalle: `${pedidos.length} en total`, icono: 'camion', color: 'naranja' },
        { titulo: 'Stock Bajo', valor: String(bajo.length), detalle: 'requieren reposición', icono: 'tendencia', color: 'morado' },
    ];
    document.getElementById('resumen').innerHTML = cards.map((c) => `
        <article class="tarjeta">
            <div class="tarjeta__cabecera">
                <span class="tarjeta__titulo">${esc(c.titulo)}</span>
                <span class="chip chip--${c.color}">${svg(c.icono)}</span>
            </div>
            <p class="tarjeta__valor">${esc(c.valor)}</p>
            <p class="tarjeta__detalle">${esc(c.detalle)}</p>
        </article>`).join('');

    // Ventas recientes (últimas 5)
    const recientes = [...ventasTodas].sort((a, b) => new Date(b.fechaVenta) - new Date(a.fechaVenta)).slice(0, 5);
    const ulV = document.getElementById('ventas');
    ulV.innerHTML = recientes.length ? recientes.map((v) => {
        const m = METODOS[v.metodoPago] || { clase: '', icono: 'efectivo' };
        return `<li>
            <div>
                <div class="venta__codigo">V-${zfill(v.idVenta)}</div>
                <div class="venta__fecha">${esc(fmtFecha(v.fechaVenta))}</div>
            </div>
            <div class="venta__derecha">
                <span class="venta__monto">${soles(v.total)}</span>
                <span class="metodo ${m.clase}">${svg(m.icono, 14)}${esc(v.metodoPago || '')}</span>
            </div>
        </li>`;
    }).join('') : `<li class="vacio">${svg('vacio', 30)}<p>Aún no hay ventas registradas.</p></li>`;

    // Alertas de stock
    const ulA = document.getElementById('alertas');
    ulA.innerHTML = bajo.length ? bajo.map((p) => `
        <li>
            <div>
                <div class="alerta__nombre">${esc(p.nombreProducto)}</div>
                <div class="alerta__sku">${esc(p.codigoSku)}</div>
            </div>
            <div class="alerta__derecha">
                <span class="pildora-stock">Stock: ${esc(p.stockActual)}</span>
                <span class="alerta__minimo">Mín: ${esc(p.stockMinimo)}</span>
            </div>
        </li>`).join('') : `<li class="vacio">${svg('check', 30)}<p>Todo el stock está por encima del mínimo.</p></li>`;

    // Pedidos en curso
    const ulP = document.getElementById('pedidos');
    ulP.innerHTML = enCurso.length ? enCurso.map((p) => {
        const clase = ESTADO_CLASE[p.estado] || 'estado--pendiente';
        return `<li>
            <span class="pedido__icono" aria-hidden="true">${svg('moto', 24)}</span>
            <div class="pedido__datos">
                <div class="pedido__cliente">
                    <strong>PD-${zfill(p.idPedido)}</strong>
                    <span class="estado ${clase}">${esc(p.estado)}</span>
                </div>
                <div class="pedido__direccion">Cliente #${esc(p.idCliente)}${p.repartidor ? ' · ' + esc(p.repartidor.nombres) : ''}</div>
            </div>
            <span class="pedido__monto">${soles(p.total)}</span>
        </li>`;
    }).join('') : `<li class="vacio">${svg('camion', 30)}<p>No hay pedidos en curso.</p></li>`;
}
