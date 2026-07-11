package dao.interfaces;

import java.util.List;

/**
 * CRUD generico del patron DAO. {@code throws Exception} para no acoplar la
 * abstraccion a JDBC/SQLException; cada implementacion decide como propagarla.
 */
public interface GenericDAO<T> {

    boolean crear(T entidad) throws Exception;

    T buscarPorId(int id) throws Exception;

    List<T> listarTodos() throws Exception;

    boolean actualizar(T entidad) throws Exception;

    /** Logica o fisica segun la implementacion. */
    boolean eliminar(int id) throws Exception;
}
