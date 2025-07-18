package lu212.sysStats.SysStats_Web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

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
    public String serverDetails(@PathVariable String name, Model model) {
        // Server anhand des Namens suchen
        ServerInfo server = ServerStats.getAllServers().stream()
                .filter(s -> s.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);

        if (server == null) {
            return "redirect:/"; // Zurück zur Übersicht, wenn nicht gefunden
        }

        model.addAttribute("server", server);
        
    	Config config = new Config();
   	 	String theme = SysStatsWebApplication.theme;
   	 	model.addAttribute("theme", theme);
        
        return "server-details"; // Ruft server-details.html auf
    }
    
}
