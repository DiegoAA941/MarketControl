package modelo;

import java.time.LocalDateTime;

/**
 * Entidad que representa la tabla [dbo].[Clientes].
 * La direccion de entrega del delivery se almacena aqui (no en Pedido).
 */
public class Cliente {

    private int idCliente;
    private String nombres;
    private String telefono;
    private String direccion;
    private String referencia;
    private LocalDateTime fechaRegistro;

    public Cliente() {
    }

    public Cliente(int idCliente, String nombres, String telefono, String direccion,
                   String referencia, LocalDateTime fechaRegistro) {
        this.idCliente = idCliente;
        this.nombres = nombres;
        this.telefono = telefono;
        this.direccion = direccion;
        this.referencia = referencia;
        this.fechaRegistro = fechaRegistro;
    }

    public int getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(int idCliente) {
        this.idCliente = idCliente;
    }

    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombres) {
        this.nombres = nombres;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getReferencia() {
        return referencia;
    }

    public void setReferencia(String referencia) {
        this.referencia = referencia;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }
}
