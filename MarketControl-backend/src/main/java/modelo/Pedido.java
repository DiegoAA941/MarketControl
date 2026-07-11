package modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.google.common.collect.Lists;

/**
 * Entidad que representa la tabla [dbo].[Pedidos] (delivery).
 * {@code idRepartidor} es Integer porque la columna admite NULL.
 * La coleccion {@code detalles} se inicializa con {@link Lists#newArrayList()}.
 */
public class Pedido {

    private int idPedido;
    private int idCliente;
    private Integer idRepartidor;
    private LocalDateTime fechaPedido;
    private String estado;
    private BigDecimal total;
    private String observaciones;
    private String tipoEntrega; // 'Delivery' | 'Recojo'
    private BigDecimal costoEnvio; // tarifa de envío (solo Delivery; 0 en Recojo)

    // Composicion: el pedido referencia al Repartidor como objeto, no solo su id.
    private Repartidor repartidor;

    // Composicion: el pedido referencia al Cliente (nombre/telefono/direccion del delivery).
    private Cliente cliente;

    private List<DetallePedido> detalles;

    public Pedido() {
        this.detalles = Lists.newArrayList();
    }

    public Pedido(int idPedido, int idCliente, Integer idRepartidor, LocalDateTime fechaPedido,
                  String estado, BigDecimal total, String observaciones) {
        this.idPedido = idPedido;
        this.idCliente = idCliente;
        this.idRepartidor = idRepartidor;
        this.fechaPedido = fechaPedido;
        this.estado = estado;
        this.total = total;
        this.observaciones = observaciones;
        this.detalles = Lists.newArrayList();
    }

    public int getIdPedido() {
        return idPedido;
    }

    public void setIdPedido(int idPedido) {
        this.idPedido = idPedido;
    }

    public int getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(int idCliente) {
        this.idCliente = idCliente;
    }

    public Integer getIdRepartidor() {
        return idRepartidor;
    }

    public void setIdRepartidor(Integer idRepartidor) {
        this.idRepartidor = idRepartidor;
    }

    /** Repartidor asignado (composicion). Puede ser null si aun no se asigna. */
    public Repartidor getRepartidor() {
        return repartidor;
    }

    /** Sincroniza {@code idRepartidor} (FK) para mantener un solo origen de verdad. */
    public void setRepartidor(Repartidor repartidor) {
        this.repartidor = repartidor;
        this.idRepartidor = (repartidor != null) ? repartidor.getIdRepartidor() : null;
    }

    /** Cliente del pedido (composicion). Trae nombre/telefono/direccion del delivery. */
    public Cliente getCliente() {
        return cliente;
    }

    /** Asigna el cliente y sincroniza el {@code idCliente} (FK). */
    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
        if (cliente != null && cliente.getIdCliente() > 0) {
            this.idCliente = cliente.getIdCliente();
        }
    }

    public LocalDateTime getFechaPedido() {
        return fechaPedido;
    }

    public void setFechaPedido(LocalDateTime fechaPedido) {
        this.fechaPedido = fechaPedido;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public String getTipoEntrega() {
        return tipoEntrega;
    }

    public void setTipoEntrega(String tipoEntrega) {
        this.tipoEntrega = tipoEntrega;
    }

    public BigDecimal getCostoEnvio() {
        return costoEnvio;
    }

    public void setCostoEnvio(BigDecimal costoEnvio) {
        this.costoEnvio = costoEnvio;
    }

    public List<DetallePedido> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetallePedido> detalles) {
        this.detalles = detalles;
    }
}
