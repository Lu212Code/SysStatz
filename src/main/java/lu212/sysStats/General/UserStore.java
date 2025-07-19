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
                    String[] parts = line.split("\\|");
                    if (parts.length == 3) {
                        String username = parts[0];
                        String password = parts[1];
                        boolean isAdmin = Boolean.parseBoolean(parts[2]);
                        users.add(new User(username, password, isAdmin));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            // Wenn Datei fehlt: Admin als Standarduser anlegen
            users.add(new User("admin", "geheim123", true));
            saveUsers();
        }
    }

    public static void saveUsers() {
        try (BufferedWriter writer = Files.newBufferedWriter(USER_FILE)) {
            for (User u : users) {
                writer.write(u.getUsername() + "|" + u.getPassword() + "|" + u.isAdmin() + "\n");
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
}
