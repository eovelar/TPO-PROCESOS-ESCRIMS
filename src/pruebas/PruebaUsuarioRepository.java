
package pruebas;

import domain.Usuario;
import persistence.UsuarioRepository;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Base64;

public class PruebaUsuarioRepository {

    private static final String EMAIL =
            "persistencia.test@escrims.local";

    public static void main(String[] args) {

        UsuarioRepository repositorio =
                new UsuarioRepository();

        try {
            UsuarioRepository.UsuarioGuardado registro =
                    repositorio.buscarPorEmail(EMAIL);

            if (registro == null) {

                System.out.println(
                        "El usuario no existe. Creando cuenta de prueba..."
                );

                String hash = generarHashPrueba();

                registro = repositorio.guardar(
                        "PersistenciaTest",
                        EMAIL,
                        hash,
                        "LAS",
                        5,
                        "JUGADOR"
                );

                System.out.println(
                        "Usuario guardado en Supabase: OK"
                );

            } else {
                System.out.println(
                        "Usuario encontrado en Supabase: OK"
                );
            }

            Usuario usuario = registro.usuario();

            verificar(
                    usuario.getId() > 0,
                    "El usuario debe tener un ID"
            );

            verificar(
                    usuario.getEmail().equals(EMAIL),
                    "El email debe coincidir"
            );

            verificar(
                    usuario.getUsername().equals("PersistenciaTest"),
                    "El nombre debe coincidir"
            );

            verificar(
                    usuario.getRegion().equals("LAS"),
                    "La region debe coincidir"
            );

            verificar(
                    registro.rango() == 5,
                    "El rango debe ser 5"
            );

            verificar(
                    registro.rol().equals("JUGADOR"),
                    "El rol debe ser JUGADOR"
            );

            UsuarioRepository.UsuarioGuardado recuperado =
                    repositorio.buscarPorId(usuario.getId());

            verificar(
                    recuperado != null,
                    "El usuario debe poder recuperarse por ID"
            );

            verificar(
                    recuperado.usuario().getId() == usuario.getId(),
                    "El ID recuperado debe coincidir"
            );

            System.out.println();
            System.out.println("ID: " + usuario.getId());
            System.out.println("Usuario: " + usuario.getUsername());
            System.out.println("Email: " + usuario.getEmail());
            System.out.println("Region: " + usuario.getRegion());
            System.out.println("Rango: " + registro.rango());
            System.out.println("Rol: " + registro.rol());

            System.out.println(
                    "\nTodas las pruebas de persistencia pasaron correctamente"
            );

        } catch (SQLException e) {

            System.err.println(
                    "Error de base de datos: " + e.getMessage()
            );

            System.exit(1);

        } catch (Exception e) {

            System.err.println(
                    "Error: " + e.getMessage()
            );

            System.exit(1);
        }
    }

    private static String generarHashPrueba() throws Exception {

        SecureRandom random = new SecureRandom();
        byte[] sal = new byte[16];

        random.nextBytes(sal);

        char[] contrasena =
                "ClaveTemporalDePrueba123!".toCharArray();

        int iteraciones = 210000;

        PBEKeySpec especificacion = new PBEKeySpec(
                contrasena,
                sal,
                iteraciones,
                256
        );

        try {
            SecretKeyFactory fabrica =
                    SecretKeyFactory.getInstance(
                            "PBKDF2WithHmacSHA256"
                    );

            byte[] hash = fabrica.generateSecret(
                    especificacion
            ).getEncoded();

            return "pbkdf2-sha256:"
                    + iteraciones + ":"
                    + Base64.getEncoder().encodeToString(sal)
                    + ":"
                    + Base64.getEncoder().encodeToString(hash);

        } finally {
            especificacion.clearPassword();
            Arrays.fill(contrasena, '\0');
        }
    }

    private static void verificar(
            boolean condicion,
            String mensaje
    ) {

        if (!condicion) {
            throw new AssertionError(mensaje);
        }
    }
}
