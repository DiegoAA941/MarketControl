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
import dao.interfaces.ICategoriaDAO;
import modelo.Categoria;

/**
 * Implementacion JDBC de {@link ICategoriaDAO} para la tabla [dbo].[Categorias].
 */
public class CategoriaDAOImpl implements ICategoriaDAO {

    private static final Logger logger = LoggerFactory.getLogger(CategoriaDAOImpl.class);

    @Override
    public boolean crear(Categoria c) throws Exception {
        Preconditions.checkNotNull(c, "La categoria no puede ser null.");
        String sql = "INSERT INTO Categorias (NombreCategoria, Descripcion, Estado) VALUES (?, ?, ?)";
        try (Connection con = ConexionBD.getInstancia().getConexion();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.getNombreCategoria());
            ps.setString(2, c.getDescripcion());
            ps.setBoolean(3, c.isEstado());
            int filas = ps.executeUpdate();
            try (ResultSet generadas = ps.getGeneratedKeys()) {
                if (generadas.next()) {
                    c.setIdCategoria(generadas.getInt(1));
                }
            }
            logger.info("Categoria '{}' creada (id={}).", c.getNombreCategoria(), c.getIdCategoria());
            return filas > 0;
        } catch (SQLException e) {
            logger.error("Error al crear la categoria '{}'.", c.getNombreCategoria(), e);
            throw e;
        }
    }

    @Override
    public Categoria buscarPorId(int id) throws Exception {
        Preconditions.checkArgument(id > 0, "El id de categoria debe ser positivo.");
        String sql = "SELECT * FROM Categorias WHERE IdCategoria = ?";
        try (Connection con = ConexionBD.getInstancia().getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        } catch (SQLException e) {
            logger.error("Error al buscar la categoria id={}.", id, e);
            throw e;
        }
    }

    @Override
    public List<Categoria> listarTodos() throws Exception {
        String sql = "SELECT * FROM Categorias";
        List<Categoria> lista = Lists.newArrayList();
        try (Connection con = ConexionBD.getInstancia().getConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
            return lista;
        } catch (SQLException e) {
            logger.error("Error al listar categorias.", e);
            throw e;
        }
    }

    @Override
    public boolean actualizar(Categoria c) throws Exception {
        Preconditions.checkNotNull(c, "La categoria no puede ser null.");
        Preconditions.checkArgument(c.getIdCategoria() > 0, "La categoria debe tener un id valido.");
        String sql = "UPDATE Categorias SET NombreCategoria=?, Descripcion=?, Estado=? WHERE IdCategoria=?";
        try (Connection con = ConexionBD.getInstancia().getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, c.getNombreCategoria());
            ps.setString(2, c.getDescripcion());
            ps.setBoolean(3, c.isEstado());
            ps.setInt(4, c.getIdCategoria());
            int filas = ps.executeUpdate();
            logger.info("Categoria id={} actualizada ({} fila(s)).", c.getIdCategoria(), filas);
            return filas > 0;
        } catch (SQLException e) {
            logger.error("Error al actualizar la categoria id={}.", c.getIdCategoria(), e);
            throw e;
        }
    }

    /** Eliminacion LOGICA: desactiva la categoria (Estado = 0). */
    @Override
    public boolean eliminar(int id) throws Exception {
        Preconditions.checkArgument(id > 0, "El id de categoria debe ser positivo.");
        String sql = "UPDATE Categorias SET Estado = 0 WHERE IdCategoria = ?";
        try (Connection con = ConexionBD.getInstancia().getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            int filas = ps.executeUpdate();
            logger.info("Categoria id={} desactivada (eliminacion logica).", id);
            return filas > 0;
        } catch (SQLException e) {
            logger.error("Error al eliminar (desactivar) la categoria id={}.", id, e);
            throw e;
        }
    }

    private Categoria mapear(ResultSet rs) throws SQLException {
        Categoria c = new Categoria();
        c.setIdCategoria(rs.getInt("IdCategoria"));
        c.setNombreCategoria(rs.getString("NombreCategoria"));
        c.setDescripcion(rs.getString("Descripcion"));
        c.setEstado(rs.getBoolean("Estado"));
        return c;
    }
}
