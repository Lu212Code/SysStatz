package lu212.sysStats.SysStats_Web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lu212.sysStats.General.Logger;
import lu212.sysStats.General.SysStatzInfo;
import lu212.sysStats.General.ThresholdConfig;

import java.io.File;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@RestController
public class ApiController {

	private final Map<String, Set<String>> sentTriggerEmails = new HashMap<>();
	
    @Autowired
    private TriggerService triggerService;

    @GetMapping("/api/servers")
    public List<ServerInfo> getServers() {
        return ServerStats.getAllServers();
    }

    @PostMapping("/api/trigger/{serverName}")
    public void setTrigger(@PathVariable String serverName, @RequestBody ThresholdConfig config) {
        triggerService.saveTrigger(serverName, config);
    }

    @GetMapping("/api/server/{name}")
    public ServerInfo getServerByName(@PathVariable String name, Model model) {
        ServerInfo server = ServerStats.getAllServers().stream()
                .filter(s -> s.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);

        if (server != null) {
            checkTrigger(server);
        }
        
        System.out.println("Trigger-Check für Server: " + name);
        try {
            ObjectMapper mapper = new ObjectMapper();
            File file = new File("triggers.json");

            if (file.exists()) {
                Map<String, List<ThresholdConfig>> allTriggers = mapper.readValue(file,
                        new TypeReference<>() {});
                List<ThresholdConfig> serverTriggers = allTriggers.get(name);
                model.addAttribute("triggers", serverTriggers);
            } else {
                model.addAttribute("triggers", null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("triggers", null);
        }

        return server;
    }

    @GetMapping("/api/version")
    public Map<String, String> getVersion() {
        return Map.of("sysstatz version", SysStatzInfo.version);
    }

    @GetMapping("/api/status")
    public Map<String, String> getStatus() {
        return Map.of("status", "OK", "uptime", "running");
    }

    @GetMapping("/api/info")
    public Map<String, Object> getInfo() {
        return Map.of(
            "name", "SysStatz",
            "author", "Lukas Zeh",
            "features", List.of("Monitoring", "API", "Live-Auslastung")
        );
    }
    
    @GetMapping("/api/server/{name}/history")
    public List<ServerHistoryEntry> getServerHistory(@PathVariable String name) {
        return ServerStats.getAllServers().stream()
                .filter(s -> s.getName().equalsIgnoreCase(name))
                .findFirst()
                .map(ServerInfo::getHistory)
                .orElse(List.of());
    }
    
    //Trigger prüfen
    private void checkTrigger(ServerInfo server) {
        triggerService.getTrigger(server.getName()).ifPresent(triggerList -> {
            double cpu = server.getCpuPercent();
            double ram = server.getRamUsed() / (double) server.getRamTotal() * 100;
            double disk = server.getStorageUsed() / (double) server.getStorageTotal() * 100;

            for (ThresholdConfig config : triggerList) {
                if (cpu > config.cpu && canSendMail(server.getName(), "CPU")) {
                    System.out.println("Sende Trigger Mail für CPU...");
                    Logger.info("Sende Trigger Mail für CPU...");
                    sendMail(config.email, server.getName(), "CPU überschritten: " + cpu + " %");
                }

                if (ram > config.ram && canSendMail(server.getName(), "RAM")) {
                    System.out.println("Sende Trigger Mail für RAM...");
                    Logger.info("Sende Trigger Mail für RAM...");
                    sendMail(config.email, server.getName(), "RAM überschritten: " + ram + " %");
                }

                if (disk > config.disk && canSendMail(server.getName(), "Disk")) {
                    System.out.println("Sende Trigger Mail für Disk...");
                    Logger.info("Sende Trigger Mail für Disk...");
                    sendMail(config.email, server.getName(), "Speicher überschritten: " + disk + " %");
                }
            }
        });
    }


    
    //Trigger E-Mail Senden
    private void sendMail(String to, String server, String message) {
        try {
            URL url = new URL("http://lukas3dprinting.czeh.de/email-warnung.php");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.setRequestProperty("Content-Type", "application/json");

            String json = new ObjectMapper().writeValueAsString(Map.of(
                    "to", to,
                    "server", server,
                    "warnungen", message
            ));

            try (OutputStream os = con.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }

            if (con.getResponseCode() != 200) {
                System.err.println("Mailversand fehlgeschlagen: " + con.getResponseCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error("Email konnte nicht gesendet werden.");
        }
    }
    
    private synchronized boolean canSendMail(String server, String type) {
        Set<String> sentTypes = sentTriggerEmails.computeIfAbsent(server, k -> new HashSet<>());
        if (sentTypes.contains(type)) {
            return false; // Mail schon gesendet
        } else {
            sentTypes.add(type); // Status merken
            return true;
        }
    }
}
