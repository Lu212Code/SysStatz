package lu212.sysStats.General;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class UserStore {
    private static final Path USER_FILE = Paths.get("users.txt");
    private static final List<User> users = new ArrayList<>();

    static {
        loadUsers(); // beim Start laden
    }

    public static void loadUsers() {
        users.clear();
        if (Files.exists(USER_FILE)) {
            try {
                List<String> lines = Files.readAllLines(USER_FILE);
                for (String line : lines) {
                    if (line.trim().isEmpty()) continue;
                    String[] parts = line.split("\\|");

                    // Akzeptiere auch alte Einträge mit 3 Feldern (kompatibel)
                    if (parts.length == 3 || parts.length == 5) {
                        String username = parts[0];
                        String password = parts[1];
                        boolean isAdmin = Boolean.parseBoolean(parts[2]);

                        User user = new User(username, password, isAdmin);

                        if (parts.length == 5) {
                            boolean twoFactorEnabled = Boolean.parseBoolean(parts[3]);
                            String twoFactorSecret = parts[4];

                            user.setTwoFactorEnabled(twoFactorEnabled);
                            user.setTwoFactorSecret(twoFactorSecret.isEmpty() ? null : twoFactorSecret);
                        }

                        users.add(user);
                    } else {
                        System.err.println("Ungültiger User-Eintrag in users.txt: " + line);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void saveUsers() {
        try (BufferedWriter writer = Files.newBufferedWriter(USER_FILE)) {
            for (User u : users) {
                if (u.isTwoFactorEnabled()) {
                    String twoFactorSecret = u.getTwoFactorSecret() == null ? "" : u.getTwoFactorSecret();
                    writer.write(u.getUsername() + "|" + u.getPassword() + "|" + u.isAdmin() + "|" + u.isTwoFactorEnabled() + "|" + twoFactorSecret + "\n");
                } else {
                    // kein 2FA, also nur 3 Felder schreiben
                    writer.write(u.getUsername() + "|" + u.getPassword() + "|" + u.isAdmin() + "\n");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<User> getAll() {
        return new ArrayList<>(users);
    }

    public static User getUserByName(String username) {
        return users.stream()
                .filter(u -> u.getUsername().equalsIgnoreCase(username))
                .findFirst()
                .orElse(null);
    }

    public static void addUser(User user) {
        users.add(user);
        saveUsers();
    }

    public static void deleteUser(String username) {
        users.removeIf(u -> u.getUsername().equalsIgnoreCase(username));
        saveUsers();
    }
    
    public static boolean hasUsers() {
        return !users.isEmpty();
    }
}
