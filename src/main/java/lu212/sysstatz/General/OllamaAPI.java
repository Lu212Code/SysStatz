package lu212.sysstatz.General;

import lu212.sysstatz.SysStats_Web.SysStatsWebApplication;
import lu212.sysstatz.logging.Logger;

public class OllamaAPI {

	public static String sendRequest(String prompt) {
		
		System.out.println("Sende Ollama analyse...");
		Logger.info("Sende Ollama analyse...");
		OllamaManager ollama = new OllamaManager(SysStatsWebApplication.ollamaIP, "default");
		
		return ollama.sendRequest(prompt);
	}
	
}
