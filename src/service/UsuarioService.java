
package service;

import domain.Usuario;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class UsuarioService {

    private static final int ITERACIONES = 600000;
    private static final int LONGITUD_CLAVE = 256;
    private static final int LONGITUD_SAL = 16;

    private final List<Usuario> usuarios;
    private final SecureRandom random;

    public UsuarioService() {
        this.usuarios = new ArrayList<>();
        this.random = new SecureRandom();
    }

    public Usuario registrarUsuario(int id, String username, String email,
                                    String password, String region, int rango) {

        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre de usuario es obligatorio"
            );
        }

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                    "El email es obligatorio"
            );
        }

        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException(
                    "La contraseña debe tener al menos 8 caracteres"
            );
        }

        for (Usuario usuario : usuarios) {

            if (usuario.getId() == id) {
                throw new IllegalArgumentException(
                        "Ya existe un usuario con ese ID"
                );
            }

            if (usuario.getUsername().equalsIgnoreCase(username.trim())) {
                throw new IllegalArgumentException(
                        "El nombre de usuario ya existe"
                );
            }

            if (usuario.getEmail().equalsIgnoreCase(email.trim())) {
                throw new IllegalArgumentException(
                        "El email ya esta registrado"
                );
            }
        }

        String passwordHash = generarHash(password);

        Usuario usuario = new Usuario(
                id, username, email, passwordHash, region, rango
        );

        usuarios.add(usuario);

        return usuario;
    }

    public Usuario iniciarSesion(String email, String password) {

        if (email == null || password == null) {
            throw new IllegalArgumentException(
                    "El email y la contraseña son obligatorios"
            );
        }

        for (Usuario usuario : usuarios) {

            if (usuario.getEmail().equalsIgnoreCase(email.trim())) {

                if (verificarPassword(password, usuario.getPasswordHash())) {
                    return usuario;
                }

                throw new IllegalArgumentException(
                        "Credenciales incorrectas"
                );
            }
        }

        throw new IllegalArgumentException(
                "Credenciales incorrectas"
        );
    }

    public Usuario buscarPorId(int id) {

        for (Usuario usuario : usuarios) {
            if (usuario.getId() == id) {
                return usuario;
            }
        }

        return null;
    }

    public List<Usuario> listarUsuarios() {
        return new ArrayList<>(usuarios);
    }

    private String generarHash(String password) {

        // genera una sal distinta para cada contraseña
        byte[] sal = new byte[LONGITUD_SAL];
        random.nextBytes(sal);

        byte[] hash = calcularHash(password, sal, ITERACIONES);

        return "pbkdf2-sha256:"
                + ITERACIONES + ":"
                + Base64.getEncoder().encodeToString(sal) + ":"
                + Base64.getEncoder().encodeToString(hash);
    }

    private boolean verificarPassword(String password, String passwordHash) {

        try {
            String[] partes = passwordHash.split(":");

            if (partes.length != 4 || !partes[0].equals("pbkdf2-sha256")) {
                return false;
            }

            int iteraciones = Integer.parseInt(partes[1]);

            if (iteraciones < 1 || iteraciones > ITERACIONES) {
                return false;
            }

            byte[] sal = Base64.getDecoder().decode(partes[2]);
            byte[] hashGuardado = Base64.getDecoder().decode(partes[3]);

            if (sal.length != LONGITUD_SAL
                    || hashGuardado.length != LONGITUD_CLAVE / 8) {
                return false;
            }

            byte[] hashCalculado = calcularHash(
                    password, sal, iteraciones
            );

            // compara los hashes sin comparar contraseñas en texto plano
            return MessageDigest.isEqual(
                    hashGuardado, hashCalculado
            );

        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private byte[] calcularHash(String password, byte[] sal, int iteraciones) {

        PBEKeySpec especificacion = new PBEKeySpec(
                password.toCharArray(),
                sal,
                iteraciones,
                LONGITUD_CLAVE
        );

        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(
                    "PBKDF2WithHmacSHA256"
            );

            return factory.generateSecret(especificacion).getEncoded();

        } catch (Exception e) {
            throw new IllegalStateException(
                    "No se pudo procesar la contraseña", e
            );

        } finally {
            // limpia la contraseña de la estructura temporal
            especificacion.clearPassword();
        }
    }
}
