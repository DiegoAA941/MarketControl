package config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Conexion a SQL Server (JDBC), patron Singleton.
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

    private ConexionBD() {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            logger.info("Driver JDBC de SQL Server cargado correctamente.");
        } catch (ClassNotFoundException e) {
            logger.error("No se encontro el driver JDBC de SQL Server (mssql-jdbc).", e);
            throw new RuntimeException("Driver JDBC de SQL Server no disponible.", e);
        }
    }

    public static synchronized ConexionBD getInstancia() {
        if (instancia == null) {
            instancia = new ConexionBD();
        }
        return instancia;
    }

    public Connection getConexion() throws SQLException {
        try {
            Connection conexion = DriverManager.getConnection(URL, USUARIO, CLAVE);
            logger.info("Conexion establecida con la base de datos '{}' en {}:{}.",
                    BASE_DATOS, SERVIDOR, PUERTO);
            return conexion;
        } catch (SQLException e) {
            logger.error("Fallo al conectar con la base de datos '{}'. Codigo SQL: {}",
                    BASE_DATOS, e.getSQLState(), e);
            throw e;
        }
    }
}
