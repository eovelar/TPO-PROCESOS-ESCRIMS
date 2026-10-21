
package state;

public class FinalizadoState implements ScrimState {

    @Override
    public void completarCupos(ScrimContext context) {
        throw new IllegalStateException(
                "La partida ya finalizo"
        );
    }

    @Override
    public void confirmarJugadores(ScrimContext context) {
        throw new IllegalStateException(
                "No se puede confirmar una partida finalizada"
        );
    }

    @Override
    public void iniciar(ScrimContext context) {
        throw new IllegalStateException(
                "No se puede iniciar una partida finalizada"
        );
    }

    @Override
    public void finalizar(ScrimContext context) {
        throw new IllegalStateException(
                "La partida ya fue finalizada"
        );
    }

    @Override
    public void cancelar(ScrimContext context) {
        throw new IllegalStateException(
                "No se puede cancelar una partida finalizada"
        );
    }

    @Override
    public String getNombre() {
        return "Finalizado";
    }
}
