package lu212.sysstatz.General;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

import lu212.sysstatz.SysStats_Web.AlertService;
import lu212.sysstatz.SysStats_Web.SysStatsWebApplication;

public class AlertUtil {

    private static final Path ALERTS_FILE = Paths.get("alerts.txt");
    private static final Set<String> existingAlerts = new HashSet<>();

    static {
        // Lade bestehende Alerts beim Start in das Set
        if (Files.exists(ALERTS_FILE)) {
            try {
                existingAlerts.addAll(Files.readAllLines(ALERTS_FILE));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public enum Level { RED, YELLOW }

    /**
     * Fügt einen neuen Alert hinzu, falls er noch nicht existiert.
     */
    public static void addAlert(String text, Level level) throws IOException {
        String safeText = text.replace("\n", " ").replace(";", ",");
        String newLine = level.name() + ";" + safeText;

        // Prüfe im Speicher-Set
        if (existingAlerts.contains(newLine)) {
            return; // Schon vorhanden → ignorieren
        }

        // Neuen Alert schreiben
        Files.write(ALERTS_FILE, Collections.singleton(newLine), StandardOpenOption.CREATE, StandardOpenOption.APPEND);

        // Alert auch ins Set einfügen
        existingAlerts.add(newLine);

        // Optional E-Mail versenden
        if (SysStatsWebApplication.alertMails) {
            AlertService.triggerEmailPHP(level, text);
        }
    }
}
