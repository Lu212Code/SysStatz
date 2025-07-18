package lu212.sysStats.SysStats_Web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;

@Controller
public class LoginController {

 @GetMapping("/login")
 public String showLoginPage(@RequestParam(value = "error", required = false) String error, Model model) {
     if (error != null) {
         model.addAttribute("error", "Falsches Passwort");
     }
	 String theme = SysStatsWebApplication.theme;
	 model.addAttribute("theme", theme);
     return "login";
 }
 
 @PostMapping("/login")
 public String handleLogin(@RequestParam String password, HttpSession session) {
     if (AuthService.checkPassword(password)) {
         session.setAttribute("loggedIn", true);
         return "redirect:/dashboard";
     } else {
         return "redirect:/login?error=true";
     }
 }


 @GetMapping("/dashboard")
 public String showDashboard(HttpSession session, Model model) {
     Boolean loggedIn = (Boolean) session.getAttribute("loggedIn");
     if (loggedIn != null && loggedIn) {
    	 
   	 	String theme = SysStatsWebApplication.theme;
   	 	model.addAttribute("theme", theme);
    	 
         return "dashboard";
     } else {
         return "redirect:/login?error=sessionExpired";
     }
 }

 
 @GetMapping("/logout")
 public String logout(HttpSession session) {
     session.invalidate();
     return "redirect:/login?logout=true";
 }
}
