package lu212.sysStats.StatsServer;

import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

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
        System.out.println("Server läuft auf Port " + PORT);
        
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
            if(name.equals(configClientName)) {
            	System.out.println("Controller/Config Client verbunden.");
            } else {
            System.out.println("Stats-Client '" + name + "' verbunden.");
            }

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
                            send("SERVER: Fehlerhaftes Format, Beispiel: SERVER:CPU 75");
                        }
                    } else {
                        send("SERVER: Nachricht empfangen: " + line);
                    }
                }
            } catch (IOException e) {
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
                                send("SERVER: Fehlerhaftes Format, Beispiel: SERVER:CPU 75");
                            }
                        } else {
                            send("SERVER: Nachricht empfangen: " + line);
                        }
                    }
                } catch (IOException e) {
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
                System.out.println("CPU:" + Server + ":" + temp.cpu);
            } catch (Exception e) {
                System.err.println("CPU konnte nicht gelesen werden: " + Auslastung);
            }
        }
        
        if (bauteil.equalsIgnoreCase("DISKUSAGE")) {
            temp.disk = Auslastung;
            System.out.println("DISK:" + Server + ":" + Auslastung);
        }

        if (bauteil.equalsIgnoreCase("RAM")) {
            temp.ram = Auslastung;
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

        if (temp.cpu != null && temp.ram != null && temp.disk != null && temp.sent != null && temp.recv != null && temp.dsent != null && temp.drecv != null) {
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
                
                ServerStats.update(Server, temp.cpu, ramUsed, ramTotal, 0, diskUsed, diskTotal, "Online", formatiert, temp.sent, temp.recv, temp.dsent, temp.drecv);

            } catch (Exception e) {
                System.err.println("RAM- oder Disk-Daten konnten nicht verarbeitet werden: RAM=" + temp.ram + ", DISK=" + temp.disk);
            }

            // Daten zurücksetzen für den nächsten Zyklus
            tempServerData.remove(Server);
        }

        // Sonderfall: Konfigurations-Client
        if (Server.equals(configClientName)) {
            handleConfigClient(bauteil, Auslastung);
        }
    }
    
    private static void handleConfigClient(String configType, String wert) {
    	if(configType.equalsIgnoreCase("stop")) {
    		System.out.println("Server wird von ConfigClient heruntergefahren.");
    		System.exit(0);
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
    }
    
    public static void sendCMD(String serverName, String command) {
    	sendToClient(serverName, "CMD:" +  command);
    }
    
}
