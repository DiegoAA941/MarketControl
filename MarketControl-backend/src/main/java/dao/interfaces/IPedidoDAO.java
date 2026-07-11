package dao.interfaces;

import java.util.List;
import modelo.DetallePedido;
import modelo.Pedido;

/**
 * No extiende {@link GenericDAO}: los cambios pasan por {@link #actualizarEstado}
 * o {@link #asignarRepartidor}; no hay "eliminar" fisico, cancelar es cambio de estado.
 */
public interface IPedidoDAO {

    /** Reserva stock si corresponde segun el tipo de entrega. */
    boolean crear(Pedido pedido) throws Exception;

    /** Incluye sus detalles. */
    Pedido buscarPorId(int id) throws Exception;

    List<Pedido> listarTodos() throws Exception;

    boolean actualizarEstado(int idPedido, String estado) throws Exception;

    boolean asignarRepartidor(int idPedido, int idRepartidor) throws Exception;

    List<DetallePedido> listarDetalles(int idPedido) throws Exception;

    /** Al cancelar un pedido. */
    boolean restaurarStock(int idPedido) throws Exception;

    /** 'Delivery' o 'Recojo'. */
    List<Pedido> listarPorTipo(String tipoEntrega) throws Exception;

    /** Filtra por telefono si se indica. */
    List<Pedido> listarRecojoPendientes(String telefono) throws Exception;
}
