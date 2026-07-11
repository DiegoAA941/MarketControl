package dao.interfaces;

import java.util.Map;

/**
 * DAO de la tabla [ConfiguracionNegocio] (clave/valor). Guarda ajustes como el
 * teléfono de WhatsApp o el nombre del negocio, compartidos entre el panel y la
 * tienda pública.
 */
public interface IConfiguracionDAO {

    Map<String, String> obtenerTodo() throws Exception;

    String obtener(String clave) throws Exception;

    /** UPSERT: inserta o actualiza segun exista la clave. */
    boolean guardar(String clave, String valor) throws Exception;
}
