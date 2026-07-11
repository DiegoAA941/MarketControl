package dao;

import dao.impl.CategoriaDAOImpl;
import dao.impl.ClienteDAOImpl;
import dao.impl.ConfiguracionDAOImpl;
import dao.impl.PedidoDAOImpl;
import dao.impl.ProductoDAOImpl;
import dao.impl.RepartidorDAOImpl;
import dao.impl.UsuarioDAOImpl;
import dao.impl.VentaDAOImpl;
import dao.interfaces.ICategoriaDAO;
import dao.interfaces.IClienteDAO;
import dao.interfaces.IConfiguracionDAO;
import dao.interfaces.IPedidoDAO;
import dao.interfaces.IProductoDAO;
import dao.interfaces.IRepartidorDAO;
import dao.interfaces.IUsuarioDAO;
import dao.interfaces.IVentaDAO;

/**
 * Punto unico de creacion de los DAO concretos.
 * Los Servlets solo conocen las interfaces; si cambia el motor de BD, solo se toca este archivo.
 */
public final class DAOFactory {

    private DAOFactory() {
    }

    public static IUsuarioDAO usuarioDAO() {
        return new UsuarioDAOImpl();
    }

    public static IProductoDAO productoDAO() {
        return new ProductoDAOImpl();
    }

    public static ICategoriaDAO categoriaDAO() {
        return new CategoriaDAOImpl();
    }

    public static IVentaDAO ventaDAO() {
        return new VentaDAOImpl();
    }

    public static IPedidoDAO pedidoDAO() {
        return new PedidoDAOImpl();
    }

    public static IClienteDAO clienteDAO() {
        return new ClienteDAOImpl();
    }

    public static IRepartidorDAO repartidorDAO() {
        return new RepartidorDAOImpl();
    }

    public static IConfiguracionDAO configuracionDAO() {
        return new ConfiguracionDAOImpl();
    }
}
