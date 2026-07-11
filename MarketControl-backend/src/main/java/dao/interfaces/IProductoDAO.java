package dao.interfaces;

import java.util.List;
import modelo.Producto;

/**
 * DAO especifico de Producto. Hereda el CRUD de {@link GenericDAO}
 * y agrega las consultas propias del modulo de inventario / tienda.
 */
public interface IProductoDAO extends GenericDAO<Producto> {

    /** Busqueda del catalogo web. */
    List<Producto> buscarPorNombre(String nombre) throws Exception;

    List<Producto> listarPorCategoria(int idCategoria) throws Exception;

    /** Stock actual en o por debajo del minimo. */
    List<Producto> listarStockBajo() throws Exception;

    /** Registra el movimiento de entrada en MovimientosInventario y devuelve el stock resultante. */
    int agregarStock(int idProducto, int cantidad, Integer idUsuario) throws Exception;
}
