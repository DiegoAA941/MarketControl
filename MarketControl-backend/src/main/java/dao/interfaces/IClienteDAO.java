package dao.interfaces;

import modelo.Cliente;

/**
 * No extiende {@link GenericDAO}: los clientes solo se ven dentro de un pedido,
 * no hay pantalla de listado/baja propia. [dbo].[Clientes] no tiene columna DNI,
 * por eso se busca por telefono en vez de por DNI.
 */
public interface IClienteDAO {

    boolean crear(Cliente cliente) throws Exception;

    boolean actualizar(Cliente cliente) throws Exception;

    Cliente buscarPorTelefono(String telefono) throws Exception;
}
