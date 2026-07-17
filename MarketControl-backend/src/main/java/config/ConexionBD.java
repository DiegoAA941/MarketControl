package config;

import java.sql.Connection;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariConfig;  
import com.zaxxer.hikari.HikariDataSource;

/**
 * Conexion a SQL Server (JDBC) via pool de conexiones (HikariCP), patron Singleton.
 * Nunca se loguean usuario ni contrasena, solo exito/fallo y codigo SQL.
 */
public class ConexionBD {

    private static final Logger logger = LoggerFactory.getLogger(ConexionBD.class);

    private static ConexionBD instancia;

    // ---- Parametros de conexion: variables de entorno, con valores por defecto
    // para servidor/puerto/base/usuario. MC_DB_CLAVE es obligatoria (sin default).
    private static final String SERVIDOR = env("MC_DB_SERVIDOR", "localhost\\SQLEXPRESS");
    private static final String PUERTO = env("MC_DB_PUERTO", "1433");
    private static final String BASE_DATOS = env("MC_DB_NOMBRE", "MarketControl");
    private static final String USUARIO = env("MC_DB_USUARIO", "sa");
    private static final String CLAVE = envRequerida("MC_DB_CLAVE");

    private static String env(String variable, String porDefecto) {
        String valor = System.getenv(variable);
        return valor == null || valor.isBlank() ? porDefecto : valor;
    }

    private static String envRequerida(String variable) {
        String valor = System.getenv(variable);
        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException("Falta la variable de entorno " + variable
                    + " (contrasena del usuario de SQL Server).");
        }
        return valor;
    }

    private static final String URL =
            "jdbc:sqlserver://" + SERVIDOR + ":" + PUERTO +
            ";databaseName=" + BASE_DATOS +
            ";encrypt=true;trustServerCertificate=true";

    private final HikariDataSource pool;

    private ConexionBD() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(URL);
        config.setUsername(USUARIO);
        config.setPassword(CLAVE);
        config.setDriverClassName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        config.setPoolName("MarketControlPool");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        try {
            pool = new HikariDataSource(config);
        } catch (Exception e) {
            logger.error("No se pudo iniciar el pool de conexiones para '{}' en {}:{}.",
                    BASE_DATOS, SERVIDOR, PUERTO, e);
            throw new RuntimeException("Pool de conexiones JDBC no disponible.", e);
        }
        logger.info("Pool de conexiones listo para '{}' en {}:{}.", BASE_DATOS, SERVIDOR, PUERTO);
    }

    public static synchronized ConexionBD getInstancia() {
        if (instancia == null) {
            instancia = new ConexionBD();
        }
        return instancia;
    }

    public Connection getConexion() throws SQLException {
        try {
            return pool.getConnection();
        } catch (SQLException e) {
            logger.error("Fallo al obtener una conexion del pool para '{}'. Codigo SQL: {}",
                    BASE_DATOS, e.getSQLState(), e);
            throw e;
        }
    }
}
