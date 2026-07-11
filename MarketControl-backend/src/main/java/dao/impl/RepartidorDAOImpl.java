package dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import config.ConexionBD;
import dao.interfaces.IRepartidorDAO;
import modelo.Repartidor;
import util.CryptoUtil;

/**
 * Implementacion JDBC de {@link IRepartidorDAO} para [dbo].[Repartidores].
 */
public class RepartidorDAOImpl implements IRepartidorDAO {

    private static final Logger logger = LoggerFactory.getLogger(RepartidorDAOImpl.class);

    @Override
    public boolean crear(Repartidor r) throws Exception {
        Preconditions.checkNotNull(r, "El repartidor no puede ser null.");
        String sql = "INSERT INTO Repartidores (Nombres, Telefono, Estado) VALUES (?, ?, ?)";
        try (Connection con = ConexionBD.getInstancia().getConexion();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, r.getNombres());
            ps.setString(2, CryptoUtil.cifrar(r.getTelefono()));
            ps.setString(3, r.getEstado() != null ? r.getEstado() : "Disponible");
            int filas = ps.executeUpdate();
            try (ResultSet generadas = ps.getGeneratedKeys()) {
                if (generadas.next()) {
                    r.setIdRepartidor(generadas.getInt(1));
                }
            }
            logger.info("Repartidor '{}' creado (id={}).", r.getNombres(), r.getIdRepartidor());
            return filas > 0;
        } catch (SQLException e) {
            logger.error("Error al crear el repartidor '{}'.", r.getNombres(), e);
            throw e;
        }
    }

    @Override
    public List<Repartidor> listarTodos() throws Exception {
        String sql = "SELECT * FROM Repartidores";
        List<Repartidor> lista = Lists.newArrayList();
        try (Connection con = ConexionBD.getInstancia().getConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
            return lista;
        } catch (SQLException e) {
            logger.error("Error al listar repartidores.", e);
            throw e;
        }
    }

    @Override
    public List<Repartidor> listarDisponibles() throws Exception {
        String sql = "SELECT * FROM Repartidores WHERE Estado = 'Disponible'";
        List<Repartidor> lista = Lists.newArrayList();
        try (Connection con = ConexionBD.getInstancia().getConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
            return lista;
        } catch (SQLException e) {
            logger.error("Error al listar repartidores disponibles.", e);
            throw e;
        }
    }

    @Override
    public boolean actualizarEstado(int idRepartidor, String estado) throws Exception {
        Preconditions.checkArgument(idRepartidor > 0, "El id de repartidor debe ser positivo.");
        Preconditions.checkArgument(estado != null && !estado.isBlank(),
                "El estado no puede estar vacio.");
        String sql = "UPDATE Repartidores SET Estado = ? WHERE IdRepartidor = ?";
        try (Connection con = ConexionBD.getInstancia().getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, estado);
            ps.setInt(2, idRepartidor);
            int filas = ps.executeUpdate();
            logger.info("Repartidor id={} cambio a estado '{}'.", idRepartidor, estado);
            return filas > 0;
        } catch (SQLException e) {
            logger.error("Error al actualizar el estado del repartidor id={}.", idRepartidor, e);
            throw e;
        }
    }

    /** Eliminacion LOGICA: marca al repartidor como 'Inactivo'. */
    @Override
    public boolean eliminar(int id) throws Exception {
        return actualizarEstado(id, "Inactivo");
    }

    private Repartidor mapear(ResultSet rs) throws SQLException {
        Repartidor r = new Repartidor();
        r.setIdRepartidor(rs.getInt("IdRepartidor"));
        r.setNombres(rs.getString("Nombres"));
        r.setTelefono(CryptoUtil.descifrar(rs.getString("Telefono")));
        r.setEstado(rs.getString("Estado"));
        return r;
    }
}
