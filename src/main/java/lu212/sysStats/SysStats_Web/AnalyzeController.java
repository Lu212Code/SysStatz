package lu212.sysStats.SysStats_Web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import lu212.sysStats.General.OllamaAPI;

import java.io.*;
import java.nio.file.*;
import java.util.*;

@Controller
public class AnalyzeController {

    @GetMapping("/analyze")
    public String showAnalyzePage(@RequestParam(required = false) String server, Model model, HttpSession session) {
        List<String> servers = listAvailableServers();
        model.addAttribute("servers", servers);
        model.addAttribute("activePage", "analyze");
        model.addAttribute("isAdmin", session.getAttribute("isAdmin"));

        if (server != null && !server.isEmpty()) {
            List<int[]> data = readServerData(server);
            if (!data.isEmpty()) {
                Map<String, Object> stats = computeStats(data);
                model.addAttribute("stats", stats);
                model.addAttribute("selectedServer", server);
            }
        }
        
   	 	String theme = SysStatsWebApplication.theme;
   	 	model.addAttribute("theme", theme);
   	 	
        return "analyze";
    }

    @PostMapping("/analyze/ai")
    public String analyzeWithAI(@RequestParam String server,
    							@RequestParam String mode, Model model) {
        List<int[]> data = readServerData(server);
        if (data.isEmpty()) {
            model.addAttribute("formatted", "<p>Keine Daten verfügbar.</p>");
            return "analyzeResult";
        }

        String prompt;
        
        Map<String, Object> stats = computeStats(data);

        if(mode.equals("advanced")) {
        	prompt = SmartServerAnalyzer.analyze(server);
        } else {
        	System.out.println("Starte einfache AI-Analyse...");
        prompt = String.format(
        	   "Analyse this data, and tell me something of the server %s:\n" +
        	   "- CPU: min %.1f%%, max %.1f%%, avg %.1f%%\n" +
        	   "- RAM: min %.1f%%, max %.1f%%, avg %.1f%%\n" +
        	   "- Disk: min %.1f%%, max %.1f%%, avg %.1f%%\n"+
        	   "Gibt es Auffälligkeiten oder Optimierungspotenzial?",
        	   server,
        	   ((Number) stats.get("cpuMin")).doubleValue(),
        	   ((Number) stats.get("cpuMax")).doubleValue(),
        	   ((Number) stats.get("cpuAvg")).doubleValue(),
        	   ((Number) stats.get("ramMin")).doubleValue(),
        	   ((Number) stats.get("ramMax")).doubleValue(),
        	   ((Number) stats.get("ramAvg")).doubleValue(),
        	   ((Number) stats.get("diskMin")).doubleValue(),
        	   ((Number) stats.get("diskMax")).doubleValue(),
        	   ((Number) stats.get("diskAvg")).doubleValue()
        	);
        }


        String rawResponse = OllamaAPI.sendRequest(prompt);

        // Markdown-artige Formatierung ersetzen:
        String formatted = rawResponse
            .replace("\n", "<br>")
            .replaceAll("\\*\\*(.*?)\\*\\*", "<strong class='highlight'>$1</strong>");

        model.addAttribute("formatted", formatted);
        
   	 	String theme = SysStatsWebApplication.theme;
   	 	model.addAttribute("theme", theme);
   	 	
        return "analyzeResult";
    }

    private List<String> listAvailableServers() {
        File folder = new File("data");
        if (!folder.exists()) return Collections.emptyList();

        String[] files = folder.list((dir, name) -> name.endsWith(".txt"));
        if (files == null) return Collections.emptyList();

        List<String> serverNames = new ArrayList<>();
        for (String file : files) {
            serverNames.add(file.replace(".txt", ""));
        }
        return serverNames;
    }

    private List<int[]> readServerData(String server) {
        List<int[]> data = new ArrayList<>();
        try {
            List<String> lines = Files.readAllLines(Paths.get("data/" + server + ".txt"));
            for (String line : lines) {
                String[] parts = line.split(";");
                if (parts.length == 4) {
                    int cpu = Integer.parseInt(parts[1]);
                    int ram = Integer.parseInt(parts[2]);
                    int disk = Integer.parseInt(parts[3]);
                    data.add(new int[]{cpu, ram, disk});
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data;
    }

    private Map<String, Object> computeStats(List<int[]> data) {
        int cpuMin = 100, cpuMax = 0, cpuSum = 0;
        int ramMin = 100, ramMax = 0, ramSum = 0;
        int diskMin = 100, diskMax = 0, diskSum = 0;

        for (int[] entry : data) {
            int cpu = entry[0], ram = entry[1], disk = entry[2];

            cpuMin = Math.min(cpuMin, cpu);
            cpuMax = Math.max(cpuMax, cpu);
            cpuSum += cpu;

            ramMin = Math.min(ramMin, ram);
            ramMax = Math.max(ramMax, ram);
            ramSum += ram;

            diskMin = Math.min(diskMin, disk);
            diskMax = Math.max(diskMax, disk);
            diskSum += disk;
        }

        int count = data.size();
        Map<String, Object> stats = new HashMap<>();
        stats.put("cpuMin", cpuMin);
        stats.put("cpuMax", cpuMax);
        stats.put("cpuAvg", cpuSum / (double) count);

        stats.put("ramMin", ramMin);
        stats.put("ramMax", ramMax);
        stats.put("ramAvg", ramSum / (double) count);

        stats.put("diskMin", diskMin);
        stats.put("diskMax", diskMax);
        stats.put("diskAvg", diskSum / (double) count);

        return stats;
    }
}
