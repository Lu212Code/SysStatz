package lu212.sysStats.General;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONObject;

public class OllamaManager {

	private String serverIP;
	private String model;
	
	public OllamaManager(String serverIP, String model) {
		if(serverIP.equalsIgnoreCase("default")) {
			this.serverIP = "192.168.178.29:11434";
		} else {
			this.serverIP = serverIP;
		}
		if(model.equalsIgnoreCase("default")) {
			this.model = "llama3";
		} else {
		this.model = model;
		}
	}
	
	public void setServerIP(String serverIP) {
		if(serverIP.equalsIgnoreCase("default")) {
			this.serverIP = "http://192.168.178.29:11434";
		} else {
			this.serverIP = serverIP;
		}
	}
	
	public void setModel(String model) {
		if(model.equalsIgnoreCase("default")) {
			this.model = "llama3";
		} else {
		this.model = model;
		}
	}
	
	public String sendRequest(String text) {
		System.out.println("Sende Request...");
		Logger.info("Sende Request...");
		try {
			URL url = new URL("http://" + serverIP + "/api/generate");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/json");
			conn.setDoOutput(true);

			// JSON-Body mit dem Prompt und Modellname (z. B. "llama3")
			String requestBody = "{"
			        + "\"model\": \"" + model + "\","
			        + "\"prompt\": \"" + text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\","
			        + "\"stream\": false"
			        + "}";


			try (OutputStream os = conn.getOutputStream()) {
				byte[] input = requestBody.getBytes("utf-8");
				os.write(input, 0, input.length);
			}
			
			int status = conn.getResponseCode();
			if (status != 200) {
			    return "Ollama-Fehler: HTTP " + status;
			}

			// Antwort lesen
			StringBuilder response = new StringBuilder();
			try (BufferedReader br = new BufferedReader(
					new InputStreamReader(conn.getInputStream(), "utf-8"))) {
				String responseLine;
				while ((responseLine = br.readLine()) != null) {
					response.append(responseLine.trim());
				}
			}

			JSONObject json = new JSONObject(response.toString());
			System.out.println("Ollama Antwort erhalten: " + json.getString("response"));
			Logger.info("Ollama Antwort erhalten: " + json.getString("response"));
			return json.getString("response");

		} catch (Exception e) {
			e.printStackTrace();
			return "Fehler bei Anfrage an Ollama: " + e.getMessage();
		}
	}
	
}
