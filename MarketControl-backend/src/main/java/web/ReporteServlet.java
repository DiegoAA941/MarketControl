package web;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dao.DAOFactory;
import dao.interfaces.ICategoriaDAO;
import dao.interfaces.IProductoDAO;
import dao.interfaces.IVentaDAO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import modelo.Categoria;
import modelo.DetalleVenta;
import modelo.Producto;
import modelo.Venta;
import servicio.CalculadoraFinanciera;

/**
 * Recurso Reportes (modulo de analisis). Calcula los indicadores a partir de
 * los DAOs existentes; no guarda datos, solo agrega.
 *
 *   GET /api/reportes/kpis?desde=&hasta=
 *   GET /api/reportes/inventario-categoria      (foto del momento actual)
 *   GET /api/reportes/productos-categoria        (foto del momento actual)
 *   GET /api/reportes/mas-vendidos?desde=&hasta=
 *   GET /api/reportes/sugerencia-pedido          (stock <= minimo)
 *
 * Las fechas se reciben como YYYY-MM-DD. Si faltan, se usa el dia de hoy.
 *
 * Para agregar un reporte nuevo basta con sumar una entrada a {@link #reportes}
 * y su metodo privado, sin tocar el despacho de {@link #doGet}.
 */
@WebServlet("/api/reportes/*")
public class ReporteServlet extends ApiBaseServlet {

    private static final long serialVersionUID = 1L;
    private final transient IVentaDAO ventaDAO = DAOFactory.ventaDAO();
    private final transient IProductoDAO productoDAO = DAOFactory.productoDAO();
    private final transient ICategoriaDAO categoriaDAO = DAOFactory.categoriaDAO();

    @FunctionalInterface
    private interface ReporteHandler {
        void manejar(HttpServletRequest req, HttpServletResponse resp) throws Exception;
    }

