/* ---------- 10. REPORTES ---------- */
const RANGO_LABEL = { hoy: 'Hoy', semana: 'Esta Semana', mes: 'Este Mes', anio: 'Este Año' };
let repTab = 'ventas';
let repExport = { nombre: 'reporte', filas: [] };

function chartLinea(puntos) {
    const W = 560, H = 260, pad = 42;
    if (!puntos.length) return '';
    const maxY = Math.max(...puntos.map((p) => p.y), 1);
    const stepX = puntos.length > 1 ? (W - 2 * pad) / (puntos.length - 1) : 0;
    const sx = (i) => pad + i * stepX;
    const sy = (v) => H - pad - (v / maxY) * (H - 2 * pad);
    let grid = '';
    for (let i = 0; i <= 4; i++) {
        const y = pad + i * ((H - 2 * pad) / 4);
        const val = Math.round(maxY * (1 - i / 4));
        grid += `<line x1="${pad}" y1="${y}" x2="${W - pad}" y2="${y}" stroke="#eceff4"/><text x="${pad - 8}" y="${y + 4}" text-anchor="end" font-size="11" fill="#98a2b3">${val}</text>`;
    }
    const linea = puntos.length > 1 ? `<polyline points="${puntos.map((p, i) => `${sx(i)},${sy(p.y)}`).join(' ')}" fill="none" stroke="#3b6fe6" stroke-width="2"/>` : '';
    const dots = puntos.map((p, i) => `<circle cx="${sx(i)}" cy="${sy(p.y)}" r="4" fill="#fff" stroke="#3b6fe6" stroke-width="2" data-tip="${esc(p.tip || (p.x + ': ' + p.y))}"/>`).join('');
    const labels = puntos.map((p, i) => `<text x="${sx(i)}" y="${H - pad + 18}" text-anchor="middle" font-size="11" fill="#667085">${esc(p.x)}</text>`).join('');
    return `<svg viewBox="0 0 ${W} ${H}">${grid}${linea}${dots}${labels}</svg>`;
}
function chartPastel(segs) {
    const total = segs.reduce((s, x) => s + x.val, 0);
    if (total <= 0) return '';
    const cx = 140, cy = 140, r = 110;
    const tip = (s) => esc(s.tip || (s.label + ': ' + Math.round(s.val / total * 100) + '%'));
    if (segs.length === 1) return `<svg viewBox="0 0 280 280"><circle cx="${cx}" cy="${cy}" r="${r}" fill="${segs[0].color}" data-tip="${tip(segs[0])}"/></svg>`;
    let a0 = -Math.PI / 2, paths = '';
    segs.forEach((s) => {
        const frac = s.val / total, a1 = a0 + frac * 2 * Math.PI;
        const x0 = cx + r * Math.cos(a0), y0 = cy + r * Math.sin(a0);
        const x1 = cx + r * Math.cos(a1), y1 = cy + r * Math.sin(a1);
        const large = frac > 0.5 ? 1 : 0;
        paths += `<path d="M${cx},${cy} L${x0.toFixed(1)},${y0.toFixed(1)} A${r},${r} 0 ${large} 1 ${x1.toFixed(1)},${y1.toFixed(1)} Z" fill="${s.color}" data-tip="${tip(s)}"/>`;
        a0 = a1;
    });
    return `<svg viewBox="0 0 280 280">${paths}</svg>`;
}
function panelVacio(texto) {
    return `<div class="rep-grafico"><div class="vacio">${svg('vacio', 34)}<p>${esc(texto)}</p></div></div>`;
}

async function cargarReportes() {
    const clave = document.getElementById('rep-rango').value;
    const { desde, hasta } = rangoDe(clave);
    const etiqueta = RANGO_LABEL[clave];

    // KPIs
    try {
        const k = await api(`/api/reportes/kpis?desde=${desde}&hasta=${hasta}`);
        document.getElementById('rep-ventas').textContent = soles(k.ventasTotales);
        document.getElementById('rep-trans').textContent = k.transacciones;
        document.getElementById('rep-margen').textContent = soles(k.margenGanancia);
        document.getElementById('rep-margen-pie').textContent = `${Number(k.margenPorcentaje || 0)}% margen`;
        document.getElementById('rep-ventas-pie').textContent = etiqueta;
        document.getElementById('rep-trans-pie').textContent = etiqueta;
    } catch (e) { notificarError(e); }

    await pintarPanelReporte(desde, hasta);
}

