package dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import config.ConexionBD;
import dao.interfaces.IClienteDAO;
import modelo.Cliente;
import util.CryptoUtil;

/**
 * Implementacion JDBC de {@link IClienteDAO} para la tabla [dbo].[Clientes].
 */
public class ClienteDAOImpl implements IClienteDAO {

    private static final Logger logger = LoggerFactory.getLogger(ClienteDAOImpl.class);

    @Override
    public boolean crear(Cliente c) throws Exception {
        Preconditions.checkNotNull(c, "El cliente no puede ser null.");
        String sql = "INSERT INTO Clientes (Nombres, Telefono, TelefonoHash, Direccion, Referencia) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection con = ConexionBD.getInstancia().getConexion();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.getNombres());
            ps.setString(2, CryptoUtil.cifrar(c.getTelefono()));
            ps.setString(3, CryptoUtil.indiceCiego(c.getTelefono()));
            ps.setString(4, CryptoUtil.cifrar(c.getDireccion()));
            ps.setString(5, CryptoUtil.cifrar(c.getReferencia()));
            int filas = ps.executeUpdate();
            try (ResultSet generadas = ps.getGeneratedKeys()) {
                if (generadas.next()) {
                    c.setIdCliente(generadas.getInt(1));
                }
            }
            logger.info("Cliente '{}' creado (id={}).", c.getNombres(), c.getIdCliente());
            return filas > 0;
        } catch (SQLException e) {
            logger.error("Error al crear el cliente '{}'.", c.getNombres(), e);
            throw e;
        }
    }

    @Override
    public Cliente buscarPorTelefono(String telefono) throws Exception {
        Preconditions.checkArgument(telefono != null && !telefono.isBlank(),
                "El telefono no puede estar vacio.");
        String sql = "SELECT * FROM Clientes WHERE TelefonoHash = ?";
        try (Connection con = ConexionBD.getInstancia().getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, CryptoUtil.indiceCiego(telefono));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        } catch (SQLException e) {
            logger.error("Error al buscar cliente por Telefono.", e);
            throw e;
        }
    }

    @Override
    public boolean actualizar(Cliente c) throws Exception {
        Preconditions.checkNotNull(c, "El cliente no puede ser null.");
        Preconditions.checkArgument(c.getIdCliente() > 0, "El cliente debe tener un id valido.");
        String sql = "UPDATE Clientes SET Nombres=?, Telefono=?, TelefonoHash=?, Direccion=?, Referencia=? "
                + "WHERE IdCliente=?";
        try (Connection con = ConexionBD.getInstancia().getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, c.getNombres());
            ps.setString(2, CryptoUtil.cifrar(c.getTelefono()));
            ps.setString(3, CryptoUtil.indiceCiego(c.getTelefono()));
            ps.setString(4, CryptoUtil.cifrar(c.getDireccion()));
            ps.setString(5, CryptoUtil.cifrar(c.getReferencia()));
            ps.setInt(6, c.getIdCliente());
            int filas = ps.executeUpdate();
            logger.info("Cliente id={} actualizado ({} fila(s)).", c.getIdCliente(), filas);
            return filas > 0;
        } catch (SQLException e) {
            logger.error("Error al actualizar el cliente id={}.", c.getIdCliente(), e);
            throw e;
        }
    }

    private Cliente mapear(ResultSet rs) throws SQLException {
        Cliente c = new Cliente();
        c.setIdCliente(rs.getInt("IdCliente"));
        c.setNombres(rs.getString("Nombres"));
        c.setTelefono(CryptoUtil.descifrar(rs.getString("Telefono")));
        c.setDireccion(CryptoUtil.descifrar(rs.getString("Direccion")));
        c.setReferencia(CryptoUtil.descifrar(rs.getString("Referencia")));
        Timestamp ts = rs.getTimestamp("FechaRegistro");
        if (ts != null) {
            c.setFechaRegistro(ts.toLocalDateTime());
        }
        return c;
    }
}
