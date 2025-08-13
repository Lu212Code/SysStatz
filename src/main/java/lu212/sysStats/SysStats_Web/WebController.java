package lu212.sysStats.SysStats_Web;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import jakarta.servlet.http.HttpSession;
import lu212.sysStats.General.PluginInfo;
import lu212.sysStats.General.Plugins;
import lu212.sysStats.StatsServer.Server;

@Controller
public class WebController {

	@GetMapping("/")
	public String index(Model model) {
		model.addAttribute("servers", ServerStats.getAllServers());
		String theme = SysStatsWebApplication.theme;
		model.addAttribute("theme", theme);
		return "index";
	}

	@GetMapping("/server/{name}")
	public String serverDetails(@PathVariable String name, Model model, HttpSession session) {
		Boolean loggedIn = (Boolean) session.getAttribute("loggedIn");
		if (loggedIn != null && loggedIn) {
			// Server anhand des Namens suchen
			ServerInfo server = ServerStats.getAllServers().stream().filter(s -> s.getName().equalsIgnoreCase(name))
					.findFirst().orElse(null);

			if (server == null) {
				return "redirect:/dashboard"; // Zurück zur Übersicht, wenn nicht gefunden
			}

			model.addAttribute("server", server);
			model.addAttribute("isAdmin", session.getAttribute("isAdmin"));

			Config config = new Config();
			String theme = SysStatsWebApplication.theme;

			Map<String, String> hardware = ServerStats.getHardwareInfo(name);

			model.addAttribute("theme", theme);
			model.addAttribute("hardwareInfo", hardware);

			Map<String, Server.GeoInfo> geoLocations = Server.getGeoLocations();
			Server.GeoInfo geoInfo = geoLocations.get(name);

			model.addAttribute("geoInfo", geoInfo);

			//Plugin Daten
			Map<String, Object> rawValues = Plugins.getPluginInfoByServerName(name);
			Map<String, Map<String, String>> grouped = new LinkedHashMap<>();

			if (rawValues != null && !rawValues.isEmpty()) {
				for (Map.Entry<String, Object> entry : rawValues.entrySet()) {
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
			}

			model.addAttribute("pluginData", grouped);

			return "server-details"; // Ruft server-details.html auf
		} else {
			return "redirect:/login?error=sessionExpired";
		}
	}

	@GetMapping("/compare")
	public String compareServers(Model model, HttpSession session) {
		Boolean loggedIn = (Boolean) session.getAttribute("loggedIn");
		if (loggedIn != null && loggedIn) {
			String theme = SysStatsWebApplication.theme;
			model.addAttribute("theme", theme);
			model.addAttribute("activePage", "compare");
			model.addAttribute("isAdmin", session.getAttribute("isAdmin"));
			return "compare"; // `compare.html` in `templates/`
		} else {
			return "redirect:/login?error=sessionExpired";
		}
	}

	@GetMapping("/longterm-analysis")
	public String showLongtermAnalysisPage(Model model, HttpSession session) {
		Boolean loggedIn = (Boolean) session.getAttribute("loggedIn");
		if (loggedIn != null && loggedIn) {
			String theme = SysStatsWebApplication.theme;
			model.addAttribute("theme", theme);
			model.addAttribute("activePage", "longterm");
			model.addAttribute("isAdmin", session.getAttribute("isAdmin"));
			return "longterm-analysis";
		} else {
			return "redirect:/login?error=sessionExpired";
		}
	}
}
