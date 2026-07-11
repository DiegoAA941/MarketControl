package modelo;

import org.apache.commons.lang3.StringUtils;

/**
 * Entidad que representa la tabla [dbo].[Repartidores]. El estado es texto
 * ('Disponible', 'En ruta', 'Inactivo'), no bit como en otras tablas.
 */
public class Repartidor {

    private int idRepartidor;
    private String nombres;
    private String telefono;
    private String estado; // 'Disponible' | 'En ruta' | 'Inactivo'

    public Repartidor() {
    }

    public Repartidor(int idRepartidor, String nombres, String telefono, String estado) {
        this.idRepartidor = idRepartidor;
        setNombres(nombres);
        this.telefono = telefono;
        this.estado = estado;
    }

    public int getIdRepartidor() {
        return idRepartidor;
    }

    public void setIdRepartidor(int idRepartidor) {
        this.idRepartidor = idRepartidor;
    }

    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombres) {
        if (StringUtils.isBlank(nombres)) {
            throw new IllegalArgumentException("El campo 'nombres' del repartidor no puede estar vacio.");
        }
        this.nombres = nombres;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
