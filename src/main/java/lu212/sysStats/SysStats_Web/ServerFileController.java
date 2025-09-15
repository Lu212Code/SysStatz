package lu212.sysStats.SysStats_Web;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/servers")
public class ServerFileController {

    private static final File SERVERS_FILE = new File("servers.txt");

    List<String> loadServers() {
        List<String> servers = new ArrayList<>();
        if (!SERVERS_FILE.exists()) return servers;

        try (BufferedReader br = new BufferedReader(new FileReader(SERVERS_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) servers.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return servers;
    }

    private synchronized void saveServers(List<String> servers) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(SERVERS_FILE))) {
            for (String server : servers) {
                bw.write(server);
                bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Server hinzufügen
    @PostMapping("/add")
    public ResponseEntity<?> addServer(@RequestBody Map<String, String> body) {
        String ip = body.get("ip");
        if (ip == null || ip.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Keine IP angegeben"));
        }

        List<String> servers = loadServers();
        if (servers.contains(ip)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Server existiert bereits"));
        }

        servers.add(ip);
        saveServers(servers);

        return ResponseEntity.ok(Map.of("status", "Server hinzugefügt", "ip", ip));
    }

    // Server entfernen
    @PostMapping("/remove")
    public ResponseEntity<?> removeServer(@RequestBody Map<String, String> body) {
        String ip = body.get("ip");   // statt "name"
        if (ip == null || ip.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Keine IP angegeben"));
        }

        List<String> servers = loadServers();
        boolean removed = servers.removeIf(s -> s.trim().equals(ip.trim()));

        if (!removed) {
            return ResponseEntity.status(404).body(Map.of("error", "Server nicht gefunden"));
        }

        saveServers(servers);
        return ResponseEntity.ok(Map.of("status", "Server entfernt", "ip", ip));
    }


    @GetMapping
    public ResponseEntity<List<String>> getServers() {
        return ResponseEntity.ok(loadServers());
    }
}
