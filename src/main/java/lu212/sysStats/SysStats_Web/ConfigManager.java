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
    private String ollamaserverip;
    private String twoFactorRequired;
    private String clientPassword;
    private String apiKey;
    private Map<String, String> mails = new HashMap<>();
    private String enableAlertMail;

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
            "theme=default",
            "ollamaserverip=default",
            "twoFactorRequired=false",
            "clientPassword=changeit",
            "apiKey=pv983qa4EHNZt",
            "enableAlertMail=false"
        );
        Files.write(CONFIG_PATH, defaultLines);
    }

    private void loadConfig() throws IOException {
        List<String> lines = Files.readAllLines(CONFIG_PATH);
        for (String line : lines) {
            String[] parts = line.split("=", 2);
            if (parts.length == 2) {
                String key = parts[0];
                String value = parts[1];
                switch (key) {
                    case "webPort" -> webServerPort = value;
                    case "statsPort" -> statsServerPort = value;
                    case "theme" -> theme = value;
                    case "ollamaserverip" -> ollamaserverip = value;
                    case "twoFactorRequired" -> twoFactorRequired = value;
                    case "clientPassword" -> clientPassword = value;
                    case "apiKey" -> apiKey = value;
                    default -> {
                        if (key.startsWith("mail.")) {
                            String username = key.substring(5);
                            mails.put(username, value);
                        } else if (key.equals("enableAlertMail")) {
                            enableAlertMail = value;
                        }
                    }
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
    
    public String getOllamaServerIP() {
    	return ollamaserverip;
    }
    
    public String getTwoFactor() {
    	return twoFactorRequired;
    }
    
    public String getClientPassword() {
    	return clientPassword;
    }
    
    public String getApiKey() {
    	return apiKey;
    }
    
    public Map<String, String> getMails() {
        return mails;
    }
    
    public String getEnableAlertMail() {
        return enableAlertMail;
    }
}
