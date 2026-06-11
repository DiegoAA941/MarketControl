const productosBD = [
    {
        IdProducto: 1,
        IdCategoria: 1,
        CodigoSKU: "PROD-001",
        NombreProducto: "Leche entera Gloria 1L",
        PrecioCompra: 5.20,     
        PrecioVenta: 4.50,      
        StockActual: 45,
        StockMinimo: 10,
        Marca: "Gloria",        
        imagen: "https://elregionalpiura.com.pe/media/jact/medium/images/Fotografias/2022/Junio_2022/Leche-Gloria-01.jpg" 
    },
    {
        IdProducto: 2,
        IdCategoria: 4,
        CodigoSKU: "PROD-002",
        NombreProducto: "Pan de Molde Blanco Familiar",
        PrecioCompra: 6.00,
        PrecioVenta: 7.20,      
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

    if (botonCarrito && sidebarCarrito && botonCerrarCarrito && botonVerProductos) {
        botonCarrito.addEventListener('click', () => {
            sidebarCarrito.classList.add('mostrar');
        });
        botonCerrarCarrito.addEventListener('click', () => {
            sidebarCarrito.classList.remove('mostrar');
        });
        botonVerProductos.addEventListener('click', () => {
            sidebarCarrito.classList.remove('mostrar');
        });
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
        const tieneDescuento = producto.PrecioVenta < producto.PrecioCompra;
        
        const porcentaje = tieneDescuento
            ? Math.round(((producto.PrecioCompra - producto.PrecioVenta) / producto.PrecioCompra) * 100)
            : 0;

        const etiquetaHTML = tieneDescuento
            ? `<div class="etiqueta-contenedor"><span>Oferta </span><span>-${porcentaje}%</span></div>`
            : '';

        const preciosHTML = tieneDescuento
            ? `<span class="precio-actual">S/${producto.PrecioVenta.toFixed(2)}</span>
               <span class="precio-antiguo">S/${producto.PrecioCompra.toFixed(2)}</span>`
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

