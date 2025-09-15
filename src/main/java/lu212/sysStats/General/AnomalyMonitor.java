package lu212.sysStats.General;

import lu212.sysStats.SysStats_Web.ServerInfo;
import lu212.sysStats.SysStats_Web.ServerStats;
import lu212.sysStats.SysStats_Web.ServerHistoryEntry;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class AnomalyMonitor {

    // Cooldown pro Server+Alert (z. B. 15 Minuten)
    private static final Duration ALERT_COOLDOWN = Duration.ofMinutes(15);
    private static final Map<String, LocalDateTime> lastAlerts = new HashMap<>();

    public static void start() {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            try {
                checkAllServers();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 5, TimeUnit.MINUTES);
    }

    private static void checkAllServers() throws IOException {
        for (ServerInfo server : ServerStats.getAllServers()) {
            List<ServerHistoryEntry> history = server.getHistory();
            if (!history.isEmpty()) {
                analyzeServer(server.getName(), history);
            }
        }
    }

    private static void analyzeServer(String name, List<ServerHistoryEntry> history) throws IOException {
        ServerHistoryEntry latest = history.get(history.size() - 1);

        int cpu = latest.getCpuPercent();
        double ramUsed = latest.getRamUsed();  // absolut
        int disk = latest.getDiskPercent();
        double swapPercent = latest.getSwapTotal() > 0
                ? (latest.getSwapUsed() / latest.getSwapTotal()) * 100
                : 0;

        // === CPU gesamt ===
        if (cpu > 90) {
            addAlert(name, "CPU usage very high (" + cpu + "%)", AlertUtil.Level.RED);
        } else if (cpu > 75) {
            addAlert(name, "CPU usage elevated (" + cpu + "%)", AlertUtil.Level.YELLOW);
        }

        // Trend: CPU steigt konstant (letzte 5 Werte)
        if (history.size() >= 5) {
            boolean increasing = true;
            for (int i = history.size() - 5; i < history.size() - 1; i++) {
                if (history.get(i).getCpuPercent() > history.get(i + 1).getCpuPercent()) {
                    increasing = false;
                    break;
                }
            }
            if (increasing) {
                addAlert(name, "CPU load rising steadily (last 5 samples)", AlertUtil.Level.YELLOW);
            }
        }

        // === CPU pro Kern ===
        Map<Integer, Double> cores = latest.getCpuCoreLoad();
        if (cores != null) {
            for (Map.Entry<Integer, Double> entry : cores.entrySet()) {
                double coreLoad = entry.getValue();
                if (coreLoad > 95) {
                    addAlert(name, "CPU core " + entry.getKey() + " overloaded (" + coreLoad + "%)", AlertUtil.Level.RED);
                }
            }
        }

        // === RAM Trend ===
        if (history.size() >= 5) {
            double avgRam = history.subList(history.size() - 5, history.size())
                    .stream().mapToDouble(ServerHistoryEntry::getRamUsed).average().orElse(0);
            if (ramUsed > avgRam * 1.2) {
                addAlert(name, "RAM usage jumped from ~" + Math.round(avgRam) + " to " + Math.round(ramUsed), AlertUtil.Level.YELLOW);
            }

            // stetiges Wachstum
            boolean ramIncreasing = true;
            for (int i = history.size() - 5; i < history.size() - 1; i++) {
                if (history.get(i).getRamUsed() > history.get(i + 1).getRamUsed()) {
                    ramIncreasing = false;
                    break;
                }
            }
            if (ramIncreasing) {
                addAlert(name, "RAM steadily increasing (possible leak)", AlertUtil.Level.YELLOW);
            }
        }

        // === Disk ===
        if (disk > 90) {
            addAlert(name, "Disk space nearly full (" + disk + "%)", AlertUtil.Level.RED);
        } else if (disk > 75) {
            addAlert(name, "Disk usage high (" + disk + "%)", AlertUtil.Level.YELLOW);
        }

        if (history.size() >= 2) {
            int prev = history.get(history.size() - 2).getDiskPercent();
            if (disk - prev > 5) {
                addAlert(name, "Disk usage jumped by " + (disk - prev) + "% in last interval", AlertUtil.Level.YELLOW);
            }
        }

        // === Swap ===
        if (swapPercent > 95) {
            addAlert(name, "Swap critically full (" + swapPercent + "%)", AlertUtil.Level.RED);
        } else if (swapPercent > 80) {
            addAlert(name, "Swap usage high (" + swapPercent + "%)", AlertUtil.Level.YELLOW);
        }
        
        if (cpu < 2 && (disk > 80 || swapPercent > 50)) {
            addAlert(name, "CPU idle but resources blocked (possible deadlock)", AlertUtil.Level.YELLOW);
        }
        
        if (cores != null && cpu < 40) {
            for (Map.Entry<Integer, Double> entry : cores.entrySet()) {
                if (entry.getValue() > 95) {
                    addAlert(name, "CPU core " + entry.getKey() + " maxed out while avg CPU low (" + cpu + "%)", AlertUtil.Level.YELLOW);
                }
            }
        }

        if (history.size() >= 20) {
            boolean alwaysIncreasing = true;
            for (int i = history.size() - 20; i < history.size() - 1; i++) {
                if (history.get(i).getRamUsed() > history.get(i + 1).getRamUsed()) {
                    alwaysIncreasing = false;
                    break;
                }
            }
            if (alwaysIncreasing) {
                addAlert(name, "RAM increasing for 20 samples (possible memory leak)", AlertUtil.Level.RED);
            }
        }
        
        if (swapPercent > 10 && ramUsed < (history.get(history.size() - 1).getRamUsed() * 0.5)) {
            addAlert(name, "Swap used despite free RAM", AlertUtil.Level.YELLOW);
        }
        
        if (history.size() >= 12) { // ~1h wenn alle 5min
            int oldDisk = history.get(history.size() - 12).getDiskPercent();
            if (disk - oldDisk > 10) {
                addAlert(name, "Disk usage grew " + (disk - oldDisk) + "% in the last hour", AlertUtil.Level.RED);
            }
        }

        if (history.size() >= 2) {
            int prevCpu = history.get(history.size() - 2).getCpuPercent();
            if (prevCpu < 30 && cpu > 90) {
                addAlert(name, "CPU spiked from " + prevCpu + "% to " + cpu + "%", AlertUtil.Level.RED);
            }
        }

        if (cpu < 1 && ramUsed < 100 && disk < 1) {
            addAlert(name, "Server appears idle/unresponsive (possible crash)", AlertUtil.Level.RED);
        }

        try {
            long sent = Long.parseLong(latest.getDsent());
            long recv = Long.parseLong(latest.getDrecv());

            if (history.size() >= 5) {
                double avgRecv = history.subList(history.size() - 5, history.size())
                        .stream().mapToLong(h -> Long.parseLong(h.getDrecv())).average().orElse(0);

                if (recv > avgRecv * 5) {
                    addAlert(name, "Network recv spiked from ~" + avgRecv + " to " + recv, AlertUtil.Level.RED);
                }
            }
        } catch (NumberFormatException ignored) {}

        if (history.size() >= 2) {
            LocalDateTime prev = history.get(history.size() - 2).getTimestamp();
            LocalDateTime now = latest.getTimestamp();
            if (Duration.between(prev, now).toMinutes() > 10) {
                addAlert(name, "Data gap detected (>10 min between samples)", AlertUtil.Level.YELLOW);
            }
        }

        if (history.size() >= 10) {
            double avgCpu = history.subList(history.size() - 10, history.size())
                    .stream().mapToInt(ServerHistoryEntry::getCpuPercent).average().orElse(0);
            double maxCpu = history.subList(history.size() - 10, history.size())
                    .stream().mapToInt(ServerHistoryEntry::getCpuPercent).max().orElse(0);

            if (maxCpu - avgCpu > 50) {
                addAlert(name, "CPU highly unstable (fluctuating >50%)", AlertUtil.Level.YELLOW);
            }
        }

    }

    private static void addAlert(String server, String text, AlertUtil.Level level) throws IOException {
        String key = server + ":" + text; // eindeutiger Key pro Server+Meldung
        LocalDateTime now = LocalDateTime.now();

        // Cooldown prüfen
        if (lastAlerts.containsKey(key)) {
            LocalDateTime last = lastAlerts.get(key);
            if (Duration.between(last, now).compareTo(ALERT_COOLDOWN) < 0) {
                return; // zu früh, kein neuer Alert
            }
        }

        lastAlerts.put(key, now);
        AlertUtil.addAlert("[" + now + "] [" + server + "] " + text, level);
    }
}
