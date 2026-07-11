/* ---------- 3. ESTADO GLOBAL ---------- */
let usuarioActual = null;        // { idUsuario, nombre, rol }
let productosCache = [];
let categoriasCache = [];
let categoriaPorId = {};
let nombrePorProducto = {};
let usuariosCache = [];
const carrito = [];
const TASA_IGV = 0.18;   // Los precios YA incluyen IGV; se extrae, no se suma encima.
let pedidoRecojoActivo = null;   // id del pedido de recojo importado al POS (o null)
let toastTimer = null;
let editandoId = null;
let editandoCategoriaId = null;
let categoriasListadoCache = [];

const NAV = [
    { id: 'dashboard', etiqueta: 'Dashboard', icono: 'dashboard' },
    { id: 'inventario', etiqueta: 'Inventario', icono: 'caja' },
    { id: 'pos', etiqueta: 'Punto de Venta', icono: 'carrito' },
    { id: 'delivery', etiqueta: 'Delivery', icono: 'camion' },
    { id: 'reportes', etiqueta: 'Reportes', icono: 'reportes' },
    { id: 'config', etiqueta: 'Configuración', icono: 'engranaje' },
];

function notificarError(e) {
    if (e instanceof ApiError && e.status === 401) { window.location.href = 'index.html'; return; }
    alert((e && e.message) ? e.message : 'Ocurrió un error de conexión con el servidor.');
}

function nombreCat(id) { return categoriaPorId[id] || 'Sin categoría'; }
