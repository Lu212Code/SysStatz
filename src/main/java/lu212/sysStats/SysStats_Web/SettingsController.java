package lu212.sysStats.SysStats_Web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import lu212.sysStats.General.Logger;
import lu212.sysStats.General.ServerUtil;
import lu212.sysStats.General.ThresholdConfig;
import lu212.sysStats.General.UserStore;
import lu212.sysStats.StatsServer.Server;

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

            model.addAttribute("config", config);           // ⚡ Config mit mails
            model.addAttribute("mails", config.getMails()); // ⚡ extra Map für Iteration
            model.addAttribute("users", UserStore.getAll());
            model.addAttribute("isAdmin", session.getAttribute("isAdmin"));
            model.addAttribute("activePage", "settings");
            model.addAttribute("servers", ServerUtil.getAllServers());

            String theme = SysStatsWebApplication.theme;
            model.addAttribute("theme", theme);

            return "settings"; // Thymeleaf-Template settings.html
        } else {
            return "redirect:/login?error=sessionExpired";
        }
    }
    
    @PostMapping("/settings/deleteMail")
    @ResponseBody
    public Map<String, Object> deleteMail(@RequestBody Map<String, String> body) {
        String key = body.get("key");
        Config config = loadConfig();
        Map<String, Object> resp = new HashMap<>();
        if(config.getMails().containsKey(key)) {
            config.getMails().remove(key);
            saveConfig(config);
            resp.put("success", true);
        } else {
            resp.put("success", false);
            resp.put("error", "User nicht gefunden");
        }
        return resp;
    }

    // POST /settings/save: Einstellungen speichern und zurück zur Seite
    @PostMapping("/settings/save")
    public String saveSettings(@ModelAttribute Config formConfig,
                               @RequestParam(required = false) String newMailUser,
                               @RequestParam(required = false) String newMailAddress,
                               @RequestParam(required = false) String deleteMail) {

        // 1. Alte Config laden
        Config config = loadConfig();

        // 2. Alte Mails aus Form übernehmen (falls sie editiert wurden)
        if (formConfig.getMails() != null) {
            config.getMails().putAll(formConfig.getMails());
        }

        // 3. Neuen User hinzufügen
        if (newMailUser != null && newMailAddress != null &&
            !newMailUser.isBlank() && !newMailAddress.isBlank()) {
            config.getMails().put(newMailUser, newMailAddress);
        }

        // 4. Mail löschen, falls gewünscht
        if (deleteMail != null) {
            config.getMails().remove(deleteMail);
        }

        // 5. Andere Settings übernehmen
        if(formConfig.getWebPort() != null) config.setWebPort(formConfig.getWebPort());
        if(formConfig.getStatsPort() != null) config.setStatsPort(formConfig.getStatsPort());
        if(formConfig.getTheme() != null) config.setTheme(formConfig.getTheme());
        if(formConfig.getOllamaserverip() != null) config.setOllamaserverip(formConfig.getOllamaserverip());
        if(formConfig.getTwoFactorRequired() != null) config.setTwoFactorRequired(formConfig.getTwoFactorRequired());
        if(formConfig.getClientPassword() != null) config.setClientPassword(formConfig.getClientPassword());
        if(formConfig.getApiKey() != null) config.setApiKey(formConfig.getApiKey());
        config.setEnableAlertMail(formConfig.isEnableAlertMail());

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
                            case "apiKey" -> config.setApiKey(parts[1]);
                        }
                        if (parts[0].startsWith("mail.")) {
                            String username = parts[0].substring(5);
                            config.getMails().put(username, parts[1]);
                        } else if (parts[0].equals("enableAlertMail")) {
                            config.setEnableAlertMail(Boolean.parseBoolean(parts[1]));
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
            Map<String, String> configMap = new LinkedHashMap<>();

            // nur die aktuellen Werte von newConfig nehmen, keine alte Datei einlesen!
            if (newConfig.getWebPort() != null) configMap.put("webPort", newConfig.getWebPort());
            if (newConfig.getStatsPort() != null) configMap.put("statsPort", newConfig.getStatsPort());
            if (newConfig.getTheme() != null) configMap.put("theme", newConfig.getTheme());
            if (newConfig.getOllamaserverip() != null) configMap.put("ollamaserverip", newConfig.getOllamaserverip());
            if (newConfig.getTwoFactorRequired() != null) configMap.put("twoFactorRequired", newConfig.getTwoFactorRequired());
            if (newConfig.getClientPassword() != null) configMap.put("clientPassword", newConfig.getClientPassword());
            if (newConfig.getApiKey() != null) configMap.put("apiKey", newConfig.getApiKey());
            if (newConfig.getMails() != null) {
                for (Map.Entry<String, String> entry : newConfig.getMails().entrySet()) {
                    configMap.put("mail." + entry.getKey(), entry.getValue());
                }
            }
            configMap.put("enableAlertMail", Boolean.toString(newConfig.isEnableAlertMail()));

            try (BufferedWriter writer = Files.newBufferedWriter(CONFIG_PATH)) {
                for (Map.Entry<String, String> entry : configMap.entrySet()) {
                    writer.write(entry.getKey() + "=" + entry.getValue() + "\n");
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    
    @GetMapping("/blocked")
    @ResponseBody
    public List<String> getBlockedIPs() {
        return new ArrayList<>(Server.getBlockedIPs());
    }
    
    @PostMapping("/blocked/accept")
    @ResponseBody
    public Map<String, String> acceptBlocked(@RequestBody Map<String, String> body) {
        String ip = body.get("ip");
        Server.acceptBlockedIP(ip);

        Map<String, String> resp = new HashMap<>();
        resp.put("status", "accepted");
        resp.put("ip", ip);
        return resp;
    }
    
    @PostMapping("/blocked/reject")
    @ResponseBody
    public Map<String, String> rejectBlocked(@RequestBody Map<String, String> body) {
        String ip = body.get("ip");
        Server.rejectBlockedIP(ip);

        Map<String, String> resp = new HashMap<>();
        resp.put("status", "rejected");
        resp.put("ip", ip);
        return resp;
    }
}
