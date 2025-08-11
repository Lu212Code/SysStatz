package lu212.sysStats.General;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class AlertUtil {

    private static final Path ALERTS_FILE = Paths.get("alerts.txt");

    public enum Level { RED, YELLOW }

    /**
     * Fügt einen neuen Alert zur Datei alerts.txt hinzu.
     * @param text Der Text des Alerts (Zeilenumbrüche und Semikolons werden bereinigt)
     * @param level Die Wichtigkeit (RED oder YELLOW)
     * @throws IOException bei Schreibfehlern
     */
    public static void addAlert(String text, Level level) throws IOException {
        // Bereinige Text (keine Zeilenumbrüche, keine Semikolons, die wir als Trenner nutzen)
        String safeText = text.replace("\n", " ").replace(";", ",");
        String line = level.name() + ";" + safeText;
        // Schreibe als neue Zeile an die Datei an (erstellt sie bei Bedarf)
        Files.write(ALERTS_FILE, Collections.singleton(line), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
}
