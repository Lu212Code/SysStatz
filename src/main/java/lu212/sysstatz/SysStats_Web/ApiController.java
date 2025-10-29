package lu212.sysstatz.SysStats_Web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpSession;

import lu212.sysstatz.General.ProcessLogEntry;
import lu212.sysstatz.General.Restart;
import lu212.sysstatz.General.SysStatzInfo;
import lu212.sysstatz.General.ThresholdConfig;
import lu212.sysstatz.StatsServer.Server;
import lu212.sysstatz.StatsServer.Server.GeoInfoDTO;
import lu212.sysstatz.StatsServer.Server.ServerProcessInfo;
import lu212.sysstatz.plugins.PluginInfo;
import lu212.sysstatz.plugins.Plugins;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@RestController
public class ApiController {

	private final Path basePath = Paths.get("data");

	@GetMapping("/api/servers")
	public List<ServerInfo> getServers() {
		return ServerStats.getAllServers();
	}

	@GetMapping("/api/servers/locations")
	public Map<String, GeoInfoDTO> getAllServerLocations() {
		Map<String, Server.GeoInfo> geoMap = Server.getGeoLocations();
		Map<String, GeoInfoDTO> result = new HashMap<>();
		for (Map.Entry<String, Server.GeoInfo> entry : geoMap.entrySet()) {
			Server.GeoInfo g = entry.getValue();
			result.put(entry.getKey(), new GeoInfoDTO(g.city, g.country, g.lat, g.lon));
		}
		return result;
	}

	@GetMapping("/api/server/{name}")
	public ServerInfo getServerByName(@PathVariable String name, Model model) {
		ServerInfo server = ServerStats.getAllServers().stream().filter(s -> s.getName().equalsIgnoreCase(name))
				.findFirst().orElse(null);

		try {
			ObjectMapper mapper = new ObjectMapper();
			File file = new File("triggers.json");

			if (file.exists()) {
				Map<String, List<ThresholdConfig>> allTriggers = mapper.readValue(file, new TypeReference<>() {
				});
				List<ThresholdConfig> serverTriggers = allTriggers.get(name);
				model.addAttribute("triggers", serverTriggers);
			} else {
				model.addAttribute("triggers", null);
			}
		} catch (Exception e) {
			e.printStackTrace();
			model.addAttribute("triggers", null);
		}

		return server;
	}

	@GetMapping("/api/plugins/{serverName}")
	public Map<String, Map<String, String>> getPluginsByServer(@PathVariable String serverName) {
		Map<String, Object> rawValues = Plugins.getPluginInfoByServerName(serverName);
		Map<String, Map<String, String>> grouped = new LinkedHashMap<>();

		if (rawValues == null || rawValues.isEmpty())
			return Collections.emptyMap();

		for (Map.Entry<String, Object> entry : rawValues.entrySet()) {
			// regKey = "PluginName|ValueKey"
			String[] parts = entry.getKey().split("\\|", 2);
			if (parts.length != 2)
				continue;
			String pluginName = parts[0];
			String valueKey = parts[1];
			Object value = entry.getValue();

			PluginInfo info = Plugins.getPluginInfo(pluginName);
			String unit = info != null && info.unit != null ? info.unit : "";

			grouped.computeIfAbsent(pluginName, k -> new LinkedHashMap<>()).put(valueKey,
					value + (unit.isEmpty() ? "" : " " + unit));
		}

		return grouped;
	}

