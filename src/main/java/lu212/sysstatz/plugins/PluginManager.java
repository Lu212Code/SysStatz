package lu212.sysstatz.plugins;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PluginManager {

    private Map<String, PluginInfo> plugins = new HashMap<>();
    private Set<String> registeredValueKeys = new HashSet<>();
    private Map<String, Object> pluginValues = new HashMap<>();
    private Map<String, String> pluginDownloadLinks = new HashMap<>();
    private Map<String, Map<String, Object>> latestPluginValues = new ConcurrentHashMap<>();

    /**
     * Lädt alle Plugins aus .txt-Dateien im Plugin-Ordner,
     * registriert nur die valueKeys.
     */
    public void loadPlugins(Path pluginFolder) throws Exception {
        plugins.clear();
        registeredValueKeys.clear();
        pluginDownloadLinks.clear();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(pluginFolder, "*.txt")) {
            for (Path file : stream) {
                PluginInfo p = PluginInfo.fromFile(file);
                if (p.name != null && p.valueKey != null) {
                    plugins.put(p.name, p);
                    pluginDownloadLinks.put(p.name, p.downloadLink);

                    // Nur ValueKey registrieren
                    String key = p.name + "|" + p.valueKey;
                    registeredValueKeys.add(key);

                    System.out.println("Plugin geladen: " + p.name + " -> " + p.downloadLink);
                }
            }
        }
    }

    public Map<String, String> getPluginDownloadLinks() {
        return Collections.unmodifiableMap(pluginDownloadLinks);
    }
    
    public String getPluginDownloadLink(String pluginName) {
        PluginInfo info = plugins.get(pluginName);
        if (info != null) {
            return info.downloadLink;
        }
        return null; // Plugin existiert nicht
    }

    public void updatePluginValue(String clientName, String pluginName, String valueKey, String valueStr) {
        String regKey = pluginName + "|" + valueKey;
        if (!registeredValueKeys.contains(regKey)) {
            System.out.println("Nicht registrierter Wert empfangen: " + regKey);
            return;
        }

        Object value;
        try {
            if (valueStr.contains(".")) {
                value = Double.parseDouble(valueStr);
            } else {
                value = Integer.parseInt(valueStr);
            }
        } catch (NumberFormatException e) {
            value = valueStr;
        }

        // Werte pro Server aktualisieren
        latestPluginValues.computeIfAbsent(clientName, k -> new ConcurrentHashMap<>())
                          .put(regKey, value);

        // Optional: Logging
        PluginInfo info = plugins.get(pluginName);
        if (info != null) {
            String pluginDisplayName = info.displayName != null ? info.displayName : pluginName;
            String unit = info.unit != null ? info.unit : "";
        }
    }

    public Map<String, Object> getPluginValues() {
        return Collections.unmodifiableMap(pluginValues);
    }

    public boolean isValueRegistered(String pluginName, String valueKey) {
        return registeredValueKeys.contains(pluginName + "|" + valueKey);
    }

    public PluginInfo getPluginInfo(String name) {
        return plugins.get(name);
    }
    
    public String getPluginNameByValueKey(String valueKey) {
        for (Map.Entry<String, PluginInfo> entry : plugins.entrySet()) {
            if (entry.getValue().valueKey.equals(valueKey)) {
                return entry.getValue().name;
            }
        }
        return null;
    }
    
    // Alle Werte für einen Server
    public Map<String, Object> getLatestPluginValuesForClient(String clientName) {
        return latestPluginValues.getOrDefault(clientName, Collections.emptyMap());
    }

    // Einen bestimmten Wert
    public Object getLatestPluginValue(String clientName, String pluginName, String valueKey) {
        String regKey = pluginName + "|" + valueKey;
        Map<String, Object> clientValues = latestPluginValues.get(clientName);
        if (clientValues != null) {
            return clientValues.get(regKey);
        }
        return null;
    }
    
    public Map<String, String> getAllDownloadLinks() {
        return Collections.unmodifiableMap(pluginDownloadLinks);
    }
}
