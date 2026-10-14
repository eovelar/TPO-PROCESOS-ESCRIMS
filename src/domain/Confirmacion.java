package domain;

public class Confirmacion {

    private int id;
    private Usuario usuario;
    private Scrim scrim;
    private boolean confirmado;

    public Confirmacion(int id, Usuario usuario, Scrim scrim) {
        if (usuario == null || scrim == null) {
            throw new IllegalArgumentException("El usuario y el scrim son obligatorios");
        }

        this.id = id;
        this.usuario = usuario;
        this.scrim = scrim;
        this.confirmado = false;
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

    public boolean isConfirmado() {
        return confirmado;
    }

    public void confirmar() {
        if (confirmado) {
            throw new IllegalStateException("El jugador ya confirmo su participacion");
        }

        this.confirmado = true;
    }
}
