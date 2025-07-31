package lu212.sysStats.General;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import lu212.sysStats.StatsServer.Server.ServerProcessInfo;
import lu212.sysStats.SysStats_Web.ServerStats;

public class ServerStatsGenerator {

    public enum LoadLevel {
        NORMAL, HOCH, SEHR_HOCH
    }

    public static void generateRandomServers(int count, LoadLevel loadLevel) {
        Random rand = new Random();

        for (int i = 1; i <= count; i++) {
            String serverName = "Server-" + i;
            int cpu = getRandomValue(rand, loadLevel, 20, 50, 70, 85, 95);
            double ramUsed = getRandomValue(rand, loadLevel, 2, 4, 8, 12, 16);
            double ramTotal = 16;
            int diskPercent = getRandomValue(rand, loadLevel, 30, 50, 70, 85, 95);
            double storageTotal = 256;
            double storageUsed = (diskPercent / 100.0) * storageTotal;
            String status = "online";
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
            String boottime = LocalDateTime.now().minusHours(rand.nextInt(100)).format(formatter);
            String sent = rand.nextInt(1000) + "MB";
            String recv = rand.nextInt(1000) + "MB";
            String dsent = rand.nextInt(200) + "MB";
            String drecv = rand.nextInt(200) + "MB";
            String scmd = "none";
            String temp = getRandomTemp(rand, loadLevel);

            List<ServerProcessInfo> dummyProcesses = new ArrayList<>(); // Leere Liste oder zufällige Prozesse hinzufügen

            ServerStats.update(serverName, cpu, ramUsed, ramTotal, diskPercent, storageUsed, storageTotal,
                    status, boottime, sent, recv, dsent, drecv, dummyProcesses, scmd, temp);
        }
    }

    private static int getRandomValue(Random rand, LoadLevel level, int n1, int n2, int h1, int h2, int max) {
        return switch (level) {
            case NORMAL -> n1 + rand.nextInt(n2 - n1 + 1); // z. B. 20–50 %
            case HOCH -> h1 + rand.nextInt(h2 - h1 + 1);   // z. B. 70–85 %
            case SEHR_HOCH -> h2 + rand.nextInt(max - h2 + 1); // z. B. 85–95+ %
        };
    }

    private static String getRandomTemp(Random rand, LoadLevel level) {
        int temp = switch (level) {
            case NORMAL -> 40 + rand.nextInt(15);
            case HOCH -> 60 + rand.nextInt(15);
            case SEHR_HOCH -> 75 + rand.nextInt(15);
        };
        return temp + "°C";
    }
}
