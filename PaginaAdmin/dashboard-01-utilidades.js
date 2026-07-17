/* MarketControl — Dashboard. Todo viene de la API real (fetch con credentials:'include' para la cookie de sesión).
   Primer archivo cargado: define API_BASE, iconos y utilidades que usan los demás. */

/* ---------- 0. CONFIGURACIÓN DE LA API ---------- */
// Ajusta esta base a donde despliegues el WAR en Tomcat.
const API_BASE = "https://bodega-el-progreso-api-ecc0fqa4atc0dbe3.brazilsouth-01.azurewebsites.net";

class ApiError extends Error {
    constructor(mensaje, status) { super(mensaje); this.status = status; }
}

async function api(path, opts = {}) {
    const cfg = {
        method: opts.method || 'GET',
        credentials: 'include',          // <- la cookie de sesión viaja al backend
        headers: {},
    };
    if (opts.body !== undefined) {
        cfg.headers['Content-Type'] = 'application/json';
        cfg.body = JSON.stringify(opts.body);
    }
    const resp = await fetch(API_BASE + path, cfg);
    const texto = await resp.text();
    let data = null;
    if (texto) { try { data = JSON.parse(texto); } catch (_) { data = texto; } }
    if (!resp.ok) {
        const msg = (data && data.error) ? data.error : ('Error ' + resp.status);
        throw new ApiError(msg, resp.status);
    }
    return data;
}

/* ---------- 1. ÍCONOS SVG (cero emojis) ---------- */
const ICONOS = {
    dashboard: '<rect x="3" y="3" width="7" height="7" rx="1.5"/><rect x="14" y="3" width="7" height="7" rx="1.5"/><rect x="14" y="14" width="7" height="7" rx="1.5"/><rect x="3" y="14" width="7" height="7" rx="1.5"/>',
    caja: '<path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/><polyline points="3.27 6.96 12 12.01 20.73 6.96"/><line x1="12" y1="22.08" x2="12" y2="12"/>',
    carrito: '<circle cx="9" cy="21" r="1"/><circle cx="20" cy="21" r="1"/><path d="M1 1h4l2.7 13.4a2 2 0 0 0 2 1.6h9.7a2 2 0 0 0 2-1.6L23 6H6"/>',
    camion: '<rect x="1" y="3" width="15" height="13" rx="1.5"/><path d="M16 8h4l3 3v5h-7z"/><circle cx="5.5" cy="18.5" r="2.5"/><circle cx="18.5" cy="18.5" r="2.5"/>',
    reportes: '<line x1="6" y1="20" x2="6" y2="14"/><line x1="12" y1="20" x2="12" y2="9"/><line x1="18" y1="20" x2="18" y2="4"/>',
    engranaje: '<circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/>',
    dinero: '<line x1="12" y1="1" x2="12" y2="23"/><path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/>',
    tendencia: '<polyline points="23 6 13.5 15.5 8.5 10.5 1 18"/><polyline points="17 6 23 6 23 12"/>',
    efectivo: '<rect x="2" y="6" width="20" height="12" rx="2"/><circle cx="12" cy="12" r="2.5"/>',
    telefono: '<rect x="6" y="2" width="12" height="20" rx="2.5"/><line x1="11" y1="18" x2="13" y2="18"/>',
    moto: '<circle cx="6" cy="18" r="3"/><circle cx="18" cy="18" r="3"/><path d="M9 18h6"/><path d="M6 15l3-7h4l3 7"/><path d="M11 8V5h3"/>',
    chevron: '<polyline points="9 18 15 12 9 6"/>',
    persona: '<path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/>',
    lapiz: '<path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.12 2.12 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>',
    basura: '<polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/><line x1="10" y1="11" x2="10" y2="17"/><line x1="14" y1="11" x2="14" y2="17"/>',
    mas: '<line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>',
    menos: '<line x1="5" y1="12" x2="19" y2="12"/>',
    check: '<polyline points="20 6 9 17 4 12"/>',
    mascirculo: '<circle cx="12" cy="12" r="9"/><line x1="12" y1="8" x2="12" y2="16"/><line x1="8" y1="12" x2="16" y2="12"/>',
    corona: '<path d="M3 19h18"/><path d="M3 8l5 4 4-7 4 7 5-4-2 9H5z"/>',
    puntos: '<circle cx="12" cy="5" r="1.2"/><circle cx="12" cy="12" r="1.2"/><circle cx="12" cy="19" r="1.2"/>',
    reloj: '<circle cx="12" cy="12" r="9"/><polyline points="12 7 12 12 15 14"/>',
    pin: '<path d="M21 10c0 7-9 12-9 12s-9-5-9-12a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/>',
    paquete: '<path d="M16.5 9.4 7.5 4.21"/><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/><polyline points="3.27 6.96 12 12.01 20.73 6.96"/><line x1="12" y1="22.08" x2="12" y2="12"/>',
    vacio: '<path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/><polyline points="3.27 6.96 12 12.01 20.73 6.96"/>',
};

function svg(nombre, ancho) {
    const trazo = ICONOS[nombre] || '';
    const w = ancho || 20;
    return `<svg viewBox="0 0 24 24" width="${w}" height="${w}" fill="none" stroke="currentColor" `
        + `stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${trazo}</svg>`;
}

/* ---------- 2. UTILIDADES ---------- */
const soles = (n) => 'S/ ' + Number(n || 0).toFixed(2);

function esc(t) {
    return String(t == null ? '' : t).replace(/[&<>"']/g, (c) => (
        { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]
    ));
}

const METODOS = {
    Efectivo: { clase: 'metodo--efectivo', icono: 'efectivo' },
    Yape: { clase: 'metodo--yape', icono: 'telefono' },
};

const ESTADO_CLASE = {
    'Pendiente': 'estado--pendiente',
    'Preparando': 'estado--preparando',
    'En Tránsito': 'estado--transito',
    'Entregado': 'estado--entregado',
    'Cancelado': 'estado--cancelado',
};

function zfill(n) { return String(n).padStart(2, '0'); }
function isoLocal(d) { return d.getFullYear() + '-' + zfill(d.getMonth() + 1) + '-' + zfill(d.getDate()); }
function hoyISO() { return isoLocal(new Date()); }

function rangoDe(clave) {
    const hoy = new Date();
    let desde = new Date(hoy);
    if (clave === 'semana') { const dow = (hoy.getDay() + 6) % 7; desde.setDate(hoy.getDate() - dow); }
    else if (clave === 'mes') { desde = new Date(hoy.getFullYear(), hoy.getMonth(), 1); }
    else if (clave === 'anio') { desde = new Date(hoy.getFullYear(), 0, 1); }
    return { desde: isoLocal(desde), hasta: isoLocal(hoy) };
}

function fmtFecha(iso) {
    if (!iso) return '—';
    const d = new Date(iso);
    if (isNaN(d.getTime())) return iso;
    return `${d.getDate()}/${d.getMonth() + 1}/${String(d.getFullYear()).slice(2)}, ${zfill(d.getHours())}:${zfill(d.getMinutes())}`;
}
function etaDe(iso) {
    const d = new Date(iso);
    if (isNaN(d.getTime())) return '—';
    d.setMinutes(d.getMinutes() + 45);
    return `${zfill(d.getHours())}:${zfill(d.getMinutes())}`;
}
function esHoy(iso) {
    const d = new Date(iso); const h = new Date();
    return d.getFullYear() === h.getFullYear() && d.getMonth() === h.getMonth() && d.getDate() === h.getDate();
}
function fechaPeruAhora() { return new Date().toLocaleString('es-PE', { timeZone: 'America/Lima' }); }
