
package facade;

import matchmaking.Candidato;
import persistence.PostulacionRepository;
import persistence.ScrimRepository;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class EscrimsFacade {

    private final ScrimRepository scrimRepository;
    private final PostulacionRepository postulacionRepository;

    public EscrimsFacade() {
        this.scrimRepository = new ScrimRepository();
        this.postulacionRepository = new PostulacionRepository();
    }

    public List<ScrimRepository.ScrimGuardado> listarScrims()
            throws SQLException {

        return scrimRepository.listar();
    }

    public int crearScrim(
            Integer creadorId,
            String juego,
            String formato,
            String region,
            int rangoMin,
            int rangoMax,
            int latenciaMax,
            LocalDateTime fechaHora,
            int duracion,
            int cupos
    ) throws SQLException {

        return scrimRepository.crear(
                creadorId,
                juego,
                formato,
                region,
                rangoMin,
                rangoMax,
                latenciaMax,
                fechaHora,
                duracion,
                cupos
        );
    }

    public int postularJugador(
            int usuarioId,
            int scrimId,
            String rolDeseado
    ) throws SQLException {

        return postulacionRepository.guardar(
                usuarioId,
                scrimId,
                rolDeseado
        );
    }

    public List<PostulacionRepository.PostulacionGuardada>
    listarPostulacionesJugador(int usuarioId)
            throws SQLException {

        return postulacionRepository.listarPorUsuario(
                usuarioId
        );
    }

    public List<PostulacionRepository.PostulacionGuardada>
    listarPostulacionesOrganizador()
            throws SQLException {

        return postulacionRepository.listarTodas();
    }

    public String procesarPostulacion(
            int postulacionId,
            String accion
    ) throws SQLException {

        return postulacionRepository.procesar(
                postulacionId,
                accion
        );
    }

    // obtiene los candidatos recomendados para una partida
    public List<Candidato> buscarCandidatosCompatibles(
            int scrimId,
            int compatibilidadMinima
    ) throws SQLException {

        return postulacionRepository.buscarCandidatosCompatibles(
                scrimId,
                compatibilidadMinima
        );
    }
}
