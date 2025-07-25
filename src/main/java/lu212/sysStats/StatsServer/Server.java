package lu212.sysStats.StatsServer;

import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

import lu212.sysStats.General.Logger;
import lu212.sysStats.SysStats_Web.ServerStats;
import lu212.sysStats.SysStats_Web.SysStatsWebApplication;

public class Server {
	
    private static int PORT = 12345;
    
    private final static Map<String, ClientHandler> clients = new ConcurrentHashMap<>();
    
    private static String configClientName = "!123conntroller123!";
    
    private static boolean debugMode = false;
    
    private static final Map<String, ServerTempInfo> tempServerData = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
    	
    	
    	
        new Server().start();
    }

    private void start() throws IOException {
    	PORT = Integer.parseInt(SysStatsWebApplication.statsserverport);
        ServerSocket serverSocket = new ServerSocket(PORT);
        Logger.info("StatsServer läuft auf Port " + PORT);
        System.out.println("StatsServer läuft auf Port " + PORT);
        
        while (true) {
            Socket clientSocket = serverSocket.accept();
            new Thread(() -> handleNewClient(clientSocket)).start();
        }
    }

    private void handleNewClient(Socket socket) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            out.println("GIB_NAME");
            String name = in.readLine();

            if (name == null || name.isBlank() || clients.containsKey(name)) {
                out.println("NAME_UNGUELTIG");
                socket.close();
                return;
            }
            out.println("NAME_ANGEKOMMEN");

            ClientHandler handler = new ClientHandler(name, socket, in, out);
            clients.put(name, handler);
            	Logger.error("Stats-Client '" + name + "' verbunden.");
            	System.out.println("Stats-Client '" + name + "' verbunden.");

            handler.listen();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Server sendet gezielt an Client
    public static void sendToClient(String clientName, String message) {
        ClientHandler handler = clients.get(clientName);
        if (handler != null) {
            handler.send(message);
        } else {
        	Logger.error("Client '" + clientName + "' nicht gefunden.");
            System.out.println("Client '" + clientName + "' nicht gefunden.");
        }
    }

    private class ClientHandler {
        private final String name;
        private final Socket socket;
        private final BufferedReader in;
        private final PrintWriter out;

        public ClientHandler(String name, Socket socket, BufferedReader in, PrintWriter out) {
            this.name = name;
            this.socket = socket;
            this.in = in;
            this.out = out;
        }

        public void listen() {
        	if(debugMode) {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                	Logger.info("Raw-Nachricht von " + name + ": " + line);
                    System.out.println("Raw-Nachricht von " + name + ": " + line);

                    if (line.startsWith("SERVER:")) {
                        String payload = line.substring("SERVER:".length()).trim();
                        String[] parts = payload.split("\\s+", 2);

                        if (parts.length == 2) {
                            String key = parts[0].toUpperCase();
                            String value = parts[1];
                            // Ausgabe im Format NAME:TYP:WERT
                            
                            String wert = name + ":" + key + ":" + value;
                            
                            buildLongs(wert);
                            
                        } else {
                        	Logger.warning("SERVER: Fehlerhaftes Format, Beispiel: SERVER:CPU 75");
                            send("SERVER: Fehlerhaftes Format, Beispiel: SERVER:CPU 75");
                        }
                    } else {
                    	Logger.info("SERVER: Nachricht empfangen: " + line);
                        send("SERVER: Nachricht empfangen: " + line);
                    }
                }
            } catch (IOException e) {
            	Logger.warning("Verbindung zu " + name + " verloren.");
                System.out.println("Verbindung zu " + name + " verloren.");
            } finally {
                disconnect();
            }
        	} else if(debugMode==false) {
        		try {
                    String line;
                    while ((line = in.readLine()) != null) {

                        if (line.startsWith("SERVER:")) {
                            String payload = line.substring("SERVER:".length()).trim();
                            String[] parts = payload.split("\\s+", 2);

                            if (parts.length == 2) {
                                String key = parts[0].toUpperCase();
                                String value = parts[1];
                                // Ausgabe im Format NAME:TYP:WERT
                                
                                String wert = name + ":" + key + ":" + value;
                                
                                buildLongs(wert);
                                
                                
                            } else {
                            	Logger.warning("SERVER: Fehlerhaftes Format, Beispiel: SERVER:CPU 75");
                                send("SERVER: Fehlerhaftes Format, Beispiel: SERVER:CPU 75");
                            }
                        } else {
                        	Logger.info("SERVER: Nachricht empfangen: " + line);
                            send("SERVER: Nachricht empfangen: " + line);
                        }
                    }
                } catch (IOException e) {
                	Logger.warning("Verbindung zu " + name + " verloren.");
                    System.out.println("Verbindung zu " + name + " verloren.");
                } finally {
                    disconnect();
                }
        	}
        }




        public void send(String message) {
            out.println(message);
        }

        private void disconnect() {
            try {
                clients.remove(name);
                socket.close();
                Logger.warning("Client '" + name + "' getrennt.");
                System.out.println("Client '" + name + "' getrennt.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private static void buildLongs(String werte) {
        
    	
        // String in 3 Teile zerlegen
        String[] parts = werte.split(":");
        if (parts.length != 3) {
        	Logger.warning("Ungültiges Format: " + werte);
            System.out.println("Ungültiges Format: " + werte);
            return;
        }

        String serverName = parts[0];
        String bauteil = parts[1];
        String auslastung = parts[2];
        
        
        sendToWebsite(serverName, bauteil, auslastung);
        
    }
    
    private static void sendToWebsite(String Server, String bauteil, String Auslastung) {
        ServerTempInfo temp = tempServerData.computeIfAbsent(Server, k -> new ServerTempInfo());

        if (bauteil.equalsIgnoreCase("CPU")) {
            try {
                String newAuslastung = Auslastung.replace(",", ".");
                double inDouble = Double.parseDouble(newAuslastung);
                temp.cpu = (int) inDouble;
                Logger.info("CPU:" + Server + ":" + temp.cpu);
                System.out.println("CPU:" + Server + ":" + temp.cpu);
            } catch (Exception e) {
            	Logger.warning("CPU konnte nicht gelesen werden: " + Auslastung);
                System.err.println("CPU konnte nicht gelesen werden: " + Auslastung);
            }
        }
        
        if (bauteil.equalsIgnoreCase("PROC")) {
            try {
                // Beispiel: "7932 javaw 188,26 61"
                // Split mit \s+ für beliebige Leerzeichen
                String[] teile = Auslastung.trim().split("\\s+");
                if (teile.length >= 4) {
                    int pid = Integer.parseInt(teile[0]);
                    String name = teile[1].replace("_", " ");
                    double cpu = Double.parseDouble(teile[2].replace(",", "."));
                    double ramGB = Double.parseDouble(teile[3].replace(",", "."));
                    long ramMB = (long) (ramGB * 1024);

                    System.out.printf("Prozess erhalten → PID: %d, Name: %s, CPU: %.2f%%, RAM: %d MB%n",
                            pid, name, cpu, ramMB);

                    ServerProcessInfo pInfo = new ServerProcessInfo(pid, name, cpu, ramMB);
                    temp.processes.add(pInfo);
                } else {
                	Logger.warning("Ungültige PROC-Daten empfangen: " + Auslastung);
                    System.err.println("Ungültige PROC-Daten empfangen: " + Auslastung);
                }
            } catch (Exception e) {
            	Logger.warning("Fehler beim Parsen von PROC-Daten: " + Auslastung);
                System.err.println("Fehler beim Parsen von PROC-Daten: " + Auslastung);
                e.printStackTrace();
            }
        }
        
        if (bauteil.equalsIgnoreCase("DISKUSAGE")) {
            temp.disk = Auslastung;
            Logger.info("DISK:" + Server + ":" + Auslastung);
            System.out.println("DISK:" + Server + ":" + Auslastung);
        }

        if (bauteil.equalsIgnoreCase("RAM")) {
            temp.ram = Auslastung;
            Logger.info("RAM:" + Server + ":" + Auslastung);
            System.out.println("RAM:" + Server + ":" + Auslastung);
        }
        
        if (bauteil.equalsIgnoreCase("NET_SENT")) {
        	temp.sent = Auslastung;
        }
        
        if (bauteil.equalsIgnoreCase("NET_RECV")) {
        	temp.recv = Auslastung;
        }
        
        if (bauteil.equalsIgnoreCase("NET_DELTA_SENT")) {
        	temp.dsent = Auslastung;
        }
        	
        if (bauteil.equalsIgnoreCase("NET_DELTA_RECV")) {
        	temp.drecv = Auslastung;
        }

        if (temp.cpu != null && temp.ram != null && temp.disk != null && temp.sent != null && temp.recv != null && temp.dsent != null && temp.drecv != null && temp.processes != null) {
            try {
                String[] teileRam = temp.ram.split("/");
                double ramUsed = Double.parseDouble(teileRam[0]);
                double ramTotal = Double.parseDouble(teileRam[1]);

                String[] teileDisk = temp.disk.split("/");
                double diskUsed = Double.parseDouble(teileDisk[0]);
                double diskTotal = Double.parseDouble(teileDisk[1]);

                LocalDateTime jetzt = LocalDateTime.now();

                // Format definieren, z. B. "18.07.2025 14:30:15"
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

                // Formatierte Ausgabe
                String formatiert = jetzt.format(formatter);
                
                ServerStats.update(Server, temp.cpu, ramUsed, ramTotal, 0, diskUsed, diskTotal, "Online", formatiert, temp.sent, temp.recv, temp.dsent, temp.drecv, temp.processes);

            } catch (Exception e) {
            	Logger.warning("RAM- oder Disk-Daten konnten nicht verarbeitet werden: RAM=" + temp.ram + ", DISK=" + temp.disk);
                System.err.println("RAM- oder Disk-Daten konnten nicht verarbeitet werden: RAM=" + temp.ram + ", DISK=" + temp.disk);
            }

            // Daten zurücksetzen für den nächsten Zyklus
            tempServerData.remove(Server);
        }
    }
    
    private static class ServerTempInfo {
        Integer cpu = null;
        String ram = null;
        String disk = null;
        String sent = null;
        String recv = null;
        String dsent = null;
        String drecv = null;
        
        public List<ServerProcessInfo> processes = new ArrayList<>();
    }
    
    public static class ServerProcessInfo {
        public int pid;
        public String name;
        public double cpu;
        public long ram;

        public ServerProcessInfo(int pid, String name, double cpu, long ram) {
            this.pid = pid;
            this.name = name;
            this.cpu = cpu;
            this.ram = ram;
        }
    }


    
    public static void sendCMD(String serverName, String command) {
    	sendToClient(serverName, "CMD:" +  command);
    }
    
    private static void handleProcessLine(String werte) {
        // Beispiel: "PROC 1234 java 12.3 1.25"
        String[] parts = werte.split(" ");
        if (parts.length != 5) {
        	Logger.warning("Ungültige PROC-Zeile: " + werte);
            System.out.println("Ungültige PROC-Zeile: " + werte);
            return;
        }

        try {
            String pid = parts[1];
            String name = parts[2];
            double cpu = Double.parseDouble(parts[3].replace(",", "."));
            double ram = Double.parseDouble(parts[4].replace(",", ".")); // in GB

            // Ausgabe (optional später ins Web senden)
            System.out.printf("PROC PID: %s, NAME: %s, CPU: %.2f%%, RAM: %.2f GB%n", pid, name, cpu, ram);

            // TODO: Webfrontend-Update? CSV schreiben? Liste zwischenspeichern?
            // z. B.:
            // ServerStats.addProcessInfo(serverName, pid, name, cpu, ram);

        } catch (Exception e) {
        	Logger.warning("Fehler beim Parsen von PROC-Zeile: " + werte);
            System.err.println("Fehler beim Parsen von PROC-Zeile: " + werte);
        }
    }
}
