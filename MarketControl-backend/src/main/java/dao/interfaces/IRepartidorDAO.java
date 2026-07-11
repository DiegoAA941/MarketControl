package dao.interfaces;

import java.util.List;
import modelo.Repartidor;

/**
 * No extiende {@link GenericDAO}: hoy no hay pantalla para consultar por id
 * ni editar un repartidor, solo crear/listar/dar de baja/cambiar disponibilidad.
 */
public interface IRepartidorDAO {

    boolean crear(Repartidor repartidor) throws Exception;

    List<Repartidor> listarTodos() throws Exception;

    /** Baja logica. */
    boolean eliminar(int idRepartidor) throws Exception;

    List<Repartidor> listarDisponibles() throws Exception;

    /** 'Disponible', 'En ruta', 'Inactivo'. */
    boolean actualizarEstado(int idRepartidor, String estado) throws Exception;
}
