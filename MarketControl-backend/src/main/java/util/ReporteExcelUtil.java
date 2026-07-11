package util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import modelo.Producto;
import modelo.Venta;

/**
 * Utilitario para exportar reportes a Excel (.xlsx) con Apache POI.
 * Expone metodos estaticos que reciben una lista de entidades y generan el
 * archivo en la ruta indicada.
 */
public final class ReporteExcelUtil {

    private static final Logger logger = LoggerFactory.getLogger(ReporteExcelUtil.class);
    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private ReporteExcelUtil() {
    }

    public static void generarReporteProductos(List<Producto> productos, String rutaArchivo)
            throws IOException {
        Preconditions.checkNotNull(productos, "La lista de productos no puede ser null.");
        Preconditions.checkArgument(rutaArchivo != null && !rutaArchivo.isBlank(),
                "La ruta del archivo no puede estar vacia.");

        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(rutaArchivo)) {

            Sheet hoja = workbook.createSheet("Productos");
            CellStyle estiloCabecera = crearEstiloCabecera(workbook);

            String[] columnas = {"ID", "SKU", "Nombre", "Marca", "Categoria (ID)",
                    "Precio Venta", "% Descuento", "Stock", "Stock Minimo"};
            crearFilaCabecera(hoja, columnas, estiloCabecera);

            int numFila = 1;
            for (Producto p : productos) {
                Row fila = hoja.createRow(numFila++);
                fila.createCell(0).setCellValue(p.getIdProducto());
                fila.createCell(1).setCellValue(p.getCodigoSku());
                fila.createCell(2).setCellValue(p.getNombreProducto());
                fila.createCell(3).setCellValue(p.getMarca());
                fila.createCell(4).setCellValue(p.getIdCategoria());
                fila.createCell(5).setCellValue(aDouble(p.getPrecioVenta()));
                fila.createCell(6).setCellValue(aDouble(p.getPorcentajeDescuento()));
                fila.createCell(7).setCellValue(p.getStockActual());
                fila.createCell(8).setCellValue(p.getStockMinimo());
            }

            autoajustar(hoja, columnas.length);
            workbook.write(fos);
            logger.info("Reporte de productos generado: {} ({} filas).", rutaArchivo, productos.size());

        } catch (IOException e) {
            logger.error("Error al generar el reporte de productos en {}", rutaArchivo, e);
            throw e;
        }
    }

    public static void generarReporteVentas(List<Venta> ventas, String rutaArchivo)
            throws IOException {
        Preconditions.checkNotNull(ventas, "La lista de ventas no puede ser null.");
        Preconditions.checkArgument(rutaArchivo != null && !rutaArchivo.isBlank(),
                "La ruta del archivo no puede estar vacia.");

        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(rutaArchivo)) {

            Sheet hoja = workbook.createSheet("Ventas");
            CellStyle estiloCabecera = crearEstiloCabecera(workbook);

            String[] columnas = {"ID Venta", "Usuario (ID)", "Fecha", "Total",
                    "Metodo Pago", "Estado"};
            crearFilaCabecera(hoja, columnas, estiloCabecera);

            int numFila = 1;
            for (Venta v : ventas) {
                Row fila = hoja.createRow(numFila++);
                fila.createCell(0).setCellValue(v.getIdVenta());
                fila.createCell(1).setCellValue(v.getIdUsuario());
                fila.createCell(2).setCellValue(formatear(v.getFechaVenta()));
                fila.createCell(3).setCellValue(aDouble(v.getTotal()));
                fila.createCell(4).setCellValue(v.getMetodoPago());
                fila.createCell(5).setCellValue(v.getEstado());
            }

            autoajustar(hoja, columnas.length);
            workbook.write(fos);
            logger.info("Reporte de ventas generado: {} ({} filas).", rutaArchivo, ventas.size());

        } catch (IOException e) {
            logger.error("Error al generar el reporte de ventas en {}", rutaArchivo, e);
            throw e;
        }
    }

    // ---------------------- Metodos auxiliares ----------------------

    private static CellStyle crearEstiloCabecera(Workbook workbook) {
        CellStyle estilo = workbook.createCellStyle();
        Font fuente = workbook.createFont();
        fuente.setBold(true);
        fuente.setColor(IndexedColors.WHITE.getIndex());
        estilo.setFont(fuente);
        estilo.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return estilo;
    }

    private static void crearFilaCabecera(Sheet hoja, String[] columnas, CellStyle estilo) {
        Row cabecera = hoja.createRow(0);
        for (int i = 0; i < columnas.length; i++) {
            Cell celda = cabecera.createCell(i);
            celda.setCellValue(columnas[i]);
            celda.setCellStyle(estilo);
        }
    }

    private static void autoajustar(Sheet hoja, int numColumnas) {
        for (int i = 0; i < numColumnas; i++) {
            hoja.autoSizeColumn(i);
        }
    }

    private static double aDouble(BigDecimal valor) {
        return valor == null ? 0d : valor.doubleValue();
    }

    private static String formatear(LocalDateTime fecha) {
        return fecha == null ? "" : fecha.format(FORMATO_FECHA);
    }
}