    private final transient Map<String, ReporteHandler> reportes = Map.<String, ReporteHandler>of(
            "/kpis", this::kpis,
            "/inventario-categoria", (req, resp) -> inventarioPorCategoria(resp),
            "/productos-categoria", (req, resp) -> productosPorCategoria(resp),
            "/mas-vendidos", this::masVendidos,
            "/sugerencia-pedido", (req, resp) -> sugerenciaPedido(resp)
    );

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!requiereAdmin(req, resp)) {
            return;
        }
        try {
            ReporteHandler handler = reportes.get(ruta(req));
            if (handler == null) {
                error(resp, HttpServletResponse.SC_NOT_FOUND, "Reporte no encontrado.");
                return;
            }
            handler.manejar(req, resp);
        } catch (Exception e) {
            manejarError(resp, e);
        }
    }

    // ---------------- Helpers de linea (redondeo UNICO a 2 decimales) ----------------
    // Se usan tanto en el KPI global como en "mas vendidos" para que la suma de las
    // lineas cuadre EXACTAMENTE con el total (sin descuadres de centimos en auditorias).

    /** Base imponible (sin IGV) de una linea: bruto / 1.18 redondeado a 2 decimales. */
    private BigDecimal baseLinea(DetalleVenta d) {
        BigDecimal bruto = (d.getSubtotal() != null)
                ? d.getSubtotal()
                : d.getPrecioUnitario().multiply(BigDecimal.valueOf(d.getCantidad()));
        return CalculadoraFinanciera.baseSinIgv(bruto);
    }

    /** Costo total de una linea: PrecioCompra * Cantidad. */
    private BigDecimal costoLinea(DetalleVenta d, Map<Integer, Producto> productos) {
        Producto p = productos.get(d.getIdProducto());
        BigDecimal costoUnit = (p != null && p.getPrecioCompra() != null)
                ? p.getPrecioCompra() : BigDecimal.ZERO;
        return costoUnit.multiply(BigDecimal.valueOf(d.getCantidad()));
    }

    /** Margen (utilidad real) de una linea: base sin IGV - costo. */
    private BigDecimal margenLinea(DetalleVenta d, Map<Integer, Producto> productos) {
        return baseLinea(d).subtract(costoLinea(d, productos));
    }

    // ---------------- KPIs superiores ----------------
    private void kpis(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        LocalDateTime[] rango = rango(req);
        List<Venta> ventas = ventaDAO.ventasPorRango(rango[0], rango[1]);
        Map<Integer, Producto> productos = mapaProductos();

        BigDecimal totalVentas = BigDecimal.ZERO;
        BigDecimal baseVentas = BigDecimal.ZERO;
        BigDecimal totalCosto = BigDecimal.ZERO;
        for (Venta v : ventas) {
            totalVentas = totalVentas.add(v.getTotal() != null ? v.getTotal() : BigDecimal.ZERO);
            for (DetalleVenta d : ventaDAO.listarDetalles(v.getIdVenta())) {
                baseVentas = baseVentas.add(baseLinea(d));
                totalCosto = totalCosto.add(costoLinea(d, productos));
            }
        }

        int transacciones = ventas.size();
        BigDecimal ticket = transacciones > 0
                ? totalVentas.divide(BigDecimal.valueOf(transacciones), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal margen = baseVentas.subtract(totalCosto);   // utilidad real: Σ((PV/1.18) - PC)
        BigDecimal margenPct = baseVentas.signum() > 0
                ? margen.multiply(BigDecimal.valueOf(100)).divide(baseVentas, 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Map<String, Object> kpi = new LinkedHashMap<>();
        kpi.put("ventasTotales", totalVentas);
        kpi.put("transacciones", transacciones);
        kpi.put("ticketPromedio", ticket);
        kpi.put("margenGanancia", margen);
        kpi.put("margenPorcentaje", margenPct);
        ok(resp, kpi);
    }

    // ---------------- Inventario por categoria (foto actual) ----------------
    private void inventarioPorCategoria(HttpServletResponse resp) throws Exception {
        Map<Integer, String> categorias = mapaCategorias();
        Map<String, BigDecimal> acumulado = new LinkedHashMap<>();
        for (Producto p : productoDAO.listarTodos()) {
            if (!p.isEstado()) {
                continue;
            }
            String cat = categorias.getOrDefault(p.getIdCategoria(), "Sin categoria");
            // Valuacion al COSTO de adquisicion (sin proyectar IGV ni utilidad no realizada).
            BigDecimal costoUnit = (p.getPrecioCompra() != null) ? p.getPrecioCompra() : BigDecimal.ZERO;
            BigDecimal valor = costoUnit.multiply(BigDecimal.valueOf(p.getStockActual()));
            acumulado.merge(cat, valor, BigDecimal::add);
        }
        ok(resp, aListaOrdenada(acumulado, "valor"));
    }

    // ---------------- Productos por categoria (foto actual) ----------------
    private void productosPorCategoria(HttpServletResponse resp) throws Exception {
        Map<Integer, String> categorias = mapaCategorias();
        Map<String, BigDecimal> conteo = new LinkedHashMap<>();
        for (Producto p : productoDAO.listarTodos()) {
            if (!p.isEstado()) {
                continue;
            }
            String cat = categorias.getOrDefault(p.getIdCategoria(), "Sin categoria");
            conteo.merge(cat, BigDecimal.ONE, BigDecimal::add);
        }
        ok(resp, aListaOrdenada(conteo, "cantidad"));
    }

    // ---------------- Top productos mas vendidos ----------------
    private void masVendidos(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        LocalDateTime[] rango = rango(req);
        Map<Integer, Producto> productos = mapaProductos();
        Map<Integer, int[]> cantidades = new LinkedHashMap<>();   // idProducto -> [cantidad]
        Map<Integer, BigDecimal> ingresos = new LinkedHashMap<>();
        Map<Integer, BigDecimal> margenes = new LinkedHashMap<>();

        for (Venta v : ventaDAO.ventasPorRango(rango[0], rango[1])) {
            for (DetalleVenta d : ventaDAO.listarDetalles(v.getIdVenta())) {
                int idp = d.getIdProducto();
                cantidades.computeIfAbsent(idp, k -> new int[1])[0] += d.getCantidad();
                ingresos.merge(idp, d.getSubtotal(), BigDecimal::add);
                margenes.merge(idp, margenLinea(d, productos), BigDecimal::add); // misma regla que el KPI
            }
        }

        List<Map<String, Object>> filas = new ArrayList<>();
        for (Map.Entry<Integer, int[]> e : cantidades.entrySet()) {
            int idp = e.getKey();
            Producto p = productos.get(idp);
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("idProducto", idp);
            fila.put("nombre", p != null ? p.getNombreProducto() : ("Producto " + idp));
            fila.put("cantidad", e.getValue()[0]);
            fila.put("ingreso", ingresos.getOrDefault(idp, BigDecimal.ZERO));
            fila.put("margen", margenes.getOrDefault(idp, BigDecimal.ZERO));
            filas.add(fila);
        }
        filas.sort(Comparator.comparingInt(f -> -((int) f.get("cantidad"))));
        if (filas.size() > 10) {
            filas = filas.subList(0, 10);
        }
        ok(resp, filas);
    }

    // ---------------- Sugerencia de pedido (stock <= minimo) ----------------
    private void sugerenciaPedido(HttpServletResponse resp) throws Exception {
        List<Map<String, Object>> filas = new ArrayList<>();
        for (Producto p : productoDAO.listarStockBajo()) {
            int sugerido = Math.max(p.getStockMinimo() * 2 - p.getStockActual(), p.getStockMinimo());
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("idProducto", p.getIdProducto());
            fila.put("nombre", p.getNombreProducto());
            fila.put("sku", p.getCodigoSku());
            fila.put("stockActual", p.getStockActual());
            fila.put("stockMinimo", p.getStockMinimo());
            fila.put("cantidadSugerida", sugerido);
            filas.add(fila);
        }
        ok(resp, filas);
    }

    // ---------------- Auxiliares ----------------
    private LocalDateTime[] rango(HttpServletRequest req) {
        String desde = req.getParameter("desde");
        String hasta = req.getParameter("hasta");
        LocalDate hoy = LocalDate.now();
        LocalDate ini = (desde != null && !desde.isBlank()) ? LocalDate.parse(desde) : hoy;
        LocalDate fin = (hasta != null && !hasta.isBlank()) ? LocalDate.parse(hasta) : hoy;
        return new LocalDateTime[]{ini.atStartOfDay(), fin.atTime(LocalTime.MAX)};
    }

    private Map<Integer, Producto> mapaProductos() throws Exception {
        Map<Integer, Producto> mapa = new LinkedHashMap<>();
        for (Producto p : productoDAO.listarTodos()) {
            mapa.put(p.getIdProducto(), p);
        }
        return mapa;
    }

    private Map<Integer, String> mapaCategorias() throws Exception {
        Map<Integer, String> mapa = new LinkedHashMap<>();
        for (Categoria c : categoriaDAO.listarTodos()) {
            mapa.put(c.getIdCategoria(), c.getNombreCategoria());
        }
        return mapa;
    }

    private List<Map<String, Object>> aListaOrdenada(Map<String, BigDecimal> datos, String claveValor) {
        List<Map<String, Object>> lista = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> e : datos.entrySet()) {
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("categoria", e.getKey());
            fila.put(claveValor, e.getValue());
            lista.add(fila);
        }
        lista.sort(Comparator.comparing(
                (Map<String, Object> f) -> (BigDecimal) f.get(claveValor)).reversed());
        return lista;
    }
}
