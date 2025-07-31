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

import org.json.JSONObject;
import org.json.JSONException;

public class Server {

	private static int PORT = 12345;

	private final static Map<String, ClientHandler> clients = new ConcurrentHashMap<>();

	private static boolean debugMode = false;

	private static final Map<String, ServerTempInfo> tempServerData = new ConcurrentHashMap<>();
	
	public static final Map<String, GeoInfo> geoLocations = new ConcurrentHashMap<>();
	
	private static Map<Integer, Double> cpuCoreLoads = new HashMap<>();

	public static void startServer(String[] args) throws IOException {
		new Server().start();
	}

	private void start() throws IOException {
		try {
		PORT = Integer.parseInt(SysStatsWebApplication.statsserverport);
		} catch (Exception e) {
			Logger.error("Fehler beim Parsen des StatsServer Ports: " + SysStatsWebApplication.statsserverport);
			System.out.println("Fehler beim Parsen des StatsServer Ports: " + SysStatsWebApplication.statsserverport);
			PORT = 12345;
		}
		
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
			if (debugMode) {
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
			} else if (debugMode == false) {
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
							}
						} else if (line.startsWith("GEO:")) {
							String geoJson = line.substring("GEO:".length()).trim();
							handleGeoInfo(name, geoJson);
						} else {
							Logger.info("Unbekannte Nachricht von " + name + ": " + line);
							send("Unbekannte Nachricht empfangen: " + line);
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
		    temp.ramAvailable = Auslastung;
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
		
		if (bauteil.equalsIgnoreCase("LOADAVG_1")) {
		    temp.loadavg1 = Auslastung;
		}
		if (bauteil.equalsIgnoreCase("LOADAVG_5")) {
		    temp.loadavg5 = Auslastung;
		}
		if (bauteil.equalsIgnoreCase("LOADAVG_15")) {
		    temp.loadavg15 = Auslastung;
		}
		
		if (bauteil.startsWith("CPU_CORE")) {
		    try {
		        int coreIndex = Integer.parseInt(bauteil.substring("CPU_CORE".length()));
		        double load = Double.parseDouble(Auslastung.replace(",", "."));
		        temp.cpuCoreLoads.put(coreIndex, load);
		    } catch (NumberFormatException e) {
		        Logger.warning("Ungültiger CPU_CORE Index oder Wert: " + bauteil + " " + Auslastung);
		    }
		}
		
		if (bauteil.equalsIgnoreCase("CPUVOLTAGE")) {
		    temp.cpuVoltage = Auslastung;
		    Logger.info("CPU Voltage von " + Server + ": " + Auslastung);
		}
		
		
		if (temp.cpu != null && temp.ramUsed  != null && temp.ramTotal != null && temp.disk != null && temp.sent != null && temp.recv != null
				&& temp.dsent != null && temp.drecv != null && temp.processes != null && temp.scmd != null && temp.temp != null && temp.swapTotal != null
				&& temp.swapUsed != null && cpuCoreLoads != null) {
			
			if(false) {
				System.out.println(temp.cpu);
				System.out.println(temp.disk);
				System.out.println(temp.temp);
				System.out.println(temp.processes);
				System.out.println(temp.swapTotal + "/" + temp.swapUsed);
				System.out.println(temp.ramAvailable + "/" + temp.ramTotal + "/" + temp.ramUsed);
			}
			
			try {
		        double ramUsed = Double.parseDouble(temp.ramUsed);
		        double ramTotal = Double.parseDouble(temp.ramTotal);

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
						temp.cpuCoreLoads, temp.swapTotal, temp.swapUsed);

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
		String ramAvailable;
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
		String loadavg1 = null;
		String loadavg5 = null;
		String loadavg15 = null;
		Map<Integer, Double> cpuCoreLoads = new HashMap<>();
		String cpuVoltage = null;
		
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

		} catch (Exception e) {
			Logger.warning("Fehler beim Parsen von PROC-Zeile: " + werte);
			System.err.println("Fehler beim Parsen von PROC-Zeile: " + werte);
		}
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
}
