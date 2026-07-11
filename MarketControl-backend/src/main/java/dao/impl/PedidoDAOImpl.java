package dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import config.ConexionBD;
import dao.interfaces.IPedidoDAO;
import modelo.DetallePedido;
import modelo.Pedido;
import modelo.Repartidor;
import modelo.Cliente;
import util.CryptoUtil;

/**
 * Implementacion JDBC de {@link IPedidoDAO} para [dbo].[Pedidos] y [dbo].[DetallePedidos].
 */
public class PedidoDAOImpl implements IPedidoDAO {

    private static final Logger logger = LoggerFactory.getLogger(PedidoDAOImpl.class);

    @Override
    public boolean crear(Pedido pedido) throws Exception {
        Preconditions.checkNotNull(pedido, "El pedido no puede ser null.");
        Preconditions.checkArgument(pedido.getDetalles() != null && !pedido.getDetalles().isEmpty(),
                "El pedido debe tener al menos un detalle.");

        String sqlPedido = "INSERT INTO Pedidos (IdCliente, IdRepartidor, Estado, Total, "
                + "Observaciones, TipoEntrega, CostoEnvio) VALUES (?, ?, ?, ?, ?, ?, ?)";
        String sqlDetalle = "INSERT INTO DetallePedidos (IdPedido, IdProducto, Cantidad, "
                + "PrecioUnitario, Subtotal) VALUES (?, ?, ?, ?, ?)";

        Connection con = null;
        try {
            con = ConexionBD.getInstancia().getConexion();
            con.setAutoCommit(false);

            try (PreparedStatement ps = con.prepareStatement(sqlPedido, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, pedido.getIdCliente());
                if (pedido.getIdRepartidor() != null) {
                    ps.setInt(2, pedido.getIdRepartidor());
                } else {
                    ps.setNull(2, Types.INTEGER);
                }
                ps.setString(3, pedido.getEstado());
                ps.setBigDecimal(4, pedido.getTotal());
                ps.setString(5, pedido.getObservaciones());
                ps.setString(6, (pedido.getTipoEntrega() != null && !pedido.getTipoEntrega().isBlank())
                        ? pedido.getTipoEntrega() : "Delivery");
                ps.setBigDecimal(7, pedido.getCostoEnvio() != null ? pedido.getCostoEnvio() : java.math.BigDecimal.ZERO);
                ps.executeUpdate();
                try (ResultSet generadas = ps.getGeneratedKeys()) {
                    if (generadas.next()) {
                        pedido.setIdPedido(generadas.getInt(1));
                    }
                }
            }

            try (PreparedStatement psDet = con.prepareStatement(sqlDetalle)) {
                for (DetallePedido d : pedido.getDetalles()) {
                    psDet.setInt(1, pedido.getIdPedido());
                    psDet.setInt(2, d.getIdProducto());
                    psDet.setInt(3, d.getCantidad());
                    psDet.setBigDecimal(4, d.getPrecioUnitario());
                    psDet.setBigDecimal(5, d.getSubtotal());
                    psDet.addBatch();
                }
                psDet.executeBatch();
            }

            // Reserva de stock SOLO para Delivery. En Recojo el stock baja al cobrar en el POS.
            if (!"Recojo".equalsIgnoreCase(pedido.getTipoEntrega())) {
                reservarStock(con, pedido);
            }

            con.commit();
            logger.info("Pedido id={} registrado con {} detalle(s). Stock reservado.",
                    pedido.getIdPedido(), pedido.getDetalles().size());
            return true;

        } catch (Exception e) {
            logger.error("Error al registrar el pedido. Se ejecuta rollback.", e);
            rollbackSilencioso(con);
            throw e;
        } finally {
            cerrar(con);
        }
    }

