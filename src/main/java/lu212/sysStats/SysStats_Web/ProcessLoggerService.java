package lu212.sysStats.SysStats_Web;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import lu212.sysStats.StatsServer.Server;

public class ProcessLoggerService {

    public static void saveProcesses(String serverName, List<Server.ServerProcessInfo> processes) {
        try {
            File dir = new File("data");
            dir.mkdirs();

            File file = new File(dir, serverName + "_processes.txt");
            if (!file.exists()) file.createNewFile(); // garantiert Datei

            if (processes.isEmpty()) return; // keine Prozesse, nichts schreiben

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
                for (Server.ServerProcessInfo p : processes) {
                    writer.write(p.pid + ";" + p.name + ";" + p.cpu + ";" + p.ram);
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
