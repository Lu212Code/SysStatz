package lu212.sysStats.SysStats_Web;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import lu212.sysStats.General.Logger;
import lu212.sysStats.StatsServer.Server.ServerProcessInfo;

public class ServerStats {
    private static final Map<String, ServerInfo> serverMap = new HashMap<>();
    private static final Map<String, Map<String, String>> hardwareInfos = new ConcurrentHashMap<>();

    public static void update(String name, int cpuPercent, double ramUsed, double ramTotal, int diskPercent,
    						double storageUsed, double storageTotal, String status, String boottime, String sent, String recv,
    						String dsent, String drecv, List<ServerProcessInfo> processes, String scmd, String temp,
    						Map<Integer, Double> cpuCoreLoads, String swapTotal, String swapUsed, Map<Integer, Double> cpuCoreFreqs) {
    	
        ServerInfo info = serverMap.get(name);
        
        
        if (info == null) {
            info = new ServerInfo(name, cpuPercent, ramUsed, ramTotal, diskPercent, storageUsed, storageTotal, status, boottime, sent,
            		recv, dsent, drecv, processes, scmd, temp, cpuCoreLoads, swapTotal, swapUsed, cpuCoreFreqs);
            serverMap.put(name, info);
        } else {
            info.update(cpuPercent, ramUsed, ramTotal, diskPercent, storageUsed, storageTotal, status, boottime, sent,
            		recv, dsent, drecv, processes, scmd, temp , cpuCoreLoads, swapTotal, swapUsed, cpuCoreFreqs);
        }
    }

    public static List<ServerInfo> getAllServers() {
        return new ArrayList<>(serverMap.values());
    }
    
    public static void saveHardwareInfo(String serverName, String hwKey, String value) {
        hardwareInfos.computeIfAbsent(serverName, k -> new ConcurrentHashMap<>()).put(hwKey, value);
        persistHardwareInfo(serverName);
    }

    public static Map<String, String> getHardwareInfo(String serverName) {
        return hardwareInfos.getOrDefault(serverName, Map.of());
    }
    
    public static Map<String, String> loadHardwareInfo(String serverName) {
        Map<String, String> map = new LinkedHashMap<>();
        File file = new File("hardware_" + serverName + ".txt");

        if (!file.exists()) return map;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":", 2);
                if (parts.length == 2) {
                    map.put(parts[0], parts[1]);
                }
            }
        } catch (IOException e) {
            Logger.warning("Fehler beim Lesen der Hardwaredaten für " + serverName);
        }

        return map;
    }
    
    public static void persistHardwareInfo(String serverName) {
        Map<String, String> hw = hardwareInfos.get(serverName);
        if (hw == null) return;

        File file = new File("hardware_" + serverName + ".txt");
        try (PrintWriter writer = new PrintWriter(file)) {
            for (Map.Entry<String, String> entry : hw.entrySet()) {
                writer.println(entry.getKey() + ":" + entry.getValue());
            }
        } catch (IOException e) {
            Logger.warning("Fehler beim Speichern der Hardwaredaten für " + serverName);
        }
    }
}
