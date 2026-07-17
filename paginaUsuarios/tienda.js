/* MarketControl — Tienda pública. API sin login (/api/public/*).
   Delivery reserva stock y crea Pedido; Recojo solo abre WhatsApp, se cobra después en el POS. */

const API_BASE = "https://bodega-el-progreso-api-ecc0fqa4atc0dbe3.brazilsouth-01.azurewebsites.net";

let productosBD = [];
let productosActuales = [];
let carrito = cargarCarrito();
let productosGrid;
let config = { telefono_whatsapp: '', nombre_negocio: 'MarketControl', direccion: '', costo_envio: '0', envio_gratis_desde: '0' };
let tipoEntrega = 'delivery';

/* ---------- API pública (sin credenciales) ---------- */
async function apiPublic(path, opts = {}) {
    const cfg = { method: opts.method || 'GET', headers: {} };
    if (opts.body !== undefined) {
        cfg.headers['Content-Type'] = 'application/json';
        cfg.body = JSON.stringify(opts.body);
    }
    const r = await fetch(API_BASE + path, cfg);
    const t = await r.text();
    let d = null;
    if (t) { try { d = JSON.parse(t); } catch (_) { d = t; } }
    if (!r.ok) { throw new Error((d && d.error) ? d.error : ('Error ' + r.status)); }
    return d;
}

/* Normaliza el producto del backend (camelCase) al formato que usa la tienda. */
function normalizar(p) {
    return {
        IdProducto: p.idProducto,
        IdCategoria: p.idCategoria,
        CodigoSKU: p.codigoSku,
        NombreProducto: p.nombreProducto,
        PrecioVenta: Number(p.precioVenta),
        PorcentajeDescuento: Number(p.porcentajeDescuento || 0),
        StockActual: Number(p.stockActual),
        Marca: p.marca || '',
        imagen: p.imagenUrl || ''
    };
}

document.addEventListener('DOMContentLoaded', init);

async function init() {
    productosGrid = document.querySelector('.productos-grid');
    bindBuscador();
    bindOrdenar();
    bindCarrito();
    bindCheckout();

    try {
        const [prods, cats, cfg] = await Promise.all([
            apiPublic('/api/public/productos'),
            apiPublic('/api/public/categorias'),
            apiPublic('/api/public/configuracion').catch(() => config),
        ]);
        productosBD = (prods || []).map(normalizar);
        if (cfg) config = cfg;
        actualizarAnuncioEnvio();
        construirCategorias(cats || []);
        productosActuales = [...productosBD];
        renderizarProductos(productosActuales);
        actualizarCarritoUI();
        if (new URLSearchParams(window.location.search).get('checkout') === '1') abrirCheckout();
    } catch (e) {
        if (productosGrid) {
            productosGrid.innerHTML = `<p class="tienda-error">No se pudo cargar el catálogo.<br>Verifica que el servidor (Tomcat) esté activo.</p>`;
        }
    }
}

/* ---------- Anuncio de envío gratis (marquesina) ---------- */
const ICONO_CAMION = `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
    stroke-linecap="round" stroke-linejoin="round" width="16" height="16" aria-hidden="true">
    <rect x="1" y="3" width="15" height="13"></rect>
    <polygon points="16 8 20 8 23 11 23 16 16 16 16 8"></polygon>
    <circle cx="5.5" cy="18.5" r="2.5"></circle>
    <circle cx="18.5" cy="18.5" r="2.5"></circle>
</svg>`;

