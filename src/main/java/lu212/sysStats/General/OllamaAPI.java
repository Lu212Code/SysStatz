package lu212.sysStats.General;

import lu212.sysStats.SysStats_Web.SysStatsWebApplication;

public class OllamaAPI {

	public static String sendRequest(String prompt) {
		
		System.out.println("Sende Ollama analyse...");
		Logger.info("Sende Ollama analyse...");
		OllamaManager ollama = new OllamaManager(SysStatsWebApplication.ollamaIP, "default");
		
		return ollama.sendRequest(prompt);
	}
	
}
