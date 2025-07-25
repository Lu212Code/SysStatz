package lu212.sysStats.SysStats_Web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
    private final Path triggerFile = Paths.get("trigger.json");
    public record TriggerDisplayEntry(String server, ThresholdConfig config) {}

    // GET /settings: Seite mit geladenen Einstellungen anzeigen
    @GetMapping("/settings")
    public String settingsPage(HttpSession session, Model model) {
        Config config = loadConfig();
        model.addAttribute("config", config);
        
        model.addAttribute("users", UserStore.getAll());
        model.addAttribute("isAdmin", session.getAttribute("isAdmin"));
        
   	 	String theme = SysStatsWebApplication.theme;
   	 	model.addAttribute("theme", theme);

        return "settings"; // Thymeleaf-Template settings.html
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
    private void saveConfig(Config config) {
        try (BufferedWriter writer = Files.newBufferedWriter(CONFIG_PATH)) {
            writer.write("webPort=" + config.getWebPort() + "\n");
            writer.write("statsPort=" + config.getStatsPort() + "\n");
            writer.write("theme=" + config.getTheme() + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }    
}
