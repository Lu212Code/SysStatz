package lu212.sysStats.General;

import com.google.gson.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;

public class UpdateService {

    private static final String VERSION_URL = "https://example.com/sysstatz/update.json";
    private static final String CURRENT_VERSION = "1.0.0"; // Deine aktuelle Version

	private static String latestVersion;
    private static String downloadUrl;

    public static void init() {
        try {
            checkForUpdates();
            if (isUpdateAvailable()) {
                System.out.println("Neue Version verfügbar: " + latestVersion);
            } else {
                System.out.println("SysStatz ist aktuell (" + CURRENT_VERSION + ")");
            }
        } catch (Exception e) {
            System.err.println("Update-Check fehlgeschlagen: " + e.getMessage());
        }
    }

    public static void checkForUpdates() throws IOException {
        String json = new String(new URL(VERSION_URL).openStream().readAllBytes());
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();

        latestVersion = obj.get("version").getAsString();
        downloadUrl = obj.get("downloadUrl").getAsString();
    }

    public static boolean isUpdateAvailable() {
        return !CURRENT_VERSION.equals(latestVersion);
    }

    public static void performUpdate() {
        if (!isUpdateAvailable()) {
            System.out.println("Keine Updates verfügbar.");
            return;
        }

        try {
            System.out.println("Lade neue Version " + latestVersion + " herunter...");

            File currentJar = new File(UpdateService.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());

            if (!currentJar.getName().endsWith(".jar")) {
                System.err.println("Nicht als JAR gestartet – Auto-Update nicht möglich.");
                return;
            }

            // Neue Datei ins gleiche Verzeichnis laden
            Path currentDir = currentJar.getParentFile().toPath();
            Path newJar = currentDir.resolve("SysStatz_new.jar");
            try (InputStream in = new URL(downloadUrl).openStream()) {
                Files.copy(in, newJar, StandardCopyOption.REPLACE_EXISTING);
            }

            // Update-Skript erzeugen
            Path script;
            if (isWindows()) {
                script = currentDir.resolve("update_sysstatz.bat");
                Files.writeString(script, createWindowsScript(currentJar.getName()));
            } else {
                script = currentDir.resolve("update_sysstatz.sh");
                Files.writeString(script, createLinuxScript(currentJar.getName()));
                script.toFile().setExecutable(true);
            }

            // Skript starten
            new ProcessBuilder(script.toAbsolutePath().toString()).start();

            System.out.println("Update-Skript gestartet, SysStatz beendet sich...");
            System.exit(0);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Update fehlgeschlagen: " + e.getMessage());
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    private static String createWindowsScript(String oldJarName) {
        return "@echo off\n"
             + "echo Warte auf Beendigung...\n"
             + "ping 127.0.0.1 -n 5 > nul\n" // ca. 4 Sek warten
             + "del \"" + oldJarName + "\"\n"
             + "rename SysStatz_new.jar \"" + oldJarName + "\"\n"
             + "start java -jar \"" + oldJarName + "\"\n";
    }

    private static String createLinuxScript(String oldJarName) {
        return "#!/bin/bash\n"
             + "echo \"Warte auf Beendigung...\"\n"
             + "sleep 5\n"
             + "rm \"" + oldJarName + "\"\n"
             + "mv SysStatz_new.jar \"" + oldJarName + "\"\n"
             + "nohup java -jar \"" + oldJarName + "\" &\n";
    }
    
    public static String getLatestVersion() {
		return latestVersion;
	}

	public static String getCurrentVersion() {
		return CURRENT_VERSION;
	}
}
