package lu212.sysStats.SysStats_Web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import lu212.sysStats.General.Logger;
import lu212.sysStats.General.ThresholdConfig;
import lu212.sysStats.General.UserStore;

import java.io.*;
import java.nio.file.*;
import java.util.*;

@Controller
public class SettingsController {

    private static final Path CONFIG_PATH = Paths.get("config.txt");
    public record TriggerDisplayEntry(String server, ThresholdConfig config) {}

    // GET /settings: Seite mit geladenen Einstellungen anzeigen
    @GetMapping("/settings")
    public String settingsPage(HttpSession session, Model model) {
		Boolean loggedIn = (Boolean) session.getAttribute("loggedIn");
		if (loggedIn != null && loggedIn) {
        Config config = loadConfig();
        model.addAttribute("config", config);
        
        model.addAttribute("users", UserStore.getAll());
        model.addAttribute("isAdmin", session.getAttribute("isAdmin"));
        model.addAttribute("activePage", "settings");
        
   	 	String theme = SysStatsWebApplication.theme;
   	 	model.addAttribute("theme", theme);

        return "settings"; // Thymeleaf-Template settings.html
		} else {
			return "redirect:/login?error=sessionExpired";
		}
    }

    // POST /settings/save: Einstellungen speichern und zurück zur Seite
    @PostMapping("/settings/save")
    public String saveSettings(@ModelAttribute Config config) {
        saveConfig(config);
        Logger.info("Einstellungen wurden gespeichert.");
        return "redirect:/settings";
    }

    // Hilfsmethode zum Laden der config.txt
    private Config loadConfig() {
    	Logger.info("Konfiguration wird geladen...");
        Config config = new Config();
        if (Files.exists(CONFIG_PATH)) {
            try {
                List<String> lines = Files.readAllLines(CONFIG_PATH);
                for (String line : lines) {
                    String[] parts = line.split("=", 2);
                    if (parts.length == 2) {
                        switch (parts[0]) {
                            case "webPort" -> config.setWebPort(parts[1]);
                            case "statsPort" -> config.setStatsPort(parts[1]);
                            case "theme" -> config.setTheme(parts[1]);
                            case "ollamaserverip" -> config.setOllamaserverip(parts[1]);
                            case "twoFactorRequired" -> config.setTwoFactorRequired(parts[1]);
                            case "clientPassword" -> config.setClientPassword(parts[1]);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                Logger.error("Konfiguration konnte nicht geladen werden.");
            }
        }
        return config;
    }

    // Hilfsmethode zum Speichern in config.txt
    private void saveConfig(Config newConfig) {
        try {
            // Alte Config laden
            Map<String, String> configMap = new LinkedHashMap<>();
            if (Files.exists(CONFIG_PATH)) {
                List<String> lines = Files.readAllLines(CONFIG_PATH);
                for (String line : lines) {
                    String[] parts = line.split("=", 2);
                    if (parts.length == 2) {
                        configMap.put(parts[0], parts[1]);
                    }
                }
            }

            // Neue Werte setzen (nur wenn != null)
            if (newConfig.getWebPort() != null) {
                configMap.put("webPort", newConfig.getWebPort());
            }
            if (newConfig.getStatsPort() != null) {
                configMap.put("statsPort", newConfig.getStatsPort());
            }
            if (newConfig.getTheme() != null) {
                configMap.put("theme", newConfig.getTheme());
            }
            if (newConfig.getOllamaserverip() != null) {
                configMap.put("ollamaserverip", newConfig.getOllamaserverip());
            }
            if (newConfig.getTwoFactorRequired() != null) {
                configMap.put("twoFactorRequired", newConfig.getTwoFactorRequired());
            }
            if (newConfig.getClientPassword() !=null) {
            	configMap.put("clientPassword", newConfig.getClientPassword());
            }

            // Zurückschreiben
            try (BufferedWriter writer = Files.newBufferedWriter(CONFIG_PATH)) {
                for (Map.Entry<String, String> entry : configMap.entrySet()) {
                    writer.write(entry.getKey() + "=" + entry.getValue() + "\n");
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
