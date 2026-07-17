/* MarketControl — Página de detalle de producto. */
const API_BASE = "https://bodega-el-progreso-api-ecc0fqa4atc0dbe3.brazilsouth-01.azurewebsites.net/MarketControl";

let carrito = cargarCarrito();
let productoActual = null;
let cantidadSeleccionada = 1;

document.addEventListener('DOMContentLoaded', init);

async function init() {
    bindCarrito();
    actualizarCarritoUI();
    window.addEventListener('storage', (e) => {
        if (e.key === CARRITO_STORAGE_KEY) {
            carrito = cargarCarrito();
            actualizarCarritoUI();
        }
    });

    const contenedor = document.getElementById('producto-contenido');
    const id = parseInt(new URLSearchParams(window.location.search).get('id'), 10);
    if (!id) {
        contenedor.innerHTML = `<p class="producto-error">Producto no especificado.</p>`;
        return;
    }

    try {
        const r = await fetch(`${API_BASE}/api/public/productos`);
        const productos = await r.json();
        const p = (productos || []).find((x) => x.idProducto === id);
        if (!p) {
            contenedor.innerHTML = `<p class="producto-error">Este producto ya no está disponible.</p>`;
            return;
        }
        productoActual = normalizar(p);
        document.title = `${productoActual.NombreProducto} · Bodega el Progreso`;
        renderizarProducto(contenedor);

        const similares = (productos || [])
            .filter((x) => x.idCategoria === p.idCategoria && x.idProducto !== id && x.estado !== false)
            .map(normalizar);
        renderizarSimilares(similares);
    } catch (e) {
        contenedor.innerHTML = `<p class="producto-error">No se pudo cargar el producto.<br>Verifica que el servidor (Tomcat) esté activo.</p>`;
    }
}

function normalizar(p) {
    return {
        IdProducto: p.idProducto,
        IdCategoria: p.idCategoria,
        NombreProducto: p.nombreProducto,
        Descripcion: p.descripcion || '',
        Marca: p.marca || '',
        PrecioVenta: Number(p.precioVenta),
        PorcentajeDescuento: Number(p.porcentajeDescuento || 0),
        StockActual: Number(p.stockActual),
        StockMinimo: Number(p.stockMinimo || 0),
        imagen: p.imagenUrl || '',
    };
}

function obtenerPrecioFinal(p) {
    const tiene = p.PorcentajeDescuento && p.PorcentajeDescuento > 0;
    return tiene ? p.PrecioVenta - (p.PrecioVenta * (p.PorcentajeDescuento / 100)) : p.PrecioVenta;
}

/* Disponibilidad inmediata (>=15) / Stock limitado (entre el mínimo y 15) / Solo quedan N (en el mínimo o por debajo). */
function mensajeStock(p) {
    if (p.StockActual <= 0) return { texto: 'Agotado', clase: 'agotado' };
    if (p.StockActual >= 15) return { texto: 'Disponibilidad inmediata', clase: 'disponible' };
    if (p.StockActual > p.StockMinimo) return { texto: 'Stock limitado', clase: 'limitado' };
    return { texto: `Solo quedan ${p.StockActual} unidades disponibles`, clase: 'critico' };
}

