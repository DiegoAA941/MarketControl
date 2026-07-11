# MarketControl

Sistema de gestión comercial para la Bodega el Progreso: Punto de Venta (POS), inventario, módulo de Delivery y una tienda virtual pública. Proyecto universitario (Proyecto Integrador I).

## Características principales

- **Tienda pública:** catálogo, carrito, pedidos por Delivery o Recojo en tienda.
- **Panel de administración:** POS, inventario, pedidos, repartidores, reportes y configuración del negocio.

## Tecnologías

- **Backend:** Java 17, Maven, Jakarta Servlets 6.0.0, Gson. Empaqueta un `.war`.
- **Base de datos:** SQL Server.
- **Servidor:** Apache Tomcat 10.1+.
- **Frontend:** HTML, CSS y JavaScript sin frameworks.

## Requisitos

- JDK 17+
- Maven 3.9+
- Apache Tomcat 10.1+
- SQL Server (Express sirve)

## Instalación

### 1. Clonar el repositorio

```bash
git clone https://github.com/tu-usuario/MarketControl.git
cd MarketControl
```

### 2. Crear la base de datos

Ejecuta **una sola vez** en SQL Server Management Studio el script `Base de datos/BaseDeDatosMarketControl.txt` (borra y recrea la base `MarketControl` desde cero; no volver a ejecutar sobre una base ya en uso).

### 3. Configurar variables de entorno

Antes de arrancar Tomcat hay que configurar estas variables (en `bin/setenv.bat` de Tomcat, o en el entorno del sistema). Las dos primeras son obligatorias: la aplicación no arranca sin ellas.

| Variable           | Obligatoria | Descripción                                                                                            |
| ------------------ | ----------- | ------------------------------------------------------------------------------------------------------ |
| `MC_CLAVE_CIFRADO` | Sí          | Clave AES-256 en Base64 (32 bytes) para cifrar datos sensibles. Generar con `openssl rand -base64 32`. |
| `MC_DB_CLAVE`      | Sí          | Contraseña del usuario de SQL Server.                                                                  |
| `MC_DB_SERVIDOR`   | No          | Default `localhost\SQLEXPRESS`.                                                                        |
| `MC_DB_PUERTO`     | No          | Default `1433`.                                                                                        |
| `MC_DB_NOMBRE`     | No          | Default `MarketControl`.                                                                               |
| `MC_DB_USUARIO`    | No          | Default `sa`.                                                                                          |

Ejemplo de `setenv.bat`:
```bat
@echo off
set "MC_CLAVE_CIFRADO=pega-aqui-tu-clave-generada"
set "MC_DB_CLAVE=tu-contrasena-de-sql-server"
```

**Nota:** el usuario administrador semilla (`admin` / `admin123`) que crea el script de base de datos tiene su DNI y Email cifrados con una clave específica. El login funciona igual sin importar qué `MC_CLAVE_CIFRADO` uses (la contraseña usa un hash aparte, PBKDF2). Pero si necesitas que ese DNI/Email sean legibles, corre `MarketControl-backend/src/main/java/util/GeneradorSeedAdmin.java` con tu propia clave y reemplaza esos valores en el `INSERT` del script.

### 4. Compilar y desplegar el backend

```bash
cd MarketControl-backend
mvn clean package
```

Copia el `.war` generado (`target/MarketControl.war`) a la carpeta `webapps/` de Tomcat y arráncalo. La API queda en `http://localhost:8080/MarketControl/api/...`.

Prueba de vida: `GET http://localhost:8080/MarketControl/api/auth/sesion` debe responder `{"error":"No hay sesion activa."}`.

### 5. Servir el frontend

Sirve `PaginaAdmin/` y `paginaUsuarios/` desde un servidor local real (por ejemplo, la extensión Live Server de VS Code), usando `localhost` para que la cookie de sesión funcione correctamente. Cada archivo JS tiene una constante `API_BASE` al inicio — ajústala si Tomcat corre en otra URL.

- Login / panel admin: `PaginaAdmin/index.html`
- Tienda pública: `paginaUsuarios/tienda.html`

### Credenciales de prueba

Usuario `admin`, contraseña `admin123`.

## Documentación adicional

Ver `Documentacion/DocumentacionTecnica.md` para detalle de arquitectura, endpoints y reglas de negocio.