function actualizarAnuncioEnvio() {
    const pista = document.getElementById('marquesina-pista');
    if (!pista) return;

    const umbral = Number(config.envio_gratis_desde) || 0;
    const texto = umbral > 0
        ? `Envío GRATIS en compras desde S/ ${umbral.toFixed(2)}`
        : 'Envío GRATIS en todos los pedidos';
    const itemHTML = `<span class="marquesina__item">${ICONO_CAMION}${esc(texto)}</span>`;

    // Repite el texto las veces necesarias para cubrir el ancho de pantalla
    // (si no, en monitores anchos queda una franja vacía a la derecha).
    pista.style.animation = 'none';
    pista.innerHTML = itemHTML;
    const anchoItem = pista.firstElementChild.getBoundingClientRect().width || 200;
    const repeticiones = Math.max(Math.ceil(window.innerWidth / anchoItem) + 1, 2);
    const bloque = itemHTML.repeat(repeticiones);
    pista.innerHTML = bloque + bloque; // dos copias identicas: el loop no deja huecos

    const anchoPista = pista.getBoundingClientRect().width / 2;
    const velocidadPxPorSeg = 60;
    const duracion = Math.max(anchoPista / velocidadPxPorSeg, 8);
    pista.style.animation = `marquesina-desplazar ${duracion}s linear infinite`;
}

let anuncioResizeTimer;
window.addEventListener('resize', () => {
    clearTimeout(anuncioResizeTimer);
    anuncioResizeTimer = setTimeout(actualizarAnuncioEnvio, 250);
});

/* ---------- Categorías dinámicas ---------- */
function construirCategorias(categorias) {
    const cont = document.querySelector('.categoria');
    if (!cont) return;
    const activas = categorias.filter((c) => c.estado !== false);
    cont.innerHTML = `<button type="button" class="categoria-producto activo" data-categoria="TODOS">Todos</button>` +
        activas.map((c) => `<button type="button" class="categoria-producto" data-categoria="${c.idCategoria}">${esc(c.nombreCategoria)}</button>`).join('');

    cont.addEventListener('click', (e) => {
        const boton = e.target.closest('.categoria-producto');
        if (!boton) return;
        document.querySelector('.categoria-producto.activo')?.classList.remove('activo');
        boton.classList.add('activo');
        const sel = boton.dataset.categoria;
        productosActuales = (sel === 'TODOS')
            ? [...productosBD]
            : productosBD.filter((p) => p.IdCategoria === parseInt(sel, 10));
        renderizarProductos(productosActuales);
    });
}

/* ---------- Buscador ---------- */
function bindBuscador() {
    const botonBuscar = document.querySelector('.boton-buscar');
    const formBuscar = document.querySelector('.barra-superior form');
    const inputBuscar = document.querySelector('.buscador-lupa');
    if (botonBuscar && formBuscar) {
        botonBuscar.addEventListener('click', () => formBuscar.classList.toggle('abierto'));
    }
    if (formBuscar) formBuscar.addEventListener('submit', (e) => e.preventDefault());
    if (inputBuscar) {
        inputBuscar.addEventListener('input', () => {
            const q = inputBuscar.value.toLowerCase().trim();
            productosActuales = productosBD.filter((p) => p.NombreProducto.toLowerCase().includes(q));
            renderizarProductos(productosActuales);
        });
    }
}

/* ---------- Ordenar ---------- */
function bindOrdenar() {
    const contenedorOrdenar = document.querySelector('.ordenar-producto');
    const botonOrdenar = document.querySelector('.boton-ordenar');
    const opcionesOrdenar = document.querySelectorAll('.ordenar-opciones button');
    const etiquetaOrden = document.querySelector('.orden-actual');
    if (botonOrdenar && contenedorOrdenar) {
        botonOrdenar.addEventListener('click', (e) => { e.stopPropagation(); contenedorOrdenar.classList.toggle('abierto'); });
        document.addEventListener('click', () => contenedorOrdenar.classList.remove('abierto'));
    }
    opcionesOrdenar.forEach((opcion) => {
        opcion.addEventListener('click', (e) => {
            const criterio = e.target.dataset.orden;
            if (etiquetaOrden) etiquetaOrden.textContent = e.target.textContent;
            ordenarProductos(criterio);
            contenedorOrdenar.classList.remove('abierto');
        });
    });
}

