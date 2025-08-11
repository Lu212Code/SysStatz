package lu212.sysStats.SysStats_Web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lu212.sysStats.General.Logger;
import lu212.sysStats.General.ThresholdConfig;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TriggerService {

    // Pro Servername eine Liste von Trigger-Konfigurationen
    private final Map<String, List<ThresholdConfig>> triggers = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final File file = new File("triggers.json");

    // Neuer Trigger wird angehängt
    public synchronized void saveTrigger(String serverName, ThresholdConfig config) {
        List<ThresholdConfig> list = triggers.computeIfAbsent(serverName, k -> new ArrayList<>());
        list.add(config);
        saveToFile();
    }

    // Alle Trigger für einen Server abrufen
    public Optional<List<ThresholdConfig>> getTrigger(String serverName) {
        return Optional.ofNullable(triggers.get(serverName));
    }

    // Datei laden beim Start
    @PostConstruct
    public void loadFromFile() {
        if (file.exists()) {
            try {
                Map<String, List<ThresholdConfig>> mapFromFile = objectMapper.readValue(file,
                        new TypeReference<Map<String, List<ThresholdConfig>>>() {});
                triggers.clear();
                triggers.putAll(mapFromFile);
                System.out.println("Trigger aus Datei geladen: " + triggers.size());
                Logger.info("Trigger aus Datei geladen: " + triggers.size());
            } catch (IOException e) {
                System.err.println("Fehler beim Laden der Trigger aus Datei: " + e.getMessage());
                Logger.error("Fehler beim Laden der Trigger aus Datei: " + e.getMessage());
            }
        } else {
            System.out.println("Trigger-Datei existiert nicht, starte mit leerer Liste.");
            Logger.info("Trigger-Datei existiert nicht, starte mit leerer Liste.");
        }
    }

    // Speichern in Datei
    private void saveToFile() {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, triggers);
            System.out.println("Trigger in Datei gespeichert.");
            Logger.info("Trigger in Datei gespeichert.");
        } catch (IOException e) {
            System.err.println("Fehler beim Speichern der Trigger: " + e.getMessage());
            Logger.error("Fehler beim Speichern der Trigger: " + e.getMessage());
        }
    }
}
