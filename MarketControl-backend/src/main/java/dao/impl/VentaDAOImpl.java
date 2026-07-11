package dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import config.ConexionBD;
import dao.interfaces.IVentaDAO;
import modelo.DetalleVenta;
import modelo.Venta;

/**
 * Implementacion JDBC de {@link IVentaDAO} para [dbo].[Ventas] y [dbo].[DetalleVentas].
 * La cabecera y sus detalles se insertan dentro de una transaccion.
 */
public class VentaDAOImpl implements IVentaDAO {

    private static final Logger logger = LoggerFactory.getLogger(VentaDAOImpl.class);

    @Override
    public boolean crear(Venta venta) throws Exception {
        return crear(venta, true);
    }

    @Override
    public boolean crear(Venta venta, boolean ajustarStock) throws Exception {
        Preconditions.checkNotNull(venta, "La venta no puede ser null.");
        Preconditions.checkArgument(venta.getDetalles() != null && !venta.getDetalles().isEmpty(),
                "La venta debe tener al menos un detalle.");

        String sqlVenta = "INSERT INTO Ventas (IdUsuario, Total, MetodoPago, Estado) "
                + "VALUES (?, ?, ?, ?)";
        String sqlDetalle = "INSERT INTO DetalleVentas (IdVenta, IdProducto, Cantidad, "
                + "PrecioUnitario, Subtotal) VALUES (?, ?, ?, ?, ?)";

        Connection con = null;
        try {
            con = ConexionBD.getInstancia().getConexion();
            con.setAutoCommit(false);

            try (PreparedStatement ps = con.prepareStatement(sqlVenta, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, venta.getIdUsuario());
                ps.setBigDecimal(2, venta.getTotal());
                ps.setString(3, venta.getMetodoPago());
                ps.setString(4, venta.getEstado());
                ps.executeUpdate();
                try (ResultSet generadas = ps.getGeneratedKeys()) {
                    if (generadas.next()) {
                        venta.setIdVenta(generadas.getInt(1));
                    }
                }
            }

            try (PreparedStatement psDet = con.prepareStatement(sqlDetalle)) {
                for (DetalleVenta d : venta.getDetalles()) {
                    psDet.setInt(1, venta.getIdVenta());
                    psDet.setInt(2, d.getIdProducto());
                    psDet.setInt(3, d.getCantidad());
                    psDet.setBigDecimal(4, d.getPrecioUnitario());
                    psDet.setBigDecimal(5, d.getSubtotal());
                    psDet.addBatch();
                }
                psDet.executeBatch();
            }

            // Delivery facturado al entregar: el stock ya bajo en la reserva, no se repite.
            if (ajustarStock) {
                descontarStock(con, venta);
            }

            // Si viene de un pedido de Recojo, se cierra en la misma transaccion.
            if (venta.getIdPedidoOrigen() != null) {
                try (PreparedStatement psPed = con.prepareStatement(
                        "UPDATE Pedidos SET Estado = 'Atendido' WHERE IdPedido = ?")) {
                    psPed.setInt(1, venta.getIdPedidoOrigen());
                    psPed.executeUpdate();
                }
            }

            con.commit();
            logger.info("Venta id={} registrada con {} detalle(s). ajustarStock={}",
                    venta.getIdVenta(), venta.getDetalles().size(), ajustarStock);
            return true;

        } catch (Exception e) {
            logger.error("Error al registrar la venta. Se ejecuta rollback.", e);
            rollbackSilencioso(con);
            throw e;
        } finally {
            cerrar(con);
        }
    }

