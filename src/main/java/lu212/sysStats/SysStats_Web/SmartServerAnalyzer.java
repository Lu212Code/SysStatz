package lu212.sysStats.SysStats_Web;

import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

import lu212.sysStats.General.OllamaAPI;

public class SmartServerAnalyzer {

    static class Sample {
        long timestamp;
        double cpu;
        double ram;
        double disk;
        LocalDateTime datetime;

        Sample(long ts, double cpu, double ram, double disk) {
            this.timestamp = ts;
            this.cpu = cpu;
            this.ram = ram;
            this.disk = disk;
            this.datetime = Instant.ofEpochSecond(ts).atZone(ZoneId.systemDefault()).toLocalDateTime();
        }
    }

    private String serverName;
    private List<Sample> samples = new ArrayList<>();

    public SmartServerAnalyzer(String serverName) {
        this.serverName = serverName;
    }

    public void loadData() {
        Path path = Paths.get("data", serverName + ".txt");
        try (BufferedReader br = Files.newBufferedReader(path)) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(";");
                if (parts.length == 4) {
                    long ts = Long.parseLong(parts[0]);
                    double cpu = Double.parseDouble(parts[1]);
                    double ram = Double.parseDouble(parts[2]);
                    double disk = Double.parseDouble(parts[3]);
                    samples.add(new Sample(ts, cpu, ram, disk));
                }
            }
        } catch (IOException e) {
            System.err.println("Fehler beim Lesen von " + path);
            e.printStackTrace();
        }
    }

    public String generateSummary() {
        if (samples.isEmpty()) return "Keine Daten verfügbar.";

        // Gruppiere nach Datum
        Map<LocalDate, List<Sample>> tage = new TreeMap<>();
        for (Sample s : samples) {
            tage.computeIfAbsent(s.datetime.toLocalDate(), d -> new ArrayList<>()).add(s);
        }

        StringBuilder summary = new StringBuilder();
        summary.append("Analyse von Server: ").append(serverName).append("\n");
        summary.append("Zeitraum: ").append(tage.keySet().iterator().next())
                .append(" bis ").append(((TreeMap<LocalDate, List<Sample>>) tage).lastKey()).append("\n\n");

        List<Double> tagesdurchschnittCpu = new ArrayList<>();
        List<Double> tagesdurchschnittRam = new ArrayList<>();

        for (Map.Entry<LocalDate, List<Sample>> entry : tage.entrySet()) {
            LocalDate datum = entry.getKey();
            List<Sample> tag = entry.getValue();

            double cpuSum = 0, ramSum = 0, diskSum = 0;
            double cpuMax = Double.MIN_VALUE, cpuMin = Double.MAX_VALUE;
            for (Sample s : tag) {
                cpuSum += s.cpu;
                ramSum += s.ram;
                diskSum += s.disk;
                if (s.cpu > cpuMax) cpuMax = s.cpu;
                if (s.cpu < cpuMin) cpuMin = s.cpu;
            }

            double avgCpu = cpuSum / tag.size();
            double avgRam = ramSum / tag.size();

            tagesdurchschnittCpu.add(avgCpu);
            tagesdurchschnittRam.add(avgRam);

            summary.append("📅 ").append(datum).append(":\n");
            summary.append(String.format(" - CPU: Ø %.1f%% (Max: %.1f%%, Min: %.1f%%)\n", avgCpu, cpuMax, cpuMin));
            summary.append(String.format(" - RAM: Ø %.1f%%\n", avgRam));
            summary.append("\n");
        }

        // Mustererkennung: z. B. CPU nachts niedriger?
        double firstDayAvg = tagesdurchschnittCpu.get(0);
        boolean stabil = tagesdurchschnittCpu.stream().allMatch(v -> Math.abs(v - firstDayAvg) < 5);

        if (stabil) {
            summary.append("🔍 CPU-Auslastung ist über mehrere Tage relativ stabil.\n");
        } else {
            summary.append("📈 Es gibt erkennbare Unterschiede in der CPU-Auslastung zwischen den Tagen.\n");
        }

        // RAM-Leck-Muster?
        if (isRising(tagesdurchschnittRam)) {
            summary.append("⚠️ Hinweis: Der RAM-Verbrauch steigt kontinuierlich über mehrere Tage – mögliches Memory Leak.\n");
        }

        return summary.toString();
    }

    private boolean isRising(List<Double> values) {
        for (int i = 1; i < values.size(); i++) {
            if (values.get(i) < values.get(i - 1)) return false;
        }
        return true;
    }

    public String sendToOllama() {
        String summary = generateSummary();
        System.out.println(">> Sende Zusammenfassung an KI...\n");
        String antwort = OllamaAPI.sendRequest("Bitte analysiere folgendes Serververhalten:\n\n" + summary);
        System.out.println(">> Antwort der KI:\n" + antwort);
        return antwort;
    }

    public static void main(String[] args) {
        SmartServerAnalyzer analyzer = new SmartServerAnalyzer("mein_servername");
        analyzer.loadData();
        analyzer.sendToOllama();
    }
    
    public static String analyze(String servername) {
    	System.out.println("Starte erweiterte AI-Auswertung...");
    	SmartServerAnalyzer analyzer = new SmartServerAnalyzer(servername);
    	analyzer.loadData();
    	return analyzer.generateSummary();
    	
    }
    
}
