package lu212.sysStats.SysStats_Web;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import jakarta.servlet.http.HttpSession;
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
		// Server anhand des Namens suchen
		ServerInfo server = ServerStats.getAllServers().stream().filter(s -> s.getName().equalsIgnoreCase(name))
				.findFirst().orElse(null);

		if (server == null) {
			return "redirect:/"; // Zurück zur Übersicht, wenn nicht gefunden
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

		return "server-details"; // Ruft server-details.html auf
	}

	@GetMapping("/compare")
	public String compareServers(Model model, HttpSession session) {
		String theme = SysStatsWebApplication.theme;
		model.addAttribute("theme", theme);
		model.addAttribute("activePage", "compare");
		model.addAttribute("isAdmin", session.getAttribute("isAdmin"));
		return "compare"; // `compare.html` in `templates/`
	}
}
