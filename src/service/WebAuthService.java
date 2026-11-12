
package service;

import domain.Usuario;
import persistence.UsuarioRepository;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WebAuthService {

    public record Sesion(Usuario usuario, String rol) {}

    public record Acceso(String token, Sesion sesion) {}

    private record SesionGuardada(
            Sesion sesion,
            Instant vencimiento
    ) {}

    private final UsuarioRepository usuarioRepository =
            new UsuarioRepository();

    private final Map<String, SesionGuardada> sesiones =
            new ConcurrentHashMap<>();

    private final SecureRandom random = new SecureRandom();

    private static final int ITERACIONES = 210000;
    private static final long DURACION_HORAS = 8;

    private final Usuario organizadorDemo;
    private final Usuario organizadorProduccion;
    private final String emailOrganizadorProduccion;

    public WebAuthService() {

        boolean produccion = System.getenv("PORT") != null;

        if (produccion) {
            organizadorDemo = null;

            String email = System.getenv("ADMIN_EMAIL");
            String contrasena = System.getenv("ADMIN_PASSWORD");

            if (email != null && contrasena != null
                    && !email.isBlank()
                    && !contrasena.isBlank()) {

                email = email.trim().toLowerCase();

                if (!email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
                    throw new IllegalStateException(
                            "El email del organizador no es valido"
                    );
                }

                if (contrasena.length() < 16) {
                    throw new IllegalStateException(
                            "La contrasena del organizador debe tener al menos 16 caracteres"
                    );
                }

                emailOrganizadorProduccion = email;

                organizadorProduccion = new Usuario(
                        999999,
                        "Organizador",
                        email,
                        generarHash(contrasena),
                        "LAS",
                        5
                );

                System.out.println(
                        "Cuenta de organizador habilitada"
                );

            } else {
                emailOrganizadorProduccion = null;
                organizadorProduccion = null;

                System.out.println(
                        "Organizador no configurado en produccion"
                );
            }

        } else {
            emailOrganizadorProduccion = null;
            organizadorProduccion = null;

            String contrasenaDemo = generarContrasenaDemo();

            organizadorDemo = new Usuario(
                    999999,
                    "OrganizadorDemo",
                    "organizador@escrims.local",
                    generarHash(contrasenaDemo),
                    "LAS",
                    5
            );

            System.out.println("Cuenta de organizador local:");
            System.out.println("Email: organizador@escrims.local");
            System.out.println("Contrasena: " + contrasenaDemo);
        }
    }

    public synchronized Acceso registrar(
            String nombre,
            String email,
            String contrasena,
            String region,
            int rango
    ) {

        validar(nombre, email, contrasena, region, rango);

        String emailNormalizado =
                email.trim().toLowerCase();

        if (emailNormalizado.equals("organizador@escrims.local")
                || emailNormalizado.equals(emailOrganizadorProduccion)) {

            throw new IllegalArgumentException(
                    "Ese email esta reservado"
            );
        }

        try {

            if (usuarioRepository.existeEmail(emailNormalizado)) {
                throw new IllegalArgumentException(
                        "Ya existe una cuenta con ese email"
                );
            }

            String hash = generarHash(contrasena);

            UsuarioRepository.UsuarioGuardado registro =
                    usuarioRepository.guardar(
                            nombre.trim(),
                            emailNormalizado,
                            hash,
                            region,
                            rango,
                            "JUGADOR"
                    );

            return crearSesion(
                    registro.usuario(),
                    registro.rol()
            );

        } catch (SQLException e) {

            if ("23505".equals(e.getSQLState())) {
                throw new IllegalArgumentException(
                        "El email o nombre de usuario ya esta registrado"
                );
            }

            throw new IllegalStateException(
                    "No se pudo guardar la cuenta en la base de datos",
                    e
            );
        }
    }

    public Acceso iniciarSesion(
            String email,
            String contrasena
    ) {

        if (email == null || contrasena == null) {
            throw new IllegalArgumentException(
                    "Email o contrasena incorrectos"
            );
        }

        String emailNormalizado =
                email.trim().toLowerCase();

        if (emailNormalizado.equals("organizador@escrims.local")) {

            if (organizadorDemo == null ||
                    !comprobarContrasena(
                            contrasena,
                            organizadorDemo.getPasswordHash()
                    )) {

                throw new IllegalArgumentException(
                        "Email o contrasena incorrectos"
                );
            }

            return crearSesion(
                    organizadorDemo,
                    "ORGANIZADOR"
            );
        }

        if (emailNormalizado.equals(emailOrganizadorProduccion)) {

            if (organizadorProduccion == null ||
                    !comprobarContrasena(
                            contrasena,
                            organizadorProduccion.getPasswordHash()
                    )) {

                throw new IllegalArgumentException(
                        "Email o contrasena incorrectos"
                );
            }

            return crearSesion(
                    organizadorProduccion,
                    "ORGANIZADOR"
            );
        }

        try {

            UsuarioRepository.UsuarioGuardado registro =
                    usuarioRepository.buscarPorEmail(
                            emailNormalizado
                    );

            if (registro == null) {
                throw new IllegalArgumentException(
                        "Email o contrasena incorrectos"
                );
            }

            Usuario usuario = registro.usuario();

            if (!comprobarContrasena(
                    contrasena,
                    usuario.getPasswordHash()
            )) {

                throw new IllegalArgumentException(
                        "Email o contrasena incorrectos"
                );
            }

            return crearSesion(
                    usuario,
                    registro.rol()
            );

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudo consultar la base de datos",
                    e
            );
        }
    }

    public Sesion obtenerSesion(String token) {

        if (token == null || token.isBlank()) {
            return null;
        }

        SesionGuardada guardada = sesiones.get(token);

        if (guardada == null) {
            return null;
        }

        if (!Instant.now().isBefore(
                guardada.vencimiento()
        )) {

            sesiones.remove(token);
            return null;
        }

        return guardada.sesion();
    }

    public void cerrarSesion(String token) {

        if (token != null) {
            sesiones.remove(token);
        }
    }

    private Acceso crearSesion(
            Usuario usuario,
            String rol
    ) {

        byte[] bytes = new byte[32];
        random.nextBytes(bytes);

        String token = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);

        Sesion sesion = new Sesion(usuario, rol);

        sesiones.put(
                token,
                new SesionGuardada(
                        sesion,
                        Instant.now().plusSeconds(
                                DURACION_HORAS * 3600
                        )
                )
        );

        return new Acceso(token, sesion);
    }

    private void validar(
            String nombre,
            String email,
            String contrasena,
            String region,
            int rango
    ) {

        if (nombre == null ||
                !nombre.trim().matches(
                        "[a-zA-Z0-9_]{3,24}"
                )) {

            throw new IllegalArgumentException(
                    "El usuario debe tener entre 3 y 24 caracteres"
            );
        }

        if (email == null ||
                email.length() > 150 ||
                !email.trim().matches(
                        "[^\\s@]+@[^\\s@]+\\.[^\\s@]+"
                )) {

            throw new IllegalArgumentException(
                    "El email no es valido"
            );
        }

        if (contrasena == null ||
                contrasena.length() < 8 ||
                contrasena.length() > 128) {

            throw new IllegalArgumentException(
                    "La contrasena debe tener entre 8 y 128 caracteres"
            );
        }

        if (region == null ||
                !region.matches("LAS|LAN|BR")) {

            throw new IllegalArgumentException(
                    "Selecciona una region valida"
            );
        }

        if (rango < 1 || rango > 10) {

            throw new IllegalArgumentException(
                    "El rango debe estar entre 1 y 10"
            );
        }
    }

    private String generarContrasenaDemo() {

        byte[] bytes = new byte[18];
        random.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    private String generarHash(String contrasena) {

        byte[] sal = new byte[16];
        random.nextBytes(sal);

        byte[] hash = derivar(
                contrasena,
                sal,
                ITERACIONES
        );

        return "pbkdf2-sha256:"
                + ITERACIONES + ":"
                + Base64.getEncoder().encodeToString(sal)
                + ":"
                + Base64.getEncoder().encodeToString(hash);
    }

    private boolean comprobarContrasena(
            String contrasena,
            String almacenado
    ) {

        if (almacenado == null) {
            return false;
        }

        try {

            String[] partes = almacenado.split(":");

            if (partes.length != 4 ||
                    !partes[0].equals("pbkdf2-sha256")) {
                return false;
            }

            int iteraciones =
                    Integer.parseInt(partes[1]);

            if (iteraciones < 100000 ||
                    iteraciones > 1000000) {
                return false;
            }

            byte[] sal = Base64.getDecoder()
                    .decode(partes[2]);

            byte[] hashEsperado = Base64.getDecoder()
                    .decode(partes[3]);

            byte[] hashCalculado = derivar(
                    contrasena,
                    sal,
                    iteraciones
            );

            return MessageDigest.isEqual(
                    hashEsperado,
                    hashCalculado
            );

        } catch (RuntimeException e) {
            return false;
        }
    }

    private byte[] derivar(
            String contrasena,
            byte[] sal,
            int iteraciones
    ) {

        char[] caracteres =
                contrasena.toCharArray();

        PBEKeySpec especificacion = new PBEKeySpec(
                caracteres,
                sal,
                iteraciones,
                256
        );

        try {

            SecretKeyFactory fabrica =
                    SecretKeyFactory.getInstance(
                            "PBKDF2WithHmacSHA256"
                    );

            return fabrica.generateSecret(
                    especificacion
            ).getEncoded();

        } catch (Exception e) {

            throw new IllegalStateException(
                    "No se pudo procesar la contraseña",
                    e
            );

        } finally {

            especificacion.clearPassword();
            Arrays.fill(caracteres, '\0');
        }
    }
}
