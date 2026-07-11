package modelo;

import java.time.LocalDateTime;

import org.apache.commons.lang3.StringUtils;

/**
 * Entidad que representa la tabla [dbo].[Usuarios]. Los campos NOT NULL de texto
 * se validan en sus setters. La columna SQL [Usuario] (login) se modela como {@code username}.
 */
public class Usuario {

    private int idUsuario;
    private String nombres;
    private String apellidos;
    private String dni;
    private String username;        // columna [Usuario]
    private String email;
    private String contrasenaHash;  // columna [ContrasenaHash] (SIEMPRE hash, nunca texto plano)
    private String rol;             // 'ADMINISTRADOR' | 'EMPLEADO'
    private boolean estado;
    private LocalDateTime fechaRegistro;

    public Usuario() {
    }

    public Usuario(int idUsuario, String nombres, String apellidos, String dni,
                   String username, String email, String contrasenaHash,
                   String rol, boolean estado, LocalDateTime fechaRegistro) {
        this.idUsuario = idUsuario;
        setNombres(nombres);
        setApellidos(apellidos);
        setDni(dni);
        setUsername(username);
        this.email = email;
        setContrasenaHash(contrasenaHash);
        setRol(rol);
        this.estado = estado;
        this.fechaRegistro = fechaRegistro;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombres) {
        if (StringUtils.isBlank(nombres)) {
            throw new IllegalArgumentException("El campo 'nombres' no puede estar vacio.");
        }
        this.nombres = nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        if (StringUtils.isBlank(apellidos)) {
            throw new IllegalArgumentException("El campo 'apellidos' no puede estar vacio.");
        }
        this.apellidos = apellidos;
    }

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        if (StringUtils.isBlank(dni)) {
            throw new IllegalArgumentException("El campo 'dni' no puede estar vacio.");
        }
        this.dni = dni;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        if (StringUtils.isBlank(username)) {
            throw new IllegalArgumentException("El campo 'username' no puede estar vacio.");
        }
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getContrasenaHash() {
        return contrasenaHash;
    }

    public void setContrasenaHash(String contrasenaHash) {
        if (StringUtils.isBlank(contrasenaHash)) {
            throw new IllegalArgumentException("El campo 'contrasenaHash' no puede estar vacio.");
        }
        this.contrasenaHash = contrasenaHash;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        if (StringUtils.isBlank(rol)) {
            throw new IllegalArgumentException("El campo 'rol' no puede estar vacio.");
        }
        this.rol = rol;
    }

    public boolean isEstado() {
        return estado;
    }

    public void setEstado(boolean estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }
}
