package servicio;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Reglas financieras del negocio (extraccion de IGV, precio con descuento).
 * Separada de los Servlets para poder cambiarla o probarla sin HttpServletRequest/Response.
 */
public final class CalculadoraFinanciera {

    /** Los precios incluyen IGV (18%); para la utilidad real se usa la base = precio / 1.18. */
    private static final BigDecimal FACTOR_IGV = new BigDecimal("1.18");

    private CalculadoraFinanciera() {
    }

    /** Base imponible (sin IGV) de un monto que ya incluye IGV, redondeada a 2 decimales. */
    public static BigDecimal baseSinIgv(BigDecimal totalConIgv) {
        if (totalConIgv == null) {
            return BigDecimal.ZERO;
        }
        return totalConIgv.divide(FACTOR_IGV, 2, RoundingMode.HALF_UP);
    }

    /** IGV contenido en un monto que ya lo incluye (Total - Base). */
    public static BigDecimal igv(BigDecimal totalConIgv) {
        if (totalConIgv == null) {
            return BigDecimal.ZERO;
        }
        return totalConIgv.subtract(baseSinIgv(totalConIgv));
    }

    /** Precio de venta aplicando el porcentaje de descuento del producto (0-100). */
    public static BigDecimal precioConDescuento(BigDecimal precioVenta, BigDecimal porcentajeDescuento) {
        BigDecimal desc = porcentajeDescuento != null ? porcentajeDescuento : BigDecimal.ZERO;
        if (desc.signum() <= 0) {
            return precioVenta.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal factor = BigDecimal.valueOf(100).subtract(desc).divide(BigDecimal.valueOf(100));
        return precioVenta.multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }
}
