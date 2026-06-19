const productosBD = [
    {
        IdProducto: 1,
        IdCategoria: 1,
        CodigoSKU: "PROD-001",
        NombreProducto: "Leche entera Gloria 390g",
        PrecioVenta: 3.50,     
        PorcentajeDescuento: 20,
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

document.addEventListener('DOMContentLoaded', () => {

    productosGrid = document.querySelector('.productos-grid');
    renderizarProductos(productosBD);

    const botonBuscar = document.querySelector('.boton-buscar');
    const formularioBuscar = document.querySelector('.barra-superior form');
    const inputBuscador = document.querySelector('.buscador-lupa'); 

    if (botonBuscar && formularioBuscar) {
        botonBuscar.addEventListener('click', () => {
            formularioBuscar.classList.toggle('abierto');
        });
    }

    if (formularioBuscar) {
        formularioBuscar.addEventListener('submit', (evento) => {
            evento.preventDefault(); 
        });
    }

    if (inputBuscador) {
        inputBuscador.addEventListener('input', () => {
            const textoBusqueda = inputBuscador.value.toLowerCase().trim();
            const productosEncontrados = productosBD.filter(producto => {
                return producto.NombreProducto.toLowerCase().includes(textoBusqueda);
            });
            renderizarProductos(productosEncontrados);
        });
    }

    const botonCarrito = document.querySelector('.boton-carrito');
    const botonCerrarCarrito = document.querySelector('.carrito-cerrar');
    const botonVerProductos = document.querySelector('.boton-accion');
    const sidebarCarrito = document.querySelector('.carrito-sidebar');

    if (botonCarrito && sidebarCarrito && botonCerrarCarrito) {
        botonCarrito.addEventListener('click', () => {
            sidebarCarrito.classList.add('mostrar');
        });
        botonCerrarCarrito.addEventListener('click', () => {
            sidebarCarrito.classList.remove('mostrar');
        });
        if (botonVerProductos) {
            botonVerProductos.addEventListener('click', () => {
                sidebarCarrito.classList.remove('mostrar');
            });
        }
    }

    const mapCategorias = {
        "Todos": "TODOS",
        "Lácteos": 1,
        "Abarrotes": 2,
        "Panadería": 4,
        "Bebidas": 5,
        "Verduras": 6,
        "Carnes": 7
    };

    const botonesCategoria = document.querySelectorAll('.categoria-producto');
    botonesCategoria.forEach(boton => {
        boton.addEventListener('click', () => {
            const botonActivoAnterior = document.querySelector('.categoria-producto.activo');
            if (botonActivoAnterior) {
                botonActivoAnterior.classList.remove('activo');
            }
            boton.classList.add('activo');

            const categoriaSeleccionada = boton.textContent.trim();
            const idCategoriaSQL = mapCategorias[categoriaSeleccionada];

            if (idCategoriaSQL === "TODOS") {
                renderizarProductos(productosBD);
            } else {
                const productosFiltrados = productosBD.filter(producto => producto.IdCategoria === idCategoriaSQL);
                renderizarProductos(productosFiltrados);
            }
        });
    });

    const contenedorOrdenar = document.querySelector('.ordenar-producto');
    const botonOrdenar = document.querySelector('.boton-ordenar');

    if (botonOrdenar && contenedorOrdenar) {
        botonOrdenar.addEventListener('click', (evento) => {
            evento.stopPropagation();
            contenedorOrdenar.classList.toggle('abierto');
        });
        document.addEventListener('click', () => {
            contenedorOrdenar.classList.remove('abierto');
        });
    }

    if (productosGrid) {
        productosGrid.addEventListener('click', (evento) => {
            if (evento.target.classList.contains('boton-agregar')) {
                const tarjeta = evento.target.closest('.producto-card');
                const idProductoSeleccionado = parseInt(tarjeta.getAttribute('data-id'));
                agregarProductoAlCarrito(idProductoSeleccionado);
            }
        });
    }
});

function renderizarProductos(productosARenderizar) {
    if (!productosGrid) return;

    const contador = document.querySelector('.cantidad-producto');
    if (contador) {
        contador.textContent = productosARenderizar.length;
    }

    productosGrid.innerHTML = '';

    productosARenderizar.forEach(producto => {
        const tieneDescuento = producto.PorcentajeDescuento && producto.PorcentajeDescuento > 0;
        
        const precioFinal = tieneDescuento
            ? producto.PrecioVenta - (producto.PrecioVenta * (producto.PorcentajeDescuento / 100))
            : producto.PrecioVenta;

        const etiquetaHTML = tieneDescuento
            ? `<div class="etiqueta-contenedor"><span>Oferta </span><span>-${producto.PorcentajeDescuento}%</span></div>`
            : '';

        const preciosHTML = tieneDescuento
            ? `<span class="precio-actual">S/${precioFinal.toFixed(2)}</span>
               <span class="precio-antiguo">S/${producto.PrecioVenta.toFixed(2)}</span>`
            : `<span class="precio-actual">S/${producto.PrecioVenta.toFixed(2)}</span>`;

        const tarjetaHTML = `
            <div class="producto-card" data-id="${producto.IdProducto}" data-categoria="${producto.IdCategoria}">
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
        const tieneDescuento = producto.PorcentajeDescuento && producto.PorcentajeDescuento > 0;
        const precioAUsar = tieneDescuento 
            ? producto.PrecioVenta - (producto.PrecioVenta * (producto.PorcentajeDescuento / 100))
            : producto.PrecioVenta;

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
        
        const botonCarritoUI = document.querySelector('.boton-carrito');
        botonCarritoUI.style.backgroundColor = '#ecfdf5';
        setTimeout(() => botonCarritoUI.style.backgroundColor = '#ffffff', 300);
    }
}

function actualizarCarritoUI() {
    const carritoCuerpo = document.querySelector('.carrito-cuerpo');
    if (!carritoCuerpo) return;

    if (carrito.length === 0) {
        carritoCuerpo.innerHTML = `
            <div class="carrito-img"></div>
            <h4>Carrito vacío</h4>
            <p>Agregar productos para comenzar</p>
            <button class="boton-accion">Ver productos</button>
        `;
        const botonVerProductos = document.querySelector('.boton-accion');
        const sidebarCarrito = document.querySelector('.carrito-sidebar');
        if (botonVerProductos && sidebarCarrito) {
            botonVerProductos.addEventListener('click', () => {
                sidebarCarrito.classList.remove('mostrar');
            });
        }
        return;
    }

    let totalPagar = 0;
    let htmlProductos = '<div style="width: 100%; text-align: left; overflow-y: auto; max-height: 70vh;">';

    carrito.forEach(item => {
        const subtotal = item.Precio * item.cantidad;
        totalPagar += subtotal;
        
        htmlProductos += `
            <div style="display: flex; align-items: center; border-bottom: 1px solid #eee; padding: 10px 0; margin-bottom: 10px;">
                <img src="${item.imagen}" alt="${item.Nombre}" style="width: 50px; height: 50px; object-fit: contain; margin-right: 15px; border-radius: 8px; background: #f9fafb;">
                <div style="flex-grow: 1;">
                    <h5 style="font-size: 13px; color: #1f2937; margin-bottom: 5px;">${item.Nombre}</h5>
                    <div style="display: flex; justify-content: space-between; font-size: 13px;">
                        <span style="color: #7d7d7d;">Cant: ${item.cantidad}</span>
                        <span style="font-weight: bold; color: #10b981;">S/${subtotal.toFixed(2)}</span>
                    </div>
                </div>
            </div>
        `;
    });

    htmlProductos += '</div>';

    htmlProductos += `
        <div style="width: 100%; margin-top: 20px; border-top: 2px solid #e0e0e0; padding-top: 15px;">
            <div style="display: flex; justify-content: space-between; font-weight: bold; font-size: 16px; margin-bottom: 15px;">
                <span>Total:</span>
                <span style="color: #10b981;">S/${totalPagar.toFixed(2)}</span>
            </div>
            <button class="boton-accion" style="width: 100%;">Procesar Pago</button>
        </div>
    `;

    carritoCuerpo.innerHTML = htmlProductos;
}