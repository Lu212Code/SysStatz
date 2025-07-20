package lu212.sysStats.SysStats_Web;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class ConfigManager {

    private static final Path CONFIG_PATH = Paths.get("config.txt");

    // Variablen für die Konfiguration
    private String webServerPort;
    private String statsServerPort;
    private String theme;

    public ConfigManager() {
        try {
            if (!Files.exists(CONFIG_PATH)) {
                createDefaultConfig();
            }
            loadConfig();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createDefaultConfig() throws IOException {
        List<String> defaultLines = List.of(
            "webPort=8080",
            "statsPort=12345",
            "theme=default"
        );
        Files.write(CONFIG_PATH, defaultLines);
    }

    private void loadConfig() throws IOException {
        List<String> lines = Files.readAllLines(CONFIG_PATH);
        for (String line : lines) {
            String[] parts = line.split("=", 2);
            if (parts.length == 2) {
                switch (parts[0]) {
                    case "webPort" -> webServerPort = parts[1];
                    case "statsPort" -> statsServerPort = parts[1];
                    case "theme" -> theme = parts[1];
                }
            }
        }
    }

    // Getter-Methoden für die Variablen

    public String getWebServerPort() {
        return webServerPort;
    }

    public String getStatsServerPort() {
        return statsServerPort;
    }
    
    public String getTheme() {
    	return theme + ".css";
    }
}