    /** Aparta (descuenta) el stock de cada producto del pedido y lo registra como 'Reserva'. */
    private void reservarStock(Connection con, Pedido pedido) throws SQLException {
        String sqlLeer = "SELECT StockActual, NombreProducto FROM Productos WHERE IdProducto = ?";
        String sqlUpd = "UPDATE Productos SET StockActual = StockActual - ? WHERE IdProducto = ?";
        String sqlMov = "INSERT INTO MovimientosInventario "
                + "(IdProducto, TipoMovimiento, Cantidad, StockResultante, Motivo, IdUsuario) "
                + "VALUES (?, 'Reserva', ?, ?, ?, NULL)";
        try (PreparedStatement psLeer = con.prepareStatement(sqlLeer);
             PreparedStatement psUpd = con.prepareStatement(sqlUpd);
             PreparedStatement psMov = con.prepareStatement(sqlMov)) {
            for (DetallePedido d : pedido.getDetalles()) {
                int stockActual;
                String nombre;
                psLeer.setInt(1, d.getIdProducto());
                try (ResultSet rs = psLeer.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalArgumentException("El producto id=" + d.getIdProducto() + " no existe.");
                    }
                    stockActual = rs.getInt("StockActual");
                    nombre = rs.getString("NombreProducto");
                }
                if (stockActual < d.getCantidad()) {
                    throw new IllegalArgumentException("Stock insuficiente para '" + nombre
                            + "': disponible " + stockActual + ", requerido " + d.getCantidad() + ".");
                }
                int resultante = stockActual - d.getCantidad();
                psUpd.setInt(1, d.getCantidad());
                psUpd.setInt(2, d.getIdProducto());
                psUpd.executeUpdate();
                psMov.setInt(1, d.getIdProducto());
                psMov.setInt(2, d.getCantidad());
                psMov.setInt(3, resultante);
                psMov.setString(4, "Reserva pedido #" + pedido.getIdPedido());
                psMov.executeUpdate();
            }
        }
    }

    /** Devuelve el stock reservado de un pedido cancelado; registra el movimiento como 'Devolucion'. */
    @Override
    public boolean restaurarStock(int idPedido) throws Exception {
        Preconditions.checkArgument(idPedido > 0, "El id de pedido debe ser positivo.");
        List<DetallePedido> detalles = listarDetalles(idPedido);
        if (detalles.isEmpty()) {
            return false;
        }
        String sqlLeer = "SELECT StockActual FROM Productos WHERE IdProducto = ?";
        String sqlUpd = "UPDATE Productos SET StockActual = StockActual + ? WHERE IdProducto = ?";
        String sqlMov = "INSERT INTO MovimientosInventario "
                + "(IdProducto, TipoMovimiento, Cantidad, StockResultante, Motivo, IdUsuario) "
                + "VALUES (?, 'Devolucion', ?, ?, ?, NULL)";
        Connection con = null;
        try {
            con = ConexionBD.getInstancia().getConexion();
            con.setAutoCommit(false);
            try (PreparedStatement psLeer = con.prepareStatement(sqlLeer);
                 PreparedStatement psUpd = con.prepareStatement(sqlUpd);
                 PreparedStatement psMov = con.prepareStatement(sqlMov)) {
                for (DetallePedido d : detalles) {
                    int stockActual;
                    psLeer.setInt(1, d.getIdProducto());
                    try (ResultSet rs = psLeer.executeQuery()) {
                        if (!rs.next()) { continue; }
                        stockActual = rs.getInt("StockActual");
                    }
                    int resultante = stockActual + d.getCantidad();
                    psUpd.setInt(1, d.getCantidad());
                    psUpd.setInt(2, d.getIdProducto());
                    psUpd.executeUpdate();
                    psMov.setInt(1, d.getIdProducto());
                    psMov.setInt(2, d.getCantidad());
                    psMov.setInt(3, resultante);
                    psMov.setString(4, "Cancelacion pedido #" + idPedido);
                    psMov.executeUpdate();
                }
            }
            con.commit();
            logger.info("Stock del pedido id={} devuelto al inventario.", idPedido);
            return true;
        } catch (Exception e) {
            rollbackSilencioso(con);
            logger.error("Error al restaurar el stock del pedido id={}.", idPedido, e);
            throw e;
        } finally {
            cerrar(con);
        }
    }

    /** SELECT base con JOIN al repartidor y al cliente (composicion). */
    private static final String SELECT_CON_REPARTIDOR =
            "SELECT p.*, r.IdRepartidor AS RepId, r.Nombres AS RepNombres, "
            + "r.Telefono AS RepTelefono, r.Estado AS RepEstado, "
            + "c.Nombres AS CliNombres, c.Telefono AS CliTelefono, "
            + "c.Direccion AS CliDireccion, c.Referencia AS CliReferencia "
            + "FROM Pedidos p "
            + "LEFT JOIN Repartidores r ON p.IdRepartidor = r.IdRepartidor "
            + "LEFT JOIN Clientes c ON p.IdCliente = c.IdCliente";

    @Override
    public Pedido buscarPorId(int id) throws Exception {
        Preconditions.checkArgument(id > 0, "El id de pedido debe ser positivo.");
        String sql = SELECT_CON_REPARTIDOR + " WHERE p.IdPedido = ?";
        try (Connection con = ConexionBD.getInstancia().getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Pedido p = mapear(rs);
                    hidratarRepartidor(rs, p);
                    hidratarCliente(rs, p);
                    p.setDetalles(listarDetalles(id));
                    return p;
                }
                return null;
            }
        } catch (SQLException e) {
            logger.error("Error al buscar el pedido id={}.", id, e);
            throw e;
        }
    }

    @Override
    public boolean actualizarEstado(int idPedido, String estado) throws Exception {
        Preconditions.checkArgument(idPedido > 0, "El id de pedido debe ser positivo.");
        Preconditions.checkArgument(estado != null && !estado.isBlank(),
                "El estado no puede estar vacio.");
        String sql = "UPDATE Pedidos SET Estado = ? WHERE IdPedido = ?";
        try (Connection con = ConexionBD.getInstancia().getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, estado);
            ps.setInt(2, idPedido);
            int filas = ps.executeUpdate();
            logger.info("Pedido id={} cambio a estado '{}'.", idPedido, estado);
            return filas > 0;
        } catch (SQLException e) {
            logger.error("Error al actualizar el estado del pedido id={}.", idPedido, e);
            throw e;
        }
    }

    @Override
    public boolean asignarRepartidor(int idPedido, int idRepartidor) throws Exception {
        Preconditions.checkArgument(idPedido > 0, "El id de pedido debe ser positivo.");
        Preconditions.checkArgument(idRepartidor > 0, "El id de repartidor debe ser positivo.");
        String sql = "UPDATE Pedidos SET IdRepartidor = ? WHERE IdPedido = ?";
        try (Connection con = ConexionBD.getInstancia().getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idRepartidor);
            ps.setInt(2, idPedido);
            int filas = ps.executeUpdate();
            logger.info("Pedido id={} asignado al repartidor id={}.", idPedido, idRepartidor);
            return filas > 0;
        } catch (SQLException e) {
            logger.error("Error al asignar repartidor al pedido id={}.", idPedido, e);
            throw e;
        }
    }

    @Override
    public List<DetallePedido> listarDetalles(int idPedido) throws Exception {
        Preconditions.checkArgument(idPedido > 0, "El id de pedido debe ser positivo.");
        String sql = "SELECT * FROM DetallePedidos WHERE IdPedido = ?";
        List<DetallePedido> lista = Lists.newArrayList();
        try (Connection con = ConexionBD.getInstancia().getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idPedido);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DetallePedido d = new DetallePedido();
                    d.setIdDetallePedido(rs.getInt("IdDetallePedido"));
                    d.setIdPedido(rs.getInt("IdPedido"));
                    d.setIdProducto(rs.getInt("IdProducto"));
                    d.setCantidad(rs.getInt("Cantidad"));
                    d.setPrecioUnitario(rs.getBigDecimal("PrecioUnitario"));
                    d.setSubtotal(rs.getBigDecimal("Subtotal"));
                    lista.add(d);
                }
            }
            return lista;
        } catch (SQLException e) {
            logger.error("Error al listar detalles del pedido id={}.", idPedido, e);
            throw e;
        }
    }

    @Override
    public List<Pedido> listarPorTipo(String tipoEntrega) throws Exception {
        Preconditions.checkArgument(tipoEntrega != null && !tipoEntrega.isBlank(),
                "El tipo de entrega no puede estar vacio.");
        String sql = SELECT_CON_REPARTIDOR + " WHERE p.TipoEntrega = ? ORDER BY p.FechaPedido DESC";
        List<Pedido> lista = Lists.newArrayList();
        try (Connection con = ConexionBD.getInstancia().getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, tipoEntrega);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Pedido p = mapear(rs);
                    hidratarRepartidor(rs, p);
                    hidratarCliente(rs, p);
                    lista.add(p);
                }
            }
            return lista;
        } catch (SQLException e) {
            logger.error("Error al listar pedidos por tipo '{}'.", tipoEntrega, e);
            throw e;
        }
    }

    @Override
    public List<Pedido> listarRecojoPendientes(String telefono) throws Exception {
        boolean conTel = telefono != null && !telefono.isBlank();
        String sql = SELECT_CON_REPARTIDOR
                + " WHERE p.TipoEntrega = 'Recojo' AND p.Estado = 'Pendiente'"
                + (conTel ? " AND c.TelefonoHash = ?" : "")
                + " ORDER BY p.FechaPedido DESC";
        List<Pedido> lista = Lists.newArrayList();
        try (Connection con = ConexionBD.getInstancia().getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (conTel) {
                ps.setString(1, CryptoUtil.indiceCiego(telefono));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Pedido p = mapear(rs);
                    hidratarRepartidor(rs, p);
                    hidratarCliente(rs, p);
                    lista.add(p);
                }
            }
            return lista;
        } catch (SQLException e) {
            logger.error("Error al listar recojos pendientes.", e);
            throw e;
        }
    }

    @Override
    public List<Pedido> listarTodos() throws Exception {
        String sql = SELECT_CON_REPARTIDOR + " ORDER BY p.FechaPedido DESC";
        List<Pedido> lista = Lists.newArrayList();
        try (Connection con = ConexionBD.getInstancia().getConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Pedido p = mapear(rs);
                hidratarRepartidor(rs, p);
                hidratarCliente(rs, p);
                lista.add(p);
            }
            return lista;
        } catch (SQLException e) {
            logger.error("Error al listar pedidos.", e);
            throw e;
        }
    }

    // ---------------------- Auxiliares ----------------------

    private Pedido mapear(ResultSet rs) throws SQLException {
        Pedido p = new Pedido();
        p.setIdPedido(rs.getInt("IdPedido"));
        p.setIdCliente(rs.getInt("IdCliente"));
        int idRep = rs.getInt("IdRepartidor");
        p.setIdRepartidor(rs.wasNull() ? null : idRep);
        Timestamp ts = rs.getTimestamp("FechaPedido");
        if (ts != null) {
            p.setFechaPedido(ts.toLocalDateTime());
        }
        p.setEstado(rs.getString("Estado"));
        p.setTotal(rs.getBigDecimal("Total"));
        p.setObservaciones(rs.getString("Observaciones"));
        p.setTipoEntrega(rs.getString("TipoEntrega"));
        p.setCostoEnvio(rs.getBigDecimal("CostoEnvio"));
        return p;
    }

    private void hidratarRepartidor(ResultSet rs, Pedido p) throws SQLException {
        int repId = rs.getInt("RepId");
        if (!rs.wasNull()) {
            Repartidor r = new Repartidor();
            r.setIdRepartidor(repId);
            r.setNombres(rs.getString("RepNombres"));
            r.setTelefono(CryptoUtil.descifrar(rs.getString("RepTelefono")));
            r.setEstado(rs.getString("RepEstado"));
            p.setRepartidor(r);
        }
    }

    private void hidratarCliente(ResultSet rs, Pedido p) throws SQLException {
        String nombres = rs.getString("CliNombres");
        if (nombres != null) {
            Cliente c = new Cliente();
            c.setIdCliente(p.getIdCliente());
            c.setNombres(nombres);
            c.setTelefono(CryptoUtil.descifrar(rs.getString("CliTelefono")));
            c.setDireccion(CryptoUtil.descifrar(rs.getString("CliDireccion")));
            c.setReferencia(CryptoUtil.descifrar(rs.getString("CliReferencia")));
            p.setCliente(c);
        }
    }

    private void rollbackSilencioso(Connection con) {
        if (con != null) {
            try {
                con.rollback();
            } catch (SQLException ex) {
                logger.error("Error al ejecutar rollback.", ex);
            }
        }
    }

    private void cerrar(Connection con) {
        if (con != null) {
            try {
                con.setAutoCommit(true);
                con.close();
            } catch (SQLException ex) {
                logger.error("Error al cerrar la conexion.", ex);
            }
        }
    }
}
