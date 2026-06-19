const productosBD = [
    {
        IdProducto: 1,
        IdCategoria: 1,
        CodigoSKU: "PROD-001",
        NombreProducto: "Leche entera Gloria 1L",
        PrecioVenta: 5.20,
        PorcentajeDescuento: 15,
        StockActual: 45,
        StockMinimo: 10,
        Marca: "Gloria",
        imagen: "https://i.ebayimg.com/images/g/NmAAAeSw36Nono0C/s-l1600.webp"
    },
    {
        IdProducto: 2,
        IdCategoria: 4,
        CodigoSKU: "PROD-002",
        NombreProducto: "Pan de Molde Blanco Familiar",
        PrecioVenta: 7.20,
        PorcentajeDescuento: 0,
        StockActual: 20,
        StockMinimo: 5,
        Marca: "Bimbo",
        imagen: "https://metroio.vtexassets.com/arquivos/ids/514803-1200-auto?v=638448465909970000&width=1200&height=auto&aspect=true"
    }
];


let productosGrid;
let carrito = [];
let productosActuales = [...productosBD];

document.addEventListener('DOMContentLoaded', () => {

    productosGrid = document.querySelector('.productos-grid');
    renderizarProductos(productosActuales);

    const botonBuscar = document.querySelector('.boton-buscar');
    const formBuscar = document.querySelector('.barra-superior form');
    const inputBuscar = document.querySelector('.buscador-lupa');

    if (botonBuscar && formBuscar) {
        botonBuscar.addEventListener('click', () => {
            formBuscar.classList.toggle('abierto');
        });
    }

    if (formBuscar) {
        formBuscar.addEventListener('submit', e => e.preventDefault());
    }

    if (inputBuscar) {
        inputBuscar.addEventListener('input', () => {
            const textoBusqueda = inputBuscar.value.toLowerCase().trim();
            productosActuales = productosBD.filter(producto => {
                return producto.NombreProducto.toLowerCase().includes(textoBusqueda);
            });
            renderizarProductos(productosActuales);
        });
    }

    const botonesCategoria = document.querySelectorAll('.categoria-producto');
    botonesCategoria.forEach(boton => {
        boton.addEventListener('click', () => {
            document.querySelector('.categoria-producto.activo')?.classList.remove('activo');
            boton.classList.add('activo');

            const categoriaSeleccionada = boton.dataset.categoria;

            if (categoriaSeleccionada === "TODOS") {
                productosActuales = [...productosBD];
            } else {
                productosActuales = productosBD.filter(p => p.IdCategoria === parseInt(categoriaSeleccionada));
            }
            renderizarProductos(productosActuales);
        });
    });

    // --- Ordenar ---
    const contenedorOrdenar = document.querySelector('.ordenar-producto');
    const botonOrdenar = document.querySelector('.boton-ordenar');
    const opcionesOrdenar = document.querySelectorAll('.ordenar-opciones button');
    const etiquetaOrden = document.querySelector('.orden-actual');

    if (botonOrdenar && contenedorOrdenar) {
        botonOrdenar.addEventListener('click', e => {
            e.stopPropagation();
            contenedorOrdenar.classList.toggle('abierto');
        });
        document.addEventListener('click', () => {
            contenedorOrdenar.classList.remove('abierto');
        });
    }

    opcionesOrdenar.forEach(opcion => {
        opcion.addEventListener('click', e => {
            const criterio = e.target.dataset.orden;
            if (etiquetaOrden) etiquetaOrden.textContent = e.target.textContent;
            ordenarProductos(criterio);
            contenedorOrdenar.classList.remove('abierto');
        });
    });

    const botonCarrito = document.querySelector('.boton-carrito');
    const botonCerrarCarrito = document.querySelector('.carrito-cerrar');
    const sidebarCarrito = document.querySelector('.carrito-sidebar');
    const overlay = document.querySelector('.overlay');

    function abrirCarrito() {
        sidebarCarrito.classList.add('mostrar');
        overlay.classList.add('mostrar');
    }

    function cerrarCarrito() {
        sidebarCarrito.classList.remove('mostrar');
        overlay.classList.remove('mostrar');
    }

    botonCarrito?.addEventListener('click', abrirCarrito);
    botonCerrarCarrito?.addEventListener('click', cerrarCarrito);
    overlay?.addEventListener('click', cerrarCarrito);

    if (productosGrid) {
        productosGrid.addEventListener('click', e => {
            if (e.target.classList.contains('boton-agregar')) {
                const tarjeta = e.target.closest('.producto-card');
                const idProducto = parseInt(tarjeta.getAttribute('data-id'));
                agregarProductoAlCarrito(idProducto);
            }
        });
    }

    const cuerpoCarrito = document.querySelector('.carrito-cuerpo');
    if (cuerpoCarrito) {
        cuerpoCarrito.addEventListener('click', e => {
            const btnRestar = e.target.closest('.btn-restar');
            const btnSumar = e.target.closest('.btn-sumar');
            const btnEliminar = e.target.closest('.btn-eliminar');
            const btnPagar = e.target.closest('.btn-pagar');

            if (btnRestar) modificarCantidadCarrito(parseInt(btnRestar.dataset.id), -1);
            if (btnSumar) modificarCantidadCarrito(parseInt(btnSumar.dataset.id), 1);
            if (btnEliminar) eliminarDelCarrito(parseInt(btnEliminar.dataset.id));
            if (btnPagar) procesarPago();
        });
    }
});

function obtenerPrecioFinal(producto) {
    const tieneDescuento = producto.PorcentajeDescuento && producto.PorcentajeDescuento > 0;
    return tieneDescuento
        ? producto.PrecioVenta - (producto.PrecioVenta * (producto.PorcentajeDescuento / 100))
        : producto.PrecioVenta;
}

