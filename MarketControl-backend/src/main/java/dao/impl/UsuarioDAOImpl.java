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
import dao.interfaces.IUsuarioDAO;
import modelo.Usuario;
import util.CryptoUtil;
import util.PasswordUtil;

/**
 * Implementacion JDBC de {@link IUsuarioDAO} para la tabla [dbo].[Usuarios].
 * Nunca se loguean contrasenas; el hash se delega a {@link PasswordUtil}.
 */
public class UsuarioDAOImpl implements IUsuarioDAO {

    private static final Logger logger = LoggerFactory.getLogger(UsuarioDAOImpl.class);

    /** Recibe la contrasena en texto plano, la hashea y jamas la loguea. */
    @Override
    public boolean registrarUsuario(Usuario usuario, String contrasenaPlana) throws Exception {
        Preconditions.checkNotNull(usuario, "El usuario no puede ser null.");
        Preconditions.checkArgument(contrasenaPlana != null && !contrasenaPlana.isBlank(),
                "La contrasena no puede estar vacia.");
        usuario.setContrasenaHash(PasswordUtil.hashContrasena(contrasenaPlana));
        logger.info("Registrando nuevo usuario '{}'.", usuario.getUsername());
        return crear(usuario);
    }

    /** Insercion cruda; solo la usa {@link #registrarUsuario} (ya con el hash listo). */
    private boolean crear(Usuario usuario) throws Exception {
        Preconditions.checkNotNull(usuario, "El usuario no puede ser null.");
        String sql = "INSERT INTO Usuarios (Nombres, Apellidos, DNI, DniHash, Usuario, Email, "
                + "EmailHash, ContrasenaHash, Rol, Estado) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = ConexionBD.getInstancia().getConexion();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, usuario.getNombres());
            ps.setString(2, usuario.getApellidos());
            ps.setString(3, CryptoUtil.cifrar(usuario.getDni()));
            ps.setString(4, CryptoUtil.indiceCiego(usuario.getDni()));
            ps.setString(5, usuario.getUsername());
            ps.setString(6, CryptoUtil.cifrar(usuario.getEmail()));
            ps.setString(7, CryptoUtil.indiceCiego(usuario.getEmail()));
            ps.setString(8, usuario.getContrasenaHash());
            ps.setString(9, usuario.getRol());
            ps.setBoolean(10, usuario.isEstado());

            int filas = ps.executeUpdate();
            try (ResultSet generadas = ps.getGeneratedKeys()) {
                if (generadas.next()) {
                    usuario.setIdUsuario(generadas.getInt(1));
                }
            }
            logger.info("Usuario '{}' creado correctamente (id={}).",
                    usuario.getUsername(), usuario.getIdUsuario());
            return filas > 0;
        } catch (SQLException e) {
            logger.error("Error al crear el usuario '{}'.", usuario.getUsername(), e);
            throw e;
        }
    }

    @Override
    public Usuario buscarPorUsername(String username) throws Exception {
        Preconditions.checkArgument(username != null && !username.isBlank(),
                "El username no puede estar vacio.");
        return buscarPorCampo("Usuario", username);
    }

    @Override
    public List<Usuario> listarTodos() throws Exception {
        String sql = "SELECT * FROM Usuarios";
        List<Usuario> lista = Lists.newArrayList();
        try (Connection con = ConexionBD.getInstancia().getConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
            return lista;
        } catch (SQLException e) {
            logger.error("Error al listar usuarios.", e);
            throw e;
        }
    }

    @Override
    public boolean actualizarContrasena(int idUsuario, String nuevaContrasenaPlana) throws Exception {
        Preconditions.checkArgument(idUsuario > 0, "El id de usuario debe ser positivo.");
        Preconditions.checkArgument(nuevaContrasenaPlana != null && !nuevaContrasenaPlana.isBlank(),
                "La nueva contrasena no puede estar vacia.");
        String hash = PasswordUtil.hashContrasena(nuevaContrasenaPlana);
        String sql = "UPDATE Usuarios SET ContrasenaHash = ? WHERE IdUsuario = ?";
        try (Connection con = ConexionBD.getInstancia().getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, hash);
            ps.setInt(2, idUsuario);
            int filas = ps.executeUpdate();
            logger.info("Contrasena del usuario id={} restablecida.", idUsuario);
            return filas > 0;
        } catch (SQLException e) {
            logger.error("Error al restablecer la contrasena del usuario id={}.", idUsuario, e);
            throw e;
        }
    }

    @Override
    public boolean reactivar(int idUsuario) throws Exception {
        Preconditions.checkArgument(idUsuario > 0, "El id de usuario debe ser positivo.");
        String sql = "UPDATE Usuarios SET Estado = 1 WHERE IdUsuario = ?";
        try (Connection con = ConexionBD.getInstancia().getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            int filas = ps.executeUpdate();
            logger.info("Usuario id={} reactivado (Estado = 1).", idUsuario);
            return filas > 0;
        } catch (SQLException e) {
            logger.error("Error al reactivar el usuario id={}.", idUsuario, e);
            throw e;
        }
    }

    /** Eliminacion LOGICA: desactiva el usuario (Estado = 0). */
    @Override
    public boolean eliminar(int id) throws Exception {
        Preconditions.checkArgument(id > 0, "El id de usuario debe ser positivo.");
        String sql = "UPDATE Usuarios SET Estado = 0 WHERE IdUsuario = ?";
        try (Connection con = ConexionBD.getInstancia().getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            int filas = ps.executeUpdate();
            logger.info("Usuario id={} desactivado (eliminacion logica).", id);
            return filas > 0;
        } catch (SQLException e) {
            logger.error("Error al eliminar (desactivar) el usuario id={}.", id, e);
            throw e;
        }
    }

    // ---------------------- Auxiliares ----------------------

    private Usuario buscarPorCampo(String campo, Object valor) throws Exception {
        String sql = "SELECT * FROM Usuarios WHERE " + campo + " = ?";
        try (Connection con = ConexionBD.getInstancia().getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, valor);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapear(rs);
                }
                return null;
            }
        } catch (SQLException e) {
            logger.error("Error al buscar usuario por {}.", campo, e);
            throw e;
        }
    }

    private Usuario mapear(ResultSet rs) throws SQLException {
        Usuario u = new Usuario();
        u.setIdUsuario(rs.getInt("IdUsuario"));
        u.setNombres(rs.getString("Nombres"));
        u.setApellidos(rs.getString("Apellidos"));
        u.setDni(CryptoUtil.descifrar(rs.getString("DNI")));
        u.setUsername(rs.getString("Usuario"));
        u.setEmail(CryptoUtil.descifrar(rs.getString("Email")));
        u.setContrasenaHash(rs.getString("ContrasenaHash"));
        u.setRol(rs.getString("Rol"));
        u.setEstado(rs.getBoolean("Estado"));
        Timestamp ts = rs.getTimestamp("FechaRegistro");
        if (ts != null) {
            u.setFechaRegistro(ts.toLocalDateTime());
        }
        return u;
    }
}
