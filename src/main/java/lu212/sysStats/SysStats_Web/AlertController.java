package lu212.sysStats.SysStats_Web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;

@Controller
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping("/alerts")
    public String alertsPage(Model model, HttpSession session) throws IOException {
        List<AlertService.Alert> alerts = alertService.getAllAlerts();
        model.addAttribute("alerts", alerts);
		String theme = SysStatsWebApplication.theme;
		model.addAttribute("theme", theme);
		model.addAttribute("activePage", "alerts");
		model.addAttribute("isAdmin", session.getAttribute("isAdmin"));
        return "alerts"; // Thymeleaf-Template alerts.html
    }

    @PostMapping("/alerts/add")
    @ResponseBody
    public String addAlert(@RequestParam String text, @RequestParam String level) throws IOException {
        AlertService.Alert.Level lvl;
        try {
            lvl = AlertService.Alert.Level.valueOf(level.toUpperCase());
        } catch (IllegalArgumentException e) {
            return "ERROR: Ungültiges Level";
        }
        alertService.addAlert(text, lvl);
        return "OK";
    }

    @PostMapping("/alerts/delete")
    @ResponseBody
    public String deleteAlert(@RequestParam String text, @RequestParam String level) throws IOException {
        AlertService.Alert.Level lvl;
        try {
            lvl = AlertService.Alert.Level.valueOf(level.toUpperCase());
        } catch (IllegalArgumentException e) {
            return "ERROR: Ungültiges Level";
        }
        AlertService.Alert alert = new AlertService.Alert(lvl, text);
        alertService.deleteAlert(alert);
        return "OK";
    }
}