function ordenarProductos(criterio) {
    if (criterio === 'precio-asc') {
        productosActuales.sort((a, b) => obtenerPrecioFinal(a) - obtenerPrecioFinal(b));
    } else if (criterio === 'precio-desc') {
        productosActuales.sort((a, b) => obtenerPrecioFinal(b) - obtenerPrecioFinal(a));
    } else if (criterio === 'nombre-az') {
        productosActuales.sort((a, b) => a.NombreProducto.localeCompare(b.NombreProducto));
    }
    renderizarProductos(productosActuales);
}

function renderizarProductos(productosARenderizar) {
    if (!productosGrid) return;

    const contador = document.querySelector('.cantidad-producto');
    if (contador) contador.textContent = productosARenderizar.length;

    productosGrid.innerHTML = '';

    productosARenderizar.forEach(producto => {
        const tieneDescuento = producto.PorcentajeDescuento && producto.PorcentajeDescuento > 0;
        const precioFinal = obtenerPrecioFinal(producto);

        const etiquetaHTML = tieneDescuento
            ? `<div class="etiqueta-contenedor"><span>Oferta </span><span>-${producto.PorcentajeDescuento}%</span></div>`
            : '';

        const preciosHTML = tieneDescuento
            ? `<span class="precio-actual">S/${precioFinal.toFixed(2)}</span>
               <span class="precio-antiguo">S/${producto.PrecioVenta.toFixed(2)}</span>`
            : `<span class="precio-actual">S/${producto.PrecioVenta.toFixed(2)}</span>`;

        const tarjetaHTML = `
            <div class="producto-card" data-id="${producto.IdProducto}">
                <div class="producto-visual">
                    <img src="${producto.imagen}" alt="${producto.NombreProducto}" class="imagen-producto">
                    ${etiquetaHTML}
                </div>
                <div class="producto-detalles">
                    <span class="producto-marca">${producto.Marca}</span>
                    <h3 class="producto-nombre">${producto.NombreProducto}</h3>
                </div>
                <div class="producto-compra">
                    <div class="precios">
                        ${preciosHTML}
                    </div>
                    <button type="button" class="boton-agregar"> + Agregar </button>
                </div>
            </div>
        `;
        productosGrid.innerHTML += tarjetaHTML;
    });
}

function agregarProductoAlCarrito(idProducto) {
    const producto = productosBD.find(p => p.IdProducto === idProducto);

    if (producto) {
        const precioAUsar = obtenerPrecioFinal(producto);
        const existeEnCarrito = carrito.find(p => p.IdProducto === idProducto);

        if (existeEnCarrito) {
            existeEnCarrito.cantidad++;
        } else {
            carrito.push({
                IdProducto: producto.IdProducto,
                Nombre: producto.NombreProducto,
                Precio: precioAUsar,
                cantidad: 1,
                imagen: producto.imagen
            });
        }
        actualizarCarritoUI();
    }
}

function modificarCantidadCarrito(idProducto, cambio) {
    const itemIndex = carrito.findIndex(p => p.IdProducto === idProducto);
    if (itemIndex > -1) {
        carrito[itemIndex].cantidad += cambio;
        if (carrito[itemIndex].cantidad <= 0) {
            carrito.splice(itemIndex, 1);
        }
        actualizarCarritoUI();
    }
}

function eliminarDelCarrito(idProducto) {
    carrito = carrito.filter(p => p.IdProducto !== idProducto);
    actualizarCarritoUI();
}

function procesarPago() {
    if (carrito.length > 0) {
        alert('¡Pago procesado con éxito! Gracias por usar MarketControl.');
        carrito = [];
        actualizarCarritoUI();
        document.querySelector('.carrito-sidebar').classList.remove('mostrar');
        document.querySelector('.overlay').classList.remove('mostrar');
    }
}

function actualizarCarritoUI() {
    const carritoCuerpo = document.querySelector('.carrito-cuerpo');
    const badgeCarrito = document.querySelector('.carrito-badge');

    if (!carritoCuerpo) return;

    const cantidadTotal = carrito.reduce((suma, item) => suma + item.cantidad, 0);
    if (badgeCarrito) {
        badgeCarrito.textContent = cantidadTotal;
        badgeCarrito.hidden = cantidadTotal === 0;
    }

    if (carrito.length === 0) {
        carritoCuerpo.classList.add('vacio');
        carritoCuerpo.innerHTML = `
            <div class="carrito-img"></div>
            <h4>Carrito vacío</h4>
            <p>Agrega productos para comenzar</p>
        `;
        return;
    }

    carritoCuerpo.classList.remove('vacio');
    let totalPagar = 0;
    let htmlProductos = '';

    carrito.forEach(item => {
        const subtotal = item.Precio * item.cantidad;
        totalPagar += subtotal;

        htmlProductos += `
            <div class="carrito-item">
                <img src="${item.imagen}" alt="${item.Nombre}" class="carrito-item-img">
                <div class="carrito-item-info">
                    <h5>${item.Nombre} 
                        <button class="btn-eliminar" data-id="${item.IdProducto}"><svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <polyline points="3 6 5 6 21 6"></polyline>
        <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
    </svg></button>
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
            </div>
        `;
    });

    carritoCuerpo.innerHTML = `
        ${htmlProductos}
        <div class="carrito-resumen">
            <div class="carrito-total">
                <span>Total:</span>
                <span class="carrito-total-monto">S/${totalPagar.toFixed(2)}</span>
            </div>
            <button class="boton-accion btn-pagar">Procesar Pago</button>
        </div>
    `;
}