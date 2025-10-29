package lu212.sysstatz.logging;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class LogManager {

    private final String logfile;

    public LogManager(String logfile) {
        this.logfile = logfile;
    }

    public void initialize() {
    	archiveOldLogs();
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
    
    private void archiveOldLogs() {
        File currentLogFile = new File(logfile);
        File logDir = currentLogFile.getParentFile();
        if (logDir == null || !logDir.exists()) return;

        File[] logFiles = logDir.listFiles((dir, name) -> name.endsWith(".log") || name.endsWith(".txt"));
        if (logFiles == null) return;

        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        Map<String, List<File>> logsByDate = new HashMap<>();

        for (File log : logFiles) {
            if (log.getName().contains(today)) continue;

            // Versuche Datum aus Dateiname zu extrahieren
            String datePart = extractDatePart(log.getName());
            if (datePart != null && !datePart.equals(today)) {
                logsByDate.computeIfAbsent(datePart, k -> new ArrayList<>()).add(log);
            }
        }

        for (Map.Entry<String, List<File>> entry : logsByDate.entrySet()) {
            String date = entry.getKey();
            List<File> files = entry.getValue();
            File zipFile = new File(logDir, "archiv_" + date + ".zip");

            try (FileOutputStream fos = new FileOutputStream(zipFile);
                 ZipOutputStream zos = new ZipOutputStream(fos)) {

                for (File log : files) {
                    try (FileInputStream fis = new FileInputStream(log)) {
                        ZipEntry zipEntry = new ZipEntry(log.getName());
                        zos.putNextEntry(zipEntry);

                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = fis.read(buffer)) > 0) {
                            zos.write(buffer, 0, length);
                        }
                        zos.closeEntry();
                    }
                    log.delete();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String extractDatePart(String filename) {
        // Erwartetes Format: log-dd-MM-yyyy_HH-mm.txt
        // Beispiel: log-26-07-2025_12-46.txt
        if (filename.startsWith("log-") && filename.contains("_")) {
            int start = "log-".length();
            int end = filename.indexOf("_");
            return filename.substring(start, end); // gibt z. B. "26-07-2025" zurück
        }
        return null;
    }
}
