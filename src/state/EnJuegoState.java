
package state;

public class EnJuegoState implements ScrimState {

    @Override
    public void completarCupos(ScrimContext context) {
        throw new IllegalStateException(
                "La partida ya comenzo"
        );
    }

    @Override
    public void confirmarJugadores(ScrimContext context) {
        throw new IllegalStateException(
                "No se pueden confirmar jugadores durante la partida"
        );
    }

    @Override
    public void iniciar(ScrimContext context) {
        throw new IllegalStateException(
                "La partida ya esta en juego"
        );
    }

    @Override
    public void finalizar(ScrimContext context) {
        context.setEstado(new FinalizadoState());
    }

    @Override
    public void cancelar(ScrimContext context) {
        throw new IllegalStateException(
                "No se puede cancelar una partida que ya comenzo"
        );
    }

    @Override
    public String getNombre() {
        return "En juego";
    }
}
