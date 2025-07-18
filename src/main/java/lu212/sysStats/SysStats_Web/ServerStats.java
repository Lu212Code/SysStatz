package lu212.sysStats.SysStats_Web;

import java.util.*;

public class ServerStats {
    private static final Map<String, ServerInfo> serverMap = new HashMap<>();

    public static void update(String name, int cpuPercent, double ramUsed, double ramTotal, int diskPercent, double storageUsed, double storageTotal, String status, String boottime, String sent, String recv, String dsent, String drecv) {
        ServerInfo info = serverMap.get(name);
        if (info == null) {
            info = new ServerInfo(name, cpuPercent, ramUsed, ramTotal, diskPercent, storageUsed, storageTotal, status, boottime, sent, recv, dsent, drecv);
            serverMap.put(name, info);
        } else {
            info.update(cpuPercent, ramUsed, ramTotal, diskPercent, storageUsed, storageTotal, status, boottime, sent, recv, dsent, drecv);
        }
    }

    public static List<ServerInfo> getAllServers() {
        return new ArrayList<>(serverMap.values());
    }
}
