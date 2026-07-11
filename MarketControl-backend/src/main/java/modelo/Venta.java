package modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.google.common.collect.Lists;

/**
 * Entidad que representa la tabla [dbo].[Ventas] (venta directa POS).
 * La coleccion {@code detalles} (composicion Venta 1..* DetalleVenta) se
 * inicializa con {@link Lists#newArrayList()} de Guava.
 */
public class Venta {

    private int idVenta;
    private int idUsuario;
    private LocalDateTime fechaVenta;
    private BigDecimal total;
    private String metodoPago;
    private String estado;

    private List<DetalleVenta> detalles;

    /**
     * Id del pedido de Recojo que origina esta venta (si aplica). No es columna
     * de [Ventas]; sirve para que el DAO cierre el pedido ('Atendido') al cobrar.
     */
    private Integer idPedidoOrigen;

    public Venta() {
        this.detalles = Lists.newArrayList();
    }

    public Venta(int idVenta, int idUsuario, LocalDateTime fechaVenta, BigDecimal total,
                 String metodoPago, String estado) {
        this.idVenta = idVenta;
        this.idUsuario = idUsuario;
        this.fechaVenta = fechaVenta;
        this.total = total;
        this.metodoPago = metodoPago;
        this.estado = estado;
        this.detalles = Lists.newArrayList();
    }

    public int getIdVenta() {
        return idVenta;
    }

    public void setIdVenta(int idVenta) {
        this.idVenta = idVenta;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public LocalDateTime getFechaVenta() {
        return fechaVenta;
    }

    public void setFechaVenta(LocalDateTime fechaVenta) {
        this.fechaVenta = fechaVenta;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public String getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(String metodoPago) {
        this.metodoPago = metodoPago;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public List<DetalleVenta> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetalleVenta> detalles) {
        this.detalles = detalles;
    }

    public Integer getIdPedidoOrigen() {
        return idPedidoOrigen;
    }

    public void setIdPedidoOrigen(Integer idPedidoOrigen) {
        this.idPedidoOrigen = idPedidoOrigen;
    }
}
