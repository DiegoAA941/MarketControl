package dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import config.ConexionBD;
import dao.interfaces.IConfiguracionDAO;

/**
 * Implementación JDBC de {@link IConfiguracionDAO} para [ConfiguracionNegocio].
 */
public class ConfiguracionDAOImpl implements IConfiguracionDAO {

    private static final Logger logger = LoggerFactory.getLogger(ConfiguracionDAOImpl.class);

    @Override
    public Map<String, String> obtenerTodo() throws Exception {
        String sql = "SELECT Clave, Valor FROM ConfiguracionNegocio";
        Map<String, String> mapa = new LinkedHashMap<>();
        try (Connection con = ConexionBD.getInstancia().getConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                mapa.put(rs.getString("Clave"), rs.getString("Valor"));
            }
            return mapa;
        } catch (SQLException e) {
            logger.error("Error al obtener la configuración.", e);
            throw e;
        }
    }

    @Override
    public String obtener(String clave) throws Exception {
        Preconditions.checkArgument(clave != null && !clave.isBlank(), "La clave no puede estar vacía.");
        String sql = "SELECT Valor FROM ConfiguracionNegocio WHERE Clave = ?";
        try (Connection con = ConexionBD.getInstancia().getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, clave);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("Valor") : null;
            }
        } catch (SQLException e) {
            logger.error("Error al obtener la clave '{}'.", clave, e);
            throw e;
        }
    }

    @Override
    public boolean guardar(String clave, String valor) throws Exception {
        Preconditions.checkArgument(clave != null && !clave.isBlank(), "La clave no puede estar vacía.");
        // UPSERT: intenta UPDATE; si no afecta filas, hace INSERT.
        String sqlUpd = "UPDATE ConfiguracionNegocio SET Valor = ? WHERE Clave = ?";
        String sqlIns = "INSERT INTO ConfiguracionNegocio (Clave, Valor) VALUES (?, ?)";
        try (Connection con = ConexionBD.getInstancia().getConexion()) {
            try (PreparedStatement ps = con.prepareStatement(sqlUpd)) {
                ps.setString(1, valor);
                ps.setString(2, clave);
                if (ps.executeUpdate() > 0) {
                    return true;
                }
            }
            try (PreparedStatement ps = con.prepareStatement(sqlIns)) {
                ps.setString(1, clave);
                ps.setString(2, valor);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            logger.error("Error al guardar la clave '{}'.", clave, e);
            throw e;
        }
    }
}
