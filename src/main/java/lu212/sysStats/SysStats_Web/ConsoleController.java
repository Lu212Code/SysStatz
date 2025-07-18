package lu212.sysStats.SysStats_Web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import lu212.sysStats.General.SysStatzInfo;

import java.io.PrintStream;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@RestController
public class ConsoleController {

	private Set<WebSocketSession> sessions = Collections.synchronizedSet(new HashSet<>());

	private DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

	// WebSocket Handler, registrieren über WebSocketConfig (siehe unten)
	public class LogWebSocketHandler extends TextWebSocketHandler {
		@Override
		public void afterConnectionEstablished(WebSocketSession session) {
			sessions.add(session);
		}

		@Override
		public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) {
			sessions.remove(session);
		}
	}

	// Logs an alle Sessions senden
	public void sendLog(String message, boolean isError) {
		String time = LocalTime.now().format(timeFormatter);
		String coloredMsg = isError ? "[ERR " + time + "] " + message : "[OUT " + time + "] " + message;

		synchronized (sessions) {
			for (WebSocketSession session : sessions) {
				try {
					session.sendMessage(new TextMessage(coloredMsg));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	// Umleitung von System.out und System.err
	@jakarta.annotation.PostConstruct
	public void redirectSystemStreams() {
		PrintStream originalOut = System.out;
		PrintStream originalErr = System.err;

		System.setOut(new PrintStream(new OutputStreamInterceptor(false, originalOut)));
		System.setErr(new PrintStream(new OutputStreamInterceptor(true, originalErr)));
	}

	// Eigene OutputStream-Klasse, die den Output abfängt
	private class OutputStreamInterceptor extends java.io.OutputStream {
		private boolean errorStream;
		private PrintStream original;
		private StringBuilder buffer = new StringBuilder();

		public OutputStreamInterceptor(boolean errorStream, PrintStream original) {
			this.errorStream = errorStream;
			this.original = original;
		}

		@Override
		public void write(int b) {
			char c = (char) b;
			buffer.append(c);
			if (c == '\n') {
				String line = buffer.toString();
				sendLog(line.trim(), errorStream);
				original.print(line); // Original weiterleiten
				buffer.setLength(0);
			}
		}
	}

	// Beispiel: API um eingehende Befehle zu verarbeiten (POST /console/input)
	@PostMapping("/console/input")
	public void onInput(@RequestBody String command) {
		System.out.println("Eingabe erhalten: " + command);
		// Hier kannst du deine Methode triggern:
		handleCommand(command);
	}

	private void handleCommand(String command) {
		if ("stop".equalsIgnoreCase(command.trim())) {
			System.exit(0);
		} else if ("help".equalsIgnoreCase(command.trim())) {
			System.out.println("--------------------Verfügbare Befehle--------------------");
			System.out.println("stop - Server herunterfahren");
			System.out.println("----------------------------------------------------------");
		} else if ("info".equalsIgnoreCase(command.trim())) {
			System.out.println("----------------------SysStatz - Info----------------------");
			System.out.println("SysStatz Version: " + SysStatzInfo.version);
			System.out.println("Name: " + SysStatzInfo.name);
			System.out.println("Description: " + SysStatzInfo.description);
			System.out.println("-----------------------------------------------------------");
		} else {
			System.err.println("Unbekannter Befehl: " + command);
		}
	}
}