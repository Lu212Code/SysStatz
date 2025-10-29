package lu212.sysstatz.General;

import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;

import lu212.sysstatz.SysStats_Web.ConfigManager;

public class commandLine {

    public static void startCli() {
        System.out.println("===== SysStatz CLI =====");
        System.out.println("Befehle:");
        System.out.println("  adduser <name> <passwort> [admin]");
        System.out.println("  deluser <name>");
        System.out.println("  listusers");
        System.out.println("  reset2fa <name>");
        System.out.println("  showconfig");
        System.out.println("  setconfig <key> <value>");
        System.out.println("  exit");
        System.out.println("========================");

        Scanner scanner = new Scanner(System.in);
        ConfigManager cfgManager = new ConfigManager();

        while (true) {
            System.out.print("> ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) continue;

            String[] parts = input.split("\\s+");
            String cmd = parts[0].toLowerCase();

            switch (cmd) {
                case "exit" -> {
                    System.out.println("CLI beendet.");
                    scanner.close();
                    return;
                }

                case "adduser" -> {
                    if (parts.length < 3) {
                        System.out.println("Syntax: adduser <name> <passwort> [admin]");
                        break;
                    }
                    String username = parts[1];
                    String password = parts[2];
                    boolean isAdmin = parts.length >= 4 && parts[3].equalsIgnoreCase("admin");

                    if (UserStore.getUserByName(username) != null) {
                        System.out.println("⚠ Benutzer existiert bereits: " + username);
                        break;
                    }

                    User newUser = new User(username, password, isAdmin);
                    UserStore.addUser(newUser);
                    System.out.println("✅ Benutzer hinzugefügt: " + username +
                                       (isAdmin ? " (Admin)" : ""));
                }

                case "deluser" -> {
                    if (parts.length != 2) {
                        System.out.println("Syntax: deluser <name>");
                        break;
                    }
                    String delName = parts[1];
                    if (UserStore.getUserByName(delName) == null) {
                        System.out.println("⚠ Benutzer nicht gefunden: " + delName);
                        break;
                    }
                    UserStore.deleteUser(delName);
                    System.out.println("🗑 Benutzer gelöscht: " + delName);
                }

                case "listusers" -> {
                    List<User> users = UserStore.getAll();
                    if (users.isEmpty()) {
                        System.out.println("Keine Benutzer vorhanden.");
                    } else {
                        System.out.println("Benutzerliste:");
                        for (User u : users) {
                            System.out.println(" - " + u.getUsername() +
                                               (u.isAdmin() ? " (Admin)" : "") +
                                               (u.isTwoFactorEnabled() ? " [2FA]" : ""));
                        }
                    }
                }

                case "reset2fa" -> {
                    if (parts.length != 2) {
                        System.out.println("Syntax: reset2fa <name>");
                        break;
                    }
                    String userName = parts[1];
                    User u = UserStore.getUserByName(userName);
                    if (u == null) {
                        System.out.println("⚠ Benutzer nicht gefunden: " + userName);
                        break;
                    }
                    u.setTwoFactorEnabled(false);
                    u.setTwoFactorSecret(null);
                    UserStore.saveUsers();
                    System.out.println("🔑 2FA für Benutzer '" + userName + "' zurückgesetzt.");
                }

                case "showconfig" -> {
                    System.out.println("Aktuelle Konfiguration:");
                    System.out.println(" webPort=" + cfgManager.getWebServerPort());
                    System.out.println(" statsPort=" + cfgManager.getStatsServerPort());
                    System.out.println(" theme=" + cfgManager.getTheme());
                    System.out.println(" ollamaserverip=" + cfgManager.getOllamaServerIP());
                    System.out.println(" twoFactorRequired=" + cfgManager.getTwoFactor());
                    System.out.println(" clientPassword=" + cfgManager.getClientPassword());
                }

                case "setconfig" -> {
                    if (parts.length < 3) {
                        System.out.println("Syntax: setconfig <key> <value>");
                        break;
                    }
                    String key = parts[1];
                    String value = parts[2];
                    try {
                        Path path = java.nio.file.Paths.get("config.txt");
                        List<String> lines = java.nio.file.Files.readAllLines(path);
                        boolean found = false;

                        for (int i = 0; i < lines.size(); i++) {
                            if (lines.get(i).startsWith(key + "=")) {
                                lines.set(i, key + "=" + value);
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            lines.add(key + "=" + value);
                        }

                        java.nio.file.Files.write(path, lines);
                        System.out.println("✅ Config-Eintrag aktualisiert: " + key + "=" + value);
                    } catch (Exception e) {
                        System.out.println("⚠ Fehler beim Speichern der Config: " + e.getMessage());
                    }
                }

                default -> {
                    System.out.println("Unbekannter Befehl: " + cmd);
                    System.out.println("Befehle: adduser, deluser, listusers, reset2fa, showconfig, setconfig, exit");
                }
            }
        }
    }
}
