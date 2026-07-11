package modelo;

/**
 * Entidad que representa la tabla [dbo].[Categorias].
 */
public class Categoria {

    private int idCategoria;
    private String nombreCategoria;
    private String descripcion;
    private boolean estado; // columna [Estado] (bit)

    public Categoria() {
    }

    public Categoria(int idCategoria, String nombreCategoria, String descripcion, boolean estado) {
        this.idCategoria = idCategoria;
        this.nombreCategoria = nombreCategoria;
        this.descripcion = descripcion;
        this.estado = estado;
    }

    public int getIdCategoria() {
        return idCategoria;
    }

    public void setIdCategoria(int idCategoria) {
        this.idCategoria = idCategoria;
    }

    public String getNombreCategoria() {
        return nombreCategoria;
    }

    public void setNombreCategoria(String nombreCategoria) {
        this.nombreCategoria = nombreCategoria;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public boolean isEstado() {
        return estado;
    }

    public void setEstado(boolean estado) {
        this.estado = estado;
    }
}