	@GetMapping("/api/server/{name}/longterm")
	public ResponseEntity<?> getLongTermStats(@PathVariable String name) {
		File file = new File("data/" + name + ".txt");
		if (!file.exists()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Datei nicht gefunden"));
		}

		try {
			List<Map<String, Object>> result = new java.util.ArrayList<>();

			java.nio.file.Files.lines(file.toPath()).forEach(line -> {
				String[] parts = line.split(";");
				if (parts.length == 4) {
					try {
						long timestamp = Long.parseLong(parts[0]);
						double cpu = Double.parseDouble(parts[1]);
						double ram = Double.parseDouble(parts[2]);
						double disk = Double.parseDouble(parts[3]);

						result.add(Map.of("timestamp", timestamp, "cpu", cpu, "ram", ram, "disk", disk));
					} catch (NumberFormatException ignored) {
					}
				}
			});

			return ResponseEntity.ok(result);
		} catch (IOException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("error", "Fehler beim Lesen der Datei"));
		}
	}

	@GetMapping("/api/servers-from-files")
	public List<String> getServersFromFiles() {
		File dataDir = new File("data");
		if (!dataDir.exists() || !dataDir.isDirectory()) {
			return List.of();
		}
		String[] files = dataDir.list((dir, name) -> name.endsWith(".txt"));
		if (files == null) {
			return List.of();
		}
		// Entferne .txt-Endung und sortiere
		return Arrays.stream(files).map(name -> name.substring(0, name.length() - 4)).sorted().toList();
	}

	@GetMapping("/api/version")
	public Map<String, String> getVersion() {
		return Map.of("sysstatz version", SysStatzInfo.version);
	}

	@GetMapping("/api/info")
	public Map<String, Object> getInfo() {
		return Map.of("name", "SysStatz", "author", "Lu212Code", "features",
				List.of("Monitoring", "API", "Live-Auslastung"));
	}

	@GetMapping("/api")
	public Map<String, Object> getApiInfo() {
		return Map.of("Status", "running", "SysStatz API-Version", SysStatzInfo.version);
	}

	@GetMapping("/api/server/{name}/history")
	public List<ServerHistoryEntry> getServerHistory(@PathVariable String name) {
		return ServerStats.getAllServers().stream().filter(s -> s.getName().equalsIgnoreCase(name)).findFirst()
				.map(ServerInfo::getHistory).orElse(List.of());
	}

	@RestController
	@RequestMapping("/api/server")
	public class AdminApiController {

		@Value("${sysstatz.api.key}")
		private String apiKey; // Aus application.properties

		@PostMapping("/{name}/stop")
		public ResponseEntity<Map<String, String>> stopServer(@PathVariable String name, @RequestParam String key,
				HttpSession session) {
			Boolean loggedIn = (Boolean) session.getAttribute("loggedIn");
			if (loggedIn != null && loggedIn) {
				if (!apiKey.equals(key)) {
					return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Ungültiger API-Key"));
				}

				try {
					Server.sendToClient(name, "SCMD:SHUTDOWN");
					return ResponseEntity.ok(Map.of("status", "Server gestoppt", "server", name));
				} catch (Exception e) {
					return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
							.body(Map.of("error", "Fehler beim Stoppen", "message", e.getMessage()));
				}
			} else {
				return ResponseEntity.status(HttpStatus.FORBIDDEN)
						.body(Map.of("error", "User konnte nicht authentifiziert werden"));
			}
		}

		@PostMapping("/{name}/reboot")
		public ResponseEntity<Map<String, String>> rebootServer(@PathVariable String name, @RequestParam String key,
				HttpSession session) {
			Boolean loggedIn = (Boolean) session.getAttribute("loggedIn");
			if (loggedIn != null && loggedIn) {
				if (!apiKey.equals(key)) {
					return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Ungültiger API-Key"));
				}

				try {
					Server.sendToClient(name, "SCMD:REBOOT");
					return ResponseEntity.ok(Map.of("status", "Server neugestartet", "server", name));
				} catch (Exception e) {
					return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
							.body(Map.of("error", "Fehler beim Reboot", "message", e.getMessage()));
				}
			} else {
				return ResponseEntity.status(HttpStatus.FORBIDDEN)
						.body(Map.of("error", "User konnte nicht authentifiziert werden"));
			}
		}

		@PostMapping("/admin/restart")
		public ResponseEntity<Map<String, String>> restartSysStatz(HttpSession session) {
			Boolean loggedIn = (Boolean) session.getAttribute("loggedIn");
			if (loggedIn != null && loggedIn) {
				new Thread(() -> {
					try {
						Thread.sleep(500); // kurz warten, damit Response zurückkommt
						Restart.restartSyStatz(); // erstellt Skript und startet neu
						System.exit(0);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}).start();

				return ResponseEntity.ok(Map.of("status", "SysStatz wird neugestartet"));
			} else {
				return ResponseEntity.status(HttpStatus.FORBIDDEN)
						.body(Map.of("error", "User konnte nicht authentifiziert werden"));
			}
		}

		public class CpuDataPoint {
			private long timestamp;
			private double cpu;

			public CpuDataPoint() {
			}

			public CpuDataPoint(long timestamp, double cpu) {
				this.timestamp = timestamp;
				this.cpu = cpu;
			}

			public long getTimestamp() {
				return timestamp;
			}

			public void setTimestamp(long timestamp) {
				this.timestamp = timestamp;
			}

			public double getCpu() {
				return cpu;
			}

			public void setCpu(double cpu) {
				this.cpu = cpu;
			}
		}

		public class SpikeDetectionResult {
			private boolean spikeDetected;
			private long spikeStartTimestamp;
			private long spikePeakTimestamp;
			private long spikeEndTimestamp;

			public SpikeDetectionResult() {
			}

			public SpikeDetectionResult(boolean spikeDetected, long spikeStartTimestamp, long spikePeakTimestamp,
					long spikeEndTimestamp) {
				this.spikeDetected = spikeDetected;
				this.spikeStartTimestamp = spikeStartTimestamp;
				this.spikePeakTimestamp = spikePeakTimestamp;
				this.spikeEndTimestamp = spikeEndTimestamp;
			}

			public boolean isSpikeDetected() {
				return spikeDetected;
			}

			public void setSpikeDetected(boolean spikeDetected) {
				this.spikeDetected = spikeDetected;
			}

			public long getSpikeStartTimestamp() {
				return spikeStartTimestamp;
			}

			public void setSpikeStartTimestamp(long spikeStartTimestamp) {
				this.spikeStartTimestamp = spikeStartTimestamp;
			}

			public long getSpikePeakTimestamp() {
				return spikePeakTimestamp;
			}

			public void setSpikePeakTimestamp(long spikePeakTimestamp) {
				this.spikePeakTimestamp = spikePeakTimestamp;
			}

			public long getSpikeEndTimestamp() {
				return spikeEndTimestamp;
			}

			public void setSpikeEndTimestamp(long spikeEndTimestamp) {
				this.spikeEndTimestamp = spikeEndTimestamp;
			}
		}

		@GetMapping("/api/server/{name}/processes")
		public ResponseEntity<List<ServerProcessInfo>> getServerProcesses(@PathVariable String name,
				@RequestParam(required = false) Long timestamp) {

			Path txtFile = basePath.resolve(name + "_processes.txt");

			try {
				if (!Files.exists(txtFile)) {
					return ResponseEntity.ok(Collections.emptyList());
				}

				List<String> lines = Files.readAllLines(txtFile);

				// Kein Timestamp -> alle Prozesse
				if (timestamp == null) {
					return ResponseEntity.ok(parseProcesses(lines));
				}

				LocalDateTime requested = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault())
						.toLocalDateTime();

				// Alle Blöcke sammeln
				List<List<String>> allBlocks = new ArrayList<>();
				List<LocalDateTime> allTimes = new ArrayList<>();
				List<String> currentBlock = null;
				LocalDateTime currentTime = null;

				for (String line : lines) {
					if (line.startsWith("#")) {
						if (currentBlock != null && currentTime != null) {
							allBlocks.add(currentBlock);
							allTimes.add(currentTime);
						}
						currentTime = LocalDateTime.parse(line.substring(1).trim()); // # entfernen
						currentBlock = new ArrayList<>();
					} else if (!line.isBlank() && currentBlock != null) {
						currentBlock.add(line);
					}
				}

				// Letzten Block hinzufügen
				if (currentBlock != null && currentTime != null) {
					allBlocks.add(currentBlock);
					allTimes.add(currentTime);
				}

				// Differenzen berechnen und die 5 nächsten Zeitpunkte auswählen
				List<AbstractMap.SimpleEntry<Integer, Long>> distances = new ArrayList<>();
				for (int i = 0; i < allTimes.size(); i++) {
					long diff = Math.abs(Duration.between(allTimes.get(i), requested).toMillis());
					distances.add(new AbstractMap.SimpleEntry<>(i, diff));
				}

				List<Integer> closestIndexes = new ArrayList<>();
				for (int i = 0; i < allTimes.size(); i++) {
					closestIndexes.add(i);
				}
				closestIndexes.sort(Comparator
						.comparingLong(i -> Math.abs(Duration.between(allTimes.get(i), requested).toMillis())));
				closestIndexes = closestIndexes.subList(0, Math.min(5, closestIndexes.size()));

				// Alle Prozesse der 5 nächsten Zeitpunkte zusammenführen
				List<String> resultLines = new ArrayList<>();
				for (int idx : closestIndexes) {
					resultLines.addAll(allBlocks.get(idx));
				}

				return ResponseEntity.ok(parseProcesses(resultLines));

			} catch (Exception e) {
				e.printStackTrace();
				return ResponseEntity.ok(Collections.emptyList());
			}
		}

		private List<ServerProcessInfo> parseProcesses(List<String> lines) {
			return lines.stream().filter(line -> !line.isBlank()).map(line -> {
				String[] parts = line.split(";");
				try {
					int pid = Integer.parseInt(parts[0]);
					String procName = parts[1];
					double cpu = Double.parseDouble(parts[2].replace(",", "."));
					long ram = Long.parseLong(parts[3]);
					return new ServerProcessInfo(pid, procName, cpu, ram);
				} catch (Exception e) {
					return null;
				}
			}).filter(Objects::nonNull).toList();
		}

		@GetMapping("/api/server/{name}/process-history/dates")
		public ResponseEntity<?> getProcessHistoryDates(@PathVariable String name) {
			ServerInfo server = ServerStats.getAllServers().stream().filter(s -> s.getName().equalsIgnoreCase(name))
					.findFirst().orElse(null);

			if (server == null) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Server nicht gefunden"));
			}

			// Alle Tage extrahieren, an denen Daten gespeichert sind
			Set<String> dates = server.getProcessHistory().stream()
					.map(entry -> entry.getTimestamp().toLocalDate().toString())
					.collect(Collectors.toCollection(TreeSet::new));

			return ResponseEntity.ok(dates);
		}

		@GetMapping("/api/server/{name}/process-history/{date}/times")
		public ResponseEntity<?> getProcessHistoryTimes(@PathVariable String name, @PathVariable String date) {

			ServerInfo server = ServerStats.getAllServers().stream().filter(s -> s.getName().equalsIgnoreCase(name))
					.findFirst().orElse(null);

			if (server == null) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Server nicht gefunden"));
			}

			// Uhrzeiten für den Tag sammeln
			List<String> times = server.getProcessHistory().stream()
					.filter(entry -> entry.getTimestamp().toLocalDate().toString().equals(date))
					.map(entry -> entry.getTimestamp().toLocalTime().withSecond(0).withNano(0).toString())
					.collect(Collectors.toList());

			return ResponseEntity.ok(times);
		}

		@GetMapping("/api/server/{name}/process-history/{date}/{time}")
		public ResponseEntity<?> getProcessHistoryAt(@PathVariable String name, @PathVariable String date,
				@PathVariable String time) {

			ServerInfo server = ServerStats.getAllServers().stream().filter(s -> s.getName().equalsIgnoreCase(name))
					.findFirst().orElse(null);

			if (server == null) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Server nicht gefunden"));
			}

			LocalDate localDate = LocalDate.parse(date);
			LocalTime localTime = LocalTime.parse(time);

			// Exaktes Match (oder nahestehendste Zeit suchen)
			Optional<ProcessLogEntry> entry = server.getProcessHistory().stream()
					.filter(e -> e.getTimestamp().toLocalDate().equals(localDate)
							&& e.getTimestamp().toLocalTime().withSecond(0).withNano(0).equals(localTime))
					.findFirst();

			if (entry.isEmpty()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Kein Verlauf gefunden"));
			}

			return ResponseEntity.ok(entry.get().getTopProcesses());
		}
	}
}
