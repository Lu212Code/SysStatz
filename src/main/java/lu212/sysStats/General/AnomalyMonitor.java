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

    // Nur ein Alert pro Tag pro Server+Typ
    private static final Duration ALERT_COOLDOWN = Duration.ofHours(24);
    // Alte Alerts löschen nach 2 Tagen
    private static final Duration ALERT_EXPIRE = Duration.ofDays(2);
    private static final Map<String, LocalDateTime> lastAlerts = new HashMap<>();

    // Schärfere Schwellenwerte
    private static final int CPU_HIGH = 95;
    private static final int CPU_WARN = 85;
    private static final int DISK_HIGH = 95;
    private static final int DISK_WARN = 80;
    private static final double RAM_JUMP_FACTOR = 1.3;
    private static final double RAM_TREND_FACTOR = 1.2;
    private static final double SWAP_HIGH = 85;

    public static void start() {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            try {
                purgeOldAlerts();
                checkAllServers();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 5, TimeUnit.MINUTES);
    }

    private static void purgeOldAlerts() {
        LocalDateTime now = LocalDateTime.now();
        lastAlerts.entrySet().removeIf(e -> Duration.between(e.getValue(), now).compareTo(ALERT_EXPIRE) > 0);
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
        double ramUsed = latest.getRamUsed();
        int disk = latest.getDiskPercent();
        double swapPercent = latest.getSwapTotal() > 0
                ? (latest.getSwapUsed() / latest.getSwapTotal()) * 100
                : 0;

        // === CPU gesamt ===
        if (cpu > CPU_HIGH) {
            addAlert(name, "CPU usage very high (" + cpu + "%)", AlertUtil.Level.RED);
        } else if (cpu > CPU_WARN) {
            addAlert(name, "CPU usage elevated (" + cpu + "%)", AlertUtil.Level.YELLOW);
        }

        // CPU Trend (letzte 8 Werte, nur bei deutlicher Steigung)
        int trendLength = 8;
        if (history.size() >= trendLength) {
            int startCpu = history.get(history.size() - trendLength).getCpuPercent();
            int endCpu = history.get(history.size() - 1).getCpuPercent();
            double cpuIncrease = endCpu - startCpu;

            if (cpuIncrease > 30) { // nur auslösen bei starkem Anstieg
                addAlert(name, "CPU rose by " + cpuIncrease + "% over last " + trendLength + " samples", AlertUtil.Level.YELLOW);
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

        // === RAM ===
        if (history.size() >= 8) {
            double avgRam = history.subList(history.size() - 8, history.size())
                    .stream().mapToDouble(ServerHistoryEntry::getRamUsed).average().orElse(0);
            if (ramUsed > avgRam * RAM_JUMP_FACTOR) {
                addAlert(name, "RAM usage jumped from ~" + Math.round(avgRam) + " to " + Math.round(ramUsed), AlertUtil.Level.YELLOW);
            }

            if (history.size() >= trendLength) {
                double startRam = history.get(history.size() - trendLength).getRamUsed();
                double endRam = history.get(history.size() - 1).getRamUsed();
                double increasePercent = (endRam - startRam) / startRam * 100;

                if (increasePercent > 50) { // nur auslösen, wenn >50% Anstieg
                    addAlert(name, "RAM increased by " + Math.round(increasePercent) + "% over last " + trendLength + " samples (possible memory leak)", AlertUtil.Level.RED);
                }
            }
        }

        // === Disk ===
        if (disk > DISK_HIGH) {
            addAlert(name, "Disk space nearly full (" + disk + "%)", AlertUtil.Level.RED);
        } else if (disk > DISK_WARN) {
            addAlert(name, "Disk usage high (" + disk + "%)", AlertUtil.Level.YELLOW);
        }

        if (history.size() >= 2) {
            int prev = history.get(history.size() - 2).getDiskPercent();
            if (disk - prev > 10) {
                addAlert(name, "Disk usage jumped by " + (disk - prev) + "% in last interval", AlertUtil.Level.YELLOW);
            }
        }

        // === Swap ===
        if (swapPercent > 95) {
            addAlert(name, "Swap critically full (" + Math.round(swapPercent) + "%)", AlertUtil.Level.RED);
        } else if (swapPercent > SWAP_HIGH) {
            addAlert(name, "Swap usage high (" + Math.round(swapPercent) + "%)", AlertUtil.Level.YELLOW);
        }

        // CPU idle & Ressourcen blockiert
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

        // RAM über längere Zeit
        if (history.size() >= 20) {
            double startRam = history.get(history.size() - 20).getRamUsed();
            double endRam = history.get(history.size() - 1).getRamUsed();
            double increasePercent = (endRam - startRam) / startRam * 100;

            if (increasePercent > 50) { // nur echte Steigerungen melden
                addAlert(name, "RAM increased by " + Math.round(increasePercent) + "% over last 20 samples (possible memory leak)", AlertUtil.Level.RED);
            }
        }

        if (swapPercent > 10 && ramUsed < (history.get(history.size() - 1).getRamUsed() * 0.5)) {
            addAlert(name, "Swap used despite free RAM", AlertUtil.Level.YELLOW);
        }

        // Diskanstieg letzte Stunde (~12 Samples)
        if (history.size() >= 12) {
            int oldDisk = history.get(history.size() - 12).getDiskPercent();
            if (disk - oldDisk > 10) {
                addAlert(name, "Disk usage grew " + (disk - oldDisk) + "% in the last hour", AlertUtil.Level.RED);
            }
        }

        // CPU Spike
        if (history.size() >= 2) {
            int prevCpu = history.get(history.size() - 2).getCpuPercent();
            if (prevCpu < 30 && cpu > 90) {
                addAlert(name, "CPU spiked from " + prevCpu + "% to " + cpu + "%", AlertUtil.Level.RED);
            }
        }

        // Server idle
        if (cpu < 1 && ramUsed < 100 && disk < 1) {
            addAlert(name, "Server appears idle/unresponsive (possible crash)", AlertUtil.Level.RED);
        }

        // Netzwerkspikes
        try {
            long recv = Long.parseLong(latest.getDrecv());
            if (history.size() >= 6) {
                double avgRecv = history.subList(history.size() - 6, history.size())
                        .stream().mapToLong(h -> Long.parseLong(h.getDrecv())).average().orElse(0);
                if (recv > avgRecv * 6) {
                    addAlert(name, "Network recv spiked from ~" + avgRecv + " to " + recv, AlertUtil.Level.RED);
                }
            }
        } catch (NumberFormatException ignored) {}

        // Datenlücke
        if (history.size() >= 2) {
            LocalDateTime prev = history.get(history.size() - 2).getTimestamp();
            LocalDateTime now = latest.getTimestamp();
            if (Duration.between(prev, now).toMinutes() > 15) {
                addAlert(name, "Data gap detected (>15 min between samples)", AlertUtil.Level.YELLOW);
            }
        }

        // CPU Stabilität
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
        String key = server + ":" + text;
        LocalDateTime now = LocalDateTime.now();

        // Cooldown prüfen (1x pro Tag)
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
