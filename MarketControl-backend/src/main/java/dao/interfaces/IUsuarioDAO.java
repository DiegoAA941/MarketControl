package dao.interfaces;

import java.util.List;

import modelo.Usuario;

/**
 * No extiende {@link GenericDAO}: crear exige hashear la contrasena
 * ({@link #registrarUsuario}), y "buscarPorId"/"actualizar" genericos no tienen uso hoy.
 */
public interface IUsuarioDAO {

    /** Recibe la contrasena en texto plano; se guarda como hash. */
    boolean registrarUsuario(Usuario usuario, String contrasenaPlana) throws Exception;

    List<Usuario> listarTodos() throws Exception;

    /** Baja logica (Estado = 0). */
    boolean eliminar(int idUsuario) throws Exception;

    Usuario buscarPorUsername(String username) throws Exception;

    /** Recibe la contrasena en texto plano; se guarda como hash. */
    boolean actualizarContrasena(int idUsuario, String nuevaContrasenaPlana) throws Exception;

    boolean reactivar(int idUsuario) throws Exception;
}
