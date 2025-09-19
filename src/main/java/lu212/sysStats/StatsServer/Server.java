package lu212.sysStats.StatsServer;

import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

import lu212.sysStats.General.AlertUtil;
import lu212.sysStats.General.AlertUtil.Level;
import lu212.sysStats.General.KeystoreManager;
import lu212.sysStats.General.Logger;
import lu212.sysStats.General.Plugins;
import lu212.sysStats.SysStats_Web.ServerStats;
import lu212.sysStats.SysStats_Web.SysStatsWebApplication;

import org.json.JSONObject;


import org.json.JSONException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

import java.security.KeyStore;

public class Server {

	private static int PORT = 12345;
	private final static Map<String, ClientHandler> clients = new ConcurrentHashMap<>();

	private static final Map<String, ServerTempInfo> tempServerData = new ConcurrentHashMap<>();
	public static final Map<String, GeoInfo> geoLocations = new ConcurrentHashMap<>();
	
	private static String SERVER_PASSWORD;
	private static final int MAX_CLIENTS = 50;
	private static final int CLIENT_TIMEOUT_MS = 20_000;
	
	private static final String SERVER_FILE = "servers.txt";
	private static final Set<String> allowedIPs = ConcurrentHashMap.newKeySet();
	
	private static final Set<String> blockedIPs = ConcurrentHashMap.newKeySet();
	
    private final Map<String, Integer> attempts = new HashMap<>();
    private final int THRESHOLD = 10;

	public static void startServer(String[] args) throws IOException {
		new Server().start();
	}