function renderizarProducto(contenedor) {
    const p = productoActual;
    const tiene = p.PorcentajeDescuento && p.PorcentajeDescuento > 0;
    const precioFinal = obtenerPrecioFinal(p);
    const agotado = p.StockActual <= 0;
    const stock = mensajeStock(p);

    const etiqueta = tiene ? `<div class="etiqueta-contenedor"><span>Oferta </span><span>-${p.PorcentajeDescuento}%</span></div>` : '';
    const precios = tiene
        ? `<span class="precio-actual">S/${precioFinal.toFixed(2)}</span><span class="precio-antiguo">S/${p.PrecioVenta.toFixed(2)}</span>`
        : `<span class="precio-actual">S/${p.PrecioVenta.toFixed(2)}</span>`;
    const imagen = p.imagen
        ? `<img src="${esc(p.imagen)}" alt="${esc(p.NombreProducto)}" class="producto-detalle-imagen" onerror="this.style.visibility='hidden'">`
        : `<div class="producto-detalle-imagen imagen-vacia"></div>`;
    const descripcion = p.Descripcion
        ? `<p class="producto-detalle-descripcion">${esc(p.Descripcion)}</p>`
        : `<p class="producto-detalle-descripcion sin-descripcion">Sin descripción disponible.</p>`;

    contenedor.innerHTML = `
        <div class="producto-detalle">
            <div class="producto-detalle-visual">${imagen}${etiqueta}</div>
            <div class="producto-detalle-info">
                <span class="producto-marca">${esc(p.Marca)}</span>
                <h1 class="producto-detalle-nombre">${esc(p.NombreProducto)}</h1>
                <div class="producto-detalle-precios">${precios}</div>
                ${descripcion}
                <p class="producto-detalle-stock ${stock.clase}">${stock.texto}</p>
                <div class="cantidad-selector" ${agotado ? 'hidden' : ''}>
                    <button type="button" class="cant-menos" aria-label="Quitar una unidad">&minus;</button>
                    <span class="cant-valor">1</span>
                    <button type="button" class="cant-mas" aria-label="Agregar una unidad">+</button>
                </div>
                <button type="button" class="boton-accion producto-detalle-agregar" ${agotado ? 'disabled' : ''}>
                    ${agotado ? 'Agotado' : '+ Agregar al carrito'}
                </button>
            </div>
        </div>`;

    if (!agotado) bindAcciones(contenedor);
}

function bindAcciones(contenedor) {
    const valor = contenedor.querySelector('.cant-valor');

    contenedor.querySelector('.cant-menos').addEventListener('click', () => {
        cantidadSeleccionada = Math.max(1, cantidadSeleccionada - 1);
        valor.textContent = cantidadSeleccionada;
    });

    contenedor.querySelector('.cant-mas').addEventListener('click', () => {
        cantidadSeleccionada = Math.min(productoActual.StockActual, cantidadSeleccionada + 1);
        valor.textContent = cantidadSeleccionada;
    });

    contenedor.querySelector('.producto-detalle-agregar').addEventListener('click', () => {
        agregarProductoAlCarrito(productoActual, cantidadSeleccionada);
        cantidadSeleccionada = 1;
        valor.textContent = 1;
    });
}

/* ---------- Productos similares (misma categoría) ---------- */
function renderizarSimilares(lista) {
    const seccion = document.getElementById('similares-seccion');
    const contenedor = document.getElementById('similares-lista');
    if (!seccion || !contenedor) return;
    if (lista.length === 0) {
        seccion.hidden = true;
        return;
    }

    contenedor.innerHTML = lista.map((p) => {
        const precioFinal = obtenerPrecioFinal(p);
        const imagen = p.imagen
            ? `<img src="${esc(p.imagen)}" alt="${esc(p.NombreProducto)}" loading="lazy" onerror="this.style.visibility='hidden'">`
            : `<div class="imagen-vacia"></div>`;
        return `
            <a class="similar-card" href="producto.html?id=${encodeURIComponent(p.IdProducto)}">
                <div class="similar-card-visual">${imagen}</div>
                <span class="producto-marca">${esc(p.Marca)}</span>
                <h3 class="similar-card-nombre">${esc(p.NombreProducto)}</h3>
                <span class="precio-actual">S/${precioFinal.toFixed(2)}</span>
            </a>`;
    }).join('');

    seccion.hidden = false;

    const desplazar = (dir) => contenedor.scrollBy({ left: dir * 340, behavior: 'smooth' });
    document.querySelector('.similar-flecha-izq').addEventListener('click', () => desplazar(-1));
    document.querySelector('.similar-flecha-der').addEventListener('click', () => desplazar(1));
}

/* ---------- Carrito (sidebar funcional, compartido vía localStorage) ---------- */
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
            if (pagar) window.location.href = 'tienda.html?checkout=1';
        });
    }
}

function agregarProductoAlCarrito(p, cantidad) {
    const en = carrito.find((x) => x.IdProducto === p.IdProducto);
    if (en) { en.cantidad += cantidad; }
    else { carrito.push({ IdProducto: p.IdProducto, Nombre: p.NombreProducto, Precio: obtenerPrecioFinal(p), cantidad, imagen: p.imagen }); }
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
            <button class="boton-accion btn-pagar">Ir a pagar</button>
        </div>`;
}

function esc(t) {
    return String(t == null ? '' : t).replace(/[&<>"']/g, (c) => (
        { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]
    ));
}
