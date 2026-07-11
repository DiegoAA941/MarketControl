package dao.interfaces;

import java.time.LocalDateTime;
import java.util.List;
import modelo.DetalleVenta;
import modelo.Venta;

/**
 * No extiende {@link GenericDAO}: una venta registrada no se edita ni se borra
 * (romperia el historial contable), por eso no hay "actualizar"/"eliminar".
 */
public interface IVentaDAO {

    /** Descuenta inventario. */
    boolean crear(Venta venta) throws Exception;

    /** {@code ajustarStock=false} cuando el stock ya bajo antes (reserva de un Delivery). */
    boolean crear(Venta venta, boolean ajustarStock) throws Exception;

    Venta buscarPorId(int id) throws Exception;

    List<Venta> listarTodos() throws Exception;

    List<Venta> ventasPorRango(LocalDateTime inicio, LocalDateTime fin) throws Exception;

    List<DetalleVenta> listarDetalles(int idVenta) throws Exception;
}
