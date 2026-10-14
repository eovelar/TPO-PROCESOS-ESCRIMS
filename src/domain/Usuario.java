
package domain;

public class Usuario {

    private int id;
    private String username;
    private String email;
    private String passwordHash;
    private String region;
    private int rango;

    public Usuario(int id, String username, String email,
                   String passwordHash, String region, int rango) {

        if (rango < 0) {
            throw new IllegalArgumentException("El rango no puede ser negativo");
        }

        this.id = id;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.region = region;
        this.rango = rango;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getRegion() {
        return region;
    }

    public int getRango() {
        return rango;
    }
}