	public void start() {
	    try {
	    	SERVER_PASSWORD = SysStatsWebApplication.clientPassword;
	    	
	        // Keystore laden oder erzeugen
	        KeyStore ks = KeystoreManager.loadOrCreateKeystore();

	        // Passwort laden
	        String keystorePassword = KeystoreManager.loadKeystorePassword();

	        // KeyManagerFactory initialisieren
	        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
	        kmf.init(ks, keystorePassword.toCharArray());

	        // SSLContext initialisieren
	        SSLContext sslContext = SSLContext.getInstance("TLS");
	        sslContext.init(kmf.getKeyManagers(), null, null);

	        // SSLServerSocketFactory erstellen
	        SSLServerSocketFactory ssf = sslContext.getServerSocketFactory();

	        // SSLServerSocket erstellen
	        SSLServerSocket serverSocket = (SSLServerSocket) ssf.createServerSocket(PORT);
	        
	        loadServerIPs();
	        markRegisteredServersOffline();
	        watchServerFile();
	        startClientTimeoutChecker();

	        System.out.println("Stats-Server laeuft auf Port " + PORT);

	        while (true) {
	            SSLSocket clientSocket = (SSLSocket) serverSocket.accept();
	            String clientIP = clientSocket.getInetAddress().getHostAddress();
	            if (!isIPAllowed(clientIP)) {
	                System.out.println("Verbindung von nicht erlaubter IP " + clientIP + " abgelehnt.");
	                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
	                out.println("SERVER: IP NICHT ERLAUBT");
	                clientSocket.close();
	                blockedIPs.add(clientIP);
	                AlertUtil.addAlert("Server mit unerlaubter IP verbunden: " + clientIP, Level.RED);
	                continue;
	            }

	            clientSocket.startHandshake();
	            new Thread(() -> handleNewClient(clientSocket)).start();
                synchronized (clients) {
                    if (clients.size() >= MAX_CLIENTS) {
                        System.out.println("Maximale Anzahl Clients erreicht. Verbindung wird abgelehnt.");
                        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                        out.println("SERVER: Maximal erlaubte Client-Anzahl erreicht, bitte später erneut verbinden.");
                        clientSocket.close();
                        AlertUtil.addAlert("Maximale Client Anzahl erreicht!", Level.RED);
                        continue; // Verbindung nicht akzeptieren, Schleife weiter
                    }
                }
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
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
				AlertUtil.addAlert("Server mit ungültigem oder doppeltem Namen verbunden: " + name, Level.RED);
				return;
			}
			out.println("NAME_ANGEKOMMEN");
			
			out.println("GIB_PASSWORT");
			String password = in.readLine();

			if (!SERVER_PASSWORD.equals(password)) {
			    out.println("PASSWORT_FALSCH");
			    socket.close();
			    AlertUtil.addAlert("Server mit falschem Passwort verbunden: " + name, Level.RED);
			    return;
			}

			out.println("PASSWORT_OK");
			
	        // IP des Clients
	        String clientIP = socket.getInetAddress().getHostAddress();

	        // Speichern in server_names.txt
	        saveServerNameAndIP(clientIP, name);

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
		
	    private volatile long lastMessageTime;
	    private volatile boolean offlineSent = false;

	    public ClientHandler(String name, Socket socket, BufferedReader in, PrintWriter out) {
	        this.name = name;
	        this.socket = socket;
	        this.in = in;
	        this.out = out;
	        this.lastMessageTime = System.currentTimeMillis();
	    }

		public void listen() {
				try {
					String line;
					while ((line = in.readLine()) != null) {
						
						if (line.startsWith("HW:")) {
							String payload = line.substring("HW:".length()).trim();
							String[] parts = payload.split("\\s+", 2);

							if (parts.length == 2) {
								String key = parts[0];
								String value = parts[1];
								handleHardwareInfo(name, key, value);
							} else {
								Logger.warning("Ungültige HW-Nachricht: " + line);
								AlertUtil.addAlert("Fehlerhaftes HW-Format von Server " + name + ", ist der Server auf der neuesten Version?", Level.YELLOW);
								recordFailedAttempt(name);
							}
						} else if (line.startsWith("SERVER:")) {
							String payload = line.substring("SERVER:".length()).trim();
							String[] parts = payload.split("\\s+", 2);

							if (parts.length == 2) {
								String key = parts[0].toUpperCase();
								String value = parts[1];
								String wert = name + ":" + key + ":" + value;
								buildLongs(wert);
							} else {
								Logger.warning("SERVER: Fehlerhaftes Format, Beispiel: SERVER:CPU 75");
								send("SERVER: Fehlerhaftes Format, Beispiel: SERVER:CPU 75");
								AlertUtil.addAlert("Fehlerhaftes Format von Server " + name + ", ist der Server auf der neuesten Version?", Level.YELLOW);
								recordFailedAttempt(name);
							}
						} else if (line.startsWith("GEO:")) {
							String geoJson = line.substring("GEO:".length()).trim();
							handleGeoInfo(name, geoJson);
						} else if (line.startsWith("PLUGIN:")) {
							String payload = line.substring("PLUGIN:".length()).trim();
							String[] parts = payload.split("\\s+", 2);
							
							if (parts.length == 2) {
								String key = parts[0].toUpperCase();
								String value = parts[1];
								Plugins.handlePluginInfo(name, Plugins.getPluginName(key), key, value);
							}
						} else {
							Logger.info("Unbekannte Nachricht von " + name + ": " + line);
							send("Unbekannte Nachricht empfangen: " + line);
							AlertUtil.addAlert("Unbekannte Nachricht von Server " + name + ", ist der Server auf der neuesten Version?", Level.YELLOW);
							recordFailedAttempt(name);
						}
					}
				} catch (IOException e) {
					Logger.warning("Verbindung zu " + name + " verloren.");
					System.out.println("Verbindung zu " + name + " verloren.");
				} finally {
					disconnect();
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

	private static void handleHardwareInfo(String serverName, String hardwareKey, String value) {
		String message = String.format("Hardwareinfo von %s → %s: %s", serverName, hardwareKey, value);
		Logger.info(message);

		// Optional: an Web-Frontend weiterreichen, z. B. speichern oder auf Seite
		// anzeigen
		ServerStats.saveHardwareInfo(serverName, hardwareKey, value);
	}

	private static void sendToWebsite(String Server, String bauteil, String Auslastung) {
		ServerTempInfo temp = tempServerData.computeIfAbsent(Server, k -> new ServerTempInfo());

		if (bauteil.equalsIgnoreCase("CPU")) {
			try {
				String newAuslastung = Auslastung.replace(",", ".");
				double inDouble = Double.parseDouble(newAuslastung);
				temp.cpu = (int) inDouble;
				Logger.info("CPU:" + Server + ":" + temp.cpu);
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
		}

		if (bauteil.equalsIgnoreCase("RAM_USED_MB")) {
		    temp.ramUsed = Auslastung;
		}
		if (bauteil.equalsIgnoreCase("RAM_AVAILABLE_MB")) {
		}
		if (bauteil.equalsIgnoreCase("RAM_TOTAL_MB")) {
		    temp.ramTotal = Auslastung;
		}
		if (bauteil.equalsIgnoreCase("SWAP_USED_MB")) {
		    temp.swapUsed = Auslastung;
		}
		if (bauteil.equalsIgnoreCase("SWAP_TOTAL_MB")) {
		    temp.swapTotal = Auslastung;
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
		
		if (bauteil.equalsIgnoreCase("SCMD")) {
			temp.scmd = Auslastung;
		}
		
		if (bauteil.equalsIgnoreCase("TEMP")) {
			temp.temp = Auslastung;
		}
		
		if (bauteil.startsWith("CPU_CORE") && bauteil.endsWith("_LOAD")) {
		    int coreIndex = Integer.parseInt(bauteil.substring("CPU_CORE".length(), bauteil.indexOf("_LOAD")));
		    double load = Double.parseDouble(Auslastung.replace(",", "."));
		    temp.cpuCoreLoads.put(coreIndex, load);
		}
		
		if (bauteil.startsWith("CPU_CORE") && bauteil.endsWith("_FREQ")) {
		    int coreIndex = Integer.parseInt(bauteil.substring("CPU_CORE".length(), bauteil.indexOf("_FREQ")));
		    double freq = Double.parseDouble(Auslastung.replace(",", "."));
		    temp.cpuCoreFreqs.put(coreIndex, freq);
		}

		
		if (temp.cpu != null && temp.ramUsed  != null && temp.ramTotal != null && temp.disk != null && temp.sent != null && temp.recv != null
				&& temp.dsent != null && temp.drecv != null && temp.processes != null && temp.scmd != null && temp.temp != null && temp.swapTotal != null
				&& temp.swapUsed != null && temp.cpuCoreLoads != null && temp.cpuCoreFreqs != null) {
			
			try {
				double ramUsed = parseDoubleSafe(temp.ramUsed);
				double ramTotal = parseDoubleSafe(temp.ramTotal);

				String[] teileDisk = temp.disk.split("/");
				double diskUsed = Double.parseDouble(teileDisk[0]);
				double diskTotal = Double.parseDouble(teileDisk[1]);

				LocalDateTime jetzt = LocalDateTime.now();

				// Format definieren, z. B. "18.07.2025 14:30:15"
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

				// Formatierte Ausgabe
				String formatiert = jetzt.format(formatter);

				int diskPercent = (int) ((diskUsed / diskTotal) * 100);

				ServerStats.update(Server, temp.cpu, ramUsed, ramTotal, diskPercent, diskUsed, diskTotal, "Online",
						formatiert, temp.sent, temp.recv, temp.dsent, temp.drecv, temp.processes, temp.scmd, temp.temp,
						temp.cpuCoreLoads, temp.swapTotal, temp.swapUsed, temp.cpuCoreFreqs);

			} catch (Exception e) {
				e.printStackTrace();
				Logger.warning("RAM- oder Disk-Daten konnten nicht verarbeitet werden: RAM=" + temp.ramUsed + "/" + temp.ramTotal + ", DISK="
						+ temp.disk);
				System.err.println("RAM- oder Disk-Daten konnten nicht verarbeitet werden: RAM=" + temp.ramUsed + "/" + temp.ramTotal + ", DISK="
						+ temp.disk);
			}

			// Daten zurücksetzen für den nächsten Zyklus
			tempServerData.remove(Server);
		}
	}

	private static class ServerTempInfo {
		Integer cpu = null;
		String ramUsed;
		String ramTotal;
		String swapUsed;
		String swapTotal;
		String disk = null;
		String sent = null;
		String recv = null;
		String dsent = null;
		String drecv = null;
		String scmd = null;
		String temp = null;
		Map<Integer, Double> cpuCoreLoads = new HashMap<>();
		Map<Integer, Double> cpuCoreFreqs = new HashMap<>();
		
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
	
	public static class GeoInfo {
	    public String city;
	    public String country;
	    public double lat;
	    public double lon;

	    GeoInfo(String city, String country, double lat, double lon) {
	        this.city = city;
	        this.country = country;
	        this.lat = lat;
	        this.lon = lon;
	    }
	}
	
	public static class GeoInfoDTO {
	    public String city;
	    public String country;
	    public double lat;
	    public double lon;

	    public GeoInfoDTO(String city, String country, double lat, double lon) {
	        this.city = city;
	        this.country = country;
	        this.lat = lat;
	        this.lon = lon;
	    }
	}


	public static void sendCMD(String serverName, String command) {
		sendToClient(serverName, "CMD:" + command);
	}

	private void handleGeoInfo(String serverName, String geoJson) {
	    try {
	        JSONObject geo = new JSONObject(geoJson);

	        // Beispielhafte Felder, die dein Client senden könnte:
	        String city = geo.optString("city", "unbekannt");
	        String country = geo.optString("country", "unbekannt");
	        double lat = geo.optDouble("lat", 0.0);
	        double lon = geo.optDouble("lon", 0.0);

	        // Beispiel: Speichere die Daten in einer Map (muss als Feld definiert sein)
	        geoLocations.put(serverName, new GeoInfo(city, country, lat, lon));

	        // Optional: Du kannst die Geo-Infos auch an dein Webfrontend weitergeben
	        // ServerStats.saveGeoLocation(serverName, city, country, lat, lon);

	    } catch (JSONException e) {
	        Logger.warning("Fehler beim Parsen der Geo-JSON-Daten von " + serverName + ": " + e.getMessage());
	        System.err.println("Fehler beim Parsen der Geo-JSON-Daten von " + serverName + ": " + e.getMessage());
	    }
	}
	
	public static Map<String, GeoInfo> getGeoLocations() {
	    return geoLocations;
	}
	
	private static void loadServerIPs() {
	    File file = new File(SERVER_FILE);
	    try {
	        if (!file.exists()) {
	            System.out.println("servers.txt existiert nicht, wird erstellt.");
	            file.createNewFile();
	        }
	        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
	            String line;
	            while ((line = reader.readLine()) != null) {
	                line = line.trim();  // <- hier trimmen!
	                if (!line.isEmpty() && isValidIP(line)) {
	                    allowedIPs.add(line);
	                }
	            }
	        }
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}
	
	private static boolean isValidIP(String ip) {
	    try {
	        InetAddress address = InetAddress.getByName(ip);
	        return address instanceof Inet4Address || address instanceof Inet6Address;
	    } catch (UnknownHostException e) {
	        return false;
	    }
	}
	
	private static boolean isIPAllowed(String ip) {
	    return allowedIPs.contains(ip);
	}
	
	public static synchronized void addAllowedIP(String ip) {
	    if (!isValidIP(ip)) return;

	    if (allowedIPs.add(ip)) {
	        try (BufferedWriter writer = new BufferedWriter(new FileWriter(SERVER_FILE, true))) {
	            writer.write(ip);
	            writer.newLine();
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	        System.out.println("Neue IP hinzugefügt: " + ip);
	    }
	}
	
	private static void watchServerFile() {
	    Thread watcher = new Thread(() -> {
	        File file = new File(SERVER_FILE);
	        long lastModified = file.exists() ? file.lastModified() : 0;

	        while (true) {
	            try {
	                Thread.sleep(5000); // alle 5 Sekunden prüfen

	                if (!file.exists()) {
	                    file.createNewFile();
	                    continue;
	                }

	                long modified = file.lastModified();
	                if (modified != lastModified) {
	                    lastModified = modified;
	                    updateAllowedIPs(file);
	                }

	            } catch (Exception e) {
	                e.printStackTrace();
	            }
	        }
	    });
	    watcher.setDaemon(true);
	    watcher.start();
	}

	private static void updateAllowedIPs(File file) {
	    Set<String> newIPs = new HashSet<>();
	    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
	        String line;
	        while ((line = reader.readLine()) != null) {
	            line = line.trim();
	            if (!line.isEmpty() && isValidIP(line)) {
	                newIPs.add(line);
	            }
	        }
	        allowedIPs.clear();
	        allowedIPs.addAll(newIPs);
	        System.out.println("Allowed IPs aktualisiert: " + allowedIPs);
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}
	
	private static void startClientTimeoutChecker() {
	    Thread t = new Thread(() -> {
	        while (true) {
	            try {
	                Thread.sleep(1000);
	                long now = System.currentTimeMillis();
	                for (ClientHandler client : clients.values()) {
	                    if (now - client.lastMessageTime > CLIENT_TIMEOUT_MS) {
	                        if (!client.offlineSent) {
	                            client.offlineSent = true;
	                            sendOfflineStatus(client.name);
	                        }
	                    }
	                }
	            } catch (InterruptedException e) {
	                e.printStackTrace();
	            }
	        }
	    });
	    t.setDaemon(true);
	    t.start();
	}

	private static void sendOfflineStatus(String clientName) {
	    ServerTempInfo temp = tempServerData.computeIfAbsent(clientName, k -> new ServerTempInfo());
	    // Alle Werte auf null setzen, nur Status auf Offline
	    ServerStats.update(clientName, temp.cpu != null ? temp.cpu : 0,
	                       temp.ramUsed != null ? Double.parseDouble(temp.ramUsed) : 0,
	                       temp.ramTotal != null ? Double.parseDouble(temp.ramTotal) : 0,
	                       0, 0, 0,
	                       "Offline", // <-- Status
	                       LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")),
	                       temp.sent, temp.recv, temp.dsent, temp.drecv, temp.processes, temp.scmd, temp.temp,
	                       temp.cpuCoreLoads, temp.swapTotal, temp.swapUsed, temp.cpuCoreFreqs);
	}
	
	private static synchronized void saveServerNameAndIP(String ip, String name) {
	    File file = new File("server_names.txt");
	    try {
	        if (!file.exists()) {
	            file.createNewFile();
	        }

	        // Prüfen, ob IP oder Name schon existiert
	        boolean exists = false;
	        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
	            String line;
	            while ((line = reader.readLine()) != null) {
	                String[] parts = line.split(";", 2); // Format: IP;Name
	                if (parts.length == 2) {
	                    if (parts[0].equals(ip) || parts[1].equalsIgnoreCase(name)) {
	                        exists = true;
	                        break;
	                    }
	                }
	            }
	        }

	        // Falls noch nicht vorhanden, hinzufügen
	        if (!exists) {
	            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
	                writer.write(ip + ";" + name);
	                writer.newLine();
	                System.out.println("Neuer Server in server_names.txt hinzugefügt: " + ip + " → " + name);
	            }
	        }

	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}
	
	private void markRegisteredServersOffline() {
	    File file = new File("server_names.txt");
	    Map<String, String> ipToName = new HashMap<>();

	    // Map von IP → Name laden
	    if (file.exists()) {
	        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
	            String line;
	            while ((line = reader.readLine()) != null) {
	                String[] parts = line.split(";", 2);
	                if (parts.length == 2) {
	                    ipToName.put(parts[0].trim(), parts[1].trim());
	                }
	            }
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	    }

	    // Für jede registrierte IP den Status offline senden, wenn ein Name existiert
	    for (String ip : allowedIPs) {
	        String name = ipToName.get(ip);
	        if (name != null && !name.isBlank()) {
	            sendOfflineStatus(name);
	            System.out.println("Server " + name + " (" + ip + ") auf Offline gesetzt.");
	        }
	    }
	}
	
    public void recordFailedAttempt(String serverName) {
        int newCount = attempts.getOrDefault(serverName, 0) + 1;
        attempts.put(serverName, newCount);

        if (newCount >= THRESHOLD) {
            onThresholdReached(serverName, newCount);
        }
    }

    public void resetAttempts(String serverName) {
        attempts.remove(serverName);
    }

    private void onThresholdReached(String serverName, int count) {
        System.out.println("Threshold erreicht für " + serverName + " (" + count + " fehlgeschlagene Versuche)");
        try {
			AlertUtil.addAlert("Der Server " + serverName + " hat sehr viele fehlerhafte Nachrichten gesendet. "
					+ "Bitte prüfe ob er Auf der neuesten Version ist, oder ob es garnicht ihr Server ist, sondern ein Fake-Client. "
					+ "Entfernen sie gegebenenfalls den Server von der IP Liste in den einstellungen: "
					+ getIPForServerName(serverName), Level.RED);
		} catch (IOException e) {
			e.printStackTrace();
		}
        resetAttempts(serverName);
    }
    
    public static String getIPForServerName(String serverName) {
        File file = new File("server_names.txt");
        if (!file.exists()) {
            return null;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(";", 2); // Format: IP;Name
                if (parts.length == 2) {
                    String ip = parts[0].trim();
                    String name = parts[1].trim();
                    if (name.equalsIgnoreCase(serverName)) {
                        return ip;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
    
    public static Set<String> getBlockedIPs() {
        return new HashSet<>(blockedIPs);
    }

    public static void acceptBlockedIP(String ip) {
        addAllowedIP(ip);       // IP in servers.txt übernehmen
        blockedIPs.remove(ip);  // aus der Liste der blockierten entfernen
    }
    
    public static void rejectBlockedIP(String ip) {
        blockedIPs.remove(ip);  // einfach nur aus Liste entfernen
    }
    
    private static double parseDoubleSafe(String s) {
        if (s == null || s.isBlank()) return 0;
        return Double.parseDouble(s.replace(",", "."));
    }
}