function obtenerPrecioFinal(p) {
    const tiene = p.PorcentajeDescuento && p.PorcentajeDescuento > 0;
    return tiene ? p.PrecioVenta - (p.PrecioVenta * (p.PorcentajeDescuento / 100)) : p.PrecioVenta;
}

function ordenarProductos(criterio) {
    if (criterio === 'precio-asc') productosActuales.sort((a, b) => obtenerPrecioFinal(a) - obtenerPrecioFinal(b));
    else if (criterio === 'precio-desc') productosActuales.sort((a, b) => obtenerPrecioFinal(b) - obtenerPrecioFinal(a));
    else if (criterio === 'nombre-az') productosActuales.sort((a, b) => a.NombreProducto.localeCompare(b.NombreProducto));
    renderizarProductos(productosActuales);
}

/* ---------- Render del catálogo ---------- */
function renderizarProductos(lista) {
    if (!productosGrid) return;
    const contador = document.querySelector('.cantidad-producto');
    if (contador) contador.textContent = lista.length;

    productosGrid.innerHTML = lista.map((producto) => {
        const tiene = producto.PorcentajeDescuento && producto.PorcentajeDescuento > 0;
        const precioFinal = obtenerPrecioFinal(producto);
        const agotado = producto.StockActual <= 0;
        const etiqueta = tiene ? `<div class="etiqueta-contenedor"><span>Oferta </span><span>-${producto.PorcentajeDescuento}%</span></div>` : '';
        const precios = tiene
            ? `<span class="precio-actual">S/${precioFinal.toFixed(2)}</span><span class="precio-antiguo">S/${producto.PrecioVenta.toFixed(2)}</span>`
            : `<span class="precio-actual">S/${producto.PrecioVenta.toFixed(2)}</span>`;
        const imagen = producto.imagen
            ? `<img src="${esc(producto.imagen)}" alt="${esc(producto.NombreProducto)}" class="imagen-producto" loading="lazy" onerror="this.style.visibility='hidden'">`
            : `<div class="imagen-producto imagen-vacia"></div>`;
        const boton = agotado
            ? `<button type="button" class="boton-agregar" disabled>Agotado</button>`
            : `<button type="button" class="boton-agregar"> + Agregar </button>`;
        return `
            <div class="producto-card ${agotado ? 'agotado' : ''}" data-id="${producto.IdProducto}">
                <a class="producto-card-link" href="producto.html?id=${encodeURIComponent(producto.IdProducto)}">
                    <div class="producto-visual">${imagen}${etiqueta}</div>
                    <div class="producto-detalles">
                        <span class="producto-marca">${esc(producto.Marca)}</span>
                        <h3 class="producto-nombre">${esc(producto.NombreProducto)}</h3>
                    </div>
                </a>
                <div class="producto-compra">
                    <div class="precios">${precios}</div>
                    ${boton}
                </div>
            </div>`;
    }).join('');
}

/* ---------- Carrito ---------- */
function bindCarrito() {
    const botonCarrito = document.querySelector('.boton-carrito');
    const botonCerrarCarrito = document.querySelector('.carrito-cerrar');
    const sidebarCarrito = document.querySelector('.carrito-sidebar');
    const overlay = document.querySelector('.overlay');
    const abrir = () => { sidebarCarrito.classList.add('mostrar'); overlay.classList.add('mostrar'); };
    const cerrar = () => { sidebarCarrito.classList.remove('mostrar'); overlay.classList.remove('mostrar'); };
    botonCarrito?.addEventListener('click', abrir);
    botonCerrarCarrito?.addEventListener('click', cerrar);
    overlay?.addEventListener('click', cerrar);

    window.addEventListener('storage', (e) => {
        if (e.key === CARRITO_STORAGE_KEY) {
            carrito = cargarCarrito();
            actualizarCarritoUI();
        }
    });

    if (productosGrid) {
        productosGrid.addEventListener('click', (e) => {
            if (e.target.classList.contains('boton-agregar') && !e.target.disabled) {
                const tarjeta = e.target.closest('.producto-card');
                agregarProductoAlCarrito(parseInt(tarjeta.getAttribute('data-id'), 10));
            }
        });
    }

    const cuerpo = document.querySelector('.carrito-cuerpo');
    if (cuerpo) {
        cuerpo.addEventListener('click', (e) => {
            const restar = e.target.closest('.btn-restar');
            const sumar = e.target.closest('.btn-sumar');
            const eliminar = e.target.closest('.btn-eliminar');
            const pagar = e.target.closest('.btn-pagar');
            if (restar) modificarCantidadCarrito(parseInt(restar.dataset.id, 10), -1);
            if (sumar) modificarCantidadCarrito(parseInt(sumar.dataset.id, 10), 1);
            if (eliminar) eliminarDelCarrito(parseInt(eliminar.dataset.id, 10));
            if (pagar) abrirCheckout();
        });
    }
}

