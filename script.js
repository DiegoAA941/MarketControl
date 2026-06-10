const botonBuscar = document.querySelector('.boton-buscar');
const formularioBuscar = document.querySelector('.barra-superior form');

botonBuscar.addEventListener('click', () => {
    formularioBuscar.classList.toggle('abierto');
});

const botonCarrito = document.querySelector('.boton-carrito');
const botonCerrarCarrito = document.querySelector('.carrito-cerrar');
const botonVerProductos = document.querySelector('.boton-accion');
const sidebarCarrito = document.querySelector('.carrito-sidebar');
botonCarrito.addEventListener('click', () => {
    sidebarCarrito.classList.add('mostrar');
});
botonCerrarCarrito.addEventListener('click', () => {
    sidebarCarrito.classList.remove('mostrar');
});
botonVerProductos.addEventListener('click', () => {
    sidebarCarrito.classList.remove('mostrar');
});

const botonesCategoria = document.querySelectorAll('.categoria-producto');
botonesCategoria.forEach(boton => {
    boton.addEventListener('click', () => {
        const botonActivoAnterior = document.querySelector('.categoria-producto.activo');
        if (botonActivoAnterior) {
            botonActivoAnterior.classList.remove('activo');
        }
        boton.classList.add('activo');
        const categoriaSeleccionada = boton.textContent.trim();
        console.log("Filtrando productos por:", categoriaSeleccionada);
    });
});