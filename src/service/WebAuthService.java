
package service;

import domain.Usuario;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class WebAuthService {

    public record Sesion(Usuario usuario, String rol) {}

    public record Acceso(String token, Sesion sesion) {}

    private static class Cuenta {
        private final Usuario usuario;
        private final String rol;

        private Cuenta(Usuario usuario, String rol) {
            this.usuario = usuario;
            this.rol = rol;
        }
    }

    private record SesionGuardada(
            Cuenta cuenta,
            Instant vencimiento
    ) {}

    private final Map<String, Cuenta> cuentas =
            new ConcurrentHashMap<>();

    private final Map<String, SesionGuardada> sesiones =
            new ConcurrentHashMap<>();

    private final AtomicInteger siguienteUsuario =
            new AtomicInteger(2000);

    private final SecureRandom random = new SecureRandom();

    private static final int ITERACIONES = 210000;
    private static final long DURACION_HORAS = 8;

    public WebAuthService() {
        byte[] clave = new byte[18];
        random.nextBytes(clave);

        String contrasena = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(clave);

        crearCuenta(
                "OrganizadorDemo",
                "organizador@escrims.local",
                contrasena,
                "LAS",
                5,
                "ORGANIZADOR"
        );

        System.out.println("Cuenta de organizador local:");
        System.out.println("Email: organizador@escrims.local");
        System.out.println("Contraseña: " + contrasena);
    }

    public synchronized Acceso registrar(
            String nombre,
            String email,
            String contrasena,
            String region,
            int rango
    ) {
        validar(nombre, email, contrasena, region, rango);

        String clave = email.trim().toLowerCase();

        if (cuentas.containsKey(clave)) {
            throw new IllegalArgumentException(
                    "Ya existe una cuenta con ese email"
            );
        }

        Cuenta cuenta = crearCuenta(
                nombre.trim(),
                clave,
                contrasena,
                region,
                rango,
                "JUGADOR"
        );

        return crearSesion(cuenta);
    }

    public Acceso iniciarSesion(
            String email,
            String contrasena
    ) {
        if (email == null || contrasena == null) {
            throw new IllegalArgumentException(
                    "Email o contraseña incorrectos"
            );
        }

        Cuenta cuenta = cuentas.get(
                email.trim().toLowerCase()
        );

        if (cuenta == null ||
                !comprobarContrasena(
                        contrasena,
                        cuenta.usuario.getPasswordHash()
                )) {
            throw new IllegalArgumentException(
                    "Email o contraseña incorrectos"
            );
        }

        return crearSesion(cuenta);
    }

    public Sesion obtenerSesion(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }

        SesionGuardada guardada = sesiones.get(token);

        if (guardada == null) {
            return null;
        }

        if (Instant.now().isAfter(guardada.vencimiento())) {
            sesiones.remove(token);
            return null;
        }

        return new Sesion(
                guardada.cuenta().usuario,
                guardada.cuenta().rol
        );
    }

    public void cerrarSesion(String token) {
        if (token != null) {
            sesiones.remove(token);
        }
    }

    private Cuenta crearCuenta(
            String nombre,
            String email,
            String contrasena,
            String region,
            int rango,
            String rol
    ) {
        String hash = generarHash(contrasena);

        Usuario usuario = new Usuario(
                siguienteUsuario.getAndIncrement(),
                nombre,
                email,
                hash,
                region,
                rango
        );

        Cuenta cuenta = new Cuenta(usuario, rol);

        cuentas.put(email.toLowerCase(), cuenta);

        return cuenta;
    }

    private Acceso crearSesion(Cuenta cuenta) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);

        String token = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);

        sesiones.put(
                token,
                new SesionGuardada(
                        cuenta,
                        Instant.now().plusSeconds(
                                DURACION_HORAS * 3600
                        )
                )
        );

        return new Acceso(
                token,
                new Sesion(cuenta.usuario, cuenta.rol)
        );
    }

    private void validar(
            String nombre,
            String email,
            String contrasena,
            String region,
            int rango
    ) {
        if (nombre == null ||
                !nombre.trim().matches("[a-zA-Z0-9_]{3,24}")) {
            throw new IllegalArgumentException(
                    "El usuario debe tener entre 3 y 24 caracteres: letras, números o guion bajo"
            );
        }

        if (email == null ||
                !email.trim().matches(
                        "[^\\s@]+@[^\\s@]+\\.[^\\s@]+"
                ) ||
                email.length() > 150) {
            throw new IllegalArgumentException(
                    "El email no es válido"
            );
        }

        if (contrasena == null ||
                contrasena.length() < 8 ||
                contrasena.length() > 128) {
            throw new IllegalArgumentException(
                    "La contraseña debe tener entre 8 y 128 caracteres"
            );
        }

        if (region == null ||
                !region.matches("LAS|LAN|BR")) {
            throw new IllegalArgumentException(
                    "Seleccioná una región válida"
            );
        }

        if (rango < 1 || rango > 10) {
            throw new IllegalArgumentException(
                    "El rango debe estar entre 1 y 10"
            );
        }
    }

    private String generarHash(String contrasena) {
        byte[] sal = new byte[16];
        random.nextBytes(sal);

        byte[] hash = derivar(contrasena, sal, ITERACIONES);

        return "pbkdf2-sha256:" + ITERACIONES + ":"
                + Base64.getEncoder().encodeToString(sal)
                + ":"
                + Base64.getEncoder().encodeToString(hash);
    }

    private boolean comprobarContrasena(
            String contrasena,
            String almacenado
    ) {
        try {
            String[] partes = almacenado.split(":");

            if (partes.length != 4 ||
                    !partes[0].equals("pbkdf2-sha256")) {
                return false;
            }

            int iteraciones = Integer.parseInt(partes[1]);

            byte[] sal = Base64.getDecoder()
                    .decode(partes[2]);

            byte[] esperado = Base64.getDecoder()
                    .decode(partes[3]);

            byte[] calculado = derivar(
                    contrasena, sal, iteraciones
            );

            return MessageDigest.isEqual(
                    esperado, calculado
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
        char[] caracteres = contrasena.toCharArray();

        PBEKeySpec especificacion = new PBEKeySpec(
                caracteres, sal, iteraciones, 256
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
                    "No se pudo proteger la contraseña",
                    e
            );

        } finally {
            especificacion.clearPassword();
            java.util.Arrays.fill(
                    caracteres, '\0'
            );
        }
    }
}
