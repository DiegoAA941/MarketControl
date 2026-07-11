package modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.apache.commons.lang3.StringUtils;

/**
 * Entidad que representa la tabla [dbo].[Productos].
 */
public class Producto {

    private int idProducto;
    private int idCategoria;
    private String codigoSku;
    private String nombreProducto;
    private String descripcion;
    private String marca;
    private String imagenUrl;
    private BigDecimal precioCompra;
    private BigDecimal precioVenta;
    private BigDecimal porcentajeDescuento;
    private int stockActual;
    private int stockMinimo;
    private LocalDateTime fechaRegistro;
    private boolean estado;

    public Producto() {
    }

    public Producto(int idProducto, int idCategoria, String codigoSku, String nombreProducto,
                    String descripcion, String marca, String imagenUrl, BigDecimal precioCompra,
                    BigDecimal precioVenta, BigDecimal porcentajeDescuento, int stockActual,
                    int stockMinimo, LocalDateTime fechaRegistro, boolean estado) {
        this.idProducto = idProducto;
        this.idCategoria = idCategoria;
        setCodigoSku(codigoSku);
        setNombreProducto(nombreProducto);
        this.descripcion = descripcion;
        this.marca = marca;
        this.imagenUrl = imagenUrl;
        this.precioCompra = precioCompra;
        this.precioVenta = precioVenta;
        this.porcentajeDescuento = porcentajeDescuento;
        this.stockActual = stockActual;
        this.stockMinimo = stockMinimo;
        this.fechaRegistro = fechaRegistro;
        this.estado = estado;
    }

    public int getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(int idProducto) {
        this.idProducto = idProducto;
    }

    public int getIdCategoria() {
        return idCategoria;
    }

    public void setIdCategoria(int idCategoria) {
        this.idCategoria = idCategoria;
    }

    public String getCodigoSku() {
        return codigoSku;
    }

    public void setCodigoSku(String codigoSku) {
        if (StringUtils.isBlank(codigoSku)) {
            throw new IllegalArgumentException("El campo 'codigoSku' no puede estar vacio.");
        }
        this.codigoSku = codigoSku;
    }

    public String getNombreProducto() {
        return nombreProducto;
    }

    public void setNombreProducto(String nombreProducto) {
        if (StringUtils.isBlank(nombreProducto)) {
            throw new IllegalArgumentException("El campo 'nombreProducto' no puede estar vacio.");
        }
        this.nombreProducto = nombreProducto;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public String getImagenUrl() {
        return imagenUrl;
    }

    public void setImagenUrl(String imagenUrl) {
        this.imagenUrl = imagenUrl;
    }

    public BigDecimal getPrecioCompra() {
        return precioCompra;
    }

    public void setPrecioCompra(BigDecimal precioCompra) {
        if (precioCompra == null || precioCompra.signum() <= 0) {
            throw new IllegalArgumentException("El campo 'precioCompra' debe ser mayor a 0.");
        }
        this.precioCompra = precioCompra;
    }

    public BigDecimal getPrecioVenta() {
        return precioVenta;
    }

    public void setPrecioVenta(BigDecimal precioVenta) {
        if (precioVenta == null || precioVenta.signum() <= 0) {
            throw new IllegalArgumentException("El campo 'precioVenta' debe ser mayor a 0.");
        }
        this.precioVenta = precioVenta;
    }

    public BigDecimal getPorcentajeDescuento() {
        return porcentajeDescuento;
    }

    public void setPorcentajeDescuento(BigDecimal porcentajeDescuento) {
        this.porcentajeDescuento = porcentajeDescuento;
    }

    public int getStockActual() {
        return stockActual;
    }

    public void setStockActual(int stockActual) {
        if (stockActual < 0) {
            throw new IllegalArgumentException("El campo 'stockActual' no puede ser negativo.");
        }
        this.stockActual = stockActual;
    }

    public int getStockMinimo() {
        return stockMinimo;
    }

    public void setStockMinimo(int stockMinimo) {
        this.stockMinimo = stockMinimo;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public boolean isEstado() {
        return estado;
    }

    public void setEstado(boolean estado) {
        this.estado = estado;
    }
}
