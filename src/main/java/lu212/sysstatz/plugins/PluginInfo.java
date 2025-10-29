package lu212.sysstatz.plugins;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class PluginInfo {
    public final String name;         // Interner Name (Frontend & Identifikation)
    public final String displayName;  // Schönformatierter Name
    public final String valueKey;     // Schlüssel vom Client
    public final String unit;         // Einheit
    public final String downloadLink; // Link zur JAR

    public PluginInfo(String name, String displayName, String valueKey, String unit, String downloadLink) {
        this.name = name;
        this.displayName = displayName;
        this.valueKey = valueKey;
        this.unit = unit;
        this.downloadLink = downloadLink;
    }

    @Override
    public String toString() {
        return displayName + " (" + valueKey + " " + unit + ") [" + downloadLink + "]";
    }

    public static PluginInfo fromFile(Path file) throws Exception {
        List<String> lines = Files.readAllLines(file);
        String name = null, displayName = null, valueKey = null, unit = null, downloadLink = null;

        for (String line : lines) {
            String[] parts = line.split("=", 2);
            if (parts.length != 2) continue;

            String key = parts[0].trim().toLowerCase();
            String value = parts[1].trim();

            switch (key) {
                case "name": name = value; break;
                case "displayname": displayName = value; break;
                case "valuekey": valueKey = value; break;
                case "unit": unit = value; break;
                case "downloadlink": downloadLink = value; break;
            }
        }

        return new PluginInfo(name, displayName, valueKey, unit, downloadLink);
    }
}
