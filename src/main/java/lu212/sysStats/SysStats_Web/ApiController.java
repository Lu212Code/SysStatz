package lu212.sysStats.SysStats_Web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import lu212.sysStats.General.SysStatzInfo;

import java.util.List;
import java.util.Map;

@RestController
public class ApiController {


    @GetMapping("/api/servers")
    public List<ServerInfo> getServers() {
        return ServerStats.getAllServers();
    }

    @GetMapping("/api/server/{name}")
    public ServerInfo getServerByName(@PathVariable String name) {
        return ServerStats.getAllServers().stream()
                .filter(s -> s.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
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
}
