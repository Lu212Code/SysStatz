package lu212.sysstatz.SysStats_Web;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ServerStatsLogger {

    // Referenz zu einer Komponente, die Server-Infos liefert, z.B. ServerStats.getAllServers()
    @Scheduled(fixedRate = 10000)  // alle 10 Sekunden
    public void logServerStats() {
        List<ServerInfo> servers = ServerStats.getAllServers();
        for (ServerInfo server : servers) {
            if (server == null) {
                System.out.println("⚠️  Leerer Server-Eintrag übersprungen.");
                continue;
            }
            
            int cpu = (int) Math.round(server.getCpuPercent());
            int ram = (int) Math.round(server.getRamUsed() / (double) server.getRamTotal() * 100);
            int disk = (int) Math.round(server.getStorageUsed() / (double) server.getStorageTotal() * 100);

            appendServerStats(server.getName(), cpu, ram, disk);
        }
    }

    public void appendServerStats(String serverName, int cpuPercent, int ramPercent, int diskPercent) {
        try {
            // Verzeichnis sicherstellen
            Files.createDirectories(Paths.get("data"));

            String filename = "data/" + serverName + ".txt";

            long timestamp = System.currentTimeMillis() / 1000; // Unixzeit in Sekunden
            String line = timestamp + ";" + cpuPercent + ";" + ramPercent + ";" + diskPercent + "\n";

            try (FileWriter fw = new FileWriter(filename, true)) {
                fw.write(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
