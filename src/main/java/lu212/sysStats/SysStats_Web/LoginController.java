package lu212.sysStats.SysStats_Web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import lu212.sysStats.General.User;
import lu212.sysStats.General.UserStore;

@Controller
public class LoginController {

	@GetMapping("/login")
	public String showLoginPage(@RequestParam(value = "error", required = false) String error, Model model) {

		if (!UserStore.hasUsers()) {
			return "redirect:/createadminuser";
		}

		if (error != null) {
			model.addAttribute("error", "Falsches Passwort");
		}
		String theme = SysStatsWebApplication.theme;
		model.addAttribute("theme", theme);
		return "login";
	}

	@PostMapping("/login")
	public String handleLogin(@RequestParam String username, @RequestParam String password, HttpSession session) {
		if (AuthService.authenticate(username, password, session)) {
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

	@GetMapping("/createadminuser")
	public String showCreateAdminPage(Model model) {
		model.addAttribute("theme", SysStatsWebApplication.theme);
		return "createadminuser";
	}

	@PostMapping("/createadminuser")
	public String createAdminUser(@RequestParam String username, @RequestParam String password, HttpSession session) {
		User newUser = new User(username, password, true);
		UserStore.addUser(newUser);
		session.setAttribute("loggedIn", true);
		session.setAttribute("username", username);
		session.setAttribute("isAdmin", true);
		return "redirect:/dashboard";
	}

}
