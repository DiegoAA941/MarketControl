package dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import config.ConexionBD;
import dao.interfaces.IProductoDAO;
import modelo.Producto;

/**
 * Implementacion JDBC de {@link IProductoDAO} para la tabla [dbo].[Productos].
 */
public class ProductoDAOImpl implements IProductoDAO {

    private static final Logger logger = LoggerFactory.getLogger(ProductoDAOImpl.class);

    @Override
    public boolean crear(Producto p) throws Exception {
        Preconditions.checkNotNull(p, "El producto no puede ser null.");
        String sql = "INSERT INTO Productos (IdCategoria, CodigoSKU, NombreProducto, Descripcion, "
                + "Marca, ImagenUrl, PrecioCompra, PrecioVenta, PorcentajeDescuento, StockActual, "
                + "StockMinimo, Estado) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = ConexionBD.getInstancia().getConexion();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, p.getIdCategoria());
            ps.setString(2, p.getCodigoSku());
            ps.setString(3, p.getNombreProducto());
            ps.setString(4, p.getDescripcion());
            ps.setString(5, p.getMarca());
            ps.setString(6, p.getImagenUrl());
            ps.setBigDecimal(7, p.getPrecioCompra());
            ps.setBigDecimal(8, p.getPrecioVenta());
            ps.setBigDecimal(9, p.getPorcentajeDescuento());
            ps.setInt(10, p.getStockActual());
            ps.setInt(11, p.getStockMinimo());
            ps.setBoolean(12, p.isEstado());
            int filas = ps.executeUpdate();
            try (ResultSet generadas = ps.getGeneratedKeys()) {
                if (generadas.next()) {
                    p.setIdProducto(generadas.getInt(1));
                }
            }
            logger.info("Producto '{}' creado (id={}).", p.getCodigoSku(), p.getIdProducto());
            return filas > 0;
        } catch (SQLException e) {
            logger.error("Error al crear el producto '{}'.", p.getCodigoSku(), e);
            throw e;
        }
    }

    @Override
    public Producto buscarPorId(int id) throws Exception {
        Preconditions.checkArgument(id > 0, "El id de producto debe ser positivo.");
        String sql = "SELECT * FROM Productos WHERE IdProducto = ?";
        try (Connection con = ConexionBD.getInstancia().getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        } catch (SQLException e) {
            logger.error("Error al buscar el producto id={}.", id, e);
            throw e;
        }
    }

    @Override
    public List<Producto> buscarPorNombre(String nombre) throws Exception {
        Preconditions.checkArgument(nombre != null && !nombre.isBlank(),
                "El nombre de busqueda no puede estar vacio.");
        String sql = "SELECT * FROM Productos WHERE NombreProducto LIKE ?";
        List<Producto> lista = Lists.newArrayList();
        try (Connection con = ConexionBD.getInstancia().getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, "%" + nombre + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
            return lista;
        } catch (SQLException e) {
            logger.error("Error al buscar productos por nombre '{}'.", nombre, e);
            throw e;
        }
    }

    @Override
    public List<Producto> listarPorCategoria(int idCategoria) throws Exception {
        Preconditions.checkArgument(idCategoria > 0, "El id de categoria debe ser positivo.");
        String sql = "SELECT * FROM Productos WHERE IdCategoria = ?";
        List<Producto> lista = Lists.newArrayList();
        try (Connection con = ConexionBD.getInstancia().getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idCategoria);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
            return lista;
        } catch (SQLException e) {
            logger.error("Error al listar productos por categoria {}.", idCategoria, e);
            throw e;
        }
    }

    @Override
    public List<Producto> listarStockBajo() throws Exception {
        String sql = "SELECT * FROM Productos WHERE StockActual <= StockMinimo";
        List<Producto> lista = Lists.newArrayList();
        try (Connection con = ConexionBD.getInstancia().getConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
            return lista;
        } catch (SQLException e) {
            logger.error("Error al listar productos con stock bajo.", e);
            throw e;
        }
    }

    @Override
    public List<Producto> listarTodos() throws Exception {
        String sql = "SELECT * FROM Productos";
        List<Producto> lista = Lists.newArrayList();
        try (Connection con = ConexionBD.getInstancia().getConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
            return lista;
        } catch (SQLException e) {
            logger.error("Error al listar productos.", e);
            throw e;
        }
    }

    @Override
    public boolean actualizar(Producto p) throws Exception {
        Preconditions.checkNotNull(p, "El producto no puede ser null.");
        Preconditions.checkArgument(p.getIdProducto() > 0, "El producto debe tener un id valido.");
        String sql = "UPDATE Productos SET IdCategoria=?, CodigoSKU=?, NombreProducto=?, "
                + "Descripcion=?, Marca=?, ImagenUrl=?, PrecioCompra=?, PrecioVenta=?, "
                + "PorcentajeDescuento=?, StockActual=?, StockMinimo=?, Estado=? WHERE IdProducto=?";
        try (Connection con = ConexionBD.getInstancia().getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, p.getIdCategoria());
            ps.setString(2, p.getCodigoSku());
            ps.setString(3, p.getNombreProducto());
            ps.setString(4, p.getDescripcion());
            ps.setString(5, p.getMarca());
            ps.setString(6, p.getImagenUrl());
            ps.setBigDecimal(7, p.getPrecioCompra());
            ps.setBigDecimal(8, p.getPrecioVenta());
            ps.setBigDecimal(9, p.getPorcentajeDescuento());
            ps.setInt(10, p.getStockActual());
            ps.setInt(11, p.getStockMinimo());
            ps.setBoolean(12, p.isEstado());
            ps.setInt(13, p.getIdProducto());
            int filas = ps.executeUpdate();
            logger.info("Producto id={} actualizado ({} fila(s)).", p.getIdProducto(), filas);
            return filas > 0;
        } catch (SQLException e) {
            logger.error("Error al actualizar el producto id={}.", p.getIdProducto(), e);
            throw e;
        }
    }

    /** Eliminacion LOGICA: desactiva el producto (Estado = 0). */
    @Override
    public boolean eliminar(int id) throws Exception {
        Preconditions.checkArgument(id > 0, "El id de producto debe ser positivo.");
        String sql = "UPDATE Productos SET Estado = 0 WHERE IdProducto = ?";
        try (Connection con = ConexionBD.getInstancia().getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            int filas = ps.executeUpdate();
            logger.info("Producto id={} desactivado (eliminacion logica).", id);
            return filas > 0;
        } catch (SQLException e) {
            logger.error("Error al eliminar (desactivar) el producto id={}.", id, e);
            throw e;
        }
    }

    @Override
    public int agregarStock(int idProducto, int cantidad, Integer idUsuario) throws Exception {
        Preconditions.checkArgument(idProducto > 0, "El id de producto debe ser positivo.");
        Preconditions.checkArgument(cantidad > 0, "La cantidad a agregar debe ser mayor a cero.");

        String sqlLeer = "SELECT StockActual FROM Productos WHERE IdProducto = ?";
        String sqlUpd = "UPDATE Productos SET StockActual = StockActual + ? WHERE IdProducto = ?";
        String sqlMov = "INSERT INTO MovimientosInventario "
                + "(IdProducto, TipoMovimiento, Cantidad, StockResultante, Motivo, IdUsuario) "
                + "VALUES (?, 'Entrada', ?, ?, ?, ?)";

        Connection con = null;
        try {
            con = ConexionBD.getInstancia().getConexion();
            con.setAutoCommit(false);

            int stockActual;
            try (PreparedStatement ps = con.prepareStatement(sqlLeer)) {
                ps.setInt(1, idProducto);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalArgumentException("El producto id=" + idProducto + " no existe.");
                    }
                    stockActual = rs.getInt("StockActual");
                }
            }
            int resultante = stockActual + cantidad;

            try (PreparedStatement ps = con.prepareStatement(sqlUpd)) {
                ps.setInt(1, cantidad);
                ps.setInt(2, idProducto);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement(sqlMov)) {
                ps.setInt(1, idProducto);
                ps.setInt(2, cantidad);
                ps.setInt(3, resultante);
                ps.setString(4, "Ingreso manual de stock");
                if (idUsuario != null && idUsuario > 0) {
                    ps.setInt(5, idUsuario);
                } else {
                    ps.setNull(5, java.sql.Types.INTEGER);
                }
                ps.executeUpdate();
            }

            con.commit();
            logger.info("Stock del producto id={} +{} => {}.", idProducto, cantidad, resultante);
            return resultante;
        } catch (Exception e) {
            if (con != null) {
                try { con.rollback(); } catch (SQLException ex) { logger.error("Error en rollback.", ex); }
            }
            logger.error("Error al agregar stock al producto id={}.", idProducto, e);
            throw e;
        } finally {
            if (con != null) {
                try { con.setAutoCommit(true); con.close(); } catch (SQLException ex) { logger.error("Error al cerrar conexion.", ex); }
            }
        }
    }

    private Producto mapear(ResultSet rs) throws SQLException {
        Producto p = new Producto();
        p.setIdProducto(rs.getInt("IdProducto"));
        p.setIdCategoria(rs.getInt("IdCategoria"));
        p.setCodigoSku(rs.getString("CodigoSKU"));
        p.setNombreProducto(rs.getString("NombreProducto"));
        p.setDescripcion(rs.getString("Descripcion"));
        p.setMarca(rs.getString("Marca"));
        p.setImagenUrl(rs.getString("ImagenUrl"));
        p.setPrecioCompra(rs.getBigDecimal("PrecioCompra"));
        p.setPrecioVenta(rs.getBigDecimal("PrecioVenta"));
        p.setPorcentajeDescuento(rs.getBigDecimal("PorcentajeDescuento"));
        p.setStockActual(rs.getInt("StockActual"));
        p.setStockMinimo(rs.getInt("StockMinimo"));
        p.setEstado(rs.getBoolean("Estado"));
        Timestamp ts = rs.getTimestamp("FechaRegistro");
        if (ts != null) {
            p.setFechaRegistro(ts.toLocalDateTime());
        }
        return p;
    }
}
