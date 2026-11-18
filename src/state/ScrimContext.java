
package state;

public class ScrimContext {

    private ScrimState estado;

    public ScrimContext() {
        this.estado = new BuscandoState();
    }

    // reconstruye el contexto a partir del estado guardado en la base
    public static ScrimContext desdeNombre(String nombre) {

        if (nombre == null) {
            throw new IllegalArgumentException(
                    "El estado no puede ser nulo"
            );
        }

        ScrimState estado = switch (nombre) {
            case "Buscando jugadores" -> new BuscandoState();
            case "Lobby armado" -> new LobbyArmadoState();
            case "Confirmado" -> new ConfirmadoState();
            case "En juego" -> new EnJuegoState();
            case "Finalizado" -> new FinalizadoState();
            case "Cancelado" -> new CanceladoState();
            default -> throw new IllegalArgumentException(
                    "Estado desconocido: " + nombre
            );
        };

        ScrimContext contexto = new ScrimContext();
        contexto.setEstado(estado);

        return contexto;
    }

    public void setEstado(ScrimState estado) {
        if (estado == null) {
            throw new IllegalArgumentException(
                    "El estado no puede ser nulo"
            );
        }

        this.estado = estado;
    }

    public String getEstadoActual() {
        return estado.getNombre();
    }

    public void completarCupos() {
        estado.completarCupos(this);
    }

    public void confirmarJugadores() {
        estado.confirmarJugadores(this);
    }

    public void iniciar() {
        estado.iniciar(this);
    }

    public void finalizar() {
        estado.finalizar(this);
    }

    public void cancelar() {
        estado.cancelar(this);
    }

    public void registrarBaja() {
        String estadoActual = getEstadoActual();

        if (!estadoActual.equals("Buscando jugadores")
                && !estadoActual.equals("Lobby armado")
                && !estadoActual.equals("Confirmado")) {
            throw new IllegalStateException(
                    "No se pueden registrar bajas en este estado"
            );
        }

        // vuelve a buscar jugadores si se libera un cupo
        if (!estadoActual.equals("Buscando jugadores")) {
            setEstado(new BuscandoState());
        }
    }
}
