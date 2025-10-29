package lu212.sysstatz.SysStats_Web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpSession;

@Controller
public class LogsController {

	
    // GET /logs: Seite mit geladenen Logs anzeigen
    @GetMapping("/logs")
    public String logsPage(Model model, HttpSession session) {
		Boolean loggedIn = (Boolean) session.getAttribute("loggedIn");
		if (loggedIn != null && loggedIn) {
   	 	String theme = SysStatsWebApplication.theme;
   	 	model.addAttribute("theme", theme);
   	 	model.addAttribute("activePage", "logs");
   	 	model.addAttribute("isAdmin", session.getAttribute("isAdmin"));
        return "logs"; // Thymeleaf-Template settings.html
		} else {
			return "redirect:/login?error=sessionExpired";
		}
    }
}
