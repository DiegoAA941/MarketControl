/* ---------- 5. CARGA DE CATÁLOGO ---------- */
async function cargarCatalogo() {
    const [productos, categorias] = await Promise.all([
        api('/api/productos'),
        api('/api/categorias'),
    ]);
    productosCache = productos || [];
    categoriasCache = (categorias || []).filter((c) => c.estado !== false);
    categoriaPorId = {};
    categoriasCache.forEach((c) => { categoriaPorId[c.idCategoria] = c.nombreCategoria; });
    nombrePorProducto = {};
    productosCache.forEach((p) => { nombrePorProducto[p.idProducto] = p.nombreProducto; });
    llenarSelectCategorias();
}

function llenarSelectCategorias() {
    const sel = document.getElementById('p-categoria');
    if (!sel) return;
    sel.innerHTML = '<option value="" disabled selected>Selecciona una categoría</option>' +
        categoriasCache.map((c) => `<option value="${c.idCategoria}">${esc(c.nombreCategoria)}</option>`).join('');
}

/** Lista TODAS las categorías (activas e inactivas) dentro de la modal de Categorías. */
async function cargarCategoriasListado() {
    const cont = document.getElementById('cat-listado');
    cont.innerHTML = `<p class="vacio">Cargando…</p>`;
    try {
        categoriasListadoCache = await api('/api/categorias');
        if (!categoriasListadoCache.length) {
            cont.innerHTML = `<div class="vacio"><p>Aún no hay categorías. Crea la primera arriba.</p></div>`;
            return;
        }
        cont.innerHTML = categoriasListadoCache.map((c) => {
            const inactiva = c.estado === false;
            const baja = inactiva ? '' :
                `<button type="button" class="icono-accion icono-accion--eliminar" data-accion="baja-cat" data-id="${c.idCategoria}" title="Desactivar" aria-label="Desactivar ${esc(c.nombreCategoria)}">${svg('basura', 18)}</button>`;
            return `<div class="cat-item ${inactiva ? 'cat-item--inactiva' : ''}">
                <div class="cat-item__datos">
                    <div class="cat-item__nombre">${esc(c.nombreCategoria)}</div>
                    ${c.descripcion ? `<div class="cat-item__desc">${esc(c.descripcion)}</div>` : ''}
                </div>
                ${inactiva ? '<span class="cat-item__estado">Inactiva</span>' : ''}
                <button type="button" class="icono-accion icono-accion--editar" data-accion="editar-cat" data-id="${c.idCategoria}" aria-label="Editar ${esc(c.nombreCategoria)}">${svg('lapiz', 18)}</button>
                ${baja}
            </div>`;
        }).join('');
    } catch (e) { notificarError(e); }
}
