package lu212.sysStats.General;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LogManager {

    private final String logfile;

    public LogManager(String logfile) {
        this.logfile = logfile;
    }

    public void initialize() {
        File file = new File(logfile);

        try {
            // Ordner anlegen, falls nicht vorhanden
            File parent = file.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            // Datei anlegen, falls nicht vorhanden
            if (file.createNewFile()) {
                writeLine("SysStatz - Logfile | Created: " + getAktuellesDatumMitUhrzeit());
            } else {
                // Prüfen ob Datei leer ist, dann Kopfzeile schreiben
                try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                    if (br.readLine() == null) {
                        writeLine("SysStatz - Logfile | Created: " + getAktuellesDatumMitUhrzeit());
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void info(String text) {
        writeLine(getAktuellesDatumMitUhrzeit() + " [INFO]: " + text);
    }

    public void error(String text) {
        writeLine(getAktuellesDatumMitUhrzeit() + " [ERROR]: " + text);
    }

    public void warning(String text) {
        writeLine(getAktuellesDatumMitUhrzeit() + " [WARNING]: " + text);
    }

    private void writeLine(String text) {
        File file = new File(logfile);
        File parent = file.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        try (BufferedWriter bwr = new BufferedWriter(new FileWriter(file, true))) { // true = append
            bwr.write(text);
            bwr.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getAktuellesDatumMitUhrzeit() {
        LocalDateTime jetzt = LocalDateTime.now();
        // für Dateinamen: kein ":" oder "|" verwenden
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy_HH-mm");
        return jetzt.format(formatter);
    }
}