function agregarProductoAlCarrito(id) {
    const p = productosBD.find((x) => x.IdProducto === id);
    if (!p) return;
    const en = carrito.find((x) => x.IdProducto === id);
    if (en) { en.cantidad++; }
    else {
        carrito.push({ IdProducto: p.IdProducto, Nombre: p.NombreProducto, Precio: obtenerPrecioFinal(p), cantidad: 1, imagen: p.imagen });
    }
    guardarCarrito(carrito);
    actualizarCarritoUI();
}

function modificarCantidadCarrito(id, cambio) {
    const i = carrito.findIndex((x) => x.IdProducto === id);
    if (i > -1) {
        carrito[i].cantidad += cambio;
        if (carrito[i].cantidad <= 0) carrito.splice(i, 1);
        guardarCarrito(carrito);
        actualizarCarritoUI();
    }
}

function eliminarDelCarrito(id) {
    carrito = carrito.filter((x) => x.IdProducto !== id);
    guardarCarrito(carrito);
    actualizarCarritoUI();
}

function totalCarrito() {
    return carrito.reduce((s, i) => s + i.Precio * i.cantidad, 0);
}

function actualizarCarritoUI() {
    const cuerpo = document.querySelector('.carrito-cuerpo');
    const badge = document.querySelector('.carrito-badge');
    if (!cuerpo) return;
    const cantidadTotal = carrito.reduce((s, i) => s + i.cantidad, 0);
    if (badge) { badge.textContent = cantidadTotal; badge.hidden = cantidadTotal === 0; }

    if (carrito.length === 0) {
        cuerpo.classList.add('vacio');
        cuerpo.innerHTML = `<div class="carrito-img"></div><h4>Carrito vacío</h4><p>Agrega productos para comenzar</p>`;
        return;
    }
    cuerpo.classList.remove('vacio');
    const items = carrito.map((item) => {
        const subtotal = item.Precio * item.cantidad;
        const img = item.imagen ? `<img src="${esc(item.imagen)}" alt="${esc(item.Nombre)}" class="carrito-item-img" onerror="this.style.visibility='hidden'">` : `<div class="carrito-item-img"></div>`;
        return `
            <div class="carrito-item">
                ${img}
                <div class="carrito-item-info">
                    <h5>${esc(item.Nombre)}
                        <button class="btn-eliminar" data-id="${item.IdProducto}" aria-label="Eliminar">&times;</button>
                    </h5>
                    <div class="carrito-item-controles">
                        <div class="contador-cantidad">
                            <button class="btn-restar" data-id="${item.IdProducto}">&minus;</button>
                            <span>${item.cantidad}</span>
                            <button class="btn-sumar" data-id="${item.IdProducto}">+</button>
                        </div>
                        <span class="carrito-item-precio">S/${subtotal.toFixed(2)}</span>
                    </div>
                </div>
            </div>`;
    }).join('');
    cuerpo.innerHTML = `${items}
        <div class="carrito-resumen">
            <div class="carrito-total"><span>Total:</span><span class="carrito-total-monto">S/${totalCarrito().toFixed(2)}</span></div>
            <button class="boton-accion btn-pagar">Finalizar pedido</button>
        </div>`;
}

