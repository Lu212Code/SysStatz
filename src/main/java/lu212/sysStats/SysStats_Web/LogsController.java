package lu212.sysStats.SysStats_Web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LogsController {

	
    // GET /logs: Seite mit geladenen Logs anzeigen
    @GetMapping("/logs")
    public String logsPage(Model model) {
   	 	String theme = SysStatsWebApplication.theme;
   	 	model.addAttribute("theme", theme);
        return "logs"; // Thymeleaf-Template settings.html
    }
}
