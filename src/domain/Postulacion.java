
package domain;

public class Postulacion {

    private int id;
    private Usuario usuario;
    private Scrim scrim;
    private String rolDeseado;
    private String estado;

    public Postulacion(int id, Usuario usuario, Scrim scrim, String rolDeseado) {
        this.id = id;
        this.usuario = usuario;
        this.scrim = scrim;
        this.rolDeseado = rolDeseado;
        this.estado = "Pendiente";
    }

    public int getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public Scrim getScrim() {
        return scrim;
    }

    public String getRolDeseado() {
        return rolDeseado;
    }

    public String getEstado() {
        return estado;
    }

    public void aceptar() {
        if (!estado.equals("Pendiente")) {
            throw new IllegalStateException("La postulacion ya fue procesada");
        }

        estado = "Aceptada";
    }

    public void rechazar() {
        if (!estado.equals("Pendiente")) {
            throw new IllegalStateException("La postulacion ya fue procesada");
        }

        estado = "Rechazada";
    }
}