/* ---------- Checkout (Delivery / Recojo) + WhatsApp ---------- */
function bindCheckout() {
    const overlay = document.getElementById('checkout-overlay');
    if (!overlay) return;
    const cerrar = () => { overlay.hidden = true; };
    document.getElementById('checkout-cerrar').addEventListener('click', cerrar);
    overlay.addEventListener('click', (e) => { if (e.target === overlay) cerrar(); });

    document.querySelector('.checkout-tipo').addEventListener('click', (e) => {
        const btn = e.target.closest('.tipo-btn');
        if (!btn) return;
        document.querySelector('.tipo-btn.activo')?.classList.remove('activo');
        btn.classList.add('activo');
        tipoEntrega = btn.dataset.tipo;
        const soloDel = document.getElementById('solo-delivery');
        const ayuda = document.getElementById('checkout-ayuda');
        const dir = document.getElementById('ck-direccion');
        if (tipoEntrega === 'recojo') {
            soloDel.hidden = true;
            dir.required = false;
            ayuda.textContent = config.direccion
                ? `Pasa por la tienda a recoger y pagar tu pedido. ${config.direccion}`
                : 'Pasa por la tienda a recoger y pagar tu pedido.';
        } else {
            soloDel.hidden = false;
            dir.required = true;
            ayuda.textContent = 'El pedido se registra y te contactamos para coordinar la entrega.';
        }
        refrescarTotalCheckout();
    });

    document.getElementById('checkout-form').addEventListener('submit', enviarPedido);
}

/* Envío para un subtotal dado: solo Delivery; gratis si supera el umbral. */
function calcularEnvio(subtotal) {
    if (tipoEntrega !== 'delivery') return 0;
    const tarifa = Number(config.costo_envio) || 0;
    const umbral = Number(config.envio_gratis_desde) || 0;
    return (umbral > 0 && subtotal >= umbral) ? 0 : tarifa;
}

/* Pinta el desglose Subtotal / Envío / Total en el checkout. */
function refrescarTotalCheckout() {
    const subtotal = totalCarrito();
    const envio = calcularEnvio(subtotal);
    const total = subtotal + envio;
    const cont = document.getElementById('ck-desglose');
    if (cont) {
        cont.innerHTML = (tipoEntrega === 'delivery')
            ? `<div class="ck-linea"><span>Subtotal productos</span><span>S/${subtotal.toFixed(2)}</span></div>
               <div class="ck-linea"><span>Envío</span><span>${envio === 0 ? 'Gratis' : 'S/' + envio.toFixed(2)}</span></div>`
            : '';
    }
    document.getElementById('ck-total').textContent = 'S/' + total.toFixed(2);
}

function abrirCheckout() {
    if (carrito.length === 0) return;
    refrescarTotalCheckout();
    document.getElementById('ck-nota').hidden = true;
    document.getElementById('checkout-overlay').hidden = false;
}

