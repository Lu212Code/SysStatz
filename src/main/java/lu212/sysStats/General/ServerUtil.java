package lu212.sysStats.General;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ServerUtil {

    private static final File SERVERS_FILE = new File("servers.txt");

    // Interne Methode zum Laden aller Server, ohne Endpoint
    public static synchronized List<String> getAllServers() {
        List<String> servers = new ArrayList<>();
        if (!SERVERS_FILE.exists()) return servers;

        try (BufferedReader br = new BufferedReader(new FileReader(SERVERS_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) servers.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return servers;
    }
}
