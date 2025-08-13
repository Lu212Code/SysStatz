package lu212.sysStats.General;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;

public class Plugins {
	
	private static PluginManager pm;
	
    public static void loadPlugins() {
    	Path pluginFolder = Paths.get("plugins");

    	// Falls der Ordner noch nicht existiert, anlegen
    	if (!Files.exists(pluginFolder)) {
    	    try {
				Files.createDirectories(pluginFolder);
			} catch (IOException e) {
				Logger.error("Plugis konnten nicht geladen werden: " + e.getStackTrace());
				System.err.println("Plugis konnten nicht geladen werden: " + e.getStackTrace());
			}
    	    System.out.println("Plugin-Ordner erstellt: " + pluginFolder.toAbsolutePath());
    	}
    	
    	pm = new PluginManager();
		try {
			pm.loadPlugins(pluginFolder);
		} catch (Exception e) {
			Logger.error("Plugins konnten nicht geladen werden: " + e.getStackTrace());
			System.err.println("Plugins konnten nicht geladen werden: " + e.getStackTrace());
		}
    }
    
    public static void handlePluginInfo(String clientName, String name, String key, String value) {
    	pm.updatePluginValue(clientName, name, key, value);
    }
	
    public static String getPluginName(String key) {
    	return pm.getPluginNameByValueKey(key);
    }
    
    public static PluginInfo getPluginInfo(String pluginName) {
        if (pm == null) return null;
        return pm.getPluginInfo(pluginName);
    }

    public static Map<String, Object> getPluginInfoByServerName(String clientName) {
        if (pm == null) return Collections.emptyMap();
        return pm.getLatestPluginValuesForClient(clientName);
    }
    
    public static String getDownloadLink(String name) {
    	return pm.getPluginDownloadLink(name);
    }
    
    public static Map<String, String> getDownloadLinks(){
    	return Plugins.getDownloadLinks();
    }
}