    /** Usa la conexion de la transaccion en curso: si algo falla, se revierte junto con la venta. */
    private void descontarStock(Connection con, Venta venta) throws SQLException {
        String sqlLeer = "SELECT StockActual, NombreProducto FROM Productos WHERE IdProducto = ?";
        String sqlUpd = "UPDATE Productos SET StockActual = StockActual - ? WHERE IdProducto = ?";
        String sqlMov = "INSERT INTO MovimientosInventario "
                + "(IdProducto, TipoMovimiento, Cantidad, StockResultante, Motivo, IdUsuario) "
                + "VALUES (?, 'Salida', ?, ?, ?, ?)";
        try (PreparedStatement psLeer = con.prepareStatement(sqlLeer);
             PreparedStatement psUpd = con.prepareStatement(sqlUpd);
             PreparedStatement psMov = con.prepareStatement(sqlMov)) {
            for (DetalleVenta d : venta.getDetalles()) {
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
                psMov.setString(4, "Venta #" + venta.getIdVenta());
                if (venta.getIdUsuario() > 0) {
                    psMov.setInt(5, venta.getIdUsuario());
                } else {
                    psMov.setNull(5, java.sql.Types.INTEGER);
                }
                psMov.executeUpdate();
            }
        }
    }

    @Override
    public Venta buscarPorId(int id) throws Exception {
        Preconditions.checkArgument(id > 0, "El id de venta debe ser positivo.");
        String sql = "SELECT * FROM Ventas WHERE IdVenta = ?";
        try (Connection con = ConexionBD.getInstancia().getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Venta v = mapear(rs);
                    v.setDetalles(listarDetalles(id));
                    return v;
                }
                return null;
            }
        } catch (SQLException e) {
            logger.error("Error al buscar la venta id={}.", id, e);
            throw e;
        }
    }

    @Override
    public List<Venta> ventasPorRango(LocalDateTime inicio, LocalDateTime fin) throws Exception {
        Preconditions.checkNotNull(inicio, "La fecha inicial no puede ser null.");
        Preconditions.checkNotNull(fin, "La fecha final no puede ser null.");
        String sql = "SELECT * FROM Ventas WHERE FechaVenta BETWEEN ? AND ?";
        List<Venta> lista = Lists.newArrayList();
        try (Connection con = ConexionBD.getInstancia().getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(inicio));
            ps.setTimestamp(2, Timestamp.valueOf(fin));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
            return lista;
        } catch (SQLException e) {
            logger.error("Error al consultar ventas por rango.", e);
            throw e;
        }
    }

    @Override
    public List<DetalleVenta> listarDetalles(int idVenta) throws Exception {
        Preconditions.checkArgument(idVenta > 0, "El id de venta debe ser positivo.");
        String sql = "SELECT * FROM DetalleVentas WHERE IdVenta = ?";
        List<DetalleVenta> lista = Lists.newArrayList();
        try (Connection con = ConexionBD.getInstancia().getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idVenta);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DetalleVenta d = new DetalleVenta();
                    d.setIdDetalleVenta(rs.getInt("IdDetalleVenta"));
                    d.setIdVenta(rs.getInt("IdVenta"));
                    d.setIdProducto(rs.getInt("IdProducto"));
                    d.setCantidad(rs.getInt("Cantidad"));
                    d.setPrecioUnitario(rs.getBigDecimal("PrecioUnitario"));
                    d.setSubtotal(rs.getBigDecimal("Subtotal"));
                    lista.add(d);
                }
            }
            return lista;
        } catch (SQLException e) {
            logger.error("Error al listar detalles de la venta id={}.", idVenta, e);
            throw e;
        }
    }

    @Override
    public List<Venta> listarTodos() throws Exception {
        String sql = "SELECT * FROM Ventas";
        List<Venta> lista = Lists.newArrayList();
        try (Connection con = ConexionBD.getInstancia().getConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
            return lista;
        } catch (SQLException e) {
            logger.error("Error al listar ventas.", e);
            throw e;
        }
    }

    // ---------------------- Auxiliares ----------------------

    private Venta mapear(ResultSet rs) throws SQLException {
        Venta v = new Venta();
        v.setIdVenta(rs.getInt("IdVenta"));
        v.setIdUsuario(rs.getInt("IdUsuario"));
        Timestamp ts = rs.getTimestamp("FechaVenta");
        if (ts != null) {
            v.setFechaVenta(ts.toLocalDateTime());
        }
        v.setTotal(rs.getBigDecimal("Total"));
        v.setMetodoPago(rs.getString("MetodoPago"));
        v.setEstado(rs.getString("Estado"));
        return v;
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
