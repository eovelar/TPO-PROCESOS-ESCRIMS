
package state;

public class LobbyArmadoState implements ScrimState {

    @Override
    public void completarCupos(ScrimContext context) {
        throw new IllegalStateException(
                "El lobby ya tiene todos los cupos completos"
        );
    }

    @Override
    public void confirmarJugadores(ScrimContext context) {
        context.setEstado(new ConfirmadoState());
    }

    @Override
    public void iniciar(ScrimContext context) {
        throw new IllegalStateException(
                "No se puede iniciar sin confirmar a todos los jugadores"
        );
    }

    @Override
    public void finalizar(ScrimContext context) {
        throw new IllegalStateException(
                "No se puede finalizar una partida que no inicio"
        );
    }

    @Override
    public void cancelar(ScrimContext context) {
        context.setEstado(new CanceladoState());
    }

    @Override
    public String getNombre() {
        return "Lobby armado";
    }
}