async function enviarPedido(e) {
    e.preventDefault();
    const nombres = document.getElementById('ck-nombres').value.trim();
    const telefono = document.getElementById('ck-telefono').value.trim();
    const direccion = document.getElementById('ck-direccion').value.trim();
    const referencia = document.getElementById('ck-referencia').value.trim();
    const obs = document.getElementById('ck-obs').value.trim();
    const metodoPago = document.querySelector('input[name="ck-metodo"]:checked')?.value || '';

    if (!nombres || !telefono) { return; }
    if (tipoEntrega === 'delivery' && !direccion) {
        notaCheckout('Ingresa la dirección de entrega para el delivery.', true);
        return;
    }
    if (tipoEntrega === 'delivery' && !metodoPago) {
        notaCheckout('Selecciona un método de pago.', true);
        return;
    }

    const boton = document.getElementById('ck-enviar');
    boton.disabled = true;
    try {
        // Ambos flujos registran el pedido (Delivery reserva stock; Recojo no, baja al cobrar en POS).
        const creado = await apiPublic('/api/public/pedidos', {
            method: 'POST',
            body: {
                tipo: tipoEntrega,
                cliente: { nombres, telefono, direccion, referencia },
                items: carrito.map((i) => ({ idProducto: i.IdProducto, cantidad: i.cantidad })),
                observaciones: obs,
                metodoPago,
            },
        });
        const numeroPedido = (creado && creado.idPedido) ? creado.idPedido : null;
        const subtotalProductos = totalCarrito();
        const envioSrv = creado && creado.costoEnvio != null ? Number(creado.costoEnvio) : calcularEnvio(subtotalProductos);
        const totalSrv = creado && creado.total != null ? Number(creado.total) : (subtotalProductos + envioSrv);
        const lineasCarrito = carrito.map((i) => `• ${i.cantidad}x ${i.Nombre} — S/${(i.Precio * i.cantidad).toFixed(2)}`).join('\n');

        abrirWhatsApp({ nombres, telefono, direccion, referencia, obs, metodoPago, numeroPedido, envio: envioSrv, total: totalSrv, lineas: lineasCarrito });

        carrito = [];
        guardarCarrito(carrito);
        actualizarCarritoUI();
        document.getElementById('checkout-overlay').hidden = true;
        document.querySelector('.carrito-sidebar')?.classList.remove('mostrar');
        document.querySelector('.overlay')?.classList.remove('mostrar');
        document.getElementById('checkout-form').reset();
        mostrarToastTienda(tipoEntrega === 'delivery'
            ? 'Pedido de delivery registrado. Abriendo WhatsApp…'
            : 'Pedido de recojo registrado. Abriendo WhatsApp…');
    } catch (err) {
        notaCheckout(err.message || 'No se pudo registrar el pedido. Inténtalo de nuevo.', true);
    } finally {
        boton.disabled = false;
    }
}

function abrirWhatsApp(datos) {
    const tipoTxt = tipoEntrega === 'delivery' ? 'DELIVERY' : 'RECOJO EN TIENDA';
    let msg = `Hola ${config.nombre_negocio}, quiero hacer un pedido (${tipoTxt}):\n\n${datos.lineas}\n`;
    if (tipoEntrega === 'delivery') {
        const subtotal = (datos.total - datos.envio).toFixed(2);
        msg += `\nSubtotal productos: S/${subtotal}`;
        msg += `\nEnvío: ${datos.envio === 0 ? 'Gratis' : 'S/' + datos.envio.toFixed(2)}`;
    }
    msg += `\n*Total a pagar: S/${datos.total.toFixed(2)}*\n\nCliente: ${datos.nombres}\nTeléfono: ${datos.telefono}`;
    if (tipoEntrega === 'delivery') {
        msg += `\nDirección: ${datos.direccion}`;
        if (datos.referencia) msg += `\nReferencia: ${datos.referencia}`;
        msg += `\nMétodo de pago: ${datos.metodoPago}`;
    }
    if (datos.obs) msg += `\nNota: ${datos.obs}`;
    if (datos.numeroPedido) msg += `\n\nN° de pedido: PD-${String(datos.numeroPedido).padStart(3, '0')}`;

    const tel = (config.telefono_whatsapp || '').replace(/\D/g, '');
    const url = `https://wa.me/${tel}?text=${encodeURIComponent(msg)}`;
    window.open(url, '_blank');
}

function notaCheckout(texto, error) {
    const n = document.getElementById('ck-nota');
    n.textContent = texto;
    n.hidden = false;
    n.classList.toggle('error', !!error);
}

let toastTimer = null;
function mostrarToastTienda(texto) {
    const t = document.getElementById('tienda-toast');
    if (!t) return;
    t.textContent = texto;
    t.hidden = false;
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => { t.hidden = true; }, 3500);
}

function esc(t) {
    return String(t == null ? '' : t).replace(/[&<>"']/g, (c) => (
        { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]
    ));
}