package lu212.sysStats.SysStats_Web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lu212.sysStats.General.Logger;
import lu212.sysStats.General.PluginInfo;
import lu212.sysStats.General.Plugins;
import lu212.sysStats.General.SysStatzInfo;
import lu212.sysStats.General.ThresholdConfig;
import lu212.sysStats.StatsServer.Server;
import lu212.sysStats.StatsServer.Server.GeoInfoDTO;

import lu212.sysStats.General.AlertUtil;
import lu212.sysStats.General.AlertUtil.Level;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
public class ApiController {

	private final Map<String, Set<String>> sentTriggerEmails = new HashMap<>();
	
	
    @Autowired
    private TriggerService triggerService;

    @GetMapping("/api/servers")
    public List<ServerInfo> getServers() {
        return ServerStats.getAllServers();
    }
    
    @GetMapping("/api/servers/locations")
    public Map<String, GeoInfoDTO> getAllServerLocations() {
        Map<String, Server.GeoInfo> geoMap = Server.getGeoLocations();
        Map<String, GeoInfoDTO> result = new HashMap<>();
        for (Map.Entry<String, Server.GeoInfo> entry : geoMap.entrySet()) {
            Server.GeoInfo g = entry.getValue();
            result.put(entry.getKey(), new GeoInfoDTO(g.city, g.country, g.lat, g.lon));
        }
        return result;
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
    
    @GetMapping("/api/plugins/{serverName}")
    public Map<String, Map<String, String>> getPluginsByServer(@PathVariable String serverName) {
        Map<String, Object> rawValues = Plugins.getPluginInfoByServerName(serverName);
        Map<String, Map<String, String>> grouped = new LinkedHashMap<>();

        if (rawValues == null || rawValues.isEmpty()) return Collections.emptyMap();

        for (Map.Entry<String, Object> entry : rawValues.entrySet()) {
            // regKey = "PluginName|ValueKey"
            String[] parts = entry.getKey().split("\\|", 2);
            if (parts.length != 2) continue;
            String pluginName = parts[0];
            String valueKey = parts[1];
            Object value = entry.getValue();

            PluginInfo info = Plugins.getPluginInfo(pluginName);
            String unit = info != null && info.unit != null ? info.unit : "";

            grouped.computeIfAbsent(pluginName, k -> new LinkedHashMap<>())
                   .put(valueKey, value + (unit.isEmpty() ? "" : " " + unit));
        }

        return grouped;
    }


    
    @GetMapping("/api/server/{name}/longterm")
    public ResponseEntity<?> getLongTermStats(@PathVariable String name) {
        File file = new File("data/" + name + ".txt");
        if (!file.exists()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Datei nicht gefunden"));
        }

        try {
            List<Map<String, Object>> result = new java.util.ArrayList<>();

            java.nio.file.Files.lines(file.toPath()).forEach(line -> {
                String[] parts = line.split("\\|");
                if (parts.length == 4) {
                    try {
                        long timestamp = Long.parseLong(parts[0]);
                        double cpu = Double.parseDouble(parts[1]);
                        double ram = Double.parseDouble(parts[2]);
                        double disk = Double.parseDouble(parts[3]);

                        result.add(Map.of(
                                "timestamp", timestamp,
                                "cpu", cpu,
                                "ram", ram,
                                "disk", disk
                        ));
                    } catch (NumberFormatException ignored) {}
                }
            });

            return ResponseEntity.ok(result);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Fehler beim Lesen der Datei"));
        }
    }
    
    @GetMapping("/api/servers-from-files")
    public List<String> getServersFromFiles() {
        File dataDir = new File("data");
        if (!dataDir.exists() || !dataDir.isDirectory()) {
            return List.of();
        }
        String[] files = dataDir.list((dir, name) -> name.endsWith(".txt"));
        if (files == null) {
            return List.of();
        }
        // Entferne .txt-Endung und sortiere
        return Arrays.stream(files)
                .map(name -> name.substring(0, name.length() - 4))
                .sorted()
                .toList();
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
                    try {
						AlertUtil.addAlert("Die CPU von " + server.getName() + " hat " + cpu + " % überschritten.", Level.RED);
					} catch (IOException e) {
						e.printStackTrace();
					}
                }

                if (ram > config.ram && canSendMail(server.getName(), "RAM")) {
                    System.out.println("Sende Trigger Mail für RAM...");
                    Logger.info("Sende Trigger Mail für RAM...");
                    sendMail(config.email, server.getName(), "RAM überschritten: " + ram + " %");
                    try {
						AlertUtil.addAlert("Der RAM von " + server.getName() + " hat " + ram + "% überschritten.", Level.RED);
					} catch (IOException e) {
						e.printStackTrace();
					}
                }

                if (disk > config.disk && canSendMail(server.getName(), "Disk")) {
                    System.out.println("Sende Trigger Mail für Disk...");
                    Logger.info("Sende Trigger Mail für Disk...");
                    sendMail(config.email, server.getName(), "Speicher überschritten: " + disk + " %");
                    try {
						AlertUtil.addAlert("Der Speicher von " + server.getName() + " hat " + disk + "% überschritten.", Level.RED);
					} catch (IOException e) {
						e.printStackTrace();
					}
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
    
    @RestController
    @RequestMapping("/api/server")
    public class AdminApiController {

        @Value("${sysstatz.api.key}")
        private String apiKey; // Aus application.properties

        @PostMapping("/{name}/stop")
        public ResponseEntity<Map<String, String>> stopServer(@PathVariable String name, @RequestParam String key) {
            if (!apiKey.equals(key)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Ungültiger API-Key"));
            }

            try {
            	Server.sendToClient(name, "SCMD:SHUTDOWN");
                return ResponseEntity.ok(Map.of("status", "Server gestoppt", "server", name));
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Fehler beim Stoppen", "message", e.getMessage()));
            }
        }

        @PostMapping("/{name}/reboot")
        public ResponseEntity<Map<String, String>> rebootServer(@PathVariable String name, @RequestParam String key) {
            if (!apiKey.equals(key)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Ungültiger API-Key"));
            }

            try {
            	Server.sendToClient(name, "SCMD:REBOOT");
                return ResponseEntity.ok(Map.of("status", "Server neugestartet", "server", name));
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Fehler beim Reboot", "message", e.getMessage()));
            }
        }
        
        @GetMapping("/admin/shutdown")
        public void shutdownSysStatz(@RequestParam String key) {
            if (apiKey.equals(key)) {
            	
            }
        }
    }
    
  
    
    public class CpuDataPoint {
        private long timestamp;
        private double cpu;

        public CpuDataPoint() {}
        public CpuDataPoint(long timestamp, double cpu) {
            this.timestamp = timestamp;
            this.cpu = cpu;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }

        public double getCpu() {
            return cpu;
        }

        public void setCpu(double cpu) {
            this.cpu = cpu;
        }
    }

    public class SpikeDetectionResult {
        private boolean spikeDetected;
        private long spikeStartTimestamp;
        private long spikePeakTimestamp;
        private long spikeEndTimestamp;

        public SpikeDetectionResult() {}

        public SpikeDetectionResult(boolean spikeDetected, long spikeStartTimestamp, long spikePeakTimestamp, long spikeEndTimestamp) {
            this.spikeDetected = spikeDetected;
            this.spikeStartTimestamp = spikeStartTimestamp;
            this.spikePeakTimestamp = spikePeakTimestamp;
            this.spikeEndTimestamp = spikeEndTimestamp;
        }

        public boolean isSpikeDetected() {
            return spikeDetected;
        }

        public void setSpikeDetected(boolean spikeDetected) {
            this.spikeDetected = spikeDetected;
        }

        public long getSpikeStartTimestamp() {
            return spikeStartTimestamp;
        }

        public void setSpikeStartTimestamp(long spikeStartTimestamp) {
            this.spikeStartTimestamp = spikeStartTimestamp;
        }

        public long getSpikePeakTimestamp() {
            return spikePeakTimestamp;
        }

        public void setSpikePeakTimestamp(long spikePeakTimestamp) {
            this.spikePeakTimestamp = spikePeakTimestamp;
        }

        public long getSpikeEndTimestamp() {
            return spikeEndTimestamp;
        }

        public void setSpikeEndTimestamp(long spikeEndTimestamp) {
            this.spikeEndTimestamp = spikeEndTimestamp;
        }
    }

    
}
