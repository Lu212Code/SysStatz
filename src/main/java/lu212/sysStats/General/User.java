package lu212.sysStats.General;

public class User {
    private String username;
    private String password;
    private boolean isAdmin;
    
    private boolean twoFactorEnabled;
    private String twoFactorSecret; // Base32-Secret für TOTP

    public User(String username, String password, boolean isAdmin) {
        this.username = username;
        this.password = password;
        this.isAdmin = isAdmin;
        this.twoFactorEnabled = false;
        this.twoFactorSecret = null;
    }

    // Getter & Setter
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public boolean isAdmin() { return isAdmin; }
    public void setAdmin(boolean admin) { isAdmin = admin; }
    
    // Getter/Setter
    public boolean isTwoFactorEnabled() { return twoFactorEnabled; }
    public void setTwoFactorEnabled(boolean enabled) { this.twoFactorEnabled = enabled; }

    public String getTwoFactorSecret() { return twoFactorSecret; }
    public void setTwoFactorSecret(String secret) { this.twoFactorSecret = secret; }
}
