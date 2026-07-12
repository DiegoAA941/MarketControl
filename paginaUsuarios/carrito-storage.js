/* Carrito compartido entre pestañas (tienda.html y producto.html) vía localStorage. */
const CARRITO_STORAGE_KEY = 'mc_carrito';

function cargarCarrito() {
    try {
        const datos = JSON.parse(localStorage.getItem(CARRITO_STORAGE_KEY));
        return Array.isArray(datos) ? datos : [];
    } catch (_) {
        return [];
    }
}

function guardarCarrito(carrito) {
    localStorage.setItem(CARRITO_STORAGE_KEY, JSON.stringify(carrito));
}