async function pintarPanelReporte(desde, hasta) {
    const panel = document.getElementById('rep-panel');
    panel.innerHTML = '<div class="rep-grafico"><p>Cargando…</p></div>';
    try {
        if (repTab === 'ventas') {
            const ventas = await api(`/api/ventas?desde=${desde}&hasta=${hasta}`);
            repExport = { nombre: 'ventas', filas: ventas.map((v) => ({ venta: v.idVenta, fecha: v.fechaVenta, total: v.total, metodo: v.metodoPago })) };
            // Ventas por hora
            const horas = {};
            ventas.forEach((v) => { const h = zfill(new Date(v.fechaVenta).getHours()) + ':00'; horas[h] = (horas[h] || 0) + Number(v.total || 0); });
            const puntos = Object.keys(horas).sort().map((h) => ({ x: h, y: horas[h], tip: `${h} — ${soles(horas[h])}` }));
            // Ventas por método
            const met = {};
            ventas.forEach((v) => { met[v.metodoPago] = (met[v.metodoPago] || 0) + Number(v.total || 0); });
            const colores = { Efectivo: '#12a866', Yape: '#7c5cf0' };
            const totalMetPrev = Object.values(met).reduce((s, x) => s + x, 0) || 1;
            const segs = Object.keys(met).map((m, i) => ({ label: m, val: met[m], color: colores[m] || ['#12a866', '#f0913a'][i % 2], tip: `${m}: ${soles(met[m])} (${Math.round(met[m] / totalMetPrev * 100)}%)` }));
            const totalMet = segs.reduce((s, x) => s + x.val, 0);

            const izq = puntos.length
                ? `<div class="rep-grafico"><h3 class="rep-grafico__titulo">Ventas por Hora</h3>${chartLinea(puntos)}<div class="rep-leyenda"><span><i style="background:#3b6fe6"></i>ventas</span></div></div>`
                : panelVacio('No hay datos de ventas para este período');
            const der = segs.length
                ? `<div class="rep-grafico"><h3 class="rep-grafico__titulo">Ventas por Método de Pago</h3>${chartPastel(segs)}<div class="rep-leyenda">${segs.map((s) => `<span><i style="background:${s.color}"></i>${esc(s.label)}: ${Math.round(s.val / totalMet * 100)}%</span>`).join('')}</div></div>`
                : panelVacio('No hay datos de ventas para este período');
            panel.innerHTML = `<div class="rep-fila">${izq}${der}</div>`;

        } else if (repTab === 'masvendidos') {
            const top = await api(`/api/reportes/mas-vendidos?desde=${desde}&hasta=${hasta}`);
            repExport = { nombre: 'mas-vendidos', filas: top.map((t) => ({ producto: t.nombre, cantidad: t.cantidad, ingreso: t.ingreso, margen: t.margen })) };
            panel.innerHTML = `<div class="rep-grafico"><h3 class="rep-grafico__titulo">Top 10 Productos Más Vendidos</h3>` +
                (top.length ? `<table class="rep-tabla"><thead><tr><th>#</th><th>Producto</th><th class="ta-der">Cantidad</th><th class="ta-der">Ingreso</th><th class="ta-der">Margen</th></tr></thead><tbody>` +
                    top.map((t, i) => `<tr><td>${i + 1}</td><td>${esc(t.nombre)}</td><td class="ta-der">${esc(t.cantidad)}</td><td class="ta-der">${soles(t.ingreso)}</td><td class="ta-der">${soles(t.margen)}</td></tr>`).join('') +
                    `</tbody></table>` : `<div class="vacio">${svg('vacio', 34)}<p>No hay datos de productos vendidos para este período</p></div>`) +
                `</div>`;

        }
    } catch (e) { panel.innerHTML = panelVacio('No se pudieron cargar los datos.'); notificarError(e); }
}

function pintarRepTabs() {
    const tabs = [
        { id: 'ventas', etiqueta: 'Ventas' },
        { id: 'masvendidos', etiqueta: 'Productos Más Vendidos' },
    ];
    document.getElementById('rep-tabs').innerHTML = tabs.map((t) =>
        `<button type="button" class="pill ${t.id === repTab ? 'pill--activo' : ''}" data-tab="${t.id}">${esc(t.etiqueta)}</button>`).join('');
}

function exportarCSV() {
    const filas = repExport.filas || [];
    if (!filas.length) { alert('No hay datos para exportar en esta vista.'); return; }
    const cols = Object.keys(filas[0]);
    const csv = [cols.join(',')].concat(filas.map((f) => cols.map((c) => `"${String(f[c] ?? '').replace(/"/g, '""')}"`).join(','))).join('\n');
    const blob = new Blob(['﻿' + csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url; a.download = `${repExport.nombre}-${hoyISO()}.csv`;
    document.body.appendChild(a); a.click(); document.body.removeChild(a);
    URL.revokeObjectURL(url);
}

function activarReportes() {
    pintarRepTabs();
    document.getElementById('rep-rango').addEventListener('change', cargarReportes);
    document.getElementById('rep-actualizar').addEventListener('click', cargarReportes);
    document.getElementById('rep-tabs').addEventListener('click', (e) => {
        const btn = e.target.closest('[data-tab]');
        if (!btn) return;
        repTab = btn.dataset.tab;
        pintarRepTabs();
        cargarReportes();
    });
    document.getElementById('rep-csv').addEventListener('click', exportarCSV);
    document.getElementById('rep-pdf').addEventListener('click', () => window.print());

    // Tooltips de gráficos: un único elemento flotante + delegación sobre el panel
    let tip = document.getElementById('grafico-tooltip');
    if (!tip) { tip = document.createElement('div'); tip.id = 'grafico-tooltip'; document.body.appendChild(tip); }
    const panel = document.getElementById('rep-panel');
    const mostrar = (e) => {
        const t = e.target.closest && e.target.closest('[data-tip]');
        if (!t) { tip.classList.remove('visible'); return; }
        tip.textContent = t.getAttribute('data-tip');
        tip.classList.add('visible');
        const margen = 14;
        let x = e.clientX + margen, y = e.clientY + margen;
        const r = tip.getBoundingClientRect();
        if (x + r.width > window.innerWidth) x = e.clientX - r.width - margen;
        if (y + r.height > window.innerHeight) y = e.clientY - r.height - margen;
        tip.style.left = x + 'px';
        tip.style.top = y + 'px';
    };
    panel.addEventListener('mousemove', mostrar);
    panel.addEventListener('mouseleave', () => tip.classList.remove('visible'));
}
